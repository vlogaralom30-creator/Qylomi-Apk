package com.example.ui.navigation

import com.example.ui.components.SocialTab
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AppNavigatorTest {

    @Test
    fun testPushAndPop() {
        val nav = AppNavigator()
        assertNull(nav.top)
        assertFalse(nav.canPop)

        nav.push(AppDestination.SeriesHome)
        assertEquals(AppDestination.SeriesHome, nav.top)
        assertTrue(nav.canPop)

        nav.push(AppDestination.SeriesDetails("naruto"))
        assertEquals(AppDestination.SeriesDetails("naruto"), nav.top)
        assertEquals(2, nav.stack.size)

        // Deduplication test: pushing same top does nothing
        nav.push(AppDestination.SeriesDetails("naruto"))
        assertEquals(2, nav.stack.size)

        nav.push(AppDestination.EpisodePlayer("s1", "e1", 15))
        assertEquals(3, nav.stack.size)

        // Pop Player -> Details
        val pop1 = nav.pop()
        assertTrue(pop1)
        assertEquals(AppDestination.SeriesDetails("naruto"), nav.top)

        // Pop Details -> SeriesHome
        val pop2 = nav.pop()
        assertTrue(pop2)
        assertEquals(AppDestination.SeriesHome, nav.top)

        // Pop SeriesHome -> Root (stack empty)
        val pop3 = nav.pop()
        assertTrue(pop3)
        assertNull(nav.top)
        assertFalse(nav.canPop)

        // Pop on empty stack returns false
        assertFalse(nav.pop())
    }

    @Test
    fun testReplaceAll() {
        val nav = AppNavigator()
        nav.push(AppDestination.SeriesHome)
        nav.push(AppDestination.SeriesDetails("bleach"))

        // Jumping to Reels from drawer replaces the entire stack
        nav.replaceAll(AppDestination.Reels())
        assertEquals(1, nav.stack.size)
        assertEquals(AppDestination.Reels(), nav.top)

        // Jumping to Groups replaces Reels
        nav.replaceAll(AppDestination.Groups)
        assertEquals(1, nav.stack.size)
        assertEquals(AppDestination.Groups, nav.top)
    }

    @Test
    fun testSwitchTabClearsStack() {
        val nav = AppNavigator(initialTab = SocialTab.FEED)
        nav.push(AppDestination.CreatorStudio)
        assertTrue(nav.stack.isNotEmpty())

        nav.switchTab(SocialTab.NOTIFICATIONS)
        assertEquals(SocialTab.NOTIFICATIONS, nav.rootTab.value)
        assertTrue(nav.stack.isEmpty())
        assertNull(nav.top)
    }

    @Test
    fun testSaverEncodeAndDecode() {
        val original = AppNavigator(initialTab = SocialTab.EXPLORE)
        original.push(AppDestination.SeriesDetails("one-piece"))
        original.push(AppDestination.EpisodePlayer("series123", "ep456", 120))

        val scope = androidx.compose.runtime.saveable.SaverScope { true }
        val savedList = with(AppNavigator.Saver) { scope.save(original) }
        assertTrue(savedList != null)

        val restored = AppNavigator.Saver.restore(savedList!!)
        assertTrue(restored != null)
        assertEquals(SocialTab.EXPLORE, restored!!.rootTab.value)
        assertEquals(2, restored.stack.size)
        assertEquals(AppDestination.SeriesDetails("one-piece"), restored.stack[0])
        val topPlayer = restored.stack[1] as AppDestination.EpisodePlayer
        assertEquals("series123", topPlayer.seriesId)
        assertEquals("ep456", topPlayer.episodeId)
        assertEquals(120, topPlayer.startPositionSec)
    }
}
