package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.Post
import com.example.data.repository.SocialRepository
import com.example.util.VideoEmbedUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ReelsViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val repository: SocialRepository = SocialRepository(application)

    private val _reels = MutableStateFlow<List<Post>>(emptyList())
    val reels: StateFlow<List<Post>> = _reels.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _isMuted = MutableStateFlow(false)
    val isMuted: StateFlow<Boolean> = _isMuted.asStateFlow()

    private val _currentSettledIndex = MutableStateFlow(0)
    val currentSettledIndex: StateFlow<Int> = _currentSettledIndex.asStateFlow()

    init {
        loadReels()
    }

    fun loadReels() {
        viewModelScope.launch {
            _isLoading.value = true
            val res = repository.getReels()
            res.onSuccess { list ->
                _reels.value = list
                // Pre-resolve direct URLs if needed
                list.take(3).forEach { resolveDirectStreamIfNeeded(it) }
            }
            _isLoading.value = false
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            val res = repository.getReels()
            res.onSuccess { list ->
                _reels.value = list
                list.take(3).forEach { resolveDirectStreamIfNeeded(it) }
            }
            _isRefreshing.value = false
        }
    }

    fun setInitialPost(post: Post) {
        val currentList = _reels.value.toMutableList()
        val index = currentList.indexOfFirst { it.id == post.id }
        if (index >= 0) {
            _currentSettledIndex.value = index
        } else {
            currentList.add(0, post)
            _reels.value = currentList
            _currentSettledIndex.value = 0
            resolveDirectStreamIfNeeded(post)
        }
    }

    fun setSettledIndex(index: Int) {
        _currentSettledIndex.value = index
        val list = _reels.value
        // Resolve stream for current and next reel in advance
        if (index in list.indices) {
            resolveDirectStreamIfNeeded(list[index])
        }
        if (index + 1 in list.indices) {
            resolveDirectStreamIfNeeded(list[index + 1])
        }
    }

    fun toggleMute() {
        _isMuted.value = !_isMuted.value
    }

    fun toggleLike(post: Post) {
        val currentIsLiked = post.isLikedByMe
        val newIsLiked = !currentIsLiked
        val newCount = if (newIsLiked) post.appreciationsCount + 1 else (post.appreciationsCount - 1).coerceAtLeast(0)

        _reels.value = _reels.value.map {
            if (it.id == post.id) it.copy(isLikedByMe = newIsLiked, appreciationsCount = newCount) else it
        }

        viewModelScope.launch {
            repository.toggleLike(post.id)
        }
    }

    fun toggleBookmark(post: Post) {
        val currentIsSaved = post.isBookmarkedByMe
        val newIsSaved = !currentIsSaved

        _reels.value = _reels.value.map {
            if (it.id == post.id) it.copy(isBookmarkedByMe = newIsSaved) else it
        }

        viewModelScope.launch {
            repository.toggleBookmark(post.id)
        }
    }

    private var prefetchJob: kotlinx.coroutines.Job? = null

    fun updatePrefetchWindow(currentIndex: Int, context: android.content.Context) {
        val list = _reels.value
        if (list.isEmpty()) return

        val connection = com.example.util.NetworkUtils.getConnectionType(context)
        val windowSize = when (connection) {
            com.example.util.NetworkUtils.ConnectionType.WIFI -> 7
            com.example.util.NetworkUtils.ConnectionType.MOBILE -> 3
            com.example.util.NetworkUtils.ConnectionType.OFFLINE_OR_DATA_SAVER -> 0
        }

        prefetchJob?.cancel()
        if (windowSize == 0) return

        prefetchJob = viewModelScope.launch(Dispatchers.IO) {
            // Keep i - 1 warm
            if (currentIndex - 1 in list.indices) {
                prefetchSingleItem(list[currentIndex - 1])
            }

            // Prefetch i + 1 to i + windowSize nearest-first
            for (offset in 1..windowSize) {
                val targetIndex = currentIndex + offset
                if (targetIndex in list.indices) {
                    prefetchSingleItem(list[targetIndex])
                }
            }
        }
    }

    private suspend fun prefetchSingleItem(post: Post) {
        val details = post.videoDetails
        val sourceUrl = details?.videoUrl ?: post.externalUrl ?: return
        val parsed = VideoEmbedUtils.parseVideoUrl(sourceUrl)
        if (parsed.platform == "tiktok") {
            try {
                val key = com.example.player.resolver.MediaKey(url = sourceUrl, platform = "tiktok")
                com.example.player.resolver.MediaResolver.resolve(key, force = false, isHighPriority = false)
            } catch (_: Exception) {}
        }
    }

    private fun resolveDirectStreamIfNeeded(post: Post) {
        val details = post.videoDetails
        val sourceUrl = details?.videoUrl ?: post.externalUrl ?: return
        val directUrl = details?.directUrl

        val isExpiredOrMissing = directUrl.isNullOrBlank() || DirectVideoCache.isExpired(sourceUrl)
        if (!isExpiredOrMissing) return

        val parsed = VideoEmbedUtils.parseVideoUrl(sourceUrl)
        if (parsed.platform == "tiktok" || parsed.platform == "gdrive") {
            viewModelScope.launch(Dispatchers.IO) {
                try {
                    DirectVideoCache.invalidate(sourceUrl)
                    val handler = com.example.video.VideoPlatformRegistry.resolve(sourceUrl)
                    val p = handler.parse(sourceUrl) ?: com.example.video.ParsedVideo(handler.platformId, sourceUrl)
                    val res = handler.extract(p).getOrNull()
                    
                    if (res != null) {
                        updatePostMetadataInMemory(post.id, res)
                    }
                } catch (_: Exception) {}
            }
        }
    }

    private suspend fun updatePostMetadataInMemory(postId: String, extracted: com.example.video.ExtractedVideo) = withContext(Dispatchers.Main) {
        _reels.value = _reels.value.map { p ->
            if (p.id == postId) {
                val details = p.videoDetails
                val updatedVp = details?.copy(
                    directUrl = extracted.directUrl ?: details.directUrl,
                    isVertical = extracted.isVertical,
                    thumbnailUrl = extracted.thumbnailUrl ?: details.thumbnailUrl,
                    title = extracted.title ?: details.title,
                    durationSeconds = extracted.durationSec ?: details.durationSeconds
                )
                p.copy(videoPosts = if (updatedVp != null) listOf(updatedVp) else p.videoPosts)
            } else p
        }
    }
}
