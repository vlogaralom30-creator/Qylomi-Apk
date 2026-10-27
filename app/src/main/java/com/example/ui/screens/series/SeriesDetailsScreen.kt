package com.example.ui.screens.series

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.Episode
import com.example.data.model.Series
import com.example.ui.components.SeriesCardItem
import com.example.ui.components.series.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.SeriesViewModel

enum class SeriesDetailsTab(val label: String) {
    EPISODES("Episodes"),
    COMMENTS("Comments"),
    RELATED("Related"),
    ABOUT("About")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SeriesDetailsScreen(
    slug: String,
    viewModel: SeriesViewModel,
    onBack: () -> Unit,
    onPlayEpisode: (String, String) -> Unit, // seriesId, episodeId
    onSeriesClick: (String) -> Unit, // navigate to related slug
    onAuthRequired: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }
    val context = LocalContext.current
    val listState = rememberLazyListState()

    val series by viewModel.currentSeries.collectAsState()
    val episodes by viewModel.seriesEpisodes.collectAsState()
    val seasons by viewModel.seriesSeasons.collectAsState()
    val relatedSeries by viewModel.relatedSeries.collectAsState()
    val continueWatching by viewModel.continueWatching.collectAsState()
    val isLoading by viewModel.isDetailsLoading.collectAsState()
    val detailsError by viewModel.detailsError.collectAsState()

    val watchlistIds by viewModel.watchlistIds.collectAsState()
    val likedIds by viewModel.likedSeriesIds.collectAsState()
    val likeCounts by viewModel.seriesLikeCounts.collectAsState()
    val userRatings by viewModel.userRatings.collectAsState()

    // Comments State
    val comments by viewModel.seriesComments.collectAsState()
    val isCommentsLoading by viewModel.isCommentsLoading.collectAsState()
    val commentInput by viewModel.commentInput.collectAsState()
    val replyingToComment by viewModel.replyingToComment.collectAsState()
    val editingComment by viewModel.editingComment.collectAsState()

    var showMatureConsentDialog by remember { mutableStateOf(false) }
    var showRatingSheet by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableStateOf(SeriesDetailsTab.EPISODES) }
    var isDescriptionExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(slug) {
        viewModel.loadSeriesBySlug(slug)
    }

    // 18+ Mature Consent Check
    LaunchedEffect(series) {
        val s = series
        if (s != null && s.isMature && !viewModel.hasMatureConsent()) {
            showMatureConsentDialog = true
        }
    }

    // Top Bar Collapsing Fade Alpha
    val topBarAlpha by remember {
        derivedStateOf {
            if (listState.firstVisibleItemIndex > 0) 1f
            else (listState.firstVisibleItemScrollOffset / 300f).coerceIn(0f, 1f)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(QBackground)
            .testTag("series_details_screen")
    ) {
        if (isLoading) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Top placeholder
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Box(modifier = Modifier.size(36.dp).clip(CircleShape).shimmerEffect())
                    Box(modifier = Modifier.size(36.dp).clip(CircleShape).shimmerEffect())
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(SeriesTokens.shapeCard)
                        .shimmerEffect()
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .width(110.dp)
                            .aspectRatio(2f / 3f)
                            .clip(SeriesTokens.shapeCard)
                            .shimmerEffect()
                    )
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(modifier = Modifier.fillMaxWidth(0.9f).height(24.dp).clip(RoundedCornerShape(4.dp)).shimmerEffect())
                        Box(modifier = Modifier.fillMaxWidth(0.5f).height(16.dp).clip(RoundedCornerShape(4.dp)).shimmerEffect())
                        Box(modifier = Modifier.fillMaxWidth(0.7f).height(14.dp).clip(RoundedCornerShape(4.dp)).shimmerEffect())
                    }
                }
                Box(modifier = Modifier.fillMaxWidth().height(48.dp).clip(SeriesTokens.shapePill).shimmerEffect())
                Spacer(modifier = Modifier.height(16.dp))
                SkeletonRow()
            }
        } else if (detailsError != null) {
            SeriesErrorState(
                message = detailsError ?: "Title not available",
                onRetry = { viewModel.loadSeriesBySlug(slug) },
                modifier = Modifier.align(Alignment.Center)
            )
        } else if (series != null) {
            val s = series!!
            val isInWatchlist = watchlistIds.contains(s.id)
            val isLiked = likedIds.contains(s.id)
            val likesCount = likeCounts[s.id] ?: s.likesCount
            val myRating = userRatings[s.id]

            // Calculate continue watching or first episode logic
            val cwItem = remember(continueWatching, s.id) {
                continueWatching.firstOrNull { it.series?.id == s.id || it.episode?.seriesId == s.id }
            }
            val targetEpisode: Episode? = remember(cwItem, episodes) {
                if (cwItem?.episode != null) {
                    episodes.firstOrNull { it.id == cwItem.episode?.id } ?: episodes.firstOrNull()
                } else {
                    episodes.firstOrNull()
                }
            }
            val playButtonLabel = remember(cwItem, targetEpisode) {
                if (cwItem != null && targetEpisode != null && cwItem.durationSec > 0 && cwItem.positionSec > 0) {
                    val remainingMins = ((cwItem.durationSec - cwItem.positionSec) / 60).coerceAtLeast(1)
                    "Continue S${targetEpisode.seasonNumber}:E${targetEpisode.episodeNumber} - $remainingMins min left"
                } else if (targetEpisode != null) {
                    "Play S${targetEpisode.seasonNumber}:E${targetEpisode.episodeNumber}"
                } else {
                    "Play"
                }
            }

            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 90.dp)
            ) {
                // 1. Immersive Banner with Yellow Ambient Glow
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(260.dp)
                    ) {
                        val banner = s.bannerUrl?.takeIf { it.isNotBlank() }
                        if (banner != null) {
                            AsyncImage(
                                model = banner,
                                contentDescription = s.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            // Blurred poster background
                            AsyncImage(
                                model = s.posterUrl,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .blur(25.dp)
                            )
                        }

                        // Gradient Scrim blend into QBackground
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            Color.Black.copy(alpha = 0.5f),
                                            Color.Transparent,
                                            QBackground.copy(alpha = 0.8f),
                                            QBackground
                                        )
                                    )
                                )
                        )
                    }
                }

                // 2. Floating Poster Card + Title + Metadata Block
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .offset(y = (-70).dp)
                            .padding(horizontal = 16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.Bottom
                        ) {
                            // Floating 2:3 Poster
                            PosterImage(
                                posterUrl = s.posterUrl,
                                bannerUrl = s.bannerUrl,
                                title = s.title,
                                modifier = Modifier
                                    .width(120.dp)
                                    .aspectRatio(2f / 3f)
                                    .shadow(elevation = 10.dp, shape = SeriesTokens.shapeCard, ambientColor = SeriesTokens.GoldGlow, spotColor = SeriesTokens.GoldGlow)
                            )

                            Spacer(modifier = Modifier.width(16.dp))

                            // Title & Quick Meta
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = s.title,
                                    style = SeriesTypography.headline,
                                    color = QOnSurface,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )

                                if (!s.originalTitle.isNullOrBlank()) {
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = s.originalTitle,
                                        style = SeriesTypography.label,
                                        color = QOnSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                // RatingRing + Year + Age Rating + Status
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    val score = s.avgUserRating?.takeIf { it > 0 } ?: s.rating ?: 0.0
                                    if (score > 0.0) {
                                        RatingRing(rating = score, size = 36.dp, strokeWidth = 3.dp)
                                    }

                                    Column {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            if (s.releaseYear != null) {
                                                Text(text = "${s.releaseYear}", style = SeriesTypography.micro, color = QOnSurfaceVariant)
                                            }
                                            if (!s.ageRating.isNullOrBlank()) {
                                                Text(text = "• ${s.ageRating}", style = SeriesTypography.micro, color = QOnSurfaceVariant)
                                            }
                                        }
                                        Text(
                                            text = s.status.replaceFirstChar { it.uppercase() },
                                            style = SeriesTypography.micro,
                                            fontWeight = FontWeight.Bold,
                                            color = if (s.status == "ongoing") QYellowDark else QOnSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        // 3. Primary Actions: Big Yellow Pill (Continue/Play) + My List + Heart Like
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            YellowPillButton(
                                text = playButtonLabel,
                                icon = Icons.Default.PlayArrow,
                                onClick = {
                                    if (targetEpisode != null) {
                                        onPlayEpisode(s.id, targetEpisode.id)
                                    } else {
                                        val first = episodes.firstOrNull()
                                        if (first != null) onPlayEpisode(s.id, first.id)
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("details_play_button")
                            )

                            // Animated Bookmark / My List
                            AnimatedBookmark(
                                isInList = isInWatchlist,
                                onClick = { viewModel.toggleWatchlist(s.id, onAuthRequired) },
                                modifier = Modifier.testTag("details_watchlist_button")
                            )

                            // HeartBurst Like
                            HeartBurstButton(
                                isLiked = isLiked,
                                likesCount = likesCount,
                                onClick = { viewModel.toggleLike(s.id, onAuthRequired) },
                                modifier = Modifier.testTag("details_like_button")
                            )

                            // Rate Button
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .clip(SeriesTokens.shapeChip)
                                    .clickable { showRatingSheet = true }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                                    .testTag("details_rate_button")
                            ) {
                                Icon(
                                    imageVector = if (myRating != null) Icons.Default.Star else Icons.Outlined.Star,
                                    contentDescription = "Rate",
                                    tint = if (myRating != null) QYellowDark else QOnSurfaceVariant,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = if (myRating != null) "$myRating★" else "Rate",
                                    style = SeriesTypography.micro,
                                    color = if (myRating != null) QYellowDark else QOnSurfaceVariant
                                )
                            }
                        }

                        // Trailer button if available
                        if (!s.trailerUrl.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            GhostPillButton(
                                text = "Watch Official Trailer",
                                icon = Icons.Outlined.Movie,
                                onClick = {
                                    val intent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse(s.trailerUrl))
                                    context.startActivity(intent)
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // 4. Genres & Language Badges
                        if (s.genres.isNotEmpty()) {
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                items(s.genres) { genre ->
                                    MetaChip(text = genre)
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        if (s.languages.isNotEmpty() || s.subtitleLanguages.isNotEmpty()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (s.languages.isNotEmpty()) {
                                    LanguageBadge(label = "Audio", value = s.languages.joinToString(", "))
                                }
                                if (s.subtitleLanguages.isNotEmpty()) {
                                    LanguageBadge(label = "Subs", value = s.subtitleLanguages.joinToString(", "))
                                }
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                        }

                        // 5. Synopsis (3 lines, animated expand)
                        if (!s.description.isNullOrBlank()) {
                            Text(
                                text = s.description,
                                style = SeriesTypography.body,
                                color = QOnSurface.copy(alpha = 0.9f),
                                maxLines = if (isDescriptionExpanded) Int.MAX_VALUE else 3,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.clickable { isDescriptionExpanded = !isDescriptionExpanded }
                            )
                            if (s.description.length > 140) {
                                Text(
                                    text = if (isDescriptionExpanded) "Show less" else "Read more",
                                    style = SeriesTypography.label,
                                    fontWeight = FontWeight.Bold,
                                    color = QYellowDark,
                                    modifier = Modifier
                                        .clickable { isDescriptionExpanded = !isDescriptionExpanded }
                                        .padding(vertical = 4.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                        }

                        // 6. Two-Column Info Card
                        GlassPanel(
                            modifier = Modifier.fillMaxWidth(),
                            backgroundColor = QSurfaceVariant.copy(alpha = 0.7f),
                            borderColor = QCardBorder
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(modifier = Modifier.fillMaxWidth()) {
                                    InfoCardField(
                                        label = "Studio",
                                        value = s.studio ?: "Independent",
                                        modifier = Modifier.weight(1f)
                                    )
                                    InfoCardField(
                                        label = "Release Year",
                                        value = s.releaseYear?.toString() ?: "N/A",
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                                Row(modifier = Modifier.fillMaxWidth()) {
                                    InfoCardField(
                                        label = "Age Rating",
                                        value = s.ageRating ?: (if (s.isMature) "18+" else "PG"),
                                        modifier = Modifier.weight(1f)
                                    )
                                    InfoCardField(
                                        label = "Total Episodes",
                                        value = "${episodes.size}${if (s.totalEpisodes != null && s.totalEpisodes > episodes.size) " / ${s.totalEpisodes}" else ""}",
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }
                }

                // 7. Sticky Tabs: Episodes | Comments | Related | About
                item {
                    TabRow(
                        selectedTabIndex = selectedTab.ordinal,
                        containerColor = QSurface,
                        contentColor = QOnSurface,
                        indicator = { tabPositions ->
                            if (selectedTab.ordinal < tabPositions.size) {
                                TabRowDefaults.SecondaryIndicator(
                                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab.ordinal]),
                                    color = QYellowPrimary
                                )
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .offset(y = (-50).dp)
                    ) {
                        SeriesDetailsTab.values().forEach { tab ->
                            val isSelected = selectedTab == tab
                            Tab(
                                selected = isSelected,
                                onClick = { selectedTab = tab },
                                text = {
                                    Text(
                                        text = when (tab) {
                                            SeriesDetailsTab.COMMENTS -> "Comments (${comments.size})"
                                            else -> tab.label
                                        },
                                        style = SeriesTypography.label,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) QYellowDark else QOnSurfaceVariant
                                    )
                                },
                                modifier = Modifier.testTag("details_tab_${tab.name.lowercase()}")
                            )
                        }
                    }
                }

                // Tab Content (Offset by -50dp to maintain flow)
                item {
                    Box(modifier = Modifier.offset(y = (-40).dp)) {
                        when (selectedTab) {
                            SeriesDetailsTab.EPISODES -> {
                                EpisodeSelector(
                                    episodes = episodes,
                                    seasons = seasons,
                                    series = s,
                                    onEpisodeClick = { ep -> onPlayEpisode(s.id, ep.id) },
                                    initialViewModeIsGrid = true
                                )
                            }

                            SeriesDetailsTab.COMMENTS -> {
                                SeriesCommentsSection(
                                    comments = comments,
                                    isLoading = isCommentsLoading,
                                    currentUserId = viewModel.currentUserId,
                                    commentInput = commentInput,
                                    replyingTo = replyingToComment,
                                    editingComment = editingComment,
                                    seriesCreatorId = s.createdBy,
                                    onInputChange = { viewModel.setCommentInput(it) },
                                    onPostComment = { viewModel.postSeriesComment(onAuthRequired) },
                                    onReply = { viewModel.setReplyingTo(it) },
                                    onEdit = { viewModel.setEditingComment(it) },
                                    onDelete = { viewModel.deleteSeriesComment(it.id) },
                                    onToggleLike = { viewModel.toggleCommentLike(it, onAuthRequired) },
                                    onCancelReplyOrEdit = { viewModel.cancelReplyOrEdit() }
                                )
                            }

                            SeriesDetailsTab.RELATED -> {
                                if (relatedSeries.isEmpty()) {
                                    SeriesEmptyState(
                                        icon = Icons.Default.Info,
                                        title = "No related titles found",
                                        description = "Explore more genres and categories from the home catalog!"
                                    )
                                } else {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 14.dp),
                                        verticalArrangement = Arrangement.spacedBy(14.dp)
                                    ) {
                                        relatedSeries.chunked(3).forEach { rowCards ->
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                                            ) {
                                                rowCards.forEach { card ->
                                                    SeriesCardItem(
                                                        card = card,
                                                        isInWatchlist = watchlistIds.contains(card.id),
                                                        onCardClick = { onSeriesClick(card.slug) },
                                                        onWatchlistClick = { viewModel.toggleWatchlist(card.id, onAuthRequired) },
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

                            SeriesDetailsTab.ABOUT -> {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Text("Overview", style = SeriesTypography.headline, color = QOnSurface)
                                    Text(
                                        text = s.description ?: "No full synopsis provided.",
                                        style = SeriesTypography.body,
                                        color = QOnSurfaceVariant
                                    )

                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("Technical Details", style = SeriesTypography.title, color = QOnSurface)

                                    if (s.languages.isNotEmpty()) {
                                        Text("Available Audio: ${s.languages.joinToString(", ")}", style = SeriesTypography.label, color = QOnSurfaceVariant)
                                    }
                                    if (s.subtitleLanguages.isNotEmpty()) {
                                        Text("Available Subtitles: ${s.subtitleLanguages.joinToString(", ")}", style = SeriesTypography.label, color = QOnSurfaceVariant)
                                    }
                                    Text("Category: ${s.category?.name ?: "Anime"}", style = SeriesTypography.label, color = QOnSurfaceVariant)
                                    Text("Status: ${s.status.replaceFirstChar { it.uppercase() }}", style = SeriesTypography.label, color = QOnSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            }

            // Top Collapsing Action Bar (Back & Share & Title on scroll)
            Surface(
                color = QBackground.copy(alpha = topBarAlpha),
                shadowElevation = if (topBarAlpha > 0.8f) 3.dp else 0.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(
                                if (topBarAlpha > 0.5f) Color.Transparent
                                else Color.Black.copy(alpha = 0.5f)
                            )
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = if (topBarAlpha > 0.5f) QOnSurface else Color.White
                        )
                    }

                    // Collapsed Title on scroll
                    AnimatedVisibility(
                        visible = topBarAlpha > 0.7f,
                        enter = fadeIn() + slideInVertically(),
                        exit = fadeOut()
                    ) {
                        Text(
                            text = s.title,
                            style = SeriesTypography.title,
                            color = QOnSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(horizontal = 12.dp)
                        )
                    }

                    IconButton(
                        onClick = {
                            val sendIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, "Watch ${s.title} on Qylomi Series!")
                                type = "text/plain"
                            }
                            context.startActivity(Intent.createChooser(sendIntent, "Share Series"))
                        },
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(
                                if (topBarAlpha > 0.5f) Color.Transparent
                                else Color.Black.copy(alpha = 0.5f)
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Share,
                            contentDescription = "Share",
                            tint = if (topBarAlpha > 0.5f) QOnSurface else Color.White
                        )
                    }
                }
            }
        }
    }

    // 18+ Mature Consent Dialog
    if (showMatureConsentDialog) {
        AlertDialog(
            onDismissRequest = {
                showMatureConsentDialog = false
                onBack()
            },
            shape = SeriesTokens.shapeCard,
            containerColor = QSurface,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = Color(0xFFD32F2F),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text("18+", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(4.dp))
                    }
                    Text("Mature Content Warning", style = SeriesTypography.title, color = QOnSurface)
                }
            },
            text = {
                Text(
                    text = "This series contains mature content intended for adult audiences. Please confirm that you are at least 18 years old.",
                    style = SeriesTypography.body,
                    color = QOnSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.grantMatureConsent()
                        showMatureConsentDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = QYellowPrimary,
                        contentColor = QOnYellow
                    ),
                    shape = SeriesTokens.shapePill
                ) {
                    Text("I am 18+, Proceed", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = {
                        showMatureConsentDialog = false
                        onBack()
                    },
                    shape = SeriesTokens.shapePill
                ) {
                    Text("Cancel", color = QOnSurface)
                }
            }
        )
    }

    // Rating Bottom Sheet (10 stars)
    if (showRatingSheet && series != null) {
        val s = series!!
        SeriesRatingBottomSheet(
            seriesTitle = s.title,
            initialRating = userRatings[s.id],
            avgRating = s.avgUserRating ?: s.rating,
            ratingsCount = s.ratingsCount,
            onDismiss = { showRatingSheet = false },
            onSubmitRating = { score ->
                viewModel.rateSeries(s.id, score, onAuthRequired) { _, _ -> }
            }
        )
    }
}

@Composable
private fun InfoCardField(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = label.uppercase(),
            style = SeriesTypography.micro.copy(fontSize = 9.sp),
            fontWeight = FontWeight.Bold,
            color = QYellowDark
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            style = SeriesTypography.label,
            fontWeight = FontWeight.SemiBold,
            color = QOnSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
