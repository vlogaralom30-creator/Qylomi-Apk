package com.example.ui.screens.series

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ContentCategory
import com.example.data.model.SeriesCard
import com.example.data.model.ShelfWithItems
import com.example.data.repository.SeriesRepository
import com.example.ui.components.SeriesCardItem
import com.example.ui.components.series.*
import com.example.ui.theme.*
import kotlinx.coroutines.launch

enum class SeriesSortOption(val label: String, val apiOrder: String) {
    LATEST("Latest", "created_at.desc"),
    TOP_RATED("Top Rated", "rating.desc.nullslast"),
    POPULAR("Popular", "views_count.desc"),
    TITLE_AZ("A - Z", "title.asc")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SeriesListScreen(
    shelfWithItems: ShelfWithItems?,
    categories: List<ContentCategory>,
    watchlistIds: Set<String>,
    onSeriesClick: (String) -> Unit,
    onWatchlistToggle: (SeriesCard) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }
    val context = LocalContext.current
    val repository = remember { SeriesRepository(context) }
    val scope = rememberCoroutineScope()

    val shelf = shelfWithItems?.shelf
    val title = shelf?.title ?: "Browse Series"

    val initialGenre = remember(shelf?.config) {
        shelf?.config?.get("genre") as? String
    }
    val initialCategory = remember(shelf?.config) {
        shelf?.config?.get("category") as? String
    }
    val initialSort = remember(shelf?.type) {
        when (shelf?.type) {
            "auto_trending" -> SeriesSortOption.POPULAR
            "latest_episodes" -> SeriesSortOption.LATEST
            else -> SeriesSortOption.LATEST
        }
    }

    var selectedSort by remember { mutableStateOf(initialSort) }
    var selectedGenre by remember { mutableStateOf<String?>(initialGenre) }
    var selectedLanguage by remember { mutableStateOf<String?>(null) }
    var selectedStatus by remember { mutableStateOf<String?>(null) }
    var selectedCategorySlug by remember { mutableStateOf<String?>(initialCategory) }

    var itemsList by remember { mutableStateOf<List<SeriesCard>>(shelfWithItems?.items ?: emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var hasMore by remember { mutableStateOf(true) }
    var offset by remember { mutableIntStateOf(0) }
    val pageSize = 24

    val genresList = listOf("All", "Action", "Adventure", "Comedy", "Drama", "Fantasy", "Horror", "Isekai", "Romance", "Sci-Fi", "Thriller")
    val languagesList = listOf("All", "Hindi", "Japanese", "English", "Bangla", "Tamil", "Telugu")
    val statusesList = listOf("All", "ongoing", "completed", "upcoming")

    fun loadItems(reset: Boolean = false) {
        scope.launch {
            if (reset) {
                offset = 0
                hasMore = true
                itemsList = emptyList()
            }
            isLoading = true

            val catObj = categories.firstOrNull { it.slug == selectedCategorySlug }
            val catFilter = catObj?.id

            val res = repository.getSeriesList(
                category = catFilter,
                genre = if (selectedGenre != null && selectedGenre != "All") selectedGenre else null,
                language = if (selectedLanguage != null && selectedLanguage != "All") selectedLanguage else null,
                status = if (selectedStatus != null && selectedStatus != "All") selectedStatus else null,
                sort = selectedSort.apiOrder,
                limit = pageSize,
                offset = if (reset) 0 else offset
            )

            if (res.isSuccess) {
                val cards = res.getOrNull() ?: emptyList()
                itemsList = if (reset) cards else itemsList + cards
                hasMore = cards.size >= pageSize
                offset += cards.size
            }
            isLoading = false
        }
    }

    LaunchedEffect(selectedSort, selectedGenre, selectedLanguage, selectedStatus, selectedCategorySlug) {
        loadItems(reset = true)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(QBackground)
            .testTag("series_list_screen")
    ) {
        // Top App Bar
        Surface(
            color = QSurface,
            shadowElevation = 1.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = QOnSurface)
                }
                Spacer(modifier = Modifier.width(6.dp))
                Column {
                    Text(
                        text = title,
                        style = SeriesTypography.headline.copy(fontSize = 18.sp),
                        color = QOnSurface
                    )
                    Text(
                        text = "${itemsList.size} titles loaded",
                        style = SeriesTypography.micro,
                        color = QOnSurfaceVariant
                    )
                }
            }
        }

        // Sticky Filter Chips Row
        Surface(
            color = QSurface,
            border = BorderStroke(0.5.dp, QCardBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            LazyRow(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Sort Chip
                item {
                    var sortExpanded by remember { mutableStateOf(false) }
                    Box {
                        Surface(
                            onClick = { sortExpanded = true },
                            shape = SeriesTokens.shapePill,
                            color = QSurfaceVariant,
                            border = BorderStroke(0.5.dp, QCardBorder)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Sort: ${selectedSort.label}", style = SeriesTypography.micro, fontWeight = FontWeight.Bold, color = QOnSurface)
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = QOnSurfaceVariant, modifier = Modifier.size(16.dp))
                            }
                        }
                        DropdownMenu(
                            expanded = sortExpanded,
                            onDismissRequest = { sortExpanded = false },
                            modifier = Modifier.background(QSurface)
                        ) {
                            SeriesSortOption.values().forEach { opt ->
                                DropdownMenuItem(
                                    text = { Text(opt.label, color = QOnSurface, style = SeriesTypography.label) },
                                    onClick = {
                                        selectedSort = opt
                                        sortExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Genre Chip
                item {
                    var genreExpanded by remember { mutableStateOf(false) }
                    val isFiltered = selectedGenre != null && selectedGenre != "All"
                    Box {
                        Surface(
                            onClick = { genreExpanded = true },
                            shape = SeriesTokens.shapePill,
                            color = if (isFiltered) QYellowPrimary else QSurfaceVariant,
                            border = BorderStroke(0.5.dp, if (isFiltered) QYellowPrimary else QCardBorder)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (isFiltered) selectedGenre!! else "Genre",
                                    style = SeriesTypography.micro,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isFiltered) QOnYellow else QOnSurface
                                )
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = if (isFiltered) QOnYellow else QOnSurfaceVariant, modifier = Modifier.size(16.dp))
                            }
                        }
                        DropdownMenu(
                            expanded = genreExpanded,
                            onDismissRequest = { genreExpanded = false },
                            modifier = Modifier.background(QSurface)
                        ) {
                            genresList.forEach { g ->
                                DropdownMenuItem(
                                    text = { Text(g, color = QOnSurface, style = SeriesTypography.label) },
                                    onClick = {
                                        selectedGenre = if (g == "All") null else g
                                        genreExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Language Chip
                item {
                    var langExpanded by remember { mutableStateOf(false) }
                    val isFiltered = selectedLanguage != null && selectedLanguage != "All"
                    Box {
                        Surface(
                            onClick = { langExpanded = true },
                            shape = SeriesTokens.shapePill,
                            color = if (isFiltered) QYellowPrimary else QSurfaceVariant,
                            border = BorderStroke(0.5.dp, if (isFiltered) QYellowPrimary else QCardBorder)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (isFiltered) selectedLanguage!! else "Language",
                                    style = SeriesTypography.micro,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isFiltered) QOnYellow else QOnSurface
                                )
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = if (isFiltered) QOnYellow else QOnSurfaceVariant, modifier = Modifier.size(16.dp))
                            }
                        }
                        DropdownMenu(
                            expanded = langExpanded,
                            onDismissRequest = { langExpanded = false },
                            modifier = Modifier.background(QSurface)
                        ) {
                            languagesList.forEach { l ->
                                DropdownMenuItem(
                                    text = { Text(l, color = QOnSurface, style = SeriesTypography.label) },
                                    onClick = {
                                        selectedLanguage = if (l == "All") null else l
                                        langExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Status Chip
                item {
                    var statusExpanded by remember { mutableStateOf(false) }
                    val isFiltered = selectedStatus != null && selectedStatus != "All"
                    Box {
                        Surface(
                            onClick = { statusExpanded = true },
                            shape = SeriesTokens.shapePill,
                            color = if (isFiltered) QYellowPrimary else QSurfaceVariant,
                            border = BorderStroke(0.5.dp, if (isFiltered) QYellowPrimary else QCardBorder)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (isFiltered) selectedStatus!!.replaceFirstChar { it.uppercase() } else "Status",
                                    style = SeriesTypography.micro,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isFiltered) QOnYellow else QOnSurface
                                )
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = if (isFiltered) QOnYellow else QOnSurfaceVariant, modifier = Modifier.size(16.dp))
                            }
                        }
                        DropdownMenu(
                            expanded = statusExpanded,
                            onDismissRequest = { statusExpanded = false },
                            modifier = Modifier.background(QSurface)
                        ) {
                            statusesList.forEach { s ->
                                DropdownMenuItem(
                                    text = { Text(s.replaceFirstChar { it.uppercase() }, color = QOnSurface, style = SeriesTypography.label) },
                                    onClick = {
                                        selectedStatus = if (s == "All") null else s
                                        statusExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        // Adaptive 3-Column Grid
        Box(modifier = Modifier.fillMaxSize()) {
            if (isLoading && itemsList.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    repeat(3) {
                        SkeletonRow()
                    }
                }
            } else if (itemsList.isEmpty()) {
                SeriesEmptyState(
                    icon = Icons.Default.FilterList,
                    title = "No series found",
                    description = "Try selecting different genres, languages or statuses.",
                    actionText = "Reset Filters",
                    onActionClick = {
                        selectedGenre = null
                        selectedLanguage = null
                        selectedStatus = null
                    },
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(itemsList.chunked(3)) { rowCards ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            for (card in rowCards) {
                                SeriesCardItem(
                                    card = card,
                                    isInWatchlist = watchlistIds.contains(card.id),
                                    onCardClick = { onSeriesClick(card.slug) },
                                    onWatchlistClick = { onWatchlistToggle(card) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            repeat(3 - rowCards.size) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }

                    // Load More Pill Button
                    if (hasMore) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isLoading) {
                                    CircularProgressIndicator(color = QYellowPrimary, modifier = Modifier.size(24.dp))
                                } else {
                                    GhostPillButton(
                                        text = "Load more titles",
                                        onClick = { loadItems(reset = false) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
