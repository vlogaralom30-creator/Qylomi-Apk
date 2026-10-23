package com.example.video

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Parsed representation of a video URL.
 */
data class ParsedVideo(
    val platform: String,
    val originalUrl: String,
    val videoId: String? = null,
    val isShort: Boolean = false,
    val startSeconds: Int = 0
)

/**
 * Metadata and playout information for an extracted video.
 */
data class ExtractedVideo(
    val platform: String,
    val videoId: String? = null,
    val originalUrl: String,
    val embedUrl: String,
    val directUrl: String? = null,
    val thumbnailUrl: String? = null,
    val title: String? = null,
    val authorName: String? = null,
    val isVertical: Boolean = false,
    val isShort: Boolean = false,
    val durationSec: Int? = null,
    val startSeconds: Int = 0,
    val isNativePlayable: Boolean = false,
    val noticeMessage: String? = null,
    val requestHeaders: Map<String, String>? = null,
    val fileSize: Long? = null,
    val mimeType: String? = null
)

/**
 * Playback configuration options for video players.
 */
data class PlayerConfig(
    val autoPlay: Boolean = false,
    val muted: Boolean = false,
    val loop: Boolean = false,
    val showControls: Boolean = true,
    val nativeOnly: Boolean = false
)

/**
 * Programmatic controller for controlling video playback across platforms.
 */
interface PlayerController {
    fun play()
    fun pause()
    fun mute()
    fun unMute()
    fun seekTo(sec: Float)
    fun release()
}

/**
 * Standard interface for platform-specific video handlers.
 */
interface VideoPlatformHandler {
    val platformId: String
    val displayName: String

    fun matches(url: String): Boolean
    fun parse(url: String): ParsedVideo?
    suspend fun extract(parsed: ParsedVideo): Result<ExtractedVideo>

    @Composable
    fun Player(
        video: ExtractedVideo,
        modifier: Modifier,
        config: PlayerConfig,
        controller: PlayerController?
    )
}
