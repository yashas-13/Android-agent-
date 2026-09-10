package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.agent.AgentController
import com.example.agent.AgentState
import com.example.agent.DeliveryStatus
import com.example.agent.GroupDeliveryResult
import com.example.data.db.WhatsAppGroupEntity
import com.example.data.repository.AgentRepository
import com.example.ui.components.CyberBadge
import com.example.ui.components.ErrorReportDialog
import com.example.ui.components.MessageTemplateDialog
import com.example.ui.components.ScheduleBroadcastDialog
import com.example.ui.theme.CyberAmber
import com.example.ui.theme.CyberBackground
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.CyberGreen
import com.example.ui.theme.CyberRed
import com.example.ui.theme.CyberSurface
import com.example.ui.theme.CyberSurfaceVariant
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WhatsAppGreen
import kotlinx.coroutines.launch

@Composable
fun WhatsAppBroadcasterScreen(
    agentController: AgentController,
    repository: AgentRepository,
    modifier: Modifier = Modifier
) {
    val groups by repository.allGroups.collectAsStateWithLifecycle(initialValue = emptyList())
    val templates by repository.allTemplates.collectAsStateWithLifecycle(initialValue = emptyList())
    val agentState by agentController.state.collectAsStateWithLifecycle()
    val groupDeliveryResults by agentController.groupDeliveryResults.collectAsStateWithLifecycle()
    val verificationResult by agentController.verificationResult.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    val appSettingsState by repository.appSettings.settings.collectAsStateWithLifecycle()
    var messageText by remember {
        mutableStateOf("🚀 Project milestone completed ahead of schedule! Please review the release notes.")
    }
    var preApproveBatch by remember(appSettingsState.preApproveBatch) { mutableStateOf(appSettingsState.preApproveBatch) }
    var showAddDialog by remember { mutableStateOf(false) }
    var showTemplateDialog by remember { mutableStateOf(false) }
    var showScheduleDialog by remember { mutableStateOf(false) }
    var showErrorReportDialog by remember { mutableStateOf(false) }
    var newGroupName by remember { mutableStateOf("") }
    var newGroupDesc by remember { mutableStateOf("") }

    val selectedGroups = groups.filter { it.isSelected }
    val isRunning = agentState == AgentState.EXECUTING ||
            agentState == AgentState.PLANNING ||
            agentState == AgentState.OBSERVING ||
            agentState == AgentState.VERIFYING ||
            agentState == AgentState.UNDERSTANDING

    val failedGroups = remember(groupDeliveryResults) {
        groupDeliveryResults.filter { it.status == DeliveryStatus.FAILED }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(CyberBackground)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(6.dp))
            // Header Banner
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(CyberSurface)
                    .border(1.dp, WhatsAppGreen.copy(alpha = 0.3f), RoundedCornerShape(14.dp))
                    .padding(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(WhatsAppGreen.copy(alpha = 0.15f))
                            .border(1.dp, WhatsAppGreen, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Groups,
                            contentDescription = "Groups",
                            tint = WhatsAppGreen,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "WHATSAPP MULTI-GROUP BROADCASTER",
                            color = TextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Share messages to multiple selected groups simultaneously with scheduling & templates.",
                            color = TextSecondary,
                            fontSize = 11.sp,
                            lineHeight = 15.sp
                        )
                    }
                }
            }
        }

        // Failure Alert Banner (if any group failed)
        if (failedGroups.isNotEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF2B1217))
                        .border(1.dp, CyberRed.copy(alpha = 0.7f), RoundedCornerShape(12.dp))
                        .padding(14.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.ErrorOutline,
                                    contentDescription = null,
                                    tint = CyberRed,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "DELIVERY ISSUE: ${failedGroups.size} GROUP(S) FAILED",
                                    color = CyberRed,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            CyberBadge(text = "ACTION REQUIRED", color = CyberRed)
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = failedGroups.firstOrNull()?.let {
                                "Reason for '${it.groupName}': ${it.failureReason ?: "Element unreachable"}"
                            } ?: "One or more target groups could not receive the broadcast.",
                            color = CyberAmber,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            lineHeight = 15.sp
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            OutlinedButton(
                                onClick = { showErrorReportDialog = true },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = CyberCyan),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.height(34.dp).testTag("view_diagnostics_button")
                            ) {
                                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Report & Diagnostics", fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            Button(
                                onClick = { agentController.retryFailedGroups() },
                                colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = Color(0xFF001F28)),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.height(34.dp).testTag("retry_failed_button")
                            ) {
                                Icon(Icons.Default.Replay, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Retry Failed", fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                            }
                        }
                    }
                }
            }
        }

        // Message Composer Card
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(CyberSurface)
                    .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(12.dp))
                    .padding(14.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "COMPOSE BROADCAST MESSAGE",
                            color = CyberCyan,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 1.sp
                        )

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Templates Button
                            OutlinedButton(
                                onClick = { showTemplateDialog = true },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = CyberCyan),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier
                                    .height(28.dp)
                                    .testTag("open_templates_button")
                            ) {
                                Icon(Icons.Default.Bookmark, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Templates (${templates.size})", fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = messageText,
                        onValueChange = { messageText = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(115.dp)
                            .testTag("broadcast_message_input"),
                        placeholder = { Text("Enter message content to broadcast to all selected groups...", color = TextMuted) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = CyberSurfaceVariant,
                            unfocusedContainerColor = CyberSurfaceVariant,
                            focusedBorderColor = CyberCyan,
                            unfocusedBorderColor = Color(0xFF334155),
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        shape = RoundedCornerShape(8.dp)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Quick Template Chips Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "QUICK TEMPLATES:",
                            color = TextMuted,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )

                        if (messageText.isNotBlank()) {
                            Text(
                                text = "Save as Template",
                                color = CyberCyan,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier
                                    .clickable { showTemplateDialog = true }
                                    .padding(horizontal = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(templates.take(5)) { t ->
                            TemplateChip(
                                label = t.title,
                                onClick = {
                                    messageText = t.content
                                    scope.launch { repository.useTemplate(t.id) }
                                }
                            )
                        }
                    }
                }
            }
        }

        // Target Groups Selection Card
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(CyberSurface)
                    .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(12.dp))
                    .padding(14.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "TARGET GROUPS",
                                color = TextPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            CyberBadge(
                                text = "${selectedGroups.size}/${groups.size} selected",
                                color = if (selectedGroups.isNotEmpty()) WhatsAppGreen else TextMuted
                            )
                        }

                        IconButton(
                            onClick = { showAddDialog = true },
                            modifier = Modifier
                                .size(32.dp)
                                .testTag("add_group_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Add Custom Group",
                                tint = CyberCyan,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    if (groups.isEmpty()) {
                        Text(
                            text = "No groups configured. Click + to add your WhatsApp group names.",
                            color = TextMuted,
                            fontSize = 12.sp
                        )
                    } else {
                        groups.forEach { group ->
                            val delivery = groupDeliveryResults.firstOrNull { it.groupName == group.name }
                            GroupItemRow(
                                group = group,
                                deliveryStatus = delivery,
                                onToggle = {
                                    scope.launch {
                                        repository.toggleGroupSelection(group.id, !group.isSelected)
                                    }
                                },
                                onDelete = {
                                    scope.launch {
                                        repository.deleteGroup(group)
                                    }
                                }
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                        }
                    }
                }
            }
        }

        // Safety Pre-Approval Switch & Execution Controls
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(CyberSurface)
                    .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(12.dp))
                    .padding(14.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "BATCH POLICY PRE-APPROVAL",
                                color = TextPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "Permits autonomous delivery across all selected groups sequentially without per-group prompt.",
                                color = TextSecondary,
                                fontSize = 11.sp,
                                lineHeight = 15.sp
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Switch(
                            checked = preApproveBatch,
                            onCheckedChange = {
                                preApproveBatch = it
                                repository.appSettings.updatePreApproveBatch(it)
                            },
                            modifier = Modifier.testTag("pre_approve_batch_switch"),
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = WhatsAppGreen,
                                checkedTrackColor = WhatsAppGreen.copy(alpha = 0.3f),
                                uncheckedThumbColor = TextMuted,
                                uncheckedTrackColor = Color(0xFF1E293B)
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Launch, Schedule, or Stop Buttons
                    if (isRunning) {
                        Button(
                            onClick = { agentController.stopAgent("User stopped broadcast") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("stop_broadcast_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CyberRed,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "STOP BROADCAST AGENT",
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 13.sp
                            )
                        }
                    } else {
                        Row(modifier = Modifier.fillMaxWidth()) {
                            // Schedule Button
                            OutlinedButton(
                                onClick = { showScheduleDialog = true },
                                enabled = selectedGroups.isNotEmpty() && messageText.isNotBlank(),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(50.dp)
                                    .testTag("schedule_broadcast_button"),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = CyberCyan,
                                    disabledContentColor = TextMuted
                                ),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.Schedule, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "SCHEDULE",
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 12.sp
                                )
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            // Send Now Button
                            Button(
                                onClick = {
                                    val groupNames = selectedGroups.map { it.name }
                                    agentController.startWhatsAppBroadcast(
                                        targetGroupNames = groupNames,
                                        message = messageText,
                                        isPreApproved = preApproveBatch
                                    )
                                },
                                enabled = selectedGroups.isNotEmpty() && messageText.isNotBlank(),
                                modifier = Modifier
                                    .weight(2f)
                                    .height(50.dp)
                                    .testTag("launch_broadcast_button"),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = WhatsAppGreen,
                                    contentColor = Color(0xFF00220E),
                                    disabledContainerColor = Color(0xFF1E293B),
                                    disabledContentColor = TextMuted
                                ),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "SEND (${selectedGroups.size} GROUPS)",
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(18.dp))
        }
    }

    // Dialog: Add Group
    if (showAddDialog) {
        androidx.compose.ui.window.Dialog(onDismissRequest = { showAddDialog = false }) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(CyberSurface)
                    .border(1.dp, CyberCyan, RoundedCornerShape(16.dp))
                    .padding(20.dp)
            ) {
                Column {
                    Text(
                        text = "ADD WHATSAPP GROUP",
                        color = CyberCyan,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = newGroupName,
                        onValueChange = { newGroupName = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("new_group_name_input"),
                        label = { Text("Group Name (Exact as in WhatsApp)", color = TextMuted) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = CyberCyan,
                            unfocusedBorderColor = Color(0xFF334155)
                        )
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = newGroupDesc,
                        onValueChange = { newGroupDesc = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Description / Note (Optional)", color = TextMuted) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = CyberCyan,
                            unfocusedBorderColor = Color(0xFF334155)
                        )
                    )
                    Spacer(modifier = Modifier.height(18.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        OutlinedButton(
                            onClick = { showAddDialog = false },
                            modifier = Modifier.height(44.dp)
                        ) {
                            Text("Cancel", color = TextSecondary)
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Button(
                            onClick = {
                                if (newGroupName.isNotBlank()) {
                                    scope.launch {
                                        repository.addGroup(newGroupName, newGroupDesc)
                                        newGroupName = ""
                                        newGroupDesc = ""
                                        showAddDialog = false
                                    }
                                }
                            },
                            enabled = newGroupName.isNotBlank(),
                            modifier = Modifier
                                .height(44.dp)
                                .testTag("confirm_add_group_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = Color(0xFF001F28))
                        ) {
                            Text("Add Group", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    // Dialog: Message Templates
    if (showTemplateDialog) {
        MessageTemplateDialog(
            repository = repository,
            currentMessage = messageText,
            onDismiss = { showTemplateDialog = false },
            onSelectTemplate = { selectedContent ->
                messageText = selectedContent
            }
        )
    }

    // Dialog: Schedule Broadcast
    if (showScheduleDialog) {
        ScheduleBroadcastDialog(
            repository = repository,
            selectedGroups = selectedGroups.map { it.name },
            message = messageText,
            onDismiss = { showScheduleDialog = false },
            onScheduled = {
                // Done
            }
        )
    }

    // Dialog: Error Report & Diagnostics
    if (showErrorReportDialog) {
        ErrorReportDialog(
            failedGroups = failedGroups,
            diagnosticLog = agentController.generateDiagnosticReport(),
            onDismiss = { showErrorReportDialog = false },
            onRetry = { agentController.retryFailedGroups() }
        )
    }
}

@Composable
private fun GroupItemRow(
    group: WhatsAppGroupEntity,
    deliveryStatus: GroupDeliveryResult?,
    onToggle: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(if (group.isSelected) WhatsAppGreen.copy(alpha = 0.08f) else CyberSurfaceVariant)
            .border(
                0.8.dp,
                if (group.isSelected) WhatsAppGreen.copy(alpha = 0.3f) else Color(0xFF334155),
                RoundedCornerShape(8.dp)
            )
            .clickable(onClick = onToggle)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = group.isSelected,
            onCheckedChange = { onToggle() },
            modifier = Modifier.testTag("group_checkbox_${group.id}"),
            colors = CheckboxDefaults.colors(
                checkedColor = WhatsAppGreen,
                checkmarkColor = Color(0xFF00220E),
                uncheckedColor = TextMuted
            )
        )
        Spacer(modifier = Modifier.width(6.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = group.name,
                color = TextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
            if (group.description.isNotBlank()) {
                Text(
                    text = group.description,
                    color = TextSecondary,
                    fontSize = 11.sp
                )
            }
            if (deliveryStatus != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    when (deliveryStatus.status) {
                        DeliveryStatus.PENDING -> CyberBadge(text = "QUEUED", color = CyberCyan)
                        DeliveryStatus.SENDING -> CyberBadge(text = "SENDING...", color = CyberAmber)
                        DeliveryStatus.SUCCESS -> CyberBadge(text = "DELIVERED", color = CyberGreen)
                        DeliveryStatus.FAILED -> CyberBadge(text = "FAILED: ${deliveryStatus.failureReason ?: "Error"}", color = CyberRed)
                    }
                }
            }
        }
        if (group.totalMessagesSent > 0) {
            CyberBadge(text = "${group.totalMessagesSent} sent", color = WhatsAppGreen)
            Spacer(modifier = Modifier.width(6.dp))
        }
        IconButton(
            onClick = onDelete,
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "Delete Group",
                tint = TextMuted,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
private fun TemplateChip(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(CyberSurfaceVariant)
            .border(0.6.dp, Color(0xFF334155), RoundedCornerShape(6.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 5.dp)
    ) {
        Text(
            text = label,
            color = TextSecondary,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace
        )
    }
}
