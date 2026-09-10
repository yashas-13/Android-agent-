package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.agent.AgentState
import com.example.ui.theme.CyberAmber
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.CyberGreen
import com.example.ui.theme.CyberRed
import com.example.ui.theme.CyberSurfaceVariant
import com.example.ui.theme.TextMuted

@Composable
fun StateBadge(state: AgentState, modifier: Modifier = Modifier) {
    val (bgColor, textColor, dotColor) = when (state) {
        AgentState.IDLE -> Triple(Color(0xFF1E293B), TextMuted, Color(0xFF64748B))
        AgentState.OBSERVING -> Triple(Color(0x3300F0FF), CyberCyan, CyberCyan)
        AgentState.UNDERSTANDING -> Triple(Color(0x33A855F7), Color(0xFFA855F7), Color(0xFFA855F7))
        AgentState.PLANNING -> Triple(Color(0x3300F0FF), CyberCyan, CyberCyan)
        AgentState.WAITING_FOR_CONFIRMATION -> Triple(Color(0x33FFB300), CyberAmber, CyberAmber)
        AgentState.EXECUTING -> Triple(Color(0x3300E676), CyberGreen, CyberGreen)
        AgentState.VERIFYING -> Triple(Color(0x3300F0FF), CyberCyan, CyberCyan)
        AgentState.RECOVERING -> Triple(Color(0x33FFB300), CyberAmber, CyberAmber)
        AgentState.SUCCESS -> Triple(Color(0x3300E676), CyberGreen, CyberGreen)
        AgentState.FAILED -> Triple(Color(0x33FF5252), CyberRed, CyberRed)
        AgentState.STOPPED -> Triple(Color(0xFF334155), Color(0xFFCBD5E1), Color(0xFF94A3B8))
    }

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .border(1.dp, dotColor.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(dotColor)
        )
        Text(
            text = state.name.replace("_", " "),
            color = textColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.padding(start = 6.dp)
        )
    }
}

@Composable
fun CyberBadge(
    text: String,
    color: Color = CyberCyan,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(color.copy(alpha = 0.15f))
            .border(0.8.dp, color.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = text,
            color = color,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = FontFamily.Monospace
        )
    }
}
