package com.example.ui

import android.net.Uri
import com.example.data.model.Episode
import com.example.data.model.EpisodeSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class BwsrEpisodeBrowserTest {

    @Test
    fun testDomainExtractionForHostServices() {
        val streamtapeUrl = "https://streamtape.com/e/x92837482"
        val doodstreamUrl = "https://doodstream.com/e/abc123xyz"
        val mixdropUrl = "https://mixdrop.co/e/m12345"

        assertEquals("streamtape.com", Uri.parse(streamtapeUrl).host)
        assertEquals("doodstream.com", Uri.parse(doodstreamUrl).host)
        assertEquals("mixdrop.co", Uri.parse(mixdropUrl).host)
    }

    @Test
    fun testEpisodeNumbersStripMapping() {
        val episodes = (1..12).map { epNum ->
            Episode(
                id = "ep-$epNum",
                seriesId = "series-1",
                episodeNumber = epNum,
                title = "Episode $epNum"
            )
        }

        assertEquals(12, episodes.size)
        assertEquals(1, episodes.first().episodeNumber)
        assertEquals(12, episodes.last().episodeNumber)

        val activeId = "ep-5"
        val selectedIndex = episodes.indexOfFirst { it.id == activeId }
        assertEquals(4, selectedIndex)
        assertEquals(5, episodes[selectedIndex].episodeNumber)
    }

    @Test
    fun testSafeUrlSchemeInspection() {
        fun isExternalIntentScheme(url: String): Boolean {
            return url.startsWith("intent://") || url.startsWith("market://") || url.startsWith("tg://")
        }

        assertTrue(isExternalIntentScheme("intent://package=com.spam#Intent;scheme=market;end"))
        assertTrue(isExternalIntentScheme("market://details?id=com.ad.spam"))
        assertFalse(isExternalIntentScheme("https://streamtape.com/e/12345"))
        assertFalse(isExternalIntentScheme("https://doodstream.com/e/54321"))
    }
}
