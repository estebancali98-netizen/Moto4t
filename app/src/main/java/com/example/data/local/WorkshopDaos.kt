package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ClientDao {
    @Query("SELECT * FROM clients ORDER BY name ASC")
    fun getAllClients(): Flow<List<ClientEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClient(client: ClientEntity)

    @Update
    suspend fun updateClient(client: ClientEntity)

    @Query("DELETE FROM clients WHERE id = :id")
    suspend fun deleteClient(id: String)
}

@Dao
interface MotorcycleDao {
    @Query("SELECT * FROM motorcycles ORDER BY plate ASC")
    fun getAllMotorcycles(): Flow<List<MotorcycleEntity>>

    @Query("SELECT * FROM motorcycles WHERE ownerClientId = :clientId")
    fun getMotorcyclesByClient(clientId: String): Flow<List<MotorcycleEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMotorcycle(motorcycle: MotorcycleEntity)

    @Update
    suspend fun updateMotorcycle(motorcycle: MotorcycleEntity)

    @Query("DELETE FROM motorcycles WHERE id = :id")
    suspend fun deleteMotorcycle(id: String)
}

@Dao
interface InventoryDao {
    @Query("SELECT * FROM inventory ORDER BY description ASC")
    fun getAllInventory(): Flow<List<InventoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: InventoryEntity)

    @Update
    suspend fun updateItem(item: InventoryEntity)

    @Query("UPDATE inventory SET stockQuantity = stockQuantity - :amount WHERE id = :id AND stockQuantity >= :amount")
    suspend fun decrementStock(id: String, amount: Int)

    @Query("UPDATE inventory SET stockQuantity = stockQuantity + :amount WHERE id = :id")
    suspend fun incrementStock(id: String, amount: Int)

    @Query("DELETE FROM inventory WHERE id = :id")
    suspend fun deleteItem(id: String)
}

@Dao
interface WorkOrderDao {
    @Query("SELECT * FROM work_orders ORDER BY entryTimestamp DESC")
    fun getAllOrders(): Flow<List<WorkOrderEntity>>

    @Query("SELECT * FROM work_orders WHERE status = :status ORDER BY entryTimestamp DESC")
    fun getOrdersByStatus(status: String): Flow<List<WorkOrderEntity>>

    @Query("SELECT * FROM work_orders WHERE assignedMechanicId = :mechanicId ORDER BY entryTimestamp DESC")
    fun getOrdersByMechanic(mechanicId: String): Flow<List<WorkOrderEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrder(order: WorkOrderEntity)

    @Update
    suspend fun updateOrder(order: WorkOrderEntity)

    @Query("DELETE FROM work_orders WHERE id = :id")
    suspend fun deleteOrder(id: String)
}

@Dao
interface CashEntryDao {
    @Query("SELECT * FROM cash_entries ORDER BY timestamp DESC")
    fun getAllCashEntries(): Flow<List<CashEntryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntry(entry: CashEntryEntity)
}

@Dao
interface AuditLogDao {
    @Query("SELECT * FROM audit_logs ORDER BY timestamp DESC LIMIT 200")
    fun getAllAuditLogs(): Flow<List<AuditLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: AuditLogEntity)
}
