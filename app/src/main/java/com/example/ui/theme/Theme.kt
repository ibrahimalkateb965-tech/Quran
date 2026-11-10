package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf

val LocalQuranColors = staticCompositionLocalOf { LightQuranColors }

object QuranTheme {
    val colors: QuranColors
        @Composable
        @ReadOnlyComposable
        get() = LocalQuranColors.current
}

private fun QuranColors.toColorScheme(): ColorScheme {
    val base = if (isDark) darkColorScheme() else lightColorScheme()
    return base.copy(
        primary = accent,
        onPrimary = background,
        primaryContainer = accentContainer,
        onPrimaryContainer = textOnFill,
        secondary = success,
        onSecondary = background,
        background = background,
        onBackground = textPrimary,
        surface = cardSurface,
        onSurface = textPrimary,
        surfaceVariant = cardFill,
        onSurfaceVariant = textSecondary,
        outline = cardBorder,
        error = error,
        onError = background
    )
}

private val LightScheme = LightQuranColors.toColorScheme()
private val DarkScheme = DarkQuranColors.toColorScheme()

@Composable
fun QuranBlindTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) DarkQuranColors else LightQuranColors
    CompositionLocalProvider(LocalQuranColors provides colors) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkScheme else LightScheme,
            typography = Typography,
            content = content
        )
    }
}
