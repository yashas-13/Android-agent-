package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val CyberColorScheme = darkColorScheme(
    primary = CyberCyan,
    onPrimary = Color(0xFF001F28),
    primaryContainer = Color(0xFF004D61),
    onPrimaryContainer = Color(0xFFBCE9FF),
    secondary = WhatsAppGreen,
    onSecondary = Color(0xFF00220E),
    secondaryContainer = Color(0xFF005327),
    onSecondaryContainer = Color(0xFF76FF9A),
    tertiary = CyberAmber,
    onTertiary = Color(0xFF261900),
    tertiaryContainer = Color(0xFF5A3C00),
    onTertiaryContainer = Color(0xFFFFDF9E),
    background = CyberBackground,
    onBackground = TextPrimary,
    surface = CyberSurface,
    onSurface = TextPrimary,
    surfaceVariant = CyberSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    outline = BorderCyber,
    error = CyberRed,
    onError = Color.White
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    // We enforce the cyber AI dark aesthetic for maximum immersion and fidelity
    MaterialTheme(
        colorScheme = CyberColorScheme,
        typography = Typography,
        content = content
    )
}

