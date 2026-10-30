package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.*
import com.example.data.repository.SeriesRepository
import com.example.video.VideoPlatformRegistry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class CreatorStudioViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = SeriesRepository(application)

    private val _mySeries = MutableStateFlow<List<Series>>(emptyList())
    val mySeries: StateFlow<List<Series>> = _mySeries.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _activeCategories = MutableStateFlow<List<ContentCategory>>(emptyList())
    val activeCategories: StateFlow<List<ContentCategory>> = _activeCategories.asStateFlow()

    private val _eligibleShelves = MutableStateFlow<List<Shelf>>(emptyList())
    val eligibleShelves: StateFlow<List<Shelf>> = _eligibleShelves.asStateFlow()

    private val _selectedSeries = MutableStateFlow<Series?>(null)
    val selectedSeries: StateFlow<Series?> = _selectedSeries.asStateFlow()

    private val _seriesSeasons = MutableStateFlow<List<Season>>(emptyList())
    val seriesSeasons: StateFlow<List<Season>> = _seriesSeasons.asStateFlow()

    private val _seriesEpisodes = MutableStateFlow<List<Episode>>(emptyList())
    val seriesEpisodes: StateFlow<List<Episode>> = _seriesEpisodes.asStateFlow()

    private val _selectedEpisode = MutableStateFlow<Episode?>(null)
    val selectedEpisode: StateFlow<Episode?> = _selectedEpisode.asStateFlow()

    private val _episodeSources = MutableStateFlow<List<EpisodeSource>>(emptyList())
    val episodeSources: StateFlow<List<EpisodeSource>> = _episodeSources.asStateFlow()

    private val _seriesShelfItems = MutableStateFlow<List<ShelfItem>>(emptyList())
    val seriesShelfItems: StateFlow<List<ShelfItem>> = _seriesShelfItems.asStateFlow()

    private val _bannerMessage = MutableStateFlow<String?>(null)
    val bannerMessage: StateFlow<String?> = _bannerMessage.asStateFlow()

    fun setBanner(msg: String?) {
        _bannerMessage.value = msg
    }

    fun generateSlug(input: String): String = SeriesRepository.generateSlug(input)

    init {
        loadInitialData()
    }

    fun loadInitialData() {
        viewModelScope.launch {
            _isLoading.value = true
            loadMySeriesInternal()
            loadCategoriesAndShelvesInternal()
            _isLoading.value = false
        }
    }

    fun refreshMySeries() {
        viewModelScope.launch {
            _isLoading.value = true
            loadMySeriesInternal()
            _isLoading.value = false
        }
    }

    private suspend fun loadMySeriesInternal() {
        val result = repository.listMySeries()
        if (result.isSuccess) {
            _mySeries.value = result.getOrDefault(emptyList())
        } else {
            _bannerMessage.value = (result as? SeriesResult.Error)?.message ?: "Failed to load series"
        }
    }

    private suspend fun loadCategoriesAndShelvesInternal() {
        val catResult = repository.getCategories(activeOnly = true)
        if (catResult.isSuccess) {
            _activeCategories.value = catResult.getOrDefault(emptyList())
        }

        val shelfResult = repository.listMyEligibleShelves()
        if (shelfResult.isSuccess) {
            _eligibleShelves.value = shelfResult.getOrDefault(emptyList())
        }
    }

    fun selectSeries(series: Series?) {
        _selectedSeries.value = series
        _selectedEpisode.value = null
        _episodeSources.value = emptyList()
        if (series != null && series.id.isNotBlank()) {
            loadSeriesContent(series.id)
        } else {
            _seriesSeasons.value = emptyList()
            _seriesEpisodes.value = emptyList()
        }
    }

    fun loadSeriesContent(seriesId: String) {
        viewModelScope.launch {
            val seasonsRes = repository.getSeasons(seriesId)
            if (seasonsRes.isSuccess) {
                _seriesSeasons.value = seasonsRes.getOrDefault(emptyList())
            }

            // Creators see both published and draft episodes
            val episodesRes = repository.getEpisodes(seriesId, publishedOnly = false)
            if (episodesRes.isSuccess) {
                _seriesEpisodes.value = episodesRes.getOrDefault(emptyList())
            }
        }
    }

    fun saveSeries(
        insert: SeriesInsert?,
        update: SeriesUpdate?,
        seriesId: String?,
        selectedManualShelves: List<Pair<String, Int>>,
        onDone: (Boolean, String?) -> Unit
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            if (seriesId.isNullOrBlank()) {
                if (insert == null) {
                    _isLoading.value = false
                    onDone(false, "Invalid series data")
                    return@launch
                }
                val result = repository.createSeriesWithAutoSlug(insert)
                if (result.isSuccess) {
                    val created = result.getOrNull()
                    if (created != null) {
                        saveManualShelves(created.id, selectedManualShelves)
                        loadMySeriesInternal()
                        selectSeries(created)
                        _isLoading.value = false
                        onDone(true, "Series '${created.title}' created successfully!")
                    } else {
                        _isLoading.value = false
                        onDone(false, "Failed to create series")
                    }
                } else {
                    _isLoading.value = false
                    val error = (result as? SeriesResult.Error)?.message ?: "Failed to create series"
                    onDone(false, error)
                }
            } else {
                if (update == null) {
                    _isLoading.value = false
                    onDone(false, "Invalid update data")
                    return@launch
                }
                val result = repository.updateSeries(seriesId, update)
                if (result.isSuccess) {
                    val updated = result.getOrNull()
                    saveManualShelves(seriesId, selectedManualShelves)
                    loadMySeriesInternal()
                    if (updated != null) {
                        _selectedSeries.value = updated
                    }
                    _isLoading.value = false
                    onDone(true, "Series updated successfully!")
                } else {
                    _isLoading.value = false
                    val error = (result as? SeriesResult.Error)?.message ?: "Failed to update series"
                    onDone(false, error)
                }
            }
        }
    }

    private suspend fun saveManualShelves(seriesId: String, selectedShelves: List<Pair<String, Int>>) {
        // Link to each selected manual shelf
        for ((shelfId, position) in selectedShelves) {
            repository.setShelfItem(shelfId, seriesId, position)
        }
    }

    fun togglePublishSeries(series: Series, onDone: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            if (series.isBlocked) {
                onDone(false, "This series was blocked by admin: ${series.blockedReason ?: "Takedown"}")
                return@launch
            }
            val newPublished = !series.isPublished
            val result = repository.updateSeries(series.id, SeriesUpdate(isPublished = newPublished))
            if (result.isSuccess) {
                loadMySeriesInternal()
                onDone(true, if (newPublished) "Series published!" else "Series moved to drafts")
            } else {
                onDone(false, (result as? SeriesResult.Error)?.message ?: "Failed to update publish state")
            }
        }
    }

    fun deleteSeries(seriesId: String, onDone: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            val result = repository.deleteSeries(seriesId)
            if (result.isSuccess) {
                loadMySeriesInternal()
                if (_selectedSeries.value?.id == seriesId) {
                    _selectedSeries.value = null
                }
                onDone(true, "Series deleted successfully")
            } else {
                onDone(false, (result as? SeriesResult.Error)?.message ?: "Failed to delete series")
            }
        }
    }

    // -------------------------------------------------------------------------
    // Seasons
    // -------------------------------------------------------------------------
    fun saveSeason(
        seriesId: String,
        seasonNumber: Int,
        title: String?,
        posterUrl: String?,
        seasonId: String? = null,
        onDone: (Boolean, String?) -> Unit
    ) {
        viewModelScope.launch {
            if (seasonId.isNullOrBlank()) {
                val insert = SeasonInsert(
                    seriesId = seriesId,
                    seasonNumber = seasonNumber,
                    title = title,
                    posterUrl = posterUrl
                )
                val res = repository.createSeason(insert)
                if (res.isSuccess) {
                    loadSeriesContent(seriesId)
                    onDone(true, "Season $seasonNumber added")
                } else {
                    onDone(false, (res as? SeriesResult.Error)?.message ?: "Failed to add season")
                }
            } else {
                val update = SeasonUpdate(
                    seasonNumber = seasonNumber,
                    title = title,
                    posterUrl = posterUrl
                )
                val res = repository.updateSeason(seasonId, update)
                if (res.isSuccess) {
                    loadSeriesContent(seriesId)
                    onDone(true, "Season updated")
                } else {
                    onDone(false, (res as? SeriesResult.Error)?.message ?: "Failed to update season")
                }
            }
        }
    }

    fun deleteSeason(seasonId: String, seriesId: String, onDone: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            val res = repository.deleteSeason(seasonId)
            if (res.isSuccess) {
                loadSeriesContent(seriesId)
                onDone(true, "Season deleted")
            } else {
                onDone(false, (res as? SeriesResult.Error)?.message ?: "Failed to delete season")
            }
        }
    }

    // -------------------------------------------------------------------------
    // Episodes
    // -------------------------------------------------------------------------
    fun selectEpisode(episode: Episode?) {
        _selectedEpisode.value = episode
        if (episode != null && episode.id.isNotBlank()) {
            loadSources(episode.id)
        } else {
            _episodeSources.value = emptyList()
        }
    }

    fun loadSources(episodeId: String) {
        viewModelScope.launch {
            val res = repository.getSources(episodeId, activeOnly = false)
            if (res.isSuccess) {
                _episodeSources.value = res.getOrDefault(emptyList())
            }
        }
    }

    fun saveEpisode(
        insert: EpisodeInsert?,
        update: EpisodeUpdate?,
        episodeId: String?,
        seriesId: String,
        hasActiveSources: Boolean,
        onDone: (Boolean, String?, Episode?) -> Unit
    ) {
        viewModelScope.launch {
            if (episodeId.isNullOrBlank()) {
                if (insert == null) {
                    onDone(false, "Invalid episode payload", null)
                    return@launch
                }
                val res = repository.createEpisode(insert)
                if (res.isSuccess) {
                    val created = res.getOrNull()
                    loadSeriesContent(seriesId)
                    onDone(true, "Episode ${insert.episodeNumber} created!", created)
                } else {
                    onDone(false, (res as? SeriesResult.Error)?.message ?: "Failed to create episode", null)
                }
            } else {
                if (update == null) {
                    onDone(false, "Invalid episode payload", null)
                    return@launch
                }
                val res = repository.updateEpisode(episodeId, update)
                if (res.isSuccess) {
                    val updated = res.getOrNull()
                    loadSeriesContent(seriesId)
                    onDone(true, "Episode updated!", updated)
                } else {
                    onDone(false, (res as? SeriesResult.Error)?.message ?: "Failed to update episode", null)
                }
            }
        }
    }

    fun deleteEpisode(episodeId: String, seriesId: String, onDone: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            val res = repository.deleteEpisode(episodeId)
            if (res.isSuccess) {
                loadSeriesContent(seriesId)
                if (_selectedEpisode.value?.id == episodeId) {
                    _selectedEpisode.value = null
                    _episodeSources.value = emptyList()
                }
                onDone(true, "Episode deleted")
            } else {
                onDone(false, (res as? SeriesResult.Error)?.message ?: "Failed to delete episode")
            }
        }
    }

    fun bulkAddEpisodes(seriesId: String, seasonNumber: Int, count: Int, onDone: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            val existingNumbers = _seriesEpisodes.value
                .filter { it.seasonNumber == seasonNumber }
                .map { it.episodeNumber }
                .toSet()

            val uid = repository.currentUserId ?: ""
            var added = 0
            var nextNum = 1

            for (i in 1..count) {
                while (existingNumbers.contains(nextNum)) {
                    nextNum++
                }
                val insert = EpisodeInsert(
                    seriesId = seriesId,
                    seasonNumber = seasonNumber,
                    episodeNumber = nextNum,
                    title = "Episode $nextNum",
                    createdBy = uid,
                    isPublished = true
                )
                val res = repository.createEpisode(insert)
                if (res.isSuccess) {
                    added++
                }
                nextNum++
            }
            loadSeriesContent(seriesId)
            onDone(true, "Created $added episode(s) in Season $seasonNumber")
        }
    }

    fun batchAddEpisodesWithSources(
        seriesId: String,
        seasonNumber: Int,
        startEpisodeNum: Int,
        count: Int,
        titlePrefix: String,
        urls: List<String>,
        defaultServerLabel: String = "Server 1",
        defaultQuality: String = "1080p",
        defaultLanguage: String? = null,
        onDone: (Boolean, String?) -> Unit
    ) {
        viewModelScope.launch {
            val uid = repository.currentUserId ?: ""
            var addedCount = 0
            var currentEpNum = startEpisodeNum

            for (i in 0 until count) {
                val epTitle = if (titlePrefix.isNotBlank()) "$titlePrefix $currentEpNum" else "Episode $currentEpNum"
                val urlForEp = urls.getOrNull(i)?.trim() ?: ""

                val epInsert = EpisodeInsert(
                    seriesId = seriesId,
                    seasonNumber = seasonNumber,
                    episodeNumber = currentEpNum,
                    title = epTitle,
                    createdBy = uid,
                    isPublished = true
                )

                val epRes = repository.createEpisode(epInsert)
                if (epRes.isSuccess) {
                    val createdEp = epRes.getOrNull()
                    if (createdEp != null && urlForEp.isNotBlank()) {
                        val platformStr = when {
                            urlForEp.contains("cloudflare") || urlForEp.contains("videodelivery") -> "cloudflare"
                            urlForEp.contains("youtube") || urlForEp.contains("youtu.be") -> "youtube"
                            urlForEp.contains("vimeo") -> "vimeo"
                            urlForEp.contains("drive.google") -> "gdrive"
                            urlForEp.endsWith(".m3u8") -> "hls"
                            urlForEp.endsWith(".mp4") -> "mp4"
                            else -> "direct"
                        }
                        val sourceInsert = SourceInsert(
                            episodeId = createdEp.id,
                            label = defaultServerLabel,
                            url = urlForEp,
                            platform = platformStr,
                            language = defaultLanguage,
                            quality = defaultQuality,
                            isActive = true
                        )
                        repository.createSource(sourceInsert)
                    }
                    addedCount++
                }
                currentEpNum++
            }
            loadSeriesContent(seriesId)
            onDone(true, "Batch generated $addedCount episode(s) for Season $seasonNumber!")
        }
    }


    // -------------------------------------------------------------------------
    // Sources
    // -------------------------------------------------------------------------
    fun saveSource(
        sourceId: String?,
        insert: SourceInsert?,
        update: SourceUpdate?,
        episodeId: String,
        onDone: (Boolean, String?) -> Unit
    ) {
        viewModelScope.launch {
            if (sourceId.isNullOrBlank()) {
                if (insert == null) {
                    onDone(false, "Invalid source data")
                    return@launch
                }
                val res = repository.createSource(insert)
                if (res.isSuccess) {
                    loadSources(episodeId)
                    onDone(true, "Source added successfully!")
                } else {
                    onDone(false, (res as? SeriesResult.Error)?.message ?: "Failed to add source")
                }
            } else {
                if (update == null) {
                    onDone(false, "Invalid source data")
                    return@launch
                }
                val res = repository.updateSource(sourceId, update)
                if (res.isSuccess) {
                    loadSources(episodeId)
                    onDone(true, "Source updated successfully!")
                } else {
                    onDone(false, (res as? SeriesResult.Error)?.message ?: "Failed to update source")
                }
            }
        }
    }

    fun deleteSource(sourceId: String, episodeId: String, onDone: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            val res = repository.deleteSource(sourceId)
            if (res.isSuccess) {
                loadSources(episodeId)
                onDone(true, "Source removed")
            } else {
                onDone(false, (res as? SeriesResult.Error)?.message ?: "Failed to remove source")
            }
        }
    }

    fun swapSourcePriority(sourceA: EpisodeSource, sourceB: EpisodeSource, episodeId: String) {
        viewModelScope.launch {
            val res = repository.swapSourcePriority(sourceA, sourceB)
            if (res.isSuccess) {
                loadSources(episodeId)
            }
        }
    }

    // -------------------------------------------------------------------------
    // Video URL Testing & Platform Detection
    // -------------------------------------------------------------------------
    fun testVideoUrl(
        url: String,
        onResult: (platform: String, durationSec: Int?, previewUrl: String?, title: String?) -> Unit
    ) {
        viewModelScope.launch {
            val cleanUrl = com.example.util.VideoEmbedUtils.extractUrlFromInput(url).trim()
            val embedInfo = com.example.util.VideoEmbedUtils.parseVideoUrl(cleanUrl)
            val lower = cleanUrl.lowercase()

            val detectedPlatform = when {
                embedInfo.platform != "direct" -> embedInfo.platform
                lower.contains(".m3u8") || lower.contains("/manifest/video.m3u8") -> "hls"
                lower.contains("cloudflarestream.com") || lower.contains("videodelivery.net") -> "cloudflare"
                lower.contains("youtube.com") || lower.contains("youtu.be") -> "youtube"
                lower.contains("drive.google.com") -> "gdrive"
                lower.contains("tiktok.com") -> "tiktok"
                else -> "direct"
            }

            var duration: Int? = null
            var preview: String? = null
            var title: String? = null

            try {
                val extracted = VideoPlatformRegistry.extract(cleanUrl).getOrNull()
                if (extracted != null) {
                    duration = extracted.durationSec
                    preview = extracted.directUrl ?: extracted.embedUrl
                    title = extracted.title
                }
            } catch (_: Exception) {}

            onResult(detectedPlatform, duration, preview, title)
        }
    }
}
