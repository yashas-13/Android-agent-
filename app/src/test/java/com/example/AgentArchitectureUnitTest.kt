package com.example

import com.example.accessibility.ScreenSnapshot
import com.example.accessibility.UiElementInfo
import com.example.agent.ActionTarget
import com.example.agent.AgentAction
import com.example.agent.AgentActionType
import com.example.agent.AgentRecovery
import com.example.agent.ElementBounds
import com.example.agent.RecoveryStrategy
import com.example.ai.LocalModelProvider
import com.example.policy.ActionPolicyEngine
import com.example.policy.PolicyDecision
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AgentArchitectureUnitTest {

    @Test
    fun testActionSchemaValidation() {
        // TYPE_TEXT without payload must be invalid
        val invalidTypeText = AgentAction(
            action = AgentActionType.TYPE_TEXT,
            payload = null
        )
        assertFalse("TYPE_TEXT without payload should be invalid", invalidTypeText.isValid())

        // TYPE_TEXT with payload must be valid
        val validTypeText = AgentAction(
            action = AgentActionType.TYPE_TEXT,
            target = ActionTarget(text = "Message field"),
            payload = "Hello World"
        )
        assertTrue("TYPE_TEXT with payload should be valid", validTypeText.isValid())

        // OPEN_APP requires package payload
        val validOpenApp = AgentAction(
            action = AgentActionType.OPEN_APP,
            payload = "com.whatsapp"
        )
        assertTrue(validOpenApp.isValid())

        val invalidOpenApp = AgentAction(
            action = AgentActionType.OPEN_APP,
            payload = ""
        )
        assertFalse(invalidOpenApp.isValid())
    }

    @Test
    fun testPolicyEngineSecurityBlocksPasswords() {
        val dangerousPasswordAction = AgentAction(
            action = AgentActionType.TYPE_TEXT,
            target = ActionTarget(text = "Password input"),
            payload = "superSecretPassword123",
            reason = "Enter account password"
        )

        val eval = ActionPolicyEngine.evaluate(dangerousPasswordAction)
        assertEquals(PolicyDecision.BLOCKED, eval.decision)
        assertTrue(eval.reason.contains("strictly forbidden", ignoreCase = true))
    }

    @Test
    fun testPolicyEngineRequiresConfirmationForSendingMessage() {
        val sendAction = AgentAction(
            action = AgentActionType.CLICK,
            target = ActionTarget(text = "Send", role = "button"),
            reason = "Send WhatsApp message"
        )

        // Without batch pre-approval, requires explicit user confirmation
        val unapprovedEval = ActionPolicyEngine.evaluate(sendAction, isBatchPreApproved = false)
        assertEquals(PolicyDecision.REQUIRES_CONFIRMATION, unapprovedEval.decision)
        assertTrue(unapprovedEval.requiresExplicitUserApproval)

        // With batch pre-approval, allowed
        val approvedEval = ActionPolicyEngine.evaluate(sendAction, isBatchPreApproved = true)
        assertEquals(PolicyDecision.ALLOWED, approvedEval.decision)
    }

    @Test
    fun testPolicyEngineAllowsSafeNavigation() {
        val navAction = AgentAction(
            action = AgentActionType.CLICK,
            target = ActionTarget(text = "Network & internet", role = "button"),
            reason = "Navigate to network settings"
        )

        val eval = ActionPolicyEngine.evaluate(navAction)
        assertEquals(PolicyDecision.ALLOWED, eval.decision)
    }

    @Test
    fun testAgentRecoveryWorkflow() {
        val recovery = AgentRecovery(maxRecoveryAttempts = 3)

        val target = ActionTarget(text = "Send", role = "button")
        val failedAction = AgentAction(AgentActionType.CLICK, target = target)

        // 1. Keyboard blocking recovery
        val keyboardSnapshot = ScreenSnapshot(
            packageName = "com.whatsapp",
            elements = emptyList(),
            isKeyboardVisible = true
        )
        val strat1 = recovery.determineRecovery(failedAction, keyboardSnapshot, "Click target occluded")
        assertTrue(strat1 is RecoveryStrategy.DismissKeyboardOrDialog)

        // 2. Vision coordinate fallback (when landmark target like Send is recognized)
        val normalSnapshot = ScreenSnapshot(
            packageName = "com.whatsapp",
            elements = emptyList(),
            isKeyboardVisible = false
        )
        val strat2 = recovery.determineRecovery(failedAction, normalSnapshot, "Node not found")
        assertTrue(strat2 is RecoveryStrategy.VisionCoordinateFallback)

        // 3. Scroll and retry (when non-landmark element is not found)
        val unknownAction = AgentAction(AgentActionType.CLICK, target = ActionTarget(text = "Unseen Group Item #42", role = "button"))
        val strat3 = recovery.determineRecovery(unknownAction, normalSnapshot, "Element off-screen")
        assertTrue(strat3 is RecoveryStrategy.ScrollAndRetry)

        // 4. Exceeding max attempts triggers safe abort
        val strat4 = recovery.determineRecovery(failedAction, normalSnapshot, "Still failing")
        assertTrue(strat4 is RecoveryStrategy.AbortSafely)
    }

    @Test
    fun testScreenSnapshotFormatting() {
        val elements = listOf(
            UiElementInfo(
                id = "btn_wifi",
                text = "Wi-Fi",
                role = "button",
                bounds = ElementBounds(40, 100, 400, 200),
                isClickable = true
            ),
            UiElementInfo(
                id = "input_search",
                text = "Search settings",
                role = "input",
                bounds = ElementBounds(40, 220, 1000, 320),
                isEditable = true
            )
        )
        val snapshot = ScreenSnapshot(
            packageName = "com.android.settings",
            elements = elements
        )

        assertEquals(2, snapshot.interactiveCount)
        val compactText = snapshot.toCompactText()
        assertTrue(compactText.contains("com.android.settings"))
        assertTrue(compactText.contains("Wi-Fi"))
        assertTrue(compactText.contains("Search settings"))
    }

    @Test
    fun testLocalModelProviderWhatsAppPlanning() = runBlocking {
        val provider = LocalModelProvider()
        provider.initializeBroadcast(listOf("Product Team"), "Milestone update")

        // 1. Initial screen not in WhatsApp -> should open app
        val homeSnapshot = ScreenSnapshot(packageName = "com.android.launcher", elements = emptyList())
        val plan1 = provider.planNextAction("WhatsApp broadcast", homeSnapshot, emptyList())

        assertNotNull(plan1.action)
        assertEquals(AgentActionType.OPEN_APP, plan1.action?.action)
        assertEquals("com.whatsapp", plan1.action?.payload)
    }

    @Test
    fun testMessageTemplateEntityOperations() {
        val template = com.example.data.db.MessageTemplateEntity(
            id = 1,
            title = "Release Standup",
            content = "Release v2.4 deployed successfully. Team please check staging logs.",
            category = "Engineering",
            timesUsed = 5
        )

        assertEquals("Release Standup", template.title)
        assertTrue(template.content.contains("v2.4"))
        assertEquals(5, template.timesUsed)
    }

    @Test
    fun testScheduledBroadcastEntitySerialization() {
        val groups = listOf("Dev Team", "Marketing Sync", "Leadership Announcement")
        val groupsString = groups.joinToString(",")

        val scheduled = com.example.data.db.ScheduledBroadcastEntity(
            id = 10,
            title = "Weekly Standup",
            message = "Reminder: Weekly team standup starts in 10 minutes.",
            targetGroupsJson = groupsString,
            groupCount = groups.size,
            scheduledTimeMillis = System.currentTimeMillis() + 3600000L,
            status = com.example.data.db.ScheduleStatus.PENDING.name
        )

        assertEquals(3, scheduled.groupNamesList.size)
        assertEquals("Dev Team", scheduled.groupNamesList[0])
        assertEquals("Marketing Sync", scheduled.groupNamesList[1])
        assertEquals("Leadership Announcement", scheduled.groupNamesList[2])
        assertEquals(com.example.data.db.ScheduleStatus.PENDING.name, scheduled.status)
    }

    @Test
    fun testGroupDeliveryResultsTracking() {
        val result1 = com.example.agent.GroupDeliveryResult(
            groupName = "Alpha Squad",
            status = com.example.agent.DeliveryStatus.SUCCESS
        )
        val result2 = com.example.agent.GroupDeliveryResult(
            groupName = "Beta Squad",
            status = com.example.agent.DeliveryStatus.FAILED,
            failureReason = "Target search item not found in WhatsApp chat list"
        )

        assertEquals(com.example.agent.DeliveryStatus.SUCCESS, result1.status)
        assertEquals(com.example.agent.DeliveryStatus.FAILED, result2.status)
        assertNotNull(result2.failureReason)
        assertTrue(result2.failureReason!!.contains("not found"))
    }
}
