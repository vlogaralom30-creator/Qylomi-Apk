package com.example.ui.screens.studio

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.*
import com.example.ui.components.ImgBbUrlInputField
import com.example.ui.theme.*
import com.example.ui.viewmodel.CreatorStudioViewModel

private val DEFAULT_GENRES = listOf(
    "Action", "Adventure", "Comedy", "Drama", "Fantasy",
    "Horror", "Isekai", "Romance", "Sci-Fi", "Slice of Life", "Thriller"
)

private val DEFAULT_LANGUAGES = listOf(
    "Hindi", "Japanese", "English", "Bangla", "Tamil", "Telugu"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SeriesFormView(
    existingSeries: Series?,
    categories: List<ContentCategory>,
    eligibleShelves: List<Shelf>,
    currentUser: UserProfile?,
    viewModel: CreatorStudioViewModel,
    onCancel: () -> Unit,
    onSaved: (Series) -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    var title by rememberSaveable { mutableStateOf(existingSeries?.title ?: "") }
    var originalTitle by rememberSaveable { mutableStateOf(existingSeries?.originalTitle ?: "") }
    var description by rememberSaveable { mutableStateOf(existingSeries?.description ?: "") }
    var selectedCategoryId by rememberSaveable { mutableStateOf(existingSeries?.categoryId ?: categories.firstOrNull()?.id ?: "") }
    var selectedGenres by remember { mutableStateOf(existingSeries?.genres?.map { it.replaceFirstChar { ch -> ch.uppercase() } }?.toSet() ?: emptySet()) }
    var customGenreText by rememberSaveable { mutableStateOf("") }
    var selectedLanguages by remember { mutableStateOf(existingSeries?.languages?.map { it.replaceFirstChar { ch -> ch.uppercase() } }?.toSet() ?: setOf("Japanese")) }
    var customLanguageText by rememberSaveable { mutableStateOf("") }
    var subtitleLanguagesText by rememberSaveable { mutableStateOf(existingSeries?.subtitleLanguages?.joinToString(", ") ?: "English") }
    var ratingSlider by rememberSaveable { mutableFloatStateOf(existingSeries?.rating?.toFloat() ?: 8.0f) }
    var ageRating by rememberSaveable { mutableStateOf(existingSeries?.ageRating ?: "13+") }
    var isMature by rememberSaveable { mutableStateOf(existingSeries?.isMature ?: false) }
    var status by rememberSaveable { mutableStateOf(existingSeries?.status ?: "ongoing") }
    var releaseYearText by rememberSaveable { mutableStateOf(existingSeries?.releaseYear?.toString() ?: "2024") }
    var studio by rememberSaveable { mutableStateOf(existingSeries?.studio ?: "") }
    var totalEpisodesText by rememberSaveable { mutableStateOf(existingSeries?.totalEpisodes?.toString() ?: "") }
    var posterUrl by rememberSaveable { mutableStateOf(existingSeries?.posterUrl ?: "") }
    var bannerUrl by rememberSaveable { mutableStateOf(existingSeries?.bannerUrl ?: "") }
    var trailerUrl by rememberSaveable { mutableStateOf(existingSeries?.trailerUrl ?: "") }
    var isPublished by rememberSaveable { mutableStateOf(existingSeries?.isPublished ?: false) }
    var isFeatured by rememberSaveable { mutableStateOf(existingSeries?.isFeatured ?: false) }
    var customSlug by rememberSaveable { mutableStateOf(existingSeries?.slug ?: "") }

    // Manual shelf selections: map of shelfId to position
    var manualShelvesPositions by remember { mutableStateOf(mutableMapOf<String, Int>()) }

    var isCategoryDropdownOpen by remember { mutableStateOf(false) }
    var isStatusDropdownOpen by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = if (existingSeries == null) "Create New Series" else "Edit Series Details",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )

        // 1. Title & Original Title
        OutlinedTextField(
            value = title,
            onValueChange = {
                title = it
                if (existingSeries == null) {
                    customSlug = viewModel.generateSlug(it)
                }
            },
            label = { Text("Title * (1-200 chars)") },
            modifier = Modifier.fillMaxWidth(),
            isError = title.trim().isEmpty()
        )

        OutlinedTextField(
            value = originalTitle,
            onValueChange = { originalTitle = it },
            label = { Text("Original / Native Title (e.g., NARUTO -ナルト-)") },
            modifier = Modifier.fillMaxWidth()
        )

        // Slug (Auto-generated or custom on create)
        OutlinedTextField(
            value = customSlug,
            onValueChange = { if (existingSeries == null) customSlug = it },
            label = { Text("URL Slug (Lowercase, hyphens only)") },
            enabled = existingSeries == null,
            modifier = Modifier.fillMaxWidth()
        )

        // 2. Category Dropdown
        ExposedDropdownMenuBox(
            expanded = isCategoryDropdownOpen,
            onExpandedChange = { isCategoryDropdownOpen = !isCategoryDropdownOpen }
        ) {
            val selectedCatName = categories.find { it.id == selectedCategoryId }?.name ?: "Select Category"
            OutlinedTextField(
                value = selectedCatName,
                onValueChange = {},
                readOnly = true,
                label = { Text("Content Category *") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isCategoryDropdownOpen) },
                modifier = Modifier
                    .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    .fillMaxWidth()
            )
            ExposedDropdownMenu(
                expanded = isCategoryDropdownOpen,
                onDismissRequest = { isCategoryDropdownOpen = false }
            ) {
                categories.forEach { cat ->
                    DropdownMenuItem(
                        text = { Text(cat.name) },
                        onClick = {
                            selectedCategoryId = cat.id
                            isCategoryDropdownOpen = false
                        }
                    )
                }
            }
        }

        // 3. Status Selector
        ExposedDropdownMenuBox(
            expanded = isStatusDropdownOpen,
            onExpandedChange = { isStatusDropdownOpen = !isStatusDropdownOpen }
        ) {
            OutlinedTextField(
                value = status.replaceFirstChar { it.uppercase() },
                onValueChange = {},
                readOnly = true,
                label = { Text("Series Status") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isStatusDropdownOpen) },
                modifier = Modifier
                    .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    .fillMaxWidth()
            )
            ExposedDropdownMenu(
                expanded = isStatusDropdownOpen,
                onDismissRequest = { isStatusDropdownOpen = false }
            ) {
                listOf("ongoing", "completed", "upcoming").forEach { s ->
                    DropdownMenuItem(
                        text = { Text(s.replaceFirstChar { it.uppercase() }) },
                        onClick = {
                            status = s
                            isStatusDropdownOpen = false
                        }
                    )
                }
            }
        }

        // 4. Description
        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            label = { Text("Synopsis / Description") },
            minLines = 3,
            maxLines = 6,
            modifier = Modifier.fillMaxWidth()
        )

        // 5. Genres Multi-select Chips
        Text("Genres", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            DEFAULT_GENRES.forEach { genre ->
                val isSelected = selectedGenres.contains(genre)
                FilterChip(
                    selected = isSelected,
                    onClick = {
                        selectedGenres = if (isSelected) selectedGenres - genre else selectedGenres + genre
                    },
                    label = { Text(genre) },
                    leadingIcon = if (isSelected) {
                        { Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    } else null
                )
            }
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = customGenreText,
                onValueChange = { customGenreText = it },
                label = { Text("Add Custom Genre") },
                modifier = Modifier.weight(1f)
            )
            Button(
                onClick = {
                    if (customGenreText.isNotBlank()) {
                        selectedGenres = selectedGenres + customGenreText.trim()
                        customGenreText = ""
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = QYellowDark, contentColor = Color.Black)
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add")
            }
        }

        // 6. Audio Languages Chips
        Text("Audio Languages", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            DEFAULT_LANGUAGES.forEach { lang ->
                val isSelected = selectedLanguages.contains(lang)
                FilterChip(
                    selected = isSelected,
                    onClick = {
                        selectedLanguages = if (isSelected) selectedLanguages - lang else selectedLanguages + lang
                    },
                    label = { Text(lang) },
                    leadingIcon = if (isSelected) {
                        { Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    } else null
                )
            }
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = customLanguageText,
                onValueChange = { customLanguageText = it },
                label = { Text("Add Audio Language") },
                modifier = Modifier.weight(1f)
            )
            Button(
                onClick = {
                    if (customLanguageText.isNotBlank()) {
                        selectedLanguages = selectedLanguages + customLanguageText.trim()
                        customLanguageText = ""
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = QYellowDark, contentColor = Color.Black)
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add")
            }
        }

        OutlinedTextField(
            value = subtitleLanguagesText,
            onValueChange = { subtitleLanguagesText = it },
            label = { Text("Subtitle Languages (comma separated, e.g. English, Hindi)") },
            modifier = Modifier.fillMaxWidth()
        )

        // 7. Editorial Rating & Age Rating
        Column {
            Text(
                text = "Editorial Rating: ${"%.1f".format(ratingSlider)} / 10",
                fontWeight = FontWeight.Bold,
                color = Color.White,
                fontSize = 14.sp
            )
            Slider(
                value = ratingSlider,
                onValueChange = { ratingSlider = it },
                valueRange = 0.0f..10.0f,
                steps = 99,
                colors = SliderDefaults.colors(thumbColor = QYellowDark, activeTrackColor = QYellowDark)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = ageRating,
                onValueChange = { ageRating = it },
                label = { Text("Age Rating (PG-13, 16+, 18+)") },
                modifier = Modifier.weight(1f)
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(
                    checked = isMature,
                    onCheckedChange = { isMature = it },
                    colors = SwitchDefaults.colors(checkedThumbColor = QYellowDark)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("18+ Mature", fontSize = 13.sp, color = if (isMature) Color(0xFFFF5252) else Color.White)
            }
        }

        // 8. Studio, Release Year & Total Episodes
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = releaseYearText,
                onValueChange = { releaseYearText = it.filter { ch -> ch.isDigit() } },
                label = { Text("Release Year") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = totalEpisodesText,
                onValueChange = { totalEpisodesText = it.filter { ch -> ch.isDigit() } },
                label = { Text("Total Episodes") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f)
            )
        }

        OutlinedTextField(
            value = studio,
            onValueChange = { studio = it },
            label = { Text("Animation Studio / Production Company") },
            modifier = Modifier.fillMaxWidth()
        )

        // 9. Media URLs (Poster 2:3 & Banner 16:9)
        ImgBbUrlInputField(
            value = posterUrl,
            onValueChange = { posterUrl = it },
            label = "Poster URL (2:3 tall aspect ratio)",
            previewAspectRatio = 2f / 3f,
            testTag = "input_poster_url"
        )

        ImgBbUrlInputField(
            value = bannerUrl,
            onValueChange = { bannerUrl = it },
            label = "Banner URL (16:9 wide aspect ratio)",
            previewAspectRatio = 16f / 9f,
            testTag = "input_banner_url"
        )

        OutlinedTextField(
            value = trailerUrl,
            onValueChange = { trailerUrl = it },
            label = { Text("Trailer URL (YouTube / Video)") },
            modifier = Modifier.fillMaxWidth()
        )

        // 10. Manual Shelves Linkage
        Text("Show in Shelves", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
        if (eligibleShelves.isEmpty()) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = QSurfaceVariant,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "No manual shelves yet. Ask an admin to create one in Admin Studio.",
                    fontSize = 12.sp,
                    color = QOnSurfaceVariant,
                    modifier = Modifier.padding(12.dp)
                )
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                eligibleShelves.forEach { shelf ->
                    val isChecked = manualShelvesPositions.containsKey(shelf.id)
                    var posText by remember { mutableStateOf("${manualShelvesPositions[shelf.id] ?: shelf.position}") }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Checkbox(
                            checked = isChecked,
                            onCheckedChange = { checked ->
                                val map = manualShelvesPositions.toMutableMap()
                                if (checked) {
                                    map[shelf.id] = posText.toIntOrNull() ?: 0
                                } else {
                                    map.remove(shelf.id)
                                }
                                manualShelvesPositions = map
                            }
                        )
                        Text(
                            text = shelf.title,
                            fontSize = 13.sp,
                            color = Color.White,
                            modifier = Modifier.weight(1f)
                        )
                        if (isChecked) {
                            OutlinedTextField(
                                value = posText,
                                onValueChange = {
                                    posText = it.filter { ch -> ch.isDigit() }
                                    val map = manualShelvesPositions.toMutableMap()
                                    map[shelf.id] = posText.toIntOrNull() ?: 0
                                    manualShelvesPositions = map
                                },
                                label = { Text("Pos") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.width(70.dp)
                            )
                        }
                    }
                }
            }
        }

        // 11. Published & Admin Featured Switches
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text("Publish Series", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                Text(
                    if (isPublished) "Publicly visible on Home & Catalog" else "Draft mode (only you can see)",
                    fontSize = 11.sp,
                    color = QOnSurfaceVariant
                )
            }
            Switch(
                checked = isPublished,
                onCheckedChange = { isPublished = it },
                colors = SwitchDefaults.colors(checkedThumbColor = QYellowDark)
            )
        }

        if (currentUser?.isAdmin == true) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Featured Series (Admin Only)", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                    Text("Eligible for Hero spotlight and high priority promotion", fontSize = 11.sp, color = QOnSurfaceVariant)
                }
                Switch(
                    checked = isFeatured,
                    onCheckedChange = { isFeatured = it },
                    colors = SwitchDefaults.colors(checkedThumbColor = QYellowDark)
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Actions: Save / Cancel
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Cancel")
            }

            Button(
                onClick = {
                    if (title.trim().isEmpty()) {
                        Toast.makeText(context, "Title is required", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    val finalSlug = customSlug.ifBlank { viewModel.generateSlug(title) }
                    val cleanGenres = selectedGenres.map { it.trim().lowercase() }.toList()
                    val cleanLanguages = selectedLanguages.map { it.trim().lowercase() }.toList()
                    val cleanSubs = subtitleLanguagesText.split(",").map { it.trim().lowercase() }.filter { it.isNotBlank() }
                    val year = releaseYearText.toIntOrNull()
                    val totalEps = totalEpisodesText.toIntOrNull()
                    val catId = selectedCategoryId.ifBlank { null }

                    val manualShelvesList = manualShelvesPositions.map { (shelfId, pos) -> Pair(shelfId, pos) }

                    if (existingSeries == null) {
                        val uid = currentUser?.id ?: ""
                        val insert = SeriesInsert(
                            title = title.trim(),
                            originalTitle = originalTitle.trim().ifBlank { null },
                            description = description.trim().ifBlank { null },
                            slug = finalSlug,
                            categoryId = catId,
                            genres = cleanGenres,
                            languages = cleanLanguages,
                            subtitleLanguages = cleanSubs,
                            rating = (Math.round(ratingSlider * 10.0) / 10.0),
                            ageRating = ageRating.trim().ifBlank { null },
                            isMature = isMature,
                            status = status,
                            releaseYear = year,
                            studio = studio.trim().ifBlank { null },
                            totalEpisodes = totalEps,
                            posterUrl = posterUrl.trim().ifBlank { null },
                            bannerUrl = bannerUrl.trim().ifBlank { null },
                            trailerUrl = trailerUrl.trim().ifBlank { null },
                            isPublished = isPublished,
                            isFeatured = isFeatured,
                            createdBy = uid
                        )
                        viewModel.saveSeries(insert = insert, update = null, seriesId = null, selectedManualShelves = manualShelvesList) { success, msg ->
                            Toast.makeText(context, msg ?: "", Toast.LENGTH_SHORT).show()
                            if (success) onCancel()
                        }
                    } else {
                        val update = SeriesUpdate(
                            title = title.trim(),
                            originalTitle = originalTitle.trim().ifBlank { null },
                            description = description.trim().ifBlank { null },
                            slug = finalSlug,
                            categoryId = catId,
                            genres = cleanGenres,
                            languages = cleanLanguages,
                            subtitleLanguages = cleanSubs,
                            rating = (Math.round(ratingSlider * 10.0) / 10.0),
                            ageRating = ageRating.trim().ifBlank { null },
                            isMature = isMature,
                            status = status,
                            releaseYear = year,
                            studio = studio.trim().ifBlank { null },
                            totalEpisodes = totalEps,
                            posterUrl = posterUrl.trim().ifBlank { null },
                            bannerUrl = bannerUrl.trim().ifBlank { null },
                            trailerUrl = trailerUrl.trim().ifBlank { null },
                            isPublished = isPublished,
                            isFeatured = isFeatured
                        )
                        viewModel.saveSeries(insert = null, update = update, seriesId = existingSeries.id, selectedManualShelves = manualShelvesList) { success, msg ->
                            Toast.makeText(context, msg ?: "", Toast.LENGTH_SHORT).show()
                            if (success) onCancel()
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = QYellowDark, contentColor = Color.Black),
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Save Series", fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}
