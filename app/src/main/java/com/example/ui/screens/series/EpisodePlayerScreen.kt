package com.example.ui.screens.series

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.data.model.*
import com.example.ui.components.EpisodePlayerView
import com.example.ui.components.SeriesCardItem
import com.example.ui.theme.*
import com.example.ui.viewmodel.PlayerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EpisodePlayerScreen(
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

    // Initial load
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

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // 1. Sticky Video Player
            if (activeSource != null && currentEp != null) {
                EpisodePlayerView(
                    rawVideoUrl = activeSource.url,
                    initialPositionSec = uiState.initialSeekPositionSec,
                    showSkipIntro = uiState.showSkipIntro,
                    introEndSec = uiState.introEndSec,
                    fallbackNotice = uiState.fallbackNotice,
                    autoplayCountdown = uiState.autoplayCountdown,
                    nextEpisodeTitle = uiState.episodes.getOrNull(
                        uiState.episodes.indexOfFirst { it.id == currentEp.id } + 1
                    )?.title,
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
                    onSkipIntroClick = {},
                    onPlayNextClick = {
                        viewModel.playNextEpisode()
                    },
                    onCancelAutoplayClick = {
                        viewModel.cancelAutoplayCountdown()
                    },
                    onBackClick = onBackClick
                )
            } else if (uiState.playerError != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                        .background(Color.Black)
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Error",
                            tint = Color.Red,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = uiState.playerError ?: "No sources available",
                            color = Color.White,
                            fontSize = 13.sp
                        )
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                        .background(Color.Black),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = QYellowPrimary)
                }
            }

            // 2. Scrollable Details, Server Chips, Actions, and Tabs
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                // Episode & Series Header Info
                item {
                    EpisodeHeaderSection(
                        series = uiState.series,
                        episode = currentEp,
                        isLiked = uiState.isEpisodeLiked,
                        likesCount = uiState.episodeLikesCount,
                        isInWatchlist = uiState.isInWatchlist,
                        onToggleLike = {
                            if (viewModel.isUserLoggedIn) {
                                viewModel.toggleEpisodeLike()
                            } else {
                                onOpenAuthDialog?.invoke()
                            }
                        },
                        onToggleWatchlist = {
                            if (viewModel.isUserLoggedIn) {
                                viewModel.toggleWatchlist()
                            } else {
                                onOpenAuthDialog?.invoke()
                            }
                        },
                        onShare = {
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, "Watch ${uiState.series?.title} - ${currentEp?.title} on Qylomi!")
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Share Episode"))
                        },
                        onOpenReportSheet = {
                            if (viewModel.isUserLoggedIn) {
                                viewModel.openReportSheet()
                            } else {
                                onOpenAuthDialog?.invoke()
                            }
                        }
                    )
                }

                // Servers / Sources Selection Bar
                if (uiState.sources.isNotEmpty()) {
                    item {
                        ServerChipsSection(
                            sources = uiState.sources,
                            activeSource = activeSource,
                            onSelectSource = { viewModel.selectSourceManually(it) }
                        )
                    }
                }

                // Tabs Header ([Episodes] | [Comments] | [More Like This])
                item {
                    TabRow(
                        selectedTabIndex = uiState.selectedTab,
                        containerColor = QSurface,
                        contentColor = Color.White,
                        indicator = { tabPositions ->
                            if (uiState.selectedTab < tabPositions.size) {
                                TabRowDefaults.SecondaryIndicator(
                                    modifier = Modifier.tabIndicatorOffset(tabPositions[uiState.selectedTab]),
                                    color = QYellowPrimary
                                )
                            }
                        }
                    ) {
                        Tab(
                            selected = uiState.selectedTab == 0,
                            onClick = { viewModel.setSelectedTab(0) },
                            text = { Text("Episodes", fontWeight = FontWeight.SemiBold) },
                            modifier = Modifier.testTag("player_tab_episodes")
                        )
                        Tab(
                            selected = uiState.selectedTab == 1,
                            onClick = { viewModel.setSelectedTab(1) },
                            text = {
                                Text(
                                    "Comments (${uiState.comments.size})",
                                    fontWeight = FontWeight.SemiBold
                                )
                            },
                            modifier = Modifier.testTag("player_tab_comments")
                        )
                        Tab(
                            selected = uiState.selectedTab == 2,
                            onClick = { viewModel.setSelectedTab(2) },
                            text = { Text("More Like This", fontWeight = FontWeight.SemiBold) },
                            modifier = Modifier.testTag("player_tab_related")
                        )
                    }
                }

                // Tab Content
                when (uiState.selectedTab) {
                    0 -> { // EPISODES TAB
                        if (uiState.seasons.size > 1) {
                            item {
                                SeasonSelectorRow(
                                    seasons = uiState.seasons,
                                    selectedSeasonNumber = uiState.selectedSeasonNumber,
                                    onSelectSeason = { viewModel.setSelectedSeasonNumber(it) }
                                )
                            }
                        }

                        val filteredEps = if (uiState.selectedSeasonNumber != null) {
                            uiState.episodes.filter { it.seasonNumber == uiState.selectedSeasonNumber }
                        } else uiState.episodes

                        items(filteredEps, key = { it.id }) { ep ->
                            EpisodeListItem(
                                episode = ep,
                                isCurrentlyPlaying = ep.id == currentEp?.id,
                                onSelectEpisode = { viewModel.selectEpisode(ep) }
                            )
                        }
                    }

                    1 -> { // COMMENTS TAB
                        item {
                            CommentInputBox(
                                commentInput = uiState.commentInput,
                                replyingTo = uiState.replyingToComment,
                                editing = uiState.editingComment,
                                onInputChange = { viewModel.setCommentInput(it) },
                                onPostClick = { viewModel.postComment() },
                                onCancelReplyOrEdit = {
                                    viewModel.setReplyingTo(null)
                                    viewModel.setEditingComment(null)
                                    viewModel.setCommentInput("")
                                }
                            )
                        }

                        if (uiState.isCommentsLoading) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(24.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(color = QYellowPrimary, modifier = Modifier.size(32.dp))
                                }
                            }
                        } else if (uiState.comments.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(32.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "No comments yet. Be the first to start a discussion!",
                                        color = QOnSurfaceVariant,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        } else {
                            val topLevelComments = uiState.comments.filter { it.parentId == null }
                            items(topLevelComments, key = { it.id }) { comment ->
                                CommentItemRow(
                                    comment = comment,
                                    allComments = uiState.comments,
                                    currentUserId = viewModel.currentUserId,
                                    onReply = { viewModel.setReplyingTo(it) },
                                    onEdit = { viewModel.setEditingComment(it) },
                                    onDelete = { viewModel.deleteComment(it.id) },
                                    onToggleLike = { viewModel.toggleCommentLike(it) }
                                )
                            }
                        }
                    }

                    2 -> { // RELATED SERIES TAB
                        if (uiState.relatedSeries.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(32.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("No related titles found", color = QOnSurfaceVariant)
                                }
                            }
                        } else {
                            item {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    uiState.relatedSeries.chunked(3).forEach { rowCards ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(bottom = 12.dp),
                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            rowCards.forEach { card ->
                                                Box(modifier = Modifier.weight(1f)) {
                                                    SeriesCardItem(
                                                        card = card,
                                                        isInWatchlist = false,
                                                        onCardClick = { onNavigateToSeries(card.slug) },
                                                        onWatchlistClick = {}
                                                    )
                                                }
                                            }
                                            // Fill remaining empty columns
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

@Composable
fun EpisodeHeaderSection(
    series: Series?,
    episode: Episode?,
    isLiked: Boolean,
    likesCount: Int,
    isInWatchlist: Boolean,
    onToggleLike: () -> Unit,
    onToggleWatchlist: () -> Unit,
    onShare: () -> Unit,
    onOpenReportSheet: () -> Unit
) {
    var isExpandedDescription by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        // Series Title & Episode Season info
        Text(
            text = series?.title ?: "",
            color = QOnSurfaceVariant,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = episode?.let { "S${it.seasonNumber}:E${it.episodeNumber} • ${it.title ?: "Episode ${it.episodeNumber}"}" } ?: "",
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(6.dp))

        // Air Date & Views Count
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (!episode?.airDate.isNullOrBlank()) {
                Text(
                    text = "Aired: ${episode?.airDate}",
                    color = QOnSurfaceVariant,
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.width(12.dp))
            }
            Text(
                text = "${episode?.viewsCount ?: 0} views",
                color = QOnSurfaceVariant,
                fontSize = 12.sp
            )
        }

        // Description
        if (!episode?.description.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = episode.description,
                color = QOnSurface,
                fontSize = 13.sp,
                maxLines = if (isExpandedDescription) Int.MAX_VALUE else 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.clickable { isExpandedDescription = !isExpandedDescription }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Action Buttons Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Like
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clickable { onToggleLike() }
                    .testTag("episode_like_button")
            ) {
                Icon(
                    imageVector = if (isLiked) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder,
                    contentDescription = "Like",
                    tint = if (isLiked) QYellowPrimary else Color.White,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (likesCount > 0) "$likesCount" else "Like",
                    color = if (isLiked) QYellowPrimary else Color.White,
                    fontSize = 11.sp
                )
            }

            // Watchlist
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.clickable { onToggleWatchlist() }
            ) {
                Icon(
                    imageVector = if (isInWatchlist) Icons.Default.Bookmark else Icons.Outlined.BookmarkBorder,
                    contentDescription = "My List",
                    tint = if (isInWatchlist) QYellowPrimary else Color.White,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "My List",
                    color = if (isInWatchlist) QYellowPrimary else Color.White,
                    fontSize = 11.sp
                )
            }

            // Share
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.clickable { onShare() }
            ) {
                Icon(
                    imageVector = Icons.Outlined.Share,
                    contentDescription = "Share",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = "Share", color = Color.White, fontSize = 11.sp)
            }

            // Report
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.clickable { onOpenReportSheet() }
            ) {
                Icon(
                    imageVector = Icons.Outlined.Flag,
                    contentDescription = "Report",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = "Report", color = Color.White, fontSize = 11.sp)
            }
        }
    }
}

@Composable
fun ServerChipsSection(
    sources: List<EpisodeSource>,
    activeSource: EpisodeSource?,
    onSelectSource: (EpisodeSource) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Text(
            text = "Streaming Servers",
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
        )

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(sources, key = { it.id }) { src ->
                val isActive = src.id == activeSource?.id
                val isHighFail = src.failCount >= 3

                Surface(
                    onClick = { onSelectSource(src) },
                    shape = RoundedCornerShape(20.dp),
                    color = if (isActive) QYellowPrimary else QSurface,
                    border = if (isHighFail && !isActive) BorderStroke(1.dp, Color.Red) else null,
                    modifier = Modifier.testTag("server_chip_${src.id}")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Dns,
                            contentDescription = null,
                            tint = if (isActive) QOnYellow else Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = src.label,
                            color = if (isActive) QOnYellow else Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        if (!src.quality.isNullOrBlank()) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = src.quality,
                                color = if (isActive) QOnYellow.copy(alpha = 0.8f) else QOnSurfaceVariant,
                                fontSize = 11.sp
                            )
                        }
                        if (!src.language.isNullOrBlank()) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "(${src.language})",
                                color = if (isActive) QOnYellow.copy(alpha = 0.8f) else QOnSurfaceVariant,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SeasonSelectorRow(
    seasons: List<Season>,
    selectedSeasonNumber: Int?,
    onSelectSeason: (Int) -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(seasons, key = { it.id }) { season ->
            val isSelected = season.seasonNumber == selectedSeasonNumber
            FilterChip(
                selected = isSelected,
                onClick = { onSelectSeason(season.seasonNumber) },
                label = { Text("Season ${season.seasonNumber}") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = QYellowPrimary,
                    selectedLabelColor = QOnYellow,
                    containerColor = QSurface,
                    labelColor = Color.White
                )
            )
        }
    }
}

@Composable
fun EpisodeListItem(
    episode: Episode,
    isCurrentlyPlaying: Boolean,
    onSelectEpisode: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelectEpisode() }
            .background(if (isCurrentlyPlaying) QSurface.copy(alpha = 0.8f) else Color.Transparent)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Thumbnail Container
        Box(
            modifier = Modifier
                .width(100.dp)
                .height(58.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(Color.DarkGray)
        ) {
            if (!episode.thumbnailUrl.isNullOrBlank()) {
                AsyncImage(
                    model = episode.thumbnailUrl,
                    contentDescription = episode.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
            if (isCurrentlyPlaying) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.6f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Playing",
                        tint = QYellowPrimary,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Episode Info
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Episode ${episode.episodeNumber}",
                color = if (isCurrentlyPlaying) QYellowPrimary else QOnSurfaceVariant,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = episode.title ?: "Episode ${episode.episodeNumber}",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (episode.durationSeconds != null) {
                Text(
                    text = "${episode.durationSeconds / 60}m",
                    color = QOnSurfaceVariant,
                    fontSize = 11.sp
                )
            }
        }
    }
}

@Composable
fun CommentInputBox(
    commentInput: String,
    replyingTo: SeriesComment?,
    editing: SeriesComment?,
    onInputChange: (String) -> Unit,
    onPostClick: () -> Unit,
    onCancelReplyOrEdit: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(QSurface)
            .padding(12.dp)
    ) {
        if (replyingTo != null || editing != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (editing != null) "Editing comment" else "Replying to comment",
                    color = QYellowPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                IconButton(
                    onClick = onCancelReplyOrEdit,
                    modifier = Modifier.size(20.dp)
                ) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Cancel", tint = Color.White)
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = commentInput,
                onValueChange = onInputChange,
                placeholder = { Text("Add a comment...", fontSize = 13.sp) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = QYellowPrimary,
                    unfocusedBorderColor = Color.Gray,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                modifier = Modifier
                    .weight(1f)
                    .testTag("comment_input_field")
            )

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(
                onClick = onPostClick,
                enabled = commentInput.trim().isNotEmpty(),
                modifier = Modifier
                    .background(
                        if (commentInput.trim().isNotEmpty()) QYellowPrimary else Color.DarkGray,
                        CircleShape
                    )
                    .testTag("comment_post_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Send",
                    tint = if (commentInput.trim().isNotEmpty()) QOnYellow else Color.Gray,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
fun CommentItemRow(
    comment: SeriesComment,
    allComments: List<SeriesComment>,
    currentUserId: String?,
    onReply: (SeriesComment) -> Unit,
    onEdit: (SeriesComment) -> Unit,
    onDelete: (SeriesComment) -> Unit,
    onToggleLike: (SeriesComment) -> Unit
) {
    val childComments = remember(allComments, comment.id) {
        allComments.filter { it.parentId == comment.id }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            // User Avatar Placeholder
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(QYellowPrimary.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = Icons.Default.Person, contentDescription = null, tint = QYellowPrimary)
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val userLabel = if (comment.userId.length >= 8) comment.userId.take(8) else comment.userId
                    Text(
                        text = userLabel,
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )

                    if (currentUserId == comment.userId) {
                        Row {
                            IconButton(
                                onClick = { onEdit(comment) },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit", tint = Color.Gray, modifier = Modifier.size(14.dp))
                            }
                            IconButton(
                                onClick = { onDelete(comment) },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red, modifier = Modifier.size(14.dp))
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))
                Text(text = comment.content, color = QOnSurface, fontSize = 13.sp)

                Spacer(modifier = Modifier.height(4.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Like (${comment.likesCount})",
                        color = QYellowPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable { onToggleLike(comment) }
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = "Reply",
                        color = QOnSurfaceVariant,
                        fontSize = 11.sp,
                        modifier = Modifier.clickable { onReply(comment) }
                    )
                }
            }
        }

        // Render Child Replies
        if (childComments.isNotEmpty()) {
            Column(modifier = Modifier.padding(start = 42.dp, top = 8.dp)) {
                childComments.forEach { reply ->
                    CommentItemRow(
                        comment = reply,
                        allComments = allComments,
                        currentUserId = currentUserId,
                        onReply = onReply,
                        onEdit = onEdit,
                        onDelete = onDelete,
                        onToggleLike = onToggleLike
                    )
                }
            }
        }
    }
}

@Composable
fun SourceReportBottomSheet(
    sourceLabel: String,
    isSubmitting: Boolean,
    onDismiss: () -> Unit,
    onSubmitReport: (reason: String, details: String?) -> Unit
) {
    var selectedReason by remember { mutableStateOf("Broken Video") }
    var detailsInput by remember { mutableStateOf("") }

    val reasons = listOf(
        "Broken Video / Black Screen",
        "Audio Desync / Muted",
        "Wrong Episode / Content",
        "Buffering / Very Slow",
        "Subtitle or Dubbing Error",
        "Other Issue"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Report Issue: $sourceLabel", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text("Select the reason for reporting this server:", color = QOnSurfaceVariant, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(8.dp))

                reasons.forEach { reason ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedReason = reason }
                            .padding(vertical = 4.dp)
                    ) {
                        RadioButton(
                            selected = selectedReason == reason,
                            onClick = { selectedReason = reason },
                            colors = RadioButtonDefaults.colors(selectedColor = QYellowPrimary)
                        )
                        Text(text = reason, color = Color.White, fontSize = 13.sp)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = detailsInput,
                    onValueChange = { detailsInput = it },
                    placeholder = { Text("Additional details (optional)...", fontSize = 12.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = QYellowPrimary,
                        unfocusedBorderColor = Color.Gray,
                        focusedTextColor = Color.White
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSubmitReport(selectedReason, detailsInput.ifBlank { null }) },
                enabled = !isSubmitting,
                colors = ButtonDefaults.buttonColors(containerColor = QYellowPrimary, contentColor = QOnYellow)
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(color = QOnYellow, modifier = Modifier.size(16.dp))
                } else {
                    Text("Submit Report", fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color.White)
            }
        },
        containerColor = QSurface
    )
}
