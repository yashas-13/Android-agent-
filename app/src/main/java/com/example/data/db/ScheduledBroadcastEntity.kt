package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class ScheduleStatus {
    PENDING,
    RUNNING,
    COMPLETED,
    FAILED,
    CANCELLED
}

@Entity(tableName = "scheduled_broadcasts")
data class ScheduledBroadcastEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val message: String,
    val targetGroupsJson: String, // Comma-separated list of group names
    val groupCount: Int,
    val scheduledTimeMillis: Long,
    val status: String = ScheduleStatus.PENDING.name,
    val createdAtMillis: Long = System.currentTimeMillis(),
    val executedAtMillis: Long = 0L,
    val failureReason: String? = null,
    val successfulGroupsCount: Int = 0,
    val failedGroupsCount: Int = 0,
    val detailedResultsJson: String? = null
) {
    val groupNamesList: List<String>
        get() = targetGroupsJson.split(",").map { it.trim() }.filter { it.isNotEmpty() }
}
