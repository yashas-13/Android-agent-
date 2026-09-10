package com.example.ui.screens

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Accessibility
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mouse
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PanTool
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.accessibility.PhoneAccessibilityService
import com.example.ui.components.CyberBadge
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
import kotlinx.coroutines.launch

@Composable
fun AccessibilityOnboardingScreen(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isConnected by PhoneAccessibilityService.isServiceConnected.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    var diagnosticMessage by remember { mutableStateOf<String?>(null) }
    var isExecutingTest by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(CyberBackground)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(4.dp))

            // Main Status Banner
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(CyberSurface)
                    .border(
                        1.dp,
                        if (isConnected) CyberGreen.copy(alpha = 0.4f) else CyberAmber.copy(alpha = 0.4f),
                        RoundedCornerShape(14.dp)
                    )
                    .padding(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(if (isConnected) CyberGreen.copy(alpha = 0.15f) else CyberAmber.copy(alpha = 0.15f))
                            .border(1.dp, if (isConnected) CyberGreen else CyberAmber, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isConnected) Icons.Default.CheckCircle else Icons.Default.Accessibility,
                            contentDescription = null,
                            tint = if (isConnected) CyberGreen else CyberAmber,
                            modifier = Modifier.size(26.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isConnected) "ACCESSIBILITY SERVICE ACTIVE" else "ACCESSIBILITY PERMISSION REQUIRED",
                            color = if (isConnected) CyberGreen else CyberAmber,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (isConnected)
                                "PhoneAccessibilityService is connected and ready to observe and execute authorized actions."
                            else
                                "Grant AccessibilityService permission to enable real screen interaction on your device.",
                            color = TextSecondary,
                            fontSize = 11.sp,
                            lineHeight = 15.sp
                        )
                    }
                }
            }
        }

        // Open Settings Button
        item {
            Button(
                onClick = {
                    val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(intent)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("open_accessibility_settings_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = CyberCyan,
                    contentColor = Color(0xFF001F28)
                ),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "OPEN ANDROID ACCESSIBILITY SETTINGS",
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp
                )
            }
        }

        // ACCESSIBILITY CAPABILITIES DIAGNOSTIC & TEST BENCH
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(CyberSurface)
                    .border(1.dp, CyberCyan.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                    .padding(14.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "CAPABILITIES DIAGNOSTIC & TEST BENCH",
                            color = CyberCyan,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 0.8.sp
                        )
                        CyberBadge(
                            text = if (isConnected) "VERIFIED" else "STANDBY",
                            color = if (isConnected) CyberGreen else CyberAmber
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Real-time verification of required interaction capabilities powered by Android Accessibility APIs:",
                        color = TextSecondary,
                        fontSize = 11.sp
                    )

                    if (diagnosticMessage != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(CyberSurfaceVariant)
                                .border(1.dp, CyberCyan.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                .padding(8.dp)
                        ) {
                            Text(
                                text = diagnosticMessage ?: "",
                                color = TextPrimary,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // 1. Clicking Capability
                    CapabilityBenchItem(
                        icon = Icons.Default.Mouse,
                        title = "1. CLICKING",
                        flag = "canRetrieveWindowContent / ACTION_CLICK",
                        description = "Dispatches semantic ACTION_CLICK to node hierarchy with coordinate center fallback.",
                        isServiceConnected = isConnected,
                        isBusy = isExecutingTest,
                        onTestExecute = {
                            scope.launch {
                                isExecutingTest = true
                                diagnosticMessage = "Executing test click..."
                                val service = PhoneAccessibilityService.instance
                                val res = service?.testExecuteClick(540f, 1000f) ?: false
                                diagnosticMessage = if (res) "CLICK test dispatched successfully ✓" else "CLICK failed: Enable service in settings."
                                isExecutingTest = false
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // 2. Typing Capability
                    CapabilityBenchItem(
                        icon = Icons.Default.Keyboard,
                        title = "2. TYPING",
                        flag = "ACTION_SET_TEXT + CLIPBOARD PASTE",
                        description = "Directly inputs text into active/editable fields with automatic clipboard fallback.",
                        isServiceConnected = isConnected,
                        isBusy = isExecutingTest,
                        onTestExecute = {
                            scope.launch {
                                isExecutingTest = true
                                diagnosticMessage = "Executing test type..."
                                val service = PhoneAccessibilityService.instance
                                val res = service?.testExecuteTypeText("AI Agent Test") ?: false
                                diagnosticMessage = if (res) "TYPE test completed ✓" else "TYPE test dispatched (no active editable node in focus)."
                                isExecutingTest = false
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // 3. Scrolling Capability
                    CapabilityBenchItem(
                        icon = Icons.Default.SwapVert,
                        title = "3. SCROLLING",
                        flag = "ACTION_SCROLL_FORWARD/BACKWARD",
                        description = "Performs node scroll on scrollable containers with gesture swipe fallback.",
                        isServiceConnected = isConnected,
                        isBusy = isExecutingTest,
                        onTestExecute = {
                            scope.launch {
                                isExecutingTest = true
                                diagnosticMessage = "Executing test scroll..."
                                val service = PhoneAccessibilityService.instance
                                val res = service?.testExecuteScroll("down") ?: false
                                diagnosticMessage = if (res) "SCROLL gesture executed successfully ✓" else "SCROLL failed: Service not bound."
                                isExecutingTest = false
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // 4. Screenshot Capture Capability
                    CapabilityBenchItem(
                        icon = Icons.Default.CameraAlt,
                        title = "4. SCREENSHOT CAPTURE",
                        flag = "canTakeScreenshot / API 30+",
                        description = "Captures hardware buffer display bitmap and generates base64 visual snapshot.",
                        isServiceConnected = isConnected,
                        isBusy = isExecutingTest,
                        onTestExecute = {
                            scope.launch {
                                isExecutingTest = true
                                diagnosticMessage = "Capturing screenshot..."
                                val service = PhoneAccessibilityService.instance
                                val res = service?.testExecuteScreenshot() ?: false
                                diagnosticMessage = if (res) "SCREENSHOT captured successfully ✓" else "SCREENSHOT failed."
                                isExecutingTest = false
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // 5. Tap Capability
                    CapabilityBenchItem(
                        icon = Icons.Default.TouchApp,
                        title = "5. TAP",
                        flag = "canPerformGestures / 80ms duration",
                        description = "Executes precise single finger touch tap at coordinates via GestureDescription.",
                        isServiceConnected = isConnected,
                        isBusy = isExecutingTest,
                        onTestExecute = {
                            scope.launch {
                                isExecutingTest = true
                                diagnosticMessage = "Executing test tap..."
                                val service = PhoneAccessibilityService.instance
                                val res = service?.testExecuteTap(540f, 1000f) ?: false
                                diagnosticMessage = if (res) "TAP gesture dispatched successfully ✓" else "TAP failed: Service not bound."
                                isExecutingTest = false
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // 6. Swipe Capability
                    CapabilityBenchItem(
                        icon = Icons.Default.PanTool,
                        title = "6. SWIPE",
                        flag = "canPerformGestures / Vector Stroke",
                        description = "Calculates motion vector path and dispatches gesture swipe (Up, Down, Left, Right).",
                        isServiceConnected = isConnected,
                        isBusy = isExecutingTest,
                        onTestExecute = {
                            scope.launch {
                                isExecutingTest = true
                                diagnosticMessage = "Executing test swipe..."
                                val service = PhoneAccessibilityService.instance
                                val res = service?.testExecuteSwipe("up") ?: false
                                diagnosticMessage = if (res) "SWIPE gesture dispatched successfully ✓" else "SWIPE failed: Service not bound."
                                isExecutingTest = false
                            }
                        }
                    )
                }
            }
        }

        // What the service does (Transparency & Permission Explanation)
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
                        text = "WHY ACCESSIBILITY PERMISSION IS NEEDED",
                        color = CyberCyan,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    CapabilityRow(
                        icon = Icons.Default.Visibility,
                        title = "1. Inspect Permitted UI Hierarchy",
                        desc = "Reads visible elements (text, buttons, input fields) in active apps so the AI agent can orient itself and understand screen state."
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    CapabilityRow(
                        icon = Icons.Default.PanTool,
                        title = "2. Interact With UI Elements",
                        desc = "Executes precise user-directed clicks, swipes, text entries, and scroll gestures to fulfill automation workflows."
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    CapabilityRow(
                        icon = Icons.Default.Security,
                        title = "3. Execute User-Requested Automation",
                        desc = "Automates tasks like navigating settings, interacting with apps, or broadcasting messages without manual repetition."
                    )
                }
            }
        }

        // Safety Guardrails Card
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
                        text = "SAFETY & POLICY GUARDRAILS",
                        color = CyberAmber,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    SafetyRow(
                        title = "Zero Password Access",
                        desc = "The policy engine strictly blocks the AI from interacting with password fields, PINs, OTP codes, or secret keys."
                    )

                    SafetyRow(
                        title = "Confirmation For High-Impact Actions",
                        desc = "Financial payments, account deletions, and sensitive operations require explicit pop-up approval before execution."
                    )

                    SafetyRow(
                        title = "Emergency Stop Kill Switch",
                        desc = "Tap 'KILL' at any second during automation to instantly terminate agent threads and cancel pending actions."
                    )

                    SafetyRow(
                        title = "Comprehensive Audit Log",
                        desc = "Every single tap, typed character, latency, and AI reasoning step is stored locally in an encrypted Room database."
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun CapabilityBenchItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    flag: String,
    description: String,
    isServiceConnected: Boolean,
    isBusy: Boolean,
    onTestExecute: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(CyberSurfaceVariant)
            .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(8.dp))
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
                        imageVector = icon,
                        contentDescription = null,
                        tint = CyberCyan,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = title,
                        color = TextPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                OutlinedButton(
                    onClick = onTestExecute,
                    enabled = isServiceConnected && !isBusy,
                    modifier = Modifier.height(28.dp),
                    shape = RoundedCornerShape(6.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = CyberCyan
                    )
                ) {
                    Text(
                        text = "TEST EXECUTE",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "FLAG: $flag",
                color = CyberGreen,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = description,
                color = TextSecondary,
                fontSize = 10.sp,
                lineHeight = 13.sp
            )
        }
    }
}

@Composable
private fun CapabilityRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    desc: String
) {
    Row(verticalAlignment = Alignment.Top) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = CyberCyan,
            modifier = Modifier
                .size(18.dp)
                .padding(top = 2.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Text(
                text = title,
                color = TextPrimary,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = FontFamily.Monospace
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = desc,
                color = TextSecondary,
                fontSize = 11.sp,
                lineHeight = 15.sp
            )
        }
    }
}

@Composable
private fun SafetyRow(title: String, desc: String) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(
            text = "• $title",
            color = CyberAmber,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = FontFamily.Monospace
        )
        Text(
            text = desc,
            color = TextSecondary,
            fontSize = 11.sp,
            lineHeight = 15.sp,
            modifier = Modifier.padding(start = 12.dp)
        )
    }
}

