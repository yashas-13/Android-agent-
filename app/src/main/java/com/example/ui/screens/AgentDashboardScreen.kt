package com.example.ui.screens

import android.content.Intent
import android.provider.Settings
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dangerous
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.accessibility.PhoneAccessibilityService
import com.example.agent.AgentController
import com.example.agent.AgentState
import com.example.ai.AiProviderType
import com.example.ui.components.AgentDebugPanel
import com.example.ui.components.CurrentActionCard
import com.example.ui.components.CyberBadge
import com.example.ui.components.LiveEventStream
import com.example.ui.components.PolicyConfirmationDialog
import com.example.ui.components.StateBadge
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

@Composable
fun AgentDashboardScreen(
    agentController: AgentController,
    onNavigateToAccessibility: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isAccessibilityConnected by PhoneAccessibilityService.isServiceConnected.collectAsStateWithLifecycle()
    val agentState by agentController.state.collectAsStateWithLifecycle()
    val currentTask by agentController.currentTask.collectAsStateWithLifecycle()
    val currentAction by agentController.currentAction.collectAsStateWithLifecycle()
    val actionHistory by agentController.actionHistory.collectAsStateWithLifecycle()
    val liveEvents by agentController.liveEvents.collectAsStateWithLifecycle()
    val pendingConfirmation by agentController.pendingConfirmationAction.collectAsStateWithLifecycle()
    val debugMetrics by agentController.debugMetrics.collectAsStateWithLifecycle()
    val activeProvider by agentController.activeProvider.collectAsStateWithLifecycle()
    val latestSnapshot by agentController.latestSnapshot.collectAsStateWithLifecycle()

    var taskInput by remember {
        mutableStateOf("Share message to multiple WhatsApp groups: Product Team, Family & Friends")
    }

    val isRunning = agentState == AgentState.EXECUTING ||
            agentState == AgentState.PLANNING ||
            agentState == AgentState.OBSERVING ||
            agentState == AgentState.VERIFYING ||
            agentState == AgentState.UNDERSTANDING ||
            agentState == AgentState.WAITING_FOR_CONFIRMATION

    // Confirmation Modal
    if (pendingConfirmation != null) {
        PolicyConfirmationDialog(
            action = pendingConfirmation!!,
            onApprove = { agentController.approvePendingConfirmation() },
            onReject = { agentController.rejectPendingConfirmation() }
        )
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(CyberBackground)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(4.dp))

            // Service status alert if not enabled
            if (!isAccessibilityConnected) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(CyberAmber.copy(alpha = 0.15f))
                        .border(1.dp, CyberAmber.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                        .clickable { onNavigateToAccessibility() }
                        .padding(12.dp)
                        .testTag("accessibility_warning_banner")
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Warning",
                            tint = CyberAmber,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "ACCESSIBILITY SERVICE INACTIVE",
                                color = CyberAmber,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "Tap to enable. Simulator fallback is active for testing.",
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                        }
                        CyberBadge(text = "SETUP", color = CyberAmber)
                    }
                }
            }
        }

        // Status & Target App Banner
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(CyberSurface)
                    .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(14.dp))
                    .padding(14.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(CyberCyan.copy(alpha = 0.15f))
                                    .border(1.dp, CyberCyan, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Psychology,
                                    contentDescription = "AI Agent",
                                    tint = CyberCyan,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "AI PHONE AGENT",
                                    color = TextPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    letterSpacing = 1.sp
                                )
                                Text(
                                    text = activeProvider.displayName.take(28),
                                    color = TextMuted,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                        StateBadge(state = agentState)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Detected App row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(CyberSurfaceVariant)
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Visibility,
                                contentDescription = "Observed App",
                                tint = CyberCyan,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "DETECTED:",
                                color = TextMuted,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = debugMetrics.currentPackage.ifEmpty { "system / homescreen" },
                                color = TextPrimary,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        CyberBadge(text = "${debugMetrics.accessibilityNodeCount} nodes", color = CyberCyan)
                    }
                }
            }
        }

        // AI Task Input Card
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
                    Text(
                        text = "NATURAL LANGUAGE TASK COMMAND",
                        color = CyberCyan,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = taskInput,
                        onValueChange = { taskInput = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("ai_task_input"),
                        placeholder = { Text("e.g. Open Settings and turn on Wi-Fi...", color = TextMuted) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = CyberSurfaceVariant,
                            unfocusedContainerColor = CyberSurfaceVariant,
                            focusedBorderColor = CyberCyan,
                            unfocusedBorderColor = Color(0xFF334155),
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        shape = RoundedCornerShape(8.dp),
                        singleLine = false,
                        maxLines = 3
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Preset Quick Task Chips
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        item {
                            QuickTaskChip(
                                label = "WhatsApp Multi-Group Share",
                                onClick = {
                                    taskInput = "Share message to multiple WhatsApp groups: Product Team, Family & Friends. Message: \"Milestone update ready!\""
                                }
                            )
                        }
                        item {
                            QuickTaskChip(
                                label = "Open Settings & Wi-Fi",
                                onClick = {
                                    taskInput = "Open Settings and turn on Wi-Fi"
                                }
                            )
                        }
                        item {
                            QuickTaskChip(
                                label = "Find Contacts",
                                onClick = {
                                    taskInput = "Open Contacts app and search for Team Lead"
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Action Execution Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (isRunning) {
                            Button(
                                onClick = { agentController.stopAgent("User cancelled task") },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("stop_agent_button"),
                                colors = ButtonDefaults.buttonColors(containerColor = CyberRed, contentColor = Color.White),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("STOP AGENT", fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                            }
                        } else {
                            Button(
                                onClick = {
                                    agentController.startTask(
                                        taskDescription = taskInput,
                                        preApprovedBatch = taskInput.lowercase().contains("whatsapp")
                                    )
                                },
                                enabled = taskInput.isNotBlank(),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("start_agent_button"),
                                colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = Color(0xFF001F28)),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("START AGENT", fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                            }
                        }

                        // Emergency Stop Button
                        OutlinedButton(
                            onClick = { agentController.emergencyStop() },
                            modifier = Modifier
                                .height(48.dp)
                                .testTag("emergency_stop_button"),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = CyberRed),
                            border = androidx.compose.foundation.BorderStroke(1.2.dp, CyberRed),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Dangerous, contentDescription = "Emergency Stop", tint = CyberRed, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("KILL", color = CyberRed, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        }
                    }
                }
            }
        }

        // Active / Planned Action Card
        item {
            CurrentActionCard(
                action = currentAction,
                lastExecuted = actionHistory.lastOrNull()
            )
        }

        // Real-Time Live Event Stream
        item {
            LiveEventStream(events = liveEvents)
        }

        // Observability & Telemetry Debug Panel
        item {
            AgentDebugPanel(
                metrics = debugMetrics,
                activeProvider = activeProvider,
                onSelectProvider = { providerType ->
                    when (providerType) {
                        AiProviderType.CACTUS_NEEDLE -> agentController.setProvider(agentController.cactusProvider)
                        AiProviderType.LOCAL_ENGINE -> agentController.setProvider(agentController.localProvider)
                        AiProviderType.GEMINI_CLOUD -> agentController.setProvider(agentController.geminiProvider)
                        AiProviderType.MOCK_SIMULATOR -> agentController.setProvider(agentController.mockProvider)
                    }
                }
            )
        }

        item {
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
private fun QuickTaskChip(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(CyberSurfaceVariant)
            .border(0.6.dp, Color(0xFF334155), RoundedCornerShape(6.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(
            text = label,
            color = CyberCyan,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace
        )
    }
}
