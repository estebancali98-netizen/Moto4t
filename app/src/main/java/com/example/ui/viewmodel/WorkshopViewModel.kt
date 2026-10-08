package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.WorkshopDatabase
import com.example.data.model.AppUser
import com.example.data.model.AuditLog
import com.example.data.model.CashEntry
import com.example.data.model.CheckStatus
import com.example.data.model.Client
import com.example.data.model.ExpressCheckItem
import com.example.data.model.FuelLevel
import com.example.data.model.InventoryItem
import com.example.data.model.Motorcycle
import com.example.data.model.QualityChecklist
import com.example.data.model.QuoteItem
import com.example.data.model.UserRole
import com.example.data.model.WorkOrder
import com.example.data.model.WorkOrderStatus
import com.example.data.repository.WorkshopRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

sealed class AppScreen {
    object Dashboard : AppScreen()
    object Orders : AppScreen()
    data class OrderDetail(val orderId: String) : AppScreen()
    object CreateOrder : AppScreen()
    object MechanicMode : AppScreen()
    object Inventory : AppScreen()
    object CrmClients : AppScreen()
    object CashRegister : AppScreen()
    object ReportsAudit : AppScreen()
}

class WorkshopViewModel(application: Application) : AndroidViewModel(application) {

    val authService = com.example.data.auth.AuthService(application)
    private val firestoreSync = com.example.data.remote.FirestoreSyncService(application)
    private val repository = WorkshopRepository(WorkshopDatabase.getDatabase(application), firestoreSync)

    val availableUsers = listOf(
        AppUser("user_admin", "Alejandro Admin", UserRole.ADMIN, "admin@mototaller.com"),
        AppUser("user_recep", "Laura Recepción", UserRole.RECEPTIONIST, "recepcion@mototaller.com"),
        AppUser("mech_1", "Carlos Mecánico", UserRole.MECHANIC, "carlos@mototaller.com"),
        AppUser("mech_2", "Mateo Especialista", UserRole.MECHANIC, "mateo@mototaller.com"),
        AppUser("user_bodega", "Andrés Bodega", UserRole.WAREHOUSE, "bodega@mototaller.com")
    )

    private val _currentUser = MutableStateFlow(availableUsers[0])
    val currentUser: StateFlow<AppUser> = _currentUser.asStateFlow()
    val authenticatedUser: StateFlow<AppUser?> = authService.currentUserState

    private val _currentScreen = MutableStateFlow<AppScreen>(AppScreen.Dashboard)
    val currentScreen: StateFlow<AppScreen> = _currentScreen.asStateFlow()

    private val _screenStack = mutableListOf<AppScreen>(AppScreen.Dashboard)

    val clients: StateFlow<List<Client>> = repository.clients.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    val motorcycles: StateFlow<List<Motorcycle>> = repository.motorcycles.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    val inventory: StateFlow<List<InventoryItem>> = repository.inventory.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    val workOrders: StateFlow<List<WorkOrder>> = repository.workOrders.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    val cashEntries: StateFlow<List<CashEntry>> = repository.cashEntries.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    val auditLogs: StateFlow<List<AuditLog>> = repository.auditLogs.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    init {
        viewModelScope.launch {
            repository.seedInitialDataIfEmpty()
        }
        viewModelScope.launch {
            authService.currentUserState.collect { user ->
                if (user != null) {
                    _currentUser.value = user
                    if (user.role == UserRole.MECHANIC && _currentScreen.value == AppScreen.Dashboard) {
                        navigateTo(AppScreen.MechanicMode)
                    }
                }
            }
        }
    }

    fun switchUserRole(newRole: UserRole) {
        val current = _currentUser.value
        viewModelScope.launch {
            authService.setUserRole(current.id, newRole)
            _currentUser.value = current.copy(role = newRole)
            if (newRole == UserRole.MECHANIC) {
                navigateTo(AppScreen.MechanicMode)
            } else if (_currentScreen.value == AppScreen.MechanicMode) {
                navigateTo(AppScreen.Dashboard)
            }
        }
    }

    fun signOut(onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            authService.signOut()
            onComplete()
        }
    }

    fun switchUser(user: AppUser) {
        _currentUser.value = user
        if (user.role == UserRole.MECHANIC) {
            navigateTo(AppScreen.MechanicMode)
        }
    }

    fun navigateTo(screen: AppScreen) {
        if (_screenStack.lastOrNull() != screen) {
            _screenStack.add(screen)
        }
        _currentScreen.value = screen
    }

    fun handleBack(): Boolean {
        if (_screenStack.size > 1) {
            _screenStack.removeAt(_screenStack.size - 1)
            _currentScreen.value = _screenStack.last()
            return true
        }
        return false
    }

    fun updateOrderStatus(orderId: String, newStatus: WorkOrderStatus) {
        viewModelScope.launch {
            repository.updateOrderStatus(
                orderId,
                newStatus,
                _currentUser.value.name,
                _currentUser.value.role.label
            )
        }
    }

    fun toggleQuoteItemApproval(orderId: String, itemId: String, approved: Boolean) {
        viewModelScope.launch {
            repository.toggleItemApproval(
                orderId,
                itemId,
                approved,
                _currentUser.value.name,
                _currentUser.value.role.label
            )
        }
    }

    fun addQuoteItem(orderId: String, item: QuoteItem) {
        viewModelScope.launch {
            repository.addQuoteItem(
                orderId,
                item,
                _currentUser.value.name,
                _currentUser.value.role.label
            )
            // If item has a matching SKU or inventory, auto-discount
            val invMatch = inventory.value.find { it.description.contains(item.description, ignoreCase = true) }
            if (invMatch != null) {
                repository.adjustStock(
                    invMatch.id,
                    -item.quantity,
                    _currentUser.value.name,
                    _currentUser.value.role.label
                )
            }
        }
    }

    fun addPresetKit(orderId: String, comboType: String) {
        viewModelScope.launch {
            when (comboType) {
                "ARRAS" -> {
                    addQuoteItem(
                        orderId,
                        QuoteItem(
                            id = "kit_arr_${System.currentTimeMillis()}",
                            description = "Combo Kit de Arrastre Completo Reforzado + Instalación",
                            type = "COMBO",
                            quantity = 1,
                            unitPrice = 115000.0,
                            isApproved = true
                        )
                    )
                }
                "AFINA" -> {
                    addQuoteItem(
                        orderId,
                        QuoteItem(
                            id = "kit_afin_${System.currentTimeMillis()}",
                            description = "Combo Afinación Mayor (Ultrasonido + Bujía + Filtros + Calibración)",
                            type = "COMBO",
                            quantity = 1,
                            unitPrice = 85000.0,
                            isApproved = true
                        )
                    )
                }
                "ACEITE" -> {
                    addQuoteItem(
                        orderId,
                        QuoteItem(
                            id = "kit_aceite_${System.currentTimeMillis()}",
                            description = "Combo Cambio de Aceite Motul 5100 + Filtro + Calibración Frenos",
                            type = "COMBO",
                            quantity = 1,
                            unitPrice = 65000.0,
                            isApproved = true
                        )
                    )
                }
                "FRENOS" -> {
                    addQuoteItem(
                        orderId,
                        QuoteItem(
                            id = "kit_frenos_${System.currentTimeMillis()}",
                            description = "Combo Frenos Integral (Pastillas Ichiban + Bandas + Líquido DOT4)",
                            type = "COMBO",
                            quantity = 1,
                            unitPrice = 72000.0,
                            isApproved = true
                        )
                    )
                }
            }
        }
    }

    fun toggleMechanicTimer(orderId: String) {
        viewModelScope.launch {
            val order = workOrders.value.find { it.id == orderId } ?: return@launch
            val now = System.currentTimeMillis()
            val updated = if (order.isTimerRunning) {
                // Pause timer and add elapsed minutes
                val elapsedMinutes = if (order.workStartTime != null) {
                    ((now - order.workStartTime) / 60000).toInt().coerceAtLeast(1)
                } else 0
                order.copy(
                    isTimerRunning = false,
                    workEndTime = now,
                    minutesWorked = order.minutesWorked + elapsedMinutes
                )
            } else {
                // Start timer
                order.copy(
                    isTimerRunning = true,
                    workStartTime = now,
                    status = if (order.status == WorkOrderStatus.RECEIVED || order.status == WorkOrderStatus.DIAGNOSIS)
                        WorkOrderStatus.IN_REPAIR else order.status
                )
            }
            repository.saveWorkOrder(updated, _currentUser.value.name, _currentUser.value.role.label)
        }
    }

    fun updateExpressCheckStatus(orderId: String, checkId: String, status: CheckStatus, notes: String) {
        viewModelScope.launch {
            val order = workOrders.value.find { it.id == orderId } ?: return@launch
            val updatedChecks = order.expressChecks.map {
                if (it.id == checkId) it.copy(status = status, notes = notes) else it
            }
            repository.saveWorkOrder(order.copy(expressChecks = updatedChecks), _currentUser.value.name, _currentUser.value.role.label)
        }
    }

    fun updateQualityCheck(orderId: String, checklist: QualityChecklist, warrantyDays: Int = 30) {
        viewModelScope.launch {
            val order = workOrders.value.find { it.id == orderId } ?: return@launch
            val updated = order.copy(
                qualityChecklist = checklist,
                warrantyDays = warrantyDays,
                status = if (checklist.isFullyCompleted && order.status != WorkOrderStatus.DELIVERED) WorkOrderStatus.READY else order.status
            )
            repository.saveWorkOrder(updated, _currentUser.value.name, _currentUser.value.role.label)
        }
    }

    fun saveClientSignature(orderId: String, signatureBase64: String) {
        viewModelScope.launch {
            val order = workOrders.value.find { it.id == orderId } ?: return@launch
            repository.saveWorkOrder(order.copy(clientSignatureBase64 = signatureBase64), _currentUser.value.name, _currentUser.value.role.label)
        }
    }

    fun addDamagedPhotoTag(orderId: String, photoNote: String) {
        viewModelScope.launch {
            val order = workOrders.value.find { it.id == orderId } ?: return@launch
            val updated = order.copy(photoNotes = order.photoNotes + photoNote)
            repository.saveWorkOrder(updated, _currentUser.value.name, _currentUser.value.role.label)
        }
    }

    fun recordPayment(orderId: String, amount: Double, method: String, isAdvance: Boolean) {
        viewModelScope.launch {
            repository.recordPayment(orderId, amount, method, isAdvance, _currentUser.value.name, _currentUser.value.role.label)
        }
    }

    fun addCashExpense(amount: Double, category: String, description: String, method: String) {
        viewModelScope.launch {
            repository.addCashExpense(amount, category, description, method, _currentUser.value.name, _currentUser.value.role.label)
        }
    }

    fun adjustStock(itemId: String, delta: Int) {
        viewModelScope.launch {
            repository.adjustStock(itemId, delta, _currentUser.value.name, _currentUser.value.role.label)
        }
    }

    fun saveInventoryItem(item: InventoryItem) {
        viewModelScope.launch {
            repository.saveInventoryItem(item, _currentUser.value.name, _currentUser.value.role.label)
        }
    }

    fun saveClient(client: Client) {
        viewModelScope.launch {
            repository.saveClient(client, _currentUser.value.name, _currentUser.value.role.label)
        }
    }

    fun saveMotorcycle(moto: Motorcycle) {
        viewModelScope.launch {
            repository.saveMotorcycle(moto, _currentUser.value.name, _currentUser.value.role.label)
        }
    }

    fun createNewOrder(
        client: Client,
        moto: Motorcycle,
        reportedIssue: String,
        mileage: Int,
        fuelLevel: FuelLevel,
        damages: List<String>,
        signature: String,
        assignedMechanic: AppUser?,
        initialChecks: List<ExpressCheckItem>
    ) {
        viewModelScope.launch {
            repository.saveClient(client, _currentUser.value.name, _currentUser.value.role.label)
            repository.saveMotorcycle(moto, _currentUser.value.name, _currentUser.value.role.label)

            val currentCount = workOrders.value.size
            val nextOrderNumber = "OT-${100 + currentCount + 1}"

            val newOrder = WorkOrder(
                id = nextOrderNumber,
                motorcycleId = moto.id,
                plate = moto.plate,
                motorcycleSummary = "${moto.brand} ${moto.model} ${moto.year} - ${moto.color}",
                clientName = client.name,
                clientPhone = client.phone,
                reportedIssue = reportedIssue,
                initialMileage = mileage,
                fuelLevel = fuelLevel,
                visualDamages = damages,
                clientSignatureBase64 = signature,
                expressChecks = initialChecks,
                status = WorkOrderStatus.RECEIVED,
                assignedMechanicId = assignedMechanic?.id ?: "",
                assignedMechanicName = assignedMechanic?.name ?: "Sin asignar",
                entryTimestamp = System.currentTimeMillis(),
                estimatedDeliveryTimestamp = System.currentTimeMillis() + 86400000L
            )

            repository.saveWorkOrder(newOrder, _currentUser.value.name, _currentUser.value.role.label)
            navigateTo(AppScreen.OrderDetail(newOrder.id))
        }
    }

    // WhatsApp Message Generator
    fun buildWhatsAppMessage(type: String, order: WorkOrder): String {
        val currencyFormat = NumberFormat.getCurrencyInstance(Locale("es", "CO"))
        return when (type) {
            "RECEIVED" -> {
                """
                🏍️ *MotoTaller Pro - Ingreso de Vehículo*
                Hola *${order.clientName}*, confirmamos el ingreso de tu motocicleta:
                📋 *Orden:* #${order.id}
                🛵 *Vehículo:* ${order.plate} (${order.motorcycleSummary})
                ⚠️ *Motivo:* ${order.reportedIssue}
                ⛽ *Combustible:* ${order.fuelLevel.label}
                🔧 *Técnico asignado:* ${order.assignedMechanicName}
                
                Te notificaremos tan pronto finalicemos el chequeo express y tengamos la cotización lista. ¡Gracias por confiar en nosotros!
                """.trimIndent()
            }
            "QUOTE" -> {
                val itemsList = order.quoteItems.joinToString("\n") {
                    "• ${it.description} x${it.quantity}: ${currencyFormat.format(it.subtotal)}"
                }
                """
                🔧 *MotoTaller Pro - Cotización de Servicio*
                Hola *${order.clientName}*, aquí tienes el detalle para tu moto *${order.plate}*:
                📋 *Orden:* #${order.id}
                
                *Ítems presupuestados:*
                $itemsList
                
                💰 *Total Estimado:* ${currencyFormat.format(order.totalApprovedQuote)}
                💵 *Anticipo abonado:* ${currencyFormat.format(order.advancePayment)}
                ⚖️ *Saldo pendiente:* ${currencyFormat.format(order.balanceDue)}
                
                Por favor confírmanos respondiendo a este mensaje para autorizar el inicio del trabajo.
                """.trimIndent()
            }
            "READY" -> {
                """
                ✅ *MotoTaller Pro - ¡Tu Moto Está Lista!*
                Hola *${order.clientName}*, tu motocicleta *${order.plate}* ha superado el control de calidad de 5 puntos y está lista para entrega en nuestro patio.
                
                📋 *Orden:* #${order.id}
                🛡️ *Garantía otorgada:* ${order.warrantyDays} días o ${order.warrantyKm} km
                💵 *Saldo a liquidar:* ${currencyFormat.format(order.balanceDue)}
                💳 *Métodos de pago:* Efectivo, Nequi, Daviplata o Tarjeta
                
                🎥 *Consejos de Cuidado:* Te invitamos a ver nuestra guía rápida de mantenimiento de cadena y frenos en: https://mototaller.pro/consejos
                
                ¡Te esperamos!
                """.trimIndent()
            }
            else -> ""
        }
    }
}
