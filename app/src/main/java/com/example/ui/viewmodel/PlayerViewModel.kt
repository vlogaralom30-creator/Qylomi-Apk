package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.*
import com.example.data.repository.SeriesRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private val <T> SeriesResult<T>.errorMessage: String?
    get() = (this as? SeriesResult.Error)?.message

data class PlayerUiState(
    val isLoadingSeries: Boolean = true,
    val series: Series? = null,
    val seasons: List<Season> = emptyList(),
    val episodes: List<Episode> = emptyList(),
    val currentEpisode: Episode? = null,
    val sources: List<EpisodeSource> = emptyList(),
    val activeSource: EpisodeSource? = null,
    val activeSourceIndex: Int = 0,
    val isVideoLoading: Boolean = true,
    val playerError: String? = null,
    val fallbackNotice: String? = null,
    val initialSeekPositionSec: Int = 0,
    val positionSec: Int = 0,
    val durationSec: Int = 0,
    val isCompleted: Boolean = false,
    val isEpisodeLiked: Boolean = false,
    val episodeLikesCount: Int = 0,
    val isInWatchlist: Boolean = false,
    val showSkipIntro: Boolean = false,
    val introStartSec: Int = 0,
    val introEndSec: Int = 0,
    val autoplayCountdown: Int? = null, // 5..1 or null
    val selectedTab: Int = 0, // 0: Episodes, 1: Comments, 2: More Like This
    val selectedSeasonNumber: Int? = null,
    val comments: List<SeriesComment> = emptyList(),
    val isCommentsLoading: Boolean = false,
    val commentInput: String = "",
    val replyingToComment: SeriesComment? = null,
    val editingComment: SeriesComment? = null,
    val relatedSeries: List<SeriesCard> = emptyList(),
    val isReportSheetOpen: Boolean = false,
    val isSubmittingReport: Boolean = false,
    val reportSuccessMessage: String? = null,
    val errorMessage: String? = null
)

class PlayerViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = SeriesRepository(application)
    private val prefs = application.getSharedPreferences("q_player_prefs", Context.MODE_PRIVATE)

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    private var progressSyncJob: Job? = null
    private var recordViewJob: Job? = null
    private var autoplayJob: Job? = null
    private var hasRecordedViewForCurrentEpisode = false

    val isUserLoggedIn: Boolean
        get() = repository.isUserLoggedIn()

    val currentUserId: String?
        get() = repository.currentUserId

    fun loadEpisodeData(seriesId: String, episodeId: String? = null, initialPositionSec: Int = 0) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingSeries = true, errorMessage = null) }

            // 1. Fetch Series
            val seriesRes = repository.getSeriesById(seriesId)
            val series = seriesRes.getOrNull()
            if (series == null) {
                _uiState.update {
                    it.copy(
                        isLoadingSeries = false,
                        errorMessage = seriesRes.errorMessage ?: "Failed to load series details"
                    )
                }
                return@launch
            }

            // 2. Fetch Seasons & Episodes
            val seasonsRes = repository.getSeasons(seriesId)
            val seasons = seasonsRes.getOrNull() ?: emptyList()

            val episodesRes = repository.getEpisodes(seriesId)
            val episodes = episodesRes.getOrNull() ?: emptyList()

            if (episodes.isEmpty()) {
                _uiState.update {
                    it.copy(
                        isLoadingSeries = false,
                        series = series,
                        seasons = seasons,
                        episodes = emptyList(),
                        errorMessage = "No published episodes found for this series"
                    )
                }
                return@launch
            }

            // Determine current episode
            val targetEpisode = if (!episodeId.isNullOrBlank()) {
                episodes.find { it.id == episodeId } ?: episodes.first()
            } else {
                episodes.first()
            }

            val defaultSeasonNum = targetEpisode.seasonNumber

            // Check Watchlist status
            val inWatchlist = repository.isInWatchlist(seriesId).getOrDefault(false)

            _uiState.update {
                it.copy(
                    isLoadingSeries = false,
                    series = series,
                    seasons = seasons,
                    episodes = episodes,
                    selectedSeasonNumber = defaultSeasonNum,
                    isInWatchlist = inWatchlist
                )
            }

            // Fetch related series
            loadRelatedSeries(seriesId)

            // Select & load episode playback
            selectEpisode(targetEpisode, initialPositionSec)
        }
    }

    fun selectEpisode(episode: Episode, startPositionSec: Int = 0) {
        stopProgressSync()
        cancelAutoplayCountdown()

        _uiState.update {
            it.copy(
                currentEpisode = episode,
                isEpisodeLiked = false,
                episodeLikesCount = episode.likesCount,
                isVideoLoading = true,
                playerError = null,
                fallbackNotice = null,
                positionSec = startPositionSec,
                initialSeekPositionSec = startPositionSec,
                durationSec = episode.durationSeconds ?: 0,
                showSkipIntro = false,
                introStartSec = episode.introStartSec ?: 0,
                introEndSec = episode.introEndSec ?: 0,
                autoplayCountdown = null
            )
        }

        hasRecordedViewForCurrentEpisode = false

        viewModelScope.launch {
            // Check if user liked episode
            val isLiked = repository.isEpisodeLiked(episode.id).getOrDefault(false)
            _uiState.update { it.copy(isEpisodeLiked = isLiked) }

            // Fetch watch progress if not supplied
            var initialSeek = startPositionSec
            if (initialSeek <= 0 && repository.isUserLoggedIn()) {
                val progressRes = repository.getProgress(episode.seriesId, episode.id).getOrNull()
                val savedProgress = progressRes?.firstOrNull()
                if (savedProgress != null && savedProgress.positionSec > 5) {
                    val maxDur = savedProgress.durationSec ?: episode.durationSeconds ?: 0
                    if (maxDur == 0 || savedProgress.positionSec < maxDur - 15) {
                        initialSeek = savedProgress.positionSec
                    }
                }
            }

            // Fetch sources
            loadSourcesForEpisode(episode.id, initialSeek)

            // Load comments
            loadComments(episode.seriesId, episode.id)
        }
    }

    private fun loadSourcesForEpisode(episodeId: String, initialSeekSec: Int) {
        viewModelScope.launch {
            val sourcesRes = repository.getSources(episodeId)
            val rawSources = sourcesRes.getOrNull() ?: emptyList()

            if (rawSources.isEmpty()) {
                _uiState.update {
                    it.copy(
                        isVideoLoading = false,
                        playerError = "No streaming sources available for this episode."
                    )
                }
                return@launch
            }

            // Get user preferred server memory from prefs
            val lastWorkedSourceId = prefs.getString("last_source_${episodeId}", null)
            val preferredLanguage = prefs.getString("preferred_language", null)

            // Sort sources: priority ASC, matching preferred language first
            val sorted = rawSources.sortedWith(
                compareBy<EpisodeSource> {
                    if (!preferredLanguage.isNullOrBlank() && it.language?.lowercase() == preferredLanguage.lowercase()) 0 else 1
                }.thenBy { it.priority }
            )

            val startIndex = if (lastWorkedSourceId != null) {
                val idx = sorted.indexOfFirst { it.id == lastWorkedSourceId }
                if (idx >= 0) idx else 0
            } else 0

            val selectedSource = sorted.getOrNull(startIndex) ?: sorted.first()

            _uiState.update {
                it.copy(
                    sources = sorted,
                    activeSource = selectedSource,
                    activeSourceIndex = startIndex,
                    isVideoLoading = true,
                    initialSeekPositionSec = initialSeekSec,
                    positionSec = initialSeekSec,
                    playerError = null,
                    fallbackNotice = null
                )
            }
        }
    }

    fun selectSourceManually(source: EpisodeSource) {
        val index = _uiState.value.sources.indexOfFirst { it.id == source.id }
        if (index < 0) return

        _uiState.update {
            it.copy(
                activeSource = source,
                activeSourceIndex = index,
                isVideoLoading = true,
                playerError = null,
                fallbackNotice = "Switched to ${source.label}"
            )
        }

        // Save last selected source
        _uiState.value.currentEpisode?.id?.let { epId ->
            prefs.edit().putString("last_source_${epId}", source.id).apply()
        }
        source.language?.let { lang ->
            prefs.edit().putString("preferred_language", lang).apply()
        }
    }

    fun handleSourcePlaybackError(errorMsg: String) {
        val state = _uiState.value
        val sources = state.sources
        val currentIndex = state.activeSourceIndex

        val nextIndex = currentIndex + 1
        if (nextIndex < sources.size) {
            val nextSource = sources[nextIndex]
            _uiState.update {
                it.copy(
                    activeSource = nextSource,
                    activeSourceIndex = nextIndex,
                    isVideoLoading = true,
                    fallbackNotice = "Server failover: Switching to ${nextSource.label}..."
                )
            }
        } else {
            _uiState.update {
                it.copy(
                    isVideoLoading = false,
                    playerError = "All servers failed to load video: $errorMsg"
                )
            }
        }
    }

    fun onVideoReady(durationSec: Int) {
        val currentEp = _uiState.value.currentEpisode
        val activeSource = _uiState.value.activeSource

        _uiState.update {
            it.copy(
                isVideoLoading = false,
                durationSec = if (durationSec > 0) durationSec else (currentEp?.durationSeconds ?: 0),
                playerError = null
            )
        }

        // Remember working source ID
        if (currentEp != null && activeSource != null) {
            prefs.edit().putString("last_source_${currentEp.id}", activeSource.id).apply()
        }

        startProgressSync()
        startRecordViewTracker()
    }

    fun updatePlaybackPosition(positionSec: Int, durationSec: Int) {
        val ep = _uiState.value.currentEpisode ?: return
        val totalDur = if (durationSec > 0) durationSec else (ep.durationSeconds ?: 0)
        val isDone = totalDur > 0 && positionSec.toFloat() / totalDur.toFloat() >= 0.9f

        val introStart = ep.introStartSec ?: 0
        val introEnd = ep.introEndSec ?: 0
        val inIntro = introStart > 0 && introEnd > introStart && positionSec in introStart..introEnd

        _uiState.update {
            it.copy(
                positionSec = positionSec,
                durationSec = totalDur,
                isCompleted = isDone || it.isCompleted,
                showSkipIntro = inIntro,
                introStartSec = introStart,
                introEndSec = introEnd
            )
        }
    }

    fun onVideoEnded() {
        // Sync final progress
        val state = _uiState.value
        val seriesId = state.series?.id ?: return
        val episodeId = state.currentEpisode?.id ?: return

        viewModelScope.launch {
            repository.upsertProgress(
                seriesId = seriesId,
                episodeId = episodeId,
                positionSec = state.durationSec,
                durationSec = state.durationSec,
                completed = true
            )
        }

        // Check next episode for autoplay
        val currentEp = state.currentEpisode
        val allEps = state.episodes
        val currentIdx = allEps.indexOfFirst { it.id == currentEp.id }
        if (currentIdx >= 0 && currentIdx + 1 < allEps.size) {
            startAutoplayCountdown()
        }
    }

    private fun startAutoplayCountdown() {
        autoplayJob?.cancel()
        autoplayJob = viewModelScope.launch {
            for (sec in 5 downTo 1) {
                _uiState.update { it.copy(autoplayCountdown = sec) }
                delay(1000)
            }
            _uiState.update { it.copy(autoplayCountdown = 0) }
            playNextEpisode()
        }
    }

    fun cancelAutoplayCountdown() {
        autoplayJob?.cancel()
        _uiState.update { it.copy(autoplayCountdown = null) }
    }

    fun playNextEpisode() {
        cancelAutoplayCountdown()
        val state = _uiState.value
        val currentEp = state.currentEpisode ?: return
        val allEps = state.episodes
        val currentIdx = allEps.indexOfFirst { it.id == currentEp.id }

        if (currentIdx >= 0 && currentIdx + 1 < allEps.size) {
            val nextEp = allEps[currentIdx + 1]
            selectEpisode(nextEp, 0)
        }
    }

    fun playPreviousEpisode() {
        cancelAutoplayCountdown()
        val state = _uiState.value
        val currentEp = state.currentEpisode ?: return
        val allEps = state.episodes
        val currentIdx = allEps.indexOfFirst { it.id == currentEp.id }

        if (currentIdx > 0) {
            val prevEp = allEps[currentIdx - 1]
            selectEpisode(prevEp, 0)
        }
    }

    private fun startProgressSync() {
        progressSyncJob?.cancel()
        progressSyncJob = viewModelScope.launch {
            while (true) {
                delay(15_000) // 15 seconds
                syncCurrentProgress()
            }
        }
    }

    fun syncCurrentProgress() {
        val state = _uiState.value
        val seriesId = state.series?.id ?: return
        val episodeId = state.currentEpisode?.id ?: return
        if (state.positionSec <= 0) return

        viewModelScope.launch {
            repository.upsertProgress(
                seriesId = seriesId,
                episodeId = episodeId,
                positionSec = state.positionSec,
                durationSec = state.durationSec,
                completed = state.isCompleted
            )
        }
    }

    private fun stopProgressSync() {
        progressSyncJob?.cancel()
        recordViewJob?.cancel()
    }

    private fun startRecordViewTracker() {
        if (hasRecordedViewForCurrentEpisode) return
        recordViewJob?.cancel()
        recordViewJob = viewModelScope.launch {
            delay(30_000) // 30 seconds of playback
            val state = _uiState.value
            val seriesId = state.series?.id ?: return@launch
            val episodeId = state.currentEpisode?.id ?: return@launch

            repository.recordView(seriesId, episodeId)
            hasRecordedViewForCurrentEpisode = true
        }
    }

    // --- Like / Watchlist Actions ---

    fun toggleEpisodeLike() {
        val state = _uiState.value
        val episodeId = state.currentEpisode?.id ?: return
        if (!repository.isUserLoggedIn()) {
            _uiState.update { it.copy(errorMessage = "Please sign in to like episodes") }
            return
        }

        val currentlyLiked = state.isEpisodeLiked
        val newCount = if (currentlyLiked) (state.episodeLikesCount - 1).coerceAtLeast(0) else state.episodeLikesCount + 1

        _uiState.update {
            it.copy(
                isEpisodeLiked = !currentlyLiked,
                episodeLikesCount = newCount
            )
        }

        viewModelScope.launch {
            val res = if (currentlyLiked) repository.unlikeEpisode(episodeId) else repository.likeEpisode(episodeId)
            if (!res.isSuccess) {
                // Revert on failure
                _uiState.update {
                    it.copy(
                        isEpisodeLiked = currentlyLiked,
                        episodeLikesCount = state.episodeLikesCount
                    )
                }
            }
        }
    }

    fun toggleWatchlist() {
        val state = _uiState.value
        val seriesId = state.series?.id ?: return
        if (!repository.isUserLoggedIn()) {
            _uiState.update { it.copy(errorMessage = "Please sign in to save titles to My List") }
            return
        }

        val current = state.isInWatchlist
        _uiState.update { it.copy(isInWatchlist = !current) }

        viewModelScope.launch {
            val res = if (current) repository.removeFromWatchlist(seriesId) else repository.addToWatchlist(seriesId)
            if (!res.isSuccess) {
                _uiState.update { it.copy(isInWatchlist = current) }
            }
        }
    }

    // --- Report Sheet Actions ---

    fun openReportSheet() {
        if (!repository.isUserLoggedIn()) {
            _uiState.update { it.copy(errorMessage = "Please sign in to report a server issue") }
            return
        }
        _uiState.update {
            it.copy(
                isReportSheetOpen = true,
                reportSuccessMessage = null
            )
        }
    }

    fun closeReportSheet() {
        _uiState.update {
            it.copy(
                isReportSheetOpen = false,
                reportSuccessMessage = null
            )
        }
    }

    fun submitSourceReport(reason: String, details: String?) {
        val source = _uiState.value.activeSource ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmittingReport = true) }
            val res = repository.reportSource(source.id, reason, details)
            if (res.isSuccess) {
                _uiState.update {
                    it.copy(
                        isSubmittingReport = false,
                        reportSuccessMessage = "Report submitted successfully. Thank you!",
                        isReportSheetOpen = false
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        isSubmittingReport = false,
                        errorMessage = res.errorMessage ?: "Failed to submit report"
                    )
                }
            }
        }
    }

    // --- Comments Tab Actions ---

    fun setSelectedTab(tabIndex: Int) {
        _uiState.update { it.copy(selectedTab = tabIndex) }
    }

    fun setSelectedSeasonNumber(seasonNumber: Int) {
        _uiState.update { it.copy(selectedSeasonNumber = seasonNumber) }
    }

    fun setCommentInput(text: String) {
        _uiState.update { it.copy(commentInput = text) }
    }

    fun setReplyingTo(comment: SeriesComment?) {
        _uiState.update { it.copy(replyingToComment = comment, editingComment = null) }
    }

    fun setEditingComment(comment: SeriesComment?) {
        _uiState.update {
            it.copy(
                editingComment = comment,
                replyingToComment = null,
                commentInput = comment?.content ?: ""
            )
        }
    }

    private fun loadComments(seriesId: String, episodeId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isCommentsLoading = true) }
            val res = repository.getComments(seriesId, episodeId)
            _uiState.update {
                it.copy(
                    isCommentsLoading = false,
                    comments = res.getOrDefault(emptyList())
                )
            }
        }
    }

    fun postComment() {
        val state = _uiState.value
        val seriesId = state.series?.id ?: return
        val episodeId = state.currentEpisode?.id ?: return
        val content = state.commentInput.trim()

        if (content.isEmpty()) return
        if (!repository.isUserLoggedIn()) {
            _uiState.update { it.copy(errorMessage = "Please sign in to comment") }
            return
        }

        viewModelScope.launch {
            val editing = state.editingComment
            if (editing != null) {
                val editRes = repository.editCommentContent(editing.id, content)
                if (editRes.isSuccess) {
                    _uiState.update { it.copy(commentInput = "", editingComment = null) }
                    loadComments(seriesId, episodeId)
                } else {
                    _uiState.update { it.copy(errorMessage = editRes.errorMessage ?: "Failed to edit comment") }
                }
            } else {
                val parentId = state.replyingToComment?.id
                val addRes = repository.addComment(seriesId, episodeId, parentId, content)
                if (addRes.isSuccess) {
                    _uiState.update { it.copy(commentInput = "", replyingToComment = null) }
                    loadComments(seriesId, episodeId)
                } else {
                    _uiState.update { it.copy(errorMessage = addRes.errorMessage ?: "Failed to post comment") }
                }
            }
        }
    }

    fun deleteComment(commentId: String) {
        val state = _uiState.value
        val seriesId = state.series?.id ?: return
        val episodeId = state.currentEpisode?.id ?: return

        viewModelScope.launch {
            val res = repository.deleteComment(commentId)
            if (res.isSuccess) {
                loadComments(seriesId, episodeId)
            } else {
                _uiState.update { it.copy(errorMessage = res.errorMessage ?: "Failed to delete comment") }
            }
        }
    }

    fun toggleCommentLike(comment: SeriesComment) {
        if (!repository.isUserLoggedIn()) {
            _uiState.update { it.copy(errorMessage = "Please sign in to like comments") }
            return
        }

        viewModelScope.launch {
            val isLiked = repository.isCommentLiked(comment.id).getOrDefault(false)
            if (isLiked) {
                repository.unlikeComment(comment.id)
            } else {
                repository.likeComment(comment.id)
            }
            val state = _uiState.value
            state.series?.id?.let { sId ->
                state.currentEpisode?.id?.let { eId ->
                    loadComments(sId, eId)
                }
            }
        }
    }

    private fun loadRelatedSeries(seriesId: String) {
        viewModelScope.launch {
            val relatedRes = repository.getRelatedSeries(seriesId, 12)
            _uiState.update { it.copy(relatedSeries = relatedRes.getOrDefault(emptyList())) }
        }
    }

    fun clearErrorMessage() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    override fun onCleared() {
        super.onCleared()
        syncCurrentProgress()
        stopProgressSync()
    }
}
