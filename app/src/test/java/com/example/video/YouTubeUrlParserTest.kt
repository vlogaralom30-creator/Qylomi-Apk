package com.example.video

import com.example.video.youtube.YouTubeHandler
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class YouTubeUrlParserTest {

    private lateinit var handler: YouTubeHandler

    @Before
    fun setUp() {
        handler = YouTubeHandler()
    }

    // 1. Standard watch?v= URL
    @Test
    fun testStandardWatchUrl() {
        val parsed = handler.parse("https://www.youtube.com/watch?v=dQw4w9WgXcQ")
        assertNotNull(parsed)
        assertEquals("dQw4w9WgXcQ", parsed?.videoId)
        assertFalse(parsed!!.isShort)
        assertEquals(0, parsed.startSeconds)
    }

    // 2. Mobile web URL (m.youtube.com)
    @Test
    fun testMobileWebUrl() {
        val parsed = handler.parse("https://m.youtube.com/watch?v=dQw4w9WgXcQ")
        assertNotNull(parsed)
        assertEquals("dQw4w9WgXcQ", parsed?.videoId)
        assertFalse(parsed!!.isShort)
    }

    // 3. YouTube Music URL (music.youtube.com)
    @Test
    fun testMusicWebUrl() {
        val parsed = handler.parse("https://music.youtube.com/watch?v=dQw4w9WgXcQ")
        assertNotNull(parsed)
        assertEquals("dQw4w9WgXcQ", parsed?.videoId)
    }

    // 4. Shortened youtu.be URL
    @Test
    fun testShortenedYouTuBeUrl() {
        val parsed = handler.parse("https://youtu.be/dQw4w9WgXcQ")
        assertNotNull(parsed)
        assertEquals("dQw4w9WgXcQ", parsed?.videoId)
        assertFalse(parsed!!.isShort)
    }

    // 5. YouTube Shorts URL
    @Test
    fun testShortsUrl() {
        val parsed = handler.parse("https://www.youtube.com/shorts/dQw4w9WgXcQ")
        assertNotNull(parsed)
        assertEquals("dQw4w9WgXcQ", parsed?.videoId)
        assertTrue(parsed!!.isShort)
    }

    // 6. Shorts URL with extra feature parameter
    @Test
    fun testShortsUrlWithParams() {
        val parsed = handler.parse("https://youtube.com/shorts/dQw4w9WgXcQ?feature=share")
        assertNotNull(parsed)
        assertEquals("dQw4w9WgXcQ", parsed?.videoId)
        assertTrue(parsed!!.isShort)
    }

    // 7. Embed URL
    @Test
    fun testEmbedUrl() {
        val parsed = handler.parse("https://www.youtube.com/embed/dQw4w9WgXcQ")
        assertNotNull(parsed)
        assertEquals("dQw4w9WgXcQ", parsed?.videoId)
    }

    // 8. Privacy-enhanced nocookie embed URL
    @Test
    fun testNoCookieEmbedUrl() {
        val parsed = handler.parse("https://www.youtube-nocookie.com/embed/dQw4w9WgXcQ")
        assertNotNull(parsed)
        assertEquals("dQw4w9WgXcQ", parsed?.videoId)
    }

    // 9. Legacy /v/ URL
    @Test
    fun testLegacyVUrl() {
        val parsed = handler.parse("https://www.youtube.com/v/dQw4w9WgXcQ")
        assertNotNull(parsed)
        assertEquals("dQw4w9WgXcQ", parsed?.videoId)
    }

    // 10. Live stream URL (/live/)
    @Test
    fun testLiveStreamUrl() {
        val parsed = handler.parse("https://www.youtube.com/live/dQw4w9WgXcQ")
        assertNotNull(parsed)
        assertEquals("dQw4w9WgXcQ", parsed?.videoId)
    }

    // 11. Start time parameter: t=90s
    @Test
    fun testStartTimeSecondsSuffix() {
        val parsed = handler.parse("https://www.youtube.com/watch?v=dQw4w9WgXcQ&t=90s")
        assertNotNull(parsed)
        assertEquals(90, parsed?.startSeconds)
    }

    // 12. Start time parameter: t=1m30s
    @Test
    fun testStartTimeMinuteSecondFormat() {
        val parsed = handler.parse("https://youtu.be/dQw4w9WgXcQ?t=1m30s")
        assertNotNull(parsed)
        assertEquals(90, parsed?.startSeconds)
    }

    // 13. Start time parameter: start=120
    @Test
    fun testStartEqualsParameter() {
        val parsed = handler.parse("https://www.youtube.com/watch?v=dQw4w9WgXcQ&start=120")
        assertNotNull(parsed)
        assertEquals(120, parsed?.startSeconds)
    }

    // 14. Start time in fragment: #t=45
    @Test
    fun testFragmentStartTime() {
        val parsed = handler.parse("https://www.youtube.com/watch?v=dQw4w9WgXcQ#t=45")
        assertNotNull(parsed)
        assertEquals(45, parsed?.startSeconds)
    }

    // 15. Extra parameters (si, feature, utm_source, pp)
    @Test
    fun testExtraTrackingParameters() {
        val parsed = handler.parse("https://www.youtube.com/watch?v=dQw4w9WgXcQ&si=AbcDef123&feature=share&utm_source=test&pp=ygUH")
        assertNotNull(parsed)
        assertEquals("dQw4w9WgXcQ", parsed?.videoId)
    }

    // 16. Video with playlist parameter (must extract video ID, not playlist)
    @Test
    fun testVideoInsidePlaylist() {
        val parsed = handler.parse("https://www.youtube.com/watch?v=dQw4w9WgXcQ&list=PL1234567890abcdef&index=3")
        assertNotNull(parsed)
        assertEquals("dQw4w9WgXcQ", parsed?.videoId)
    }

    // 17. URL missing https:// scheme
    @Test
    fun testMissingScheme() {
        val parsed = handler.parse("youtube.com/watch?v=dQw4w9WgXcQ")
        assertNotNull(parsed)
        assertEquals("dQw4w9WgXcQ", parsed?.videoId)
    }

    // 18. URL with trailing slash
    @Test
    fun testTrailingSlash() {
        val parsed = handler.parse("https://youtu.be/dQw4w9WgXcQ/")
        assertNotNull(parsed)
        assertEquals("dQw4w9WgXcQ", parsed?.videoId)
    }

    // 19. URL with surrounding text in clipboard
    @Test
    fun testSurroundingText() {
        val parsed = handler.parse("Hey check out this video: https://www.youtube.com/watch?v=dQw4w9WgXcQ it was awesome!")
        assertNotNull(parsed)
        assertEquals("dQw4w9WgXcQ", parsed?.videoId)
    }

    // 20. Bad: 10-character ID (must be rejected)
    @Test
    fun testInvalidTenCharacterIdReturnsNull() {
        val parsed = handler.parse("https://www.youtube.com/watch?v=1234567890")
        assertNull(parsed)
    }

    // 21. Bad: Channel URL (must return null)
    @Test
    fun testChannelUrlReturnsNull() {
        val parsed = handler.parse("https://www.youtube.com/channel/UC1234567890abcdef123456")
        assertNull(parsed)
    }

    // 22. Bad: User handle URL (must return null)
    @Test
    fun testUserHandleUrlReturnsNull() {
        val parsed = handler.parse("https://www.youtube.com/@techcreator")
        assertNull(parsed)
    }

    // 23. Bad: Playlist-only URL without v parameter (must return null)
    @Test
    fun testPlaylistOnlyUrlReturnsNull() {
        val parsed = handler.parse("https://www.youtube.com/playlist?list=PL1234567890abcdef")
        assertNull(parsed)
    }

    // 24. Bad: YouTube home page (must return null)
    @Test
    fun testYouTubeHomeReturnsNull() {
        val parsed = handler.parse("https://www.youtube.com/")
        assertNull(parsed)
    }

    // 25. Bad: External non-YouTube domain (must return null)
    @Test
    fun testNonYouTubeDomainReturnsNull() {
        val parsed = handler.parse("https://www.google.com/search?q=youtube")
        assertNull(parsed)
    }
}
