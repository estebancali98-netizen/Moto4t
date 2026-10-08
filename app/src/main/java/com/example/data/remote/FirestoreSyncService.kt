package com.example.data.remote

import android.content.Context
import android.util.Log
import com.example.R
import com.example.data.model.InventoryItem
import com.example.data.model.WorkOrder
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class FirestoreSyncService(private val context: Context) {

    private val db: FirebaseFirestore by lazy {
        val dbId = context.getString(R.string.firestore_database_id)
        FirebaseFirestore.getInstance(dbId)
    }

    private val auth by lazy { Firebase.auth }

    val currentUserId: String?
        get() = auth.currentUser?.uid

    val isAuthenticated: Boolean
        get() = auth.currentUser != null

    // Real-time observation of Work Orders collection (Kanban live stream)
    fun observeWorkOrdersRealtime(): Flow<List<WorkOrder>> = callbackFlow {
        if (!isAuthenticated) {
            close()
            return@callbackFlow
        }

        val registration: ListenerRegistration = db.collection("work_orders")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w("FirestoreSync", "Error en listener de work_orders", error)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val orders = snapshot.documents.mapNotNull { doc ->
                        try {
                            val data = doc.data ?: return@mapNotNull null
                            WorkOrder(
                                id = doc.id,
                                motorcycleId = data["motorcycleId"] as? String ?: "",
                                plate = data["plate"] as? String ?: "",
                                motorcycleSummary = data["motorcycleSummary"] as? String ?: "",
                                clientName = data["clientName"] as? String ?: "",
                                clientPhone = data["clientPhone"] as? String ?: "",
                                reportedIssue = data["reportedIssue"] as? String ?: "",
                                initialMileage = (data["initialMileage"] as? Number)?.toInt() ?: 0,
                                entryTimestamp = (data["entryTimestamp"] as? Number)?.toLong() ?: System.currentTimeMillis()
                            )
                        } catch (e: Exception) {
                            Log.e("FirestoreSync", "Error parseando orden ${doc.id}", e)
                            null
                        }
                    }
                    trySend(orders)
                }
            }

        awaitClose { registration.remove() }
    }

    // Sync a single work order up to Firestore
    fun pushWorkOrder(order: WorkOrder, onError: (String) -> Unit = {}) {
        // Guard: Only sync to Firestore if the user is authenticated
        if (!isAuthenticated) {
            return
        }

        val uid = currentUserId ?: return
        val payload = hashMapOf<String, Any>(
            "id" to order.id,
            "plate" to order.plate,
            "motorcycleSummary" to order.motorcycleSummary,
            "clientName" to order.clientName,
            "clientPhone" to order.clientPhone,
            "reportedIssue" to order.reportedIssue,
            "status" to order.status.name,
            "assignedMechanicName" to order.assignedMechanicName,
            "initialMileage" to order.initialMileage,
            "fuelLevel" to order.fuelLevel.name,
            "totalApprovedQuote" to order.totalApprovedQuote,
            "advancePayment" to order.advancePayment,
            "paidAmount" to order.paidAmount,
            "balanceDue" to order.balanceDue,
            "minutesWorked" to order.minutesWorked,
            "entryTimestamp" to order.entryTimestamp,
            "userId" to uid,
            "updatedAt" to FieldValue.serverTimestamp()
        )

        db.collection("work_orders").document(order.id)
            .set(payload, SetOptions.merge())
            .addOnFailureListener { e ->
                Log.e("FirestoreSync", "Error subiendo orden a Firestore", e)
                onError(e.localizedMessage ?: "Error de sincronización")
            }
    }

    // Sync inventory item up to Firestore
    fun pushInventoryItem(item: InventoryItem, onError: (String) -> Unit = {}) {
        // Guard: Only sync to Firestore if the user is authenticated
        if (!isAuthenticated) {
            return
        }

        val uid = currentUserId ?: return
        val payload = hashMapOf<String, Any>(
            "id" to item.id,
            "sku" to item.sku,
            "description" to item.description,
            "brand" to item.brand,
            "costPrice" to item.costPrice,
            "salePrice" to item.salePrice,
            "stockQuantity" to item.stockQuantity,
            "minStock" to item.minStock,
            "warehouseLocation" to item.warehouseLocation,
            "supplier" to item.supplier,
            "crossCompatibility" to item.crossCompatibility,
            "userId" to uid,
            "updatedAt" to FieldValue.serverTimestamp()
        )

        db.collection("inventory").document(item.id)
            .set(payload, SetOptions.merge())
            .addOnFailureListener { e ->
                Log.e("FirestoreSync", "Error subiendo repuesto a Firestore", e)
                onError(e.localizedMessage ?: "Error de sincronización")
            }
    }

    // Bulk upload active orders and inventory once signed in
    fun syncAll(orders: List<WorkOrder>, inventory: List<InventoryItem>) {
        if (!isAuthenticated) return
        orders.forEach { pushWorkOrder(it) }
        inventory.forEach { pushInventoryItem(it) }
    }
}
