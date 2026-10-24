package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.*
import com.example.data.repository.SeriesRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SeriesViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = SeriesRepository(application)
    private val prefs = application.getSharedPreferences("q_series_prefs", android.content.Context.MODE_PRIVATE)

    // Catalog state
    private val _categories = MutableStateFlow<List<ContentCategory>>(emptyList())
    val categories: StateFlow<List<ContentCategory>> = _categories.asStateFlow()

    private val _selectedCategory = MutableStateFlow<ContentCategory?>(null)
    val selectedCategory: StateFlow<ContentCategory?> = _selectedCategory.asStateFlow()

    private val _shelves = MutableStateFlow<List<ShelfWithItems>>(emptyList())
    val shelves: StateFlow<List<ShelfWithItems>> = _shelves.asStateFlow()

    private val _continueWatching = MutableStateFlow<List<ContinueWatchingItem>>(emptyList())
    val continueWatching: StateFlow<List<ContinueWatchingItem>> = _continueWatching.asStateFlow()

    private val _watchlist = MutableStateFlow<List<SeriesCard>>(emptyList())
    val watchlist: StateFlow<List<SeriesCard>> = _watchlist.asStateFlow()

    private val _isHomeLoading = MutableStateFlow(false)
    val isHomeLoading: StateFlow<Boolean> = _isHomeLoading.asStateFlow()

    private val _isHomeRefreshing = MutableStateFlow(false)
    val isHomeRefreshing: StateFlow<Boolean> = _isHomeRefreshing.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    // Optimistic tracking maps
    private val _watchlistIds = MutableStateFlow<Set<String>>(emptySet())
    val watchlistIds: StateFlow<Set<String>> = _watchlistIds.asStateFlow()

    private val _likedSeriesIds = MutableStateFlow<Set<String>>(emptySet())
    val likedSeriesIds: StateFlow<Set<String>> = _likedSeriesIds.asStateFlow()

    private val _seriesLikeCounts = MutableStateFlow<Map<String, Int>>(emptyMap())
    val seriesLikeCounts: StateFlow<Map<String, Int>> = _seriesLikeCounts.asStateFlow()

    private val _userRatings = MutableStateFlow<Map<String, Int>>(emptyMap())
    val userRatings: StateFlow<Map<String, Int>> = _userRatings.asStateFlow()

    // Search state
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _searchResults = MutableStateFlow<List<SeriesCard>>(emptyList())
    val searchResults: StateFlow<List<SeriesCard>> = _searchResults.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    private var searchJob: Job? = null

    // Details state
    private val _currentSeries = MutableStateFlow<Series?>(null)
    val currentSeries: StateFlow<Series?> = _currentSeries.asStateFlow()

    private val _seriesEpisodes = MutableStateFlow<List<Episode>>(emptyList())
    val seriesEpisodes: StateFlow<List<Episode>> = _seriesEpisodes.asStateFlow()

    private val _seriesSeasons = MutableStateFlow<List<Season>>(emptyList())
    val seriesSeasons: StateFlow<List<Season>> = _seriesSeasons.asStateFlow()

    private val _relatedSeries = MutableStateFlow<List<SeriesCard>>(emptyList())
    val relatedSeries: StateFlow<List<SeriesCard>> = _relatedSeries.asStateFlow()

    private val _isDetailsLoading = MutableStateFlow(false)
    val isDetailsLoading: StateFlow<Boolean> = _isDetailsLoading.asStateFlow()

    private val _detailsError = MutableStateFlow<String?>(null)
    val detailsError: StateFlow<String?> = _detailsError.asStateFlow()

    init {
        loadHomeData()
    }

    fun loadHomeData(isRefresh: Boolean = false) {
        viewModelScope.launch {
            if (isRefresh) {
                _isHomeRefreshing.value = true
            } else if (_shelves.value.isEmpty()) {
                _isHomeLoading.value = true
            }
            _errorMessage.value = null

            // 1. Categories
            val catRes = repository.getCategories(activeOnly = true)
            if (catRes.isSuccess) {
                _categories.value = (catRes.getOrNull() ?: emptyList()).sortedBy { it.sortOrder }
            }

            // 2. Shelves via get_series_home
            val catSlug = _selectedCategory.value?.slug
            val shelvesRes = repository.getHome(category = catSlug)
            if (shelvesRes.isSuccess) {
                val list = shelvesRes.getOrNull() ?: emptyList()
                _shelves.value = list
                // Prepopulate like counts
                val likeMap = mutableMapOf<String, Int>()
                list.forEach { shelf ->
                    shelf.items.forEach { card ->
                        likeMap[card.id] = card.likesCount
                    }
                }
                _seriesLikeCounts.value = _seriesLikeCounts.value + likeMap
            } else {
                _errorMessage.value = (shelvesRes as? SeriesResult.Error)?.message ?: "Failed to load series catalog"
            }

            // 3. User personalized rows if logged in
            if (repository.isUserLoggedIn()) {
                val cwRes = repository.getContinueWatching(limit = 10)
                if (cwRes.isSuccess) {
                    _continueWatching.value = cwRes.getOrNull() ?: emptyList()
                }

                val wlRes = repository.getWatchlist()
                if (wlRes.isSuccess) {
                    val wlItems = wlRes.getOrNull() ?: emptyList()
                    _watchlist.value = wlItems
                    _watchlistIds.value = wlItems.map { it.id }.toSet()
                }
            } else {
                _continueWatching.value = emptyList()
                _watchlist.value = emptyList()
                _watchlistIds.value = emptySet()
            }

            _isHomeLoading.value = false
            _isHomeRefreshing.value = false
        }
    }

    fun selectCategory(category: ContentCategory?) {
        if (_selectedCategory.value?.id == category?.id) return
        _selectedCategory.value = category
        loadHomeData(isRefresh = false)
    }

    fun refresh() {
        loadHomeData(isRefresh = true)
    }

    // -------------------------------------------------------------------------
    // Search
    // -------------------------------------------------------------------------
    fun setSearchQuery(query: String) {
        _searchQuery.value = query
        searchJob?.cancel()
        if (query.trim().length < 2) {
            _searchResults.value = emptyList()
            _isSearching.value = false
            return
        }

        searchJob = viewModelScope.launch {
            delay(300) // 300ms debounce
            _isSearching.value = true
            val catSlug = _selectedCategory.value?.slug
            val res = repository.searchSeries(query.trim(), category = catSlug)
            if (res.isSuccess) {
                _searchResults.value = res.getOrNull() ?: emptyList()
            } else {
                _searchResults.value = emptyList()
            }
            _isSearching.value = false
        }
    }

    // -------------------------------------------------------------------------
    // Details
    // -------------------------------------------------------------------------
    fun loadSeriesBySlug(slug: String) {
        viewModelScope.launch {
            _isDetailsLoading.value = true
            _detailsError.value = null
            _currentSeries.value = null

            val res = repository.getSeriesBySlug(slug)
            if (res.isSuccess) {
                val series = res.getOrNull()
                if (series == null || (series.isBlocked && !canBypassBlock(series))) {
                    _detailsError.value = "This title is not available."
                    _isDetailsLoading.value = false
                    return@launch
                }
                _currentSeries.value = series
                _seriesLikeCounts.value = _seriesLikeCounts.value + (series.id to series.likesCount)

                // Load episodes
                val epRes = repository.getEpisodes(series.id, publishedOnly = !canBypassBlock(series))
                if (epRes.isSuccess) {
                    val eps = epRes.getOrNull() ?: emptyList()
                    _seriesEpisodes.value = eps.sortedWith(compareBy({ it.seasonNumber }, { it.episodeNumber }))
                }

                // Load seasons
                val seasonRes = repository.getSeasons(series.id)
                if (seasonRes.isSuccess) {
                    _seriesSeasons.value = seasonRes.getOrNull() ?: emptyList()
                }

                // Load related series
                val relRes = repository.getRelatedSeries(series.id, limit = 10)
                if (relRes.isSuccess) {
                    _relatedSeries.value = relRes.getOrNull() ?: emptyList()
                }

                // Check watchlist and like status
                if (repository.isUserLoggedIn()) {
                    checkUserStatusForSeries(series.id)
                }
            } else {
                val err = (res as? SeriesResult.Error)?.message ?: "Unable to load series details"
                _detailsError.value = err
            }
            _isDetailsLoading.value = false
        }
    }

    private fun canBypassBlock(series: Series): Boolean {
        val currentUserId = repository.currentUserId
        return currentUserId != null && (series.createdBy == currentUserId)
    }

    private suspend fun checkUserStatusForSeries(seriesId: String) {
        val wlRes = repository.isInWatchlist(seriesId)
        if (wlRes.isSuccess && wlRes.getOrNull() == true) {
            _watchlistIds.value = _watchlistIds.value + seriesId
        }

        val likedRes = repository.isSeriesLiked(seriesId)
        if (likedRes.isSuccess && likedRes.getOrNull() == true) {
            _likedSeriesIds.value = _likedSeriesIds.value + seriesId
        }

        val ratingRes = repository.getUserRating(seriesId)
        if (ratingRes.isSuccess) {
            val r = ratingRes.getOrNull()
            if (r != null) {
                _userRatings.value = _userRatings.value + (seriesId to r.rating)
            }
        }
    }

    // -------------------------------------------------------------------------
    // Optimistic Watchlist, Like, Rating
    // -------------------------------------------------------------------------
    fun toggleWatchlist(seriesId: String, onAuthRequired: () -> Unit) {
        if (!repository.isUserLoggedIn()) {
            onAuthRequired()
            return
        }

        val wasInWatchlist = _watchlistIds.value.contains(seriesId)
        // Optimistic update
        _watchlistIds.value = if (wasInWatchlist) {
            _watchlistIds.value - seriesId
        } else {
            _watchlistIds.value + seriesId
        }

        viewModelScope.launch {
            val res = if (wasInWatchlist) {
                repository.removeFromWatchlist(seriesId)
            } else {
                repository.addToWatchlist(seriesId)
            }
            if (!res.isSuccess) {
                // Rollback on failure
                _watchlistIds.value = if (wasInWatchlist) {
                    _watchlistIds.value + seriesId
                } else {
                    _watchlistIds.value - seriesId
                }
            } else {
                // Refresh personal watchlist row
                val wlRes = repository.getWatchlist()
                if (wlRes.isSuccess) {
                    _watchlist.value = wlRes.getOrNull() ?: emptyList()
                }
            }
        }
    }

    fun toggleLike(seriesId: String, onAuthRequired: () -> Unit) {
        if (!repository.isUserLoggedIn()) {
            onAuthRequired()
            return
        }

        val wasLiked = _likedSeriesIds.value.contains(seriesId)
        val currentCount = _seriesLikeCounts.value[seriesId] ?: 0
        val optimisticCount = if (wasLiked) (currentCount - 1).coerceAtLeast(0) else currentCount + 1

        // Optimistic update
        _likedSeriesIds.value = if (wasLiked) _likedSeriesIds.value - seriesId else _likedSeriesIds.value + seriesId
        _seriesLikeCounts.value = _seriesLikeCounts.value + (seriesId to optimisticCount)

        viewModelScope.launch {
            val res = if (wasLiked) {
                repository.unlikeSeries(seriesId)
            } else {
                repository.likeSeries(seriesId)
            }
            if (!res.isSuccess) {
                // Rollback
                _likedSeriesIds.value = if (wasLiked) _likedSeriesIds.value + seriesId else _likedSeriesIds.value - seriesId
                _seriesLikeCounts.value = _seriesLikeCounts.value + (seriesId to currentCount)
            }
        }
    }

    fun rateSeries(seriesId: String, rating: Int, onAuthRequired: () -> Unit, onDone: (Boolean, String?) -> Unit) {
        if (!repository.isUserLoggedIn()) {
            onAuthRequired()
            return
        }

        val prevRating = _userRatings.value[seriesId]
        _userRatings.value = _userRatings.value + (seriesId to rating)

        viewModelScope.launch {
            val res = repository.rateSeries(seriesId, rating)
            if (res.isSuccess) {
                onDone(true, "Rating saved!")
                // Refresh series details to get updated avg_user_rating & ratings_count
                _currentSeries.value?.let { current ->
                    if (current.id == seriesId) {
                        val ref = repository.getSeriesBySlug(current.slug)
                        if (ref.isSuccess && ref.getOrNull() != null) {
                            _currentSeries.value = ref.getOrNull()
                        }
                    }
                }
            } else {
                // Rollback
                if (prevRating != null) {
                    _userRatings.value = _userRatings.value + (seriesId to prevRating)
                } else {
                    _userRatings.value = _userRatings.value - seriesId
                }
                val msg = (res as? SeriesResult.Error)?.message ?: "Failed to save rating"
                onDone(false, msg)
            }
        }
    }

    // -------------------------------------------------------------------------
    // 18+ Mature Consent Storage
    // -------------------------------------------------------------------------
    fun hasMatureConsent(): Boolean {
        return prefs.getBoolean("has_mature_consent", false)
    }

    fun grantMatureConsent() {
        prefs.edit().putBoolean("has_mature_consent", true).apply()
    }
}
