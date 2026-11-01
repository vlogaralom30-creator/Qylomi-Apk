package com.example

import com.example.analytics.UserEventTracker
import com.example.data.api.FlexibleBooleanAdapter
import com.example.data.api.FlexibleStringAdapter
import com.example.data.model.FeedRankItem
import com.example.data.model.FeedTab
import com.example.data.model.Post
import com.example.data.model.UserEventRecord
import com.example.data.model.UserEventType
import com.example.data.model.VideoPost
import com.example.util.FeedPresets
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FeedAlgorithmTest {

    private lateinit var moshi: Moshi

    @Before
    fun setUp() {
        moshi = Moshi.Builder()
            .add(FlexibleBooleanAdapter())
            .add(FlexibleStringAdapter())
            .addLast(KotlinJsonAdapterFactory())
            .build()
    }

    @Test
    fun testUserEventType_allowedValuesAndSerialization() {
        // Spec Part 3 requires exact 8 enum values
        val expectedValues = setOf("impression", "open", "like", "comment", "relay", "bookmark", "share", "hide")
        val actualValues = UserEventType.values().map { it.value }.toSet()
        assertEquals(expectedValues, actualValues)

        // Test Moshi serialization of UserEventRecord without user_id
        val record = UserEventRecord(
            postId = "post-123",
            eventType = UserEventType.IMPRESSION.value
        )
        val adapter = moshi.adapter(UserEventRecord::class.java)
        val json = adapter.toJson(record)
        assertTrue(json.contains("\"post_id\":\"post-123\""))
        assertTrue(json.contains("\"event_type\":\"impression\""))
        // client-sent user_id MUST NOT be in the payload
        assertFalse(json.contains("user_id"))
    }

    @Test
    fun testFeedPresets_bengaliCharacterDetection() {
        // Bengali Unicode range U+0980..U+09FF
        assertTrue(FeedPresets.hasBengaliCharacters("বাংলা"))
        assertTrue(FeedPresets.hasBengaliCharacters("Hello বাংলাদেশ!"))
        assertTrue(FeedPresets.hasBengaliCharacters("আমার সোনার বাংলা"))
        assertFalse(FeedPresets.hasBengaliCharacters("Hello world"))
        assertFalse(FeedPresets.hasBengaliCharacters("12345!@#$"))
        assertFalse(FeedPresets.hasBengaliCharacters(null))
        assertFalse(FeedPresets.hasBengaliCharacters(""))
    }

    @Test
    fun testFeedPresets_isBanglaPost() {
        val banglaTitlePost = Post(id = "1", title = "নতুন আপডেট", content = "Everything is great")
        assertTrue(FeedPresets.isBanglaPost(banglaTitlePost))

        val banglaContentPost = Post(id = "2", title = "Update", content = "আজকের দিনে নতুন খবর")
        assertTrue(FeedPresets.isBanglaPost(banglaContentPost))

        val tagInContentPost = Post(id = "3", title = "Post", content = "Check out this #bangla news")
        assertTrue(FeedPresets.isBanglaPost(tagInContentPost))

        val englishPost = Post(id = "4", title = "English Title", content = "English Content only")
        assertFalse(FeedPresets.isBanglaPost(englishPost))
    }

    @Test
    fun testFeedPresets_isVideoPost() {
        val directVideoPost = Post(
            id = "1",
            content = "Check video",
            videoPosts = listOf(VideoPost(id = "v1", postId = "1", videoUrl = "https://example.com/video.mp4"))
        )
        assertTrue(FeedPresets.isVideoPost(directVideoPost))

        val streamtapePost = Post(
            id = "2",
            content = "Watch this",
            externalUrl = "https://streamtape.com/e/q3qZQ0Ld7giw1Y/"
        )
        assertTrue(FeedPresets.isVideoPost(streamtapePost))

        val youtubePost = Post(
            id = "3",
            content = "YouTube stream",
            externalUrl = "https://www.youtube.com/watch?v=dQw4w9WgXcQ"
        )
        assertTrue(FeedPresets.isVideoPost(youtubePost))

        val textOnlyPost = Post(
            id = "4",
            content = "Just plain text without links or video attachment"
        )
        assertFalse(FeedPresets.isVideoPost(textOnlyPost))
    }

    @Test
    fun testFeedPresets_trendingScoreCalculation() {
        // Score formula: appreciations * 2 + replies * 3
        val post = Post(
            id = "p1",
            appreciationsCount = 10,
            repliesCount = 5
        )
        val expectedScore = (10L * 2L) + (5L * 3L) // 20 + 15 = 35
        assertEquals(35L, FeedPresets.trendingScore(post))

        val zeroPost = Post(id = "p2", appreciationsCount = 0, repliesCount = 0)
        assertEquals(0L, FeedPresets.trendingScore(zeroPost))
    }

    @Test
    fun testFeedPresets_applyPresetTrendingOrdering() {
        val postLow = Post(id = "p1", appreciationsCount = 2, repliesCount = 1) // 4 + 3 = 7
        val postHigh = Post(id = "p2", appreciationsCount = 100, repliesCount = 20) // 200 + 60 = 260
        val postMid = Post(id = "p3", appreciationsCount = 20, repliesCount = 10) // 40 + 30 = 70

        val unordered = listOf(postLow, postHigh, postMid)
        val sorted = FeedPresets.applyPreset(unordered, FeedTab.TRENDING)

        assertEquals(listOf("p2", "p3", "p1"), sorted.map { it.id })
    }

    @Test
    fun testServerRankOrderRestoration() {
        // Simulated server ranking from get_feed RPC
        val serverRankItems = listOf(
            FeedRankItem(postId = "post-C", score = 9.8),
            FeedRankItem(postId = "post-A", score = 8.5),
            FeedRankItem(postId = "post-D", score = 7.1),
            FeedRankItem(postId = "post-B", score = 5.2)
        )

        // Database returned posts in arbitrary/unordered sequence
        val fetchedPosts = listOf(
            Post(id = "post-A", title = "A"),
            Post(id = "post-B", title = "B"),
            Post(id = "post-C", title = "C"),
            Post(id = "post-D", title = "D")
        )
        val postsMap = fetchedPosts.associateBy { it.id }

        // Reconstruct client list strictly following server rank order
        val reconstructed = serverRankItems.mapIndexedNotNull { index, item ->
            val post = postsMap[item.postId] ?: return@mapIndexedNotNull null
            post.copy(
                rankScore = item.score,
                rankPosition = index + 1
            )
        }

        // Verify exact order preservation
        assertEquals(listOf("post-C", "post-A", "post-D", "post-B"), reconstructed.map { it.id })
        assertEquals(1, reconstructed[0].rankPosition)
        assertEquals(9.8, reconstructed[0].rankScore!!, 0.001)
        assertEquals(4, reconstructed[3].rankPosition)
        assertEquals(5.2, reconstructed[3].rankScore!!, 0.001)
    }

    @Test
    fun testExclusionListCappingAt400() {
        // Generate 500 post IDs
        val largeShownIds = (1..500).map { "post-$it" }.toSet()
        val cappedList = largeShownIds.toList().takeLast(400)

        assertEquals(400, cappedList.size)
        // Should contain the newest items
        assertTrue(cappedList.contains("post-500"))
    }

    @Test
    fun testUserEventTracker_impressionDeduplicationAndGuestSkipping() {
        val guestPostId = "guest-post-${System.currentTimeMillis()}"
        com.example.data.api.SupabaseClient.setAuthToken(null)
        UserEventTracker.track(guestPostId, UserEventType.IMPRESSION)
        // Guests must be skipped silently
        assertFalse(UserEventTracker.isImpressionSeen(guestPostId))

        // Authenticated user
        com.example.data.api.SupabaseClient.setAuthToken("test-authenticated-token")
        val testPostId = "unique-test-post-${System.currentTimeMillis()}"
        assertFalse(UserEventTracker.isImpressionSeen(testPostId))

        // Track impression
        UserEventTracker.track(testPostId, UserEventType.IMPRESSION)

        // Now it must be recorded as seen
        assertTrue(UserEventTracker.isImpressionSeen(testPostId))
    }
}
