package com.example.player.resolver

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MediaResolverTest {

    @Test
    fun expiryParser_extractsQueryUnixTimestamp() {
        val nowMs = 1700000000000L // arbitrary fixed test time
        val expSeconds = 1700003600L // +1 hour

        val testUrl1 = "https://v16m-default.akamaized.net/video.mp4?x-expires=$expSeconds&token=abc"
        val expiry1 = ExpiryParser.calculateExpiryMs(testUrl1, nowMs)
        assertEquals(expSeconds * 1000L, expiry1)

        val testUrl2 = "https://www.tikwm.com/media/stream.mp4?expires=$expSeconds"
        val expiry2 = ExpiryParser.calculateExpiryMs(testUrl2, nowMs)
        assertEquals(expSeconds * 1000L, expiry2)

        val testUrl3 = "https://v16m.tiktok.com/video.mp4?e=$expSeconds"
        val expiry3 = ExpiryParser.calculateExpiryMs(testUrl3, nowMs)
        assertEquals(expSeconds * 1000L, expiry3)
    }

    @Test
    fun expiryParser_defaultsToFifteenMinutesWhenMissing() {
        val nowMs = 1700000000000L
        val testUrl = "https://www.tikwm.com/video/media/play/123456.mp4"
        val expiry = ExpiryParser.calculateExpiryMs(testUrl, nowMs)
        assertEquals(nowMs + 15 * 60 * 1000L, expiry)
    }

    @Test
    fun resolvedMedia_stalenessCalculation() {
        val nowMs = 1000000L
        val freshMedia = ResolvedMedia(
            url = "https://video.mp4",
            resolvedAtMs = nowMs,
            expiresAtMs = nowMs + 10 * 60 * 1000L // 10 mins away
        )
        assertFalse(freshMedia.isStale(nowMs))

        val staleMedia = ResolvedMedia(
            url = "https://video.mp4",
            resolvedAtMs = nowMs - 14 * 60 * 1000L,
            expiresAtMs = nowMs + 1 * 60 * 1000L // expires in 1 min, but rule says stale 2 mins before expiry
        )
        assertTrue(staleMedia.isStale(nowMs))
    }

    @Test
    fun mediaResolver_lruCacheAndInvalidate() {
        val key = MediaKey("https://www.tiktok.com/@user/video/123", "tiktok")
        assertNull(MediaResolver.getCached(key))

        val media = ResolvedMedia(
            url = "https://tikwm.com/play.mp4",
            resolvedAtMs = System.currentTimeMillis(),
            expiresAtMs = System.currentTimeMillis() + 600000L
        )

        MediaResolver.putInCache(key, media)
        assertNotNull(MediaResolver.getCached(key))

        // Test invalidation
        MediaResolver.invalidate(key)
        assertNull(MediaResolver.getCached(key))
    }
}
