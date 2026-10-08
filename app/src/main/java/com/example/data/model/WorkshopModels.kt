package com.example.data.model

enum class UserRole(val label: String, val shortRole: String) {
    ADMIN("Administrador", "Admin"),
    RECEPTIONIST("Recepcionista / Asesor", "Recepción"),
    MECHANIC("Mecánico Especialista", "Mecánico"),
    WAREHOUSE("Bodega e Inventario", "Bodega")
}

data class AppUser(
    val id: String,
    val name: String,
    val role: UserRole,
    val email: String = ""
)

enum class WorkOrderStatus(val label: String, val stepIndex: Int) {
    RECEIVED("Recibida", 0),
    DIAGNOSIS("En Diagnóstico", 1),
    WAITING_PARTS("Esperando Repuestos", 2),
    IN_REPAIR("En Reparación", 3),
    READY("Lista", 4),
    DELIVERED("Entregada", 5)
}

enum class FuelLevel(val label: String, val fraction: Float) {
    RESERVE("Reserva (E)", 0.05f),
    QUARTER("1/4 Tanque", 0.25f),
    HALF("1/2 Tanque", 0.50f),
    THREE_QUARTERS("3/4 Tanque", 0.75f),
    FULL("Lleno (F)", 1.0f)
}

enum class CheckStatus {
    OK, REGULAR, BAD
}

data class ExpressCheckItem(
    val id: String,
    val label: String,
    val status: CheckStatus = CheckStatus.OK,
    val notes: String = ""
)

data class QuoteItem(
    val id: String,
    val description: String,
    val type: String, // "REPUESTO", "MANO_OBRA", "COMBO"
    val quantity: Int = 1,
    val unitPrice: Double,
    val isApproved: Boolean = true,
    val isRouteAddition: Boolean = false
) {
    val subtotal: Double get() = quantity * unitPrice
}

data class QualityChecklist(
    val roadTestOk: Boolean = false,
    val boltsTorqued: Boolean = false,
    val cleaned: Boolean = false,
    val tirePressureOk: Boolean = false,
    val oldPartsReturned: Boolean = false
) {
    val isFullyCompleted: Boolean get() = roadTestOk && boltsTorqued && cleaned && tirePressureOk && oldPartsReturned
}

enum class ClientSegment(val label: String) {
    VIP("Cliente VIP"),
    FREQUENT("Cliente Frecuente"),
    OCCASIONAL("Cliente Ocasional")
}

data class Client(
    val id: String,
    val name: String,
    val phone: String,
    val documentNumber: String,
    val address: String = "",
    val email: String = "",
    val segment: ClientSegment = ClientSegment.FREQUENT,
    val notes: String = "",
    val totalVisits: Int = 1
)

data class Motorcycle(
    val id: String,
    val plate: String,
    val brand: String,
    val model: String,
    val year: Int,
    val displacementCc: Int,
    val color: String,
    val vin: String = "",
    val engineNumber: String = "",
    val ownerClientId: String,
    val currentMileage: Int,
    val soatExpiryDate: String, // YYYY-MM-DD
    val tecnoExpiryDate: String, // YYYY-MM-DD
    val lastServiceMileage: Int = 0
)

data class InventoryItem(
    val id: String,
    val sku: String,
    val description: String,
    val brand: String,
    val costPrice: Double,
    val salePrice: Double,
    val stockQuantity: Int,
    val minStock: Int,
    val warehouseLocation: String, // e.g. "Estante B-2"
    val supplier: String,
    val crossCompatibility: String // e.g. "AK125 NKD, Boxer CT100, GN 125"
) {
    val isLowStock: Boolean get() = stockQuantity <= minStock
}

data class WorkOrder(
    val id: String, // "OT-101"
    val motorcycleId: String,
    val plate: String,
    val motorcycleSummary: String,
    val clientName: String,
    val clientPhone: String,
    val reportedIssue: String,
    val initialMileage: Int,
    val fuelLevel: FuelLevel = FuelLevel.HALF,
    val visualDamages: List<String> = emptyList(),
    val clientSignatureBase64: String = "", // Saved signature
    val expressChecks: List<ExpressCheckItem> = emptyList(),
    val status: WorkOrderStatus = WorkOrderStatus.RECEIVED,
    val assignedMechanicId: String = "",
    val assignedMechanicName: String = "Sin asignar",
    val entryTimestamp: Long = System.currentTimeMillis(),
    val estimatedDeliveryTimestamp: Long = System.currentTimeMillis() + 86400000L,
    val actualDeliveryTimestamp: Long? = null,
    val workStartTime: Long? = null,
    val workEndTime: Long? = null,
    val minutesWorked: Int = 0,
    val isTimerRunning: Boolean = false,
    val quoteItems: List<QuoteItem> = emptyList(),
    val advancePayment: Double = 0.0,
    val paidAmount: Double = 0.0,
    val paymentMethod: String = "Efectivo",
    val qualityChecklist: QualityChecklist = QualityChecklist(),
    val warrantyDays: Int = 30,
    val warrantyKm: Int = 1000,
    val photoNotes: List<String> = emptyList() // Damaged parts photos/tags
) {
    val totalApprovedQuote: Double
        get() = quoteItems.filter { it.isApproved }.sumOf { it.subtotal }

    val balanceDue: Double
        get() = (totalApprovedQuote - paidAmount).coerceAtLeast(0.0)

    val isFullyPaid: Boolean
        get() = balanceDue <= 0.0 && totalApprovedQuote > 0
}

enum class CashEntryType {
    INCOME, EXPENSE
}

data class CashEntry(
    val id: String,
    val timestamp: Long,
    val type: CashEntryType,
    val category: String, // "Anticipo", "Liquidación Orden", "Insumos", "Repuestos urgentes", "Gasolina pruebas"
    val amount: Double,
    val paymentMethod: String, // "Efectivo", "Nequi", "Daviplata", "Tarjeta"
    val relatedOrderId: String? = null,
    val description: String,
    val registeredBy: String
)

data class AuditLog(
    val id: String,
    val timestamp: Long,
    val userName: String,
    val userRole: String,
    val actionDescription: String,
    val entityType: String,
    val entityId: String
)
