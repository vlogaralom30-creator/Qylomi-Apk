package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Surface
import com.example.data.model.Post
import com.example.data.model.SeriesCard
import com.example.data.model.Shelf
import com.example.data.model.ShelfWithItems
import com.example.data.model.UserProfile
import com.example.ui.components.CreatorRankChip
import com.example.ui.components.PointsBadge
import com.example.ui.components.PostCard
import com.example.ui.components.SeriesCardItem
import com.example.ui.components.StoryAvatar
import com.example.ui.components.UserAvatar
import com.example.ui.theme.QBackground
import com.example.ui.theme.QCardBorder
import com.example.ui.theme.QOnSurface
import com.example.ui.theme.QOnSurfaceVariant
import com.example.ui.theme.QOnYellow
import com.example.ui.theme.QSurface
import com.example.ui.theme.QSurfaceVariant
import com.example.ui.theme.QYellowDark
import com.example.ui.theme.QYellowLight
import com.example.ui.theme.QYellowPrimary

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedScreen(
    posts: List<Post>,
    isLoading: Boolean,
    isRefreshing: Boolean,
    currentUser: UserProfile?,
    allCreators: List<UserProfile>,
    selectedCategory: String,
    onCategorySelected: (String) -> Unit,
    onLikeClick: (Post) -> Unit,
    onCommentClick: (Post) -> Unit,
    onBookmarkClick: (Post) -> Unit,
    onAuthorClick: (UserProfile) -> Unit,
    onRefresh: () -> Unit,
    onCreatePostClick: () -> Unit,
    onEditPostClick: (Post) -> Unit = {},
    onDeletePostClick: (Post) -> Unit = {},
    onGroupsClick: () -> Unit = {},
    onOpenReels: ((Post) -> Unit)? = null,
    onOpenMenu: ((Post) -> Unit)? = null,
    onLikersClick: ((Post) -> Unit)? = null,
    onRelayClick: ((Post) -> Unit)? = null,
    feedError: String? = null,
    onRetryFeed: () -> Unit = onRefresh,
    seriesShelves: List<ShelfWithItems> = emptyList(),
    watchlistIds: Set<String> = emptySet(),
    onSeriesClick: ((String) -> Unit)? = null,
    onWatchlistToggle: ((SeriesCard) -> Unit)? = null,
    onSeeAllSeriesClick: (() -> Unit)? = null
) {
    var storyPreviewProfile by remember { mutableStateOf<UserProfile?>(null) }

    val categories = listOf("All", "Post", "Idea", "Resource", "Question")

    val filteredPosts = remember(posts, selectedCategory) {
        if (selectedCategory == "All") {
            posts
        } else {
            posts.filter { it.type.equals(selectedCategory, ignoreCase = true) }
        }
    }

    val effectiveShelves = remember(seriesShelves) {
        if (seriesShelves.any { it.items.isNotEmpty() }) {
            seriesShelves.filter { it.items.isNotEmpty() }
        } else {
            getFallbackSeriesShelves()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(QBackground)
    ) {
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = onRefresh,
            modifier = Modifier.fillMaxSize()
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("feed_list"),
                contentPadding = PaddingValues(bottom = 90.dp)
            ) {
            // Stories / Highlights Header
            item {
                Card(
                    shape = RoundedCornerShape(0.dp),
                    colors = CardDefaults.cardColors(containerColor = QSurface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(vertical = 12.dp)) {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Current user story
                            currentUser?.let { user ->
                                item {
                                    StoryAvatar(
                                        profile = user,
                                        isCurrentUser = true,
                                        onClick = { storyPreviewProfile = user }
                                    )
                                }
                            }

                            // Other creators
                            items(allCreators.filter { it.id != currentUser?.id }) { creator ->
                                StoryAvatar(
                                    profile = creator,
                                    isCurrentUser = false,
                                    onClick = { storyPreviewProfile = creator }
                                )
                            }
                        }
                    }
                }
            }

            // Categories & Groups Filter Bar
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(QBackground)
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Groups / Communities Chip Button
                    FilterChip(
                        selected = false,
                        onClick = onGroupsClick,
                        label = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = androidx.compose.material.icons.Icons.Default.Groups,
                                    contentDescription = "Communities",
                                    tint = QYellowDark,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Groups", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = QYellowLight,
                            labelColor = QYellowDark
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = false,
                            borderColor = QYellowDark.copy(alpha = 0.5f)
                        ),
                        shape = RoundedCornerShape(18.dp)
                    )

                    categories.forEach { cat ->
                        val isSelected = selectedCategory == cat
                        FilterChip(
                            selected = isSelected,
                            onClick = { onCategorySelected(cat) },
                            label = {
                                Text(
                                    text = cat,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 13.sp
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = QYellowPrimary,
                                selectedLabelColor = QOnYellow,
                                containerColor = QSurface,
                                labelColor = QOnSurfaceVariant
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isSelected,
                                borderColor = if (isSelected) QYellowDark else QCardBorder
                            ),
                            shape = RoundedCornerShape(18.dp)
                        )
                    }
                }
            }

            // Loading state
            if (isLoading && posts.isEmpty()) {
                item {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(40.dp)
                    ) {
                        CircularProgressIndicator(color = QYellowPrimary)
                    }
                }
            } else if (feedError != null && filteredPosts.isEmpty()) {
                // Error state
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = QSurface),
                        border = androidx.compose.foundation.BorderStroke(1.dp, QCardBorder),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp)
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(RoundedCornerShape(32.dp))
                                    .background(androidx.compose.ui.graphics.Color.Red.copy(alpha = 0.15f))
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = androidx.compose.ui.graphics.Color.Red,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = "Unable to load feed",
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp,
                                color = QOnSurface
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = feedError,
                                fontSize = 13.sp,
                                color = QOnSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(18.dp))
                            Button(
                                onClick = onRetryFeed,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = QYellowPrimary,
                                    contentColor = QOnYellow
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Retry", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            } else if (filteredPosts.isEmpty()) {
                // Empty state
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = QSurface),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp)
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(RoundedCornerShape(32.dp))
                                    .background(QYellowLight)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = QYellowDark,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = "No posts in $selectedCategory yet",
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp,
                                color = QOnSurface
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Be the first creator to share an update or question with the community!",
                                fontSize = 13.sp,
                                color = QOnSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(18.dp))
                            Button(
                                onClick = onCreatePostClick,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = QYellowPrimary,
                                    contentColor = QOnYellow
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Create Post", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            } else {
                // Posts list with side-scrolling series rows
                filteredPosts.forEachIndexed { index, post ->
                    item(key = "post_${post.id}") {
                        Box(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) {
                            PostCard(
                                post = post,
                                onLikeClick = { onLikeClick(post) },
                                onCommentClick = { onCommentClick(post) },
                                onBookmarkClick = { onBookmarkClick(post) },
                                onAuthorClick = onAuthorClick,
                                currentUserId = currentUser?.id,
                                onEditPostClick = onEditPostClick,
                                onDeletePostClick = onDeletePostClick,
                                onOpenReels = onOpenReels,
                                onOpenMenu = onOpenMenu,
                                onLikersClick = onLikersClick,
                                onRelayClick = onRelayClick
                            )
                        }
                    }

                    // Insert side-scrolling series shelf after index 2 (3rd post), and every 6 posts thereafter
                    val shelfIndex = when (index) {
                        2 -> 0
                        7 -> 1
                        13 -> 2
                        19 -> 3
                        else -> -1
                    }
                    if (shelfIndex >= 0 && effectiveShelves.isNotEmpty()) {
                        val targetShelf = effectiveShelves[shelfIndex % effectiveShelves.size]
                        item(key = "series_shelf_${shelfIndex}_${targetShelf.shelf.id}") {
                            FeedSeriesShelfRow(
                                shelf = targetShelf,
                                watchlistIds = watchlistIds,
                                onSeriesClick = { slug -> onSeriesClick?.invoke(slug) },
                                onWatchlistToggle = { card -> onWatchlistToggle?.invoke(card) },
                                onSeeAllClick = onSeeAllSeriesClick
                            )
                        }
                    }
                }

                // If feed has fewer than 3 posts (e.g. 1 or 2 posts), show the series row right after them
                if (filteredPosts.isNotEmpty() && filteredPosts.size < 3 && effectiveShelves.isNotEmpty()) {
                    val targetShelf = effectiveShelves.first()
                    item(key = "series_shelf_short_${targetShelf.shelf.id}") {
                        FeedSeriesShelfRow(
                            shelf = targetShelf,
                            watchlistIds = watchlistIds,
                            onSeriesClick = { slug -> onSeriesClick?.invoke(slug) },
                            onWatchlistToggle = { card -> onWatchlistToggle?.invoke(card) },
                            onSeeAllClick = onSeeAllSeriesClick
                        )
                    }
                }
            }
        }

        // Story Preview Dialog
        storyPreviewProfile?.let { profile ->
            AlertDialog(
                onDismissRequest = { storyPreviewProfile = null },
                confirmButton = {
                    Button(
                        onClick = {
                            storyPreviewProfile = null
                            onAuthorClick(profile)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = QYellowPrimary,
                            contentColor = QOnYellow
                        )
                    ) {
                        Text("View Full Profile")
                    }
                },
                dismissButton = {
                    IconButton(onClick = { storyPreviewProfile = null }) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                },
                text = {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        UserAvatar(
                            avatarUrl = profile.avatarUrl,
                            initials = profile.initials,
                            size = 72.dp
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = profile.effectiveDisplayName,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = QOnSurface
                        )
                        Text(
                            text = "@${profile.effectiveUsername}",
                            fontSize = 13.sp,
                            color = QOnSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        CreatorRankChip(rank = profile.creatorRank, level = profile.creatorLevel)
                        Spacer(modifier = Modifier.height(8.dp))
                        PointsBadge(points = profile.points)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = profile.headline ?: profile.bio ?: "Active creator on Qylomi",
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center,
                            color = QOnSurface
                        )
                    }
                }
            )
        }
    }
}
}

/**
 * Side-scrolling Series Cards Row for the Feed
 */
@Composable
fun FeedSeriesShelfRow(
    shelf: ShelfWithItems,
    watchlistIds: Set<String>,
    onSeriesClick: (String) -> Unit,
    onWatchlistToggle: (SeriesCard) -> Unit,
    onSeeAllClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    if (shelf.items.isEmpty()) return

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = QSurface),
        border = BorderStroke(1.dp, QCardBorder),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .testTag("feed_series_shelf_${shelf.shelf.id}")
    ) {
        Column(
            modifier = Modifier.padding(vertical = 12.dp)
        ) {
            // Header: Icon + Shelf Title + "See All"
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(QYellowPrimary.copy(alpha = 0.18f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = QYellowDark,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = shelf.shelf.title.ifBlank { "Featured Series" },
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = QOnSurface
                        )
                        Text(
                            text = "Swipe to explore anime & series",
                            fontSize = 11.sp,
                            color = QOnSurfaceVariant
                        )
                    }
                }

                if (onSeeAllClick != null) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = QYellowPrimary.copy(alpha = 0.12f),
                        modifier = Modifier.clickable { onSeeAllClick() }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Text(
                                text = "See All",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = QYellowDark
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = "See All Series",
                                tint = QYellowDark,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Side-scrolling row of series cards
            LazyRow(
                contentPadding = PaddingValues(horizontal = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(shelf.items, key = { it.id }) { card ->
                    SeriesCardItem(
                        card = card,
                        isInWatchlist = watchlistIds.contains(card.id),
                        onCardClick = { onSeriesClick(card.slug) },
                        onWatchlistClick = { onWatchlistToggle(card) },
                        modifier = Modifier.width(130.dp)
                    )
                }
            }
        }
    }
}

fun getFallbackSeriesShelves(): List<ShelfWithItems> {
    val sampleCards = listOf(
        SeriesCard(
            id = "s-demon-slayer",
            slug = "demon-slayer",
            title = "Demon Slayer: Kimetsu no Yaiba",
            description = "Tanjiro Kamado, a kindhearted boy who sells charcoal for a living, finds his family slaughtered by a demon.",
            posterUrl = "https://image.tmdb.org/t/p/w500/xUfRZu2mi8jH6SzQEJGP6tjBuYj.jpg",
            bannerUrl = "https://image.tmdb.org/t/p/original/nTvM4mhqZlHIvUkIvd6vv79QOqv.jpg",
            rating = 8.7,
            status = "ongoing",
            episodesCount = 26,
            releaseYear = 2019,
            genres = listOf("Action", "Fantasy", "Animation")
        ),
        SeriesCard(
            id = "s-solo-leveling",
            slug = "solo-leveling",
            title = "Solo Leveling: Arise",
            description = "In a world where hunters must battle deadly monsters, Sung Jinwoo is an E-rank hunter who discovers a unique quest system.",
            posterUrl = "https://image.tmdb.org/t/p/w500/geCRueV3ElhRTr0xtJuClJknQd7.jpg",
            bannerUrl = "https://image.tmdb.org/t/p/original/96yA0B59aJ1o6x3q8g1Cqg9.jpg",
            rating = 8.5,
            status = "ongoing",
            episodesCount = 12,
            releaseYear = 2024,
            genres = listOf("Action", "Adventure", "Fantasy")
        ),
        SeriesCard(
            id = "s-jujutsu-kaisen",
            slug = "jujutsu-kaisen",
            title = "Jujutsu Kaisen",
            description = "A boy swallows a cursed talisman - the finger of a demon - and becomes cursed himself.",
            posterUrl = "https://image.tmdb.org/t/p/w500/hD8pZgMvV6Z2r9lW2Vn2zP2Kq.jpg",
            bannerUrl = "https://image.tmdb.org/t/p/original/gmP0Kj9ZpTq6gN8yK4w2rP7v.jpg",
            rating = 8.6,
            status = "completed",
            episodesCount = 24,
            releaseYear = 2020,
            genres = listOf("Action", "Supernatural")
        ),
        SeriesCard(
            id = "s-attack-on-titan",
            slug = "attack-on-titan",
            title = "Attack on Titan",
            description = "After his hometown is destroyed and his mother is killed, young Eren Jaeger vows to cleanse the earth of the giant humanoid Titans.",
            posterUrl = "https://image.tmdb.org/t/p/w500/hTP1DtLGFamjfu8WqjnuQdP1n4i.jpg",
            bannerUrl = "https://image.tmdb.org/t/p/original/2wTfQ7V8e7iQG7iT3u0eR9m.jpg",
            rating = 9.0,
            status = "completed",
            episodesCount = 89,
            releaseYear = 2013,
            genres = listOf("Action", "Drama", "Mystery")
        )
    )
    return listOf(
        ShelfWithItems(
            shelf = Shelf(id = "shelf-trending-feed", slug = "trending", title = "🔥 Trending Anime & Series", layout = "row"),
            items = sampleCards
        )
    )
}
