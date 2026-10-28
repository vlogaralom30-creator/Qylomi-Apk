package com.example

import com.example.data.model.Episode
import com.example.data.model.PixelArtData
import com.example.data.model.SavedAccount
import com.example.ui.navigation.AppDestination
import com.example.ui.navigation.AppNavigator
import com.example.ui.theme.ThemeMode
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class SecurityAndFeatureRegressionTest {

    private lateinit var moshi: Moshi

    @Before
    fun setUp() {
        moshi = Moshi.Builder()
            .addLast(KotlinJsonAdapterFactory())
            .build()
    }

    @Test
    fun savedAccount_noPlaintextPasswordStored() {
        val account = SavedAccount(
            email = "user@qylomi.app",
            username = "qylomi_user",
            displayName = "Qylomi Creator",
            avatarUrl = "https://example.com/avatar.png",
            refreshToken = "refresh_token_abc",
            accessToken = "access_token_xyz"
        )

        val adapter = moshi.adapter(SavedAccount::class.java)
        val json = adapter.toJson(account)

        // Verify that 'pass' field is not serialized or present
        assertFalse("JSON must not contain 'pass'", json.contains("\"pass\""))
        assertTrue("JSON must contain refreshToken", json.contains("refresh_token_abc"))

        val deserialized = adapter.fromJson(json)
        assertNotNull(deserialized)
        assertEquals(account.email, deserialized?.email)
        assertEquals(account.username, deserialized?.username)
        assertEquals(account.refreshToken, deserialized?.refreshToken)
    }

    @Test
    fun appDestination_settingsDestinationEncodeDecode() {
        val dest = AppDestination.Settings
        val encoded = AppNavigator.encodeDestination(dest)
        assertEquals("settings", encoded)

        val decoded = AppNavigator.decodeDestination(encoded)
        assertTrue("Decoded destination must be AppDestination.Settings", decoded is AppDestination.Settings)
    }

    @Test
    fun themeMode_allValuesHandled() {
        assertEquals(3, ThemeMode.entries.size)
        assertTrue(ThemeMode.entries.contains(ThemeMode.SYSTEM))
        assertTrue(ThemeMode.entries.contains(ThemeMode.LIGHT))
        assertTrue(ThemeMode.entries.contains(ThemeMode.DARK))
    }

    @Test
    fun pixelArtData_serializationRoundTrip() {
        val pixelArt = PixelArtData(
            gridSize = 16,
            colors = mapOf("0" to "#000000", "1" to "#FFFFFF", "2" to "#FFDD00"),
            pixels = listOf(0, 1, 2, 0, 1, 2)
        )

        val adapter = moshi.adapter(PixelArtData::class.java)
        val json = adapter.toJson(pixelArt)

        assertTrue(json.contains("\"gridSize\":16"))
        val restored = adapter.fromJson(json)
        assertNotNull(restored)
        assertEquals(16, restored?.gridSize)
        assertEquals(3, restored?.colors?.size)
        assertEquals(6, restored?.pixels?.size)
    }

    @Test
    fun episodeList_boundsCheckSafety() {
        val episodes = listOf(
            Episode(id = "ep-1", title = "Episode 1", episodeNumber = 1),
            Episode(id = "ep-2", title = "Episode 2", episodeNumber = 2),
            Episode(id = "ep-3", title = "Episode 3", episodeNumber = 3)
        )

        fun getNext(currentId: String?): Episode? {
            val idx = episodes.indexOfFirst { it.id == currentId }
            return if (idx in 0 until episodes.size - 1) episodes[idx + 1] else null
        }

        fun getPrev(currentId: String?): Episode? {
            val idx = episodes.indexOfFirst { it.id == currentId }
            return if (idx > 0) episodes[idx - 1] else null
        }

        assertNull("Previous of first episode must be null", getPrev("ep-1"))
        assertEquals("ep-2", getNext("ep-1")?.id)
        assertEquals("ep-1", getPrev("ep-2")?.id)
        assertEquals("ep-3", getNext("ep-2")?.id)
        assertNull("Next of last episode must be null", getNext("ep-3"))
        assertEquals("ep-2", getPrev("ep-3")?.id)
        assertNull("Unknown episode returns null next", getNext("ep-unknown"))
        assertNull("Unknown episode returns null prev", getPrev("ep-unknown"))
    }
}
