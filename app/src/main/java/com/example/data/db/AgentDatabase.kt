package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        AuditLogEntity::class,
        WhatsAppGroupEntity::class,
        ScheduledBroadcastEntity::class,
        MessageTemplateEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AgentDatabase : RoomDatabase() {

    abstract fun auditLogDao(): AuditLogDao
    abstract fun whatsAppGroupDao(): WhatsAppGroupDao
    abstract fun scheduledBroadcastDao(): ScheduledBroadcastDao
    abstract fun messageTemplateDao(): MessageTemplateDao

    companion object {
        @Volatile
        private var INSTANCE: AgentDatabase? = null

        fun getInstance(context: Context): AgentDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AgentDatabase::class.java,
                    "ai_phone_agent.db"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
