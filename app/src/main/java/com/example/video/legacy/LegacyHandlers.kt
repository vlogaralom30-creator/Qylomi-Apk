package com.example.video.legacy

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.example.ui.components.EmbedVideoPlayer
import com.example.ui.components.NativeVideoPlayer
import com.example.ui.viewmodel.DirectVideoCache
import com.example.ui.viewmodel.TikTokProviderConfig
import com.example.util.VideoPlayerCache
import com.example.video.ExtractedVideo
import com.example.video.ParsedVideo
import com.example.video.PlayerConfig
import com.example.video.PlayerController
import com.example.video.VideoPlatformHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
import java.util.regex.Pattern

/**
 * Legacy handler wrapping existing TikTok logic and TikWM direct video extractor.
 */
class TikTokLegacyHandler : VideoPlatformHandler {
    override val platformId: String = "tiktok"
    override val displayName: String = "TikTok"

    override fun matches(url: String): Boolean {
        return url.contains("tiktok.com", ignoreCase = true)
    }

    override fun parse(url: String): ParsedVideo? {
        if (!matches(url)) return null
        val pattern = Pattern.compile("video/(\\d+)", Pattern.CASE_INSENSITIVE)
        val matcher = pattern.matcher(url)
        val id = if (matcher.find()) matcher.group(1) else null
        return ParsedVideo(
            platform = platformId,
            originalUrl = url.trim(),
            videoId = id,
            isShort = true
        )
    }

    override suspend fun extract(parsed: ParsedVideo): Result<ExtractedVideo> = withContext(Dispatchers.IO) {
        val originalUrl = parsed.originalUrl
        val defaultEmbedUrl = parsed.videoId?.let { "https://www.tiktok.com/embed/v2/$it" } ?: originalUrl

        // Check in-memory cache first
        DirectVideoCache.get(originalUrl)?.let { cachedDirect ->
            return@withContext Result.success(
                ExtractedVideo(
                    platform = platformId,
                    videoId = parsed.videoId,
                    originalUrl = originalUrl,
                    embedUrl = defaultEmbedUrl,
                    directUrl = cachedDirect,
                    isNativePlayable = true,
                    isVertical = true
                )
            )
        }

        val resolvedUrl = followUrlRedirects(originalUrl)
        val providers = listOf(TikTokProviderConfig.PRIMARY_API_BASE) + TikTokProviderConfig.FALLBACK_API_BASES

        for (apiBase in providers) {
            try {
                val encoded = URLEncoder.encode(resolvedUrl, "UTF-8")
                val apiUrl = "$apiBase?url=$encoded"
                val jsonStr = fetchUrlText(apiUrl) ?: continue
                val json = JSONObject(jsonStr)

                if (json.optInt("code") == 0) {
                    val data = json.getJSONObject("data")
                    var playUrl = data.optString("hdplay")
                    if (playUrl.isBlank()) {
                        playUrl = data.optString("play")
                    }

                    if (playUrl.isNotBlank() && playUrl.startsWith("/")) {
                        playUrl = "https://www.tikwm.com$playUrl"
                    }

                    var cover = data.optString("cover")
                    if (cover.startsWith("/")) {
                        cover = "https://www.tikwm.com$cover"
                    }

                    val title = data.optString("title").ifBlank { "TikTok Video" }
                    val authorObj = data.optJSONObject("author")
                    val author = authorObj?.optString("nickname") ?: authorObj?.optString("unique_id")
                    val duration = data.optInt("duration", 0)

                    val width = data.optInt("width", 0).takeIf { it > 0 }
                    val height = data.optInt("height", 0).takeIf { it > 0 }
                    val isVertical = if (width != null && height != null) height > width else true

                    if (playUrl.isNotBlank()) {
                        DirectVideoCache.put(originalUrl, playUrl)
                        return@withContext Result.success(
                            ExtractedVideo(
                                platform = platformId,
                                videoId = parsed.videoId,
                                originalUrl = originalUrl,
                                embedUrl = defaultEmbedUrl,
                                directUrl = playUrl,
                                thumbnailUrl = cover.ifBlank { null },
                                title = title,
                                authorName = author,
                                durationSec = duration.takeIf { it > 0 },
                                isVertical = isVertical,
                                isShort = true,
                                isNativePlayable = true
                            )
                        )
                    }
                }
            } catch (_: Exception) {}
        }

        // Fallback to embed
        Result.success(
            ExtractedVideo(
                platform = platformId,
                videoId = parsed.videoId,
                originalUrl = originalUrl,
                embedUrl = defaultEmbedUrl,
                title = "TikTok Video",
                isVertical = true,
                isShort = true,
                isNativePlayable = false,
                noticeMessage = "Direct video extraction failed; showing TikTok web embed instead."
            )
        )
    }

    @Composable
    override fun Player(
        video: ExtractedVideo,
        modifier: Modifier,
        config: PlayerConfig,
        controller: PlayerController?
    ) {
        if (video.isNativePlayable && !video.directUrl.isNullOrBlank()) {
            NativeVideoPlayer(
                videoUrl = video.directUrl,
                thumbnailUrl = video.thumbnailUrl,
                isVertical = video.isVertical,
                autoPlay = config.autoPlay,
                loop = config.loop,
                muted = config.muted,
                modifier = modifier
            )
        } else {
            EmbedVideoPlayer(
                videoUrl = video.embedUrl,
                modifier = modifier,
                title = video.title
            )
        }
    }

    private fun followUrlRedirects(url: String): String {
        return try {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36")
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
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36")
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
}

/**
 * Legacy handler wrapping existing Instagram embed behavior.
 */
class InstagramLegacyHandler : VideoPlatformHandler {
    override val platformId: String = "instagram"
    override val displayName: String = "Instagram"

    override fun matches(url: String): Boolean {
        return url.contains("instagram.com", ignoreCase = true) ||
                url.contains("instagr.am", ignoreCase = true)
    }

    override fun parse(url: String): ParsedVideo? {
        if (!matches(url)) return null
        return ParsedVideo(
            platform = platformId,
            originalUrl = url.trim(),
            isShort = true
        )
    }

    override suspend fun extract(parsed: ParsedVideo): Result<ExtractedVideo> = withContext(Dispatchers.IO) {
        val cleanUrl = parsed.originalUrl.split("?").firstOrNull() ?: parsed.originalUrl
        val embed = if (cleanUrl.endsWith("/")) "${cleanUrl}embed" else "$cleanUrl/embed"
        Result.success(
            ExtractedVideo(
                platform = platformId,
                originalUrl = parsed.originalUrl,
                embedUrl = embed,
                title = "Instagram Post",
                isVertical = true,
                isShort = true,
                isNativePlayable = false
            )
        )
    }

    @Composable
    override fun Player(
        video: ExtractedVideo,
        modifier: Modifier,
        config: PlayerConfig,
        controller: PlayerController?
    ) {
        EmbedVideoPlayer(
            videoUrl = video.embedUrl,
            modifier = modifier,
            title = video.title ?: "Instagram Post"
        )
    }
}

/**
 * Legacy handler wrapping existing Facebook video embed behavior.
 */
class FacebookLegacyHandler : VideoPlatformHandler {
    override val platformId: String = "facebook"
    override val displayName: String = "Facebook Video"

    override fun matches(url: String): Boolean {
        return url.contains("facebook.com", ignoreCase = true) ||
                url.contains("fb.watch", ignoreCase = true) ||
                url.contains("fb.com", ignoreCase = true)
    }

    override fun parse(url: String): ParsedVideo? {
        if (!matches(url)) return null
        return ParsedVideo(
            platform = platformId,
            originalUrl = url.trim(),
            isShort = false
        )
    }

    override suspend fun extract(parsed: ParsedVideo): Result<ExtractedVideo> = withContext(Dispatchers.IO) {
        val encoded = try {
            URLEncoder.encode(parsed.originalUrl, "UTF-8")
        } catch (_: Exception) {
            parsed.originalUrl
        }
        val embed = "https://www.facebook.com/plugins/video.php?href=$encoded&show_text=0"
        Result.success(
            ExtractedVideo(
                platform = platformId,
                originalUrl = parsed.originalUrl,
                embedUrl = embed,
                title = "Facebook Video",
                isVertical = false,
                isNativePlayable = false
            )
        )
    }

    @Composable
    override fun Player(
        video: ExtractedVideo,
        modifier: Modifier,
        config: PlayerConfig,
        controller: PlayerController?
    ) {
        EmbedVideoPlayer(
            videoUrl = video.embedUrl,
            modifier = modifier,
            title = video.title ?: "Facebook Video"
        )
    }
}

/**
 * Legacy handler for direct video streams (.mp4, .webm, .m3u8, .mov) or generic web video links.
 */
class DirectLegacyHandler : VideoPlatformHandler {
    override val platformId: String = "direct"
    override val displayName: String = "Video Stream"

    override fun matches(url: String): Boolean = true

    override fun parse(url: String): ParsedVideo? {
        val clean = url.trim()
        if (clean.isBlank()) return null
        return ParsedVideo(
            platform = platformId,
            originalUrl = clean,
            isShort = false
        )
    }

    override suspend fun extract(parsed: ParsedVideo): Result<ExtractedVideo> = withContext(Dispatchers.IO) {
        val lower = parsed.originalUrl.lowercase()
        val isStream = lower.contains(".mp4") ||
                lower.contains(".webm") ||
                lower.contains(".m3u8") ||
                lower.contains(".mov")

        Result.success(
            ExtractedVideo(
                platform = platformId,
                originalUrl = parsed.originalUrl,
                embedUrl = parsed.originalUrl,
                directUrl = if (isStream) parsed.originalUrl else null,
                title = if (isStream) "Direct Video File" else "Web Video Link",
                isVertical = false,
                isNativePlayable = isStream
            )
        )
    }

    @Composable
    override fun Player(
        video: ExtractedVideo,
        modifier: Modifier,
        config: PlayerConfig,
        controller: PlayerController?
    ) {
        if (video.isNativePlayable && !video.directUrl.isNullOrBlank()) {
            NativeVideoPlayer(
                videoUrl = video.directUrl,
                thumbnailUrl = video.thumbnailUrl,
                isVertical = video.isVertical,
                autoPlay = config.autoPlay,
                loop = config.loop,
                muted = config.muted,
                modifier = modifier
            )
        } else {
            EmbedVideoPlayer(
                videoUrl = video.embedUrl,
                modifier = modifier,
                title = video.title ?: "Direct Video"
            )
        }
    }
}
