package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.Post
import com.example.data.model.Tag
import com.example.data.model.UserProfile
import com.example.data.repository.SocialRepository
import com.example.util.ErrorLogger
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class SearchTab {
    ALL, POSTS, PEOPLE, TAGS
}

sealed interface SearchResultState<out T> {
    object Idle : SearchResultState<Nothing>
    object Loading : SearchResultState<Nothing>
    data class Success<T>(val data: List<T>, val hasMore: Boolean = false) : SearchResultState<T>
    data class Error(val message: String) : SearchResultState<Nothing>
}

@OptIn(FlowPreview::class)
class SearchViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = SocialRepository(application)
    private val prefs: SharedPreferences = application.getSharedPreferences("qylomi_search_prefs", Context.MODE_PRIVATE)

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedTab = MutableStateFlow(SearchTab.ALL)
    val selectedTab: StateFlow<SearchTab> = _selectedTab.asStateFlow()

    private val _recentSearches = MutableStateFlow<List<String>>(emptyList())
    val recentSearches: StateFlow<List<String>> = _recentSearches.asStateFlow()

    private val _trendingTags = MutableStateFlow<List<Tag>>(emptyList())
    val trendingTags: StateFlow<List<Tag>> = _trendingTags.asStateFlow()

    private val _isOverlayVisible = MutableStateFlow(false)
    val isOverlayVisible: StateFlow<Boolean> = _isOverlayVisible.asStateFlow()

    // Results states
    private val _postsState = MutableStateFlow<SearchResultState<Post>>(SearchResultState.Idle)
    val postsState: StateFlow<SearchResultState<Post>> = _postsState.asStateFlow()

    private val _usersState = MutableStateFlow<SearchResultState<UserProfile>>(SearchResultState.Idle)
    val usersState: StateFlow<SearchResultState<UserProfile>> = _usersState.asStateFlow()

    private val _tagsState = MutableStateFlow<SearchResultState<Tag>>(SearchResultState.Idle)
    val tagsState: StateFlow<SearchResultState<Tag>> = _tagsState.asStateFlow()

    // Tag Feed screen state
    private val _selectedTagForFeed = MutableStateFlow<Tag?>(null)
    val selectedTagForFeed: StateFlow<Tag?> = _selectedTagForFeed.asStateFlow()

    private val _tagFeedPosts = MutableStateFlow<List<Post>>(emptyList())
    val tagFeedPosts: StateFlow<List<Post>> = _tagFeedPosts.asStateFlow()

    private val _isTagFeedLoading = MutableStateFlow(false)
    val isTagFeedLoading: StateFlow<Boolean> = _isTagFeedLoading.asStateFlow()

    private val _isTagFeedRefreshing = MutableStateFlow(false)
    val isTagFeedRefreshing: StateFlow<Boolean> = _isTagFeedRefreshing.asStateFlow()

    init {
        loadRecentSearches()
        loadTrendingTags()

        // Debounced live search logic
        viewModelScope.launch {
            _searchQuery
                .debounce(300)
                .distinctUntilChanged()
                .collectLatest { query ->
                    val trimmed = query.trim()
                    if (trimmed.length < 2) {
                        _postsState.value = SearchResultState.Idle
                        _usersState.value = SearchResultState.Idle
                        _tagsState.value = SearchResultState.Idle
                    } else {
                        performSearch(trimmed)
                    }
                }
        }
    }

    fun openSearchOverlay(initialQuery: String = "") {
        _isOverlayVisible.value = true
        if (initialQuery.isNotBlank()) {
            onQueryChanged(initialQuery)
        }
    }

    fun closeSearchOverlay() {
        _isOverlayVisible.value = false
    }

    fun onQueryChanged(newQuery: String) {
        _searchQuery.value = newQuery

        // Auto-switch tabs based on prefixes
        val trimmed = newQuery.trim()
        if (trimmed.startsWith("#")) {
            _selectedTab.value = SearchTab.TAGS
        } else if (trimmed.startsWith("@")) {
            _selectedTab.value = SearchTab.PEOPLE
        }
    }

    fun selectTab(tab: SearchTab) {
        _selectedTab.value = tab
    }

    fun executeSearch() {
        val query = _searchQuery.value.trim()
        if (query.isNotBlank()) {
            addRecentSearch(query)
            performSearch(query)
        }
    }

    private fun performSearch(query: String) {
        viewModelScope.launch {
            _postsState.value = SearchResultState.Loading
            _usersState.value = SearchResultState.Loading
            _tagsState.value = SearchResultState.Loading

            val cleanQuery = query.removePrefix("#").removePrefix("@")

            launch {
                repository.searchPosts(cleanQuery, limit = 20, offset = 0)
                    .onSuccess { posts ->
                        _postsState.value = SearchResultState.Success(posts, hasMore = posts.size >= 20)
                    }
                    .onFailure { e ->
                        ErrorLogger.logException(e, "SearchViewModel", "Failed to search posts")
                        _postsState.value = SearchResultState.Error(e.message ?: "Failed to load posts")
                    }
            }

            launch {
                repository.searchUsers(cleanQuery, limit = 20, offset = 0)
                    .onSuccess { users ->
                        _usersState.value = SearchResultState.Success(users, hasMore = users.size >= 20)
                    }
                    .onFailure { e ->
                        ErrorLogger.logException(e, "SearchViewModel", "Failed to search users")
                        _usersState.value = SearchResultState.Error(e.message ?: "Failed to load users")
                    }
            }

            launch {
                repository.searchTags(cleanQuery, limit = 20, offset = 0)
                    .onSuccess { tags ->
                        _tagsState.value = SearchResultState.Success(tags, hasMore = tags.size >= 20)
                    }
                    .onFailure { e ->
                        ErrorLogger.logException(e, "SearchViewModel", "Failed to search tags")
                        _tagsState.value = SearchResultState.Error(e.message ?: "Failed to load tags")
                    }
            }
        }
    }

    fun loadTrendingTags() {
        viewModelScope.launch {
            repository.getTrendingTags()
                .onSuccess { tags ->
                    _trendingTags.value = tags
                }
                .onFailure { e ->
                    ErrorLogger.logException(e, "SearchViewModel", "Failed to load trending tags")
                }
        }
    }

    private fun loadRecentSearches() {
        try {
            val raw = prefs.getString("recent_searches", "") ?: ""
            if (raw.isNotBlank()) {
                _recentSearches.value = raw.split("|||").filter { it.isNotBlank() }.take(10)
            }
        } catch (e: Exception) {
            ErrorLogger.logException(e, "SearchViewModel", "Failed to load recent searches")
        }
    }

    fun addRecentSearch(query: String) {
        val q = query.trim()
        if (q.isBlank()) return
        val current = _recentSearches.value.toMutableList()
        current.remove(q)
        current.add(0, q)
        val updated = current.take(10)
        _recentSearches.value = updated
        prefs.edit().putString("recent_searches", updated.joinToString("|||")).apply()
    }

    fun removeRecentSearch(query: String) {
        val current = _recentSearches.value.toMutableList()
        current.remove(query)
        _recentSearches.value = current
        prefs.edit().putString("recent_searches", current.joinToString("|||")).apply()
    }

    fun clearAllRecentSearches() {
        _recentSearches.value = emptyList()
        prefs.edit().remove("recent_searches").apply()
    }

    // Tag Feed Methods
    fun openTagFeed(tag: Tag) {
        _selectedTagForFeed.value = tag
        loadTagFeed(tag)
    }

    fun openTagFeedByName(tagName: String) {
        val cleanName = tagName.removePrefix("#").lowercase()
        val tag = Tag(id = "", name = cleanName)
        _selectedTagForFeed.value = tag
        loadTagFeed(tag)
    }

    fun closeTagFeed() {
        _selectedTagForFeed.value = null
        _tagFeedPosts.value = emptyList()
    }

    fun loadTagFeed(tag: Tag, isRefresh: Boolean = false) {
        viewModelScope.launch {
            if (isRefresh) {
                _isTagFeedRefreshing.value = true
            } else {
                _isTagFeedLoading.value = true
            }

            repository.getPostsByTag(tagId = tag.id, tagName = tag.name)
                .onSuccess { posts ->
                    _tagFeedPosts.value = posts
                }
                .onFailure { e ->
                    ErrorLogger.logException(e, "SearchViewModel", "Failed to load tag feed for ${tag.name}")
                    _tagFeedPosts.value = emptyList()
                }

            _isTagFeedLoading.value = false
            _isTagFeedRefreshing.value = false
        }
    }
}
