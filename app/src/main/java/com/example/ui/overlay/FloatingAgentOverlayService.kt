package com.example.ui.overlay

import android.annotation.SuppressLint
import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.KeyboardReturn
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Swipe
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.VerticalAlignBottom
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.AiPhoneAgentApp
import com.example.MainActivity
import com.example.R
import com.example.agent.ActionTarget
import com.example.agent.AgentAction
import com.example.agent.AgentActionType
import com.example.agent.AgentController
import com.example.agent.AgentEvent
import com.example.agent.AgentEventType
import com.example.agent.AgentState
import com.example.ai.AiProviderType
import com.example.data.settings.AppSettings
import com.example.ui.components.CyberBadge
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class FloatingAgentOverlayService : Service() {

    private lateinit var windowManager: WindowManager
    private lateinit var lifecycleOwner: ServiceLifecycleOwner
    private var overlayComposeView: ComposeView? = null
    private lateinit var layoutParams: WindowManager.LayoutParams

    private var isExpandedState by mutableStateOf(false)
    private var showSettingsState by mutableStateOf(false)

    private var initialX = 0
    private var initialY = 0
    private var initialTouchX = 0f
    private var initialTouchY = 0f
    private var isDragging = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        lifecycleOwner = ServiceLifecycleOwner().apply {
            onCreate()
            onStart()
        }

        startForegroundNotification()
        setupOverlayView()
        OverlayHelper.updateServiceRunning(true)
    }

    private fun startForegroundNotification() {
        val notificationIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            notificationIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification: Notification = NotificationCompat.Builder(this, NotificationHelper.CHANNEL_ID_OVERLAY)
            .setContentTitle("Cactus Needle Phone Agent Active")
            .setContentText("Floating overlay HUD monitoring system tasks")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        startForeground(NOTIFICATION_ID, notification)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupOverlayView() {
        val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        layoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 24
            y = 260
        }

        overlayComposeView = ComposeView(this).apply {
            lifecycleOwner.attachToView(this)
            setContent {
                val app = AiPhoneAgentApp.instance
                val agentController = app.agentController
                val appSettings = app.appSettings

                FloatingOverlayContent(
                    agentController = agentController,
                    appSettings = appSettings,
                    isExpanded = isExpandedState,
                    showSettings = showSettingsState,
                    onToggleExpand = { toggleExpand() },
                    onToggleSettings = { showSettingsState = !showSettingsState },
                    onCloseService = {
                        stopSelf()
                    },
                    onDragDelta = { dx, dy ->
                        val p = this@FloatingAgentOverlayService.layoutParams
                        p.x = (p.x + dx.toInt()).coerceAtLeast(0)
                        p.y = (p.y + dy.toInt()).coerceAtLeast(0)
                        windowManager.updateViewLayout(this@apply, p)
                    }
                )
            }
        }

        windowManager.addView(overlayComposeView, layoutParams)
    }

    private fun toggleExpand() {
        isExpandedState = !isExpandedState
        if (isExpandedState) {
            // Allow typing in HUD textfield
            layoutParams.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH
            layoutParams.width = WindowManager.LayoutParams.MATCH_PARENT
            layoutParams.height = WindowManager.LayoutParams.WRAP_CONTENT
        } else {
            showSettingsState = false
            layoutParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
            layoutParams.width = WindowManager.LayoutParams.WRAP_CONTENT
            layoutParams.height = WindowManager.LayoutParams.WRAP_CONTENT
        }
        overlayComposeView?.let { windowManager.updateViewLayout(it, layoutParams) }
    }

    override fun onDestroy() {
        super.onDestroy()
        OverlayHelper.updateServiceRunning(false)
        overlayComposeView?.let {
            try {
                windowManager.removeView(it)
            } catch (e: Exception) {
                // Ignore
            }
        }
        lifecycleOwner.onDestroy()
    }

    companion object {
        private const val NOTIFICATION_ID = 2048
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FloatingOverlayContent(
    agentController: AgentController,
    appSettings: AppSettings,
    isExpanded: Boolean,
    showSettings: Boolean,
    onToggleExpand: () -> Unit,
    onToggleSettings: () -> Unit,
    onCloseService: () -> Unit,
    onDragDelta: (Float, Float) -> Unit
) {
    val agentState by agentController.state.collectAsStateWithLifecycle()
    val currentTask by agentController.currentTask.collectAsStateWithLifecycle()
    val currentAction by agentController.currentAction.collectAsStateWithLifecycle()
    val latestSnapshot by agentController.latestSnapshot.collectAsStateWithLifecycle()
    val liveEvents by agentController.liveEvents.collectAsStateWithLifecycle()
    val settingsState by appSettings.settings.collectAsStateWithLifecycle()

    val stateColor = when (agentState) {
        AgentState.IDLE -> CyberGreen
        AgentState.OBSERVING, AgentState.UNDERSTANDING -> CyberCyan
        AgentState.PLANNING -> Color(0xFF64B5F6)
        AgentState.EXECUTING -> CyberAmber
        AgentState.WAITING_FOR_CONFIRMATION -> CyberAmber
        AgentState.VERIFYING -> Color(0xFF80CBC4)
        AgentState.SUCCESS -> CyberGreen
        AgentState.FAILED -> CyberRed
        AgentState.STOPPED -> TextMuted
        AgentState.RECOVERING -> Color(0xFFFF80AB)
    }

    if (!isExpanded) {
        // --- COMPACT FLOATING ICON MODE ---
        Box(
            modifier = Modifier
                .padding(8.dp)
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        onDragDelta(dragAmount.x, dragAmount.y)
                    }
                }
                .clip(RoundedCornerShape(28.dp))
                .background(CyberSurface.copy(alpha = settingsState.overlayTransparency))
                .border(2.dp, stateColor, RoundedCornerShape(28.dp))
                .clickable { onToggleExpand() }
                .padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(stateColor.copy(alpha = 0.2f))
                        .border(1.5.dp, stateColor, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Psychology,
                        contentDescription = "Cactus Needle Agent",
                        tint = stateColor,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "NEEDLE",
                            color = CyberCyan,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(stateColor)
                        )
                    }

                    Text(
                        text = if (agentState == AgentState.IDLE) "Ready" else agentState.name,
                        color = TextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = FontFamily.Monospace,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    } else {
        // --- FULL EXPANDED HUD MODE (VERBOSE EXECUTION & OVERLAY SETTINGS) ---
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp)
                .heightIn(max = 640.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = CyberBackground.copy(alpha = settingsState.overlayTransparency)
            ),
            border = androidx.compose.foundation.BorderStroke(1.5.dp, CyberCyan.copy(alpha = 0.8f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                // Header Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(CyberCyan.copy(alpha = 0.2f))
                                .border(1.dp, CyberCyan, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Psychology,
                                contentDescription = null,
                                tint = CyberCyan,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        Column {
                            Text(
                                text = "CACTUS NEEDLE AI HUD",
                                color = CyberCyan,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "App: ${latestSnapshot?.packageName?.substringAfterLast('.') ?: "system"}",
                                color = TextMuted,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CyberBadge(
                            text = agentState.name,
                            color = stateColor
                        )

                        IconButton(
                            onClick = onToggleSettings,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Overlay Settings",
                                tint = if (showSettings) CyberCyan else TextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        IconButton(
                            onClick = onToggleExpand,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Minimize HUD",
                                tint = TextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 8.dp),
                    color = CyberSurface
                )

                // OVERLAY SETTINGS PANEL (Collapsible)
                AnimatedVisibility(visible = showSettings) {
                    OverlaySettingsPanel(
                        appSettings = appSettings,
                        agentController = agentController
                    )
                }

                // QUICK CAPABILITY ACTIONS (Screenshot, Tap, Swipe, Next, Minimize, Send, Enter)
                Text(
                    text = "CAPABILITY ACTIONS",
                    color = TextMuted,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.height(4.dp))

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    QuickActionButton(
                        label = "Screenshot",
                        icon = Icons.Default.CameraAlt,
                        color = CyberCyan
                    ) {
                        agentController.executeDirectAction(
                            AgentAction(
                                action = AgentActionType.SCREENSHOT,
                                reason = "User invoked screenshot capture via overlay"
                            )
                        )
                    }

                    QuickActionButton(
                        label = "Tap",
                        icon = Icons.Default.TouchApp,
                        color = CyberGreen
                    ) {
                        agentController.executeDirectAction(
                            AgentAction(
                                action = AgentActionType.TAP,
                                target = ActionTarget(x = 540f, y = 1200f),
                                reason = "User invoked screen tap at center"
                            )
                        )
                    }

                    QuickActionButton(
                        label = "Swipe",
                        icon = Icons.Default.Swipe,
                        color = Color(0xFF64B5F6)
                    ) {
                        agentController.executeDirectAction(
                            AgentAction(
                                action = AgentActionType.SWIPE,
                                payload = "up",
                                reason = "User invoked swipe gesture via overlay"
                            )
                        )
                    }

                    QuickActionButton(
                        label = "Next",
                        icon = Icons.AutoMirrored.Filled.ArrowForward,
                        color = CyberAmber
                    ) {
                        agentController.executeDirectAction(
                            AgentAction(
                                action = AgentActionType.NEXT,
                                reason = "User invoked Next field focus"
                            )
                        )
                    }

                    QuickActionButton(
                        label = "Minimize",
                        icon = Icons.Default.VerticalAlignBottom,
                        color = Color(0xFFCE93D8)
                    ) {
                        agentController.executeDirectAction(
                            AgentAction(
                                action = AgentActionType.MINIMIZE,
                                reason = "User invoked Minimize/Home action"
                            )
                        )
                    }

                    QuickActionButton(
                        label = "Send",
                        icon = Icons.AutoMirrored.Filled.Send,
                        color = CyberGreen
                    ) {
                        agentController.executeDirectAction(
                            AgentAction(
                                action = AgentActionType.SEND,
                                reason = "User invoked Send action via overlay"
                            )
                        )
                    }

                    QuickActionButton(
                        label = "Enter",
                        icon = Icons.Default.KeyboardReturn,
                        color = Color(0xFF80CBC4)
                    ) {
                        agentController.executeDirectAction(
                            AgentAction(
                                action = AgentActionType.ENTER_KEY,
                                reason = "User invoked Enter Key action"
                            )
                        )
                    }

                    if (agentState != AgentState.IDLE && agentState != AgentState.STOPPED) {
                        QuickActionButton(
                            label = "Stop Agent",
                            icon = Icons.Default.Stop,
                            color = CyberRed
                        ) {
                            agentController.stopAgent("User stopped agent from floating HUD")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // CURRENT TASK OR NEW TASK DISPATCH
                var taskInput by remember { mutableStateOf("") }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    OutlinedTextField(
                        value = taskInput,
                        onValueChange = { taskInput = it },
                        placeholder = {
                            Text(
                                text = if (currentTask.isNotEmpty()) currentTask else "Enter AI task (e.g. Turn on Wi-Fi)",
                                color = TextMuted,
                                fontSize = 11.sp,
                                maxLines = 1
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyberCyan,
                            unfocusedBorderColor = CyberSurface,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        shape = RoundedCornerShape(8.dp),
                        singleLine = true
                    )

                    Button(
                        onClick = {
                            if (taskInput.isNotBlank()) {
                                agentController.startTask(taskInput)
                                taskInput = ""
                            }
                        },
                        enabled = taskInput.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = CyberCyan),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(44.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Execute Task",
                            tint = CyberBackground,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // CURRENT ACTION DISPLAY
                currentAction?.let { act ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(CyberSurface)
                            .border(1.dp, CyberAmber.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                            .padding(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CyberBadge(text = act.action.name, color = CyberAmber)
                            Text(
                                text = act.target?.summary() ?: act.payload ?: act.reason,
                                color = TextPrimary,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                }

                // VERBOSE TASKS EXECUTION STREAM
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "VERBOSE TASKS EXECUTION STREAM (${liveEvents.size})",
                        color = CyberCyan,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )

                    Text(
                        text = "Auto-scroll ON",
                        color = CyberGreen,
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

                val listState = rememberLazyListState()
                LaunchedEffect(liveEvents.size) {
                    if (liveEvents.isNotEmpty()) {
                        listState.animateScrollToItem(liveEvents.lastIndex)
                    }
                }

                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 100.dp, max = 220.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(CyberSurface.copy(alpha = 0.6f))
                        .padding(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (liveEvents.isEmpty()) {
                        item {
                            Text(
                                text = "Awaiting task dispatch. Events will stream here in real-time.",
                                color = TextMuted,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                    } else {
                        items(liveEvents, key = { it.id }) { event ->
                            VerboseEventRow(event)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun QuickActionButton(
    label: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(color.copy(alpha = 0.15f))
            .border(1.dp, color.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 5.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = color,
                modifier = Modifier.size(13.dp)
            )
            Text(
                text = label,
                color = TextPrimary,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

@Composable
fun VerboseEventRow(event: AgentEvent) {
    val typeColor = when (event.type) {
        AgentEventType.OBSERVE -> CyberCyan
        AgentEventType.UNDERSTAND -> Color(0xFF64B5F6)
        AgentEventType.PLAN -> Color(0xFFCE93D8)
        AgentEventType.ACT -> CyberAmber
        AgentEventType.VERIFY -> Color(0xFF80CBC4)
        AgentEventType.COMPLETE -> CyberGreen
        AgentEventType.CONFIRM -> CyberAmber
        AgentEventType.RECOVER -> Color(0xFFFF80AB)
        AgentEventType.ERROR -> CyberRed
    }

    val timeStr = remember(event.timestamp) {
        SimpleDateFormat("HH:mm:ss.SS", Locale.US).format(Date(event.timestamp))
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = timeStr,
            color = TextMuted,
            fontSize = 9.sp,
            fontFamily = FontFamily.Monospace
        )

        Text(
            text = "[${event.type.name}]",
            color = typeColor,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = event.title,
                color = TextPrimary,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = FontFamily.Monospace
            )
            if (event.details.isNotBlank()) {
                Text(
                    text = event.details,
                    color = TextSecondary,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        event.badge?.let { badge ->
            CyberBadge(text = badge, color = typeColor)
        }
    }
}

@Composable
fun OverlaySettingsPanel(
    appSettings: AppSettings,
    agentController: AgentController
) {
    val settingsState by appSettings.settings.collectAsStateWithLifecycle()
    val activeProvider by agentController.activeProvider.collectAsStateWithLifecycle()

    var endpointInput by remember(settingsState.cactusEndpoint) {
        mutableStateOf(settingsState.cactusEndpoint)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        colors = CardDefaults.cardColors(containerColor = CyberSurface),
        shape = RoundedCornerShape(10.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, CyberCyan.copy(alpha = 0.4f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "OVERLAY & AI SETTINGS",
                    color = CyberCyan,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )

                Text(
                    text = "Default: https://cactuscompute.com/needle",
                    color = TextMuted,
                    fontSize = 8.sp,
                    fontFamily = FontFamily.Monospace
                )
            }

            // Cactus Needle Endpoint Input
            Text(
                text = "Cactus Needle Server Endpoint:",
                color = TextSecondary,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                OutlinedTextField(
                    value = endpointInput,
                    onValueChange = {
                        endpointInput = it
                        appSettings.updateCactusEndpoint(it)
                        agentController.cactusProvider.setEndpoint(it)
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(42.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CyberCyan,
                        unfocusedBorderColor = CyberBackground,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    shape = RoundedCornerShape(6.dp),
                    singleLine = true,
                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                )

                IconButton(
                    onClick = {
                        val def = "https://cactuscompute.com/needle"
                        endpointInput = def
                        appSettings.updateCactusEndpoint(def)
                        agentController.cactusProvider.setEndpoint(def)
                    },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Reset Endpoint",
                        tint = CyberCyan,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Provider Selector
            Text(
                text = "AI Model Engine:",
                color = TextSecondary,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Cactus Needle (Default)
                val isCactus = activeProvider.providerType == AiProviderType.CACTUS_NEEDLE
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isCactus) CyberCyan.copy(alpha = 0.25f) else CyberBackground)
                        .border(1.dp, if (isCactus) CyberCyan else TextMuted, RoundedCornerShape(6.dp))
                        .clickable {
                            agentController.setProvider(agentController.cactusProvider)
                            appSettings.updateActiveAiProvider("CACTUS_NEEDLE")
                        }
                        .padding(vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Cactus Needle",
                        color = if (isCactus) CyberCyan else TextMuted,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                // Local Semantic
                val isLocal = activeProvider.providerType == AiProviderType.LOCAL_ENGINE
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isLocal) CyberGreen.copy(alpha = 0.25f) else CyberBackground)
                        .border(1.dp, if (isLocal) CyberGreen else TextMuted, RoundedCornerShape(6.dp))
                        .clickable {
                            agentController.setProvider(agentController.localProvider)
                            appSettings.updateActiveAiProvider("LOCAL_ENGINE")
                        }
                        .padding(vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Local Model",
                        color = if (isLocal) CyberGreen else TextMuted,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                // Gemini Cloud
                val isGemini = activeProvider.providerType == AiProviderType.GEMINI_CLOUD
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isGemini) CyberAmber.copy(alpha = 0.25f) else CyberBackground)
                        .border(1.dp, if (isGemini) CyberAmber else TextMuted, RoundedCornerShape(6.dp))
                        .clickable {
                            agentController.setProvider(agentController.geminiProvider)
                            appSettings.updateActiveAiProvider("GEMINI_CLOUD")
                        }
                        .padding(vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Gemini Cloud",
                        color = if (isGemini) CyberAmber else TextMuted,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            // Transparency Slider
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "HUD Opacity: ${(settingsState.overlayTransparency * 100).toInt()}%",
                    color = TextSecondary,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )

                Slider(
                    value = settingsState.overlayTransparency,
                    onValueChange = { appSettings.updateOverlayTransparency(it) },
                    valueRange = 0.5f..1.0f,
                    modifier = Modifier
                        .width(160.dp)
                        .height(24.dp),
                    colors = SliderDefaults.colors(
                        thumbColor = CyberCyan,
                        activeTrackColor = CyberCyan,
                        inactiveTrackColor = CyberBackground
                    )
                )
            }
        }
    }
}
