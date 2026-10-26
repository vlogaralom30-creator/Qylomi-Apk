package com.example.video.youtube

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Singleton manager ensuring only one active YouTube WebView plays app-wide.
 * When a new player starts, any previous player is automatically paused/released.
 */
object YouTubePlayerManager {
    /**
     * Whether YouTube videos in the feed should auto-play when visible.
     * Toggled via Settings.
     */
    var AUTOPLAY_YOUTUBE_IN_FEED: Boolean by mutableStateOf(false)

    private val _activePlayerId = MutableStateFlow<String?>(null)
    val activePlayerId: StateFlow<String?> = _activePlayerId.asStateFlow()

    fun requestActive(playerId: String) {
        _activePlayerId.value = playerId
    }

    fun release(playerId: String) {
        if (_activePlayerId.value == playerId) {
            _activePlayerId.value = null
        }
    }

    fun isActive(playerId: String): Boolean {
        return _activePlayerId.value == playerId
    }
}
