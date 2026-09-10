package com.example.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageTemplateDao {
    @Query("SELECT * FROM message_templates ORDER BY timesUsed DESC, id DESC")
    fun getAllTemplates(): Flow<List<MessageTemplateEntity>>

    @Query("SELECT * FROM message_templates WHERE category = :category ORDER BY timesUsed DESC")
    fun getTemplatesByCategory(category: String): Flow<List<MessageTemplateEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTemplate(template: MessageTemplateEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTemplates(templates: List<MessageTemplateEntity>)

    @Update
    suspend fun updateTemplate(template: MessageTemplateEntity)

    @Delete
    suspend fun deleteTemplate(template: MessageTemplateEntity)

    @Query("UPDATE message_templates SET timesUsed = timesUsed + 1 WHERE id = :id")
    suspend fun incrementUsage(id: Long)

    @Query("SELECT COUNT(*) FROM message_templates")
    suspend fun getCount(): Int
}
