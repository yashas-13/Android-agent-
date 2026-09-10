package com.example.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface WhatsAppGroupDao {
    @Query("SELECT * FROM whatsapp_groups ORDER BY id ASC")
    fun getAllGroups(): Flow<List<WhatsAppGroupEntity>>

    @Query("SELECT * FROM whatsapp_groups WHERE isSelected = 1")
    suspend fun getSelectedGroups(): List<WhatsAppGroupEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGroup(group: WhatsAppGroupEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGroups(groups: List<WhatsAppGroupEntity>)

    @Update
    suspend fun updateGroup(group: WhatsAppGroupEntity)

    @Delete
    suspend fun deleteGroup(group: WhatsAppGroupEntity)

    @Query("UPDATE whatsapp_groups SET isSelected = :selected WHERE id = :id")
    suspend fun updateSelection(id: Long, selected: Boolean)

    @Query("UPDATE whatsapp_groups SET totalMessagesSent = totalMessagesSent + 1, lastBroadcastTimestamp = :timestamp WHERE name = :name")
    suspend fun incrementMessageCount(name: String, timestamp: Long)

    @Query("SELECT COUNT(*) FROM whatsapp_groups")
    suspend fun getCount(): Int
}
