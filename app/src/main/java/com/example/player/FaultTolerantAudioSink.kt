package com.example.player

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.DefaultAudioSink
import androidx.media3.exoplayer.audio.ForwardingAudioSink
import java.nio.ByteBuffer

/**
 * An AudioSink decorator that catches UnexpectedDiscontinuityException from DefaultAudioSink
 * when audio timestamp jitter or discontinuity occurs in remote streams (such as HLS, TikTok,
 * Streamtape, or stitched clips). Instead of crashing or failing playback with an unhandled
 * MediaCodecAudioRenderer error, it flushes the sink to resync the timestamp baseline,
 * or safely swallows the single bad buffer so video and audio playback can continue seamlessly.
 */
@OptIn(UnstableApi::class)
class FaultTolerantAudioSink(private val delegate: AudioSink) : ForwardingAudioSink(delegate) {

    override fun handleBuffer(
        buffer: ByteBuffer,
        presentationTimeUs: Long,
        encodedAccessUnitCount: Int
    ): Boolean {
        return try {
            super.handleBuffer(buffer, presentationTimeUs, encodedAccessUnitCount)
        } catch (e: Exception) {
            if (isAudioSinkDiscontinuity(e)) {
                safeLogW(
                    "FaultTolerantAudioSink",
                    "Caught audio track timestamp discontinuity: ${e.message}. Resyncing audio sink."
                )
                try {
                    flush()
                    super.handleBuffer(buffer, presentationTimeUs, encodedAccessUnitCount)
                } catch (retryException: Exception) {
                    safeLogW(
                        "FaultTolerantAudioSink",
                        "Discarding discontinuous audio buffer to maintain smooth playback: ${retryException.message}"
                    )
                    // Advance buffer to end and indicate it was handled so MediaCodecAudioRenderer doesn't fail
                    buffer.position(buffer.limit())
                    true
                }
            } else {
                throw e
            }
        }
    }
}

private fun safeLogW(tag: String, message: String) {
    try {
        android.util.Log.w(tag, message)
    } catch (_: Throwable) {
        println("[$tag] $message")
    }
}

/**
 * Creates a DefaultRenderersFactory configured with FaultTolerantAudioSink and decoder fallback.
 */
@OptIn(UnstableApi::class)
fun createFaultTolerantRenderersFactory(context: Context): DefaultRenderersFactory {
    return object : DefaultRenderersFactory(context) {
        override fun buildAudioSink(
            context: Context,
            enableFloatOutput: Boolean,
            enableAudioTrackPlaybackParams: Boolean
        ): AudioSink? {
            val defaultSink = DefaultAudioSink.Builder(context)
                .setEnableFloatOutput(enableFloatOutput)
                .setEnableAudioTrackPlaybackParams(enableAudioTrackPlaybackParams)
                .build()
            return FaultTolerantAudioSink(defaultSink)
        }
    }.apply {
        setEnableDecoderFallback(true)
        setEnableAudioTrackPlaybackParams(true)
    }
}

/**
 * Helper to check whether any cause in the exception chain matches an AudioSink discontinuity or track error.
 */
fun isAudioSinkDiscontinuity(throwable: Throwable?): Boolean {
    var curr = throwable
    while (curr != null) {
        val className = curr.javaClass.name
        val message = curr.message ?: ""
        if (className.contains("UnexpectedDiscontinuityException") ||
            className.contains("AudioSink") ||
            message.contains("UnexpectedDiscontinuityException") ||
            message.contains("Unexpected audio track timestamp discontinuity") ||
            message.contains("Audio track timestamp discontinuity")
        ) {
            return true
        }
        curr = curr.cause
    }
    return false
}
