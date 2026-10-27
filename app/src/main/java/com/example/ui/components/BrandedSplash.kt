package com.example.ui.components

import android.provider.Settings
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first

private const val COMPANY_NAME = "Naxxivo"

@Composable
fun BrandedSplash(
    isReady: Boolean,
    onSplashFinished: () -> Unit
) {
    val context = LocalContext.current
    val animatorDurationScale = remember {
        try {
            Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1.0f
            )
        } catch (_: Throwable) {
            1.0f
        }
    }

    val isAnimationEnabled = animatorDurationScale > 0

    var startAnimation by remember { mutableStateOf(false) }
    var showNaxxivo by remember { mutableStateOf(false) }
    var startExit by remember { mutableStateOf(false) }

    val logoScale = remember { Animatable(if (isAnimationEnabled) 0.6f else 1f) }
    val logoAlpha = remember { Animatable(if (isAnimationEnabled) 0f else 1f) }
    val exitAlpha = remember { Animatable(1f) }
    val exitScale = remember { Animatable(1f) }

    val infiniteTransition = rememberInfiniteTransition(label = "SplashInfinite")
    
    val breathingScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "BreathingLogo"
    )

    val ripple1 = infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "Ripple1"
    )

    val ripple2 = infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearOutSlowInEasing, delayMillis = 600),
            repeatMode = RepeatMode.Restart
        ),
        label = "Ripple2"
    )

    val shimmerTranslate = infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "Shimmer"
    )

    val currentIsReady by rememberUpdatedState(isReady)

    LaunchedEffect(Unit) {
        val startTime = System.currentTimeMillis()
        
        if (isAnimationEnabled) {
            delay(100)
            startAnimation = true
            
            // Run logo animations in parallel
            val animJob = launch {
                launch { logoAlpha.animateTo(1f, tween(600)) }
                launch { logoScale.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)) }
            }
            
            delay(400)
            showNaxxivo = true
            
            animJob.join()
        } else {
            showNaxxivo = true
        }
        
        // Wait for ready signal or 3s hard timeout from start
        val maxWait = (3000L - (System.currentTimeMillis() - startTime)).coerceAtLeast(0L)
        if (maxWait > 0) {
            try {
                withTimeoutOrNull(maxWait) {
                    snapshotFlow { currentIsReady }.filter { it }.first()
                }
            } catch (_: Throwable) {}
        }
        
        // Enforce min 1.4s only if animations are enabled
        val elapsed = System.currentTimeMillis() - startTime
        if (isAnimationEnabled && elapsed < 1400) {
            try {
                delay(1400 - elapsed)
            } catch (_: Throwable) {}
        }
        
        startExit = true
        try {
            if (isAnimationEnabled) {
                launch { exitScale.animateTo(1.15f, tween(300, easing = FastOutSlowInEasing)) }
                exitAlpha.animateTo(0f, tween(300))
            } else {
                exitScale.snapTo(1.15f)
                exitAlpha.snapTo(0f)
            }
        } catch (_: Throwable) {}
        onSplashFinished()
    }


    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(QBackground)
            .graphicsLayer {
                alpha = exitAlpha.value
                scaleX = exitScale.value
                scaleY = exitScale.value
            }
    ) {
        // Radial Glow
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2, size.height / 2)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(QYellowLight.copy(alpha = 0.4f), Color.Transparent),
                    center = center,
                    radius = size.width * 0.4f
                ),
                center = center,
                radius = size.width * 0.4f
            )
        }

        // Ripples
        if (isAnimationEnabled && startAnimation) {
            Box(modifier = Modifier.align(Alignment.Center)) {
                RippleCircle(progress = ripple1.value)
                RippleCircle(progress = ripple2.value)
            }
        }

        // Logo
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .graphicsLayer {
                    val scale = if (startAnimation && !startExit) logoScale.value * breathingScale else logoScale.value
                    scaleX = scale
                    scaleY = scale
                    alpha = logoAlpha.value
                }
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_q_logo),
                contentDescription = null,
                modifier = Modifier.size(120.dp)
            )
        }

        // Bottom Brand
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val fromAlpha by animateFloatAsState(
                targetValue = if (showNaxxivo) 1f else 0f,
                animationSpec = tween(500),
                label = "FromAlpha"
            )
            
            Text(
                text = "from",
                color = QOnSurfaceVariant,
                fontSize = 14.sp,
                modifier = Modifier.alpha(fromAlpha)
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Row {
                COMPANY_NAME.forEachIndexed { index, char ->
                    val letterAlpha = remember { Animatable(0f) }
                    val letterOffset = remember { Animatable(8f) }

                    LaunchedEffect(showNaxxivo) {
                        if (showNaxxivo) {
                            delay(index * 60L)
                            if (isAnimationEnabled) {
                                launch { letterAlpha.animateTo(1f, tween(300)) }
                                launch { letterOffset.animateTo(0f, tween(300)) }
                            } else {
                                letterAlpha.snapTo(1f)
                                letterOffset.snapTo(0f)
                            }
                        }
                    }

                    Box {
                        Text(
                            text = char.toString(),
                            color = QOnSurface,
                            fontWeight = FontWeight.Bold,
                            fontSize = 24.sp,
                            letterSpacing = 4.sp,
                            modifier = Modifier
                                .graphicsLayer {
                                    translationY = letterOffset.value.dp.toPx()
                                    alpha = letterAlpha.value
                                }
                        )
                        
                        // Shimmer sweep
                        if (isAnimationEnabled && showNaxxivo) {
                            val shimmerAlpha by animateFloatAsState(
                                targetValue = if (shimmerTranslate.value > 0.1f && shimmerTranslate.value < 0.9f) 1f else 0f,
                                label = "ShimmerAlpha"
                            )
                            
                            // Approximate shimmer overlay logic
                            // Simplified for the effect: just a highlight
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RippleCircle(progress: Float) {
    Canvas(modifier = Modifier.size(240.dp)) {
        drawCircle(
            color = QYellowPrimary,
            radius = size.minDimension / 2 * progress,
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx()),
            alpha = (1f - progress) * 0.5f
        )
    }
}
