package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessibilityNew
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.accessibility.PhoneAccessibilityService
import com.example.agent.AgentController
import com.example.ai.AiProviderType
import com.example.data.settings.AppSettings
import com.example.ui.components.CyberBadge
import com.example.ui.overlay.OverlayHelper
import com.example.ui.theme.CyberAmber
import com.example.ui.theme.CyberBackground
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.CyberGreen
import com.example.ui.theme.CyberRed
import com.example.ui.theme.CyberSurface
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.util.NotificationHelper

@Composable
fun SettingsAndPermissionsScreen(
    appSettings: AppSettings,
    agentController: AgentController,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isServiceConnected by PhoneAccessibilityService.isServiceConnected.collectAsStateWithLifecycle()
    val settingsState by appSettings.settings.collectAsStateWithLifecycle()

    var areNotificationsEnabled by remember {
        mutableStateOf(NotificationManagerCompat.from(context).areNotificationsEnabled())
    }

    val notificationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        areNotificationsEnabled = granted
    }

    var hasOverlayPermission by remember {
        mutableStateOf(OverlayHelper.canDrawOverlays(context))
    }
    val isOverlayRunning by OverlayHelper.isOverlayRunning.collectAsStateWithLifecycle()

    var cactusEndpointInput by remember(settingsState.cactusEndpoint) {
        mutableStateOf(settingsState.cactusEndpoint)
    }
    var cactusApiKeyInput by remember(settingsState.cactusApiKey) {
        mutableStateOf(settingsState.cactusApiKey)
    }

    var apiKeyInput by remember(settingsState.customGeminiApiKey) {
        mutableStateOf(settingsState.customGeminiApiKey)
    }

    LaunchedEffect(Unit) {
        hasOverlayPermission = OverlayHelper.canDrawOverlays(context)
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = null,
                    tint = CyberCyan,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "SETTINGS & PERMISSIONS ACCESS",
                    color = TextPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 0.5.sp
                )
            }
        }

        // --- 1. PERMISSIONS ACCESS SECTION ---
        item {
            SettingsCard(title = "PERMISSIONS & HARDWARE ACCESS", icon = Icons.Default.Security) {
                // Accessibility Service Status
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(CyberBackground)
                        .border(
                            0.8.dp,
                            if (isServiceConnected) CyberGreen.copy(alpha = 0.5f) else CyberAmber.copy(alpha = 0.5f),
                            RoundedCornerShape(8.dp)
                        )
                        .padding(10.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (isServiceConnected) Icons.Default.CheckCircle else Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = if (isServiceConnected) CyberGreen else CyberAmber,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Accessibility Service",
                                    color = TextPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            CyberBadge(
                                text = if (isServiceConnected) "ACTIVE" else "DISABLED",
                                color = if (isServiceConnected) CyberGreen else CyberAmber
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Permits the agent to inspect WhatsApp chat elements, navigate group lists, and execute automated actions.",
                            color = TextSecondary,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            lineHeight = 14.sp
                        )

                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = {
                                val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                                context.startActivity(intent)
                            },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = CyberCyan),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.fillMaxWidth().testTag("open_accessibility_settings_btn")
                        ) {
                            Icon(Icons.Default.AccessibilityNew, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Open Accessibility Settings", fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Notification Permission Status
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(CyberBackground)
                        .border(
                            0.8.dp,
                            if (areNotificationsEnabled) CyberGreen.copy(alpha = 0.5f) else CyberAmber.copy(alpha = 0.5f),
                            RoundedCornerShape(8.dp)
                        )
                        .padding(10.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (areNotificationsEnabled) Icons.Default.NotificationsActive else Icons.Default.Notifications,
                                    contentDescription = null,
                                    tint = if (areNotificationsEnabled) CyberGreen else CyberAmber,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Broadcast Notifications",
                                    color = TextPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            CyberBadge(
                                text = if (areNotificationsEnabled) "ALLOWED" else "BLOCKED",
                                color = if (areNotificationsEnabled) CyberGreen else CyberAmber
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Notifies you immediately if any group delivery fails with a clear root-cause reason and report option.",
                            color = TextSecondary,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            lineHeight = 14.sp
                        )

                        if (!areNotificationsEnabled) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                        notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    } else {
                                        val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                                            putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                                        }
                                        context.startActivity(intent)
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = Color(0xFF001F28)),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Enable Notifications", fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Floating Overlay (System Alert Window) Permission Status
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(CyberBackground)
                        .border(
                            0.8.dp,
                            if (hasOverlayPermission) CyberGreen.copy(alpha = 0.5f) else CyberAmber.copy(alpha = 0.5f),
                            RoundedCornerShape(8.dp)
                        )
                        .padding(10.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (hasOverlayPermission) Icons.Default.CheckCircle else Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = if (hasOverlayPermission) CyberGreen else CyberAmber,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Floating Window / Overlay",
                                    color = TextPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            CyberBadge(
                                text = if (hasOverlayPermission) "GRANTED" else "REQUIRED",
                                color = if (hasOverlayPermission) CyberGreen else CyberAmber
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Required for the floating agent icon and real-time verbose tasks execution HUD to display over any active app.",
                            color = TextSecondary,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            lineHeight = 14.sp
                        )

                        if (!hasOverlayPermission) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = {
                                    OverlayHelper.openOverlaySettings(context)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = Color(0xFF001F28)),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Grant Overlay Permission", fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                            }
                        }
                    }
                }
            }
        }

        // --- 2. FLOATING AGENT OVERLAY & HUD SETTINGS ---
        item {
            SettingsCard(title = "FLOATING AGENT OVERLAY & HUD", icon = Icons.Default.Layers) {
                // Overlay Service Launch / Stop Controls
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Floating Agent Overlay HUD",
                            color = TextPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Draggable icon showing live verbose tasks execution and quick actions (Screenshot, Tap, Swipe, Next, Minimize, Send, Enter).",
                            color = TextMuted,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    CyberBadge(
                        text = if (isOverlayRunning) "ACTIVE" else "STOPPED",
                        color = if (isOverlayRunning) CyberGreen else TextMuted
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            if (!hasOverlayPermission) {
                                OverlayHelper.openOverlaySettings(context)
                            } else {
                                OverlayHelper.startOverlayService(context)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CyberCyan, contentColor = Color(0xFF001F28)),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (isOverlayRunning) "Relaunch Overlay" else "Launch Floating Icon", fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    }

                    if (isOverlayRunning) {
                        OutlinedButton(
                            onClick = {
                                OverlayHelper.stopOverlayService(context)
                            },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = CyberRed),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Icon(Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Stop Overlay", fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Toggle: Enable Floating Icon Feature
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "Enable Floating Agent Feature", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text(text = "Permit floating icon during agent tasks", color = TextMuted, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                    }
                    Switch(
                        checked = settingsState.overlayFloatingIconEnabled,
                        onCheckedChange = { appSettings.updateOverlayFloatingIconEnabled(it) },
                        colors = SwitchDefaults.colors(checkedThumbColor = CyberCyan, checkedTrackColor = CyberCyan.copy(alpha = 0.4f))
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Toggle: Verbose Task Logging
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "Verbose Task Execution Stream", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text(text = "Display micro-step logs [OBSERVE, UNDERSTAND, PLAN, ACT, VERIFY] on HUD", color = TextMuted, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                    }
                    Switch(
                        checked = settingsState.overlayVerboseLogging,
                        onCheckedChange = { appSettings.updateOverlayVerboseLogging(it) },
                        colors = SwitchDefaults.colors(checkedThumbColor = CyberCyan, checkedTrackColor = CyberCyan.copy(alpha = 0.4f))
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Slider: Overlay Transparency
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "HUD Panel Opacity", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text(text = "${(settingsState.overlayTransparency * 100).toInt()}%", color = CyberCyan, fontSize = 12.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                    }
                    Slider(
                        value = settingsState.overlayTransparency,
                        onValueChange = { appSettings.updateOverlayTransparency(it) },
                        valueRange = 0.5f..1.0f,
                        colors = SliderDefaults.colors(
                            thumbColor = CyberCyan,
                            activeTrackColor = CyberCyan,
                            inactiveTrackColor = Color(0xFF334155)
                        )
                    )
                }
            }
        }

        // --- 3. BROADCAST ENGINE SETTINGS ---
        item {
            SettingsCard(title = "BROADCAST ENGINE CONFIGURATION", icon = Icons.Default.Speed) {
                // Inter-group delay
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Delay Between Groups",
                            color = TextPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${settingsState.delayBetweenGroupsSeconds}s",
                            color = CyberCyan,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Slider(
                        value = settingsState.delayBetweenGroupsSeconds.toFloat(),
                        onValueChange = {
                            appSettings.updateDelayBetweenGroups(it.toInt())
                        },
                        valueRange = 1f..10f,
                        steps = 8,
                        colors = SliderDefaults.colors(
                            thumbColor = CyberCyan,
                            activeTrackColor = CyberCyan,
                            inactiveTrackColor = Color(0xFF334155)
                        )
                    )
                    Text(
                        text = "Allows WhatsApp UI animations to settle and prevents spam rate-limits.",
                        color = TextMuted,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Max Retries
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Automatic Retry Attempts",
                            color = TextPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${settingsState.autoRetryCount} retries",
                            color = CyberCyan,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Slider(
                        value = settingsState.autoRetryCount.toFloat(),
                        onValueChange = {
                            appSettings.updateAutoRetryCount(it.toInt())
                        },
                        valueRange = 0f..3f,
                        steps = 2,
                        colors = SliderDefaults.colors(
                            thumbColor = CyberCyan,
                            activeTrackColor = CyberCyan,
                            inactiveTrackColor = Color(0xFF334155)
                        )
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Toggle: Notify on Failure
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "Alert on Delivery Failure", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text(text = "Trigger high-priority notification with error reason", color = TextMuted, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                    }
                    Switch(
                        checked = settingsState.notifyOnFailure,
                        onCheckedChange = {
                            appSettings.updateNotifyOnFailure(it)
                        },
                        colors = SwitchDefaults.colors(checkedThumbColor = CyberCyan, checkedTrackColor = CyberCyan.copy(alpha = 0.4f))
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Toggle: Notify on Completion
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "Alert on Broadcast Completion", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text(text = "Show summary notification when all groups finish", color = TextMuted, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                    }
                    Switch(
                        checked = settingsState.notifyOnCompletion,
                        onCheckedChange = {
                            appSettings.updateNotifyOnCompletion(it)
                        },
                        colors = SwitchDefaults.colors(checkedThumbColor = CyberCyan, checkedTrackColor = CyberCyan.copy(alpha = 0.4f))
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Toggle: Batch Pre-Approval
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "Pre-Approve Group Broadcasts", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text(text = "Avoid manual confirmation popup on each individual group send", color = TextMuted, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                    }
                    Switch(
                        checked = settingsState.preApproveBatch,
                        onCheckedChange = {
                            appSettings.updatePreApproveBatch(it)
                        },
                        colors = SwitchDefaults.colors(checkedThumbColor = CyberCyan, checkedTrackColor = CyberCyan.copy(alpha = 0.4f))
                    )
                }
            }
        }

        // --- 3. AI PROVIDER CONFIGURATION ---
        item {
            SettingsCard(title = "AI ENGINE & REASONING PROVIDER", icon = Icons.Default.Psychology) {
                Text(
                    text = "Select reasoning provider for screen understanding and UI action planning:",
                    color = TextSecondary,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )

                Spacer(modifier = Modifier.height(8.dp))

                val providers = listOf(
                    agentController.cactusProvider,
                    agentController.localProvider,
                    agentController.geminiProvider,
                    agentController.mockProvider
                )
                val currentActive by agentController.activeProvider.collectAsStateWithLifecycle()

                providers.forEach { p ->
                    val isSelected = currentActive.providerType == p.providerType
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) CyberCyan.copy(alpha = 0.1f) else CyberBackground)
                            .border(
                                0.8.dp,
                                if (isSelected) CyberCyan else Color(0xFF1E293B),
                                RoundedCornerShape(8.dp)
                            )
                            .clickable {
                                agentController.setProvider(p)
                                appSettings.updateActiveAiProvider(p.providerType.name)
                            }
                            .padding(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = p.displayName,
                                    color = if (isSelected) CyberCyan else TextPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = when (p.providerType) {
                                        AiProviderType.CACTUS_NEEDLE -> "Default autonomous AI model (https://cactuscompute.com/needle) with multimodal screen action capabilities"
                                        AiProviderType.LOCAL_ENGINE -> "Fast, deterministic, 100% offline on-device execution"
                                        AiProviderType.GEMINI_CLOUD -> "Multimodal vision + semantic reasoning via Gemini Cloud"
                                        AiProviderType.MOCK_SIMULATOR -> "Predictable testing fixture for CI/CD and verification"
                                    },
                                    color = TextMuted,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            if (isSelected) {
                                CyberBadge(text = "ACTIVE", color = CyberCyan)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Cactus Needle Endpoint Configuration
                Text(
                    text = "Cactus Needle Server Endpoint (Default):",
                    color = TextPrimary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    OutlinedTextField(
                        value = cactusEndpointInput,
                        onValueChange = {
                            cactusEndpointInput = it
                            appSettings.updateCactusEndpoint(it)
                            agentController.cactusProvider.setEndpoint(it)
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyberCyan,
                            unfocusedBorderColor = Color(0xFF334155),
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                    )

                    IconButton(
                        onClick = {
                            val def = "https://cactuscompute.com/needle"
                            cactusEndpointInput = def
                            appSettings.updateCactusEndpoint(def)
                            agentController.cactusProvider.setEndpoint(def)
                        },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Reset to default endpoint",
                            tint = CyberCyan,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = cactusApiKeyInput,
                    onValueChange = {
                        cactusApiKeyInput = it
                        appSettings.updateCactusApiKey(it)
                        agentController.cactusProvider.setApiKey(it)
                    },
                    label = { Text("Cactus Needle API Key / Bearer Token (Optional)", fontSize = 11.sp) },
                    placeholder = { Text("Optional authorization token", fontSize = 11.sp, color = TextMuted) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CyberCyan,
                        unfocusedBorderColor = Color(0xFF334155),
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = apiKeyInput,
                    onValueChange = {
                        apiKeyInput = it
                        appSettings.updateCustomGeminiApiKey(it)
                    },
                    label = { Text("Gemini Cloud API Key (Optional)", fontSize = 11.sp) },
                    placeholder = { Text("AIzaSy...", fontSize = 11.sp, color = TextMuted) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CyberCyan,
                        unfocusedBorderColor = Color(0xFF334155),
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // --- 4. ERROR REPORTING & SYSTEM DIAGNOSTICS ---
        item {
            SettingsCard(title = "ERROR HANDLING & DIAGNOSTIC REPORTING", icon = Icons.Default.Share) {
                Text(
                    text = "If message broadcasting fails for any group, generate and share a diagnostic report to identify the root cause.",
                    color = TextSecondary,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    lineHeight = 14.sp
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = {
                            val report = agentController.generateDiagnosticReport()
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT, "AI Phone Agent Full Diagnostic Log")
                                putExtra(Intent.EXTRA_TEXT, report)
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Share Diagnostics"))
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = CyberCyan),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("export_diagnostics_button")
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Export Report", fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    OutlinedButton(
                        onClick = {
                            NotificationHelper.showBroadcastFailure(
                                context = context,
                                title = "Test Failure Alert",
                                failedCount = 1,
                                reason = "Simulated diagnostic test. Notification channel verified."
                            )
                            Toast.makeText(context, "Test failure alert dispatched", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = CyberAmber),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Notifications, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Test Alert", fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun SettingsCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(CyberSurface)
            .border(0.8.dp, Color(0xFF1E293B), RoundedCornerShape(12.dp))
            .padding(14.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = CyberCyan,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    color = CyberCyan,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 0.5.sp
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            content()
        }
    }
}
