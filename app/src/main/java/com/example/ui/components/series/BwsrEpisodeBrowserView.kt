package com.example.ui.components.series

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
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
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.*
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
 * In-App Browser Mode (BWSR) for Series Episode Video Player.
 *
 * Runs the raw episode URL directly in an optimized in-app WebView without forcing
 * an internal native player. Perfect for host services (Streamtape, DoodStream,
 * Mixdrop, etc.) whose embedded iframes fail or are blocked.
 *
 * Layout features:
 * - Left side (Bam side): Browser Navigation (< Back, > Forward, Reload, Reset URL, Open in Chrome)
 * - Center: Full in-app Web view with HTML5 video fullscreen support
 * - Right side (Dan side): Vertical scrollable Episode Numbers list ("Epi", 1, 2, 3, 4, 5...)
 * - Top: BWSR status badge, domain indicator, loading progress, fullscreen & exit toggles
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

    // UI fullscreen mode (expanding the browser container)
    var isPlayerFullscreen by remember { mutableStateOf(false) }
    var areSidebarsVisible by remember { mutableStateOf(true) }

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

    // Fullscreen Immersive Mode & Orientation handling for isPlayerFullscreen
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
        // If HTML5 video entered full screen via WebChromeClient.onShowCustomView
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

            // Floating exit HTML5 custom view button
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
            // Main Web Player Area
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
                                // Intercept external intent schemes so spam ads don't force app switches
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

            // TOP STATUS & CONTROLS BAR
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
            ) {
                Surface(
                    color = Color.Black.copy(alpha = 0.82f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Left: BWSR badge + Domain info
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = QYellowPrimary,
                                shadowElevation = 2.dp
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Public,
                                        contentDescription = null,
                                        tint = QOnYellow,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = "BWSR",
                                        color = QOnYellow,
                                        style = SeriesTypography.micro,
                                        fontWeight = FontWeight.Black
                                    )
                                }
                            }

                            Column {
                                Text(
                                    text = if (episodeNumber != null) "EP $episodeNumber • $currentDomain" else currentDomain,
                                    color = Color.White,
                                    style = SeriesTypography.label,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "In-App Host Web Player",
                                    color = Color.White.copy(alpha = 0.6f),
                                    style = SeriesTypography.micro,
                                    fontSize = 10.sp
                                )
                            }
                        }

                        // Right: Sidebar toggle, Fullscreen toggle, and Exit button
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            // Toggle sidebars visibility (so user can view web page without obstruction)
                            IconButton(
                                onClick = { areSidebarsVisible = !areSidebarsVisible },
                                modifier = Modifier.size(30.dp)
                            ) {
                                Icon(
                                    imageVector = if (areSidebarsVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = "Toggle Controls",
                                    tint = if (areSidebarsVisible) QYellowPrimary else Color.White.copy(alpha = 0.7f),
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            // Fullscreen toggle
                            IconButton(
                                onClick = { isPlayerFullscreen = !isPlayerFullscreen },
                                modifier = Modifier.size(30.dp)
                            ) {
                                Icon(
                                    imageVector = if (isPlayerFullscreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                                    contentDescription = "Fullscreen",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            // Exit BWSR to return to standard native player
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = Color.White.copy(alpha = 0.15f),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.25f)),
                                modifier = Modifier.clickable { onExitBwsr() }
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Tv,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Player",
                                        color = Color.White,
                                        style = SeriesTypography.micro,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }

                // Page Loading Indicator
                if (isLoading) {
                    LinearProgressIndicator(
                        progress = { (loadingProgress / 100f).coerceIn(0.05f, 1f) },
                        modifier = Modifier.fillMaxWidth().height(2.5.dp),
                        color = QYellowPrimary,
                        trackColor = Color.Transparent
                    )
                }
            }

            // LEFT SIDEBAR (BAM SIDE): Browser Controls (< Back, > Forward, Reload, Reset URL, Open external)
            AnimatedVisibility(
                visible = areSidebarsVisible,
                enter = fadeIn(tween(150)) + slideInHorizontally(tween(150)) { -it },
                exit = fadeOut(tween(150)) + slideOutHorizontally(tween(150)) { -it },
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 6.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color.Black.copy(alpha = 0.85f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.18f)),
                    shadowElevation = 8.dp
                ) {
                    Column(
                        modifier = Modifier.padding(vertical = 6.dp, horizontal = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // < Back
                        IconButton(
                            onClick = {
                                if (webViewInstance?.canGoBack() == true) {
                                    webViewInstance?.goBack()
                                } else {
                                    webViewInstance?.loadUrl(rawUrl)
                                }
                            },
                            enabled = canGoBack || currentWebUrl != rawUrl,
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Browser Back",
                                tint = if (canGoBack || currentWebUrl != rawUrl) Color.White else Color.White.copy(alpha = 0.3f),
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        // > Forward
                        IconButton(
                            onClick = {
                                if (webViewInstance?.canGoForward() == true) {
                                    webViewInstance?.goForward()
                                }
                            },
                            enabled = canGoForward,
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = "Browser Forward",
                                tint = if (canGoForward) Color.White else Color.White.copy(alpha = 0.3f),
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        HorizontalDivider(
                            modifier = Modifier.width(20.dp),
                            color = Color.White.copy(alpha = 0.2f),
                            thickness = 1.dp
                        )

                        // Reload
                        IconButton(
                            onClick = { webViewInstance?.reload() },
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Reload",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        // Reset to Original Episode URL
                        IconButton(
                            onClick = { webViewInstance?.loadUrl(rawUrl) },
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Replay,
                                contentDescription = "Reset URL",
                                tint = QYellowPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        // Open in External Browser
                        IconButton(
                            onClick = {
                                try {
                                    val target = if (currentWebUrl.isNotBlank()) currentWebUrl else rawUrl
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(target))
                                    context.startActivity(intent)
                                } catch (_: Exception) {}
                            },
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.OpenInNew,
                                contentDescription = "Open in Chrome/Browser",
                                tint = Color.White.copy(alpha = 0.85f),
                                modifier = Modifier.size(17.dp)
                            )
                        }
                    }
                }
            }

            // RIGHT SIDEBAR (DAN SIDE): Episode Numbers Strip ("Epi", 1, 2, 3, 4, 5... with up/down scroll)
            AnimatedVisibility(
                visible = areSidebarsVisible && episodes.isNotEmpty(),
                enter = fadeIn(tween(150)) + slideInHorizontally(tween(150)) { it },
                exit = fadeOut(tween(150)) + slideOutHorizontally(tween(150)) { it },
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 6.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color.Black.copy(alpha = 0.88f),
                    border = BorderStroke(1.dp, QYellowPrimary.copy(alpha = 0.4f)),
                    shadowElevation = 8.dp,
                    modifier = Modifier.widthIn(min = 44.dp, max = 50.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .heightIn(max = if (isPlayerFullscreen) 280.dp else 190.dp)
                            .padding(vertical = 6.dp, horizontal = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // "Epi" Header
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

                        // Up/Down Scrollable Episode Numbers
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
                                    color = if (isSelected) QYellowPrimary else Color.White.copy(alpha = 0.14f),
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
