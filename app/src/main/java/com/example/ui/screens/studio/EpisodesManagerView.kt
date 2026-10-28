package com.example.ui.screens.studio

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.*
import com.example.ui.components.EpisodeThumbnail
import com.example.ui.components.ImgBbUrlInputField
import com.example.ui.theme.*
import com.example.ui.viewmodel.CreatorStudioViewModel

@Composable
fun EpisodesManagerView(
    series: Series,
    seasons: List<Season>,
    episodes: List<Episode>,
    onAddSeason: () -> Unit,
    onBulkAdd: () -> Unit,
    onAddEpisode: () -> Unit,
    onEditEpisode: (Episode) -> Unit,
    onDeleteEpisode: (Episode) -> Unit,
    onDeleteSeason: (String) -> Unit
) {
    var selectedSeasonNumber by rememberSaveable { mutableIntStateOf(1) }

    val distinctSeasonNumbers = remember(episodes, seasons) {
        val fromSeasons = seasons.map { it.seasonNumber }
        val fromEpisodes = episodes.map { it.seasonNumber }
        (fromSeasons + fromEpisodes + listOf(1)).distinct().sorted()
    }

    val currentSeasonEpisodes = remember(episodes, selectedSeasonNumber) {
        episodes.filter { it.seasonNumber == selectedSeasonNumber }.sortedBy { it.episodeNumber }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Top Toolbar: Seasons selector & Add Season / Bulk Add
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(QSurface)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Seasons & Episodes",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = QOnSurface
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = onAddSeason,
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("+ Season", fontSize = 12.sp)
                    }

                    Button(
                        onClick = onBulkAdd,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = QYellowDark, contentColor = Color.Black),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Icon(imageVector = Icons.Default.LibraryAdd, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Bulk Add", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Season Tabs
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                distinctSeasonNumbers.forEach { sNum ->
                    val isSelected = sNum == selectedSeasonNumber
                    val seasonMeta = seasons.find { it.seasonNumber == sNum }
                    val label = seasonMeta?.title ?: "Season $sNum"

                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedSeasonNumber = sNum },
                        label = { Text(label) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = QYellowDark,
                            selectedLabelColor = Color.Black
                        )
                    )
                }
            }
        }

        // Episodes List
        if (currentSeasonEpisodes.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.VideoLibrary,
                        contentDescription = null,
                        tint = QOnSurfaceVariant,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "No episodes in Season $selectedSeasonNumber yet",
                        color = QOnSurfaceVariant,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = onAddEpisode,
                        colors = ButtonDefaults.buttonColors(containerColor = QYellowDark, contentColor = Color.Black)
                    ) {
                        Text("Add Episode $selectedSeasonNumber.1")
                    }
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(currentSeasonEpisodes, key = { it.id }) { ep ->
                    EpisodeRowCard(
                        episode = ep,
                        series = series,
                        onEdit = { onEditEpisode(ep) },
                        onDelete = { onDeleteEpisode(ep) }
                    )
                }
            }
        }

        // Bottom Add Episode Action Bar
        Surface(
            color = QSurface,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${currentSeasonEpisodes.size} episode(s) in this season",
                    fontSize = 13.sp,
                    color = QOnSurfaceVariant
                )
                Button(
                    onClick = onAddEpisode,
                    colors = ButtonDefaults.buttonColors(containerColor = QYellowDark, contentColor = Color.Black),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Add Episode", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun EpisodeRowCard(
    episode: Episode,
    series: Series,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = QSurface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Episode Thumbnail with fallback chain
            EpisodeThumbnail(
                episode = episode,
                series = series,
                modifier = Modifier.width(88.dp),
                showBadges = true
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "E${episode.episodeNumber}: ${episode.title ?: "Untitled Episode"}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = QOnSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = if (episode.durationSeconds != null) "${episode.durationSeconds / 60}m ${episode.durationSeconds % 60}s" else "No duration",
                        fontSize = 11.sp,
                        color = QOnSurfaceVariant
                    )
                    Text(
                        text = if (episode.isPublished) "• Published" else "• Draft",
                        fontSize = 11.sp,
                        color = if (episode.isPublished) Color(0xFF81C784) else Color.Gray,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            IconButton(onClick = onEdit) {
                Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit", tint = Color.White, modifier = Modifier.size(18.dp))
            }

            IconButton(onClick = onDelete) {
                Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = Color(0xFFFF5252), modifier = Modifier.size(18.dp))
            }
        }
    }
}

// -----------------------------------------------------------------------------
// Component: Episode & Video Sources Form View
// -----------------------------------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EpisodeFormView(
    series: Series,
    existingEpisode: Episode?,
    seasons: List<Season>,
    existingEpisodes: List<Episode>,
    viewModel: CreatorStudioViewModel,
    onDone: () -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    // Next suggested episode number
    val nextFreeNumber = remember(existingEpisodes, existingEpisode) {
        if (existingEpisode != null) existingEpisode.episodeNumber
        else (existingEpisodes.maxOfOrNull { it.episodeNumber } ?: 0) + 1
    }

    var seasonNumber by rememberSaveable { mutableIntStateOf(existingEpisode?.seasonNumber ?: 1) }
    var episodeNumberText by rememberSaveable { mutableStateOf("${existingEpisode?.episodeNumber ?: nextFreeNumber}") }
    var title by rememberSaveable { mutableStateOf(existingEpisode?.title ?: "") }
    var description by rememberSaveable { mutableStateOf(existingEpisode?.description ?: "") }
    var thumbnailUrl by rememberSaveable { mutableStateOf(existingEpisode?.thumbnailUrl ?: "") }
    var durationSecondsText by rememberSaveable { mutableStateOf(existingEpisode?.durationSeconds?.toString() ?: "") }
    var airDate by rememberSaveable { mutableStateOf(existingEpisode?.airDate ?: "") }
    var introStartText by rememberSaveable { mutableStateOf(existingEpisode?.introStartSec?.toString() ?: "") }
    var introEndText by rememberSaveable { mutableStateOf(existingEpisode?.introEndSec?.toString() ?: "") }
    var isPublished by rememberSaveable { mutableStateOf(existingEpisode?.isPublished ?: true) }

    val episodeSources by viewModel.episodeSources.collectAsState()

    // Editing source state
    var showSourceDialog by remember { mutableStateOf(false) }
    var sourceToEdit by remember { mutableStateOf<EpisodeSource?>(null) }

    // Enforce UI Rule: An episode needs at least 1 active source to be published
    val hasActiveSource = remember(episodeSources) {
        episodeSources.any { it.isActive }
    }

    LaunchedEffect(existingEpisode) {
        viewModel.selectEpisode(existingEpisode)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = if (existingEpisode == null) "New Episode" else "Edit Episode Details",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = QOnSurface
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = "$seasonNumber",
                onValueChange = { seasonNumber = it.toIntOrNull() ?: 1 },
                label = { Text("Season #") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f)
            )

            OutlinedTextField(
                value = episodeNumberText,
                onValueChange = { episodeNumberText = it.filter { ch -> ch.isDigit() } },
                label = { Text("Episode # *") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f)
            )
        }

        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text("Episode Title (e.g., Homecoming)") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            label = { Text("Episode Description") },
            minLines = 2,
            maxLines = 4,
            modifier = Modifier.fillMaxWidth()
        )

        ImgBbUrlInputField(
            value = thumbnailUrl,
            onValueChange = { thumbnailUrl = it },
            label = "Thumbnail URL (16:9)",
            previewAspectRatio = 16f / 9f,
            testTag = "input_episode_thumbnail_url"
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val seriesPoster = series.posterUrl ?: series.bannerUrl
            if (!seriesPoster.isNullOrBlank()) {
                OutlinedButton(
                    onClick = { thumbnailUrl = seriesPoster },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Use series poster", fontSize = 12.sp)
                }
            }
            if (thumbnailUrl.isNotBlank()) {
                OutlinedButton(
                    onClick = { thumbnailUrl = "" },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Clear (Use fallback)", fontSize = 12.sp)
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = durationSecondsText,
                onValueChange = { durationSecondsText = it.filter { ch -> ch.isDigit() } },
                label = { Text("Duration (seconds)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f)
            )

            OutlinedTextField(
                value = airDate,
                onValueChange = { airDate = it },
                label = { Text("Air Date (YYYY-MM-DD)") },
                modifier = Modifier.weight(1f)
            )
        }

        // Skip Intro Timings
        Text("Skip Intro Timestamps (Optional)", fontWeight = FontWeight.Bold, color = QOnSurface, fontSize = 14.sp)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = introStartText,
                onValueChange = { introStartText = it.filter { ch -> ch.isDigit() } },
                label = { Text("Intro Start (sec)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f)
            )

            OutlinedTextField(
                value = introEndText,
                onValueChange = { introEndText = it.filter { ch -> ch.isDigit() } },
                label = { Text("Intro End (sec)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f)
            )
        }

        // Published switch
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text("Publish Episode", fontWeight = FontWeight.Bold, color = QOnSurface, fontSize = 14.sp)
                Text(
                    if (!hasActiveSource && existingEpisode != null) "Requires at least 1 active video source" else "Available to viewers",
                    fontSize = 11.sp,
                    color = if (!hasActiveSource && existingEpisode != null) Color(0xFFFF8A80) else QOnSurfaceVariant
                )
            }
            Switch(
                checked = isPublished,
                enabled = hasActiveSource || existingEpisode == null,
                onCheckedChange = { isPublished = it },
                colors = SwitchDefaults.colors(checkedThumbColor = QYellowDark)
            )
        }

        Divider(color = QSurfaceVariant)

        // =====================================================================
        // Sources Editor Section
        // =====================================================================
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Video Sources / Servers", fontWeight = FontWeight.Bold, color = QOnSurface, fontSize = 16.sp)
                Text("Ordered by priority (lower number plays first)", fontSize = 11.sp, color = QOnSurfaceVariant)
            }
            Button(
                onClick = {
                    if (existingEpisode == null) {
                        Toast.makeText(context, "Please save the episode first to add sources", Toast.LENGTH_SHORT).show()
                    } else {
                        sourceToEdit = null
                        showSourceDialog = true
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = QYellowDark, contentColor = Color.Black),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add Source", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }

        if (existingEpisode == null) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = QSurfaceVariant,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Save this episode first to attach video sources.",
                    fontSize = 12.sp,
                    color = QOnSurfaceVariant,
                    modifier = Modifier.padding(12.dp)
                )
            }
        } else if (episodeSources.isEmpty()) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = QSurfaceVariant,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "No sources added yet. Add at least one active video source before publishing.",
                    fontSize = 12.sp,
                    color = Color(0xFFFF8A80),
                    modifier = Modifier.padding(12.dp)
                )
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                episodeSources.forEachIndexed { index, source ->
                    SourceItemRow(
                        source = source,
                        isFirst = index == 0,
                        isLast = index == episodeSources.size - 1,
                        onMoveUp = {
                            if (index > 0) {
                                viewModel.swapSourcePriority(source, episodeSources[index - 1], existingEpisode.id)
                            }
                        },
                        onMoveDown = {
                            if (index < episodeSources.size - 1) {
                                viewModel.swapSourcePriority(source, episodeSources[index + 1], existingEpisode.id)
                            }
                        },
                        onEdit = {
                            sourceToEdit = source
                            showSourceDialog = true
                        },
                        onToggleActive = { active ->
                            viewModel.saveSource(
                                sourceId = source.id,
                                insert = null,
                                update = SourceUpdate(isActive = active),
                                episodeId = existingEpisode.id
                            ) { success, msg ->
                                Toast.makeText(context, msg ?: "", Toast.LENGTH_SHORT).show()
                            }
                        },
                        onDelete = {
                            viewModel.deleteSource(source.id, existingEpisode.id) { success, msg ->
                                Toast.makeText(context, msg ?: "", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Save / Cancel Episode Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onDone,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Cancel")
            }

            Button(
                onClick = {
                    val epNum = episodeNumberText.toIntOrNull()
                    if (epNum == null) {
                        Toast.makeText(context, "Valid episode number required", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    val startSec = introStartText.toIntOrNull()
                    val endSec = introEndText.toIntOrNull()
                    if (startSec != null && endSec != null && endSec <= startSec) {
                        Toast.makeText(context, "Intro end must be greater than intro start", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    val dur = durationSecondsText.toIntOrNull()

                    if (existingEpisode == null) {
                        val uid = series.createdBy ?: ""
                        val insert = EpisodeInsert(
                            seriesId = series.id,
                            seasonNumber = seasonNumber,
                            episodeNumber = epNum,
                            title = title.trim().ifBlank { null },
                            description = description.trim().ifBlank { null },
                            thumbnailUrl = thumbnailUrl.trim().ifBlank { null },
                            durationSeconds = dur,
                            airDate = airDate.trim().ifBlank { null },
                            introStartSec = startSec,
                            introEndSec = endSec,
                            isPublished = isPublished,
                            createdBy = uid
                        )
                        viewModel.saveEpisode(insert, null, null, series.id, hasActiveSource) { success, msg, created ->
                            Toast.makeText(context, msg ?: "", Toast.LENGTH_SHORT).show()
                            if (success && created != null) {
                                viewModel.selectEpisode(created)
                            }
                        }
                    } else {
                        val update = EpisodeUpdate(
                            seasonNumber = seasonNumber,
                            episodeNumber = epNum,
                            title = title.trim().ifBlank { null },
                            description = description.trim().ifBlank { null },
                            thumbnailUrl = thumbnailUrl.trim().ifBlank { null },
                            durationSeconds = dur,
                            airDate = airDate.trim().ifBlank { null },
                            introStartSec = startSec,
                            introEndSec = endSec,
                            isPublished = if (hasActiveSource) isPublished else false
                        )
                        viewModel.saveEpisode(null, update, existingEpisode.id, series.id, hasActiveSource) { success, msg, _ ->
                            Toast.makeText(context, msg ?: "", Toast.LENGTH_SHORT).show()
                            if (success) onDone()
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = QYellowDark, contentColor = Color.Black),
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Save Episode", fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }

    // Modal Dialog: Add / Edit Video Source
    if (showSourceDialog && existingEpisode != null) {
        SourceEditorDialog(
            existingSource = sourceToEdit,
            episodeId = existingEpisode.id,
            viewModel = viewModel,
            onDismiss = { showSourceDialog = false },
            onSaved = { showSourceDialog = false }
        )
    }
}

// -----------------------------------------------------------------------------
// Component: Source Row Item
// -----------------------------------------------------------------------------
@Composable
private fun SourceItemRow(
    source: EpisodeSource,
    isFirst: Boolean,
    isLast: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onEdit: () -> Unit,
    onToggleActive: (Boolean) -> Unit,
    onDelete: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = QSurface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Text(
                        text = source.label,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = QOnSurface
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    source.platform?.let { plat ->
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = QYellowDark.copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = plat.uppercase(),
                                fontSize = 10.sp,
                                color = QYellowDark,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    source.quality?.let { q ->
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = QSurfaceVariant
                        ) {
                            Text(
                                text = q,
                                fontSize = 10.sp,
                                color = QOnSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                // Priority Up / Down
                Row {
                    IconButton(onClick = onMoveUp, enabled = !isFirst, modifier = Modifier.size(28.dp)) {
                        Icon(imageVector = Icons.Default.KeyboardArrowUp, contentDescription = "Move Up", tint = if (isFirst) Color.Gray else Color.White)
                    }
                    IconButton(onClick = onMoveDown, enabled = !isLast, modifier = Modifier.size(28.dp)) {
                        Icon(imageVector = Icons.Default.KeyboardArrowDown, contentDescription = "Move Down", tint = if (isLast) Color.Gray else Color.White)
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = source.url,
                fontSize = 11.sp,
                color = QOnSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // Warning chip for fail_count
            if (source.failCount > 0) {
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = Color(0xFFE65100).copy(alpha = 0.2f)
                ) {
                    Text(
                        text = "Fail count: ${source.failCount} reports",
                        fontSize = 10.sp,
                        color = Color(0xFFFFB74D),
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(
                        checked = source.isActive,
                        onCheckedChange = onToggleActive,
                        colors = SwitchDefaults.colors(checkedThumbColor = QYellowDark),
                        modifier = Modifier.height(24.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (source.isActive) "Active" else "Disabled",
                        fontSize = 12.sp,
                        color = if (source.isActive) Color.White else Color.Gray
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = onEdit) {
                        Text("Edit", fontSize = 12.sp, color = QYellowDark)
                    }
                    TextButton(onClick = onDelete) {
                        Text("Delete", fontSize = 12.sp, color = Color(0xFFFF5252))
                    }
                }
            }
        }
    }
}

// -----------------------------------------------------------------------------
// Component: Source Editor & Tester Dialog
// -----------------------------------------------------------------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SourceEditorDialog(
    existingSource: EpisodeSource?,
    episodeId: String,
    viewModel: CreatorStudioViewModel,
    onDismiss: () -> Unit,
    onSaved: () -> Unit
) {
    val context = LocalContext.current

    var label by remember { mutableStateOf(existingSource?.label ?: "Server 1") }
    var url by remember { mutableStateOf(existingSource?.url ?: "") }
    var language by remember { mutableStateOf(existingSource?.language ?: "Hindi Dub") }
    var quality by remember { mutableStateOf(existingSource?.quality ?: "Auto") }
    var platform by remember { mutableStateOf(existingSource?.platform ?: "direct") }
    var isActive by remember { mutableStateOf(existingSource?.isActive ?: true) }

    var isTesting by remember { mutableStateOf(false) }
    var testResultPlatform by remember { mutableStateOf<String?>(null) }
    var testResultDuration by remember { mutableStateOf<Int?>(null) }
    var isQualityDropdownOpen by remember { mutableStateOf(false) }
    var showDoodManager by remember { mutableStateOf(false) }

    if (showDoodManager) {
        com.example.ui.components.DoodStreamManagerDialog(
            onSelectEmbedUrl = { selectedUrl ->
                url = selectedUrl
                platform = "doodstream"
                label = "DoodStream Server"
            },
            onDismiss = { showDoodManager = false }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existingSource == null) "Add Video Source" else "Edit Source") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text("Server Label (e.g., Server 1 - Cloudflare)") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text("Stream / Video URL *") },
                    modifier = Modifier.fillMaxWidth()
                )

                // DoodStream Manager & Uploader Button
                Button(
                    onClick = { showDoodManager = true },
                    colors = ButtonDefaults.buttonColors(containerColor = QYellowDark, contentColor = Color.Black),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(imageVector = Icons.Default.Cloud, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Open DoodStream Manager & Uploader", fontWeight = FontWeight.Bold)
                }

                // Test URL Button & Platform Detector
                Button(
                    onClick = {
                        if (!url.trim().startsWith("http://") && !url.trim().startsWith("https://")) {
                            Toast.makeText(context, "URL must start with http:// or https://", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        isTesting = true
                        viewModel.testVideoUrl(url) { detectedPlat, dur, _, _ ->
                            isTesting = false
                            platform = detectedPlat
                            testResultPlatform = detectedPlat
                            testResultDuration = dur
                            Toast.makeText(context, "Detected platform: $detectedPlat", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = QSurfaceVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isTesting) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = QYellowDark)
                    } else {
                        Icon(imageVector = Icons.Default.PlayCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Test URL & Detect Platform")
                    }
                }

                if (testResultPlatform != null) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = QYellowDark.copy(alpha = 0.15f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = "Platform: ${testResultPlatform?.uppercase()}",
                                fontWeight = FontWeight.Bold,
                                color = QYellowDark,
                                fontSize = 12.sp
                            )
                            testResultDuration?.let { dur ->
                                Text("Duration: ${dur / 60}m ${dur % 60}s", fontSize = 11.sp, color = QOnSurfaceVariant)
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = language,
                    onValueChange = { language = it },
                    label = { Text("Language (e.g. Hindi Dub, English, Japanese Sub)") },
                    modifier = Modifier.fillMaxWidth()
                )

                // Quality Dropdown
                ExposedDropdownMenuBox(
                    expanded = isQualityDropdownOpen,
                    onExpandedChange = { isQualityDropdownOpen = !isQualityDropdownOpen }
                ) {
                    OutlinedTextField(
                        value = quality,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Quality") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isQualityDropdownOpen) },
                        modifier = Modifier
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = isQualityDropdownOpen,
                        onDismissRequest = { isQualityDropdownOpen = false }
                    ) {
                        listOf("Auto", "1080p", "720p", "480p", "360p").forEach { q ->
                            DropdownMenuItem(
                                text = { Text(q) },
                                onClick = {
                                    quality = q
                                    isQualityDropdownOpen = false
                                }
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Active Source", fontSize = 13.sp, color = QOnSurface)
                    Switch(
                        checked = isActive,
                        onCheckedChange = { isActive = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = QYellowDark)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val trimmedUrl = url.trim()
                    if (!trimmedUrl.startsWith("http://") && !trimmedUrl.startsWith("https://")) {
                        Toast.makeText(context, "URL must start with http:// or https://", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    if (existingSource == null) {
                        val insert = SourceInsert(
                            episodeId = episodeId,
                            label = label.trim().ifBlank { "Server 1" },
                            url = trimmedUrl,
                            platform = platform,
                            language = language.trim().ifBlank { null },
                            quality = quality,
                            isActive = isActive
                        )
                        viewModel.saveSource(null, insert, null, episodeId) { success, msg ->
                            Toast.makeText(context, msg ?: "", Toast.LENGTH_SHORT).show()
                            if (success) onSaved()
                        }
                    } else {
                        val update = SourceUpdate(
                            label = label.trim().ifBlank { "Server 1" },
                            url = trimmedUrl,
                            platform = platform,
                            language = language.trim().ifBlank { null },
                            quality = quality,
                            isActive = isActive
                        )
                        viewModel.saveSource(existingSource.id, null, update, episodeId) { success, msg ->
                            Toast.makeText(context, msg ?: "", Toast.LENGTH_SHORT).show()
                            if (success) onSaved()
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = QYellowDark, contentColor = Color.Black)
            ) {
                Text("Save Source")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
        containerColor = QSurface,
        titleContentColor = Color.White,
        textContentColor = QOnSurfaceVariant
    )
}
