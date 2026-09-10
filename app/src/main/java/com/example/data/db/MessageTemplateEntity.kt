package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "message_templates")
data class MessageTemplateEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val category: String = "General",
    val content: String,
    val createdAtMillis: Long = System.currentTimeMillis(),
    val timesUsed: Int = 0
)
