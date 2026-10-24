package com.example.ui.screens.series

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ContentCategory
import com.example.data.model.ContinueWatchingItem
import com.example.data.model.SeriesCard
import com.example.data.model.ShelfWithItems
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.SeriesViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SeriesHomeScreen(
    viewModel: SeriesViewModel,
    onMenuClick: () -> Unit,
    onSeriesClick: (String) -> Unit, // slug
    onEpisodePlay: (String, String) -> Unit, // seriesId, episodeId
    onSeeAllClick: (ShelfWithItems) -> Unit,
    onAuthRequired: () -> Unit,
    modifier: Modifier = Modifier
) {
    val categories by viewModel.categories.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val shelves by viewModel.shelves.collectAsState()
    val continueWatching by viewModel.continueWatching.collectAsState()
    val watchlist by viewModel.watchlist.collectAsState()
    val isHomeLoading by viewModel.isHomeLoading.collectAsState()
    val isHomeRefreshing by viewModel.isHomeRefreshing.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val watchlistIds by viewModel.watchlistIds.collectAsState()

    // Search state
    var isSearchActive by remember { mutableStateOf(false) }
    val searchQuery by viewModel.searchQuery.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    val isSearching by viewModel.isSearching.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(QBackground)
            .testTag("series_home_screen")
    ) {
        // 1. Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(QSurface)
                .statusBarsPadding()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            if (isSearchActive) {
                // Search Input Mode
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.setSearchQuery(it) },
                    placeholder = { Text("Search anime, series, movies...", fontSize = 14.sp) },
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = QOnSurfaceVariant)
                    },
                    trailingIcon = {
                        IconButton(onClick = {
                            if (searchQuery.isNotEmpty()) {
                                viewModel.setSearchQuery("")
                            } else {
                                isSearchActive = false
                            }
                        }) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Close search", tint = QOnSurfaceVariant)
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = QSurfaceVariant,
                        unfocusedContainerColor = QSurfaceVariant,
                        focusedBorderColor = QYellowPrimary,
                        unfocusedBorderColor = Color.Transparent
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("series_search_input")
                )
            } else {
                // Normal Header Mode
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onMenuClick,
                        modifier = Modifier.testTag("series_menu_button")
                    ) {
                        Icon(imageVector = Icons.Default.Menu, contentDescription = "Menu", tint = QOnSurface)
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Series & Anime",
                        fontSize = 19.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = QOnSurface,
                        letterSpacing = (-0.5).sp
                    )
                }

                IconButton(
                    onClick = { isSearchActive = true },
                    modifier = Modifier.testTag("series_search_button")
                ) {
                    Icon(imageVector = Icons.Default.Search, contentDescription = "Search catalog", tint = QOnSurface)
                }
            }
        }

        // Search Results Overlay
        if (isSearchActive) {
            SearchResultsView(
                query = searchQuery,
                results = searchResults,
                isSearching = isSearching,
                isInWatchlist = { id -> watchlistIds.contains(id) },
                onCardClick = { card ->
                    onSeriesClick(card.slug)
                },
                onWatchlistToggle = { card ->
                    viewModel.toggleWatchlist(card.id, onAuthRequired)
                }
            )
        } else {
            // Normal Catalog View with Pull-To-Refresh
            PullToRefreshBox(
                isRefreshing = isHomeRefreshing,
                onRefresh = { viewModel.refresh() },
                modifier = Modifier.fillMaxSize()
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 90.dp)
                ) {
                    // 2. Category Chips Bar
                    item {
                        CategoryChipsRow(
                            categories = categories,
                            selectedCategory = selectedCategory,
                            onCategorySelected = { cat -> viewModel.selectCategory(cat) }
                        )
                    }

                    // Loading Spinner
                    if (isHomeLoading) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(color = QYellowPrimary)
                            }
                        }
                    }

                    // Error Banner
                    if (errorMessage != null && shelves.isEmpty()) {
                        item {
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = QSurface),
                                border = androidx.compose.foundation.BorderStroke(1.dp, QCardBorder),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = errorMessage ?: "Failed to load series catalog",
                                        color = QOnSurface,
                                        fontSize = 14.sp
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Button(
                                        onClick = { viewModel.refresh() },
                                        colors = ButtonDefaults.buttonColors(containerColor = QYellowPrimary, contentColor = QOnYellow)
                                    ) {
                                        Text("Retry")
                                    }
                                }
                            }
                        }
                    }

                    // 3. Continue Watching Personal Row
                    if (continueWatching.isNotEmpty()) {
                        item {
                            ShelfHeader(title = "Continue Watching", onSeeAllClick = null)
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(continueWatching, key = { it.episode?.id ?: it.series?.id ?: it.hashCode().toString() }) { item ->
                                    ContinueWatchingCard(
                                        item = item,
                                        onClick = {
                                            val sId = item.series?.id ?: item.episode?.seriesId ?: ""
                                            val epId = item.episode?.id ?: ""
                                            onEpisodePlay(sId, epId)
                                        }
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }

                    // 4. My List Personal Row
                    if (watchlist.isNotEmpty()) {
                        item {
                            ShelfHeader(title = "My List", onSeeAllClick = null)
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(watchlist, key = { it.id }) { card ->
                                    SeriesCardItem(
                                        card = card,
                                        isInWatchlist = true,
                                        onCardClick = { onSeriesClick(card.slug) },
                                        onWatchlistClick = { viewModel.toggleWatchlist(card.id, onAuthRequired) }
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }

                    // 5. Dynamic Shelves (strictly rendered by the response layout in order)
                    if (shelves.isEmpty() && !isHomeLoading && errorMessage == null) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "No series available in this category yet.",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = QOnSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "Check back soon for new updates!",
                                        fontSize = 12.sp,
                                        color = QOnSurfaceVariant.copy(alpha = 0.7f)
                                    )
                                }
                            }
                        }
                    }

                    items(shelves, key = { it.shelf.id }) { shelfWithItems ->
                        val shelf = shelfWithItems.shelf
                        val items = shelfWithItems.items

                        if (items.isNotEmpty()) {
                            when (shelf.layout) {
                                "hero" -> {
                                    HeroCarousel(
                                        items = items,
                                        isInWatchlist = { id -> watchlistIds.contains(id) },
                                        onCardClick = { card -> onSeriesClick(card.slug) },
                                        onPlayClick = { card -> onSeriesClick(card.slug) },
                                        onWatchlistToggle = { card -> viewModel.toggleWatchlist(card.id, onAuthRequired) },
                                        modifier = Modifier.padding(bottom = 16.dp)
                                    )
                                }

                                "top10" -> {
                                    ShelfHeader(
                                        title = shelf.title,
                                        onSeeAllClick = { onSeeAllClick(shelfWithItems) }
                                    )
                                    LazyRow(
                                        contentPadding = PaddingValues(horizontal = 16.dp),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        itemsIndexed(items, key = { _, card -> card.id }) { index, card ->
                                            Top10SeriesCardItem(
                                                rank = index + 1,
                                                card = card,
                                                isInWatchlist = watchlistIds.contains(card.id),
                                                onCardClick = { onSeriesClick(card.slug) },
                                                onWatchlistClick = { viewModel.toggleWatchlist(card.id, onAuthRequired) }
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(16.dp))
                                }

                                else -> { // "row" or fallback
                                    ShelfHeader(
                                        title = shelf.title,
                                        onSeeAllClick = { onSeeAllClick(shelfWithItems) }
                                    )
                                    LazyRow(
                                        contentPadding = PaddingValues(horizontal = 16.dp),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        items(items, key = { it.id }) { card ->
                                            SeriesCardItem(
                                                card = card,
                                                isInWatchlist = watchlistIds.contains(card.id),
                                                onCardClick = { onSeriesClick(card.slug) },
                                                onWatchlistClick = { viewModel.toggleWatchlist(card.id, onAuthRequired) }
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(16.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryChipsRow(
    categories: List<ContentCategory>,
    selectedCategory: ContentCategory?,
    onCategorySelected: (ContentCategory?) -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // "All" chip
        item {
            FilterChip(
                selected = selectedCategory == null,
                onClick = { onCategorySelected(null) },
                label = { Text("All", fontWeight = FontWeight.SemiBold) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = QYellowPrimary,
                    selectedLabelColor = QOnYellow,
                    containerColor = QSurface,
                    labelColor = QOnSurface
                ),
                shape = RoundedCornerShape(20.dp),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = selectedCategory == null,
                    borderColor = QCardBorder,
                    selectedBorderColor = QYellowPrimary
                ),
                modifier = Modifier.testTag("category_chip_all")
            )
        }

        items(categories, key = { it.id }) { cat ->
            val isSelected = selectedCategory?.id == cat.id
            FilterChip(
                selected = isSelected,
                onClick = { onCategorySelected(cat) },
                label = { Text(cat.name, fontWeight = FontWeight.SemiBold) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = QYellowPrimary,
                    selectedLabelColor = QOnYellow,
                    containerColor = QSurface,
                    labelColor = QOnSurface
                ),
                shape = RoundedCornerShape(20.dp),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = isSelected,
                    borderColor = QCardBorder,
                    selectedBorderColor = QYellowPrimary
                ),
                modifier = Modifier.testTag("category_chip_${cat.slug}")
            )
        }
    }
}

@Composable
private fun ShelfHeader(
    title: String,
    onSeeAllClick: (() -> Unit)?
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            color = QOnSurface
        )

        if (onSeeAllClick != null) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onSeeAllClick() }
                    .padding(horizontal = 6.dp, vertical = 4.dp)
                    .testTag("see_all_$title")
            ) {
                Text(
                    text = "See all",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = QYellowDark
                )
                Spacer(modifier = Modifier.width(2.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = QYellowDark,
                    modifier = Modifier.size(13.dp)
                )
            }
        }
    }
}

@Composable
private fun SearchResultsView(
    query: String,
    results: List<SeriesCard>,
    isSearching: Boolean,
    isInWatchlist: (String) -> Boolean,
    onCardClick: (SeriesCard) -> Unit,
    onWatchlistToggle: (SeriesCard) -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        if (isSearching) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = QYellowPrimary)
            }
        } else if (query.trim().length >= 2 && results.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No series matching \"$query\"",
                    fontSize = 14.sp,
                    color = QOnSurfaceVariant
                )
            }
        } else if (query.trim().length < 2) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Type at least 2 characters to search...",
                    fontSize = 14.sp,
                    color = QOnSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Text(
                        text = "Found ${results.size} result${if (results.size == 1) "" else "s"}",
                        fontSize = 13.sp,
                        color = QOnSurfaceVariant
                    )
                }

                // Grid layout with 3 items per row
                items(results.chunked(3)) { rowItems ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        for (card in rowItems) {
                            SeriesCardItem(
                                card = card,
                                isInWatchlist = isInWatchlist(card.id),
                                onCardClick = { onCardClick(card) },
                                onWatchlistClick = { onWatchlistToggle(card) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        // Filler space if row has less than 3
                        repeat(3 - rowItems.size) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}
