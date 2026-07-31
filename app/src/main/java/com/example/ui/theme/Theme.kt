package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val AccessibleColorScheme = darkColorScheme(
    primary = AccessibleGold,
    onPrimary = DarkImmersiveBg,
    primaryContainer = DarkImmersiveCard,
    onPrimaryContainer = AccessibleGold,
    secondary = AccessibleGreenAccent,
    onSecondary = Color.Black,
    background = DarkImmersiveBg,
    onBackground = TextPrimaryWhite,
    surface = DarkImmersiveCard,
    onSurface = TextPrimaryWhite,
    surfaceVariant = DarkImmersiveSurface,
    onSurfaceVariant = TextPrimaryWhite,
    error = AccessibleRedAlert,
    onError = Color.White
)

@Composable
fun QuranBlindTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = AccessibleColorScheme,
        typography = Typography,
        content = content
    )
}

