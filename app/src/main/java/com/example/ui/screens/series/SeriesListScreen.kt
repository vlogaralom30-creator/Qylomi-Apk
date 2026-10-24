package com.example.ui.screens.series

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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

    // Parse pre-filter from config
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

    // Function to reload items with current filters
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
                if (reset) {
                    itemsList = cards
                } else {
                    itemsList = itemsList + cards
                }
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
        // Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(QSurface)
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
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = QOnSurface
                )
                Text(
                    text = "${itemsList.size} titles",
                    fontSize = 11.sp,
                    color = QOnSurfaceVariant
                )
            }
        }

        // Filters Bar (Horizontal scrolling chips)
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .background(QSurface)
                .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Sort Dropdown button
            item {
                var sortExpanded by remember { mutableStateOf(false) }
                Box {
                    AssistChip(
                        onClick = { sortExpanded = true },
                        label = { Text("Sort: ${selectedSort.label}", fontSize = 12.sp) },
                        trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        colors = AssistChipDefaults.assistChipColors(containerColor = QSurfaceVariant)
                    )
                    DropdownMenu(
                        expanded = sortExpanded,
                        onDismissRequest = { sortExpanded = false },
                        modifier = Modifier.background(QSurface)
                    ) {
                        SeriesSortOption.values().forEach { opt ->
                            DropdownMenuItem(
                                text = { Text(opt.label, color = QOnSurface, fontSize = 13.sp) },
                                onClick = {
                                    selectedSort = opt
                                    sortExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            // Genre Dropdown
            item {
                var genreExpanded by remember { mutableStateOf(false) }
                Box {
                    AssistChip(
                        onClick = { genreExpanded = true },
                        label = { Text(if (selectedGenre.isNullOrBlank() || selectedGenre == "All") "Genre" else selectedGenre!!, fontSize = 12.sp) },
                        trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = if (selectedGenre != null && selectedGenre != "All") QYellowPrimary.copy(alpha = 0.2f) else QSurfaceVariant
                        )
                    )
                    DropdownMenu(
                        expanded = genreExpanded,
                        onDismissRequest = { genreExpanded = false },
                        modifier = Modifier.background(QSurface)
                    ) {
                        genresList.forEach { g ->
                            DropdownMenuItem(
                                text = { Text(g, color = QOnSurface, fontSize = 13.sp) },
                                onClick = {
                                    selectedGenre = if (g == "All") null else g
                                    genreExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            // Status Dropdown
            item {
                var statusExpanded by remember { mutableStateOf(false) }
                Box {
                    AssistChip(
                        onClick = { statusExpanded = true },
                        label = { Text(if (selectedStatus.isNullOrBlank() || selectedStatus == "All") "Status" else selectedStatus!!.replaceFirstChar { it.uppercase() }, fontSize = 12.sp) },
                        trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = if (selectedStatus != null && selectedStatus != "All") QYellowPrimary.copy(alpha = 0.2f) else QSurfaceVariant
                        )
                    )
                    DropdownMenu(
                        expanded = statusExpanded,
                        onDismissRequest = { statusExpanded = false },
                        modifier = Modifier.background(QSurface)
                    ) {
                        statusesList.forEach { s ->
                            DropdownMenuItem(
                                text = { Text(s.replaceFirstChar { it.uppercase() }, color = QOnSurface, fontSize = 13.sp) },
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

        // Grid Content (3 columns)
        Box(modifier = Modifier.fillMaxSize()) {
            if (isLoading && itemsList.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = QYellowPrimary)
                }
            } else if (itemsList.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No series found matching current filters.", color = QOnSurfaceVariant, fontSize = 14.sp)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(itemsList.chunked(3)) { rowItems ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            for (card in rowItems) {
                                SeriesCardItem(
                                    card = card,
                                    isInWatchlist = watchlistIds.contains(card.id),
                                    onCardClick = { onSeriesClick(card.slug) },
                                    onWatchlistClick = { onWatchlistToggle(card) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            repeat(3 - rowItems.size) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }

                    // Load More Button
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
                                    OutlinedButton(
                                        onClick = { loadItems(reset = false) },
                                        shape = RoundedCornerShape(20.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = QOnSurface)
                                    ) {
                                        Text("Load more")
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
