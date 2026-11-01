package com.example.ui.components.series

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import android.net.Uri
import android.view.View
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.data.model.Episode
import com.example.ui.theme.*
import kotlinx.coroutines.launch

/**
 * Clean In-App Web Player (BWSR) for Series Episodes.
 *
 * Runs raw web URLs directly (Streamtape, DoodStream, Mixdrop, etc.) in an optimized WebView.
 *
 * UI Structure (Ultra Clean):
 * - Top Bar: Close button, Series & Episode info with host domain, and Fullscreen toggle.
 * - Left Side Bar: Navigation only (< Back, > Forward).
 * - Right Side Bar: "Epi" label + vertical scrollable episode number list (1, 2, 3, 4, 5...).
 * - No extra browser controls, no adblock filters blocking streamtape/doodstream.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun BwsrEpisodeBrowserView(
    rawUrl: String,
    episodes: List<Episode>,
    currentEpisodeId: String?,
    modifier: Modifier = Modifier,
    seriesTitle: String? = null,
    episodeNumber: Int? = null,
    onSelectEpisode: (Episode) -> Unit,
    onExitBwsr: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val coroutineScope = rememberCoroutineScope()

    var webViewInstance by remember { mutableStateOf<WebView?>(null) }
    var canGoBack by remember { mutableStateOf(false) }
    var canGoForward by remember { mutableStateOf(false) }
    var loadingProgress by remember { mutableIntStateOf(0) }
    var isLoading by remember { mutableStateOf(true) }
    var currentWebUrl by remember(rawUrl) { mutableStateOf(rawUrl) }
    var currentDomain by remember(rawUrl) {
        mutableStateOf(try { Uri.parse(rawUrl).host ?: "web" } catch (_: Exception) { "web" })
    }

    // HTML5 Video Fullscreen state
    var customView by remember { mutableStateOf<View?>(null) }
    var customViewCallback by remember { mutableStateOf<WebChromeClient.CustomViewCallback?>(null) }

    // Screen Fullscreen Mode
    var isPlayerFullscreen by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()

    // Auto-scroll episode list to active episode
    LaunchedEffect(currentEpisodeId, episodes) {
        val activeIndex = episodes.indexOfFirst { it.id == currentEpisodeId }
        if (activeIndex >= 0) {
            coroutineScope.launch {
                listState.animateScrollToItem(activeIndex.coerceAtLeast(0))
            }
        }
    }

    // Update webview when rawUrl changes
    LaunchedEffect(rawUrl) {
        if (rawUrl.isNotBlank() && webViewInstance != null) {
            isLoading = true
            currentWebUrl = rawUrl
            currentDomain = try { Uri.parse(rawUrl).host ?: "web" } catch (_: Exception) { "web" }
            webViewInstance?.loadUrl(rawUrl)
        }
    }

    // Back handler: If in custom fullscreen, close it; else if webview can go back, go back; else exit BWSR
    BackHandler(enabled = true) {
        if (customView != null) {
            customViewCallback?.onCustomViewHidden()
            customView = null
            customViewCallback = null
        } else if (isPlayerFullscreen) {
            isPlayerFullscreen = false
        } else if (webViewInstance?.canGoBack() == true) {
            webViewInstance?.goBack()
        } else {
            onExitBwsr()
        }
    }

    // Screen orientation & immersive mode handling for Fullscreen
    DisposableEffect(isPlayerFullscreen, activity) {
        val window = activity?.window
        if (window != null) {
            val controller = WindowCompat.getInsetsController(window, window.decorView)
            if (isPlayerFullscreen) {
                activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                controller.hide(WindowInsetsCompat.Type.systemBars())
            } else {
                activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                controller.show(WindowInsetsCompat.Type.systemBars())
            }
        }
        onDispose {
            if (activity != null) {
                activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                activity.window?.let { win ->
                    WindowCompat.getInsetsController(win, win.decorView).show(WindowInsetsCompat.Type.systemBars())
                }
            }
        }
    }

    val containerModifier = if (isPlayerFullscreen) {
        Modifier.fillMaxSize()
    } else {
        modifier
    }

    Box(
        modifier = containerModifier
            .background(Color.Black)
            .testTag("bwsr_episode_browser_container")
    ) {
        // HTML5 Video Fullscreen View
        if (customView != null) {
            AndroidView(
                factory = {
                    FrameLayout(it).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        (customView?.parent as? ViewGroup)?.removeView(customView)
                        addView(customView)
                    }
                },
                modifier = Modifier.fillMaxSize()
            )

            // Floating button to exit HTML5 video fullscreen
            IconButton(
                onClick = {
                    customViewCallback?.onCustomViewHidden()
                    customView = null
                    customViewCallback = null
                },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.7f))
            ) {
                Icon(Icons.Default.FullscreenExit, contentDescription = "Exit Fullscreen", tint = Color.White)
            }
        } else {
            // Main Web Player WebView
            AndroidView(
                factory = { ctx ->
                    WebView(ctx).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        settings.apply {
                            javaScriptEnabled = true
                            domStorageEnabled = true
                            databaseEnabled = true
                            mediaPlaybackRequiresUserGesture = false
                            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                            allowContentAccess = true
                            allowFileAccess = true
                            loadWithOverviewMode = true
                            useWideViewPort = true
                            setSupportZoom(true)
                            builtInZoomControls = true
                            displayZoomControls = false
                            cacheMode = WebSettings.LOAD_DEFAULT
                            userAgentString = "Mozilla/5.0 (Linux; Android 14; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Mobile Safari/537.36"
                        }

                        // Clean Native WebViewClient without adblock filtering blocking host domains
                        webViewClient = object : WebViewClient() {
                            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                super.onPageStarted(view, url, favicon)
                                isLoading = true
                                url?.let {
                                    currentWebUrl = it
                                    currentDomain = try { Uri.parse(it).host ?: "web" } catch (_: Exception) { "web" }
                                }
                                canGoBack = view?.canGoBack() == true
                                canGoForward = view?.canGoForward() == true
                            }

                            override fun onPageFinished(view: WebView?, url: String?) {
                                super.onPageFinished(view, url)
                                isLoading = false
                                canGoBack = view?.canGoBack() == true
                                canGoForward = view?.canGoForward() == true
                            }

                            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                                val reqUrl = request?.url?.toString() ?: return false
                                // Consume intent schemes safely
                                if (reqUrl.startsWith("intent://") || reqUrl.startsWith("market://") || reqUrl.startsWith("tg://")) {
                                    return try {
                                        val intent = Intent.parseUri(reqUrl, Intent.URI_INTENT_SCHEME)
                                        ctx.startActivity(intent)
                                        true
                                    } catch (_: Exception) {
                                        true
                                    }
                                }
                                return false
                            }

                            override fun onRenderProcessGone(view: WebView?, detail: android.webkit.RenderProcessGoneDetail?): Boolean {
                                isLoading = false
                                try {
                                    val parent = view?.parent as? android.view.ViewGroup
                                    parent?.removeView(view)
                                    view?.destroy()
                                } catch (_: Exception) {}
                                return true
                            }
                        }

                        webChromeClient = object : WebChromeClient() {
                            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                loadingProgress = newProgress
                                if (newProgress >= 100) {
                                    isLoading = false
                                }
                                canGoBack = view?.canGoBack() == true
                                canGoForward = view?.canGoForward() == true
                            }

                            override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
                                customView = view
                                customViewCallback = callback
                                activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                            }

                            override fun onHideCustomView() {
                                customView = null
                                customViewCallback = null
                                activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                            }
                        }

                        if (rawUrl.isNotBlank()) {
                            loadUrl(rawUrl)
                        }
                        webViewInstance = this
                    }
                },
                update = { wv ->
                    webViewInstance = wv
                },
                modifier = Modifier.fillMaxSize()
            )

            // 1. CLEAN TOP BAR (Browser Address Bar & Controls)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
            ) {
                Surface(
                    color = Color.Black.copy(alpha = 0.90f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Exit BWSR Mode Button
                        IconButton(
                            onClick = { onExitBwsr() },
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Exit Web Player",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // Browser Address Bar Pill
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = Color(0xFF1E1E1E),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.18f)),
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = "Secure",
                                    tint = QYellowPrimary,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = currentWebUrl.ifBlank { rawUrl },
                                    color = Color.White.copy(alpha = 0.92f),
                                    style = SeriesTypography.micro,
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Reload",
                                    tint = Color.White.copy(alpha = 0.7f),
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clickable { webViewInstance?.reload() }
                                )
                            }
                        }

                        // Fullscreen Toggle Button
                        IconButton(
                            onClick = { isPlayerFullscreen = !isPlayerFullscreen },
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(
                                imageVector = if (isPlayerFullscreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                                contentDescription = "Toggle Fullscreen",
                                tint = QYellowPrimary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }

                // Loading progress line
                if (isLoading) {
                    LinearProgressIndicator(
                        progress = { (loadingProgress / 100f).coerceIn(0.05f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(2.5.dp),
                        color = QYellowPrimary,
                        trackColor = Color.Transparent
                    )
                }
            }

            // 2. LEFT SIDE BAR (BAM SIDE): Navigation (< Back, > Forward) ONLY
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color.Black.copy(alpha = 0.85f),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)),
                shadowElevation = 8.dp,
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 6.dp)
            ) {
                Column(
                    modifier = Modifier.padding(vertical = 6.dp, horizontal = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // < Back Button
                    IconButton(
                        onClick = {
                            if (webViewInstance?.canGoBack() == true) {
                                webViewInstance?.goBack()
                            } else {
                                webViewInstance?.loadUrl(rawUrl)
                            }
                        },
                        enabled = canGoBack || currentWebUrl != rawUrl,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = if (canGoBack || currentWebUrl != rawUrl) Color.White else Color.White.copy(alpha = 0.3f),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // > Forward Button
                    IconButton(
                        onClick = {
                            if (webViewInstance?.canGoForward() == true) {
                                webViewInstance?.goForward()
                            }
                        },
                        enabled = canGoForward,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Forward",
                            tint = if (canGoForward) Color.White else Color.White.copy(alpha = 0.3f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // 3. RIGHT SIDE BAR (DAN SIDE): "Epi" label + Episode Number Strip
            if (episodes.isNotEmpty()) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color.Black.copy(alpha = 0.88f),
                    border = BorderStroke(1.dp, QYellowPrimary.copy(alpha = 0.4f)),
                    shadowElevation = 8.dp,
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 6.dp)
                        .widthIn(min = 44.dp, max = 50.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .heightIn(max = if (isPlayerFullscreen) 280.dp else 200.dp)
                            .padding(vertical = 6.dp, horizontal = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // "Epi" Header Tag
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = QYellowPrimary,
                            modifier = Modifier.padding(bottom = 6.dp)
                        ) {
                            Text(
                                text = "Epi",
                                color = QOnYellow,
                                style = SeriesTypography.micro,
                                fontWeight = FontWeight.Black,
                                fontSize = 10.sp,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                            )
                        }

                        // Up/Down Scrollable Vertical Episode Numbers List
                        LazyColumn(
                            state = listState,
                            verticalArrangement = Arrangement.spacedBy(5.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.weight(1f, fill = false)
                        ) {
                            items(episodes, key = { it.id }) { ep ->
                                val isSelected = ep.id == currentEpisodeId
                                Surface(
                                    shape = CircleShape,
                                    color = if (isSelected) QYellowPrimary else Color.White.copy(alpha = 0.15f),
                                    border = if (isSelected) BorderStroke(1.5.dp, Color.White) else BorderStroke(0.5.dp, Color.White.copy(alpha = 0.2f)),
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clickable {
                                            if (!isSelected) {
                                                onSelectEpisode(ep)
                                            }
                                        }
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = "${ep.episodeNumber}",
                                            color = if (isSelected) QOnYellow else Color.White,
                                            style = SeriesTypography.label,
                                            fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
                                            fontSize = 12.sp,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
