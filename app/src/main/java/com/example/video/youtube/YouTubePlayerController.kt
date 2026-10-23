package com.example.video.youtube

import android.webkit.WebView
import com.example.video.PlayerController
import java.lang.ref.WeakReference

/**
 * Controller implementation for communicating with YouTube IFrame Player inside a WebView.
 */
class YouTubePlayerController(
    val instanceId: String
) : PlayerController {

    private var webViewRef: WeakReference<WebView>? = null

    var isReady: Boolean = false
        internal set

    var playerState: Int = -1 // -1: unstarted, 0: ended, 1: playing, 2: paused, 3: buffering, 5: cued
        internal set

    var currentProgressSec: Float = 0f
        internal set

    var totalDurationSec: Float = 0f
        internal set

    var isMuted: Boolean = false
        internal set

    fun attachWebView(webView: WebView) {
        webViewRef = WeakReference(webView)
    }

    override fun play() {
        YouTubePlayerManager.requestActive(instanceId)
        executeJs("playVideo()")
    }

    override fun pause() {
        executeJs("pauseVideo()")
    }

    override fun mute() {
        isMuted = true
        executeJs("muteVideo()")
    }

    override fun unMute() {
        isMuted = false
        executeJs("unMuteVideo()")
    }

    override fun seekTo(sec: Float) {
        executeJs("seekTo($sec, true)")
    }

    fun loadVideo(videoId: String, startSeconds: Int = 0) {
        executeJs("loadVideo('$videoId', $startSeconds)")
    }

    override fun release() {
        YouTubePlayerManager.release(instanceId)
        executeJs("pauseVideo()")
        webViewRef?.clear()
        webViewRef = null
    }

    private fun executeJs(code: String) {
        webViewRef?.get()?.post {
            try {
                webViewRef?.get()?.evaluateJavascript(code, null)
            } catch (_: Exception) {}
        }
    }
}
