package com.example.ui.screens.series

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
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
import com.example.data.model.Season
import com.example.data.model.Series
import com.example.data.model.SeriesCard
import com.example.ui.components.SeriesCardItem
import com.example.ui.theme.*
import com.example.ui.viewmodel.SeriesViewModel

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

    val series by viewModel.currentSeries.collectAsState()
    val episodes by viewModel.seriesEpisodes.collectAsState()
    val seasons by viewModel.seriesSeasons.collectAsState()
    val relatedSeries by viewModel.relatedSeries.collectAsState()
    val isLoading by viewModel.isDetailsLoading.collectAsState()
    val detailsError by viewModel.detailsError.collectAsState()

    val watchlistIds by viewModel.watchlistIds.collectAsState()
    val likedIds by viewModel.likedSeriesIds.collectAsState()
    val likeCounts by viewModel.seriesLikeCounts.collectAsState()
    val userRatings by viewModel.userRatings.collectAsState()

    var showMatureConsentDialog by remember { mutableStateOf(false) }
    var showRatingSheet by remember { mutableStateOf(false) }
    var selectedSeasonNum by remember { mutableIntStateOf(1) }
    var isDescriptionExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(slug) {
        viewModel.loadSeriesBySlug(slug)
    }

    // Check 18+ mature consent
    LaunchedEffect(series) {
        val s = series
        if (s != null && s.isMature && !viewModel.hasMatureConsent()) {
            showMatureConsentDialog = true
        }
    }

    // Season determination
    val seasonNumbers = remember(episodes, seasons) {
        val fromEp = episodes.map { it.seasonNumber }.distinct()
        val fromSeas = seasons.map { it.seasonNumber }
        (fromEp + fromSeas).distinct().sorted().ifEmpty { listOf(1) }
    }

    val episodesForSeason = remember(episodes, selectedSeasonNum) {
        episodes.filter { it.seasonNumber == selectedSeasonNum }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(QBackground)
            .testTag("series_details_screen")
    ) {
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = QYellowPrimary)
            }
        } else if (detailsError != null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = QOnSurfaceVariant,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = detailsError ?: "Title not available",
                    color = QOnSurface,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onBack,
                    colors = ButtonDefaults.buttonColors(containerColor = QYellowPrimary, contentColor = QOnYellow)
                ) {
                    Text("Go Back")
                }
            }
        } else if (series != null) {
            val s = series!!
            val isInWatchlist = watchlistIds.contains(s.id)
            val isLiked = likedIds.contains(s.id)
            val likesCount = likeCounts[s.id] ?: s.likesCount
            val myRating = userRatings[s.id]

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                // 1. Header Banner & Floating Back Button
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(280.dp)
                    ) {
                        val hasBanner = !s.bannerUrl.isNullOrBlank()
                        if (hasBanner) {
                            AsyncImage(
                                model = s.bannerUrl,
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
                            AsyncImage(
                                model = s.posterUrl,
                                contentDescription = s.title,
                                contentScale = ContentScale.Fit,
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .height(200.dp)
                                    .aspectRatio(2f / 3f)
                                    .clip(RoundedCornerShape(8.dp))
                            )
                        }

                        // Gradient overlay
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        listOf(
                                            Color.Black.copy(alpha = 0.4f),
                                            Color.Transparent,
                                            QBackground
                                        )
                                    )
                                )
                        )

                        // Top Action Icons
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
                                    .background(Color.Black.copy(alpha = 0.5f))
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    tint = Color.White
                                )
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                IconButton(
                                    onClick = {
                                        val sendIntent: Intent = Intent().apply {
                                            action = Intent.ACTION_SEND
                                            putExtra(Intent.EXTRA_TEXT, "Watch ${s.title} on Qylomi Series!")
                                            type = "text/plain"
                                        }
                                        context.startActivity(Intent.createChooser(sendIntent, "Share Series"))
                                    },
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(Color.Black.copy(alpha = 0.5f))
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Share,
                                        contentDescription = "Share",
                                        tint = Color.White
                                    )
                                }
                            }
                        }
                    }
                }

                // 2. Series Title & Metadata
                item {
                    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                        Text(
                            text = s.title,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = QOnSurface,
                            lineHeight = 28.sp
                        )

                        if (!s.originalTitle.isNullOrBlank()) {
                            Text(
                                text = s.originalTitle,
                                fontSize = 14.sp,
                                color = QOnSurfaceVariant,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Metadata Badges (Rating, Year, Age rating, Status, Category)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // Rating Badge
                            if ((s.rating ?: 0.0) > 0.0) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(QYellowPrimary.copy(alpha = 0.15f))
                                        .padding(horizontal = 6.dp, vertical = 3.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Star,
                                        contentDescription = null,
                                        tint = QYellowPrimary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = String.format("%.1f", s.rating),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = QYellowPrimary
                                    )
                                }
                            }

                            // User Community Rating
                            if ((s.avgUserRating ?: 0.0) > 0.0) {
                                Text(
                                    text = "★ ${String.format("%.1f", s.avgUserRating)} (${s.ratingsCount})",
                                    fontSize = 12.sp,
                                    color = QOnSurfaceVariant
                                )
                            }

                            if (s.releaseYear != null) {
                                Text(text = "${s.releaseYear}", fontSize = 12.sp, color = QOnSurfaceVariant)
                            }

                            if (s.ageRating != null) {
                                Box(
                                    modifier = Modifier
                                        .border(0.5.dp, QCardBorder, RoundedCornerShape(4.dp))
                                        .padding(horizontal = 4.dp, vertical = 2.dp)
                                ) {
                                    Text(text = s.ageRating, fontSize = 11.sp, color = QOnSurfaceVariant)
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(QSurfaceVariant)
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = s.status.replaceFirstChar { it.uppercase() },
                                    fontSize = 11.sp,
                                    color = QOnSurfaceVariant,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Languages & Subtitles
                        if (s.languages.isNotEmpty() || s.subtitleLanguages.isNotEmpty()) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                if (s.languages.isNotEmpty()) {
                                    Text(
                                        text = "Audio: ${s.languages.joinToString(", ")}",
                                        fontSize = 12.sp,
                                        color = QOnSurfaceVariant
                                    )
                                }
                                if (s.subtitleLanguages.isNotEmpty()) {
                                    Text(
                                        text = "• Subs: ${s.subtitleLanguages.joinToString(", ")}",
                                        fontSize = 12.sp,
                                        color = QOnSurfaceVariant
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        // Genres Chips
                        if (s.genres.isNotEmpty()) {
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                items(s.genres) { genre ->
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(QSurfaceVariant)
                                            .padding(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Text(text = genre, fontSize = 11.sp, color = QOnSurface)
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                        }

                        // Action Buttons: Play (S1E1), + My List, Like, Rate, Trailer
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = {
                                    val firstEp = episodes.firstOrNull()
                                    if (firstEp != null) {
                                        onPlayEpisode(s.id, firstEp.id)
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = QYellowPrimary,
                                    contentColor = QOnYellow
                                ),
                                shape = RoundedCornerShape(24.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(46.dp)
                                    .testTag("details_play_button")
                            ) {
                                Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Play", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            }

                            if (!s.trailerUrl.isNullOrBlank()) {
                                OutlinedButton(
                                    onClick = {
                                        // Open trailer
                                        val intent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse(s.trailerUrl))
                                        context.startActivity(intent)
                                    },
                                    shape = RoundedCornerShape(24.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = QOnSurface),
                                    modifier = Modifier.height(46.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.Movie, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Trailer", fontSize = 13.sp)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Secondary actions: Watchlist, Like, Rate
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Watchlist
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { viewModel.toggleWatchlist(s.id, onAuthRequired) }
                                    .padding(8.dp)
                            ) {
                                Icon(
                                    imageVector = if (isInWatchlist) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                    contentDescription = "Watchlist",
                                    tint = if (isInWatchlist) QYellowPrimary else QOnSurface
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = if (isInWatchlist) "In List" else "My List",
                                    fontSize = 11.sp,
                                    color = if (isInWatchlist) QYellowPrimary else QOnSurfaceVariant
                                )
                            }

                            // Like
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { viewModel.toggleLike(s.id, onAuthRequired) }
                                    .padding(8.dp)
                            ) {
                                Icon(
                                    imageVector = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                    contentDescription = "Like",
                                    tint = if (isLiked) QLikeRed else QOnSurface
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = if (likesCount > 0) "$likesCount" else "Like",
                                    fontSize = 11.sp,
                                    color = if (isLiked) QLikeRed else QOnSurfaceVariant
                                )
                            }

                            // Rate
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { showRatingSheet = true }
                                    .padding(8.dp)
                            ) {
                                Icon(
                                    imageVector = if (myRating != null) Icons.Default.Star else Icons.Default.StarBorder,
                                    contentDescription = "Rate",
                                    tint = if (myRating != null) QYellowPrimary else QOnSurface
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = if (myRating != null) "Rated $myRating" else "Rate",
                                    fontSize = 11.sp,
                                    color = if (myRating != null) QYellowPrimary else QOnSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Description (Expandable)
                        if (!s.description.isNullOrBlank()) {
                            Text(
                                text = s.description,
                                fontSize = 13.sp,
                                color = QOnSurface.copy(alpha = 0.9f),
                                lineHeight = 19.sp,
                                maxLines = if (isDescriptionExpanded) Int.MAX_VALUE else 3,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier
                                    .animateContentSize()
                                    .clickable { isDescriptionExpanded = !isDescriptionExpanded }
                            )
                            if (s.description.length > 150) {
                                Text(
                                    text = if (isDescriptionExpanded) "Show less" else "Read more",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = QYellowDark,
                                    modifier = Modifier
                                        .clickable { isDescriptionExpanded = !isDescriptionExpanded }
                                        .padding(vertical = 4.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        if (!s.studio.isNullOrBlank()) {
                            Text(
                                text = "Studio: ${s.studio}",
                                fontSize = 12.sp,
                                color = QOnSurfaceVariant,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }

                // 3. Seasons & Episodes Section
                item {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Episodes",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = QOnSurface,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                        )

                        // Season Picker Tabs if more than 1 season
                        if (seasonNumbers.size > 1) {
                            ScrollableTabRow(
                                selectedTabIndex = seasonNumbers.indexOf(selectedSeasonNum).coerceAtLeast(0),
                                containerColor = QSurface,
                                edgePadding = 16.dp,
                                divider = {}
                            ) {
                                seasonNumbers.forEach { seasonNum ->
                                    val isSelected = selectedSeasonNum == seasonNum
                                    val customTitle = seasons.firstOrNull { it.seasonNumber == seasonNum }?.title
                                    Tab(
                                        selected = isSelected,
                                        onClick = { selectedSeasonNum = seasonNum },
                                        text = {
                                            Text(
                                                text = customTitle ?: "Season $seasonNum",
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                color = if (isSelected) QYellowPrimary else QOnSurfaceVariant
                                            )
                                        }
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }

                if (episodesForSeason.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No episodes released for this season yet.",
                                fontSize = 13.sp,
                                color = QOnSurfaceVariant
                            )
                        }
                    }
                } else {
                    items(episodesForSeason, key = { it.id }) { ep ->
                        EpisodeCardItem(
                            episode = ep,
                            onClick = { onPlayEpisode(s.id, ep.id) },
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                        )
                    }
                }

                // 4. More Like This Row (get_related_series)
                if (relatedSeries.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "More Like This",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = QOnSurface,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )

                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(relatedSeries, key = { it.id }) { card ->
                                SeriesCardItem(
                                    card = card,
                                    isInWatchlist = watchlistIds.contains(card.id),
                                    onCardClick = { onSeriesClick(card.slug) },
                                    onWatchlistClick = { viewModel.toggleWatchlist(card.id, onAuthRequired) }
                                )
                            }
                        }
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
            title = { Text("Age-Restricted Content (18+)") },
            text = {
                Text("This series contains mature themes and content rated 18+. Are you 18 years of age or older and do you consent to view this material?")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.grantMatureConsent()
                        showMatureConsentDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F))
                ) {
                    Text("I am 18+, Proceed")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = {
                    showMatureConsentDialog = false
                    onBack()
                }) {
                    Text("Go Back")
                }
            }
        )
    }

    // Rating Bottom Sheet (1 - 10)
    if (showRatingSheet && series != null) {
        ModalBottomSheet(
            onDismissRequest = { showRatingSheet = false },
            containerColor = QSurface
        ) {
            val s = series!!
            var selectedRate by remember { mutableIntStateOf(userRatings[s.id] ?: 8) }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Rate \"${s.title}\"",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = QOnSurface
                )
                Spacer(modifier = Modifier.height(16.dp))

                // Big Score Display
                Text(
                    text = "$selectedRate / 10",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Black,
                    color = QYellowPrimary
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Rating Slider
                Slider(
                    value = selectedRate.toFloat(),
                    onValueChange = { selectedRate = it.toInt() },
                    valueRange = 1f..10f,
                    steps = 8,
                    colors = SliderDefaults.colors(
                        thumbColor = QYellowPrimary,
                        activeTrackColor = QYellowPrimary,
                        inactiveTrackColor = QSurfaceVariant
                    ),
                    modifier = Modifier.fillMaxWidth(0.9f)
                )

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = {
                        viewModel.rateSeries(s.id, selectedRate, onAuthRequired) { _, _ ->
                            showRatingSheet = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = QYellowPrimary,
                        contentColor = QOnYellow
                    ),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth(0.9f)
                ) {
                    Text("Submit Rating", fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun EpisodeCardItem(
    episode: Episode,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = QSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, QCardBorder),
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("episode_card_${episode.id}")
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Episode Thumbnail (16:9)
            Box(
                modifier = Modifier
                    .width(110.dp)
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(6.dp))
                    .background(QSurfaceVariant)
            ) {
                if (!episode.thumbnailUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = episode.thumbnailUrl,
                        contentDescription = episode.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.6f))
                        .align(Alignment.Center)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = QYellowPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "E${episode.episodeNumber}: ${episode.title ?: "Episode ${episode.episodeNumber}"}",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = QOnSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (episode.durationSeconds != null && episode.durationSeconds > 0) {
                    val mins = episode.durationSeconds / 60
                    Text(
                        text = "${mins} min",
                        fontSize = 11.sp,
                        color = QOnSurfaceVariant
                    )
                }

                if (!episode.description.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = episode.description,
                        fontSize = 11.sp,
                        color = QOnSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 14.sp
                    )
                }
            }
        }
    }
}
