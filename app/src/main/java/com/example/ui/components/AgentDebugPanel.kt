package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Icon
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.agent.AgentDebugMetrics
import com.example.ai.AiProvider
import com.example.ai.AiProviderType
import com.example.ui.theme.CyberAmber
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.CyberGreen
import com.example.ui.theme.CyberRed
import com.example.ui.theme.CyberSurface
import com.example.ui.theme.CyberSurfaceVariant
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun AgentDebugPanel(
    metrics: AgentDebugMetrics,
    activeProvider: AiProvider,
    onSelectProvider: (AiProviderType) -> Unit,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(CyberSurface)
            .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(12.dp))
            .testTag("agent_debug_panel")
    ) {
        // Toggle bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { isExpanded = !isExpanded }
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Analytics,
                    contentDescription = "Debug Metrics",
                    tint = CyberCyan,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "AGENT OBSERVABILITY TELEMETRY",
                    color = TextPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                CyberBadge(
                    text = "${(metrics.confidence * 100).toInt()}% conf",
                    color = if (metrics.confidence > 0.9f) CyberGreen else CyberAmber
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = "Toggle Panel",
                    tint = TextSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        AnimatedVisibility(visible = isExpanded) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                // Provider Selector
                Text(
                    text = "AI REASONING ENGINE:",
                    color = TextMuted,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ProviderChip(
                        label = "Local (Offline)",
                        selected = activeProvider.providerType == AiProviderType.LOCAL_ENGINE,
                        onClick = { onSelectProvider(AiProviderType.LOCAL_ENGINE) }
                    )
                    ProviderChip(
                        label = "Gemini 3.5 Flash",
                        selected = activeProvider.providerType == AiProviderType.GEMINI_CLOUD,
                        onClick = { onSelectProvider(AiProviderType.GEMINI_CLOUD) }
                    )
                    ProviderChip(
                        label = "Mock (Sim)",
                        selected = activeProvider.providerType == AiProviderType.MOCK_SIMULATOR,
                        onClick = { onSelectProvider(AiProviderType.MOCK_SIMULATOR) }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Telemetry Metrics Grid
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MetricCard(title = "AI LATENCY", value = "${metrics.aiLatencyMs}ms", color = CyberCyan, modifier = Modifier.weight(1f))
                    MetricCard(title = "ACTION LATENCY", value = "${metrics.actionLatencyMs}ms", color = CyberGreen, modifier = Modifier.weight(1f))
                    MetricCard(title = "VERIFY LATENCY", value = "${metrics.verificationLatencyMs}ms", color = Color(0xFF38BDF8), modifier = Modifier.weight(1f))
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MetricCard(title = "ACTIONS", value = "${metrics.totalActions}", color = TextPrimary, modifier = Modifier.weight(1f))
                    MetricCard(title = "FAILURES", value = "${metrics.failures}", color = if (metrics.failures > 0) CyberRed else TextSecondary, modifier = Modifier.weight(1f))
                    MetricCard(title = "RECOVERIES", value = "${metrics.recoveryAttempts}", color = if (metrics.recoveryAttempts > 0) CyberAmber else TextSecondary, modifier = Modifier.weight(1f))
                    MetricCard(title = "UI NODES", value = "${metrics.accessibilityNodeCount}", color = CyberCyan, modifier = Modifier.weight(1f))
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "PACKAGE: ${metrics.currentPackage.ifEmpty { "system" }}",
                        color = TextMuted,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "STATE: ${metrics.currentState.name}",
                        color = CyberCyan,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}

@Composable
private fun MetricCard(
    title: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(CyberSurfaceVariant)
            .border(0.5.dp, Color(0xFF334155), RoundedCornerShape(8.dp))
            .padding(vertical = 6.dp, horizontal = 8.dp)
    ) {
        Column {
            Text(
                text = title,
                color = TextMuted,
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = value,
                color = color,
                fontSize = 13.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun ProviderChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) CyberCyan.copy(alpha = 0.2f) else CyberSurfaceVariant)
            .border(1.dp, if (selected) CyberCyan else Color(0xFF334155), RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(
            text = label,
            color = if (selected) CyberCyan else TextSecondary,
            fontSize = 11.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            fontFamily = FontFamily.Monospace
        )
    }
}
