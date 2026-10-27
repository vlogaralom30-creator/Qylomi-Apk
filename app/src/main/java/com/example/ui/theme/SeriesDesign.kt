package com.example.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * "Golden Cinema" Premium Series Design Tokens & Constants
 */
object SeriesTokens {
    // Spacing
    val space4 = 4.dp
    val space8 = 8.dp
    val space12 = 12.dp
    val space16 = 16.dp
    val space24 = 24.dp
    val space32 = 32.dp

    // Corner Radii
    val radiusChip = 12.dp
    val radiusCard = 20.dp
    val radiusSheet = 28.dp
    val radiusPill = 999.dp

    val shapeChip = RoundedCornerShape(radiusChip)
    val shapeCard = RoundedCornerShape(radiusCard)
    val shapeSheet = RoundedCornerShape(topStart = radiusSheet, topEnd = radiusSheet)
    val shapePill = RoundedCornerShape(radiusPill)

    // Motion durations
    const val durationFast = 120
    const val durationBase = 220
    const val durationSlow = 360

    val motionFast = tween<Float>(durationFast, easing = FastOutSlowInEasing)
    val motionBase = tween<Float>(durationBase, easing = FastOutSlowInEasing)
    val motionSlow = tween<Float>(durationSlow, easing = FastOutSlowInEasing)

    fun <T> springPress() = spring<T>(
        dampingRatio = 0.6f,
        stiffness = Spring.StiffnessMedium
    )

    // Golden Cinema Colors & Gradients
    val GoldAmber = Color(0xFFFBBF24)
    val GoldDark = Color(0xFFD97706)
    val GoldLight = Color(0xFFFEF3C7)
    val GoldGlow = Color(0x33FBBF24)
    val DarkInk = Color(0xFF1E1B18)
    val TrueBlack = Color(0xFF000000)
    val StageBlack = Color(0xFF0B0C0E)
    val GlassSurface = Color(0xCC181A20)
    val GlassLight = Color(0xEEFFFFFF)

    val GoldenGradient = Brush.horizontalGradient(
        listOf(Color(0xFFF59E0B), Color(0xFFFBBF24))
    )

    val GoldenVerticalGradient = Brush.verticalGradient(
        listOf(Color(0xFFFBBF24), Color(0xFFD97706))
    )

    val ScrimTop = Brush.verticalGradient(
        colors = listOf(Color.Black.copy(alpha = 0.75f), Color.Transparent)
    )

    val ScrimBottom = Brush.verticalGradient(
        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))
    )

    val AmbientYellowGlow = Brush.radialGradient(
        colors = listOf(GoldGlow, Color.Transparent),
        radius = 500f
    )
}

/**
 * Series Typography Scale based on prompt requirements
 */
object SeriesTypography {
    val display = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 34.sp
    )

    val headline = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 28.sp
    )

    val title = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 22.sp
    )

    val body = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 21.sp
    )

    val label = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp
    )

    val micro = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 10.sp,
        lineHeight = 14.sp
    )
}
