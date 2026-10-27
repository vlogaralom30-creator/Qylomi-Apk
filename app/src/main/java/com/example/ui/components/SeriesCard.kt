package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.ContinueWatchingItem
import com.example.data.model.SeriesCard
import com.example.ui.components.series.*
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.temporal.ChronoUnit

/**
 * Golden Cinema 2:3 Series Card Item
 */
@Composable
fun SeriesCardItem(
    card: SeriesCard,
    isInWatchlist: Boolean,
    onCardClick: () -> Unit,
    onWatchlistClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isNew = remember(card.lastEpisodeAt, card.createdAt) {
        isWithin7Days(card.lastEpisodeAt ?: card.createdAt)
    }

    Column(
        modifier = modifier
            .width(135.dp)
            .pressScale(onClick = onCardClick)
            .testTag("series_card_${card.id}")
    ) {
        // 2:3 Poster Card with 20dp radii and thin border
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(2f / 3f)
                .clip(SeriesTokens.shapeCard)
                .background(QSurfaceVariant)
                .border(0.75.dp, QCardBorder, SeriesTokens.shapeCard)
                .shadow(elevation = 4.dp, shape = SeriesTokens.shapeCard, ambientColor = SeriesTokens.GoldGlow, spotColor = SeriesTokens.GoldGlow)
        ) {
            PosterImage(
                posterUrl = card.posterUrl,
                bannerUrl = card.bannerUrl,
                title = card.title,
                modifier = Modifier.fillMaxSize(),
                shape = SeriesTokens.shapeCard
            )

            // Top Badges Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (isNew) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = QYellowPrimary,
                            shadowElevation = 2.dp
                        ) {
                            Text(
                                text = "NEW",
                                style = SeriesTypography.micro.copy(fontSize = 9.sp),
                                fontWeight = FontWeight.Black,
                                color = QOnYellow,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                            )
                        }
                    }
                    if (card.isMature) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFFD32F2F)
                        ) {
                            Text(
                                text = "18+",
                                style = SeriesTypography.micro.copy(fontSize = 9.sp),
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                // Bookmark Icon Button
                IconButton(
                    onClick = onWatchlistClick,
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.55f))
                        .testTag("watchlist_button_${card.id}")
                ) {
                    Icon(
                        imageVector = if (isInWatchlist) Icons.Default.Bookmark else Icons.Outlined.BookmarkBorder,
                        contentDescription = "Watchlist",
                        tint = if (isInWatchlist) QYellowPrimary else Color.White,
                        modifier = Modifier.size(17.dp)
                    )
                }
            }

            // Bottom Gradient Scrim with Rating and Language
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color.Black.copy(alpha = 0.88f))
                        )
                    )
                    .padding(horizontal = 8.dp, vertical = 6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val score = card.avgUserRating?.takeIf { it > 0 } ?: card.rating ?: 0.0
                    if (score > 0.0) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = QYellowPrimary,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = String.format("%.1f", score),
                                color = Color.White,
                                style = SeriesTypography.micro,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.width(1.dp))
                    }

                    val lang = card.languages.firstOrNull()
                    if (!lang.isNullOrBlank()) {
                        Text(
                            text = lang.take(5),
                            color = Color.White.copy(alpha = 0.85f),
                            style = SeriesTypography.micro.copy(fontSize = 9.sp),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Title
        Text(
            text = card.title,
            style = SeriesTypography.label,
            fontWeight = FontWeight.SemiBold,
            color = QOnSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * Top 10 Card Item with big stylized outlined numerals behind poster
 */
@Composable
fun Top10SeriesCardItem(
    rank: Int,
    card: SeriesCard,
    isInWatchlist: Boolean,
    onCardClick: () -> Unit,
    onWatchlistClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .width(170.dp)
            .testTag("top10_card_${card.id}")
    ) {
        // Large stylized rank number behind poster
        Text(
            text = "$rank",
            fontSize = 84.sp,
            fontWeight = FontWeight.Black,
            color = QYellowPrimary.copy(alpha = 0.35f),
            lineHeight = 84.sp,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .offset(x = (-4).dp, y = (4).dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            SeriesCardItem(
                card = card,
                isInWatchlist = isInWatchlist,
                onCardClick = onCardClick,
                onWatchlistClick = onWatchlistClick,
                modifier = Modifier.width(128.dp)
            )
        }
    }
}

/**
 * Continue Watching Card (Wide 16:9 with progress bar and remaining time)
 */
@Composable
fun ContinueWatchingCard(
    item: ContinueWatchingItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val progress = remember(item.positionSec, item.durationSec) {
        val dur = item.durationSec ?: 0
        if (dur > 0) (item.positionSec.toFloat() / dur.toFloat()).coerceIn(0f, 1f) else 0f
    }

    val remainingMinutes = remember(item.positionSec, item.durationSec) {
        val dur = item.durationSec ?: 0
        if (dur > item.positionSec) ((dur - item.positionSec) / 60).coerceAtLeast(1) else 0
    }

    val s = item.series
    val ep = item.episode
    val idTag = ep?.id ?: s?.id ?: "unknown"

    Card(
        shape = SeriesTokens.shapeCard,
        colors = CardDefaults.cardColors(containerColor = QSurface),
        border = BorderStroke(1.dp, QCardBorder),
        modifier = modifier
            .width(220.dp)
            .pressScale(onClick = onClick)
            .testTag("continue_watching_$idTag")
    ) {
        Column {
            // 16:9 Thumbnail
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .background(QSurfaceVariant)
            ) {
                AsyncImage(
                    model = ep?.thumbnailUrl ?: s?.effectiveBannerUrl ?: s?.posterUrl,
                    contentDescription = ep?.title ?: s?.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                // Play icon overlay with golden glow
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.65f))
                        .border(1.dp, QYellowPrimary.copy(alpha = 0.6f), CircleShape)
                        .align(Alignment.Center)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Resume",
                        tint = QYellowPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                }

                // Progress Bar at bottom
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .align(Alignment.BottomCenter),
                    color = QYellowPrimary,
                    trackColor = Color.White.copy(alpha = 0.3f)
                )
            }

            Column(modifier = Modifier.padding(10.dp)) {
                Text(
                    text = s?.title ?: "Series",
                    style = SeriesTypography.label,
                    fontWeight = FontWeight.Bold,
                    color = QOnSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "S${ep?.seasonNumber ?: 1}:E${ep?.episodeNumber ?: 1}",
                        style = SeriesTypography.micro,
                        fontWeight = FontWeight.SemiBold,
                        color = QYellowDark
                    )
                    if (remainingMinutes > 0) {
                        Text(
                            text = "$remainingMinutes min left",
                            style = SeriesTypography.micro,
                            color = QOnSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

/**
 * Premium Hero Carousel with Segmented Story Bars and Glass Info Panel
 */
@Composable
fun HeroCarousel(
    items: List<SeriesCard>,
    isInWatchlist: (String) -> Boolean,
    onCardClick: (SeriesCard) -> Unit,
    onPlayClick: (SeriesCard) -> Unit,
    onWatchlistToggle: (SeriesCard) -> Unit,
    modifier: Modifier = Modifier
) {
    if (items.isEmpty()) return

    val pagerState = rememberPagerState(pageCount = { items.size })
    var isUserInteracting by remember { mutableStateOf(false) }

    // Auto-advance 5 seconds, paused on touch
    LaunchedEffect(pagerState, isUserInteracting) {
        while (true) {
            delay(5000)
            if (!isUserInteracting && items.isNotEmpty()) {
                val next = (pagerState.currentPage + 1) % items.size
                pagerState.animateScrollToPage(next)
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(310.dp)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        isUserInteracting = true
                        tryAwaitRelease()
                        isUserInteracting = false
                    }
                )
            }
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            val series = items[page]
            val banner = series.bannerUrl?.takeIf { it.isNotBlank() }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable { onCardClick(series) }
            ) {
                if (banner != null) {
                    AsyncImage(
                        model = banner,
                        contentDescription = series.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    AsyncImage(
                        model = series.posterUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .blur(20.dp)
                    )
                    AsyncImage(
                        model = series.posterUrl,
                        contentDescription = series.title,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(end = 20.dp, top = 20.dp, bottom = 60.dp)
                            .aspectRatio(2f / 3f)
                            .clip(SeriesTokens.shapeCard)
                    )
                }

                // Dark gradient scrim
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.5f),
                                    Color.Black.copy(alpha = 0.95f)
                                )
                            )
                        )
                )

                // Glass info panel with title, rating, genres, Play and My List buttons
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(horizontal = 16.dp, vertical = 20.dp)
                        .fillMaxWidth(0.82f)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        series.genres.take(2).forEach { genre ->
                            Surface(
                                shape = SeriesTokens.shapeChip,
                                color = Color.White.copy(alpha = 0.2f)
                            ) {
                                Text(
                                    text = genre,
                                    style = SeriesTypography.micro,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }

                        val score = series.avgUserRating?.takeIf { it > 0 } ?: series.rating ?: 0.0
                        if (score > 0.0) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Star, contentDescription = null, tint = QYellowPrimary, modifier = Modifier.size(13.dp))
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = String.format("%.1f", score),
                                    style = SeriesTypography.micro,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = series.title,
                        style = SeriesTypography.headline.copy(fontSize = 20.sp),
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        YellowPillButton(
                            text = "Play",
                            icon = Icons.Default.PlayArrow,
                            onClick = { onPlayClick(series) },
                            modifier = Modifier
                                .height(40.dp)
                                .testTag("hero_play_button")
                        )

                        val inList = isInWatchlist(series.id)
                        OutlinedButton(
                            onClick = { onWatchlistToggle(series) },
                            shape = SeriesTokens.shapePill,
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = if (inList) QYellowPrimary else Color.White
                            ),
                            border = BorderStroke(
                                1.dp,
                                if (inList) QYellowPrimary else Color.White.copy(alpha = 0.7f)
                            ),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                            modifier = Modifier.height(40.dp)
                        ) {
                            Icon(
                                imageVector = if (inList) Icons.Default.Bookmark else Icons.Outlined.BookmarkBorder,
                                contentDescription = null,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (inList) "In List" else "+ My List", style = SeriesTypography.label)
                        }
                    }
                }
            }
        }

        // Story-Style Segmented Progress Bars at the bottom
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            repeat(items.size) { index ->
                val isCurrent = pagerState.currentPage == index
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(3.dp)
                        .clip(SeriesTokens.shapePill)
                        .background(
                            if (isCurrent) QYellowPrimary
                            else Color.White.copy(alpha = 0.3f)
                        )
                )
            }
        }
    }
}

private fun isWithin7Days(isoDate: String?): Boolean {
    if (isoDate.isNullOrBlank()) return false
    return try {
        val dt = Instant.parse(isoDate)
        val now = Instant.now()
        val days = ChronoUnit.DAYS.between(dt, now)
        days in 0..7
    } catch (_: Exception) {
        false
    }
}
