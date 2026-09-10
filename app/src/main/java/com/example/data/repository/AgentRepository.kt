package com.example.data.repository

import android.content.Context
import com.example.data.db.AuditLogDao
import com.example.data.db.AuditLogEntity
import com.example.data.db.MessageTemplateDao
import com.example.data.db.MessageTemplateEntity
import com.example.data.db.ScheduledBroadcastDao
import com.example.data.db.ScheduledBroadcastEntity
import com.example.data.db.WhatsAppGroupDao
import com.example.data.db.WhatsAppGroupEntity
import com.example.data.settings.AppSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class AgentRepository(
    private val auditLogDao: AuditLogDao,
    private val whatsAppGroupDao: WhatsAppGroupDao,
    private val scheduledBroadcastDao: ScheduledBroadcastDao,
    private val messageTemplateDao: MessageTemplateDao,
    val appSettings: AppSettings,
    private val context: Context
) {
    val allLogs: Flow<List<AuditLogEntity>> = auditLogDao.getAllLogs()
    val allGroups: Flow<List<WhatsAppGroupEntity>> = whatsAppGroupDao.getAllGroups()
    val allScheduledBroadcasts: Flow<List<ScheduledBroadcastEntity>> = scheduledBroadcastDao.getAllScheduledBroadcasts()
    val allTemplates: Flow<List<MessageTemplateEntity>> = messageTemplateDao.getAllTemplates()

    init {
        CoroutineScope(Dispatchers.IO).launch {
            // Seed default groups if empty
            if (whatsAppGroupDao.getCount() == 0) {
                whatsAppGroupDao.insertGroups(
                    listOf(
                        WhatsAppGroupEntity(name = "Product Team", description = "Daily sprint & product updates", isSelected = true),
                        WhatsAppGroupEntity(name = "Family & Friends", description = "Family announcements & photos", isSelected = true),
                        WhatsAppGroupEntity(name = "Developers Hub", description = "Tech news, releases & code discussions", isSelected = true),
                        WhatsAppGroupEntity(name = "Neighborhood Watch", description = "Community notices and safety alerts", isSelected = false)
                    )
                )
            }

            // Seed default templates if empty
            if (messageTemplateDao.getCount() == 0) {
                messageTemplateDao.insertTemplates(
                    listOf(
                        MessageTemplateEntity(
                            title = "Daily Sprint Standup",
                            category = "Work",
                            content = "🚀 Team Standup: Please share what you completed yesterday, what you are targeting today, and any blocking issues.",
                            timesUsed = 3
                        ),
                        MessageTemplateEntity(
                            title = "Urgent System Maintenance",
                            category = "Urgent",
                            content = "⚠️ SCHEDULED NOTICE: System maintenance will take place tonight from 11:00 PM to 11:30 PM UTC. Brief intermittent connectivity may occur.",
                            timesUsed = 1
                        ),
                        MessageTemplateEntity(
                            title = "Weekly Community Sync",
                            category = "Community",
                            content = "📅 Community Sync Reminder: Join us for our weekly open discussion tomorrow at 4:00 PM GMT. Looking forward to seeing everyone!",
                            timesUsed = 2
                        ),
                        MessageTemplateEntity(
                            title = "Product Milestone Release",
                            category = "Announcements",
                            content = "🎉 Major milestone achieved! Version 2.0 has been deployed with refreshed UI, upgraded performance, and bug fixes.",
                            timesUsed = 4
                        ),
                        MessageTemplateEntity(
                            title = "Quick Family Check-in",
                            category = "Family",
                            content = "👋 Quick check-in with everyone! Hope you all have an amazing weekend ahead.",
                            timesUsed = 0
                        )
                    )
                )
            }
        }
    }

    suspend fun getSelectedGroups(): List<WhatsAppGroupEntity> {
        return whatsAppGroupDao.getSelectedGroups()
    }

    suspend fun addGroup(name: String, description: String) {
        whatsAppGroupDao.insertGroup(
            WhatsAppGroupEntity(
                name = name.trim(),
                description = description.trim(),
                isSelected = true
            )
        )
    }

    suspend fun toggleGroupSelection(id: Long, selected: Boolean) {
        whatsAppGroupDao.updateSelection(id, selected)
    }

    suspend fun deleteGroup(group: WhatsAppGroupEntity) {
        whatsAppGroupDao.deleteGroup(group)
    }

    suspend fun recordGroupMessageSent(name: String) {
        whatsAppGroupDao.incrementMessageCount(name, System.currentTimeMillis())
    }

    suspend fun recordAuditLog(log: AuditLogEntity): Long {
        return auditLogDao.insertLog(log)
    }

    suspend fun clearAuditLogs() {
        auditLogDao.clearAll()
    }

    // Template Operations
    suspend fun addTemplate(title: String, category: String, content: String): Long {
        return messageTemplateDao.insertTemplate(
            MessageTemplateEntity(
                title = title.trim(),
                category = category.trim(),
                content = content.trim()
            )
        )
    }

    suspend fun deleteTemplate(template: MessageTemplateEntity) {
        messageTemplateDao.deleteTemplate(template)
    }

    suspend fun useTemplate(templateId: Long) {
        messageTemplateDao.incrementUsage(templateId)
    }

    // Scheduled Broadcast Operations
    suspend fun scheduleBroadcast(
        title: String,
        message: String,
        targetGroups: List<String>,
        scheduledTimeMillis: Long
    ): Long {
        return scheduledBroadcastDao.insertScheduledBroadcast(
            ScheduledBroadcastEntity(
                title = title.ifBlank { "Broadcast (${targetGroups.size} groups)" },
                message = message,
                targetGroupsJson = targetGroups.joinToString(","),
                groupCount = targetGroups.size,
                scheduledTimeMillis = scheduledTimeMillis
            )
        )
    }

    suspend fun updateScheduledStatus(id: Long, status: String) {
        scheduledBroadcastDao.updateStatus(id, status)
    }

    suspend fun markBroadcastCompleted(
        id: Long,
        status: String,
        failureReason: String? = null,
        successCount: Int = 0,
        failedCount: Int = 0,
        resultsJson: String? = null
    ) {
        scheduledBroadcastDao.markCompleted(
            id = id,
            status = status,
            executedAtMillis = System.currentTimeMillis(),
            failureReason = failureReason,
            successCount = successCount,
            failedCount = failedCount,
            resultsJson = resultsJson
        )
    }

    suspend fun deleteScheduledBroadcast(broadcast: ScheduledBroadcastEntity) {
        scheduledBroadcastDao.deleteScheduledBroadcast(broadcast)
    }

    suspend fun getDueBroadcasts(): List<ScheduledBroadcastEntity> {
        return scheduledBroadcastDao.getDueBroadcasts(System.currentTimeMillis())
    }
}
