package com.example.ui.screens.series

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Tv
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
import com.example.ui.components.series.*
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
        Surface(
            color = QSurface,
            shadowElevation = 1.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (isSearchActive) {
                    // Search Input Field
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { viewModel.setSearchQuery(it) },
                        placeholder = { Text("Search anime, series, movies...", style = SeriesTypography.body) },
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
                        shape = SeriesTokens.shapePill,
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
                    // Golden Header
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = onMenuClick,
                            modifier = Modifier.testTag("series_menu_button")
                        ) {
                            Icon(imageVector = Icons.Default.Menu, contentDescription = "Menu", tint = QOnSurface)
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Golden",
                                style = SeriesTypography.headline,
                                fontWeight = FontWeight.Black,
                                color = QYellowDark
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Cinema",
                                style = SeriesTypography.headline,
                                fontWeight = FontWeight.Bold,
                                color = QOnSurface
                            )
                        }
                    }

                    IconButton(
                        onClick = { isSearchActive = true },
                        modifier = Modifier.testTag("series_search_button")
                    ) {
                        Icon(imageVector = Icons.Outlined.Search, contentDescription = "Search catalog", tint = QOnSurface)
                    }
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
                onCardClick = { card -> onSeriesClick(card.slug) },
                onWatchlistToggle = { card -> viewModel.toggleWatchlist(card.id, onAuthRequired) }
            )
        } else {
            // Main Pull-To-Refresh Catalog
            PullToRefreshBox(
                isRefreshing = isHomeRefreshing,
                onRefresh = { viewModel.refresh() },
                modifier = Modifier.fillMaxSize()
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 90.dp)
                ) {
                    // Category Chips Bar
                    item {
                        CategoryChipsRow(
                            categories = categories,
                            selectedCategory = selectedCategory,
                            onCategorySelected = { cat -> viewModel.selectCategory(cat) }
                        )
                    }

                    // Shimmer Skeletons while loading
                    if (isHomeLoading && shelves.isEmpty()) {
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(280.dp)
                                        .padding(horizontal = 16.dp)
                                        .clip(SeriesTokens.shapeCard)
                                        .shimmerEffect()
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                SkeletonRow()
                                Spacer(modifier = Modifier.height(10.dp))
                                SkeletonRow()
                            }
                        }
                    }

                    // Error Banner with Retry
                    if (errorMessage != null && shelves.isEmpty()) {
                        item {
                            SeriesErrorState(
                                message = errorMessage ?: "Failed to load series catalog",
                                onRetry = { viewModel.refresh() },
                                modifier = Modifier.padding(32.dp)
                            )
                        }
                    }

                    // 1. Continue Watching Personal Row
                    if (continueWatching.isNotEmpty()) {
                        item {
                            SectionHeader(title = "Continue Watching", onSeeAllClick = null)
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

                    // 2. My List Personal Row
                    if (watchlist.isNotEmpty()) {
                        item {
                            SectionHeader(title = "My List", onSeeAllClick = null)
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

                    // Empty Category State
                    if (shelves.isEmpty() && !isHomeLoading && errorMessage == null) {
                        item {
                            SeriesEmptyState(
                                icon = Icons.Outlined.Tv,
                                title = "No titles in this category",
                                description = "Check back soon for new updates, or explore other categories!"
                            )
                        }
                    }

                    // 3. Dynamic Shelves by Layout
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
                                    SectionHeader(
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
                                    Spacer(modifier = Modifier.height(18.dp))
                                }

                                "grid" -> {
                                    SectionHeader(
                                        title = shelf.title,
                                        onSeeAllClick = { onSeeAllClick(shelfWithItems) }
                                    )
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 14.dp),
                                        verticalArrangement = Arrangement.spacedBy(14.dp)
                                    ) {
                                        items.chunked(3).forEach { rowCards ->
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
                                    Spacer(modifier = Modifier.height(18.dp))
                                }

                                else -> { // "row" or standard shelf
                                    SectionHeader(
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
                                    Spacer(modifier = Modifier.height(18.dp))
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
            val isSelected = selectedCategory == null
            Surface(
                onClick = { onCategorySelected(null) },
                shape = SeriesTokens.shapePill,
                color = if (isSelected) QYellowPrimary else QSurfaceVariant,
                border = BorderStroke(0.75.dp, if (isSelected) QYellowPrimary else QCardBorder),
                modifier = Modifier
                    .pressScale(onClick = { onCategorySelected(null) })
                    .testTag("category_chip_all")
            ) {
                Text(
                    text = "All",
                    style = SeriesTypography.label,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) QOnYellow else QOnSurface,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 7.dp)
                )
            }
        }

        items(categories, key = { it.id }) { cat ->
            val isSelected = selectedCategory?.id == cat.id
            Surface(
                onClick = { onCategorySelected(cat) },
                shape = SeriesTokens.shapePill,
                color = if (isSelected) QYellowPrimary else QSurfaceVariant,
                border = BorderStroke(0.75.dp, if (isSelected) QYellowPrimary else QCardBorder),
                modifier = Modifier
                    .pressScale(onClick = { onCategorySelected(cat) })
                    .testTag("category_chip_${cat.slug}")
            ) {
                Text(
                    text = cat.name,
                    style = SeriesTypography.label,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) QOnYellow else QOnSurface,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 7.dp)
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
            SeriesEmptyState(
                icon = Icons.Outlined.Search,
                title = "No results found",
                description = "We couldn't find any series matching \"$query\". Try checking for typos or searching another term."
            )
        } else if (query.trim().length < 2) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Type at least 2 characters to search...",
                    style = SeriesTypography.body,
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
                        style = SeriesTypography.label,
                        color = QOnSurfaceVariant
                    )
                }

                items(results.chunked(3)) { rowItems ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
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
                        repeat(3 - rowItems.size) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}
