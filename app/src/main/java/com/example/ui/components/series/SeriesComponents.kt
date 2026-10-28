package com.example.ui.components.series

import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin

/**
 * Shimmer effect modifier for skeleton loaders
 */
fun Modifier.shimmerEffect(): Modifier = composed {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerTranslate"
    )

    val shimmerColors = listOf(
        QSurfaceVariant.copy(alpha = 0.6f),
        QYellowLight.copy(alpha = 0.45f),
        QSurfaceVariant.copy(alpha = 0.6f)
    )

    background(
        brush = Brush.linearGradient(
            colors = shimmerColors,
            start = Offset(translateAnim - 400f, translateAnim - 400f),
            end = Offset(translateAnim, translateAnim)
        )
    )
}

/**
 * Spring press scale modifier with haptic feedback tick
 */
fun Modifier.pressScale(
    targetScale: Float = 0.96f,
    onClick: () -> Unit
): Modifier = composed {
    val view = LocalView.current
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) targetScale else 1f,
        animationSpec = SeriesTokens.springPress(),
        label = "pressScale"
    )

    this
        .scale(scale)
        .pointerInput(Unit) {
            detectTapGestures(
                onPress = {
                    isPressed = true
                    try {
                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    } catch (_: Exception) {}
                    tryAwaitRelease()
                    isPressed = false
                },
                onTap = {
                    onClick()
                }
            )
        }
}

/**
 * Premium Poster Image with fallback chain and shimmer
 */
@Composable
fun PosterImage(
    posterUrl: String?,
    bannerUrl: String? = null,
    title: String? = null,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    shape: RoundedCornerShape = SeriesTokens.shapeCard
) {
    val context = LocalContext.current
    val effectiveUrl = remember(posterUrl, bannerUrl) {
        posterUrl?.takeIf { it.isNotBlank() } ?: bannerUrl?.takeIf { it.isNotBlank() }
    }

    var isLoading by remember { mutableStateOf(true) }
    var isError by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .clip(shape)
            .background(QSurfaceVariant)
            .border(0.75.dp, QCardBorder, shape)
    ) {
        if (!effectiveUrl.isNullOrBlank() && !isError) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(effectiveUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = title,
                contentScale = contentScale,
                onLoading = { isLoading = true },
                onSuccess = { isLoading = false; isError = false },
                onError = { isLoading = false; isError = true },
                modifier = Modifier.fillMaxSize()
            )
        }

        if (isLoading && !effectiveUrl.isNullOrBlank()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .shimmerEffect()
            )
        }

        if (isError || effectiveUrl.isNullOrBlank()) {
            // Branded placeholder with title initials
            val initials = remember(title) {
                if (title.isNullOrBlank()) "Q"
                else title.split(" ")
                    .filter { it.isNotBlank() }
                    .take(2)
                    .mapNotNull { it.firstOrNull()?.uppercase() }
                    .joinToString("")
                    .ifEmpty { "Q" }
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.linearGradient(
                            listOf(QYellowLight, QSurfaceVariant)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Surface(
                        shape = CircleShape,
                        color = QYellowPrimary,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = initials,
                                color = QOnYellow,
                                fontWeight = FontWeight.Black,
                                fontSize = 18.sp
                            )
                        }
                    }
                    if (!title.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = title,
                            style = SeriesTypography.micro,
                            color = QOnSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(horizontal = 8.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

/**
 * GlassPanel - translucent frosted card with soft border
 */
@Composable
fun GlassPanel(
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = SeriesTokens.shapeCard,
    backgroundColor: Color = SeriesTokens.GlassLight,
    borderColor: Color = QOutline.copy(alpha = 0.5f),
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .clip(shape)
            .background(backgroundColor)
            .border(1.dp, borderColor, shape)
    ) {
        content()
    }
}

/**
 * Golden Cinema Yellow Pill Button
 */
@Composable
fun YellowPillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    isLoading: Boolean = false,
    enabled: Boolean = true
) {
    Surface(
        onClick = onClick,
        enabled = enabled && !isLoading,
        shape = SeriesTokens.shapePill,
        color = if (enabled) QYellowPrimary else QSurfaceVariant,
        shadowElevation = if (enabled) 4.dp else 0.dp,
        modifier = modifier
            .height(48.dp)
            .pressScale(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .background(
                    if (enabled) SeriesTokens.GoldenGradient
                    else Brush.linearGradient(listOf(QSurfaceVariant, QSurfaceVariant))
                )
                .padding(horizontal = 20.dp, vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    color = QOnYellow,
                    strokeWidth = 2.5.dp,
                    modifier = Modifier.size(20.dp)
                )
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    if (icon != null) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = if (enabled) QOnYellow else QOnSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(
                        text = text,
                        style = SeriesTypography.title.copy(fontSize = 15.sp),
                        fontWeight = FontWeight.Bold,
                        color = if (enabled) QOnYellow else QOnSurfaceVariant
                    )
                }
            }
        }
    }
}

/**
 * Ghost Pill Button for secondary actions
 */
@Composable
fun GhostPillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    isActive: Boolean = false
) {
    Surface(
        onClick = onClick,
        shape = SeriesTokens.shapePill,
        color = if (isActive) QYellowLight else QSurface,
        border = BorderStroke(
            1.dp,
            if (isActive) QYellowPrimary else QOutline
        ),
        modifier = modifier
            .height(48.dp)
            .pressScale(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isActive) QYellowDark else QOnSurface,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
            }
            Text(
                text = text,
                style = SeriesTypography.label,
                fontWeight = FontWeight.SemiBold,
                color = if (isActive) QYellowDark else QOnSurface
            )
        }
    }
}

/**
 * MetaChip for tags, genres, qualities
 */
@Composable
fun MetaChip(
    text: String,
    modifier: Modifier = Modifier,
    isHighlighted: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    Surface(
        shape = SeriesTokens.shapeChip,
        color = if (isHighlighted) QYellowLight else QSurfaceVariant,
        border = BorderStroke(
            0.5.dp,
            if (isHighlighted) QYellowPrimary.copy(alpha = 0.5f) else QCardBorder
        ),
        modifier = modifier.then(
            if (onClick != null) Modifier.clickable { onClick() } else Modifier
        )
    ) {
        Text(
            text = text,
            style = SeriesTypography.micro,
            fontWeight = if (isHighlighted) FontWeight.Bold else FontWeight.Medium,
            color = if (isHighlighted) QYellowDark else QOnSurface,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
        )
    }
}

/**
 * LanguageBadge for Audio/Subs
 */
@Composable
fun LanguageBadge(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(QSurfaceVariant)
            .border(0.5.dp, QCardBorder, RoundedCornerShape(6.dp))
            .padding(horizontal = 6.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label.uppercase(),
            style = SeriesTypography.micro.copy(fontSize = 9.sp),
            fontWeight = FontWeight.Bold,
            color = QYellowDark
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = value,
            style = SeriesTypography.micro.copy(fontSize = 11.sp),
            color = QOnSurface
        )
    }
}

/**
 * RatingRing - Circular progress indicator with score in center
 */
@Composable
fun RatingRing(
    rating: Double,
    maxRating: Double = 10.0,
    size: Dp = 48.dp,
    strokeWidth: Dp = 3.5.dp,
    modifier: Modifier = Modifier
) {
    val progress = (rating / maxRating).coerceIn(0.0, 1.0).toFloat()
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = SeriesTokens.motionSlow,
        label = "ratingProgress"
    )

    val trackColor = QOutline
    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
            val strokePx = strokeWidth.toPx()
            // Track
            drawArc(
                color = trackColor,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(width = strokePx)
            )
            // Active Progress
            drawArc(
                brush = Brush.sweepGradient(
                    listOf(QYellowPrimary, QYellowDark, QYellowPrimary)
                ),
                startAngle = -90f,
                sweepAngle = 360f * animatedProgress,
                useCenter = false,
                style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = String.format("%.1f", rating),
                style = SeriesTypography.label,
                fontWeight = FontWeight.ExtraBold,
                color = QOnSurface
            )
        }
    }
}

/**
 * Formats a count cleanly (e.g. 1.2K, 345)
 */
fun formatCount(count: Int): String {
    return when {
        count >= 1_000_000 -> String.format("%.1fM", count / 1_000_000f)
        count >= 1_000 -> String.format("%.1fK", count / 1_000f)
        else -> count.toString()
    }
}

/**
 * Animated Count Text
 */
@Composable
fun AnimatedCount(
    count: Int,
    color: Color = QOnSurfaceVariant,
    style: androidx.compose.ui.text.TextStyle = SeriesTypography.label,
    modifier: Modifier = Modifier
) {
    val formatted = remember(count) { formatCount(count) }
    Text(
        text = formatted,
        color = color,
        style = style,
        modifier = modifier
    )
}

/**
 * HeartBurstButton - scales with particle burst bounce and haptic feedback
 */
@Composable
fun HeartBurstButton(
    isLiked: Boolean,
    likesCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val view = LocalView.current
    var burstTrigger by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue = if (burstTrigger) 1.35f else 1f,
        animationSpec = spring(dampingRatio = 0.4f, stiffness = Spring.StiffnessMedium),
        finishedListener = { burstTrigger = false },
        label = "heartScale"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clip(SeriesTokens.shapeChip)
            .clickable {
                burstTrigger = true
                try {
                    view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                } catch (_: Exception) {}
                onClick()
            }
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = if (isLiked) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder,
                contentDescription = if (isLiked) "Unlike" else "Like",
                tint = if (isLiked) QLikeRed else QOnSurfaceVariant,
                modifier = Modifier
                    .size(24.dp)
                    .scale(scale)
            )
        }
        Spacer(modifier = Modifier.height(2.dp))
        AnimatedCount(
            count = likesCount,
            color = if (isLiked) QLikeRed else QOnSurfaceVariant,
            style = SeriesTypography.micro
        )
    }
}

/**
 * Animated Bookmark Button
 */
@Composable
fun AnimatedBookmark(
    isInList: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val view = LocalView.current
    var animTrigger by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue = if (animTrigger) 1.25f else 1f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessMedium),
        finishedListener = { animTrigger = false },
        label = "bookmarkScale"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clip(SeriesTokens.shapeChip)
            .clickable {
                animTrigger = true
                try {
                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                } catch (_: Exception) {}
                onClick()
            }
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Icon(
            imageVector = if (isInList) Icons.Default.Bookmark else Icons.Outlined.BookmarkBorder,
            contentDescription = if (isInList) "In My List" else "Add to List",
            tint = if (isInList) QYellowDark else QOnSurfaceVariant,
            modifier = Modifier
                .size(24.dp)
                .scale(scale)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = if (isInList) "In List" else "My List",
            style = SeriesTypography.micro,
            color = if (isInList) QYellowDark else QOnSurfaceVariant
        )
    }
}

/**
 * Golden Section Header with "See all" arrow
 */
@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    onSeeAllClick: (() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(18.dp)
                    .clip(SeriesTokens.shapePill)
                    .background(QYellowPrimary)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = title,
                style = SeriesTypography.headline.copy(fontSize = 18.sp),
                color = QOnSurface
            )
        }

        if (onSeeAllClick != null) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(SeriesTokens.shapeChip)
                    .clickable { onSeeAllClick() }
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "See all",
                    style = SeriesTypography.label,
                    fontWeight = FontWeight.SemiBold,
                    color = QYellowDark
                )
                Spacer(modifier = Modifier.width(3.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = QYellowDark,
                    modifier = Modifier.size(13.dp)
                )
            }
        }
    }
}

/**
 * Clean Empty State component
 */
@Composable
fun SeriesEmptyState(
    icon: ImageVector,
    title: String,
    description: String,
    actionText: String? = null,
    onActionClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            shape = CircleShape,
            color = QYellowLight,
            modifier = Modifier.size(64.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = QYellowDark,
                    modifier = Modifier.size(32.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = title,
            style = SeriesTypography.title,
            color = QOnSurface,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = description,
            style = SeriesTypography.body,
            color = QOnSurfaceVariant,
            textAlign = TextAlign.Center
        )
        if (actionText != null && onActionClick != null) {
            Spacer(modifier = Modifier.height(20.dp))
            YellowPillButton(
                text = actionText,
                onClick = onActionClick
            )
        }
    }
}

/**
 * Series Error State with Retry
 */
@Composable
fun SeriesErrorState(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.CloudOff,
            contentDescription = null,
            tint = QOnSurfaceVariant,
            modifier = Modifier.size(48.dp)
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = message,
            style = SeriesTypography.body,
            color = QOnSurface,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = onRetry,
            colors = ButtonDefaults.buttonColors(
                containerColor = QYellowPrimary,
                contentColor = QOnYellow
            ),
            shape = SeriesTokens.shapePill
        ) {
            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Retry", fontWeight = FontWeight.Bold)
        }
    }
}

/**
 * Skeleton Loader Card for series posters
 */
@Composable
fun SkeletonCard(modifier: Modifier = Modifier) {
    Column(modifier = modifier.width(130.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(2f / 3f)
                .clip(SeriesTokens.shapeCard)
                .shimmerEffect()
        )
        Spacer(modifier = Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .height(14.dp)
                .clip(RoundedCornerShape(4.dp))
                .shimmerEffect()
        )
        Spacer(modifier = Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth(0.5f)
                .height(10.dp)
                .clip(RoundedCornerShape(4.dp))
                .shimmerEffect()
        )
    }
}

/**
 * Skeleton Loader Row for horizontal shelves
 */
@Composable
fun SkeletonRow(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        repeat(4) {
            SkeletonCard()
        }
    }
}

/**
 * Animated 3-bar equalizer for currently playing items
 */
@Composable
fun AnimatedEqualizer(
    color: Color = QYellowPrimary,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "equalizer")
    val h1 by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(tween(400, easing = LinearEasing), RepeatMode.Reverse),
        label = "eq1"
    )
    val h2 by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 0.2f,
        animationSpec = infiniteRepeatable(tween(550, easing = LinearEasing), RepeatMode.Reverse),
        label = "eq2"
    )
    val h3 by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(tween(480, easing = LinearEasing), RepeatMode.Reverse),
        label = "eq3"
    )

    Row(
        modifier = modifier.height(14.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        Box(
            modifier = Modifier
                .width(3.dp)
                .fillMaxHeight(h1)
                .clip(RoundedCornerShape(1.dp))
                .background(color)
        )
        Box(
            modifier = Modifier
                .width(3.dp)
                .fillMaxHeight(h2)
                .clip(RoundedCornerShape(1.dp))
                .background(color)
        )
        Box(
            modifier = Modifier
                .width(3.dp)
                .fillMaxHeight(h3)
                .clip(RoundedCornerShape(1.dp))
                .background(color)
        )
    }
}
