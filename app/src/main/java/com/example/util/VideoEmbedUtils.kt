package com.example.util

import java.net.URLEncoder
import java.util.regex.Pattern

data class DetectedVideoInfo(
    val platform: String, // "youtube", "tiktok", "facebook", "instagram", "direct"
    val originalUrl: String,
    val embedUrl: String,
    val platformDisplayName: String
)

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

        // 1. YouTube
        val ytVideoId = extractYouTubeId(trimmed)
        if (ytVideoId != null) {
            val embed = "https://www.youtube.com/embed/$ytVideoId?autoplay=0&rel=0&modestbranding=1"
            return DetectedVideoInfo(
                platform = "youtube",
                originalUrl = trimmed,
                embedUrl = embed,
                platformDisplayName = "YouTube"
            )
        }

        // 2. TikTok
        if (trimmed.contains("tiktok.com", ignoreCase = true)) {
            val tiktokId = extractTikTokId(trimmed)
            val embed = if (tiktokId != null) {
                "https://www.tiktok.com/embed/v2/$tiktokId"
            } else {
                trimmed
            }
            return DetectedVideoInfo(
                platform = "tiktok",
                originalUrl = trimmed,
                embedUrl = embed,
                platformDisplayName = "TikTok"
            )
        }

        // 3. Instagram
        if (trimmed.contains("instagram.com", ignoreCase = true) || trimmed.contains("instagr.am", ignoreCase = true)) {
            val cleanUrl = trimmed.split("?").firstOrNull() ?: trimmed
            val embed = if (cleanUrl.endsWith("/")) "${cleanUrl}embed" else "$cleanUrl/embed"
            return DetectedVideoInfo(
                platform = "instagram",
                originalUrl = trimmed,
                embedUrl = embed,
                platformDisplayName = "Instagram"
            )
        }

        // 4. Facebook
        if (trimmed.contains("facebook.com", ignoreCase = true) || trimmed.contains("fb.watch", ignoreCase = true) || trimmed.contains("fb.com", ignoreCase = true)) {
            val encoded = try {
                URLEncoder.encode(trimmed, "UTF-8")
            } catch (_: Exception) {
                trimmed
            }
            val embed = "https://www.facebook.com/plugins/video.php?href=$encoded&show_text=0"
            return DetectedVideoInfo(
                platform = "facebook",
                originalUrl = trimmed,
                embedUrl = embed,
                platformDisplayName = "Facebook Video"
            )
        }

        // Default: Direct / Web Video
        return DetectedVideoInfo(
            platform = "direct",
            originalUrl = trimmed,
            embedUrl = trimmed,
            platformDisplayName = "Video Stream"
        )
    }

    private fun extractYouTubeId(url: String): String? {
        val patterns = listOf(
            "(?:youtube(?:-nocookie)?\\.com/(?:[^/\\n\\s]+/.+/|(?:v|e(?:mbed)?)/|.*[?&]v=)|youtu\\.be/|youtube\\.com/shorts/)([a-zA-Z0-9_-]{11})",
            "^([a-zA-Z0-9_-]{11})$"
        )
        for (p in patterns) {
            val matcher = Pattern.compile(p, Pattern.CASE_INSENSITIVE).matcher(url)
            if (matcher.find()) {
                return matcher.group(1)
            }
        }
        return null
    }

    private fun extractTikTokId(url: String): String? {
        val pattern = Pattern.compile("video/(\\d+)", Pattern.CASE_INSENSITIVE)
        val matcher = pattern.matcher(url)
        return if (matcher.find()) matcher.group(1) else null
    }
}
