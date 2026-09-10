package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "whatsapp_groups")
data class WhatsAppGroupEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val description: String = "",
    val isSelected: Boolean = true,
    val lastBroadcastTimestamp: Long = 0L,
    val totalMessagesSent: Int = 0
)
