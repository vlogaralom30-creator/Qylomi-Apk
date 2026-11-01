package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.analytics.UserEventTracker
import com.example.data.api.SupabaseClient
import com.example.data.model.Comment
import com.example.data.model.FeedTab
import com.example.data.model.NotificationItem
import com.example.data.model.Post
import com.example.data.model.ProfileUpdateRequest
import com.example.data.model.SavedAccount
import com.example.data.model.UserEventType
import com.example.data.model.UserProfile
import com.example.data.repository.FeedRepository
import com.example.data.repository.SocialRepository
import com.example.util.FeedPresets
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SocialViewModel(application: Application) : AndroidViewModel(application) {

    val repository = SocialRepository(application)
    val feedRepository = FeedRepository(application, SupabaseClient, repository)
    private val feedPrefs = application.getSharedPreferences("qylomi_feed_prefs", Context.MODE_PRIVATE)

    val currentUser: StateFlow<UserProfile?> = repository.currentUser
    val isLoggedIn: StateFlow<Boolean> = repository.isLoggedIn
    val savedAccounts: StateFlow<List<SavedAccount>> = repository.savedAccounts

    fun removeSavedAccount(account: SavedAccount) {
        repository.removeSavedAccount(account.email)
    }

    private val _selectedFeedTab = MutableStateFlow(FeedTab.FOR_YOU)
    val selectedFeedTab: StateFlow<FeedTab> = _selectedFeedTab.asStateFlow()

    private val _isPersonalizedFeedEnabled = MutableStateFlow(feedPrefs.getBoolean("pref_personalized_feed", true))
    val isPersonalizedFeedEnabled: StateFlow<Boolean> = _isPersonalizedFeedEnabled.asStateFlow()

    private val _hasMoreFeed = MutableStateFlow(true)
    val hasMoreFeed: StateFlow<Boolean> = _hasMoreFeed.asStateFlow()

    private val _isFeedLoadingMore = MutableStateFlow(false)
    val isFeedLoadingMore: StateFlow<Boolean> = _isFeedLoadingMore.asStateFlow()

    private val shownRankedIds = mutableSetOf<String>()
    private var currentChronologicalOffset = 0

    private val _feedPosts = MutableStateFlow<List<Post>>(com.example.data.mock.SimulatedFeedDataProvider.getSimulatedPosts())
    val feedPosts: StateFlow<List<Post>> = _feedPosts.asStateFlow()

    private val _savedPosts = MutableStateFlow<List<Post>>(emptyList())
    val savedPosts: StateFlow<List<Post>> = _savedPosts.asStateFlow()

    private val _isFeedLoading = MutableStateFlow(false)
    val isFeedLoading: StateFlow<Boolean> = _isFeedLoading.asStateFlow()

    private val _feedError = MutableStateFlow<String?>(null)
    val feedError: StateFlow<String?> = _feedError.asStateFlow()

    private val _isFeedRefreshing = MutableStateFlow(false)
    val isFeedRefreshing: StateFlow<Boolean> = _isFeedRefreshing.asStateFlow()

    private val _isProfileRefreshing = MutableStateFlow(false)
    val isProfileRefreshing: StateFlow<Boolean> = _isProfileRefreshing.asStateFlow()

    private val _selectedCategory = MutableStateFlow("All")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    private val _allProfiles = MutableStateFlow<List<UserProfile>>(com.example.data.mock.SimulatedFeedDataProvider.creators)
    val allProfiles: StateFlow<List<UserProfile>> = _allProfiles.asStateFlow()

    private val _viewedProfile = MutableStateFlow<UserProfile?>(null)
    val viewedProfile: StateFlow<UserProfile?> = _viewedProfile.asStateFlow()

    private val _viewedUserPosts = MutableStateFlow<List<Post>>(emptyList())
    val viewedUserPosts: StateFlow<List<Post>> = _viewedUserPosts.asStateFlow()

    private val _trackersCount = MutableStateFlow(0)
    val trackersCount: StateFlow<Int> = _trackersCount.asStateFlow()

    private val _trackingCount = MutableStateFlow(0)
    val trackingCount: StateFlow<Int> = _trackingCount.asStateFlow()

    private val _isProfileLoading = MutableStateFlow(false)
    val isProfileLoading: StateFlow<Boolean> = _isProfileLoading.asStateFlow()

    private val _selectedPostForComments = MutableStateFlow<Post?>(null)
    val selectedPostForComments: StateFlow<Post?> = _selectedPostForComments.asStateFlow()

    private val _comments = MutableStateFlow<List<Comment>>(emptyList())
    val comments: StateFlow<List<Comment>> = _comments.asStateFlow()

    private val _isCommentsLoading = MutableStateFlow(false)
    val isCommentsLoading: StateFlow<Boolean> = _isCommentsLoading.asStateFlow()

    private val _notifications = MutableStateFlow<List<NotificationItem>>(emptyList())
    val notifications: StateFlow<List<NotificationItem>> = _notifications.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _bannerMessage = MutableStateFlow<String?>(null)
    val bannerMessage: StateFlow<String?> = _bannerMessage.asStateFlow()

    private val _isReady = MutableStateFlow(false)
    val isReady: StateFlow<Boolean> = _isReady.asStateFlow()

    init {
        viewModelScope.launch {
            try {
                repository.restoreSession()
                loadFeed()
                loadProfiles()
                loadSavedPosts()
            } catch (e: Throwable) {
                android.util.Log.e("SocialViewModel", "Initialization error: ${e.message}", e)
            } finally {
                _isReady.value = true
            }
        }
    }


    fun clearBanner() {
        _bannerMessage.value = null
    }

    fun setBanner(msg: String) {
        _bannerMessage.value = msg
    }

    fun login(email: String, pass: String, onDone: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            val result = repository.login(email.trim(), pass)
            result.onSuccess {
                loadFeed()
                loadProfiles()
                loadSavedPosts()
                onDone(true, null)
            }.onFailure { e ->
                onDone(false, e.localizedMessage ?: "Login failed")
            }
        }
    }

    fun signUp(email: String, pass: String, username: String, displayName: String, onDone: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            val result = repository.signUp(email.trim(), pass, username.trim(), displayName.trim())
            result.onSuccess {
                loadFeed()
                loadProfiles()
                loadSavedPosts()
                onDone(true, null)
            }.onFailure { e ->
                onDone(false, e.localizedMessage ?: "Registration failed")
            }
        }
    }

    fun loginAsDemoUser(profile: UserProfile) {
        repository.loginAsDemoUser(profile)
        viewModelScope.launch {
            repository.loadUserEngagement(profile.id)
            loadFeed()
            loadProfiles()
            loadSavedPosts()
        }
    }

    fun logout() {
        repository.logout()
        _savedPosts.value = emptyList()
    }

    private fun createFeaturedStreamtapeTestPost(): Post {
        return Post(
            id = "test_streamtape_embed_1",
            userId = "system_test_user",
            type = "Post",
            title = "Streamtape Embed & Ad Shield v2 Test",
            content = "Testing Streamtape iframe player embed (https://streamtape.com/e/q3qZQ0Ld7giw1Y/) with Ad Shield v2 active.",
            externalUrl = "https://streamtape.com/e/q3qZQ0Ld7giw1Y/",
            authorProfile = UserProfile(
                id = "system_test_user",
                username = "adshield_test",
                displayName = "Ad Shield Inspector Test",
                avatarUrl = null
            ),
            videoPosts = listOf(
                com.example.data.model.VideoPost(
                    id = "vid_streamtape_1",
                    postId = "test_streamtape_embed_1",
                    videoUrl = "https://streamtape.com/e/q3qZQ0Ld7giw1Y/",
                    embedUrl = "https://streamtape.com/e/q3qZQ0Ld7giw1Y/",
                    platform = "streamtape",
                    title = "Streamtape Embed Test"
                )
            )
        )
    }

    private fun getSeedPostsWithFeatured(): List<Post> {
        val testPost = createFeaturedStreamtapeTestPost()
        val simulated = com.example.data.mock.SimulatedFeedDataProvider.getSimulatedPosts()
        return (listOf(testPost) + simulated).distinctBy { it.id }
    }

    fun selectFeedTab(tab: FeedTab) {
        if (_selectedFeedTab.value == tab) return
        _selectedFeedTab.value = tab
        loadFeed()
    }

    fun togglePersonalizedFeed(enabled: Boolean) {
        _isPersonalizedFeedEnabled.value = enabled
        UserEventTracker.isPersonalizedFeedEnabled = enabled
        feedPrefs.edit().putBoolean("pref_personalized_feed", enabled).apply()
        if (_selectedFeedTab.value == FeedTab.FOR_YOU) {
            loadFeed()
        }
    }

    fun resetPersonalization(onDone: (Boolean, String?) -> Unit) {
        val user = currentUser.value
        if (user == null) {
            onDone(false, "Please sign in to reset feed personalization")
            return
        }
        viewModelScope.launch {
            try {
                val res = SupabaseClient.api.resetUserEvents("eq.${user.id}")
                if (res.isSuccessful) {
                    UserEventTracker.clearSessionData()
                    loadFeed()
                    onDone(true, null)
                } else {
                    onDone(false, res.errorBody()?.string() ?: "Failed to reset personalization")
                }
            } catch (e: Exception) {
                onDone(false, e.localizedMessage ?: "Failed to reset personalization")
            }
        }
    }

    fun loadFeed() {
        viewModelScope.launch {
            _isFeedLoading.value = true
            shownRankedIds.clear()
            currentChronologicalOffset = 0
            _hasMoreFeed.value = true

            val tab = _selectedFeedTab.value
            val category = _selectedCategory.value
            val isPersonalized = _isPersonalizedFeedEnabled.value

            when (tab) {
                FeedTab.FOR_YOU -> {
                    if (isPersonalized && category == "All") {
                        val res = feedRepository.getRankedPage(shownRankedIds, pageSize = 20)
                        res.onSuccess { list ->
                            shownRankedIds.addAll(list.map { it.id })
                            val pool = if (list.isNotEmpty()) {
                                (list + com.example.data.mock.SimulatedFeedDataProvider.getSimulatedPosts()).distinctBy { it.id }
                            } else {
                                getSeedPostsWithFeatured()
                            }
                            val testPost = createFeaturedStreamtapeTestPost()
                            val combined = listOf(testPost) + pool.filter { it.id != testPost.id }
                            _feedPosts.value = combined
                            _hasMoreFeed.value = list.isNotEmpty()
                            _feedError.value = null
                        }.onFailure { err ->
                            _feedError.value = err.localizedMessage
                            _feedPosts.value = getSeedPostsWithFeatured()
                        }
                    } else {
                        val res = feedRepository.getChronologicalPage(offset = 0, pageSize = 20)
                        res.onSuccess { list ->
                            currentChronologicalOffset = list.size
                            val pool = if (list.isNotEmpty()) {
                                (list + com.example.data.mock.SimulatedFeedDataProvider.getSimulatedPosts()).distinctBy { it.id }
                            } else {
                                getSeedPostsWithFeatured()
                            }
                            val filtered = if (category != "All") pool.filter { it.type.equals(category, ignoreCase = true) } else pool
                            val testPost = createFeaturedStreamtapeTestPost()
                            val combined = if (category == "All") listOf(testPost) + filtered.filter { it.id != testPost.id } else filtered
                            _feedPosts.value = combined
                            _hasMoreFeed.value = list.size >= 20
                            _feedError.value = null
                        }.onFailure { err ->
                            _feedError.value = err.localizedMessage
                            val pool = getSeedPostsWithFeatured()
                            val filtered = if (category != "All") pool.filter { it.type.equals(category, ignoreCase = true) } else pool
                            _feedPosts.value = filtered
                        }
                    }
                }
                FeedTab.LATEST -> {
                    val res = feedRepository.getChronologicalPage(offset = 0, pageSize = 20)
                    res.onSuccess { list ->
                        currentChronologicalOffset = list.size
                        val pool = if (list.isNotEmpty()) {
                            (list + com.example.data.mock.SimulatedFeedDataProvider.getSimulatedPosts()).distinctBy { it.id }
                        } else {
                            getSeedPostsWithFeatured()
                        }
                        val filtered = if (category != "All") pool.filter { it.type.equals(category, ignoreCase = true) } else pool
                        val testPost = createFeaturedStreamtapeTestPost()
                        val combined = if (category == "All") listOf(testPost) + filtered.filter { it.id != testPost.id } else filtered
                        _feedPosts.value = combined
                        _hasMoreFeed.value = list.size >= 20
                        _feedError.value = null
                    }.onFailure { err ->
                        _feedError.value = err.localizedMessage
                        val pool = getSeedPostsWithFeatured()
                        val filtered = if (category != "All") pool.filter { it.type.equals(category, ignoreCase = true) } else pool
                        _feedPosts.value = filtered
                    }
                }
                FeedTab.TRENDING -> {
                    val res = feedRepository.getPoolPosts(limit = 100, offset = 0)
                    res.onSuccess { pool ->
                        val combinedPool = (pool + com.example.data.mock.SimulatedFeedDataProvider.getSimulatedPosts()).distinctBy { it.id }
                        val sorted = FeedPresets.applyPreset(combinedPool, FeedTab.TRENDING)
                        val filtered = if (category != "All") sorted.filter { it.type.equals(category, ignoreCase = true) } else sorted
                        _feedPosts.value = filtered
                        _hasMoreFeed.value = false
                        _feedError.value = null
                    }.onFailure { err ->
                        _feedError.value = err.localizedMessage
                        val sorted = FeedPresets.applyPreset(getSeedPostsWithFeatured(), FeedTab.TRENDING)
                        val filtered = if (category != "All") sorted.filter { it.type.equals(category, ignoreCase = true) } else sorted
                        _feedPosts.value = filtered
                    }
                }
                FeedTab.FOLLOWING -> {
                    val user = currentUser.value
                    if (user == null) {
                        val simulated = com.example.data.mock.SimulatedFeedDataProvider.getSimulatedPosts()
                        val filtered = if (category != "All") simulated.filter { it.type.equals(category, ignoreCase = true) } else simulated
                        _feedPosts.value = filtered
                        _hasMoreFeed.value = false
                        _feedError.value = null
                    } else {
                        val res = feedRepository.getFollowingPage(user.id, offset = 0, pageSize = 20)
                        res.onSuccess { list ->
                            currentChronologicalOffset = list.size
                            val pool = if (list.isNotEmpty()) list else com.example.data.mock.SimulatedFeedDataProvider.getSimulatedPosts()
                            val filtered = if (category != "All") pool.filter { it.type.equals(category, ignoreCase = true) } else pool
                            _feedPosts.value = filtered
                            _hasMoreFeed.value = list.size >= 20
                            _feedError.value = null
                        }.onFailure { err ->
                            _feedError.value = err.localizedMessage
                            val simulated = com.example.data.mock.SimulatedFeedDataProvider.getSimulatedPosts()
                            val filtered = if (category != "All") simulated.filter { it.type.equals(category, ignoreCase = true) } else simulated
                            _feedPosts.value = filtered
                        }
                    }
                }
                FeedTab.VIDEOS -> {
                    val resultList = mutableListOf<Post>()
                    var poolOffset = 0
                    var page = 0
                    while (resultList.size < 10 && page < 3) {
                        val poolRes = feedRepository.getPoolPosts(limit = 100, offset = poolOffset)
                        val posts = poolRes.getOrNull() ?: break
                        if (posts.isEmpty()) break
                        val filtered = FeedPresets.applyPreset(posts, FeedTab.VIDEOS)
                        resultList.addAll(filtered)
                        poolOffset += posts.size
                        page++
                        if (posts.size < 100) break
                    }
                    val combinedWithSim = (resultList + FeedPresets.applyPreset(getSeedPostsWithFeatured(), FeedTab.VIDEOS)).distinctBy { it.id }
                    val filtered = if (category != "All") combinedWithSim.filter { it.type.equals(category, ignoreCase = true) } else combinedWithSim
                    _feedPosts.value = filtered
                    _hasMoreFeed.value = false
                    _feedError.value = null
                }
                FeedTab.BANGLA -> {
                    val resultList = mutableListOf<Post>()
                    var poolOffset = 0
                    var page = 0
                    while (resultList.size < 10 && page < 3) {
                        val poolRes = feedRepository.getPoolPosts(limit = 100, offset = poolOffset)
                        val posts = poolRes.getOrNull() ?: break
                        if (posts.isEmpty()) break
                        val filtered = FeedPresets.applyPreset(posts, FeedTab.BANGLA)
                        resultList.addAll(filtered)
                        poolOffset += posts.size
                        page++
                        if (posts.size < 100) break
                    }
                    val combinedWithSim = (resultList + FeedPresets.applyPreset(getSeedPostsWithFeatured(), FeedTab.BANGLA)).distinctBy { it.id }
                    val filtered = if (category != "All") combinedWithSim.filter { it.type.equals(category, ignoreCase = true) } else combinedWithSim
                    _feedPosts.value = filtered
                    _hasMoreFeed.value = false
                    _feedError.value = null
                }
            }
            _isFeedLoading.value = false
        }
    }

    fun loadMoreFeed() {
        if (_isFeedLoadingMore.value || !_hasMoreFeed.value || _isFeedLoading.value) return
        viewModelScope.launch {
            _isFeedLoadingMore.value = true
            val tab = _selectedFeedTab.value
            val category = _selectedCategory.value
            val isPersonalized = _isPersonalizedFeedEnabled.value

            when (tab) {
                FeedTab.FOR_YOU -> {
                    if (isPersonalized && category == "All") {
                        val res = feedRepository.getRankedPage(shownRankedIds, pageSize = 20)
                        res.onSuccess { list ->
                            if (list.isEmpty()) {
                                _hasMoreFeed.value = false
                            } else {
                                shownRankedIds.addAll(list.map { it.id })
                                val currentIds = _feedPosts.value.map { it.id }.toSet()
                                val newItems = list.filter { !currentIds.contains(it.id) }
                                _feedPosts.value = _feedPosts.value + newItems
                            }
                        }.onFailure {
                            _hasMoreFeed.value = false
                        }
                    } else {
                        val res = feedRepository.getChronologicalPage(offset = currentChronologicalOffset, pageSize = 20)
                        res.onSuccess { list ->
                            if (list.isEmpty()) {
                                _hasMoreFeed.value = false
                            } else {
                                currentChronologicalOffset += list.size
                                val currentIds = _feedPosts.value.map { it.id }.toSet()
                                val filtered = if (category != "All") list.filter { it.type.equals(category, ignoreCase = true) } else list
                                val newItems = filtered.filter { !currentIds.contains(it.id) }
                                _feedPosts.value = _feedPosts.value + newItems
                                if (list.size < 20) _hasMoreFeed.value = false
                            }
                        }.onFailure {
                            _hasMoreFeed.value = false
                        }
                    }
                }
                FeedTab.LATEST -> {
                    val res = feedRepository.getChronologicalPage(offset = currentChronologicalOffset, pageSize = 20)
                    res.onSuccess { list ->
                        if (list.isEmpty()) {
                            _hasMoreFeed.value = false
                        } else {
                            currentChronologicalOffset += list.size
                            val currentIds = _feedPosts.value.map { it.id }.toSet()
                            val filtered = if (category != "All") list.filter { it.type.equals(category, ignoreCase = true) } else list
                            val newItems = filtered.filter { !currentIds.contains(it.id) }
                            _feedPosts.value = _feedPosts.value + newItems
                            if (list.size < 20) _hasMoreFeed.value = false
                        }
                    }.onFailure {
                        _hasMoreFeed.value = false
                    }
                }
                FeedTab.FOLLOWING -> {
                    val user = currentUser.value
                    if (user != null) {
                        val res = feedRepository.getFollowingPage(user.id, offset = currentChronologicalOffset, pageSize = 20)
                        res.onSuccess { list ->
                            if (list.isEmpty()) {
                                _hasMoreFeed.value = false
                            } else {
                                currentChronologicalOffset += list.size
                                val currentIds = _feedPosts.value.map { it.id }.toSet()
                                val filtered = if (category != "All") list.filter { it.type.equals(category, ignoreCase = true) } else list
                                val newItems = filtered.filter { !currentIds.contains(it.id) }
                                _feedPosts.value = _feedPosts.value + newItems
                                if (list.size < 20) _hasMoreFeed.value = false
                            }
                        }.onFailure {
                            _hasMoreFeed.value = false
                        }
                    } else {
                        _hasMoreFeed.value = false
                    }
                }
                else -> {
                    _hasMoreFeed.value = false
                }
            }
            _isFeedLoadingMore.value = false
        }
    }

    fun refreshFeed() {
        viewModelScope.launch {
            _isFeedRefreshing.value = true
            loadFeed()
            loadSavedPosts()
            _isFeedRefreshing.value = false
        }
    }

    fun refreshProfile(userId: String) {
        viewModelScope.launch {
            _isProfileRefreshing.value = true
            val res = repository.getUserPosts(userId)
            res.onSuccess { list ->
                _viewedUserPosts.value = list
            }
            loadSocialGraphCounts(userId)
            loadSavedPosts()
            _isProfileRefreshing.value = false
        }
    }

    fun loadSavedPosts() {
        viewModelScope.launch {
            val res = repository.getSavedPosts()
            res.onSuccess { list ->
                _savedPosts.value = list
            }
        }
    }

    fun setCategory(category: String) {
        _selectedCategory.value = category
        loadFeed()
    }

    fun loadProfiles() {
        viewModelScope.launch {
            val res = repository.getAllProfiles()
            res.onSuccess { list ->
                val combined = (list + com.example.data.mock.SimulatedFeedDataProvider.creators).distinctBy { it.id }
                _allProfiles.value = combined
            }.onFailure {
                _allProfiles.value = com.example.data.mock.SimulatedFeedDataProvider.creators
            }
        }
    }

    fun createPost(type: String, title: String?, content: String, url: String?, onDone: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            val res = repository.createPost(type, title, content, url)
            res.onSuccess { newPost ->
                _feedPosts.value = listOf(newPost) + _feedPosts.value
                setBanner("Post published!")
                onDone(true, null)
            }.onFailure { e ->
                onDone(false, e.localizedMessage ?: "Failed to post")
            }
        }
    }

    fun createVideoPost(
        title: String?,
        videoUrl: String,
        embedUrl: String? = null,
        directUrl: String? = null,
        platform: String? = null,
        thumbnailUrl: String? = null,
        durationSeconds: Int? = null,
        width: Int? = null,
        height: Int? = null,
        isVertical: Boolean? = false,
        category: String? = null,
        tags: List<String> = emptyList(),
        onDone: (Boolean, String?) -> Unit
    ) {
        viewModelScope.launch {
            val res = repository.createVideoPost(
                title = title,
                videoUrl = videoUrl,
                embedUrl = embedUrl,
                directUrl = directUrl,
                platform = platform,
                thumbnailUrl = thumbnailUrl,
                durationSeconds = durationSeconds,
                width = width,
                height = height,
                isVertical = isVertical,
                category = category,
                tags = tags
            )
            res.onSuccess { newPost ->
                _feedPosts.value = listOf(newPost) + _feedPosts.value
                setBanner("Video post published!")
                onDone(true, null)
            }.onFailure { e ->
                onDone(false, e.localizedMessage ?: "Failed to publish video post")
            }
        }
    }

    fun createPhotoPost(
        caption: String?,
        photoUrls: List<String>,
        category: String?,
        tags: List<String>,
        onDone: (Boolean, String?) -> Unit
    ) {
        viewModelScope.launch {
            val res = repository.createPhotoPost(caption, photoUrls, category, tags)
            res.onSuccess { newPost ->
                _feedPosts.value = listOf(newPost) + _feedPosts.value
                setBanner("Photo gallery published!")
                onDone(true, null)
            }.onFailure { e ->
                onDone(false, e.localizedMessage ?: "Failed to publish photo gallery")
            }
        }
    }

    fun createTextPost(
        caption: String,
        templateId: String?,
        category: String?,
        tags: List<String>,
        onDone: (Boolean, String?) -> Unit
    ) {
        viewModelScope.launch {
            val res = repository.createTextPost(caption, templateId, category, tags)
            res.onSuccess { newPost ->
                _feedPosts.value = listOf(newPost) + _feedPosts.value
                setBanner("Thought post published!")
                onDone(true, null)
            }.onFailure { e ->
                onDone(false, e.localizedMessage ?: "Failed to publish thought post")
            }
        }
    }

    // Toggle reaction via post_reactions table
    fun toggleLike(post: Post) {
        val currentIsLiked = post.isLikedByMe
        val newIsLiked = !currentIsLiked
        val newCount = if (newIsLiked) post.appreciationsCount + 1 else (post.appreciationsCount - 1).coerceAtLeast(0)

        // Optimistic UI update
        _feedPosts.value = _feedPosts.value.map {
            if (it.id == post.id) it.copy(isLikedByMe = newIsLiked, appreciationsCount = newCount) else it
        }
        _viewedUserPosts.value = _viewedUserPosts.value.map {
            if (it.id == post.id) it.copy(isLikedByMe = newIsLiked, appreciationsCount = newCount) else it
        }
        _savedPosts.value = _savedPosts.value.map {
            if (it.id == post.id) it.copy(isLikedByMe = newIsLiked, appreciationsCount = newCount) else it
        }

        viewModelScope.launch {
            val isNowLiked = repository.toggleLike(post.id)
            if (isNowLiked) {
                UserEventTracker.track(post.id, UserEventType.LIKE)
            }
        }
    }

    // Toggle bookmark via saved_posts table
    fun toggleBookmark(post: Post) {
        val currentIsSaved = post.isBookmarkedByMe
        val newIsSaved = !currentIsSaved

        _feedPosts.value = _feedPosts.value.map {
            if (it.id == post.id) it.copy(isBookmarkedByMe = newIsSaved) else it
        }
        _viewedUserPosts.value = _viewedUserPosts.value.map {
            if (it.id == post.id) it.copy(isBookmarkedByMe = newIsSaved) else it
        }

        viewModelScope.launch {
            val isNowSaved = repository.toggleBookmark(post.id)
            if (isNowSaved) {
                UserEventTracker.track(post.id, UserEventType.BOOKMARK)
            }
            setBanner(if (isNowSaved) "Saved to Bookmarks" else "Removed from Bookmarks")
            loadSavedPosts()
        }
    }

    fun openComments(post: Post) {
        _selectedPostForComments.value = post
        _comments.value = emptyList()
        _isCommentsLoading.value = true
        viewModelScope.launch {
            val res = repository.getComments(post.id)
            res.onSuccess {
                _comments.value = it
            }
            _isCommentsLoading.value = false
        }
    }

    fun closeComments() {
        _selectedPostForComments.value = null
        _comments.value = emptyList()
    }

    fun addComment(text: String, onDone: (Boolean) -> Unit) {
        val post = _selectedPostForComments.value ?: return
        if (text.isBlank()) return
        viewModelScope.launch {
            val res = repository.addComment(post.id, text.trim())
            res.onSuccess { comment ->
                _comments.value = _comments.value + comment
                _feedPosts.value = _feedPosts.value.map {
                    if (it.id == post.id) it.copy(repliesCount = it.repliesCount + 1) else it
                }
                onDone(true)
            }.onFailure {
                onDone(false)
            }
        }
    }

    fun viewUserProfile(profile: UserProfile) {
        _viewedProfile.value = profile
        loadUserPosts(profile.id)
        loadSocialGraphCounts(profile.id)
    }

    fun loadUserPosts(userId: String) {
        viewModelScope.launch {
            _isProfileLoading.value = true
            val res = repository.getUserPosts(userId)
            res.onSuccess {
                _viewedUserPosts.value = it
            }
            _isProfileLoading.value = false
        }
    }

    fun loadSocialGraphCounts(userId: String) {
        viewModelScope.launch {
            _trackersCount.value = repository.getFollowersCount(userId)
            _trackingCount.value = repository.getFollowingCount(userId)
        }
    }

    fun updateProfile(userId: String, updates: ProfileUpdateRequest, onDone: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            val res = repository.updateProfile(userId, updates)
            res.onSuccess { updated ->
                _viewedProfile.value = updated
                _allProfiles.value = _allProfiles.value.map { if (it.id == updated.id) updated else it }
                setBanner("Profile updated successfully!")
                onDone(true, null)
            }.onFailure { e ->
                onDone(false, e.localizedMessage ?: "Failed to update profile")
            }
        }
    }

    fun toggleFollow(targetProfile: UserProfile) {
        viewModelScope.launch {
            val isNowFollowing = repository.toggleFollow(targetProfile.id)
            setBanner(if (isNowFollowing) "Tracking @${targetProfile.effectiveUsername}" else "Unfollowed")
            loadProfiles()
            loadSocialGraphCounts(targetProfile.id)
        }
    }

    fun isUserFollowed(userId: String): Boolean = repository.isUserFollowed(userId)

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    private val _groups = MutableStateFlow<List<com.example.data.model.GroupCommunity>>(emptyList())
    val groups: StateFlow<List<com.example.data.model.GroupCommunity>> = _groups.asStateFlow()

    private val _isGroupsLoading = MutableStateFlow(false)
    val isGroupsLoading: StateFlow<Boolean> = _isGroupsLoading.asStateFlow()

    fun editPost(postId: String, newContent: String, newTitle: String? = null, onDone: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            val res = repository.updatePost(postId, newContent, newTitle)
            res.onSuccess { updatedPost ->
                _feedPosts.value = _feedPosts.value.map { if (it.id == postId) updatedPost else it }
                _viewedUserPosts.value = _viewedUserPosts.value.map { if (it.id == postId) updatedPost else it }
                setBanner("Post updated successfully!")
                onDone(true, null)
            }.onFailure { e ->
                onDone(false, e.localizedMessage ?: "Failed to edit post")
            }
        }
    }

    fun deletePost(postId: String, onDone: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            val res = repository.deletePost(postId)
            res.onSuccess {
                _feedPosts.value = _feedPosts.value.filter { it.id != postId }
                _viewedUserPosts.value = _viewedUserPosts.value.filter { it.id != postId }
                _savedPosts.value = _savedPosts.value.filter { it.id != postId }
                setBanner("Post deleted")
                onDone(true, null)
            }.onFailure { e ->
                onDone(false, e.localizedMessage ?: "Failed to delete post")
            }
        }
    }

    fun loadGroups() {
        viewModelScope.launch {
            _isGroupsLoading.value = true
            val res = repository.getGroups()
            res.onSuccess { list ->
                _groups.value = list
            }
            _isGroupsLoading.value = false
        }
    }

    fun createCommunityGroup(name: String, description: String, category: String = "general", onDone: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            val res = repository.createGroup(name, description, category)
            res.onSuccess { newGroup ->
                _groups.value = listOf(newGroup) + _groups.value
                setBanner("Community group created!")
                onDone(true, null)
            }.onFailure { e ->
                onDone(false, e.localizedMessage ?: "Failed to create group")
            }
        }
    }

    fun toggleJoinGroup(group: com.example.data.model.GroupCommunity) {
        val currentJoined = group.isJoinedByMe
        val newJoined = !currentJoined
        val newCount = if (newJoined) group.membersCount + 1 else (group.membersCount - 1).coerceAtLeast(1)

        _groups.value = _groups.value.map {
            if (it.id == group.id) it.copy(isJoinedByMe = newJoined, membersCount = newCount) else it
        }

        viewModelScope.launch {
            if (newJoined) {
                repository.joinGroup(group.id)
                setBanner("Joined ${group.name}")
            } else {
                repository.leaveGroup(group.id)
                setBanner("Left ${group.name}")
            }
        }
    }

    private val _selectedGroup = MutableStateFlow<com.example.data.model.GroupCommunity?>(null)
    val selectedGroup: StateFlow<com.example.data.model.GroupCommunity?> = _selectedGroup.asStateFlow()

    private val _groupPosts = MutableStateFlow<List<Post>>(emptyList())
    val groupPosts: StateFlow<List<Post>> = _groupPosts.asStateFlow()

    private val _groupMembers = MutableStateFlow<List<com.example.data.model.GroupMember>>(emptyList())
    val groupMembers: StateFlow<List<com.example.data.model.GroupMember>> = _groupMembers.asStateFlow()

    fun selectGroup(group: com.example.data.model.GroupCommunity?) {
        _selectedGroup.value = group
        if (group != null) {
            loadGroupPosts(group.id)
            loadGroupMembers(group.id)
        } else {
            _groupPosts.value = emptyList()
            _groupMembers.value = emptyList()
        }
    }

    fun loadGroupPosts(groupId: String) {
        viewModelScope.launch {
            val res = repository.getPostsByGroup(groupId)
            res.onSuccess { list ->
                _groupPosts.value = list
            }.onFailure {
                _groupPosts.value = emptyList()
            }
        }
    }

    fun loadGroupMembers(groupId: String) {
        viewModelScope.launch {
            val res = repository.getGroupMembers(groupId)
            res.onSuccess { list ->
                _groupMembers.value = list
            }.onFailure {
                _groupMembers.value = emptyList()
            }
        }
    }

    fun createVideoPostInGroup(
        groupId: String,
        title: String?,
        videoUrl: String,
        embedUrl: String? = null,
        directUrl: String? = null,
        platform: String? = null,
        thumbnailUrl: String? = null,
        durationSeconds: Int? = null,
        width: Int? = null,
        height: Int? = null,
        isVertical: Boolean? = false,
        category: String? = null,
        tags: List<String> = emptyList(),
        onDone: (Boolean, String?) -> Unit
    ) {
        viewModelScope.launch {
            val res = repository.createVideoPost(
                title = title,
                videoUrl = videoUrl,
                embedUrl = embedUrl,
                directUrl = directUrl,
                platform = platform,
                thumbnailUrl = thumbnailUrl,
                durationSeconds = durationSeconds,
                width = width,
                height = height,
                isVertical = isVertical,
                category = category,
                tags = tags,
                groupId = groupId
            )
            res.onSuccess { newPost ->
                _groupPosts.value = listOf(newPost) + _groupPosts.value
                _feedPosts.value = listOf(newPost) + _feedPosts.value
                setBanner("Posted in community!")
                onDone(true, null)
            }.onFailure { e ->
                onDone(false, e.localizedMessage ?: "Failed to post in group")
            }
        }
    }

    fun createPhotoPostInGroup(
        groupId: String,
        caption: String?,
        photoUrls: List<String>,
        category: String?,
        tags: List<String>,
        onDone: (Boolean, String?) -> Unit
    ) {
        viewModelScope.launch {
            val res = repository.createPhotoPost(caption, photoUrls, category, tags, groupId = groupId)
            res.onSuccess { newPost ->
                _groupPosts.value = listOf(newPost) + _groupPosts.value
                _feedPosts.value = listOf(newPost) + _feedPosts.value
                setBanner("Posted in community!")
                onDone(true, null)
            }.onFailure { e ->
                onDone(false, e.localizedMessage ?: "Failed to post in group")
            }
        }
    }

    fun createTextPostInGroup(
        groupId: String,
        caption: String,
        templateId: String?,
        category: String?,
        tags: List<String>,
        onDone: (Boolean, String?) -> Unit
    ) {
        viewModelScope.launch {
            val res = repository.createTextPost(caption, templateId, category, tags, groupId = groupId)
            res.onSuccess { newPost ->
                _groupPosts.value = listOf(newPost) + _groupPosts.value
                _feedPosts.value = listOf(newPost) + _feedPosts.value
                setBanner("Posted in community!")
                onDone(true, null)
            }.onFailure { e ->
                onDone(false, e.localizedMessage ?: "Failed to post in group")
            }
        }
    }

    fun loadNotifications() {
        viewModelScope.launch {
            val res = repository.getNotifications()
            res.onSuccess {
                _notifications.value = it
            }
        }
    }
}

