package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

// Qylomi Brand Colors - Vibrant Amber / Yellow Base Palette
val QYellowPrimary = Color(0xFFFBBF24) // Warm vibrant Amber/Yellow
val QYellowDark = Color(0xFFD97706)
val QOnYellow = Color(0xFF1E1B18) // High contrast dark on yellow

val QLikeRed = Color(0xFFEF4444)
val QRelayGreen = Color(0xFF10B981)
val QVerifiedBlue = Color(0xFF0284C7)
val QBadgeGold = Color(0xFFEAB308)

// Static tokens for light theme base
val QLightBackground = Color(0xFFF8FAFC)
val QLightSurface = Color(0xFFFFFFFF)
val QLightSurfaceVariant = Color(0xFFF1F5F9)
val QLightSurfaceHover = Color(0xFFF8FAFC)
val QLightOnBackground = Color(0xFF0F172A)
val QLightOnSurface = Color(0xFF0F172A)
val QLightOnSurfaceVariant = Color(0xFF64748B)
val QLightOutline = Color(0xFFE2E8F0)
val QLightOutlineVariant = Color(0xFFCBD5E1)
val QLightCardBorder = Color(0xFFE2E8F0)
val QLightYellowLight = Color(0xFFFEF3C7)
val QLightYellowContainer = Color(0xFFFFFBEB)

// Static tokens for dark theme base
val QDarkBackground = Color(0xFF0F1117)
val QDarkSurface = Color(0xFF181B22)
val QDarkSurfaceVariant = Color(0xFF222632)
val QDarkSurfaceHover = Color(0xFF2A2E3D)
val QDarkOnBackground = Color(0xFFF1F5F9)
val QDarkOnSurface = Color(0xFFF1F5F9)
val QDarkOnSurfaceVariant = Color(0xFF94A3B8)
val QDarkOutline = Color(0xFF2E3342)
val QDarkOutlineVariant = Color(0xFF1E222D)
val QDarkCardBorder = Color(0xFF2E3342)
val QDarkYellowLight = Color(0xFF382A0E)
val QDarkYellowContainer = Color(0xFF2E2008)

// Legacy alias tokens
val QCrunchyOrange = QYellowPrimary
val QCrunchyOrangeDark = QYellowDark
val QDarkObsidian = Color(0xFF0F1014)
val QDarkCharcoal = Color(0xFF181A20)

// Dynamic theme-aware color properties that respond to Dark/Light theme automatically
val QBackground: Color
    @Composable
    @ReadOnlyComposable
    get() = MaterialTheme.colorScheme.background

val QSurface: Color
    @Composable
    @ReadOnlyComposable
    get() = MaterialTheme.colorScheme.surface

val QSurfaceVariant: Color
    @Composable
    @ReadOnlyComposable
    get() = MaterialTheme.colorScheme.surfaceVariant

val QSurfaceHover: Color
    @Composable
    @ReadOnlyComposable
    get() = if (MaterialTheme.colorScheme.surface.luminance() < 0.5f) QDarkSurfaceHover else QLightSurfaceHover

val QOnBackground: Color
    @Composable
    @ReadOnlyComposable
    get() = MaterialTheme.colorScheme.onBackground

val QOnSurface: Color
    @Composable
    @ReadOnlyComposable
    get() = MaterialTheme.colorScheme.onSurface

val QOnSurfaceVariant: Color
    @Composable
    @ReadOnlyComposable
    get() = MaterialTheme.colorScheme.onSurfaceVariant

val QOutline: Color
    @Composable
    @ReadOnlyComposable
    get() = MaterialTheme.colorScheme.outline

val QOutlineVariant: Color
    @Composable
    @ReadOnlyComposable
    get() = MaterialTheme.colorScheme.outlineVariant

val QCardBorder: Color
    @Composable
    @ReadOnlyComposable
    get() = if (MaterialTheme.colorScheme.surface.luminance() < 0.5f) QDarkCardBorder else QLightCardBorder

val QYellowLight: Color
    @Composable
    @ReadOnlyComposable
    get() = if (MaterialTheme.colorScheme.surface.luminance() < 0.5f) QDarkYellowLight else QLightYellowLight

val QYellowContainer: Color
    @Composable
    @ReadOnlyComposable
    get() = if (MaterialTheme.colorScheme.surface.luminance() < 0.5f) QDarkYellowContainer else QLightYellowContainer
