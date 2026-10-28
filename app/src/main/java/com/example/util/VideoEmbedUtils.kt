package com.example.util

import com.example.video.VideoPlatformRegistry
import java.net.URLEncoder

data class DetectedVideoInfo(
    val platform: String, // "youtube", "tiktok", "facebook", "instagram", "direct"
    val originalUrl: String,
    val embedUrl: String,
    val platformDisplayName: String
)

/**
 * Backward-compatible wrapper delegating to the unified [VideoPlatformRegistry].
 */
object VideoEmbedUtils {

    fun parseVideoUrl(url: String): DetectedVideoInfo {
        val trimmed = url.trim()
        if (trimmed.isBlank()) {
            return DetectedVideoInfo(
                platform = "direct",
                originalUrl = "",
                embedUrl = "",
                platformDisplayName = "Video Link"
            )
        }

        val handler = VideoPlatformRegistry.resolve(trimmed)
        val parsed = handler.parse(trimmed)

        val embedUrl = when (handler.platformId) {
            "youtube" -> {
                if (parsed?.videoId != null) {
                    "https://www.youtube.com/embed/${parsed.videoId}?autoplay=0&rel=0&modestbranding=1"
                } else {
                    trimmed
                }
            }
            "tiktok" -> {
                val videoId = parsed?.videoId ?: extractTikTokId(trimmed)
                if (videoId != null) {
                    "https://www.tiktok.com/embed/v2/$videoId"
                } else {
                    trimmed
                }
            }
            "instagram" -> {
                val cleanUrl = trimmed.split("?").firstOrNull() ?: trimmed
                if (cleanUrl.endsWith("/")) "${cleanUrl}embed" else "$cleanUrl/embed"
            }
            "facebook" -> {
                val encoded = try {
                    URLEncoder.encode(trimmed, "UTF-8")
                } catch (_: Exception) {
                    trimmed
                }
                "https://www.facebook.com/plugins/video.php?href=$encoded&show_text=0"
            }
            "gdrive" -> {
                val fileId = parsed?.videoId?.substringBefore("|")
                if (fileId != null) "https://drive.google.com/file/d/$fileId/preview" else trimmed
            }
            "streamtape" -> {
                val videoId = parsed?.videoId ?: com.example.video.streamtape.StreamtapeHandler.extractId(trimmed)
                if (videoId != null) "https://streamtape.com/e/$videoId/" else trimmed
            }
            else -> trimmed
        }

        return DetectedVideoInfo(
            platform = handler.platformId,
            originalUrl = trimmed,
            embedUrl = embedUrl,
            platformDisplayName = handler.displayName
        )
    }

    private fun extractTikTokId(url: String): String? {
        val pattern = java.util.regex.Pattern.compile("(?:video/|v/|/embed/v2/|/photo/)(\\d+)", java.util.regex.Pattern.CASE_INSENSITIVE)
        val matcher = pattern.matcher(url)
        if (matcher.find()) return matcher.group(1)
        val fallbackPattern = java.util.regex.Pattern.compile("/(\\d{15,20})", java.util.regex.Pattern.CASE_INSENSITIVE)
        val fallbackMatcher = fallbackPattern.matcher(url)
        if (fallbackMatcher.find()) return fallbackMatcher.group(1)
        return null
    }
}
