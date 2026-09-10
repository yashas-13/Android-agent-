package com.example.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ScheduledBroadcastDao {
    @Query("SELECT * FROM scheduled_broadcasts ORDER BY scheduledTimeMillis ASC")
    fun getAllScheduledBroadcasts(): Flow<List<ScheduledBroadcastEntity>>

    @Query("SELECT * FROM scheduled_broadcasts WHERE status = 'PENDING' AND scheduledTimeMillis <= :currentTimeMillis ORDER BY scheduledTimeMillis ASC")
    suspend fun getDueBroadcasts(currentTimeMillis: Long): List<ScheduledBroadcastEntity>

    @Query("SELECT * FROM scheduled_broadcasts WHERE id = :id")
    suspend fun getBroadcastById(id: Long): ScheduledBroadcastEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScheduledBroadcast(broadcast: ScheduledBroadcastEntity): Long

    @Update
    suspend fun updateScheduledBroadcast(broadcast: ScheduledBroadcastEntity)

    @Delete
    suspend fun deleteScheduledBroadcast(broadcast: ScheduledBroadcastEntity)

    @Query("UPDATE scheduled_broadcasts SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: Long, status: String)

    @Query("UPDATE scheduled_broadcasts SET status = :status, executedAtMillis = :executedAtMillis, failureReason = :failureReason, successfulGroupsCount = :successCount, failedGroupsCount = :failedCount, detailedResultsJson = :resultsJson WHERE id = :id")
    suspend fun markCompleted(
        id: Long,
        status: String,
        executedAtMillis: Long,
        failureReason: String?,
        successCount: Int,
        failedCount: Int,
        resultsJson: String?
    )
}
