package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
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
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.temporal.ChronoUnit

@Composable
fun SeriesCardItem(
    card: SeriesCard,
    isInWatchlist: Boolean,
    onCardClick: () -> Unit,
    onWatchlistClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isNew = remember(card.lastEpisodeAt) {
        isWithin7Days(card.lastEpisodeAt)
    }

    Column(
        modifier = modifier
            .width(130.dp)
            .clickable { onCardClick() }
            .testTag("series_card_${card.id}")
    ) {
        // Poster (2:3 Aspect Ratio)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(2f / 3f)
                .clip(RoundedCornerShape(10.dp))
                .background(QSurfaceVariant)
                .border(0.5.dp, QCardBorder, RoundedCornerShape(10.dp))
        ) {
            AsyncImage(
                model = card.posterUrl,
                contentDescription = card.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // Top Badges Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                // Left badge: NEW or 18+
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (isNew) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(QYellowDark)
                                .padding(horizontal = 5.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "NEW",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.Black
                            )
                        }
                    }
                    if (card.isMature) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFFD32F2F))
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "18+",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }

                // Watchlist toggle button
                IconButton(
                    onClick = onWatchlistClick,
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.5f))
                        .testTag("watchlist_button_${card.id}")
                ) {
                    Icon(
                        imageVector = if (isInWatchlist) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                        contentDescription = "Watchlist",
                        tint = if (isInWatchlist) QYellowPrimary else Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // Bottom Gradient with Rating & Language
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))
                        )
                    )
                    .padding(horizontal = 6.dp, vertical = 6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Rating
                    if ((card.rating ?: 0.0) > 0.0) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = QYellowPrimary,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = String.format("%.1f", card.rating),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.width(1.dp))
                    }

                    // First language or "Sub"
                    val lang = card.languages.firstOrNull()
                    if (!lang.isNullOrBlank()) {
                        Text(
                            text = lang.take(5),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Title
        Text(
            text = card.title,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = QOnSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            lineHeight = 15.sp
        )
    }
}

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
            .width(160.dp)
            .clickable { onCardClick() }
            .testTag("top10_card_${card.id}")
    ) {
        // Large stylized rank number behind/beside poster
        Text(
            text = "$rank",
            fontSize = 72.sp,
            fontWeight = FontWeight.Black,
            color = QYellowPrimary.copy(alpha = 0.35f),
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
                modifier = Modifier.width(125.dp)
            )
        }
    }
}

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

    val s = item.series
    val ep = item.episode
    val idTag = ep?.id ?: s?.id ?: "unknown"

    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = QSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, QCardBorder),
        modifier = modifier
            .width(200.dp)
            .clickable { onClick() }
            .testTag("continue_watching_$idTag")
    ) {
        Column {
            // Thumbnail (16:9)
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

                // Play icon overlay
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.6f))
                        .align(Alignment.Center)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Resume",
                        tint = QYellowPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                }

                // Progress Bar at Bottom
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .align(Alignment.BottomCenter),
                    color = QYellowPrimary,
                    trackColor = Color.White.copy(alpha = 0.3f),
                )
            }

            Column(modifier = Modifier.padding(8.dp)) {
                Text(
                    text = s?.title ?: "Series",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = QOnSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "S${ep?.seasonNumber ?: 1} E${ep?.episodeNumber ?: 1}: ${ep?.title ?: "Episode"}",
                    fontSize = 11.sp,
                    color = QOnSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

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

    // Auto-slide every 5 seconds
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
            .height(280.dp)
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
            val hasBanner = !series.bannerUrl.isNullOrBlank()

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable { onCardClick(series) }
            ) {
                // Background image: banner or blurred poster fallback
                if (hasBanner) {
                    AsyncImage(
                        model = series.bannerUrl,
                        contentDescription = series.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    // Blurred poster fallback + sharp poster
                    AsyncImage(
                        model = series.posterUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .blur(20.dp)
                    )
                    // Sharp poster in center-end
                    AsyncImage(
                        model = series.posterUrl,
                        contentDescription = series.title,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(end = 20.dp, top = 20.dp, bottom = 40.dp)
                            .aspectRatio(2f / 3f)
                            .clip(RoundedCornerShape(8.dp))
                    )
                }

                // Dark gradient overlay for readable text
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

                // Hero Content at bottom
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(16.dp)
                        .fillMaxWidth(0.75f)
                ) {
                    // Genres / Category chips
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        series.genres.take(2).forEach { genre ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color.White.copy(alpha = 0.2f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = genre,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color.White
                                )
                            }
                        }

                        if ((series.rating ?: 0.0) > 0.0) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = null,
                                    tint = QYellowPrimary,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = String.format("%.1f", series.rating),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = series.title,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Buttons: Play and + My List
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = { onPlayClick(series) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = QYellowPrimary,
                                contentColor = QOnYellow
                            ),
                            shape = RoundedCornerShape(20.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("hero_play_button")
                        ) {
                            Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Play", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }

                        val inList = isInWatchlist(series.id)
                        OutlinedButton(
                            onClick = { onWatchlistToggle(series) },
                            shape = RoundedCornerShape(20.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = if (inList) QYellowPrimary else Color.White
                            ),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (inList) QYellowPrimary else Color.White.copy(alpha = 0.7f)
                            ),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("hero_watchlist_button")
                        ) {
                            Icon(
                                imageVector = if (inList) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                contentDescription = null,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (inList) "In List" else "+ My List", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }
        }

        // Pager indicator dots at bottom center
        Row(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            repeat(items.size) { index ->
                val isSelected = pagerState.currentPage == index
                Box(
                    modifier = Modifier
                        .size(if (isSelected) 8.dp else 5.dp)
                        .clip(CircleShape)
                        .background(if (isSelected) QYellowPrimary else Color.White.copy(alpha = 0.4f))
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
