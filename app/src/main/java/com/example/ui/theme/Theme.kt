package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val QLightColorScheme = lightColorScheme(
    primary = QYellowPrimary,
    onPrimary = QOnYellow,
    primaryContainer = QYellowContainer,
    onPrimaryContainer = QYellowDark,
    secondary = QYellowDark,
    onSecondary = QOnYellow,
    background = QBackground,
    onBackground = QOnBackground,
    surface = QSurface,
    onSurface = QOnSurface,
    surfaceVariant = QSurfaceVariant,
    onSurfaceVariant = QOnSurfaceVariant,
    outline = QOutline,
    outlineVariant = QOutlineVariant
)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = QLightColorScheme,
        typography = Typography,
        content = content
    )
}
