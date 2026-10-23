package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.util.VideoEmbedUtils
import com.example.video.ExtractedVideo
import com.example.video.ParsedVideo
import com.example.video.VideoPlatformRegistry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap
import java.util.regex.Pattern

data class ExtractedVideoResult(
    val platform: String, // "tiktok", "youtube", "facebook", "instagram", "direct"
    val platformDisplayName: String,
    val originalUrl: String,
    val directVideoUrl: String? = null,
    val embedUrl: String,
    val title: String? = null,
    val thumbnailUrl: String? = null,
    val authorName: String? = null,
    val isDirectPlayable: Boolean = false,
    val durationSeconds: Int? = null,
    val width: Int? = null,
    val height: Int? = null,
    val isVertical: Boolean = false,
    val isFallbackEmbed: Boolean = false,
    val noticeMessage: String? = null,
    val videoId: String? = null,
    val isShort: Boolean = false,
    val startSeconds: Int = 0,
    val fileSize: Long? = null,
    val mimeType: String? = null
) {
    fun toExtractedVideo(): ExtractedVideo {
        return ExtractedVideo(
            platform = platform,
            videoId = videoId,
            originalUrl = originalUrl,
            embedUrl = embedUrl,
            directUrl = directVideoUrl,
            thumbnailUrl = thumbnailUrl,
            title = title,
            authorName = authorName,
            isVertical = isVertical,
            isShort = isShort,
            durationSec = durationSeconds,
            startSeconds = startSeconds,
            isNativePlayable = isDirectPlayable,
            noticeMessage = noticeMessage
        )
    }
}

sealed interface VideoExtractionState {
    object Idle : VideoExtractionState
    object Extracting : VideoExtractionState
    data class Success(val result: ExtractedVideoResult) : VideoExtractionState
    data class Error(val message: String, val fallbackResult: ExtractedVideoResult) : VideoExtractionState
}

object TikTokProviderConfig {
    const val PRIMARY_API_BASE = "https://www.tikwm.com/api/"
    val FALLBACK_API_BASES = listOf(
        "https://api.tikwm.com/api/"
    )
}

object DirectVideoCache {
    private val cache = ConcurrentHashMap<String, Pair<String, Long>>()
    private const val TTL_MS = 30 * 60 * 1000L // 30 minutes

    fun get(originalUrl: String): String? {
        val entry = cache[originalUrl] ?: return null
        if (System.currentTimeMillis() - entry.second > TTL_MS) {
            cache.remove(originalUrl)
            return null
        }
        return entry.first
    }

    fun put(originalUrl: String, directUrl: String) {
        cache[originalUrl] = directUrl to System.currentTimeMillis()
    }
}

class VideoExtractorViewModel : ViewModel() {

    private val _uiState = MutableStateFlow<VideoExtractionState>(VideoExtractionState.Idle)
    val uiState: StateFlow<VideoExtractionState> = _uiState.asStateFlow()

    fun reset() {
        _uiState.value = VideoExtractionState.Idle
    }

    fun extractVideo(rawInput: String) {
        val url = extractFirstUrl(rawInput).trim()
        if (url.isBlank()) {
            _uiState.value = VideoExtractionState.Idle
            return
        }

        _uiState.value = VideoExtractionState.Extracting

        viewModelScope.launch {
            try {
                val extracted = withContext(Dispatchers.IO) {
                    val handler = VideoPlatformRegistry.resolve(url)
                    val parsed = handler.parse(url) ?: ParsedVideo(handler.platformId, url)
                    handler.extract(parsed).getOrThrow()
                }

                val handler = VideoPlatformRegistry.resolve(url)
                val result = ExtractedVideoResult(
                    platform = extracted.platform,
                    platformDisplayName = if (extracted.isShort && extracted.platform == "youtube") "YouTube Shorts" else handler.displayName,
                    originalUrl = extracted.originalUrl,
                    directVideoUrl = extracted.directUrl,
                    embedUrl = extracted.embedUrl,
                    title = extracted.title,
                    thumbnailUrl = extracted.thumbnailUrl,
                    authorName = extracted.authorName,
                    isDirectPlayable = extracted.isNativePlayable,
                    durationSeconds = extracted.durationSec,
                    isVertical = extracted.isVertical,
                    isFallbackEmbed = !extracted.isNativePlayable && extracted.platform != "youtube",
                    noticeMessage = extracted.noticeMessage,
                    videoId = extracted.videoId,
                    isShort = extracted.isShort,
                    startSeconds = extracted.startSeconds,
                    fileSize = extracted.fileSize,
                    mimeType = extracted.mimeType
                )
                _uiState.value = VideoExtractionState.Success(result)
            } catch (e: Exception) {
                val rawMsg = e.localizedMessage ?: "Failed to extract video details"
                val displayMsg = rawMsg.substringAfter("|")
                val errorCode = if (rawMsg.contains("|")) rawMsg.substringBefore("|") else null
                
                val detected = VideoEmbedUtils.parseVideoUrl(url)
                val fallback = ExtractedVideoResult(
                    platform = detected.platform,
                    platformDisplayName = detected.platformDisplayName,
                    originalUrl = url,
                    embedUrl = detected.embedUrl,
                    isDirectPlayable = false,
                    isFallbackEmbed = true,
                    noticeMessage = displayMsg
                )
                _uiState.value = VideoExtractionState.Error(
                    message = displayMsg,
                    fallbackResult = fallback
                )
            }
        }
    }

    suspend fun reExtractDirectUrl(originalUrl: String): String? = withContext(Dispatchers.IO) {
        DirectVideoCache.get(originalUrl)?.let { return@withContext it }
        try {
            val handler = VideoPlatformRegistry.resolve(originalUrl)
            val parsed = handler.parse(originalUrl) ?: ParsedVideo(handler.platformId, originalUrl)
            val res = handler.extract(parsed).getOrNull()
            if (res?.directUrl != null) {
                DirectVideoCache.put(originalUrl, res.directUrl)
                res.directUrl
            } else {
                null
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun extractFirstUrl(text: String): String {
        val pattern = Pattern.compile("https?://[^\\s]+", Pattern.CASE_INSENSITIVE)
        val matcher = pattern.matcher(text)
        return if (matcher.find()) matcher.group() else text
    }
}
