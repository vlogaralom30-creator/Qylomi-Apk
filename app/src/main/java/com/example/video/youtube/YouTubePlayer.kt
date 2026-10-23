package com.example.video.youtube

import android.util.Log

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.ui.theme.QLikeRed
import com.example.ui.theme.QOnSurface
import com.example.ui.theme.QOnSurfaceVariant
import com.example.ui.theme.QOnYellow
import com.example.ui.theme.QSurfaceVariant
import com.example.ui.theme.QYellowDark
import com.example.ui.theme.QYellowPrimary
import com.example.video.ExtractedVideo
import com.example.video.PlayerConfig
import com.example.video.PlayerController
import kotlinx.coroutines.delay
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.UUID

/**
 * JavaScript Bridge exposed to the YouTube IFrame player.
 */
private class YtBridge(
    private val onReadyCallback: () -> Unit,
    private val onStateChangeCallback: (Int) -> Unit,
    private val onErrorCallback: (Int) -> Unit,
    private val onProgressCallback: (Float, Float) -> Unit,
    private val onAutoplayBlockedCallback: () -> Unit
) {
    private val mainHandler = Handler(Looper.getMainLooper())

    @JavascriptInterface
    fun onReady() {
        Log.d("YT_FEED", "bridge onReady")
        mainHandler.post { onReadyCallback() }
    }

    @JavascriptInterface
    fun onStateChange(state: Int) {
        Log.d("YT_FEED", "bridge onStateChange: $state")
        mainHandler.post { onStateChangeCallback(state) }
    }

    @JavascriptInterface
    fun onError(code: Int) {
        Log.d("YT_FEED", "bridge onError: $code")
        mainHandler.post { onErrorCallback(code) }
    }

    @JavascriptInterface
    fun onProgress(curr: Float, dur: Float) {
        // Log.d("YT_FEED", "bridge onProgress: $curr / $dur") // too spammy
        mainHandler.post { onProgressCallback(curr, dur) }
    }

    @JavascriptInterface
    fun onAutoplayBlocked() {
        Log.d("YT_FEED", "bridge onAutoplayBlocked")
        mainHandler.post { onAutoplayBlockedCallback() }
    }
}

/**
 * Dedicated, policy-compliant YouTube Player Composable utilizing the official IFrame Player API.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun YouTubePlayer(
    video: ExtractedVideo,
    modifier: Modifier = Modifier,
    config: PlayerConfig = PlayerConfig(),
    controller: PlayerController? = null
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val instanceId = remember { "yt_${UUID.randomUUID()}" }
    val internalController = remember(instanceId) { YouTubePlayerController(instanceId) }
    val activeController = (controller as? YouTubePlayerController) ?: internalController

    var isReady by remember { mutableStateOf(false) }
    var errorCode by remember { mutableStateOf<Int?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isTimedOut by remember { mutableStateOf(false) }
    var retryTrigger by remember { mutableStateOf(0) }
    var customFullscreenView by remember { mutableStateOf<View?>(null) }
    var customViewCallback by remember { mutableStateOf<WebChromeClient.CustomViewCallback?>(null) }

    val activeAppPlayerId by YouTubePlayerManager.activePlayerId.collectAsStateWithLifecycle()

    // Pause when another YouTube video becomes active
    LaunchedEffect(activeAppPlayerId) {
        if (activeAppPlayerId != null && activeAppPlayerId != instanceId && isReady) {
            activeController.pause()
        }
    }

    // 8-second ready timeout guard
    LaunchedEffect(isReady, errorCode, retryTrigger) {
        if (!isReady && errorCode == null) {
            isTimedOut = false
            delay(8000)
            if (!isReady && errorCode == null) {
                isTimedOut = true
                errorMessage = "Player took too long to load. Tap to retry or open in YouTube."
            }
        }
    }

    // Lifecycle Observer: pause on stop, restore only if previously playing
    val currentIsReady by rememberUpdatedState(isReady)
    DisposableEffect(lifecycleOwner, activeController) {
        var wasPlayingBeforePause = false
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE, Lifecycle.Event.ON_STOP -> {
                    if (currentIsReady && activeController.playerState == 1) {
                        wasPlayingBeforePause = true
                        activeController.pause()
                    }
                }
                Lifecycle.Event.ON_RESUME -> {
                    if (currentIsReady && wasPlayingBeforePause) {
                        wasPlayingBeforePause = false
                        activeController.play()
                    }
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            activeController.release()
        }
    }

    // Fullscreen View Dialog
    if (customFullscreenView != null) {
        Dialog(
            onDismissRequest = {
                customViewCallback?.onCustomViewHidden()
                customFullscreenView = null
                customViewCallback = null
            },
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = false
            )
        ) {
            BackHandler {
                customViewCallback?.onCustomViewHidden()
                customFullscreenView = null
                customViewCallback = null
            }
            AndroidView(
                factory = { customFullscreenView!! },
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
            )
        }
    }

    // Size & Viewport calculations: Minimum 200x200 dp enforced per API requirements
    val isShortOrVertical = video.isShort || video.isVertical

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 200.dp)
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        val stageModifier = if (isShortOrVertical) {
            Modifier
                .fillMaxWidth()
                .heightIn(min = 200.dp, max = 560.dp)
                .aspectRatio(9f / 16f, matchHeightConstraintsFirst = true)
        } else {
            Modifier
                .fillMaxWidth()
                .heightIn(min = 200.dp)
                .aspectRatio(16f / 9f)
        }

        val hasFailure = errorCode != null || isTimedOut

        if (hasFailure) {
            // Friendly error card with direct YouTube app link
            YouTubeErrorFallbackCard(
                video = video,
                errorCode = errorCode,
                errorMessage = errorMessage ?: getFriendlyErrorMessage(errorCode),
                onRetry = {
                    errorCode = null
                    errorMessage = null
                    isTimedOut = false
                    isReady = false
                    retryTrigger++
                },
                modifier = stageModifier
            )
        } else {
            Box(
                modifier = stageModifier,
                contentAlignment = Alignment.Center
            ) {
                var webViewCreatedSuccessfully by remember { mutableStateOf(true) }

                if (webViewCreatedSuccessfully) {
                    AndroidView<View>(
                        factory = { ctx: Context ->
                            try {
                                createConfiguredWebView(
                                    context = ctx,
                                    video = video,
                                    config = config,
                                    activeController = activeController,
                                    onReady = {
                                        isReady = true
                                        isTimedOut = false
                                        activeController.isReady = true
                                    },
                                    onStateChange = { state ->
                                        activeController.playerState = state
                                    },
                                    onError = { code ->
                                        errorCode = code
                                        errorMessage = getFriendlyErrorMessage(code)
                                    },
                                    onProgress = { curr, dur ->
                                        activeController.currentProgressSec = curr
                                        activeController.totalDurationSec = dur
                                    },
                                    onShowCustomView = { view, callback ->
                                        customFullscreenView = view
                                        customViewCallback = callback
                                    },
                                    onHideCustomView = {
                                        customFullscreenView = null
                                        customViewCallback = null
                                    }
                                )
                            } catch (e: Throwable) {
                                webViewCreatedSuccessfully = false
                                errorMessage = "WebView could not be initialized on this device"
                                View(ctx)
                            }
                        },
                        modifier = Modifier.fillMaxSize(),
                        onRelease = { view: View ->
                            try {
                                (view as? WebView)?.let { webView ->
                                    webView.removeJavascriptInterface("YtBridge")
                                    webView.stopLoading()
                                    webView.loadUrl("about:blank")
                                    (webView.parent as? ViewGroup)?.removeView(webView)
                                    webView.destroy()
                                }
                            } catch (_: Exception) {}
                        }
                    )
                } else {
                    YouTubeErrorFallbackCard(
                        video = video,
                        errorCode = 5,
                        errorMessage = "Android System WebView unavailable or crashed",
                        onRetry = {
                            webViewCreatedSuccessfully = true
                            retryTrigger++
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // Loading overlay while player initializes
                if (!isReady && !hasFailure) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black),
                        contentAlignment = Alignment.Center
                    ) {
                        if (!video.thumbnailUrl.isNullOrBlank()) {
                            AsyncImage(
                                model = video.thumbnailUrl,
                                contentDescription = video.title ?: "YouTube Video",
                                contentScale = ContentScale.Fit,
                                modifier = Modifier.fillMaxSize()
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black.copy(alpha = 0.45f))
                            )
                        }
                        CircularProgressIndicator(
                            color = QYellowPrimary,
                            modifier = Modifier.size(36.dp),
                            strokeWidth = 3.dp
                        )
                    }
                }
            }
        }
    }
}

/**
 * Creates and configures the WebView instance loading the official YouTube IFrame Player.
 */
@SuppressLint("SetJavaScriptEnabled")
private fun createConfiguredWebView(
    context: Context,
    video: ExtractedVideo,
    config: PlayerConfig,
    activeController: YouTubePlayerController,
    onReady: () -> Unit,
    onStateChange: (Int) -> Unit,
    onError: (Int) -> Unit,
    onProgress: (Float, Float) -> Unit,
    onShowCustomView: (View, WebChromeClient.CustomViewCallback) -> Unit,
    onHideCustomView: () -> Unit
): WebView {
    val webView = WebView(context)
    Log.d("YT_FEED", "WebView created")
    activeController.attachWebView(webView)

    webView.layoutParams = ViewGroup.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.MATCH_PARENT
    )

    // Hardware acceleration and transparent background
    webView.setBackgroundColor(android.graphics.Color.TRANSPARENT)
    webView.isHapticFeedbackEnabled = false
    webView.isLongClickable = false
    webView.setOnLongClickListener { true }

    webView.settings.apply {
        javaScriptEnabled = true
        domStorageEnabled = true
        mediaPlaybackRequiresUserGesture = false
        allowFileAccess = false
        allowContentAccess = false
        mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
        useWideViewPort = true
        loadWithOverviewMode = true
        cacheMode = WebSettings.LOAD_DEFAULT
    }

    val bridge = YtBridge(
        onReadyCallback = onReady,
        onStateChangeCallback = onStateChange,
        onErrorCallback = onError,
        onProgressCallback = onProgress,
        onAutoplayBlockedCallback = onReady
    )
    webView.addJavascriptInterface(bridge, "YtBridge")

    webView.webChromeClient = object : WebChromeClient() {
        override fun onShowCustomView(view: View, callback: CustomViewCallback) {
            onShowCustomView(view, callback)
        }

        override fun onHideCustomView() {
            onHideCustomView()
        }

        override fun onConsoleMessage(consoleMessage: android.webkit.ConsoleMessage?): Boolean {
            Log.d("YT_FEED", "console: ${consoleMessage?.message()}")
            return super.onConsoleMessage(consoleMessage)
        }
    }

    webView.webViewClient = object : WebViewClient() {
        override fun onPageFinished(view: WebView?, url: String?) {
            Log.d("YT_FEED", "onPageFinished: $url")
            super.onPageFinished(view, url)
        }

        override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: android.webkit.WebResourceError?) {
            Log.d("YT_FEED", "onReceivedError: ${error?.description}")
            super.onReceivedError(view, request, error)
        }

        override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
            val targetUrl = request?.url?.toString() ?: return false
            val host = request.url.host?.lowercase() ?: ""

            // Keep allowed video player domain requests inside the WebView
            val isAllowedDomain = host.endsWith("youtube.com") ||
                    host.endsWith("youtu.be") ||
                    host.endsWith("ytimg.com") ||
                    host.endsWith("googlevideo.com") ||
                    host.endsWith("google.com")

            if (isAllowedDomain) {
                // If the user clicks on an ad or video title navigation link, open externally
                if (targetUrl.contains("/watch?v=") && !targetUrl.contains("embed")) {
                    openExternalUrl(context, targetUrl)
                    return true
                }
                return false
            }

            // Open external URLs in external browser / app
            openExternalUrl(context, targetUrl)
            return true
        }

        override fun onRenderProcessGone(view: WebView?, detail: RenderProcessGoneDetail?): Boolean {
            onError(5)
            try {
                view?.destroy()
            } catch (_: Exception) {}
            return true
        }
    }

    // Read HTML template from assets
    val rawHtml = loadAssetHtml(context, "youtube_player.html")

    // Error 153 prevention: origin and baseUrl match the application identity
    val originDomain = "https://${context.packageName}"

    val filledHtml = rawHtml
        .replace("__VIDEO_ID__", video.videoId ?: "")
        .replace("__AUTOPLAY__", if (config.autoPlay) "1" else "0")
        .replace("__MUTED__", if (config.muted) "1" else "0")
        .replace("__LOOP__", if (config.loop) "1" else "0")
        .replace("__CONTROLS__", if (config.showControls) "1" else "0")
        .replace("__START_SECONDS__", video.startSeconds.toString())
        .replace("__ORIGIN__", originDomain)

    webView.loadDataWithBaseURL(
        originDomain,
        filledHtml,
        "text/html",
        "utf-8",
        null
    )

    return webView
}

/**
 * Loads raw HTML string from assets safely.
 */
private fun loadAssetHtml(context: Context, filename: String): String {
    return try {
        context.assets.open(filename).use { stream ->
            BufferedReader(InputStreamReader(stream)).readText()
        }
    } catch (_: Exception) {
        // Fallback embedded minimal HTML
        """
        <!DOCTYPE html>
        <html>
        <head>
        <meta name="viewport" content="width=device-width, initial-scale=1.0">
        <style>body{margin:0;background:#000;}</style>
        </head>
        <body>
        <div id="player"></div>
        <script src="https://www.youtube.com/iframe_api"></script>
        <script>
        function onYouTubeIframeAPIReady(){
            new YT.Player('player', {
                videoId: '__VIDEO_ID__',
                playerVars: { playsinline: 1, rel: 0, modestbranding: 1, origin: '__ORIGIN__' },
                events: {
                    'onReady': function(){ window.YtBridge && window.YtBridge.onReady(); },
                    'onError': function(e){ window.YtBridge && window.YtBridge.onError(e.data); }
                }
            });
        }
        </script>
        </body>
        </html>
        """.trimIndent()
    }
}

/**
 * Maps YouTube IFrame error codes to clear, friendly user explanations.
 */
private fun getFriendlyErrorMessage(code: Int?): String {
    return when (code) {
        2 -> "Invalid video link or video ID."
        5 -> "HTML5 video player encountered an error. Tap to retry."
        100 -> "This video was removed or is marked private."
        101, 150 -> "The owner has disabled embedding for this video."
        153 -> "Playback restricted by YouTube API client identity (Error 153)."
        else -> "Could not play video inside the app."
    }
}

/**
 * Friendly Error & Embedding Disabled Fallback Card with thumbnail and "Open in YouTube" button.
 */
@Composable
fun YouTubeErrorFallbackCard(
    video: ExtractedVideo,
    errorCode: Int?,
    errorMessage: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF111827))
            .border(1.dp, Color(0xFF374151), RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center
    ) {
        // Background dim thumbnail
        if (!video.thumbnailUrl.isNullOrBlank()) {
            AsyncImage(
                model = video.thumbnailUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.82f))
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(16.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(if (errorCode == 101 || errorCode == 150) QLikeRed else Color(0xFF4B5563))
            ) {
                Icon(
                    imageVector = Icons.Default.ErrorOutline,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = video.title ?: "YouTube Video",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 1,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = errorMessage,
                fontSize = 12.sp,
                color = Color(0xFFD1D5DB),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Open in YouTube button
                Button(
                    onClick = {
                        openYouTubeDirect(context, video.videoId, video.originalUrl)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = QLikeRed,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.OpenInNew,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Open in YouTube", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                // Retry Button (shown for timeout or HTML5 error 5)
                if (errorCode == 5 || errorCode == null) {
                    OutlinedButton(
                        onClick = onRetry,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Retry", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

/**
 * Opens a video directly in the YouTube native app or falls back to browser.
 */
fun openYouTubeDirect(context: Context, videoId: String?, originalUrl: String) {
    if (!videoId.isNullOrBlank()) {
        try {
            val appIntent = Intent(Intent.ACTION_VIEW, Uri.parse("vnd.youtube:$videoId"))
            appIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            context.startActivity(appIntent)
            return
        } catch (_: Exception) {}
    }
    openExternalUrl(context, originalUrl)
}

private fun openExternalUrl(context: Context, url: String) {
    try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        context.startActivity(intent)
    } catch (_: Exception) {}
}
