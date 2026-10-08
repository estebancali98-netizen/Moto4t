package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.data.model.Client
import com.example.data.model.ClientSegment
import com.example.data.model.ExpressCheckItem
import com.example.data.model.FuelLevel
import com.example.data.model.InventoryItem
import com.example.data.model.Motorcycle
import com.example.data.model.QualityChecklist
import com.example.data.model.QuoteItem
import com.example.data.model.WorkOrder
import com.example.data.model.WorkOrderStatus
import com.example.data.model.CashEntry
import com.example.data.model.CashEntryType
import com.example.data.model.AuditLog

@Entity(tableName = "clients")
data class ClientEntity(
    @PrimaryKey val id: String,
    val name: String,
    val phone: String,
    val documentNumber: String,
    val address: String,
    val email: String,
    val segment: String,
    val notes: String,
    val totalVisits: Int
) {
    fun toDomain() = Client(
        id = id,
        name = name,
        phone = phone,
        documentNumber = documentNumber,
        address = address,
        email = email,
        segment = try { ClientSegment.valueOf(segment) } catch (_: Exception) { ClientSegment.FREQUENT },
        notes = notes,
        totalVisits = totalVisits
    )

    companion object {
        fun fromDomain(c: Client) = ClientEntity(
            id = c.id,
            name = c.name,
            phone = c.phone,
            documentNumber = c.documentNumber,
            address = c.address,
            email = c.email,
            segment = c.segment.name,
            notes = c.notes,
            totalVisits = c.totalVisits
        )
    }
}

@Entity(tableName = "motorcycles")
data class MotorcycleEntity(
    @PrimaryKey val id: String,
    val plate: String,
    val brand: String,
    val model: String,
    val year: Int,
    val displacementCc: Int,
    val color: String,
    val vin: String,
    val engineNumber: String,
    val ownerClientId: String,
    val currentMileage: Int,
    val soatExpiryDate: String,
    val tecnoExpiryDate: String,
    val lastServiceMileage: Int
) {
    fun toDomain() = Motorcycle(
        id = id,
        plate = plate,
        brand = brand,
        model = model,
        year = year,
        displacementCc = displacementCc,
        color = color,
        vin = vin,
        engineNumber = engineNumber,
        ownerClientId = ownerClientId,
        currentMileage = currentMileage,
        soatExpiryDate = soatExpiryDate,
        tecnoExpiryDate = tecnoExpiryDate,
        lastServiceMileage = lastServiceMileage
    )

    companion object {
        fun fromDomain(m: Motorcycle) = MotorcycleEntity(
            id = m.id,
            plate = m.plate,
            brand = m.brand,
            model = m.model,
            year = m.year,
            displacementCc = m.displacementCc,
            color = m.color,
            vin = m.vin,
            engineNumber = m.engineNumber,
            ownerClientId = m.ownerClientId,
            currentMileage = m.currentMileage,
            soatExpiryDate = m.soatExpiryDate,
            tecnoExpiryDate = m.tecnoExpiryDate,
            lastServiceMileage = m.lastServiceMileage
        )
    }
}

@Entity(tableName = "inventory")
data class InventoryEntity(
    @PrimaryKey val id: String,
    val sku: String,
    val description: String,
    val brand: String,
    val costPrice: Double,
    val salePrice: Double,
    val stockQuantity: Int,
    val minStock: Int,
    val warehouseLocation: String,
    val supplier: String,
    val crossCompatibility: String
) {
    fun toDomain() = InventoryItem(
        id = id,
        sku = sku,
        description = description,
        brand = brand,
        costPrice = costPrice,
        salePrice = salePrice,
        stockQuantity = stockQuantity,
        minStock = minStock,
        warehouseLocation = warehouseLocation,
        supplier = supplier,
        crossCompatibility = crossCompatibility
    )

    companion object {
        fun fromDomain(i: InventoryItem) = InventoryEntity(
            id = i.id,
            sku = i.sku,
            description = i.description,
            brand = i.brand,
            costPrice = i.costPrice,
            salePrice = i.salePrice,
            stockQuantity = i.stockQuantity,
            minStock = i.minStock,
            warehouseLocation = i.warehouseLocation,
            supplier = i.supplier,
            crossCompatibility = i.crossCompatibility
        )
    }
}

@Entity(tableName = "work_orders")
data class WorkOrderEntity(
    @PrimaryKey val id: String,
    val motorcycleId: String,
    val plate: String,
    val motorcycleSummary: String,
    val clientName: String,
    val clientPhone: String,
    val reportedIssue: String,
    val initialMileage: Int,
    val fuelLevel: String,
    val visualDamagesJson: String,
    val clientSignatureBase64: String,
    val expressChecksJson: String,
    val status: String,
    val assignedMechanicId: String,
    val assignedMechanicName: String,
    val entryTimestamp: Long,
    val estimatedDeliveryTimestamp: Long,
    val actualDeliveryTimestamp: Long?,
    val workStartTime: Long?,
    val workEndTime: Long?,
    val minutesWorked: Int,
    val isTimerRunning: Boolean,
    val quoteItemsJson: String,
    val advancePayment: Double,
    val paidAmount: Double,
    val paymentMethod: String,
    val qualityChecklistJson: String,
    val warrantyDays: Int,
    val warrantyKm: Int,
    val photoNotesJson: String
)

@Entity(tableName = "cash_entries")
data class CashEntryEntity(
    @PrimaryKey val id: String,
    val timestamp: Long,
    val type: String,
    val category: String,
    val amount: Double,
    val paymentMethod: String,
    val relatedOrderId: String?,
    val description: String,
    val registeredBy: String
) {
    fun toDomain() = CashEntry(
        id = id,
        timestamp = timestamp,
        type = try { CashEntryType.valueOf(type) } catch (_: Exception) { CashEntryType.INCOME },
        category = category,
        amount = amount,
        paymentMethod = paymentMethod,
        relatedOrderId = relatedOrderId,
        description = description,
        registeredBy = registeredBy
    )

    companion object {
        fun fromDomain(e: CashEntry) = CashEntryEntity(
            id = e.id,
            timestamp = e.timestamp,
            type = e.type.name,
            category = e.category,
            amount = e.amount,
            paymentMethod = e.paymentMethod,
            relatedOrderId = e.relatedOrderId,
            description = e.description,
            registeredBy = e.registeredBy
        )
    }
}

@Entity(tableName = "audit_logs")
data class AuditLogEntity(
    @PrimaryKey val id: String,
    val timestamp: Long,
    val userName: String,
    val userRole: String,
    val actionDescription: String,
    val entityType: String,
    val entityId: String
) {
    fun toDomain() = AuditLog(
        id = id,
        timestamp = timestamp,
        userName = userName,
        userRole = userRole,
        actionDescription = actionDescription,
        entityType = entityType,
        entityId = entityId
    )

    companion object {
        fun fromDomain(a: AuditLog) = AuditLogEntity(
            id = a.id,
            timestamp = a.timestamp,
            userName = a.userName,
            userRole = a.userRole,
            actionDescription = a.actionDescription,
            entityType = a.entityType,
            entityId = a.entityId
        )
    }
}
