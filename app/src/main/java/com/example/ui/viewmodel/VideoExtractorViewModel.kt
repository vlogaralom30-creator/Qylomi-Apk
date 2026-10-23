package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.util.VideoEmbedUtils
import com.example.util.VideoPlayerCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
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
    val noticeMessage: String? = null
)

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
                val result = withContext(Dispatchers.IO) {
                    performExtraction(url)
                }
                _uiState.value = VideoExtractionState.Success(result)
            } catch (e: Exception) {
                val detected = VideoEmbedUtils.parseVideoUrl(url)
                val fallback = ExtractedVideoResult(
                    platform = detected.platform,
                    platformDisplayName = detected.platformDisplayName,
                    originalUrl = url,
                    embedUrl = detected.embedUrl,
                    isDirectPlayable = false,
                    isFallbackEmbed = true,
                    noticeMessage = "Could not extract direct stream. Using web embed fallback."
                )
                _uiState.value = VideoExtractionState.Error(
                    message = e.localizedMessage ?: "Failed to extract video details",
                    fallbackResult = fallback
                )
            }
        }
    }

    suspend fun reExtractDirectUrl(originalUrl: String): String? = withContext(Dispatchers.IO) {
        DirectVideoCache.get(originalUrl)?.let { return@withContext it }
        try {
            val res = performExtraction(originalUrl)
            if (res.directVideoUrl != null) {
                DirectVideoCache.put(originalUrl, res.directVideoUrl)
                res.directVideoUrl
            } else {
                null
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun performExtraction(url: String): ExtractedVideoResult {
        // Check cache first
        DirectVideoCache.get(url)?.let { cachedDirect ->
            val detected = VideoEmbedUtils.parseVideoUrl(url)
            return ExtractedVideoResult(
                platform = detected.platform,
                platformDisplayName = detected.platformDisplayName,
                originalUrl = url,
                directVideoUrl = cachedDirect,
                embedUrl = detected.embedUrl,
                isDirectPlayable = true,
                isVertical = true
            )
        }

        val detected = VideoEmbedUtils.parseVideoUrl(url)

        return when (detected.platform) {
            "tiktok" -> extractTikTokVideo(url, detected.embedUrl)
            "youtube" -> extractYouTubeVideo(url, detected.embedUrl)
            "instagram" -> extractInstagramVideo(url, detected.embedUrl)
            "facebook" -> extractFacebookVideo(url, detected.embedUrl)
            else -> extractDirectVideo(url)
        }
    }

    // 1. TikTok Extraction via TikWM API (HD/play no-watermark)
    private fun extractTikTokVideo(originalUrl: String, defaultEmbedUrl: String): ExtractedVideoResult {
        val resolvedUrl = followUrlRedirects(originalUrl)

        // Try primary provider, then fallbacks
        val providers = listOf(TikTokProviderConfig.PRIMARY_API_BASE) + TikTokProviderConfig.FALLBACK_API_BASES

        for (apiBase in providers) {
            try {
                val encoded = URLEncoder.encode(resolvedUrl, "UTF-8")
                val apiUrl = "$apiBase?url=$encoded"
                val jsonStr = fetchUrlText(apiUrl) ?: continue
                val json = JSONObject(jsonStr)

                if (json.optInt("code") == 0) {
                    val data = json.getJSONObject("data")

                    // Prefer hdplay, else play. NEVER use wmplay.
                    var playUrl = data.optString("hdplay")
                    if (playUrl.isBlank()) {
                        playUrl = data.optString("play")
                    }

                    if (playUrl.isNotBlank()) {
                        if (playUrl.startsWith("/")) {
                            playUrl = "https://www.tikwm.com$playUrl"
                        }
                    }

                    var cover = data.optString("cover")
                    if (cover.startsWith("/")) {
                        cover = "https://www.tikwm.com$cover"
                    }

                    val title = data.optString("title").ifBlank { "TikTok Video" }
                    val authorObj = data.optJSONObject("author")
                    val author = authorObj?.optString("nickname") ?: authorObj?.optString("unique_id")
                    val duration = data.optInt("duration", 0)

                    // Dimension check
                    val width = data.optInt("width", 0).takeIf { it > 0 }
                    val height = data.optInt("height", 0).takeIf { it > 0 }
                    val isVertical = if (width != null && height != null) height > width else true

                    if (playUrl.isNotBlank()) {
                        DirectVideoCache.put(originalUrl, playUrl)
                        return ExtractedVideoResult(
                            platform = "tiktok",
                            platformDisplayName = "TikTok",
                            originalUrl = originalUrl,
                            directVideoUrl = playUrl,
                            embedUrl = defaultEmbedUrl,
                            title = title,
                            thumbnailUrl = cover.ifBlank { null },
                            authorName = author,
                            isDirectPlayable = true,
                            durationSeconds = duration.takeIf { it > 0 },
                            width = width,
                            height = height,
                            isVertical = isVertical
                        )
                    }
                }
            } catch (_: Exception) {
                // Try next provider
            }
        }

        // Fallback to TikTok embed
        return ExtractedVideoResult(
            platform = "tiktok",
            platformDisplayName = "TikTok",
            originalUrl = originalUrl,
            directVideoUrl = null,
            embedUrl = defaultEmbedUrl,
            title = "TikTok Video",
            isDirectPlayable = false,
            isFallbackEmbed = true,
            isVertical = true,
            noticeMessage = "Direct video extraction failed; showing TikTok web embed instead."
        )
    }

    // 2. YouTube Metadata Extraction via oEmbed
    private fun extractYouTubeVideo(originalUrl: String, defaultEmbedUrl: String): ExtractedVideoResult {
        return try {
            val encoded = URLEncoder.encode(originalUrl, "UTF-8")
            val oembedUrl = "https://www.youtube.com/oembed?url=$encoded&format=json"
            val jsonStr = fetchUrlText(oembedUrl)

            if (!jsonStr.isNullOrBlank()) {
                val json = JSONObject(jsonStr)
                val title = json.optString("title")
                val author = json.optString("author_name")
                val thumbnail = json.optString("thumbnail_url")
                val isShorts = originalUrl.contains("/shorts/", ignoreCase = true)

                ExtractedVideoResult(
                    platform = "youtube",
                    platformDisplayName = if (isShorts) "YouTube Shorts" else "YouTube",
                    originalUrl = originalUrl,
                    directVideoUrl = null,
                    embedUrl = defaultEmbedUrl,
                    title = title.ifBlank { "YouTube Video" },
                    thumbnailUrl = thumbnail.ifBlank { null },
                    authorName = author,
                    isDirectPlayable = false,
                    isVertical = isShorts
                )
            } else {
                ExtractedVideoResult(
                    platform = "youtube",
                    platformDisplayName = "YouTube",
                    originalUrl = originalUrl,
                    embedUrl = defaultEmbedUrl,
                    title = "YouTube Video",
                    isDirectPlayable = false
                )
            }
        } catch (_: Exception) {
            ExtractedVideoResult(
                platform = "youtube",
                platformDisplayName = "YouTube",
                originalUrl = originalUrl,
                embedUrl = defaultEmbedUrl,
                title = "YouTube Video",
                isDirectPlayable = false
            )
        }
    }

    // 3. Instagram Video Embed
    private fun extractInstagramVideo(originalUrl: String, defaultEmbedUrl: String): ExtractedVideoResult {
        return ExtractedVideoResult(
            platform = "instagram",
            platformDisplayName = "Instagram",
            originalUrl = originalUrl,
            directVideoUrl = null,
            embedUrl = defaultEmbedUrl,
            title = "Instagram Post",
            isDirectPlayable = false,
            isVertical = true
        )
    }

    // 4. Facebook Video Embed
    private fun extractFacebookVideo(originalUrl: String, defaultEmbedUrl: String): ExtractedVideoResult {
        return ExtractedVideoResult(
            platform = "facebook",
            platformDisplayName = "Facebook Video",
            originalUrl = originalUrl,
            directVideoUrl = null,
            embedUrl = defaultEmbedUrl,
            title = "Facebook Video",
            isDirectPlayable = false
        )
    }

    // 5. Direct Stream (.mp4, .webm, .m3u8, .mov)
    private fun extractDirectVideo(originalUrl: String): ExtractedVideoResult {
        val clean = originalUrl.lowercase()
        val isDirectStream = clean.contains(".mp4") ||
                clean.contains(".webm") ||
                clean.contains(".m3u8") ||
                clean.contains(".mov")

        if (isDirectStream) {
            DirectVideoCache.put(originalUrl, originalUrl)
        }

        return ExtractedVideoResult(
            platform = "direct",
            platformDisplayName = if (isDirectStream) "Direct Video File" else "Web Video Link",
            originalUrl = originalUrl,
            directVideoUrl = if (isDirectStream) originalUrl else null,
            embedUrl = originalUrl,
            title = "Direct Video Stream",
            isDirectPlayable = isDirectStream
        )
    }

    private fun followUrlRedirects(url: String): String {
        return try {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Mobile Safari/537.36")
                .head()
                .build()

            val response = VideoPlayerCache.okHttpClient.newCall(request).execute()
            val finalUrl = response.request.url.toString()
            response.close()
            finalUrl
        } catch (_: Exception) {
            url
        }
    }

    private fun fetchUrlText(urlString: String): String? {
        return try {
            val request = Request.Builder()
                .url(urlString)
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Mobile Safari/537.36")
                .get()
                .build()

            val response = VideoPlayerCache.okHttpClient.newCall(request).execute()
            val body = response.body?.string()
            response.close()
            body
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
