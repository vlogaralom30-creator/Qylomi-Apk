package com.example.video.legacy

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import kotlinx.coroutines.launch
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
        val lower = url.lowercase()
        return lower.contains("tiktok.com") || lower.contains("tikwm.com") || lower.contains("tiklink")
    }

    override fun parse(url: String): ParsedVideo? {
        if (!matches(url)) return null
        val id = extractVideoIdFromUrl(url)
        return ParsedVideo(
            platform = platformId,
            originalUrl = url.trim(),
            videoId = id,
            isShort = true
        )
    }

    override suspend fun extract(parsed: ParsedVideo): Result<ExtractedVideo> = withContext(Dispatchers.IO) {
        val originalUrl = parsed.originalUrl
        val resolvedUrl = followUrlRedirects(originalUrl)
        val resolvedVideoId = parsed.videoId ?: extractVideoIdFromUrl(resolvedUrl)
        val defaultEmbedUrl = resolvedVideoId?.let { "https://www.tiktok.com/embed/v2/$it" } ?: resolvedUrl

        // Check in-memory cache first if not expired
        if (!DirectVideoCache.isExpired(originalUrl)) {
            DirectVideoCache.get(originalUrl)?.let { cachedDirect ->
                return@withContext Result.success(
                    ExtractedVideo(
                        platform = platformId,
                        videoId = resolvedVideoId,
                        originalUrl = originalUrl,
                        embedUrl = defaultEmbedUrl,
                        directUrl = cachedDirect,
                        isNativePlayable = true,
                        isVertical = true
                    )
                )
            }
        } else {
            DirectVideoCache.invalidate(originalUrl)
        }

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
                                videoId = resolvedVideoId,
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
                videoId = resolvedVideoId,
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

    private fun extractVideoIdFromUrl(url: String): String? {
        val pattern = Pattern.compile("(?:video/|v/|/embed/v2/|/photo/)(\\d+)", Pattern.CASE_INSENSITIVE)
        val matcher = pattern.matcher(url)
        if (matcher.find()) return matcher.group(1)
        val fallbackPattern = Pattern.compile("/(\\d{15,20})", Pattern.CASE_INSENSITIVE)
        val fallbackMatcher = fallbackPattern.matcher(url)
        if (fallbackMatcher.find()) return fallbackMatcher.group(1)
        return null
    }

    @Composable
    override fun Player(
        video: ExtractedVideo,
        modifier: Modifier,
        config: PlayerConfig,
        controller: PlayerController?
    ) {
        TikTokSmartPlayer(
            video = video,
            modifier = modifier,
            config = config
        )
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

@Composable
private fun TikTokSmartPlayer(
    video: ExtractedVideo,
    modifier: Modifier,
    config: PlayerConfig
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val key = remember(video.originalUrl) { com.example.player.resolver.MediaKey(url = video.originalUrl, platform = "tiktok") }

    var resolveState by remember(key) { mutableStateOf<com.example.player.resolver.MediaResolveState>(com.example.player.resolver.MediaResolveState.Idle) }
    var recoveryCount by remember(key) { androidx.compose.runtime.mutableIntStateOf(0) }

    LaunchedEffect(key) {
        val cached = com.example.player.resolver.MediaResolver.getCached(key)
        if (cached != null) {
            resolveState = com.example.player.resolver.MediaResolveState.Ready(cached)
        } else {
            resolveState = com.example.player.resolver.MediaResolveState.Resolving
            try {
                val media = com.example.player.resolver.MediaResolver.resolve(key, isHighPriority = true)
                resolveState = com.example.player.resolver.MediaResolveState.Ready(media)
            } catch (e: Exception) {
                val isOffline = !com.example.util.NetworkUtils.isOnline(context)
                resolveState = com.example.player.resolver.MediaResolveState.Failed(
                    if (isOffline) "You are offline. Check your connection and tap Retry." else "Couldn't load this video. Check your connection and retry.",
                    e.message
                )
            }
        }
    }

    val aspectRatio = if (video.isVertical) 9f / 14f else 16f / 9f

    androidx.compose.foundation.layout.Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(aspectRatio)
            .background(androidx.compose.ui.graphics.Color.Black),
        contentAlignment = androidx.compose.ui.Alignment.Center
    ) {
        when (val state = resolveState) {
            is com.example.player.resolver.MediaResolveState.Idle,
            is com.example.player.resolver.MediaResolveState.Resolving,
            is com.example.player.resolver.MediaResolveState.Recovering -> {
                if (!video.thumbnailUrl.isNullOrBlank()) {
                    coil.compose.AsyncImage(
                        model = video.thumbnailUrl,
                        contentDescription = "Thumbnail",
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    androidx.compose.foundation.layout.Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.35f))
                    )
                }
                androidx.compose.material3.CircularProgressIndicator(
                    color = com.example.ui.theme.QYellowPrimary,
                    strokeWidth = 3.dp,
                    modifier = Modifier.size(44.dp)
                )
            }

            is com.example.player.resolver.MediaResolveState.Ready -> {
                NativeVideoPlayer(
                    videoUrl = state.media.url,
                    requestHeaders = state.media.headers,
                    thumbnailUrl = video.thumbnailUrl,
                    isVertical = video.isVertical,
                    autoPlay = config.autoPlay,
                    loop = config.loop,
                    muted = config.muted,
                    showControls = config.showControls,
                    modifier = Modifier.fillMaxSize(),
                    onError = { rawError ->
                        android.util.Log.d("MediaResolver", "Playback error encountered: $rawError")
                        val isOffline = !com.example.util.NetworkUtils.isOnline(context) ||
                                rawError.contains("ERROR_CODE_IO_NETWORK_CONNECTION_FAILED", ignoreCase = true) ||
                                rawError.contains("UnknownHostException", ignoreCase = true)

                        if (isOffline) {
                            resolveState = com.example.player.resolver.MediaResolveState.Failed(
                                "You are offline. Check your connection and tap Retry.",
                                rawError
                            )
                        } else if (recoveryCount < 2) {
                            recoveryCount++
                            resolveState = com.example.player.resolver.MediaResolveState.Recovering
                            coroutineScope.launch {
                                try {
                                    val fresh = com.example.player.resolver.MediaResolver.resolve(key, force = true, isHighPriority = true)
                                    resolveState = com.example.player.resolver.MediaResolveState.Ready(fresh)
                                } catch (e: Exception) {
                                    resolveState = com.example.player.resolver.MediaResolveState.Failed(
                                        "Couldn't load this video. Check your connection and retry.",
                                        e.message
                                    )
                                }
                            }
                        } else {
                            resolveState = com.example.player.resolver.MediaResolveState.Failed(
                                "Couldn't load this video. Check your connection and retry.",
                                rawError
                            )
                        }
                    }
                )
            }

            is com.example.player.resolver.MediaResolveState.Failed -> {
                androidx.compose.foundation.layout.Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.88f))
                        .padding(16.dp),
                    contentAlignment = androidx.compose.ui.Alignment.Center
                ) {
                    androidx.compose.foundation.layout.Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
                        androidx.compose.material3.Text(
                            text = "Video Playback Error",
                            color = androidx.compose.ui.graphics.Color.White,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(4.dp))
                        androidx.compose.material3.Text(
                            text = state.message,
                            color = androidx.compose.ui.graphics.Color(0xFFEF4444),
                            fontSize = 12.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            maxLines = 2
                        )
                        androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(14.dp))
                        androidx.compose.material3.Button(
                            onClick = {
                                recoveryCount = 0
                                resolveState = com.example.player.resolver.MediaResolveState.Resolving
                                coroutineScope.launch {
                                    try {
                                        val fresh = com.example.player.resolver.MediaResolver.resolve(key, force = true, isHighPriority = true)
                                        resolveState = com.example.player.resolver.MediaResolveState.Ready(fresh)
                                    } catch (e: Exception) {
                                        val isOffline = !com.example.util.NetworkUtils.isOnline(context)
                                        resolveState = com.example.player.resolver.MediaResolveState.Failed(
                                            if (isOffline) "You are offline. Check your connection and tap Retry." else "Couldn't load this video. Check your connection and retry.",
                                            e.message
                                        )
                                    }
                                }
                            },
                            colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                containerColor = com.example.ui.theme.QYellowPrimary,
                                contentColor = com.example.ui.theme.QOnYellow
                            ),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            androidx.compose.material3.Icon(
                                androidx.compose.material.icons.Icons.Default.Refresh,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            androidx.compose.foundation.layout.Spacer(modifier = Modifier.width(6.dp))
                            androidx.compose.material3.Text("Retry", fontSize = 12.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                        }
                    }
                }
            }
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
