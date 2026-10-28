package com.example.ui.screens.studio

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.CreatorStudioViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreatorStudioScreen(
    viewModel: CreatorStudioViewModel,
    currentUser: UserProfile?,
    onBack: () -> Unit
) {
    val context = LocalContext.current

    // Security & Role Guard: close if not creator or admin
    LaunchedEffect(currentUser) {
        if (currentUser == null || !currentUser.canCreateSeries) {
            onBack()
        }
    }

    val mySeries by viewModel.mySeries.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val activeCategories by viewModel.activeCategories.collectAsState()
    val eligibleShelves by viewModel.eligibleShelves.collectAsState()
    val selectedSeries by viewModel.selectedSeries.collectAsState()
    val seasons by viewModel.seriesSeasons.collectAsState()
    val episodes by viewModel.seriesEpisodes.collectAsState()
    val bannerMessage by viewModel.bannerMessage.collectAsState()

    var isCreatingSeries by rememberSaveable { mutableStateOf(false) }
    var isManagingContent by rememberSaveable { mutableStateOf(false) }
    var episodeToEdit by remember { mutableStateOf<Episode?>(null) }
    var isCreatingEpisode by rememberSaveable { mutableStateOf(false) }
    var showBulkAddDialog by rememberSaveable { mutableStateOf(false) }
    var showAddSeasonDialog by rememberSaveable { mutableStateOf(false) }
    var seriesToDelete by remember { mutableStateOf<Series?>(null) }

    LaunchedEffect(bannerMessage) {
        bannerMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.setBanner(null)
        }
    }

    BackHandler {
        when {
            episodeToEdit != null || isCreatingEpisode -> {
                episodeToEdit = null
                isCreatingEpisode = false
            }
            isManagingContent -> isManagingContent = false
            isCreatingSeries -> {
                isCreatingSeries = false
                viewModel.selectSeries(null)
            }
            else -> onBack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = when {
                                episodeToEdit != null -> "Edit Episode"
                                isCreatingEpisode -> "New Episode"
                                isManagingContent -> selectedSeries?.title ?: "Manage Content"
                                isCreatingSeries -> if (selectedSeries != null) "Edit Series" else "New Series"
                                else -> "Creator Studio"
                            },
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = if (currentUser?.isAdmin == true) "Role: Admin" else "Role: Creator",
                            fontSize = 11.sp,
                            color = QYellowDark
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        when {
                            episodeToEdit != null || isCreatingEpisode -> {
                                episodeToEdit = null
                                isCreatingEpisode = false
                            }
                            isManagingContent -> isManagingContent = false
                            isCreatingSeries -> {
                                isCreatingSeries = false
                                viewModel.selectSeries(null)
                            }
                            else -> onBack()
                        }
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = QBackground,
                    titleContentColor = QOnSurface
                )
            )
        },
        floatingActionButton = {
            if (!isCreatingSeries && !isManagingContent && episodeToEdit == null && !isCreatingEpisode) {
                FloatingActionButton(
                    onClick = {
                        viewModel.selectSeries(null)
                        isCreatingSeries = true
                    },
                    containerColor = QYellowDark,
                    contentColor = Color.Black,
                    modifier = Modifier.testTag("new_series_fab")
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "New Series")
                }
            }
        },
        containerColor = QBackground
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                // 1. Episode and Sources Editor
                episodeToEdit != null || isCreatingEpisode -> {
                    val targetSeries = selectedSeries
                    if (targetSeries != null) {
                        EpisodeFormView(
                            series = targetSeries,
                            existingEpisode = episodeToEdit,
                            seasons = seasons,
                            existingEpisodes = episodes,
                            viewModel = viewModel,
                            onDone = {
                                episodeToEdit = null
                                isCreatingEpisode = false
                            }
                        )
                    }
                }

                // 2. Episodes & Seasons Manager
                isManagingContent -> {
                    val current = selectedSeries
                    if (current != null) {
                        EpisodesManagerView(
                            series = current,
                            seasons = seasons,
                            episodes = episodes,
                            onAddSeason = { showAddSeasonDialog = true },
                            onBulkAdd = { showBulkAddDialog = true },
                            onAddEpisode = { isCreatingEpisode = true },
                            onEditEpisode = { ep -> episodeToEdit = ep },
                            onDeleteEpisode = { ep ->
                                viewModel.deleteEpisode(ep.id, current.id) { success, msg ->
                                    Toast.makeText(context, msg ?: "", Toast.LENGTH_SHORT).show()
                                }
                            },
                            onDeleteSeason = { sId ->
                                viewModel.deleteSeason(sId, current.id) { success, msg ->
                                    Toast.makeText(context, msg ?: "", Toast.LENGTH_SHORT).show()
                                }
                            }
                        )
                    }
                }

                // 3. Series Metadata Form (Create / Edit)
                isCreatingSeries -> {
                    SeriesFormView(
                        existingSeries = selectedSeries,
                        categories = activeCategories,
                        eligibleShelves = eligibleShelves,
                        currentUser = currentUser,
                        viewModel = viewModel,
                        onCancel = {
                            isCreatingSeries = false
                            viewModel.selectSeries(null)
                        },
                        onSaved = { series ->
                            isCreatingSeries = false
                            viewModel.selectSeries(series)
                        }
                    )
                }

                // 4. My Series Catalog Overview
                else -> {
                    if (isLoading && mySeries.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = QYellowDark)
                        }
                    } else if (mySeries.isEmpty()) {
                        EmptyStudioState(onCreateClick = { isCreatingSeries = true })
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(mySeries, key = { it.id }) { series ->
                                CreatorSeriesCard(
                                    series = series,
                                    onEdit = {
                                        viewModel.selectSeries(series)
                                        isCreatingSeries = true
                                    },
                                    onManageEpisodes = {
                                        viewModel.selectSeries(series)
                                        isManagingContent = true
                                    },
                                    onTogglePublish = {
                                        viewModel.togglePublishSeries(series) { success, msg ->
                                            Toast.makeText(context, msg ?: "", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    onDelete = { seriesToDelete = series }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Confirmation dialog: Delete Series
    seriesToDelete?.let { series ->
        AlertDialog(
            onDismissRequest = { seriesToDelete = null },
            title = { Text("Delete '${series.title}'?") },
            text = { Text("This will permanently remove the series and all its episodes and sources.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        val sId = series.id
                        seriesToDelete = null
                        viewModel.deleteSeries(sId) { success, msg ->
                            Toast.makeText(context, msg ?: "", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = Color.Red)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { seriesToDelete = null }) { Text("Cancel") }
            },
            containerColor = QSurface,
            titleContentColor = Color.White,
            textContentColor = QOnSurfaceVariant
        )
    }

    // Dialog: Batch Add Episodes & Video Sources
    if (showBulkAddDialog && selectedSeries != null) {
        var countText by remember { mutableStateOf("12") }
        var startEpText by remember { mutableStateOf("1") }
        var seasonNumText by remember { mutableStateOf("1") }
        var titlePrefix by remember { mutableStateOf("Episode") }
        var urlsText by remember { mutableStateOf("") }
        var serverLabel by remember { mutableStateOf("Server 1") }
        var quality by remember { mutableStateOf("1080p") }
        var language by remember { mutableStateOf("Japanese Subbed") }

        AlertDialog(
            onDismissRequest = { showBulkAddDialog = false },
            title = { Text("Batch Episode Generator") },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Batch create episodes and attach video URLs automatically. Paste stream links below (one per line).",
                        fontSize = 12.sp,
                        color = QOnSurfaceVariant
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = seasonNumText,
                            onValueChange = { seasonNumText = it.filter { ch -> ch.isDigit() } },
                            label = { Text("Season #") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )

                        OutlinedTextField(
                            value = startEpText,
                            onValueChange = { startEpText = it.filter { ch -> ch.isDigit() } },
                            label = { Text("Start Ep #") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )

                        OutlinedTextField(
                            value = countText,
                            onValueChange = { countText = it.filter { ch -> ch.isDigit() } },
                            label = { Text("Ep Count") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    OutlinedTextField(
                        value = titlePrefix,
                        onValueChange = { titlePrefix = it },
                        label = { Text("Title Prefix (e.g., Episode)") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = urlsText,
                        onValueChange = { urlsText = it },
                        label = { Text("Stream URLs (One URL per line)") },
                        placeholder = { Text("https://server.com/ep1.m3u8\nhttps://server.com/ep2.m3u8...") },
                        minLines = 4,
                        maxLines = 8,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = serverLabel,
                            onValueChange = { serverLabel = it },
                            label = { Text("Server Label") },
                            modifier = Modifier.weight(1f)
                        )

                        OutlinedTextField(
                            value = language,
                            onValueChange = { language = it },
                            label = { Text("Audio/Sub") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val count = countText.toIntOrNull() ?: 1
                        val startEp = startEpText.toIntOrNull() ?: 1
                        val seasonNum = seasonNumText.toIntOrNull() ?: 1
                        val urlsList = urlsText.lines().map { it.trim() }.filter { it.isNotBlank() }

                        showBulkAddDialog = false
                        viewModel.batchAddEpisodesWithSources(
                            seriesId = selectedSeries!!.id,
                            seasonNumber = seasonNum,
                            startEpisodeNum = startEp,
                            count = count.coerceAtLeast(if (urlsList.isNotEmpty()) urlsList.size else 1),
                            titlePrefix = titlePrefix.trim(),
                            urls = urlsList,
                            defaultServerLabel = serverLabel.ifBlank { "Server 1" },
                            defaultQuality = quality,
                            defaultLanguage = language.ifBlank { null }
                        ) { success, msg ->
                            Toast.makeText(context, msg ?: "", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = QYellowDark, contentColor = Color.Black)
                ) {
                    Text("Batch Generate", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showBulkAddDialog = false }) { Text("Cancel") }
            },
            containerColor = QSurface,
            titleContentColor = Color.White,
            textContentColor = QOnSurfaceVariant
        )
    }


    // Dialog: Add Season
    if (showAddSeasonDialog && selectedSeries != null) {
        var seasonNumText by remember { mutableStateOf("${(seasons.maxOfOrNull { it.seasonNumber } ?: 0) + 1}") }
        var seasonTitle by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddSeasonDialog = false },
            title = { Text("Add Season") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = seasonNumText,
                        onValueChange = { seasonNumText = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Season Number") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = seasonTitle,
                        onValueChange = { seasonTitle = it },
                        label = { Text("Title (Optional)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val num = seasonNumText.toIntOrNull() ?: 1
                        showAddSeasonDialog = false
                        viewModel.saveSeason(selectedSeries!!.id, num, seasonTitle.takeIf { it.isNotBlank() }, null) { success, msg ->
                            Toast.makeText(context, msg ?: "", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = QYellowDark, contentColor = Color.Black)
                ) {
                    Text("Add")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddSeasonDialog = false }) { Text("Cancel") }
            },
            containerColor = QSurface,
            titleContentColor = Color.White,
            textContentColor = QOnSurfaceVariant
        )
    }
}

// -----------------------------------------------------------------------------
// Component: Creator Series Card
// -----------------------------------------------------------------------------
@Composable
private fun CreatorSeriesCard(
    series: Series,
    onEdit: () -> Unit,
    onManageEpisodes: () -> Unit,
    onTogglePublish: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = QSurface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                // 2:3 Poster
                AsyncImage(
                    model = series.posterUrl ?: series.bannerUrl,
                    contentDescription = series.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .width(72.dp)
                        .height(108.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(QSurfaceVariant)
                )

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = series.title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = QOnSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (!series.originalTitle.isNullOrBlank()) {
                        Text(
                            text = series.originalTitle,
                            fontSize = 12.sp,
                            color = QOnSurfaceVariant,
                            maxLines = 1
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        series.category?.let { cat ->
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = QSurfaceVariant
                            ) {
                                Text(
                                    text = cat.name,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = QYellowDark,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (series.isPublished) Color(0xFF1B5E20) else Color(0xFF424242)
                        ) {
                            Text(
                                text = if (series.isPublished) "Published" else "Draft",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Episodes: ${series.episodesCount}  •  Views: ${series.viewsCount}",
                        fontSize = 11.sp,
                        color = QOnSurfaceVariant
                    )
                }
            }

            // Blocked Banner if admin takedown
            if (series.isBlocked) {
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFB71C1C).copy(alpha = 0.2f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFB71C1C)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = Color(0xFFFF5252),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Blocked by admin: ${series.blockedReason ?: "Content flagged"}",
                            fontSize = 11.sp,
                            color = Color(0xFFFF8A80),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onManageEpisodes,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(imageVector = Icons.Default.VideoLibrary, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Episodes", fontSize = 12.sp)
                }

                OutlinedButton(
                    onClick = onEdit,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(imageVector = Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Edit", fontSize = 12.sp)
                }

                IconButton(
                    onClick = onTogglePublish,
                    enabled = !series.isBlocked,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(QSurfaceVariant)
                ) {
                    Icon(
                        imageVector = if (series.isPublished) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = "Toggle Publish",
                        tint = if (series.isBlocked) Color.Gray else if (series.isPublished) QYellowDark else Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color.Red.copy(alpha = 0.15f))
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = Color(0xFFFF5252),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

// -----------------------------------------------------------------------------
// Component: Empty State
// -----------------------------------------------------------------------------
@Composable
private fun EmptyStudioState(onCreateClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Movie,
            contentDescription = null,
            tint = QYellowDark,
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "No Series Created Yet",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = QOnSurface
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Create your first Anime, Movie, or Drama series to share with viewers.",
            fontSize = 13.sp,
            color = QOnSurfaceVariant,
            modifier = Modifier.padding(horizontal = 24.dp)
        )
        Spacer(modifier = Modifier.height(20.dp))
        Button(
            onClick = onCreateClick,
            colors = ButtonDefaults.buttonColors(containerColor = QYellowDark, contentColor = Color.Black),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(imageVector = Icons.Default.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(6.dp))
            Text("Create New Series", fontWeight = FontWeight.Bold)
        }
    }
}
