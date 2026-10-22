package com.example

import com.example.data.model.UserProfile
import com.example.util.MediaParser
import com.example.util.MediaType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun userProfile_initialsComputation() {
        val profile = UserProfile(
            id = "test-123",
            displayName = "Rony Ahammad",
            username = "ronyvi"
        )
        assertEquals("RA", profile.initials)
        assertEquals("Rony Ahammad", profile.effectiveDisplayName)
        assertEquals("ronyvi", profile.effectiveUsername)
    }

    @Test
    fun userProfile_fallbackUsername() {
        val profile = UserProfile(
            id = "abcdef123456",
            displayName = null,
            username = null
        )
        assertEquals("user_abcdef", profile.effectiveUsername)
        assertEquals("Q Creator", profile.effectiveDisplayName)
    }

    @Test
    fun mediaParser_detectsYouTube() {
        val parsed = MediaParser.parseSingleUrl("https://youtu.be/d1vUvvOvCyI?si=E0oEttnYb7RbUTWo")
        assertNotNull(parsed)
        assertEquals(MediaType.YOUTUBE, parsed?.type)
        assertEquals("d1vUvvOvCyI", parsed?.id)
    }

    @Test
    fun mediaParser_detectsDirectImage() {
        val parsed = MediaParser.parseSingleUrl("https://i.ibb.co/yBgSrpjx/blob.jpg")
        assertNotNull(parsed)
        assertEquals(MediaType.DIRECT_IMAGE, parsed?.type)
    }

    @Test
    fun mediaParser_cleanContent() {
        val raw = "Hello world\n<!-- qylomi:media:v1 {\"previews\":[\"...\"]} -->"
        val cleaned = MediaParser.cleanContentText(raw)
        assertEquals("Hello world", cleaned)
    }
}
