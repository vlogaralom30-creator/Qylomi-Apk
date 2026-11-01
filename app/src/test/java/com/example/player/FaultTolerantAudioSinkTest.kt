package com.example.player

import androidx.media3.common.PlaybackException
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.audio.AudioSink
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.lang.reflect.Proxy
import java.nio.ByteBuffer

@UnstableApi
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class FaultTolerantAudioSinkTest {

    @Test
    fun testIsAudioSinkDiscontinuity_detectsDiscontinuityExceptions() {
        val discontinuityEx = AudioSink.UnexpectedDiscontinuityException(2000L, 1000L)
        assertTrue(isAudioSinkDiscontinuity(discontinuityEx))

        // Nested inside another exception
        val wrappedException = RuntimeException("Playback failed", discontinuityEx)
        assertTrue(isAudioSinkDiscontinuity(wrappedException))

        // Message-based detection
        val msgException = RuntimeException("Unexpected audio track timestamp discontinuity: expected 1000000024625, got 1000000232199")
        assertTrue(isAudioSinkDiscontinuity(msgException))

        // Unrelated exception
        val ioException = java.io.IOException("Connection reset by peer")
        assertFalse(isAudioSinkDiscontinuity(ioException))
        assertFalse(isAudioSinkDiscontinuity(null))
    }

    @Test
    fun testFaultTolerantAudioSink_absorbsDiscontinuityAndFlushes() {
        var flushCallCount = 0
        var handleBufferAttempt = 0

        val fakeSink = Proxy.newProxyInstance(
            AudioSink::class.java.classLoader,
            arrayOf(AudioSink::class.java)
        ) { _, method, args ->
            when (method.name) {
                "handleBuffer" -> {
                    handleBufferAttempt++
                    if (handleBufferAttempt == 1) {
                        // Simulate unexpected timestamp discontinuity on first attempt
                        throw AudioSink.UnexpectedDiscontinuityException(2000L, 1000L)
                    }
                    true // Succeeded on retry after flush
                }
                "flush" -> {
                    flushCallCount++
                    null
                }
                "hashCode" -> 1
                "equals" -> false
                "toString" -> "FakeAudioSink"
                else -> {
                    val returnType = method.returnType
                    when {
                        returnType == Boolean::class.javaPrimitiveType -> false
                        returnType == Int::class.javaPrimitiveType -> 0
                        returnType == Long::class.javaPrimitiveType -> 0L
                        else -> null
                    }
                }
            }
        } as AudioSink

        val faultTolerantSink = FaultTolerantAudioSink(fakeSink)
        val dummyBuffer = ByteBuffer.allocate(64)

        // Must not throw UnexpectedDiscontinuityException to the caller
        val result = faultTolerantSink.handleBuffer(dummyBuffer, 1000L, 1)

        assertTrue("Buffer must be marked as handled", result)
        assertEquals("Flush must be called to resync audio clock", 1, flushCallCount)
        assertEquals("handleBuffer should retry after flush", 2, handleBufferAttempt)
    }

    @Test
    fun testFaultTolerantAudioSink_discardsCorruptedBufferIfRetryAlsoFails() {
        var flushCallCount = 0

        val alwaysFailingSink = Proxy.newProxyInstance(
            AudioSink::class.java.classLoader,
            arrayOf(AudioSink::class.java)
        ) { _, method, _ ->
            when (method.name) {
                "handleBuffer" -> {
                    throw AudioSink.UnexpectedDiscontinuityException(2000L, 1000L)
                }
                "flush" -> {
                    flushCallCount++
                    null
                }
                "hashCode" -> 1
                "equals" -> false
                "toString" -> "FailingAudioSink"
                else -> null
            }
        } as AudioSink

        val faultTolerantSink = FaultTolerantAudioSink(alwaysFailingSink)
        val buffer = ByteBuffer.allocate(64)
        buffer.put(ByteArray(32))
        buffer.flip()

        // Even if both initial call and retry throw, it should discard the buffer and return true
        val result = faultTolerantSink.handleBuffer(buffer, 1000L, 1)

        assertTrue("Should return true to allow MediaCodecAudioRenderer to continue", result)
        assertEquals("Buffer position should advance to limit", buffer.limit(), buffer.position())
        assertEquals("Flush should have been called once", 1, flushCallCount)
    }
}
