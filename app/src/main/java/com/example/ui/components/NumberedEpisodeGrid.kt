package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Episode
import com.example.ui.theme.*

@Composable
fun NumberedEpisodeGrid(
    episodes: List<Episode>,
    currentEpisodeId: String? = null,
    onEpisodeClick: (Episode) -> Unit,
    modifier: Modifier = Modifier,
    initialViewModeIsGrid: Boolean = true
) {
    var isGridView by remember { mutableStateOf(initialViewModeIsGrid) }

    Column(modifier = modifier.fillMaxWidth()) {
        // Header & View Mode Switcher
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${episodes.size} Episodes",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White.copy(alpha = 0.8f)
            )

            // View toggle: Numbered Grid vs List Cards
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(QDarkSurface)
                    .border(1.dp, QDarkBorder, RoundedCornerShape(8.dp))
                    .padding(2.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                IconButton(
                    onClick = { isGridView = true },
                    modifier = Modifier
                        .size(30.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isGridView) QCrunchyOrange else Color.Transparent)
                ) {
                    Icon(
                        imageVector = Icons.Default.GridView,
                        contentDescription = "Grid View",
                        tint = if (isGridView) Color.Black else Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.size(16.dp)
                    )
                }

                IconButton(
                    onClick = { isGridView = false },
                    modifier = Modifier
                        .size(30.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (!isGridView) QCrunchyOrange else Color.Transparent)
                ) {
                    Icon(
                        imageVector = Icons.Default.List,
                        contentDescription = "List View",
                        tint = if (!isGridView) Color.Black else Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        if (isGridView) {
            // Crunchyroll Style Numbered Grid: 4 items per row
            val rows = remember(episodes) { episodes.chunked(4) }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                rows.forEach { rowEps ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        rowEps.forEach { ep ->
                            Box(modifier = Modifier.weight(1f)) {
                                NumberedEpisodeBadge(
                                    episode = ep,
                                    isPlaying = ep.id == currentEpisodeId,
                                    onClick = { onEpisodeClick(ep) }
                                )
                            }
                        }
                        // Fill empty slots in incomplete final row
                        repeat(4 - rowEps.size) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        } else {
            // Detailed List view
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                episodes.forEach { ep ->
                    DetailedEpisodeRow(
                        episode = ep,
                        isPlaying = ep.id == currentEpisodeId,
                        onClick = { onEpisodeClick(ep) }
                    )
                }
            }
        }
    }
}

@Composable
fun NumberedEpisodeBadge(
    episode: Episode,
    isPlaying: Boolean,
    onClick: () -> Unit
) {
    val isCompleted = episode.isCompleted || episode.watchedProgressPercent >= 0.9
    val formattedNumber = String.format("%02d", episode.episodeNumber)

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = when {
            isPlaying -> QCrunchyOrange.copy(alpha = 0.2f)
            else -> QDarkSurface
        },
        border = androidx.compose.foundation.BorderStroke(
            width = if (isPlaying) 2.dp else 1.dp,
            color = if (isPlaying) QCrunchyOrange else QDarkBorder
        ),
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .testTag("episode_grid_item_${episode.episodeNumber}")
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 6.dp, vertical = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    if (isPlaying) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Playing",
                            tint = QCrunchyOrange,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                    } else if (isCompleted) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Watched",
                            tint = Color(0xFF4ADE80),
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                    }

                    Text(
                        text = formattedNumber,
                        fontSize = if (isPlaying) 17.sp else 16.sp,
                        fontWeight = if (isPlaying) FontWeight.Bold else FontWeight.Medium,
                        color = when {
                            isPlaying -> QCrunchyOrange
                            else -> Color.White
                        }
                    )
                }

                if (!episode.title.isNullOrBlank()) {
                    Text(
                        text = episode.title,
                        fontSize = 10.sp,
                        color = if (isPlaying) QCrunchyOrange else Color.White.copy(alpha = 0.6f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Progress bar at the bottom
            if (episode.watchedProgressPercent > 0 && !isCompleted) {
                LinearProgressIndicator(
                    progress = { episode.watchedProgressPercent.toFloat() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .align(Alignment.BottomCenter),
                    color = QCrunchyOrange,
                    trackColor = Color.White.copy(alpha = 0.1f)
                )
            }
        }
    }
}

@Composable
fun DetailedEpisodeRow(
    episode: Episode,
    isPlaying: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = if (isPlaying) QCrunchyOrange.copy(alpha = 0.15f) else QDarkSurface,
        border = androidx.compose.foundation.BorderStroke(
            width = if (isPlaying) 1.5.dp else 1.dp,
            color = if (isPlaying) QCrunchyOrange else QDarkBorder
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("episode_list_item_${episode.episodeNumber}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Episode Number Box
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isPlaying) QCrunchyOrange else QDarkBorder.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                if (isPlaying) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Playing",
                        tint = Color.Black,
                        modifier = Modifier.size(22.dp)
                    )
                } else {
                    Text(
                        text = String.format("%02d", episode.episodeNumber),
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = episode.title?.takeIf { it.isNotBlank() } ?: "Episode ${episode.episodeNumber}",
                    fontWeight = if (isPlaying) FontWeight.Bold else FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = if (isPlaying) QCrunchyOrange else Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (!episode.description.isNullOrBlank()) {
                    Text(
                        text = episode.description,
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.6f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                episode.durationSeconds?.let { dur ->
                    Text(
                        text = "${dur / 60}m ${dur % 60}s",
                        fontSize = 11.sp,
                        color = QCrunchyOrange.copy(alpha = 0.9f)
                    )
                }
            }

            if (isPlaying) {
                Surface(
                    shape = CircleShape,
                    color = QCrunchyOrange,
                    modifier = Modifier.size(8.dp)
                ) {}
            }
        }
    }
}
