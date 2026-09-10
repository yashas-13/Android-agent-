package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "audit_logs")
data class AuditLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val taskId: String,
    val taskDescription: String,
    val actionType: String,
    val targetDescription: String,
    val packageName: String,
    val status: String, // SUCCESS, FAILED, CONFIRMED, BLOCKED_BY_POLICY, EMERGENCY_STOP
    val reason: String,
    val confidence: Float,
    val latencyMs: Long = 0L,
    val wasPolicyApproved: Boolean = true
)
