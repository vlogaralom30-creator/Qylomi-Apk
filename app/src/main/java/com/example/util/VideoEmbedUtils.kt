package com.example.util

import com.example.video.VideoPlatformRegistry
import java.net.URLEncoder
import java.util.regex.Pattern

data class DetectedVideoInfo(
    val platform: String, // "youtube", "tiktok", "facebook", "instagram", "streamtape", "doodstream", "gdrive", "direct", "embed"
    val originalUrl: String,
    val embedUrl: String,
    val platformDisplayName: String,
    val isIframeCode: Boolean = false
)

/**
 * Backward-compatible wrapper delegating to the unified [VideoPlatformRegistry]
 * with automatic detection for <iframe>...</iframe> embed codes.
 */
object VideoEmbedUtils {

    private val IFRAME_SRC_REGEX = Pattern.compile(
        """<iframe[^>]+src\s*=\s*['"]([^'"]+)['"]""",
        Pattern.CASE_INSENSITIVE
    )

    private val IFRAME_SRC_NO_QUOTES_REGEX = Pattern.compile(
        """<iframe[^>]+src\s*=\s*([^\s>]+)""",
        Pattern.CASE_INSENSITIVE
    )

    /**
     * Checks if the given input is raw HTML iframe embed code.
     */
    fun isIframeEmbedCode(input: String): Boolean {
        val trimmed = input.trim()
        return (trimmed.startsWith("<iframe", ignoreCase = true) || trimmed.contains("<iframe", ignoreCase = true)) &&
                trimmed.contains("src=", ignoreCase = true)
    }

    /**
     * Extracts the target URL from an iframe embed snippet or returns the cleaned URL.
     */
    fun extractUrlFromInput(input: String): String {
        val trimmed = input.trim()
        if (isIframeEmbedCode(trimmed)) {
            val matcher = IFRAME_SRC_REGEX.matcher(trimmed)
            if (matcher.find()) {
                val extracted = matcher.group(1)?.trim()
                if (!extracted.isNullOrBlank()) {
                    return decodeHtmlEntities(extracted)
                }
            }
            val noQuotesMatcher = IFRAME_SRC_NO_QUOTES_REGEX.matcher(trimmed)
            if (noQuotesMatcher.find()) {
                val extracted = noQuotesMatcher.group(1)?.trim()
                if (!extracted.isNullOrBlank()) {
                    return decodeHtmlEntities(extracted)
                }
            }
        }
        return trimmed
    }

    private fun decodeHtmlEntities(url: String): String {
        return url.replace("&amp;", "&")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
    }

    fun parseVideoUrl(url: String): DetectedVideoInfo {
        val isCode = isIframeEmbedCode(url)
        val cleanUrl = extractUrlFromInput(url)

        if (cleanUrl.isBlank()) {
            return DetectedVideoInfo(
                platform = "direct",
                originalUrl = "",
                embedUrl = "",
                platformDisplayName = "Video Link",
                isIframeCode = false
            )
        }

        val handler = VideoPlatformRegistry.resolve(cleanUrl)
        val parsed = handler.parse(cleanUrl)

        val embedUrl = when (handler.platformId) {
            "youtube" -> {
                if (parsed?.videoId != null) {
                    "https://www.youtube.com/embed/${parsed.videoId}?autoplay=0&rel=0&modestbranding=1"
                } else {
                    cleanUrl
                }
            }
            "tiktok" -> {
                val videoId = parsed?.videoId ?: extractTikTokId(cleanUrl)
                if (videoId != null) {
                    "https://www.tiktok.com/embed/v2/$videoId"
                } else {
                    cleanUrl
                }
            }
            "instagram" -> {
                val base = cleanUrl.split("?").firstOrNull() ?: cleanUrl
                if (base.endsWith("/")) "${base}embed" else "$base/embed"
            }
            "facebook" -> {
                val encoded = try {
                    URLEncoder.encode(cleanUrl, "UTF-8")
                } catch (_: Exception) {
                    cleanUrl
                }
                "https://www.facebook.com/plugins/video.php?href=$encoded&show_text=0"
            }
            "gdrive" -> {
                val fileId = parsed?.videoId?.substringBefore("|")
                if (fileId != null) "https://drive.google.com/file/d/$fileId/preview" else cleanUrl
            }
            "streamtape" -> {
                val videoId = parsed?.videoId ?: com.example.video.streamtape.StreamtapeHandler.extractId(cleanUrl)
                if (videoId != null) "https://streamtape.com/e/$videoId/" else cleanUrl
            }
            "doodstream" -> {
                val videoId = parsed?.videoId ?: com.example.video.doodstream.DoodStreamHandler.extractId(cleanUrl)
                if (videoId != null) "https://dood.to/e/$videoId/" else cleanUrl
            }
            else -> {
                if (isCode || cleanUrl.contains("/e/") || cleanUrl.contains("/embed/")) {
                    cleanUrl
                } else {
                    cleanUrl
                }
            }
        }

        val effectivePlatform = if (handler.platformId == "direct" && (isCode || cleanUrl.contains("/e/") || cleanUrl.contains("/embed/"))) {
            "embed"
        } else {
            handler.platformId
        }

        val effectiveDisplayName = if (effectivePlatform == "embed") {
            "Embed Player"
        } else {
            handler.displayName
        }

        return DetectedVideoInfo(
            platform = effectivePlatform,
            originalUrl = cleanUrl,
            embedUrl = embedUrl,
            platformDisplayName = effectiveDisplayName,
            isIframeCode = isCode
        )
    }

    private fun extractTikTokId(url: String): String? {
        val pattern = Pattern.compile("(?:video/|v/|/embed/v2/|/photo/)(\\d+)", Pattern.CASE_INSENSITIVE)
        val matcher = pattern.matcher(url)
        if (matcher.find()) return matcher.group(1)
        val fallbackPattern = Pattern.compile("/(\\d{15,20})", Pattern.CASE_INSENSITIVE)
        val fallbackMatcher = fallbackPattern.matcher(url)
        if (fallbackMatcher.find()) return fallbackMatcher.group(1)
        return null
    }
}
