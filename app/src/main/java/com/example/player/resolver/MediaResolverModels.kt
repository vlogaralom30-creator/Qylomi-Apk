package com.example.player.resolver

import android.net.Uri
import java.util.regex.Pattern

data class MediaKey(
    val url: String,
    val platform: String = "tiktok"
)

data class ResolvedMedia(
    val url: String,
    val headers: Map<String, String> = emptyMap(),
    val resolvedAtMs: Long = System.currentTimeMillis(),
    val expiresAtMs: Long,
    val isHls: Boolean = false,
    val thumbnailUrl: String? = null,
    val durationMs: Long? = null,
    val isDirectPlayable: Boolean = true,
    val embedUrl: String? = null
) {
    fun isStale(nowMs: Long = System.currentTimeMillis()): Boolean {
        // Treat entry as stale 2 minutes (120,000 ms) before actual expiry
        return nowMs >= (expiresAtMs - 120_000L)
    }
}

sealed class MediaResolveState {
    object Idle : MediaResolveState()
    object Resolving : MediaResolveState()
    data class Ready(val media: ResolvedMedia) : MediaResolveState()
    object Recovering : MediaResolveState()
    data class Fallback(val video: com.example.video.ExtractedVideo) : MediaResolveState()
    data class Failed(val message: String, val rawError: String? = null) : MediaResolveState()
}

object ExpiryParser {
    private const val DEFAULT_TTL_MS = 15 * 60 * 1000L // 15 minutes default TTL

    fun calculateExpiryMs(url: String, nowMs: Long = System.currentTimeMillis()): Long {
        if (url.isBlank()) return nowMs + DEFAULT_TTL_MS

        try {
            val pattern = Pattern.compile("(?:x-expires|expires|expire|e|exp|signed-token|token_expiry)=(\\d{10,13})", Pattern.CASE_INSENSITIVE)
            val matcher = pattern.matcher(url)
            if (matcher.find()) {
                val numStr = matcher.group(1) ?: ""
                val num = numStr.toLongOrNull()
                if (num != null && num > 0) {
                    val expMs = if (numStr.length >= 13) num else num * 1000L
                    if (expMs > nowMs) {
                        return expMs
                    }
                }
            }
        } catch (_: Exception) {}

        return nowMs + DEFAULT_TTL_MS
    }
}
