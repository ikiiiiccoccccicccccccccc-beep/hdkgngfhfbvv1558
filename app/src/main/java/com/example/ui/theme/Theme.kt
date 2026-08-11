package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val GamingDarkColorScheme = darkColorScheme(
    primary = NeonRed,
    onPrimary = TextPrimary,
    primaryContainer = NeonRedDark,
    secondary = NeonCyan,
    onSecondary = DarkBackground,
    secondaryContainer = NeonCyanDark,
    background = DarkBackground,
    onBackground = TextPrimary,
    surface = DarkSurface,
    onSurface = TextPrimary,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    outline = DarkCardBorder
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = GamingDarkColorScheme,
        typography = Typography,
        content = content
    )
}
