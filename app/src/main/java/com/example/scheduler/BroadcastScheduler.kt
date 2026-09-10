package com.example.scheduler

import android.content.Context
import android.util.Log
import com.example.agent.AgentController
import com.example.agent.AgentState
import com.example.data.db.ScheduleStatus
import com.example.data.db.ScheduledBroadcastEntity
import com.example.data.repository.AgentRepository
import com.example.util.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class BroadcastScheduler(
    private val context: Context,
    private val repository: AgentRepository,
    private val agentController: AgentController,
    private val scope: CoroutineScope
) {
    private val TAG = "BroadcastScheduler"

    fun startPolling() {
        scope.launch {
            Log.d(TAG, "Starting scheduled broadcast background watcher...")
            while (isActive) {
                try {
                    checkAndTriggerDueBroadcasts()
                } catch (e: Exception) {
                    Log.e(TAG, "Error in scheduled broadcast watcher: ${e.message}", e)
                }
                // Check every 20 seconds
                delay(20_000L)
            }
        }
    }

    suspend fun checkAndTriggerDueBroadcasts() {
        val due = repository.getDueBroadcasts()
        if (due.isNotEmpty()) {
            Log.d(TAG, "Found ${due.size} due scheduled broadcasts.")
            for (broadcast in due) {
                executeScheduledBroadcast(broadcast)
            }
        }
    }

    fun triggerNow(broadcast: ScheduledBroadcastEntity) {
        scope.launch {
            executeScheduledBroadcast(broadcast)
        }
    }

    private suspend fun executeScheduledBroadcast(broadcast: ScheduledBroadcastEntity) {
        if (agentController.agentState.value != AgentState.IDLE &&
            agentController.agentState.value != AgentState.SUCCESS &&
            agentController.agentState.value != AgentState.STOPPED &&
            agentController.agentState.value != AgentState.FAILED
        ) {
            Log.w(TAG, "Agent is currently busy with another task. Rescheduling check.")
            return
        }

        repository.updateScheduledStatus(broadcast.id, ScheduleStatus.RUNNING.name)
        val groups = broadcast.groupNamesList

        try {
            agentController.startWhatsAppBroadcast(
                targetGroupNames = groups,
                message = broadcast.message,
                isPreApproved = true
            )

            // Wait for completion or failure
            while (
                agentController.agentState.value != AgentState.SUCCESS &&
                agentController.agentState.value != AgentState.FAILED &&
                agentController.agentState.value != AgentState.STOPPED
            ) {
                delay(1000L)
            }

            val finalState = agentController.agentState.value
            if (finalState == AgentState.SUCCESS) {
                repository.markBroadcastCompleted(
                    id = broadcast.id,
                    status = ScheduleStatus.COMPLETED.name,
                    successCount = groups.size,
                    failedCount = 0
                )
                if (repository.appSettings.settings.value.notifyOnCompletion) {
                    NotificationHelper.showBroadcastSuccess(
                        context = context,
                        groupCount = groups.size,
                        title = broadcast.title
                    )
                }
            } else {
                val errorReason = agentController.verificationResult.value ?: "Broadcast stopped or interrupted"
                repository.markBroadcastCompleted(
                    id = broadcast.id,
                    status = ScheduleStatus.FAILED.name,
                    failureReason = errorReason,
                    successCount = 0,
                    failedCount = groups.size
                )
                if (repository.appSettings.settings.value.notifyOnFailure) {
                    NotificationHelper.showBroadcastFailure(
                        context = context,
                        title = broadcast.title,
                        failedCount = groups.size,
                        reason = errorReason
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed executing scheduled broadcast: ${e.message}", e)
            repository.markBroadcastCompleted(
                id = broadcast.id,
                status = ScheduleStatus.FAILED.name,
                failureReason = e.message ?: "Execution error"
            )
        }
    }
}
