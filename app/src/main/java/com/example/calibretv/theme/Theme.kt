package com.example.calibretv.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val BookSpreadDarkColorScheme = darkColorScheme(
    primary = AccentGold,
    onPrimary = Color.Black,
    primaryContainer = SurfaceContainerHigh,
    onPrimaryContainer = BrightGold,
    secondary = AccentGold,
    onSecondary = Color.Black,
    background = SurfaceBase,
    onBackground = TextPrimary,
    surface = SurfaceBase,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceContainer,
    onSurfaceVariant = TextMuted,
)

@Composable
fun BookSpreadTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = BookSpreadDarkColorScheme,
        typography = Typography,
        content = content
    )
}

/**
 * Alias retrocompatible para CalibreTVTheme durante la migración arquitectónica.
 */
@Composable
fun CalibreTVTheme(
    content: @Composable () -> Unit
) {
    BookSpreadTheme(content = content)
}
