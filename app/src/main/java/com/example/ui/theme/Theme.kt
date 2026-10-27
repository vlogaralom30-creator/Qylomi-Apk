package com.example.ui.theme

import android.content.Context
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK
}

object ThemePreferences {
    private const val PREFS_NAME = "qylomi_theme_prefs"
    private const val KEY_THEME_MODE = "theme_mode"

    fun getThemeMode(context: Context): ThemeMode {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val modeStr = prefs.getString(KEY_THEME_MODE, ThemeMode.SYSTEM.name) ?: ThemeMode.SYSTEM.name
        return try {
            ThemeMode.valueOf(modeStr)
        } catch (_: Exception) {
            ThemeMode.SYSTEM
        }
    }

    fun setThemeMode(context: Context, mode: ThemeMode) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_THEME_MODE, mode.name).apply()
    }
}

val LocalThemeMode = compositionLocalOf { ThemeMode.SYSTEM }
val LocalSetThemeMode = compositionLocalOf<(ThemeMode) -> Unit> { {} }

private val QDarkColorScheme = darkColorScheme(
    primary = QYellowPrimary,
    onPrimary = QOnYellow,
    primaryContainer = Color(0xFF78350F),
    onPrimaryContainer = Color(0xFFFEF3C7),
    secondary = QYellowDark,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF451A03),
    onSecondaryContainer = Color(0xFFFDE68A),
    background = Color(0xFF0F1117),
    onBackground = Color(0xFFF1F5F9),
    surface = Color(0xFF181B22),
    onSurface = Color(0xFFF1F5F9),
    surfaceVariant = Color(0xFF222632),
    onSurfaceVariant = Color(0xFF94A3B8),
    outline = Color(0xFF2E3342),
    outlineVariant = Color(0xFF1E222D),
    error = QLikeRed,
    onError = Color.White
)

private val QLightColorScheme = lightColorScheme(
    primary = QYellowDark,
    onPrimary = Color.White,
    primaryContainer = QYellowContainer,
    onPrimaryContainer = QYellowDark,
    secondary = QYellowPrimary,
    onSecondary = QOnYellow,
    background = QBackground,
    onBackground = QOnBackground,
    surface = QSurface,
    onSurface = QOnSurface,
    surfaceVariant = QSurfaceVariant,
    onSurfaceVariant = QOnSurfaceVariant,
    outline = QOutline,
    outlineVariant = QOutlineVariant,
    error = QLikeRed,
    onError = Color.White
)

@Composable
fun QylomiTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    onThemeChange: (ThemeMode) -> Unit = {},
    content: @Composable () -> Unit
) {
    val isDark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    val colorScheme = if (isDark) QDarkColorScheme else QLightColorScheme

    CompositionLocalProvider(
        LocalThemeMode provides themeMode,
        LocalSetThemeMode provides onThemeChange
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}

// Backward-compatibility alias
@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    var currentMode by remember { mutableStateOf(ThemePreferences.getThemeMode(context)) }

    QylomiTheme(
        themeMode = currentMode,
        onThemeChange = { newMode ->
            currentMode = newMode
            ThemePreferences.setThemeMode(context, newMode)
        },
        content = content
    )
}
