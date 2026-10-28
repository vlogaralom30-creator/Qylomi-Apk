package com.example.video

import com.example.video.legacy.DirectLegacyHandler
import com.example.video.legacy.FacebookLegacyHandler
import com.example.video.legacy.InstagramLegacyHandler
import com.example.video.legacy.TikTokLegacyHandler
import com.example.video.youtube.YouTubeHandler

/**
 * Central registry of video platform handlers.
 * Ordered by matching priority.
 *
 * To add a new platform in the future:
 * 1. Create a new class implementing [VideoPlatformHandler].
 * 2. Add one line to the [handlers] list below.
 */
object VideoPlatformRegistry {

    private val directFallback = DirectLegacyHandler()

    private val handlers: List<VideoPlatformHandler> = listOf(
        YouTubeHandler(),
        com.example.video.streamtape.StreamtapeHandler(),
        com.example.video.doodstream.DoodStreamHandler(),
        com.example.video.gdrive.GoogleDriveHandler(),
        TikTokLegacyHandler(),
        InstagramLegacyHandler(),
        FacebookLegacyHandler(),
        directFallback
    )

    /**
     * Resolves the appropriate handler for a given video URL.
     */
    fun resolve(url: String): VideoPlatformHandler {
        val trimmed = url.trim()
        for (handler in handlers) {
            if (handler.matches(trimmed)) {
                return handler
            }
        }
        return directFallback
    }

    /**
     * Parses a video URL using the matching platform handler.
     */
    fun parse(url: String): ParsedVideo {
        val handler = resolve(url)
        return handler.parse(url) ?: ParsedVideo(
            platform = handler.platformId,
            originalUrl = url.trim()
        )
    }

    /**
     * Resolves and extracts video metadata using the matching platform handler.
     */
    suspend fun extract(url: String): Result<ExtractedVideo> {
        val handler = resolve(url)
        val parsed = handler.parse(url) ?: ParsedVideo(
            platform = handler.platformId,
            originalUrl = url.trim()
        )
        return handler.extract(parsed)
    }
}
