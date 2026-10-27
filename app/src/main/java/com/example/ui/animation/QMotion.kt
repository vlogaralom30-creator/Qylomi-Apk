package com.example.ui.animation

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.IntOffset

object QMotion {
    val BouncySpring = spring<Float>(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessLow
    )

    val SmoothSpring = spring<Float>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMediumLow
    )

    val FadeEnter = fadeIn(animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing))
    val FadeExit = fadeOut(animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing))

    val SlideUpEnter = slideInVertically(
        initialOffsetY = { fullHeight -> (fullHeight * 0.15f).toInt() },
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
    ) + fadeIn(animationSpec = tween(250))

    val SlideDownExit = slideOutVertically(
        targetOffsetY = { fullHeight -> (fullHeight * 0.15f).toInt() },
        animationSpec = tween(200)
    ) + fadeOut(animationSpec = tween(180))

    val ScalePopEnter = scaleIn(
        initialScale = 0.85f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium)
    ) + fadeIn(animationSpec = tween(200))
}
