package com.example.data.repository

import com.example.data.local.AuditLogEntity
import com.example.data.local.CashEntryEntity
import com.example.data.local.ClientEntity
import com.example.data.local.InventoryEntity
import com.example.data.local.MotorcycleEntity
import com.example.data.local.WorkshopConverters
import com.example.data.local.WorkshopDatabase
import com.example.data.model.AuditLog
import com.example.data.model.CashEntry
import com.example.data.model.CashEntryType
import com.example.data.model.CheckStatus
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class WorkshopRepository(
    private val db: WorkshopDatabase,
    private val firestoreSync: com.example.data.remote.FirestoreSyncService? = null
) {

    private val clientDao = db.clientDao()
    private val motorcycleDao = db.motorcycleDao()
    private val inventoryDao = db.inventoryDao()
    private val workOrderDao = db.workOrderDao()
    private val cashEntryDao = db.cashEntryDao()
    private val auditLogDao = db.auditLogDao()

    val clients: Flow<List<Client>> = clientDao.getAllClients().map { list ->
        list.map { it.toDomain() }
    }

    val motorcycles: Flow<List<Motorcycle>> = motorcycleDao.getAllMotorcycles().map { list ->
        list.map { it.toDomain() }
    }

    val inventory: Flow<List<InventoryItem>> = inventoryDao.getAllInventory().map { list ->
        list.map { it.toDomain() }
    }

    val workOrders: Flow<List<WorkOrder>> = workOrderDao.getAllOrders().map { list ->
        list.map { WorkshopConverters.entityToWorkOrder(it) }
    }

    val cashEntries: Flow<List<CashEntry>> = cashEntryDao.getAllCashEntries().map { list ->
        list.map { it.toDomain() }
    }

    val auditLogs: Flow<List<AuditLog>> = auditLogDao.getAllAuditLogs().map { list ->
        list.map { it.toDomain() }
    }

    suspend fun logAction(userName: String, userRole: String, action: String, entityType: String, entityId: String) {
        val log = AuditLogEntity(
            id = "log_${System.currentTimeMillis()}_${(100..999).random()}",
            timestamp = System.currentTimeMillis(),
            userName = userName,
            userRole = userRole,
            actionDescription = action,
            entityType = entityType,
            entityId = entityId
        )
        auditLogDao.insertLog(log)
    }

    suspend fun saveWorkOrder(order: WorkOrder, userName: String = "Sistema", userRole: String = "Admin") {
        withContext(Dispatchers.IO) {
            val entity = WorkshopConverters.workOrderToEntity(order)
            workOrderDao.insertOrder(entity)
            firestoreSync?.pushWorkOrder(order)
            logAction(userName, userRole, "Actualizó Orden ${order.id} (Estado: ${order.status.label})", "WorkOrder", order.id)
        }
    }

    suspend fun updateOrderStatus(orderId: String, newStatus: WorkOrderStatus, userName: String, userRole: String) {
        withContext(Dispatchers.IO) {
            val currentOrders = workOrders.first()
            val existing = currentOrders.find { it.id == orderId } ?: return@withContext
            val updated = existing.copy(
                status = newStatus,
                actualDeliveryTimestamp = if (newStatus == WorkOrderStatus.DELIVERED) System.currentTimeMillis() else existing.actualDeliveryTimestamp
            )
            workOrderDao.insertOrder(WorkshopConverters.workOrderToEntity(updated))
            logAction(userName, userRole, "Cambió estado de ${existing.id} a ${newStatus.label}", "WorkOrder", orderId)
        }
    }

    suspend fun addQuoteItem(orderId: String, item: QuoteItem, userName: String, userRole: String) {
        withContext(Dispatchers.IO) {
            val currentOrders = workOrders.first()
            val existing = currentOrders.find { it.id == orderId } ?: return@withContext
            val updatedList = existing.quoteItems + item
            val updated = existing.copy(quoteItems = updatedList)
            workOrderDao.insertOrder(WorkshopConverters.workOrderToEntity(updated))
            logAction(userName, userRole, "Agregó ítem a cotización: ${item.description} ($${item.unitPrice})", "WorkOrder", orderId)
        }
    }

    suspend fun toggleItemApproval(orderId: String, itemId: String, approved: Boolean, userName: String, userRole: String) {
        withContext(Dispatchers.IO) {
            val currentOrders = workOrders.first()
            val existing = currentOrders.find { it.id == orderId } ?: return@withContext
            val updatedList = existing.quoteItems.map {
                if (it.id == itemId) it.copy(isApproved = approved) else it
            }
            val updated = existing.copy(quoteItems = updatedList)
            workOrderDao.insertOrder(WorkshopConverters.workOrderToEntity(updated))
            logAction(userName, userRole, "${if (approved) "Aprobó" else "Desaprobó"} ítem $itemId en ${existing.id}", "WorkOrder", orderId)
        }
    }

    suspend fun recordPayment(
        orderId: String,
        amount: Double,
        method: String,
        isAdvance: Boolean,
        userName: String,
        userRole: String
    ) {
        withContext(Dispatchers.IO) {
            val currentOrders = workOrders.first()
            val existing = currentOrders.find { it.id == orderId } ?: return@withContext
            val newPaid = existing.paidAmount + amount
            val newAdvance = if (isAdvance) existing.advancePayment + amount else existing.advancePayment
            val updated = existing.copy(
                paidAmount = newPaid,
                advancePayment = newAdvance,
                paymentMethod = method
            )
            workOrderDao.insertOrder(WorkshopConverters.workOrderToEntity(updated))

            val cashEntry = CashEntryEntity(
                id = "cash_${System.currentTimeMillis()}",
                timestamp = System.currentTimeMillis(),
                type = CashEntryType.INCOME.name,
                category = if (isAdvance) "Anticipo Orden" else "Liquidación de Saldo",
                amount = amount,
                paymentMethod = method,
                relatedOrderId = orderId,
                description = "Pago para orden $orderId ($method)",
                registeredBy = userName
            )
            cashEntryDao.insertEntry(cashEntry)
            logAction(userName, userRole, "Registró pago de $${amount.toInt()} para $orderId con $method", "Payment", orderId)
        }
    }

    suspend fun addCashExpense(
        amount: Double,
        category: String,
        description: String,
        paymentMethod: String,
        userName: String,
        userRole: String
    ) {
        withContext(Dispatchers.IO) {
            val entry = CashEntryEntity(
                id = "cash_${System.currentTimeMillis()}",
                timestamp = System.currentTimeMillis(),
                type = CashEntryType.EXPENSE.name,
                category = category,
                amount = amount,
                paymentMethod = paymentMethod,
                relatedOrderId = null,
                description = description,
                registeredBy = userName
            )
            cashEntryDao.insertEntry(entry)
            logAction(userName, userRole, "Registró gasto de caja: $description ($${amount.toInt()})", "Cash", entry.id)
        }
    }

    suspend fun saveInventoryItem(item: InventoryItem, userName: String, userRole: String) {
        withContext(Dispatchers.IO) {
            inventoryDao.insertItem(InventoryEntity.fromDomain(item))
            firestoreSync?.pushInventoryItem(item)
            logAction(userName, userRole, "Guardó repuesto: ${item.description} (Stock: ${item.stockQuantity})", "Inventory", item.id)
        }
    }

    suspend fun adjustStock(itemId: String, delta: Int, userName: String, userRole: String) {
        withContext(Dispatchers.IO) {
            if (delta > 0) {
                inventoryDao.incrementStock(itemId, delta)
            } else {
                inventoryDao.decrementStock(itemId, -delta)
            }
            logAction(userName, userRole, "Ajustó stock de $itemId en $delta unidades", "Inventory", itemId)
        }
    }

    suspend fun saveClient(client: Client, userName: String, userRole: String) {
        withContext(Dispatchers.IO) {
            clientDao.insertClient(ClientEntity.fromDomain(client))
            logAction(userName, userRole, "Guardó cliente ${client.name}", "Client", client.id)
        }
    }

    suspend fun saveMotorcycle(moto: Motorcycle, userName: String, userRole: String) {
        withContext(Dispatchers.IO) {
            motorcycleDao.insertMotorcycle(MotorcycleEntity.fromDomain(moto))
            logAction(userName, userRole, "Guardó moto ${moto.plate} - ${moto.brand} ${moto.model}", "Motorcycle", moto.id)
        }
    }

    suspend fun seedInitialDataIfEmpty() {
        withContext(Dispatchers.IO) {
            val existingClients = clientDao.getAllClients().first()
            if (existingClients.isNotEmpty()) return@withContext

            // 1. Seed Clients
            val initialClients = listOf(
                Client("cli_1", "Carlos Andrés Gómez", "3124567890", "1098765432", "Cra 45 # 22-10", "carlos.gomez@email.com", ClientSegment.VIP, "Siempre pide aceite sintético 10W-40", 7),
                Client("cli_2", "Mariana Valencia", "3159876543", "1128456789", "Calle 12 # 5-40", "mariana.v@email.com", ClientSegment.FREQUENT, "Revisión constante de frenos", 4),
                Client("cli_3", "Sebastián Roa", "3201234567", "1015678901", "Av 6 Norte # 34-12", "s.roa@email.com", ClientSegment.OCCASIONAL, "Cliente nuevo por recomendación", 1),
                Client("cli_4", "Fabián Rodríguez", "3186543210", "1087654321", "Cra 15 # 100-20", "fabian.rod@email.com", ClientSegment.FREQUENT, "Mensajero urbano de alta rotación", 12),
                Client("cli_5", "Paola Ortiz", "3103456789", "1023456789", "Diag 40 # 18-05", "paola.o@email.com", ClientSegment.VIP, "Muy puntual con mantenimiento preventivo", 6)
            )
            initialClients.forEach { clientDao.insertClient(ClientEntity.fromDomain(it)) }

            // 2. Seed Motorcycles (Including high-rotation popular bikes requested: AK125 NKD, Suzuki GN 125, Bajaj CT100, NMAX 155, XTZ 150)
            val initialBikes = listOf(
                Motorcycle("moto_1", "ABC-12D", "AKT", "AK125 NKD", 2023, 125, "Negro Mate", "9F6NKD125P001", "NKD-88231", "cli_1", 18500, "2026-10-25", "2026-11-15", 15000),
                Motorcycle("moto_2", "XYZ-89E", "Yamaha", "NMAX 155", 2024, 155, "Gris Grafito", "JYAR33800R002", "G3J-44120", "cli_2", 8200, "2027-02-10", "2027-02-10", 6000),
                Motorcycle("moto_3", "KJH-45F", "Suzuki", "GN 125", 2022, 125, "Azul Clásico", "9F4GN125N003", "GN-99014", "cli_3", 34200, "2026-10-12", "2026-10-20", 30000), // SOAT due soon!
                Motorcycle("moto_4", "POU-67G", "Bajaj", "Boxer CT100", 2023, 100, "Rojo Pasión", "MD2BAJ100P004", "BM-10294", "cli_4", 42000, "2026-11-05", "2026-11-05", 39000),
                Motorcycle("moto_5", "MNO-34H", "Yamaha", "XTZ 150", 2023, 150, "Blanco / Azul", "JYAR55100P005", "XT-55410", "cli_5", 14300, "2027-04-18", "2027-04-18", 12000)
            )
            initialBikes.forEach { motorcycleDao.insertMotorcycle(MotorcycleEntity.fromDomain(it)) }

            // 3. Seed Inventory with cross-compatibility and locations
            val initialInventory = listOf(
                InventoryItem("inv_1", "ACE-MOT-5100", "Aceite Motul 5100 15W-50 4T (1L)", "Motul", 32000.0, 48000.0, 14, 5, "Estante A-1", "Distribuidora Motos del Valle", "Universal 4T / Todas las marcas"),
                InventoryItem("inv_2", "ACE-YAM-LUBE", "Aceite Yamalube 10W-40 Semisintético (1L)", "Yamalube", 28000.0, 42000.0, 3, 6, "Estante A-2", "Importadora Yamaha SAS", "NMAX, XTZ 150, FZ 2.0"), // Low stock alert!
                InventoryItem("inv_3", "KIT-ARR-NKD", "Kit de Arrastre Reforzado 428H", "Choho", 45000.0, 75000.0, 2, 4, "Estante B-1", "Repuestos del Centro", "AK125 NKD, Suzuki GN 125, Boxer CT100"), // Low stock alert!
                InventoryItem("inv_4", "PAS-FRE-YAM", "Pastillas de Freno Cerámicas Delanteras", "Ichiban", 18000.0, 32000.0, 8, 3, "Estante C-3", "Frenos Colombia", "NMAX 155, XTZ 150, Pulsar NS 200"),
                InventoryItem("inv_5", "BAN-FRE-NKD", "Bandas de Freno Traseras", "Replay", 12000.0, 24000.0, 12, 4, "Estante C-4", "Frenos Colombia", "AK125 NKD, Boxer CT100, GN 125"),
                InventoryItem("inv_6", "BUJ-NGK-CPR8", "Bujía NGK CPR8EA-9", "NGK", 10000.0, 18000.0, 15, 5, "Cajonera E-1", "Eléctricos & Bujías SAS", "Yamaha NMAX 155, XTZ 150"),
                InventoryItem("inv_7", "FIL-AIR-NKD", "Filtro de Aire Espuma Alto Flujo", "AKT Parts", 8000.0, 16000.0, 9, 3, "Estante D-2", "AKT Colombia", "AK125 NKD, AK125 SL"),
                InventoryItem("inv_8", "GUA-EMB-GN125", "Guaya de Embrague Original", "Suzuki", 14000.0, 26000.0, 5, 2, "Estante D-5", "Suzuki Motors", "Suzuki GN 125, GS 125"),
                InventoryItem("inv_9", "LIQ-FRE-DOT4", "Líquido de Frenos DOT 4 Bosch (250ml)", "Bosch", 9000.0, 16000.0, 7, 3, "Estante A-4", "Químicos AutoMoto", "Universal Disco"),
                InventoryItem("inv_10", "FIL-ACE-XTZ", "Filtro de Aceite de Papel", "Yamaha", 11000.0, 22000.0, 6, 3, "Estante A-3", "Importadora Yamaha SAS", "Yamaha XTZ 150, FZ16, YBR 125")
            )
            initialInventory.forEach { inventoryDao.insertItem(InventoryEntity.fromDomain(it)) }

            // 4. Seed Standard 10-point Express Check Generator
            fun defaultChecks(): List<ExpressCheckItem> = listOf(
                ExpressCheckItem("chk_1", "Luces y Direccionales", CheckStatus.OK, "Bombillos en buen estado"),
                ExpressCheckItem("chk_2", "Frenos Delantero / Trasero", CheckStatus.REGULAR, "Pastillas a media vida"),
                ExpressCheckItem("chk_3", "Nivel de Aceite y Fugas", CheckStatus.BAD, "Aceite quemado, requiere cambio"),
                ExpressCheckItem("chk_4", "Presión y Estado de Llantas", CheckStatus.OK, "28 PSI del / 32 PSI tras"),
                ExpressCheckItem("chk_5", "Kit de Arrastre (Tensión y Dientes)", CheckStatus.REGULAR, "Cadena destensada"),
                ExpressCheckItem("chk_6", "Suspensión y Retenedores", CheckStatus.OK, "Barras secas sin fugas"),
                ExpressCheckItem("chk_7", "Batería y Sistema de Carga", CheckStatus.OK, "12.6V en reposo"),
                ExpressCheckItem("chk_8", "Dirección y Cunas", CheckStatus.OK, "Giro suave sin juego"),
                ExpressCheckItem("chk_9", "Mandos, Guayas y Acelerador", CheckStatus.OK, "Retorno rápido"),
                ExpressCheckItem("chk_10", "Espejos y Bocina / Pito", CheckStatus.OK, "Funcionando normal")
            )

            // 5. Seed Work Orders representing each Kanban stage
            val initialOrders = listOf(
                WorkOrder(
                    id = "OT-101",
                    motorcycleId = "moto_1",
                    plate = "ABC-12D",
                    motorcycleSummary = "AKT AK125 NKD 2023 - Negro Mate",
                    clientName = "Carlos Andrés Gómez",
                    clientPhone = "3124567890",
                    reportedIssue = "Mantenimiento general preventivo. Ruido metálico al acelerar en 2da y pérdida de fuerza.",
                    initialMileage = 18500,
                    fuelLevel = FuelLevel.HALF,
                    visualDamages = listOf("Rayón leve en guardabarro delantero", "Tapa lateral izquierda con desgaste de pintura"),
                    clientSignatureBase64 = "FIRMA_REGISTRADA_OK",
                    expressChecks = defaultChecks(),
                    status = WorkOrderStatus.IN_REPAIR,
                    assignedMechanicId = "mech_1",
                    assignedMechanicName = "Carlos Mecánico",
                    entryTimestamp = System.currentTimeMillis() - 7200000L,
                    estimatedDeliveryTimestamp = System.currentTimeMillis() + 14400000L,
                    minutesWorked = 45,
                    isTimerRunning = true,
                    quoteItems = listOf(
                        QuoteItem("q_1", "Aceite Motul 5100 15W-50 4T", "REPUESTO", 1, 48000.0, isApproved = true),
                        QuoteItem("q_2", "Kit de Arrastre Reforzado 428H Choho", "REPUESTO", 1, 75000.0, isApproved = true),
                        QuoteItem("q_3", "Mano de obra Sincronización + Cambio Arrastre", "MANO_OBRA", 1, 55000.0, isApproved = true),
                        QuoteItem("q_4", "Filtro de Aire Alto Flujo AKT", "REPUESTO", 1, 16000.0, isApproved = true, isRouteAddition = true)
                    ),
                    advancePayment = 80000.0,
                    paidAmount = 80000.0,
                    paymentMethod = "Nequi",
                    photoNotes = listOf("Cadena estirada con desgaste irregular", "Bujía con carbonilla")
                ),
                WorkOrder(
                    id = "OT-102",
                    motorcycleId = "moto_2",
                    plate = "XYZ-89E",
                    motorcycleSummary = "Yamaha NMAX 155 2024 - Gris Grafito",
                    clientName = "Mariana Valencia",
                    clientPhone = "3159876543",
                    reportedIssue = "Vibración en la transmisión al arrancar en frío y chirrido en freno delantero.",
                    initialMileage = 8200,
                    fuelLevel = FuelLevel.THREE_QUARTERS,
                    visualDamages = listOf("Cúpula con pequeña marca de piedra"),
                    clientSignatureBase64 = "FIRMA_REGISTRADA_OK",
                    expressChecks = defaultChecks(),
                    status = WorkOrderStatus.WAITING_PARTS,
                    assignedMechanicId = "mech_2",
                    assignedMechanicName = "Mateo Especialista",
                    entryTimestamp = System.currentTimeMillis() - 14400000L,
                    estimatedDeliveryTimestamp = System.currentTimeMillis() + 28800000L,
                    minutesWorked = 25,
                    isTimerRunning = false,
                    quoteItems = listOf(
                        QuoteItem("q_5", "Mantenimiento y desengrase de CVT + Rodillos", "MANO_OBRA", 1, 45000.0, isApproved = true),
                        QuoteItem("q_6", "Pastillas de Freno Cerámicas Ichiban", "REPUESTO", 1, 32000.0, isApproved = true),
                        QuoteItem("q_7", "Líquido de Frenos DOT 4 Bosch", "REPUESTO", 1, 16000.0, isApproved = true)
                    ),
                    advancePayment = 40000.0,
                    paidAmount = 40000.0,
                    paymentMethod = "Daviplata",
                    photoNotes = listOf("Campana CVT con acumulación de polvo de correa")
                ),
                WorkOrder(
                    id = "OT-103",
                    motorcycleId = "moto_3",
                    plate = "KJH-45F",
                    motorcycleSummary = "Suzuki GN 125 2022 - Azul Clásico",
                    clientName = "Sebastián Roa",
                    clientPhone = "3201234567",
                    reportedIssue = "La moto se apaga en semáforos, humo negro en escape y clutch muy duro.",
                    initialMileage = 34200,
                    fuelLevel = FuelLevel.QUARTER,
                    visualDamages = listOf("Tanque con pequeño hundimiento lateral derecho"),
                    clientSignatureBase64 = "FIRMA_REGISTRADA_OK",
                    expressChecks = defaultChecks(),
                    status = WorkOrderStatus.DIAGNOSIS,
                    assignedMechanicId = "mech_1",
                    assignedMechanicName = "Carlos Mecánico",
                    entryTimestamp = System.currentTimeMillis() - 3600000L,
                    estimatedDeliveryTimestamp = System.currentTimeMillis() + 18000000L,
                    minutesWorked = 15,
                    isTimerRunning = false,
                    quoteItems = listOf(
                        QuoteItem("q_8", "Limpieza de Carburador ultrasonido + Calibración", "MANO_OBRA", 1, 35000.0, isApproved = true),
                        QuoteItem("q_9", "Guaya de Embrague Original Suzuki", "REPUESTO", 1, 26000.0, isApproved = true),
                        QuoteItem("q_10", "Bujía NGK CPR8EA-9", "REPUESTO", 1, 18000.0, isApproved = false) // Pending customer authorization
                    ),
                    advancePayment = 0.0,
                    paidAmount = 0.0,
                    paymentMethod = "Efectivo"
                ),
                WorkOrder(
                    id = "OT-104",
                    motorcycleId = "moto_4",
                    plate = "POU-67G",
                    motorcycleSummary = "Bajaj Boxer CT100 2023 - Rojo Pasión",
                    clientName = "Fabián Rodríguez",
                    clientPhone = "3186543210",
                    reportedIssue = "Cambio de aceite express y calibración de bandas traseras para turno de entrega.",
                    initialMileage = 42000,
                    fuelLevel = FuelLevel.FULL,
                    visualDamages = emptyList(),
                    clientSignatureBase64 = "FIRMA_REGISTRADA_OK",
                    expressChecks = defaultChecks(),
                    status = WorkOrderStatus.READY,
                    assignedMechanicId = "mech_2",
                    assignedMechanicName = "Mateo Especialista",
                    entryTimestamp = System.currentTimeMillis() - 10800000L,
                    estimatedDeliveryTimestamp = System.currentTimeMillis() - 1800000L,
                    actualDeliveryTimestamp = null,
                    minutesWorked = 30,
                    isTimerRunning = false,
                    quoteItems = listOf(
                        QuoteItem("q_11", "Aceite Motul 5100 15W-50 4T", "REPUESTO", 1, 48000.0, isApproved = true),
                        QuoteItem("q_12", "Bandas de Freno Traseras Replay", "REPUESTO", 1, 24000.0, isApproved = true),
                        QuoteItem("q_13", "Mano de Obra Cambio y Tensión", "MANO_OBRA", 1, 20000.0, isApproved = true)
                    ),
                    advancePayment = 0.0,
                    paidAmount = 0.0, // Unpaid ready order alert!
                    paymentMethod = "Efectivo",
                    qualityChecklist = QualityChecklist(
                        roadTestOk = true,
                        boltsTorqued = true,
                        cleaned = true,
                        tirePressureOk = true,
                        oldPartsReturned = true
                    ),
                    warrantyDays = 30,
                    warrantyKm = 1000
                ),
                WorkOrder(
                    id = "OT-105",
                    motorcycleId = "moto_5",
                    plate = "MNO-34H",
                    motorcycleSummary = "Yamaha XTZ 150 2023 - Blanco / Azul",
                    clientName = "Paola Ortiz",
                    clientPhone = "3103456789",
                    reportedIssue = "Recepción inicial para revisión de suspensión delantera tras viaje largo por trocha.",
                    initialMileage = 14300,
                    fuelLevel = FuelLevel.HALF,
                    visualDamages = listOf("Calcas despegadas en guardabarro"),
                    clientSignatureBase64 = "FIRMA_REGISTRADA_OK",
                    expressChecks = defaultChecks(),
                    status = WorkOrderStatus.RECEIVED,
                    assignedMechanicId = "",
                    assignedMechanicName = "Sin asignar",
                    entryTimestamp = System.currentTimeMillis() - 1800000L,
                    estimatedDeliveryTimestamp = System.currentTimeMillis() + 86400000L,
                    quoteItems = emptyList()
                )
            )
            initialOrders.forEach { workOrderDao.insertOrder(WorkshopConverters.workOrderToEntity(it)) }

            // 6. Seed Cash Register Entries
            val initialCash = listOf(
                CashEntryEntity("c_1", System.currentTimeMillis() - 28800000L, CashEntryType.INCOME.name, "Apertura de Caja", 250000.0, "Efectivo", null, "Base diaria de cambio en mostrador", "Admin"),
                CashEntryEntity("c_2", System.currentTimeMillis() - 21600000L, CashEntryType.INCOME.name, "Anticipo Orden", 80000.0, "Nequi", "OT-101", "Anticipo de repuestos para OT-101", "Recepción"),
                CashEntryEntity("c_3", System.currentTimeMillis() - 14400000L, CashEntryType.INCOME.name, "Anticipo Orden", 40000.0, "Daviplata", "OT-102", "Anticipo Mariana Valencia", "Recepción"),
                CashEntryEntity("c_4", System.currentTimeMillis() - 10800000L, CashEntryType.EXPENSE.name, "Insumos Taller", 18000.0, "Efectivo", null, "Compra de desengrasante y estopa para el patio", "Admin")
            )
            initialCash.forEach { cashEntryDao.insertEntry(it) }

            // 7. Seed Audit Logs
            val initialLogs = listOf(
                AuditLogEntity("l_1", System.currentTimeMillis() - 28800000L, "Admin", "Admin", "Apertura de caja diaria por $250.000", "Cash", "c_1"),
                AuditLogEntity("l_2", System.currentTimeMillis() - 25000000L, "Recepción", "Recepción", "Ingreso de orden OT-101 (Placa ABC-12D)", "WorkOrder", "OT-101"),
                AuditLogEntity("l_3", System.currentTimeMillis() - 21600000L, "Recepción", "Recepción", "Registró anticipo $80.000 Nequi para OT-101", "Payment", "OT-101"),
                AuditLogEntity("l_4", System.currentTimeMillis() - 14400000L, "Carlos Mecánico", "Mecánico", "Inició labor en OT-101 y detectó filtro sucio", "WorkOrder", "OT-101"),
                AuditLogEntity("l_5", System.currentTimeMillis() - 7200000L, "Mateo Especialista", "Mecánico", "Completó control de calidad de 5 puntos en OT-104", "WorkOrder", "OT-104")
            )
            initialLogs.forEach { auditLogDao.insertLog(it) }
        }
    }
}
