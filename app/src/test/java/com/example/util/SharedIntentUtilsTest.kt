package com.example.util

import android.content.Intent
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class SharedIntentUtilsTest {

    @Test
    fun testUrlExtractionFromShareCopy() {
        val tiktokText = "Check out this trending video on TikTok! https://vt.tiktok.com/ZS2x4yM/ #viral"
        val extractedTiktok = SharedIntentUtils.findFirstUrl(tiktokText)
        assertEquals("https://vt.tiktok.com/ZS2x4yM/", extractedTiktok)

        val ytText = "Watch this incredible episode on YouTube: https://youtu.be/dQw4w9WgXcQ"
        val extractedYt = SharedIntentUtils.findFirstUrl(ytText)
        assertEquals("https://youtu.be/dQw4w9WgXcQ", extractedYt)

        val fbText = "Look at this video on Facebook https://www.facebook.com/watch/?v=123456789"
        val extractedFb = SharedIntentUtils.findFirstUrl(fbText)
        assertEquals("https://www.facebook.com/watch/?v=123456789", extractedFb)
    }

    @Test
    fun testActionSendIntentParsing() {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, "Cool video https://streamtape.com/v/qjkBZomm7ohzVWe/Solo_Leveling.mp4")
        }

        val result = SharedIntentUtils.parseSharedIntent(intent)
        assertNotNull(result)
        assertEquals("https://streamtape.com/v/qjkBZomm7ohzVWe/Solo_Leveling.mp4", result?.url)
        assertEquals("streamtape", result?.platformId)
        assertEquals("Streamtape", result?.platformDisplayName)
    }

    @Test
    fun testNonSendIntentReturnsNull() {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = android.net.Uri.parse("https://qylomi.app")
        }
        val result = SharedIntentUtils.parseSharedIntent(intent)
        assertNull(result)
    }
}
