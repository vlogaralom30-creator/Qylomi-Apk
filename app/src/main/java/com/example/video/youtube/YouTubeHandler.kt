package com.example.video.youtube

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.util.VideoPlayerCache
import com.example.video.ExtractedVideo
import com.example.video.ParsedVideo
import com.example.video.PlayerConfig
import com.example.video.PlayerController
import com.example.video.VideoPlatformHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

/**
 * Robust handler for YouTube videos and YouTube Shorts.
 * Complies strictly with YouTube API Developer Policies:
 * - Uses official IFrame Player API inside WebView.
 * - Does not extract or re-host video streams.
 * - Respects branding and viewport dimensions (minimum 200x200 dp).
 */
class YouTubeHandler : VideoPlatformHandler {

    override val platformId: String = "youtube"
    override val displayName: String = "YouTube"

    companion object {
        private val ID_REGEX = Pattern.compile("^[A-Za-z0-9_-]{11}$")
        private val FIRST_URL_REGEX = Pattern.compile("(https?://[^\\s]+|[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}/[^\\s]*)", Pattern.CASE_INSENSITIVE)

        // Session-scoped in-memory cache for oEmbed and verified thumbnail URLs
        private val oEmbedCache = ConcurrentHashMap<String, ExtractedVideo>()

        private val okHttpClient: OkHttpClient by lazy {
            VideoPlayerCache.okHttpClient.newBuilder()
                .connectTimeout(8, TimeUnit.SECONDS)
                .readTimeout(8, TimeUnit.SECONDS)
                .callTimeout(8, TimeUnit.SECONDS)
                .build()
        }
    }

    override fun matches(url: String): Boolean {
        val lower = url.lowercase().trim()
        return lower.contains("youtube.com") ||
                lower.contains("youtu.be") ||
                lower.contains("youtube-nocookie.com")
    }

    override fun parse(url: String): ParsedVideo? {
        val extractedRaw = extractUrlFromText(url) ?: return null
        val normalized = normalizeUrl(extractedRaw)

        val uri = try {
            Uri.parse(normalized)
        } catch (_: Exception) {
            return null
        }

        val host = uri.host?.lowercase() ?: return null
        val isYtHost = host.endsWith("youtube.com") ||
                host.endsWith("youtu.be") ||
                host.endsWith("youtube-nocookie.com")

        if (!isYtHost) return null

        val path = uri.path ?: ""
        var extractedId: String? = null
        var isShort = false

        when {
            // youtu.be/<ID>
            host.contains("youtu.be") -> {
                val segments = uri.pathSegments
                if (segments.isNotEmpty()) {
                    extractedId = segments[0]
                }
            }

            // /shorts/<ID>
            path.contains("/shorts/") -> {
                isShort = true
                val segments = uri.pathSegments
                val shortsIndex = segments.indexOf("shorts")
                if (shortsIndex != -1 && segments.size > shortsIndex + 1) {
                    extractedId = segments[shortsIndex + 1]
                }
            }

            // /embed/<ID> or /v/<ID> or /live/<ID>
            path.contains("/embed/") || path.contains("/v/") || path.contains("/live/") -> {
                val segments = uri.pathSegments
                if (segments.size >= 2) {
                    extractedId = segments[1]
                }
            }

            // /watch?v=<ID>
            else -> {
                extractedId = uri.getQueryParameter("v")
            }
        }

        // Clean any trailing query parameters or fragments if attached to ID
        extractedId = extractedId?.trimEnd('/')
        if (extractedId != null && extractedId.contains("?")) {
            extractedId = extractedId.substringBefore("?")
        }
        if (extractedId != null && extractedId.contains("&")) {
            extractedId = extractedId.substringBefore("&")
        }
        if (extractedId != null && extractedId.contains("#")) {
            extractedId = extractedId.substringBefore("#")
        }

        // Reject if ID is missing or does NOT match exact 11-char regex
        if (extractedId.isNullOrBlank() || !ID_REGEX.matcher(extractedId).matches()) {
            return null
        }

        // Parse start timestamp: t=90, t=1m30s, start=90, #t=90
        val startSeconds = parseStartSeconds(normalized)

        return ParsedVideo(
            platform = platformId,
            originalUrl = normalized,
            videoId = extractedId,
            isShort = isShort,
            startSeconds = startSeconds
        )
    }

    override suspend fun extract(parsed: ParsedVideo): Result<ExtractedVideo> = withContext(Dispatchers.IO) {
        val videoId = parsed.videoId
        if (videoId.isNullOrBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Invalid YouTube video ID"))
        }

        // Check cache
        oEmbedCache[videoId]?.let { cached ->
            return@withContext Result.success(cached.copy(startSeconds = parsed.startSeconds))
        }

        val canonicalWatchUrl = "https://www.youtube.com/watch?v=$videoId"
        val embedUrl = "https://www.youtube.com/embed/$videoId"
        val maxResThumb = "https://i.ytimg.com/vi/$videoId/maxresdefault.jpg"
        val hqThumb = "https://i.ytimg.com/vi/$videoId/hqdefault.jpg"

        // Check thumbnail availability via HEAD request (fallback to hqdefault if maxres returns 404)
        val verifiedThumbnail = if (checkUrlExists(maxResThumb)) maxResThumb else hqThumb

        // Fetch oEmbed metadata without API key
        val encodedUrl = try {
            URLEncoder.encode(canonicalWatchUrl, "UTF-8")
        } catch (_: Exception) {
            canonicalWatchUrl
        }

        val oembedApiUrl = "https://www.youtube.com/oembed?url=$encodedUrl&format=json"
        val oembedRequest = Request.Builder()
            .url(oembedApiUrl)
            .header("User-Agent", "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36")
            .get()
            .build()

        try {
            val response = okHttpClient.newCall(oembedRequest).execute()
            val code = response.code
            val bodyString = response.body?.string()
            response.close()

            if (code in 200..299 && !bodyString.isNullOrBlank()) {
                val json = JSONObject(bodyString)
                val title = json.optString("title").ifBlank { "YouTube Video" }
                val author = json.optString("author_name").ifBlank { null }
                val oembedThumb = json.optString("thumbnail_url").ifBlank { null }

                val result = ExtractedVideo(
                    platform = platformId,
                    videoId = videoId,
                    originalUrl = parsed.originalUrl,
                    embedUrl = embedUrl,
                    directUrl = null,
                    thumbnailUrl = verifiedThumbnail.ifBlank { oembedThumb },
                    title = title,
                    authorName = author,
                    isVertical = parsed.isShort,
                    isShort = parsed.isShort,
                    startSeconds = parsed.startSeconds,
                    isNativePlayable = false
                )
                oEmbedCache[videoId] = result
                Result.success(result)
            } else if (code == 401 || code == 403 || code == 404) {
                // Video is private, removed, or embedding is restricted
                val fallback = ExtractedVideo(
                    platform = platformId,
                    videoId = videoId,
                    originalUrl = parsed.originalUrl,
                    embedUrl = embedUrl,
                    directUrl = null,
                    thumbnailUrl = verifiedThumbnail,
                    title = "YouTube Video ($videoId)",
                    isVertical = parsed.isShort,
                    isShort = parsed.isShort,
                    startSeconds = parsed.startSeconds,
                    isNativePlayable = false,
                    noticeMessage = "Embedding restricted or video is private/unavailable. Tap to open in YouTube."
                )
                oEmbedCache[videoId] = fallback
                Result.success(fallback)
            } else {
                val fallback = ExtractedVideo(
                    platform = platformId,
                    videoId = videoId,
                    originalUrl = parsed.originalUrl,
                    embedUrl = embedUrl,
                    thumbnailUrl = verifiedThumbnail,
                    title = "YouTube Video",
                    isVertical = parsed.isShort,
                    isShort = parsed.isShort,
                    startSeconds = parsed.startSeconds,
                    isNativePlayable = false
                )
                Result.success(fallback)
            }
        } catch (e: Exception) {
            val fallback = ExtractedVideo(
                platform = platformId,
                videoId = videoId,
                originalUrl = parsed.originalUrl,
                embedUrl = embedUrl,
                thumbnailUrl = verifiedThumbnail,
                title = "YouTube Video",
                isVertical = parsed.isShort,
                isShort = parsed.isShort,
                startSeconds = parsed.startSeconds,
                isNativePlayable = false,
                noticeMessage = "Network timeout fetching metadata. Preview player ready."
            )
            Result.success(fallback)
        }
    }

    @Composable
    override fun Player(
        video: ExtractedVideo,
        modifier: Modifier,
        config: PlayerConfig,
        controller: PlayerController?
    ) {
        YouTubePlayer(
            video = video,
            modifier = modifier,
            config = config,
            controller = controller
        )
    }

    /**
     * Checks if an image URL exists using an HTTP HEAD request.
     */
    private fun checkUrlExists(url: String): Boolean {
        return try {
            val request = Request.Builder()
                .url(url)
                .head()
                .build()
            val response = okHttpClient.newCall(request).execute()
            val success = response.isSuccessful
            response.close()
            success
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Extracts first valid URL found in input text.
     */
    private fun extractUrlFromText(text: String): String? {
        val trimmed = text.trim()
        if (trimmed.isBlank()) return null
        val matcher = FIRST_URL_REGEX.matcher(trimmed)
        return if (matcher.find()) matcher.group(1) else trimmed
    }

    /**
     * Normalizes missing scheme, trailing punctuation, and whitespace.
     */
    private fun normalizeUrl(url: String): String {
        var clean = url.trim()
        if (!clean.startsWith("http://", ignoreCase = true) && !clean.startsWith("https://", ignoreCase = true)) {
            clean = "https://$clean"
        }
        return clean
    }

    /**
     * Parses start timestamp seconds from various YouTube parameter styles:
     * - t=90
     * - t=1m30s
     * - start=90
     * - #t=90
     * - t=1h2m3s
     */
    fun parseStartSeconds(url: String): Int {
        val patterns = listOf(
            "[?&#]t=([0-9hms]+)",
            "[?&#]start=([0-9]+)"
        )

        for (patternStr in patterns) {
            val matcher = Pattern.compile(patternStr, Pattern.CASE_INSENSITIVE).matcher(url)
            if (matcher.find()) {
                val value = matcher.group(1) ?: continue
                return parseTimeStringToSeconds(value)
            }
        }
        return 0
    }

    private fun parseTimeStringToSeconds(timeStr: String): Int {
        // Simple integer seconds (e.g. "90" or "90s")
        if (timeStr.all { it.isDigit() }) {
            return timeStr.toIntOrNull() ?: 0
        }
        val clean = timeStr.lowercase().trimEnd('s')
        if (clean.all { it.isDigit() }) {
            return clean.toIntOrNull() ?: 0
        }

        // HMS format: e.g. 1h2m3s or 2m30s
        var totalSec = 0
        var currentDigits = ""

        for (ch in timeStr.lowercase()) {
            if (ch.isDigit()) {
                currentDigits += ch
            } else {
                val num = currentDigits.toIntOrNull() ?: 0
                currentDigits = ""
                when (ch) {
                    'h' -> totalSec += num * 3600
                    'm' -> totalSec += num * 60
                    's' -> totalSec += num
                }
            }
        }
        if (currentDigits.isNotEmpty()) {
            totalSec += currentDigits.toIntOrNull() ?: 0
        }
        return totalSec
    }
}
