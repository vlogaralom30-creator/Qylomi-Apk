package com.example.ui.screens.series

import android.content.Intent
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.model.*
import com.example.ui.components.EpisodePlayerView
import com.example.ui.components.SeriesCardItem
import com.example.ui.components.series.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.PlayerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SeriesPlayerScreen(
    seriesId: String,
    episodeId: String? = null,
    initialPositionSec: Int = 0,
    onBackClick: () -> Unit,
    onNavigateToSeries: (String) -> Unit,
    onOpenAuthDialog: (() -> Unit)? = null,
    viewModel: PlayerViewModel = viewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    // Load series and episode playback state
    LaunchedEffect(seriesId, episodeId) {
        viewModel.loadEpisodeData(seriesId, episodeId, initialPositionSec)
    }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearErrorMessage()
        }
    }

    LaunchedEffect(uiState.reportSuccessMessage) {
        uiState.reportSuccessMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = QBackground
    ) { innerPadding ->
        if (uiState.isLoadingSeries && uiState.series == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = QYellowPrimary)
            }
            return@Scaffold
        }

        val currentEp = uiState.currentEpisode
        val activeSource = uiState.activeSource
        val currentSeries = uiState.series

        val currentIndex = remember(uiState.episodes, currentEp) {
            if (currentEp == null) -1
            else uiState.episodes.indexOfFirst { it.id == currentEp.id }
        }
        val hasPrevEp = currentIndex > 0
        val hasNextEp = currentIndex in 0 until (uiState.episodes.size - 1)
        val nextEp = if (hasNextEp) uiState.episodes.getOrNull(currentIndex + 1) else null

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // 1. Media3 ExoPlayer View (Pinned 16:9 full width)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .background(SeriesTokens.StageBlack)
            ) {
                if (activeSource != null && currentEp != null) {
                    EpisodePlayerView(
                        rawVideoUrl = activeSource.url,
                        initialPositionSec = uiState.initialSeekPositionSec,
                        showSkipIntro = uiState.showSkipIntro,
                        introStartSec = currentEp.introStartSec ?: 0,
                        introEndSec = currentEp.introEndSec ?: 0,
                        fallbackNotice = uiState.fallbackNotice,
                        autoplayCountdown = uiState.autoplayCountdown,
                        nextEpisodeTitle = nextEp?.title,
                        episodeNumber = currentEp.episodeNumber,
                        episodeTitle = currentEp.title,
                        seriesTitle = currentSeries?.title,
                        backdropUrl = currentEp.thumbnailUrl ?: currentSeries?.effectiveBannerUrl,
                        serverBadge = "${activeSource.label}${if (!activeSource.quality.isNullOrBlank()) " • ${activeSource.quality}" else ""}",
                        hasPrevEp = hasPrevEp,
                        hasNextEp = hasNextEp,
                        onPositionUpdate = { pos, dur ->
                            viewModel.updatePlaybackPosition(pos, dur)
                        },
                        onVideoReady = { dur ->
                            viewModel.onVideoReady(dur)
                        },
                        onError = { err ->
                            viewModel.handleSourcePlaybackError(err)
                        },
                        onEnded = {
                            viewModel.onVideoEnded()
                        },
                        onPlayPrevClick = if (hasPrevEp) {
                            { viewModel.selectEpisode(uiState.episodes[currentIndex - 1]) }
                        } else null,
                        onPlayNextClick = {
                            viewModel.playNextEpisode()
                        },
                        onCancelAutoplayClick = {
                            viewModel.cancelAutoplayCountdown()
                        },
                        onRetrySource = {
                            viewModel.selectSourceManually(activeSource)
                        },
                        onBackClick = onBackClick
                    )
                } else if (uiState.playerError != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black)
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Error",
                                tint = QLikeRed,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = uiState.playerError ?: "No sources available",
                                color = Color.White,
                                style = SeriesTypography.body
                            )
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = QYellowPrimary)
                    }
                }
            }

            // 2. Scrollable Body
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                // Series Link & Episode Info Header
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        if (currentSeries != null) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(SeriesTokens.shapePill)
                                    .clickable { onNavigateToSeries(currentSeries.slug) }
                                    .padding(vertical = 2.dp)
                            ) {
                                Text(
                                    text = currentSeries.title,
                                    style = SeriesTypography.label,
                                    fontWeight = FontWeight.Bold,
                                    color = QYellowDark
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Icon(
                                    imageVector = Icons.Default.ChevronRight,
                                    contentDescription = null,
                                    tint = QYellowDark,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                        }

                        Text(
                            text = if (currentEp != null) {
                                "S${currentEp.seasonNumber} - E${currentEp.episodeNumber} - ${currentEp.title ?: "Episode ${currentEp.episodeNumber}"}"
                            } else "Playing Episode",
                            style = SeriesTypography.headline.copy(fontSize = 18.sp),
                            color = QOnSurface
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        // Meta Row
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (!currentEp?.airDate.isNullOrBlank()) {
                                MetaChip(text = currentEp.airDate)
                            }
                            MetaChip(text = "${currentEp?.viewsCount ?: 0} views")
                            if (!activeSource?.language.isNullOrBlank()) {
                                MetaChip(text = activeSource.language, isHighlighted = true)
                            }
                            if (!activeSource?.quality.isNullOrBlank()) {
                                MetaChip(text = activeSource.quality)
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Action Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            HeartBurstButton(
                                isLiked = uiState.isEpisodeLiked,
                                likesCount = uiState.episodeLikesCount,
                                onClick = {
                                    if (viewModel.isUserLoggedIn) viewModel.toggleEpisodeLike()
                                    else onOpenAuthDialog?.invoke()
                                },
                                modifier = Modifier.weight(1f)
                            )

                            AnimatedBookmark(
                                isInList = uiState.isInWatchlist,
                                onClick = {
                                    if (viewModel.isUserLoggedIn) viewModel.toggleWatchlist()
                                    else onOpenAuthDialog?.invoke()
                                },
                                modifier = Modifier.weight(1f)
                            )

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(SeriesTokens.shapeChip)
                                    .clickable {
                                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                            type = "text/plain"
                                            putExtra(
                                                Intent.EXTRA_TEXT,
                                                "Watch ${currentSeries?.title} - ${currentEp?.title} on Qylomi!"
                                            )
                                        }
                                        context.startActivity(Intent.createChooser(shareIntent, "Share Episode"))
                                    }
                                    .padding(vertical = 6.dp)
                            ) {
                                Icon(Icons.Outlined.Share, contentDescription = "Share", tint = QOnSurfaceVariant, modifier = Modifier.size(24.dp))
                                Spacer(modifier = Modifier.height(2.dp))
                                Text("Share", style = SeriesTypography.micro, color = QOnSurfaceVariant)
                            }

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(SeriesTokens.shapeChip)
                                    .clickable {
                                        if (viewModel.isUserLoggedIn) viewModel.openReportSheet()
                                        else onOpenAuthDialog?.invoke()
                                    }
                                    .padding(vertical = 6.dp)
                            ) {
                                Icon(Icons.Outlined.Flag, contentDescription = "Report", tint = QOnSurfaceVariant, modifier = Modifier.size(24.dp))
                                Spacer(modifier = Modifier.height(2.dp))
                                Text("Report", style = SeriesTypography.micro, color = QOnSurfaceVariant)
                            }
                        }
                    }
                }

                // Servers Section
                if (uiState.sources.isNotEmpty()) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "STREAM SERVERS",
                                style = SeriesTypography.micro.copy(fontSize = 11.sp),
                                fontWeight = FontWeight.ExtraBold,
                                color = QOnSurfaceVariant,
                                letterSpacing = 0.5.sp
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(uiState.sources, key = { it.id }) { src ->
                                    val isSelected = src.id == activeSource?.id
                                    val isFailing = src.failCount >= 3

                                    Surface(
                                        onClick = { viewModel.selectSourceManually(src) },
                                        shape = SeriesTokens.shapePill,
                                        color = if (isSelected) QYellowPrimary else QSurfaceVariant,
                                        border = BorderStroke(
                                            1.dp,
                                            if (isSelected) QYellowPrimary else if (isFailing) QLikeRed else QCardBorder
                                        ),
                                        modifier = Modifier.pressScale(onClick = { viewModel.selectSourceManually(src) })
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "${src.label}${if (!src.quality.isNullOrBlank()) " • ${src.quality}" else ""}",
                                                style = SeriesTypography.label,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                color = if (isSelected) QOnYellow else QOnSurface
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Up Next Card
                if (nextEp != null) {
                    item {
                        Card(
                            shape = SeriesTokens.shapeCard,
                            colors = CardDefaults.cardColors(containerColor = QSurface),
                            border = BorderStroke(1.dp, QCardBorder),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                                .clickable { viewModel.selectEpisode(nextEp) }
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                PosterImage(
                                    posterUrl = nextEp.thumbnailUrl,
                                    bannerUrl = currentSeries?.effectiveBannerUrl,
                                    title = nextEp.title,
                                    modifier = Modifier
                                        .width(96.dp)
                                        .aspectRatio(16f / 9f)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "UP NEXT",
                                        style = SeriesTypography.micro.copy(fontSize = 10.sp),
                                        fontWeight = FontWeight.Black,
                                        color = QYellowDark
                                    )
                                    Text(
                                        text = "E${nextEp.episodeNumber} - ${nextEp.title ?: "Episode ${nextEp.episodeNumber}"}",
                                        style = SeriesTypography.title.copy(fontSize = 13.sp),
                                        fontWeight = FontWeight.Bold,
                                        color = QOnSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                IconButton(
                                    onClick = { viewModel.selectEpisode(nextEp) },
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(QYellowPrimary)
                                ) {
                                    Icon(Icons.Default.PlayArrow, contentDescription = "Play next", tint = QOnYellow)
                                }
                            }
                        }
                    }
                }

                // Sticky Tabs
                val commentTabIndex = if (uiState.selectedTab == 0) 1 else uiState.selectedTab
                item {
                    TabRow(
                        selectedTabIndex = if (commentTabIndex == 2) 1 else 0,
                        containerColor = QSurface,
                        contentColor = QOnSurface,
                        indicator = { tabPositions ->
                            val currentPos = if (commentTabIndex == 2) 1 else 0
                            if (currentPos < tabPositions.size) {
                                TabRowDefaults.SecondaryIndicator(
                                    modifier = Modifier.tabIndicatorOffset(tabPositions[currentPos]),
                                    color = QYellowPrimary
                                )
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                    ) {
                        Tab(
                            selected = commentTabIndex != 2,
                            onClick = { viewModel.setSelectedTab(1) },
                            text = {
                                Text(
                                    "Comments (${uiState.comments.size})",
                                    style = SeriesTypography.label,
                                    fontWeight = FontWeight.Bold,
                                    color = if (commentTabIndex != 2) QYellowDark else QOnSurfaceVariant
                                )
                            },
                            modifier = Modifier.testTag("series_player_tab_comments")
                        )
                        Tab(
                            selected = commentTabIndex == 2,
                            onClick = { viewModel.setSelectedTab(2) },
                            text = {
                                Text(
                                    "More Like This",
                                    style = SeriesTypography.label,
                                    fontWeight = FontWeight.Bold,
                                    color = if (commentTabIndex == 2) QYellowDark else QOnSurfaceVariant
                                )
                            },
                            modifier = Modifier.testTag("series_player_tab_related")
                        )
                    }
                }

                // Tab Content
                when (uiState.selectedTab) {
                    0 -> { // EPISODES
                        item {
                            EpisodeSelector(
                                episodes = uiState.episodes,
                                seasons = uiState.seasons,
                                series = currentSeries,
                                currentEpisodeId = currentEp?.id,
                                onEpisodeClick = { ep -> viewModel.selectEpisode(ep) },
                                initialViewModeIsGrid = true,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                    }

                    1 -> { // COMMENTS
                        item {
                            SeriesCommentsSection(
                                comments = uiState.comments,
                                isLoading = uiState.isCommentsLoading,
                                currentUserId = viewModel.currentUserId,
                                commentInput = uiState.commentInput,
                                replyingTo = uiState.replyingToComment,
                                editingComment = uiState.editingComment,
                                seriesCreatorId = currentSeries?.createdBy,
                                onInputChange = { viewModel.setCommentInput(it) },
                                onPostComment = {
                                    if (viewModel.isUserLoggedIn) viewModel.postComment()
                                    else onOpenAuthDialog?.invoke()
                                },
                                onReply = { viewModel.setReplyingTo(it) },
                                onEdit = { viewModel.setEditingComment(it) },
                                onDelete = { viewModel.deleteComment(it.id) },
                                onToggleLike = {
                                    if (viewModel.isUserLoggedIn) viewModel.toggleCommentLike(it)
                                    else onOpenAuthDialog?.invoke()
                                },
                                onCancelReplyOrEdit = {
                                    viewModel.setReplyingTo(null)
                                    viewModel.setEditingComment(null)
                                    viewModel.setCommentInput("")
                                },
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                    }

                    2 -> { // RELATED SERIES
                        item {
                            if (uiState.relatedSeries.isEmpty()) {
                                SeriesEmptyState(
                                    icon = Icons.Default.Info,
                                    title = "No related titles found",
                                    description = "Discover more series on the home page."
                                )
                            } else {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    uiState.relatedSeries.chunked(3).forEach { rowCards ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            rowCards.forEach { card ->
                                                SeriesCardItem(
                                                    card = card,
                                                    isInWatchlist = false,
                                                    onCardClick = { onNavigateToSeries(card.slug) },
                                                    onWatchlistClick = {},
                                                    modifier = Modifier.weight(1f)
                                                )
                                            }
                                            repeat(3 - rowCards.size) {
                                                Spacer(modifier = Modifier.weight(1f))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Report Source Bottom Sheet
        if (uiState.isReportSheetOpen) {
            SourceReportBottomSheet(
                sourceLabel = activeSource?.label ?: "Server",
                isSubmitting = uiState.isSubmittingReport,
                onDismiss = { viewModel.closeReportSheet() },
                onSubmitReport = { reason, details ->
                    viewModel.submitSourceReport(reason, details)
                }
            )
        }
    }
}
