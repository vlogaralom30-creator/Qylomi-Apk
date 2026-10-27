package com.example.ui.components.series

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Episode
import com.example.data.model.Season
import com.example.data.model.Series
import com.example.ui.components.EpisodeThumbnail
import com.example.ui.theme.*
import java.time.Instant
import java.time.temporal.ChronoUnit

/**
 * Premium Golden Cinema Episode Selector Component
 * Used in both SeriesDetailsScreen and EpisodePlayerScreen
 */
@Composable
fun EpisodeSelector(
    episodes: List<Episode>,
    seasons: List<Season> = emptyList(),
    series: Series? = null,
    currentEpisodeId: String? = null,
    onEpisodeClick: (Episode) -> Unit,
    modifier: Modifier = Modifier,
    initialViewModeIsGrid: Boolean = true
) {
    var isGridView by remember { mutableStateOf(initialViewModeIsGrid) }
    var selectedSeasonNumber by remember {
        mutableIntStateOf(
            episodes.firstOrNull { it.id == currentEpisodeId }?.seasonNumber
                ?: seasons.firstOrNull()?.seasonNumber
                ?: 1
        )
    }
    var jumpEpisodeText by remember { mutableStateOf("") }
    var showJumpInput by remember { mutableStateOf(false) }
    var selectedRangeIndex by remember { mutableIntStateOf(0) }
    val focusManager = LocalFocusManager.current

    // Distinct seasons
    val seasonNumbers = remember(episodes, seasons) {
        val fromEp = episodes.map { it.seasonNumber }
        val fromSeas = seasons.map { it.seasonNumber }
        (fromEp + fromSeas).distinct().sorted().ifEmpty { listOf(1) }
    }

    // Filter episodes for current season
    val seasonEpisodes = remember(episodes, selectedSeasonNumber) {
        val filtered = episodes.filter { it.seasonNumber == selectedSeasonNumber }
        filtered.sortedBy { it.episodeNumber }
    }

    // Range chunking for long series (> 50 episodes)
    val chunkSize = 50
    val isLongSeries = seasonEpisodes.size > chunkSize
    val ranges = remember(seasonEpisodes.size) {
        if (seasonEpisodes.size <= chunkSize) emptyList()
        else {
            val count = (seasonEpisodes.size + chunkSize - 1) / chunkSize
            (0 until count).map { i ->
                val start = i * chunkSize + 1
                val end = minOf((i + 1) * chunkSize, seasonEpisodes.size)
                "$start-$end"
            }
        }
    }

    val displayedEpisodes = remember(seasonEpisodes, selectedRangeIndex, isLongSeries) {
        if (!isLongSeries || ranges.isEmpty()) seasonEpisodes
        else {
            val start = selectedRangeIndex * chunkSize
            val end = minOf(start + chunkSize, seasonEpisodes.size)
            seasonEpisodes.subList(start, end)
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        // Season Selector (Segmented pills or dropdown if > 4)
        if (seasonNumbers.size > 1) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                items(seasonNumbers) { sNum ->
                    val isSelected = selectedSeasonNumber == sNum
                    val customTitle = seasons.firstOrNull { it.seasonNumber == sNum }?.title
                    Surface(
                        onClick = {
                            selectedSeasonNumber = sNum
                            selectedRangeIndex = 0
                        },
                        shape = SeriesTokens.shapePill,
                        color = if (isSelected) QYellowPrimary else QSurfaceVariant,
                        border = BorderStroke(
                            0.75.dp,
                            if (isSelected) QYellowPrimary else QCardBorder
                        ),
                        modifier = Modifier.pressScale(onClick = {
                            selectedSeasonNumber = sNum
                            selectedRangeIndex = 0
                        })
                    ) {
                        Text(
                            text = customTitle ?: "Season $sNum",
                            style = SeriesTypography.label,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) QOnYellow else QOnSurface,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                        )
                    }
                }
            }
        }

        // Top Toolbar: Range chips (if long series), search/jump box, grid/list view switcher
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${seasonEpisodes.size} Episodes",
                style = SeriesTypography.label,
                fontWeight = FontWeight.Bold,
                color = QOnSurfaceVariant
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Jump to Episode Icon Button
                IconButton(
                    onClick = { showJumpInput = !showJumpInput },
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(
                        imageVector = if (showJumpInput) Icons.Default.Close else Icons.Default.Search,
                        contentDescription = "Jump to episode",
                        tint = if (showJumpInput) QYellowDark else QOnSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Grid / List Toggle
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = QSurfaceVariant,
                    border = BorderStroke(0.5.dp, QCardBorder),
                    modifier = Modifier.padding(2.dp)
                ) {
                    Row(modifier = Modifier.padding(2.dp)) {
                        IconButton(
                            onClick = { isGridView = true },
                            modifier = Modifier
                                .size(28.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isGridView) QYellowPrimary else Color.Transparent)
                        ) {
                            Icon(
                                imageVector = Icons.Default.GridView,
                                contentDescription = "Grid View",
                                tint = if (isGridView) QOnYellow else QOnSurfaceVariant,
                                modifier = Modifier.size(15.dp)
                            )
                        }

                        IconButton(
                            onClick = { isGridView = false },
                            modifier = Modifier
                                .size(28.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (!isGridView) QYellowPrimary else Color.Transparent)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ViewList,
                                contentDescription = "List View",
                                tint = if (!isGridView) QOnYellow else QOnSurfaceVariant,
                                modifier = Modifier.size(17.dp)
                            )
                        }
                    }
                }
            }
        }

        // Jump to episode input field
        AnimatedVisibility(visible = showJumpInput) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = jumpEpisodeText,
                    onValueChange = { input ->
                        jumpEpisodeText = input.filter { it.isDigit() }
                    },
                    placeholder = { Text("Jump to episode #", fontSize = 12.sp) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Go
                    ),
                    keyboardActions = KeyboardActions(
                        onGo = {
                            val epNum = jumpEpisodeText.toIntOrNull()
                            if (epNum != null) {
                                val match = seasonEpisodes.firstOrNull { it.episodeNumber == epNum }
                                if (match != null) {
                                    onEpisodeClick(match)
                                    focusManager.clearFocus()
                                }
                            }
                        }
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = QYellowPrimary,
                        unfocusedBorderColor = QOutline,
                        focusedContainerColor = QSurface,
                        unfocusedContainerColor = QSurface
                    ),
                    shape = SeriesTokens.shapePill,
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = {
                        val epNum = jumpEpisodeText.toIntOrNull()
                        if (epNum != null) {
                            val match = seasonEpisodes.firstOrNull { it.episodeNumber == epNum }
                            if (match != null) {
                                onEpisodeClick(match)
                                focusManager.clearFocus()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = QYellowPrimary,
                        contentColor = QOnYellow
                    ),
                    shape = SeriesTokens.shapePill,
                    modifier = Modifier.height(44.dp)
                ) {
                    Text("Go", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }

        // Range chips for series with many episodes (e.g. 1-50, 51-100)
        if (isLongSeries && ranges.isNotEmpty()) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                itemsIndexed(ranges) { index, rangeLabel ->
                    val isRangeSelected = selectedRangeIndex == index
                    FilterChip(
                        selected = isRangeSelected,
                        onClick = { selectedRangeIndex = index },
                        label = { Text(rangeLabel, fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = QYellowPrimary,
                            selectedLabelColor = QOnYellow,
                            containerColor = QSurfaceVariant,
                            labelColor = QOnSurface
                        ),
                        shape = SeriesTokens.shapeChip,
                        border = null
                    )
                }
            }
        }

        // Episodes View (Grid or List)
        if (displayedEpisodes.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No episodes released for this season yet.",
                    style = SeriesTypography.body,
                    color = QOnSurfaceVariant
                )
            }
        } else if (isGridView) {
            // Compact numbered tiles (6 per row)
            val rows = remember(displayedEpisodes) { displayedEpisodes.chunked(6) }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                rows.forEach { rowEps ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        rowEps.forEach { ep ->
                            val isCurrent = ep.id == currentEpisodeId
                            Box(modifier = Modifier.weight(1f)) {
                                EpisodeGridTile(
                                    episode = ep,
                                    isCurrent = isCurrent,
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
            // Detailed List View
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                displayedEpisodes.forEachIndexed { index, ep ->
                    val isCurrent = ep.id == currentEpisodeId
                    EpisodeListItemCard(
                        episode = ep,
                        series = series,
                        isCurrent = isCurrent,
                        onClick = { onEpisodeClick(ep) }
                    )
                }
            }
        }
    }
}

/**
 * Compact Episode Grid Tile
 */
@Composable
private fun EpisodeGridTile(
    episode: Episode,
    isCurrent: Boolean,
    onClick: () -> Unit
) {
    val isWatched = episode.isCompleted || episode.watchedProgressPercent >= 90.0

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = when {
            isCurrent -> QYellowPrimary
            isWatched -> QYellowLight
            else -> QSurface
        },
        border = BorderStroke(
            1.dp,
            when {
                isCurrent -> QYellowPrimary
                isWatched -> QYellowDark.copy(alpha = 0.4f)
                else -> QOutline
            }
        ),
        shadowElevation = if (isCurrent) 3.dp else 0.dp,
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .pressScale(onClick = onClick)
            .testTag("episode_grid_tile_${episode.episodeNumber}")
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "${episode.episodeNumber}",
                    fontSize = 16.sp,
                    fontWeight = if (isCurrent) FontWeight.Black else FontWeight.Bold,
                    color = when {
                        isCurrent -> QOnYellow
                        isWatched -> QYellowDark
                        else -> QOnSurface
                    }
                )
                if (isWatched && !isCurrent) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Watched",
                        tint = QYellowDark,
                        modifier = Modifier.size(10.dp)
                    )
                }
            }
        }
    }
}

/**
 * Detailed Episode List Item Card
 */
@Composable
private fun EpisodeListItemCard(
    episode: Episode,
    series: Series?,
    isCurrent: Boolean,
    onClick: () -> Unit
) {
    val isNew = remember(episode.airDate) {
        isEpisodeNew(episode.airDate)
    }

    Surface(
        onClick = onClick,
        shape = SeriesTokens.shapeCard,
        color = if (isCurrent) QYellowLight.copy(alpha = 0.5f) else QSurface,
        border = BorderStroke(
            width = if (isCurrent) 1.5.dp else 1.dp,
            color = if (isCurrent) QYellowPrimary else QCardBorder
        ),
        shadowElevation = if (isCurrent) 2.dp else 0.5.dp,
        modifier = Modifier
            .fillMaxWidth()
            .pressScale(onClick = onClick)
            .testTag("episode_list_card_${episode.episodeNumber}")
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 16:9 Thumbnail Container
            Box(modifier = Modifier.width(118.dp)) {
                EpisodeThumbnail(
                    episode = episode,
                    series = series,
                    showBadges = true,
                    isPlaying = isCurrent
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Details Column
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "E${episode.episodeNumber}",
                        style = SeriesTypography.micro,
                        fontWeight = FontWeight.Bold,
                        color = if (isCurrent) QYellowDark else QOnSurfaceVariant
                    )

                    if (isNew) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = QYellowDark
                        ) {
                            Text(
                                text = "NEW",
                                style = SeriesTypography.micro.copy(fontSize = 9.sp),
                                fontWeight = FontWeight.Black,
                                color = Color.Black,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }

                    if (isCurrent) {
                        AnimatedEqualizer(
                            color = QYellowDark,
                            modifier = Modifier.padding(start = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = episode.title?.takeIf { it.isNotBlank() } ?: "Episode ${episode.episodeNumber}",
                    style = SeriesTypography.title.copy(fontSize = 14.sp),
                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.SemiBold,
                    color = QOnSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (episode.durationSeconds != null && episode.durationSeconds > 0) {
                        Text(
                            text = "${episode.durationSeconds / 60} min",
                            style = SeriesTypography.micro,
                            color = QOnSurfaceVariant
                        )
                    }
                    if (!episode.airDate.isNullOrBlank()) {
                        Text(
                            text = "• ${episode.airDate}",
                            style = SeriesTypography.micro,
                            color = QOnSurfaceVariant
                        )
                    }
                }

                if (!episode.description.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = episode.description,
                        style = SeriesTypography.micro,
                        color = QOnSurfaceVariant.copy(alpha = 0.8f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

private fun isEpisodeNew(isoDate: String?): Boolean {
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
