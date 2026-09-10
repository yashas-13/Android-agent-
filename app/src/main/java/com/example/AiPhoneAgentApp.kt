package com.example

import android.app.Application
import com.example.agent.AgentController
import com.example.data.db.AgentDatabase
import com.example.data.repository.AgentRepository
import com.example.data.settings.AppSettings
import com.example.scheduler.BroadcastScheduler
import com.example.util.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class AiPhoneAgentApp : Application() {

    lateinit var database: AgentDatabase
        private set

    lateinit var repository: AgentRepository
        private set

    lateinit var agentController: AgentController
        private set

    lateinit var appSettings: AppSettings
        private set

    lateinit var broadcastScheduler: BroadcastScheduler
        private set

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        instance = this
        NotificationHelper.createNotificationChannel(this)

        database = AgentDatabase.getInstance(this)
        appSettings = AppSettings(this)
        repository = AgentRepository(
            auditLogDao = database.auditLogDao(),
            whatsAppGroupDao = database.whatsAppGroupDao(),
            scheduledBroadcastDao = database.scheduledBroadcastDao(),
            messageTemplateDao = database.messageTemplateDao(),
            appSettings = appSettings,
            context = this
        )
        agentController = AgentController(
            repository = repository,
            scope = applicationScope
        )
        broadcastScheduler = BroadcastScheduler(
            context = this,
            repository = repository,
            agentController = agentController,
            scope = applicationScope
        )
        broadcastScheduler.startPolling()
    }

    companion object {
        lateinit var instance: AiPhoneAgentApp
            private set
    }
}
