package com.example.util

import com.example.data.model.Post

enum class MediaType {
    YOUTUBE,
    STREAMTAPE,
    DOODSTREAM,
    TIKTOK,
    GOOGLE_DRIVE,
    DIRECT_VIDEO,
    DIRECT_IMAGE,
    AUDIO,
    GENERIC_LINK
}

data class ParsedMedia(
    val type: MediaType,
    val url: String,
    val title: String? = null,
    val id: String? = null,
    val thumbnailUrl: String? = null,
    val embedUrl: String? = null
)

object MediaParser {

    private val YOUTUBE_REGEX = Regex(
        "(?:youtu\\.be/|youtube\\.com/(?:watch\\?(?:.*&)?v=|(?:embed|v|shorts)/))([a-zA-Z0-9_-]{11})"
    )

    private val STREAMTAPE_REGEX = Regex(
        "https?://(?:www\\.)?streamtape\\.(?:com|net|to|pe|cc)/(?:v|e)/([a-zA-Z0-9_-]+)",
        RegexOption.IGNORE_CASE
    )

    private val TIKTOK_REGEX = Regex(
        "tiktok\\.com/(?:@[^/]+/video/|v/|t/)?(\\d{15,22})"
    )

    private val GOOGLE_DRIVE_REGEX = Regex(
        "drive\\.google\\.com/file/d/([a-zA-Z0-9_-]+)"
    )

    private val VIDEO_TOKEN_REGEX = Regex(
        "\\[video:([^|\\]]+)(?:\\|([^|\\]]*))?(?:\\|([^|\\]]*))?\\]"
    )

    private val AUDIO_TOKEN_REGEX = Regex(
        "\\[audio:([^|\\]]+)(?:\\|([^|\\]]*))?\\]"
    )

    private val IMAGE_URL_REGEX = Regex(
        "https?://\\S+\\.(?:jpg|jpeg|png|gif|webp|bmp)(?:\\?\\S*)?",
        RegexOption.IGNORE_CASE
    )

    private val DIRECT_VIDEO_REGEX = Regex(
        "https?://\\S+\\.(?:mp4|webm|mkv|mov|m4v)(?:\\?\\S*)?",
        RegexOption.IGNORE_CASE
    )

    fun parseMedia(post: Post): ParsedMedia? {
        // 1. Check videoPreviewUrls from Supabase
        val directPreviewUrl = post.videoPreviewUrls?.firstOrNull { it.isNotBlank() }
        if (!directPreviewUrl.isNullOrBlank()) {
            val parsed = parseSingleUrl(directPreviewUrl)
            if (parsed != null) return parsed
        }

        // 2. Check attachments
        post.attachments?.firstOrNull()?.let { att ->
            if (att.category == "video" || att.type?.startsWith("video/") == true) {
                return ParsedMedia(
                    type = MediaType.DIRECT_VIDEO,
                    url = att.url,
                    title = att.name
                )
            } else if (att.category == "image" || att.type?.startsWith("image/") == true) {
                return ParsedMedia(
                    type = MediaType.DIRECT_IMAGE,
                    url = att.url,
                    title = att.name
                )
            }
        }

        // 3. Check externalUrl
        if (!post.externalUrl.isNullOrBlank()) {
            val parsed = parseSingleUrl(post.externalUrl)
            if (parsed != null) return parsed
        }

        // 4. Check embedded tokens in content: [video:...]
        VIDEO_TOKEN_REGEX.find(post.content)?.let { match ->
            val url = match.groupValues[1]
            val title = match.groupValues.getOrNull(2)?.takeIf { it.isNotBlank() }
            val parsed = parseSingleUrl(url)
            if (parsed != null) {
                return parsed.copy(title = title ?: parsed.title)
            }
            return ParsedMedia(
                type = MediaType.DIRECT_VIDEO,
                url = url,
                title = title
            )
        }

        // 5. Check embedded tokens: [audio:...]
        AUDIO_TOKEN_REGEX.find(post.content)?.let { match ->
            val url = match.groupValues[1]
            val title = match.groupValues.getOrNull(2)
            return ParsedMedia(
                type = MediaType.AUDIO,
                url = url,
                title = title
            )
        }

        // 6. Check images in content
        IMAGE_URL_REGEX.find(post.content)?.let { match ->
            return ParsedMedia(
                type = MediaType.DIRECT_IMAGE,
                url = match.value
            )
        }

        // 7. Check direct video url in content
        DIRECT_VIDEO_REGEX.find(post.content)?.let { match ->
            return ParsedMedia(
                type = MediaType.DIRECT_VIDEO,
                url = match.value
            )
        }

        // 8. If post.externalUrl exists but not a video, return generic link
        if (!post.externalUrl.isNullOrBlank()) {
            return ParsedMedia(
                type = MediaType.GENERIC_LINK,
                url = post.externalUrl
            )
        }

        return null
    }

    fun parseSingleUrl(url: String): ParsedMedia? {
        val trimmed = url.trim()

        // YouTube check
        YOUTUBE_REGEX.find(trimmed)?.let { match ->
            val videoId = match.groupValues[1]
            return ParsedMedia(
                type = MediaType.YOUTUBE,
                url = trimmed,
                id = videoId,
                thumbnailUrl = "https://img.youtube.com/vi/$videoId/hqdefault.jpg",
                embedUrl = "https://www.youtube-nocookie.com/embed/$videoId?autoplay=1&modestbranding=1&playsinline=1"
            )
        }

        // Streamtape check
        STREAMTAPE_REGEX.find(trimmed)?.let { match ->
            val videoId = match.groupValues[1]
            return ParsedMedia(
                type = MediaType.STREAMTAPE,
                url = trimmed,
                id = videoId,
                embedUrl = "https://streamtape.com/e/$videoId/"
            )
        }
        if (isStreamtapeUrl(trimmed)) {
            val videoId = com.example.video.streamtape.StreamtapeHandler.extractId(trimmed)
            return ParsedMedia(
                type = MediaType.STREAMTAPE,
                url = trimmed,
                id = videoId,
                embedUrl = if (videoId != null) "https://streamtape.com/e/$videoId/" else trimmed
            )
        }

        // DoodStream check
        if (isDoodStreamUrl(trimmed)) {
            val videoId = com.example.video.doodstream.DoodStreamHandler.extractId(trimmed)
            return ParsedMedia(
                type = MediaType.DOODSTREAM,
                url = trimmed,
                id = videoId,
                embedUrl = if (videoId != null) "https://dood.to/e/$videoId/" else trimmed
            )
        }

        // TikTok check
        TIKTOK_REGEX.find(trimmed)?.let { match ->
            val videoId = match.groupValues[1]
            return ParsedMedia(
                type = MediaType.TIKTOK,
                url = trimmed,
                id = videoId,
                embedUrl = "https://www.tiktok.com/embed/v2/$videoId"
            )
        }
        if (trimmed.contains("tiktok.com", ignoreCase = true)) {
            return ParsedMedia(
                type = MediaType.TIKTOK,
                url = trimmed,
                embedUrl = "https://www.tiktok.com/embed/v2/${extractTikTokId(trimmed)}"
            )
        }

        // Google Drive check (via registry)
        val handler = com.example.video.VideoPlatformRegistry.resolve(trimmed)
        if (handler.platformId == "gdrive") {
            val p = handler.parse(trimmed)
            val fileId = p?.videoId?.substringBefore("|")
            if (fileId != null) {
                return ParsedMedia(
                    type = MediaType.GOOGLE_DRIVE,
                    url = trimmed,
                    id = p.videoId,
                    embedUrl = "https://drive.google.com/file/d/$fileId/preview"
                )
            }
        }

        // Direct Image
        if (IMAGE_URL_REGEX.matches(trimmed) || isLikelyImageUrl(trimmed)) {
            return ParsedMedia(
                type = MediaType.DIRECT_IMAGE,
                url = trimmed
            )
        }

        // Direct Video
        if (DIRECT_VIDEO_REGEX.matches(trimmed) || trimmed.endsWith(".mp4", ignoreCase = true)) {
            return ParsedMedia(
                type = MediaType.DIRECT_VIDEO,
                url = trimmed
            )
        }

        return null
    }

    private fun isLikelyImageUrl(url: String): Boolean {
        val lower = url.lowercase()
        return lower.contains("i.ibb.co") ||
                lower.contains("images.unsplash.com") ||
                lower.contains("imgur.com") ||
                lower.endsWith(".png") ||
                lower.endsWith(".jpg") ||
                lower.endsWith(".jpeg") ||
                lower.endsWith(".webp")
    }

    private fun extractTikTokId(url: String): String {
        return url.substringAfter("/video/").substringBefore("?").substringBefore("/")
            .takeIf { it.all { char -> char.isDigit() } } ?: ""
    }

    fun isStreamtapeUrl(url: String): Boolean {
        val lower = url.lowercase().trim()
        return lower.contains("streamtape.com") ||
                lower.contains("streamtape.net") ||
                lower.contains("streamtape.to") ||
                lower.contains("streamta.pe") ||
                lower.contains("streamtape.cc")
    }

    fun isDoodStreamUrl(url: String): Boolean {
        val lower = url.lowercase().trim()
        return lower.contains("dood.") ||
                lower.contains("doodstream") ||
                lower.contains("doodcdn") ||
                lower.contains("doodapi")
    }

    fun isYouTubeUrl(url: String): Boolean {
        val lower = url.lowercase().trim()
        return lower.contains("youtube.com") || lower.contains("youtu.be")
    }

    fun isDriveUrl(url: String): Boolean {
        val lower = url.lowercase().trim()
        return lower.contains("drive.google.com") || lower.contains("docs.google.com") || lower.contains("drive.usercontent.google.com")
    }

    fun extractStreamtapeEmbedUrl(url: String): String {
        return com.example.video.streamtape.StreamtapeHandler.toEmbedUrl(url)
    }

    fun extractStreamtapeId(url: String): String? {
        return com.example.video.streamtape.StreamtapeHandler.extractId(url)
    }

    fun extractDoodStreamEmbedUrl(url: String): String {
        return com.example.video.doodstream.DoodStreamHandler.toEmbedUrl(url)
    }

    fun extractDoodStreamId(url: String): String? {
        return com.example.video.doodstream.DoodStreamHandler.extractId(url)
    }

    fun cleanContentText(content: String): String {
        return content
            .replace(Regex("<!--.*?-->", RegexOption.DOT_MATCHES_ALL), "")
            .replace(VIDEO_TOKEN_REGEX, "")
            .replace(AUDIO_TOKEN_REGEX, "")
            .trim()
    }
}
