package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        ClientEntity::class,
        MotorcycleEntity::class,
        InventoryEntity::class,
        WorkOrderEntity::class,
        CashEntryEntity::class,
        AuditLogEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class WorkshopDatabase : RoomDatabase() {
    abstract fun clientDao(): ClientDao
    abstract fun motorcycleDao(): MotorcycleDao
    abstract fun inventoryDao(): InventoryDao
    abstract fun workOrderDao(): WorkOrderDao
    abstract fun cashEntryDao(): CashEntryDao
    abstract fun auditLogDao(): AuditLogDao

    companion object {
        @Volatile
        private var INSTANCE: WorkshopDatabase? = null

        fun getDatabase(context: Context): WorkshopDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    WorkshopDatabase::class.java,
                    "mototaller_pro.db"
                ).fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
