package com.example.ui.components

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

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        // View Mode Switcher Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${episodes.size} EPISODES",
                fontSize = 12.sp,
                fontWeight = FontWeight.ExtraBold,
                color = QOnSurfaceVariant,
                letterSpacing = 0.5.sp
            )

            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(QSurfaceVariant)
                    .border(1.dp, QCardBorder, RoundedCornerShape(8.dp))
                    .padding(2.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                IconButton(
                    onClick = { isGridView = true },
                    modifier = Modifier
                        .size(30.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isGridView) QYellowPrimary else Color.Transparent)
                ) {
                    Icon(
                        imageVector = Icons.Default.GridView,
                        contentDescription = "Grid View",
                        tint = if (isGridView) QOnYellow else QOnSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                }

                IconButton(
                    onClick = { isGridView = false },
                    modifier = Modifier
                        .size(30.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (!isGridView) QYellowPrimary else Color.Transparent)
                ) {
                    Icon(
                        imageVector = Icons.Default.List,
                        contentDescription = "List View",
                        tint = if (!isGridView) QOnYellow else QOnSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        if (isGridView) {
            // WebApp Screenshot Style 6-Column Yellow Numbered Grid
            val rows = remember(episodes) { episodes.chunked(6) }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
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
                        repeat(6 - rowEps.size) {
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
                    .padding(horizontal = 12.dp, vertical = 6.dp),
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
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = if (isPlaying) Color(0xFF1E1B18) else QYellowPrimary,
        border = if (isPlaying) androidx.compose.foundation.BorderStroke(2.dp, QYellowPrimary) else null,
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f) // Square tile matching WebApp Screenshot 1
            .testTag("episode_grid_item_${episode.episodeNumber}")
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            Text(
                text = "${episode.episodeNumber}",
                fontSize = 17.sp,
                fontWeight = FontWeight.Black,
                color = if (isPlaying) QYellowPrimary else QOnYellow
            )
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
        color = if (isPlaying) QYellowLight else QSurface,
        border = androidx.compose.foundation.BorderStroke(
            width = if (isPlaying) 1.5.dp else 1.dp,
            color = if (isPlaying) QYellowPrimary else QCardBorder
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
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isPlaying) Color(0xFF1E1B18) else QYellowPrimary),
                contentAlignment = Alignment.Center
            ) {
                if (isPlaying) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Playing",
                        tint = QYellowPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                } else {
                    Text(
                        text = String.format("%02d", episode.episodeNumber),
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = QOnYellow
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = episode.title?.takeIf { it.isNotBlank() } ?: "Episode ${episode.episodeNumber}",
                    fontWeight = if (isPlaying) FontWeight.Bold else FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = QOnSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (!episode.description.isNullOrBlank()) {
                    Text(
                        text = episode.description,
                        fontSize = 12.sp,
                        color = QOnSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            if (isPlaying) {
                Surface(
                    shape = CircleShape,
                    color = QYellowPrimary,
                    modifier = Modifier.size(8.dp)
                ) {}
            }
        }
    }
}
