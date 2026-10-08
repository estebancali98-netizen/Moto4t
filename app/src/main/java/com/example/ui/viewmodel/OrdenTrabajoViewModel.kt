package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.R
import com.example.data.local.WorkshopDatabase
import com.example.data.model.QualityChecklist
import com.example.data.model.QuoteItem
import com.example.data.model.WorkOrder
import com.example.data.model.WorkOrderStatus
import com.example.data.remote.FirestoreSyncService
import com.example.data.repository.WorkshopRepository
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Estado de la UI para el flujo de trabajo de órdenes (Kanban y Detalle)
 */
sealed interface OrdenTrabajoUiState {
    object Loading : OrdenTrabajoUiState
    data class Success(
        val orders: List<WorkOrder> = emptyList(),
        val selectedOrder: WorkOrder? = null,
        val isCloudSyncing: Boolean = false,
        val lastSyncTimestamp: Long = System.currentTimeMillis()
    ) : OrdenTrabajoUiState
    data class Error(val message: String) : OrdenTrabajoUiState
}

class OrdenTrabajoViewModel(application: Application) : AndroidViewModel(application) {

    // Instancia de Firestore configurada con el Database ID dedicado del proyecto
    private val firestore: FirebaseFirestore by lazy {
        val databaseId = application.getString(R.string.firestore_database_id)
        FirebaseFirestore.getInstance(databaseId)
    }

    private val firestoreSync = FirestoreSyncService(application)
    private val repository = WorkshopRepository(
        WorkshopDatabase.getDatabase(application),
        firestoreSync
    )

    // Filtro activo de columna Kanban (null = Todas)
    private val _selectedStatusFilter = MutableStateFlow<WorkOrderStatus?>(null)
    val selectedStatusFilter: StateFlow<WorkOrderStatus?> = _selectedStatusFilter.asStateFlow()

    // ID de la orden actualmente seleccionada en pantalla de detalle
    private val _selectedOrderId = MutableStateFlow<String?>(null)
    val selectedOrderId: StateFlow<String?> = _selectedOrderId.asStateFlow()

    // Indicador transitorio de sincronización en progreso
    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    // Listener en tiempo real de Firestore para la colección 'work_orders'
    private var firestoreListener: ListenerRegistration? = null

    // Flujo principal de la UI conectado reactivamente mediante StateFlow
    val uiState: StateFlow<OrdenTrabajoUiState> = combine(
        repository.workOrders,
        _selectedOrderId,
        _isSyncing
    ) { orders, selectedId, syncing ->
        val selected = orders.find { it.id == selectedId }
        val state: OrdenTrabajoUiState = OrdenTrabajoUiState.Success(
            orders = orders,
            selectedOrder = selected,
            isCloudSyncing = syncing,
            lastSyncTimestamp = System.currentTimeMillis()
        )
        state
    }.catch { error ->
        emit(OrdenTrabajoUiState.Error(error.localizedMessage ?: "Error al cargar órdenes de trabajo"))
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = OrdenTrabajoUiState.Loading
    )

    // Órdenes filtradas para el tablero Kanban
    val kanbanOrders: StateFlow<List<WorkOrder>> = combine(
        repository.workOrders,
        _selectedStatusFilter
    ) { orders, statusFilter ->
        if (statusFilter == null) orders else orders.filter { it.status == statusFilter }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = emptyList()
    )

    init {
        // Escucha estado de autenticación para activar Firestore únicamente cuando el usuario esté autenticado
        Firebase.auth.addAuthStateListener { auth ->
            if (auth.currentUser != null) {
                attachFirestoreRealtimeListener()
            } else {
                firestoreListener?.remove()
                _isSyncing.value = false
            }
        }
    }

    /**
     * Conecta el Snapshot Listener de Firestore a la base de datos
     */
    private fun attachFirestoreRealtimeListener() {
        firestoreListener?.remove()
        if (Firebase.auth.currentUser == null) {
            _isSyncing.value = false
            return
        }
        _isSyncing.value = true

        firestoreListener = firestore.collection("work_orders")
            .addSnapshotListener { snapshot, error ->
                _isSyncing.value = false
                if (error != null) {
                    return@addSnapshotListener
                }

                if (snapshot != null && !snapshot.isEmpty) {
                    viewModelScope.launch {
                        for (change in snapshot.documentChanges) {
                            val data = change.document.data
                            val orderId = change.document.id
                            val statusName = data["status"] as? String ?: continue
                            val newStatus = try {
                                WorkOrderStatus.valueOf(statusName)
                            } catch (_: Exception) {
                                null
                            }

                            if (newStatus != null) {
                                val current = (uiState.value as? OrdenTrabajoUiState.Success)?.orders?.find { it.id == orderId }
                                if (current != null && current.status != newStatus) {
                                    repository.updateOrderStatus(orderId, newStatus, "Firestore Realtime", "Cloud")
                                }
                            }
                        }
                    }
                }
            }
    }

    /**
     * Cambia la columna o estado de una orden en el Kanban y sincroniza con Firestore
     */
    fun updateOrderStatus(orderId: String, newStatus: WorkOrderStatus) {
        viewModelScope.launch {
            val user = Firebase.auth.currentUser
            val userName = user?.displayName ?: "Mostrador"

            // 1. Actualiza repositorio local siempre
            repository.updateOrderStatus(orderId, newStatus, userName, "Operación")

            // 2. Si está autenticado, propaga cambio a Firestore en tiempo real
            if (user != null) {
                val payload = hashMapOf<String, Any>(
                    "status" to newStatus.name,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                if (newStatus == WorkOrderStatus.DELIVERED) {
                    payload["actualDeliveryTimestamp"] = System.currentTimeMillis()
                }

                firestore.collection("work_orders").document(orderId)
                    .set(payload, SetOptions.merge())
            }
        }
    }

    /**
     * Selecciona una orden para ver en pantalla de detalle
     */
    fun selectOrder(orderId: String?) {
        _selectedOrderId.value = orderId
    }

    /**
     * Aplica filtro por estado en el tablero Kanban
     */
    fun setKanbanFilter(status: WorkOrderStatus?) {
        _selectedStatusFilter.value = status
    }

    /**
     * Inicia o pausa el cronómetro de productividad del mecánico
     */
    fun toggleMechanicTimer(orderId: String) {
        viewModelScope.launch {
            val orders = repository.workOrders.first()
            val order = orders.find { it.id == orderId } ?: return@launch
            val now = System.currentTimeMillis()
            val updated = if (order.isTimerRunning) {
                val elapsed = if (order.workStartTime != null) ((now - order.workStartTime) / 60000).toInt().coerceAtLeast(1) else 0
                order.copy(
                    isTimerRunning = false,
                    workEndTime = now,
                    minutesWorked = order.minutesWorked + elapsed
                )
            } else {
                order.copy(
                    isTimerRunning = true,
                    workStartTime = now,
                    status = if (order.status == WorkOrderStatus.RECEIVED || order.status == WorkOrderStatus.DIAGNOSIS) WorkOrderStatus.IN_REPAIR else order.status
                )
            }

            val user = Firebase.auth.currentUser
            repository.saveWorkOrder(updated, user?.displayName ?: "Mecánico", "Mecánico")

            if (user != null) {
                firestore.collection("work_orders").document(orderId).set(
                    hashMapOf(
                        "isTimerRunning" to updated.isTimerRunning,
                        "minutesWorked" to updated.minutesWorked,
                        "status" to updated.status.name,
                        "updatedAt" to FieldValue.serverTimestamp()
                    ),
                    SetOptions.merge()
                )
            }
        }
    }

    /**
     * Agrega un repuesto o mano de obra a la cotización y la sincroniza en la nube
     */
    fun addQuoteItem(orderId: String, item: QuoteItem) {
        viewModelScope.launch {
            val user = Firebase.auth.currentUser
            repository.addQuoteItem(orderId, item, user?.displayName ?: "Mostrador", "Admin")

            val currentOrder = repository.workOrders.first().find { it.id == orderId }
            if (currentOrder != null && user != null) {
                val updatedItems = currentOrder.quoteItems + item
                firestore.collection("work_orders").document(orderId).set(
                    hashMapOf(
                        "totalApprovedQuote" to updatedItems.filter { it.isApproved }.sumOf { it.subtotal },
                        "updatedAt" to FieldValue.serverTimestamp()
                    ),
                    SetOptions.merge()
                )
            }
        }
    }

    /**
     * Autoriza o desaprueba un ítem específico del presupuesto
     */
    fun toggleQuoteItemApproval(orderId: String, itemId: String, approved: Boolean) {
        viewModelScope.launch {
            val user = Firebase.auth.currentUser
            repository.toggleItemApproval(orderId, itemId, approved, user?.displayName ?: "Cliente", "CRM")

            val currentOrder = repository.workOrders.first().find { it.id == orderId }
            if (currentOrder != null && user != null) {
                val newTotal = currentOrder.quoteItems.map {
                    if (it.id == itemId) it.copy(isApproved = approved) else it
                }.filter { it.isApproved }.sumOf { it.subtotal }

                firestore.collection("work_orders").document(orderId).set(
                    hashMapOf(
                        "totalApprovedQuote" to newTotal,
                        "balanceDue" to (newTotal - currentOrder.paidAmount).coerceAtLeast(0.0),
                        "updatedAt" to FieldValue.serverTimestamp()
                    ),
                    SetOptions.merge()
                )
            }
        }
    }

    /**
     * Registra un pago de anticipo o liquidación final y actualiza saldo en Firestore
     */
    fun recordPayment(orderId: String, amount: Double, method: String, isAdvance: Boolean) {
        viewModelScope.launch {
            val user = Firebase.auth.currentUser
            repository.recordPayment(orderId, amount, method, isAdvance, user?.displayName ?: "Cajero", "Caja")

            val currentOrder = repository.workOrders.first().find { it.id == orderId }
            if (currentOrder != null && user != null) {
                val newPaid = currentOrder.paidAmount + amount
                val newBalance = (currentOrder.totalApprovedQuote - newPaid).coerceAtLeast(0.0)

                firestore.collection("work_orders").document(orderId).set(
                    hashMapOf(
                        "paidAmount" to newPaid,
                        "balanceDue" to newBalance,
                        "paymentMethod" to method,
                        "updatedAt" to FieldValue.serverTimestamp()
                    ),
                    SetOptions.merge()
                )
            }
        }
    }

    /**
     * Guarda el checklist de control de calidad final de 5 puntos
     */
    fun updateQualityChecklist(orderId: String, checklist: QualityChecklist, warrantyDays: Int) {
        viewModelScope.launch {
            val orders = repository.workOrders.first()
            val order = orders.find { it.id == orderId } ?: return@launch
            val updated = order.copy(
                qualityChecklist = checklist,
                warrantyDays = warrantyDays,
                status = if (checklist.isFullyCompleted && order.status != WorkOrderStatus.DELIVERED) WorkOrderStatus.READY else order.status
            )

            val user = Firebase.auth.currentUser
            repository.saveWorkOrder(updated, user?.displayName ?: "Calidad", "Control")

            if (user != null) {
                val payload = hashMapOf<String, Any>(
                    "warrantyDays" to warrantyDays,
                    "isQualityCompleted" to checklist.isFullyCompleted,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                if (checklist.isFullyCompleted) {
                    payload["status"] = WorkOrderStatus.READY.name
                }

                firestore.collection("work_orders").document(orderId).set(payload, SetOptions.merge())
            }
        }
    }

    /**
     * Registra una nota de evidencia o fotografía de pieza averiada
     */
    fun addPhotoEvidenceNote(orderId: String, note: String) {
        viewModelScope.launch {
            val orders = repository.workOrders.first()
            val order = orders.find { it.id == orderId } ?: return@launch
            val updated = order.copy(photoNotes = order.photoNotes + note)

            val user = Firebase.auth.currentUser
            repository.saveWorkOrder(updated, user?.displayName ?: "Mecánico", "Mecánico")

            if (user != null) {
                firestore.collection("work_orders").document(orderId).set(
                    hashMapOf(
                        "photoNotes" to FieldValue.arrayUnion(note),
                        "updatedAt" to FieldValue.serverTimestamp()
                    ),
                    SetOptions.merge()
                )
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        firestoreListener?.remove()
    }
}
