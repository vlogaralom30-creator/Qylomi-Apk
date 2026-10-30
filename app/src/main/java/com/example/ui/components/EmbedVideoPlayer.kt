package com.example.ui.components

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.view.ViewGroup
import android.webkit.WebSettings
import android.webkit.WebView
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.adshield.ui.AdShieldInspectorDialog
import com.example.adshield.ui.AdShieldWebChromeClient
import com.example.adshield.ui.AdShieldWebViewClient
import com.example.ui.theme.QCardBorder
import com.example.ui.theme.QOnSurface
import com.example.ui.theme.QOnSurfaceVariant
import com.example.ui.theme.QOnYellow
import com.example.ui.theme.QSurface
import com.example.ui.theme.QYellowDark
import com.example.ui.theme.QYellowPrimary
import com.example.util.VideoEmbedUtils
import kotlinx.coroutines.delay

@OptIn(ExperimentalFoundationApi::class)
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun EmbedVideoPlayer(
    videoUrl: String,
    modifier: Modifier = Modifier,
    title: String? = null,
    standalone: Boolean = false
) {
    val context = LocalContext.current
    val detectedInfo = remember(videoUrl) { VideoEmbedUtils.parseVideoUrl(videoUrl) }
    var hasError by remember { mutableStateOf(false) }
    var reloadTrigger by remember { mutableIntStateOf(0) }
    var isStalled by remember { mutableStateOf(false) }
    var isPageLoaded by remember(detectedInfo.embedUrl, reloadTrigger) { mutableStateOf(false) }
    var showInspector by remember { mutableStateOf(false) }

    // 15-second stall detection timer (triggered ONLY if the page fails to load within 15 seconds)
    LaunchedEffect(detectedInfo.embedUrl, reloadTrigger, isPageLoaded) {
        if (!isPageLoaded && detectedInfo.embedUrl.isNotBlank() && detectedInfo.platform != "youtube") {
            delay(15_000)
            if (!isPageLoaded) {
                isStalled = true
            }
        }
    }

    val platformColor = when (detectedInfo.platform) {
        "youtube" -> Color(0xFFFF0000)
        "streamtape" -> Color(0xFF00ADB5)
        "doodstream" -> Color(0xFFFF9800)
        "mixdrop" -> Color(0xFF9C27B0)
        "tiktok" -> Color(0xFF00F2FE)
        "facebook" -> Color(0xFF1877F2)
        "instagram" -> Color(0xFFE4405F)
        else -> QYellowDark
    }

    val aspectRatio = when (detectedInfo.platform) {
        "tiktok", "instagram" -> 9f / 14f
        else -> 16f / 9f
    }

    if (showInspector) {
        AdShieldInspectorDialog(onDismissRequest = { showInspector = false })
    }

    if (standalone) {
        // Full-bleed standalone mode (for EpisodePlayerView / Series stage)
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            EmbedPlayerCanvasContent(
                detectedInfo = detectedInfo,
                videoUrl = videoUrl,
                title = title,
                hasError = hasError,
                isStalled = isStalled,
                reloadTrigger = reloadTrigger,
                onPageLoaded = { isPageLoaded = true },
                onError = { hasError = true },
                onReload = {
                    hasError = false
                    isStalled = false
                    isPageLoaded = false
                    reloadTrigger++
                },
                modifier = Modifier.fillMaxSize()
            )

            // Subtle floating Ad Shield badge & reload action on top right
            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color.Black.copy(alpha = 0.65f),
                    border = androidx.compose.foundation.BorderStroke(0.5.dp, platformColor.copy(alpha = 0.8f)),
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .combinedClickable(
                            onClick = {},
                            onLongClick = { showInspector = true }
                        )
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = "Ad Shield Protected",
                            tint = platformColor,
                            modifier = Modifier.size(11.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${detectedInfo.platform.uppercase()} • SHIELD",
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                IconButton(
                    onClick = {
                        hasError = false
                        isStalled = false
                        reloadTrigger++
                    },
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.65f))
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Reload embed player",
                        tint = Color.White,
                        modifier = Modifier.size(13.dp)
                    )
                }
            }
        }
    } else {
        // Card mode with header bar (for Feed post items)
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = QSurface),
            border = androidx.compose.foundation.BorderStroke(1.dp, QCardBorder),
            modifier = modifier
                .fillMaxWidth()
                .testTag("embed_video_card")
        ) {
            Column {
                // Header Bar
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Black.copy(alpha = 0.4f))
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    // Platform Badge with secret long-press to launch Ad Shield Inspector
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(platformColor)
                            .combinedClickable(
                                onClick = {},
                                onLongClick = { showInspector = true }
                            )
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = "Shielded",
                            tint = Color.White,
                            modifier = Modifier.size(11.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = detectedInfo.platformDisplayName.uppercase(),
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = title ?: "Video Media",
                        color = QOnSurface,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        modifier = Modifier.weight(1f)
                    )

                    // Open in External App / Browser
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable {
                                try {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(detectedInfo.originalUrl))
                                    context.startActivity(intent)
                                } catch (_: Exception) {}
                            }
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "Open",
                            fontSize = 11.sp,
                            color = QOnSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Icon(
                            imageVector = Icons.Default.OpenInNew,
                            contentDescription = "Open video in app",
                            tint = QOnSurfaceVariant,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }

                // Embedded Video Player Canvas
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(aspectRatio)
                        .background(Color.Black),
                    contentAlignment = Alignment.Center
                ) {
                    EmbedPlayerCanvasContent(
                        detectedInfo = detectedInfo,
                        videoUrl = videoUrl,
                        title = title,
                        hasError = hasError,
                        isStalled = isStalled,
                        reloadTrigger = reloadTrigger,
                        onPageLoaded = { isPageLoaded = true },
                        onError = { hasError = true },
                        onReload = {
                            hasError = false
                            isStalled = false
                            isPageLoaded = false
                            reloadTrigger++
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun EmbedPlayerCanvasContent(
    detectedInfo: com.example.util.DetectedVideoInfo,
    videoUrl: String,
    title: String?,
    hasError: Boolean,
    isStalled: Boolean,
    reloadTrigger: Int,
    onPageLoaded: () -> Unit = {},
    onError: () -> Unit,
    onReload: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var webViewRef by remember { mutableStateOf<WebView?>(null) }

    androidx.compose.runtime.DisposableEffect(reloadTrigger) {
        onDispose {
            webViewRef?.let { wv ->
                try {
                    wv.onPause()
                    wv.pauseTimers()
                    wv.stopLoading()
                    wv.loadUrl("about:blank")
                } catch (_: Exception) {}
            }
        }
    }

    if (detectedInfo.platform == "youtube") {
        val ytHandler = com.example.video.VideoPlatformRegistry.resolve(videoUrl)
        val parsed = ytHandler.parse(videoUrl)
        val extracted = com.example.video.ExtractedVideo(
            platform = "youtube",
            videoId = parsed?.videoId,
            originalUrl = videoUrl,
            embedUrl = detectedInfo.embedUrl,
            title = title,
            isVertical = parsed?.isShort == true,
            isShort = parsed?.isShort == true
        )
        ytHandler.Player(
            video = extracted,
            modifier = modifier.fillMaxSize(),
            config = com.example.video.PlayerConfig(autoPlay = false, showControls = true),
            controller = null
        )
    } else if (detectedInfo.embedUrl.isNotBlank() && !hasError && !isStalled) {
        androidx.compose.runtime.key(reloadTrigger) {
            AndroidView<WebView>(
                factory = { ctx: android.content.Context ->
                    WebView(ctx).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.mediaPlaybackRequiresUserGesture = false
                        settings.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                        settings.setSupportMultipleWindows(false)
                        settings.userAgentString = "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"

                        webChromeClient = AdShieldWebChromeClient()
                        webViewClient = AdShieldWebViewClient(
                            activeProfileId = detectedInfo.platform,
                            onPageFinishedCallback = onPageLoaded,
                            onErrorOccurred = onError
                        )

                        val htmlContent = """
                            <!DOCTYPE html>
                            <html>
                            <head>
                                <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
                                <style>
                                    * { box-sizing: border-box; }
                                    body, html {
                                        margin: 0;
                                        padding: 0;
                                        width: 100%;
                                        height: 100%;
                                        background-color: black;
                                        overflow: hidden;
                                        display: flex;
                                        justify-content: center;
                                        align-items: center;
                                    }
                                    iframe, video {
                                        width: 100%;
                                        height: 100%;
                                        border: none;
                                        overflow: hidden;
                                        object-fit: contain;
                                    }
                                </style>
                            </head>
                            <body>
                                <iframe src="${detectedInfo.embedUrl}" width="100%" height="100%" allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture" allowfullscreen allowtransparency scrolling="no" frameborder="0"></iframe>
                            </body>
                            </html>
                        """.trimIndent()

                        val baseUrl = when (detectedInfo.platform) {
                            "streamtape" -> "https://streamtape.com"
                            "doodstream" -> "https://dood.to"
                            "mixdrop" -> "https://mixdrop.co"
                            "tiktok" -> "https://www.tiktok.com"
                            "facebook" -> "https://www.facebook.com"
                            "instagram" -> "https://www.instagram.com"
                            else -> {
                                val host = com.example.adshield.engine.AdShieldEngine.extractHost(detectedInfo.embedUrl)
                                if (!host.isNullOrBlank()) "https://$host" else "https://streamtape.com"
                            }
                        }

                        loadDataWithBaseURL(baseUrl, htmlContent, "text/html", "utf-8", null)
                    }
                },
                update = { webView: WebView ->
                    webViewRef = webView
                },
                onRelease = { webView: WebView ->
                    try {
                        webView.onPause()
                        webView.pauseTimers()
                        webView.stopLoading()
                        webView.clearHistory()
                        webView.loadUrl("about:blank")
                        (webView.parent as? ViewGroup)?.removeView(webView)
                        webView.destroy()
                    } catch (_: Exception) {}
                },
                modifier = modifier.fillMaxSize()
            )
        }
    } else if (isStalled) {
        // 15s Stall Failover UI (Clean Retry option without relaxing ad blocking)
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = modifier.padding(16.dp)
        ) {
            Text(
                text = "Playback Stalled or Delayed",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Ad Shield is keeping this embed ad-free.",
                color = Color.Gray,
                fontSize = 11.sp
            )
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = onReload,
                colors = ButtonDefaults.buttonColors(
                    containerColor = QYellowPrimary,
                    contentColor = QOnYellow
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Reload Embed",
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Reload Embed", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    } else {
        // Fallback Click-to-Play Card
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = modifier
                .padding(16.dp)
                .clickable {
                    try {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(detectedInfo.originalUrl))
                        context.startActivity(intent)
                    } catch (_: Exception) {}
                }
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(54.dp)
                    .clip(RoundedCornerShape(27.dp))
                    .background(QYellowPrimary)
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Play Video",
                    tint = QOnYellow,
                    modifier = Modifier.size(32.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Tap to Play ${detectedInfo.platformDisplayName}",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            )
            Text(
                text = detectedInfo.originalUrl,
                color = Color.Gray,
                fontSize = 11.sp,
                maxLines = 1
            )
        }
    }
}

