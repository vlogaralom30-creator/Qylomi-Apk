package com.example.video.doodstream

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.view.ViewGroup
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.ui.theme.QOnYellow
import com.example.ui.theme.QSurfaceVariant
import com.example.ui.theme.QYellowPrimary
import com.example.video.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.regex.Pattern

/**
 * Platform handler for DoodStream videos (dood.to, dood.la, dood.watch, dood.so, doodstream.com).
 */
class DoodStreamHandler : VideoPlatformHandler {
    override val platformId: String = "doodstream"
    override val displayName: String = "DoodStream"

    companion object {
        private val DOOD_ID_REGEX = Pattern.compile(
            "https?://(?:www\\.)?(?:dood\\.(?:to|la|watch|so|pe|com|cc)|doodstream\\.com|doodcdn\\.io|doodapi\\.co)/(?:v|e|d)/([a-zA-Z0-9_-]+)",
            Pattern.CASE_INSENSITIVE
        )

        fun extractId(url: String): String? {
            val matcher = DOOD_ID_REGEX.matcher(url.trim())
            if (matcher.find()) {
                return matcher.group(1)
            }
            // Fallback for simple file codes or custom paths
            val trimmed = url.trim()
            if (trimmed.length in 8..15 && !trimmed.contains(" ") && !trimmed.contains("/")) {
                return trimmed
            }
            return null
        }

        fun toEmbedUrl(url: String): String {
            val id = extractId(url)
            return if (id != null) "https://dood.to/e/$id/" else url.trim()
        }

        fun buildIframeHtml(embedUrl: String): String {
            return """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
                    <style>
                        * { box-sizing: border-box; }
                        html, body {
                            margin: 0;
                            padding: 0;
                            width: 100%;
                            height: 100%;
                            background-color: #000000;
                            overflow: hidden;
                            display: flex;
                            align-items: center;
                            justify-content: center;
                        }
                        iframe {
                            width: 100%;
                            height: 100%;
                            border: 0;
                            overflow: hidden;
                        }
                    </style>
                </head>
                <body>
                    <iframe src="$embedUrl" width="100%" height="100%" allowfullscreen allowtransparency allow="autoplay" scrolling="no" frameborder="0"></iframe>
                </body>
                </html>
            """.trimIndent()
        }
    }

    override fun matches(url: String): Boolean {
        val lower = url.lowercase().trim()
        return lower.contains("dood.") ||
                lower.contains("doodstream") ||
                lower.contains("doodcdn") ||
                lower.contains("doodapi")
    }

    override fun parse(url: String): ParsedVideo? {
        val trimmed = url.trim()
        if (!matches(trimmed)) return null
        val videoId = extractId(trimmed)
        return ParsedVideo(
            platform = platformId,
            originalUrl = trimmed,
            videoId = videoId
        )
    }

    override suspend fun extract(parsed: ParsedVideo): Result<ExtractedVideo> = withContext(Dispatchers.IO) {
        val id = parsed.videoId ?: return@withContext Result.failure(IllegalArgumentException("Invalid DoodStream URL"))
        val embed = "https://dood.to/e/$id/"
        Result.success(
            ExtractedVideo(
                platform = platformId,
                videoId = id,
                originalUrl = parsed.originalUrl,
                embedUrl = embed,
                title = "DoodStream Video",
                isNativePlayable = false,
                isVertical = false
            )
        )
    }

    @Composable
    override fun Player(
        video: ExtractedVideo,
        modifier: Modifier,
        config: PlayerConfig,
        controller: PlayerController?
    ) {
        DoodStreamPlayer(
            embedUrl = video.embedUrl,
            originalUrl = video.originalUrl,
            autoPlay = config.autoPlay,
            modifier = modifier
        )
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun DoodStreamPlayer(
    embedUrl: String,
    originalUrl: String,
    modifier: Modifier = Modifier,
    autoPlay: Boolean = true
) {
    val context = LocalContext.current
    var isStarted by remember { mutableStateOf(autoPlay) }
    var hasError by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(16f / 9f)
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        if (!isStarted) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable { isStarted = true },
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .clip(RoundedCornerShape(30.dp))
                            .background(QYellowPrimary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Play DoodStream",
                            tint = QOnYellow,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                    Text(
                        text = "Play DoodStream Video",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
        } else if (hasError) {
            Card(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(containerColor = QSurfaceVariant)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Failed to load DoodStream embed", fontWeight = FontWeight.Bold, color = Color.White)
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = { hasError = false }) {
                        Text("Retry")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(onClick = {
                        try {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(originalUrl))
                            context.startActivity(intent)
                        } catch (_: Exception) {}
                    }) {
                        Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Open in Browser")
                    }
                }
            }
        } else {
            val htmlData = remember(embedUrl) {
                DoodStreamHandler.buildIframeHtml(embedUrl)
            }

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
                            mediaPlaybackRequiresUserGesture = false
                            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                            userAgentString = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
                            allowFileAccess = true
                        }
                        webChromeClient = com.example.adshield.ui.AdShieldWebChromeClient()
                        webViewClient = object : com.example.adshield.ui.AdShieldWebViewClient(activeProfileId = "doodstream") {
                            override fun onRenderProcessGone(view: WebView?, detail: RenderProcessGoneDetail?): Boolean {
                                hasError = true
                                try {
                                    view?.destroy()
                                } catch (_: Exception) {}
                                return true
                            }
                        }
                        loadDataWithBaseURL("https://dood.to", htmlData, "text/html", "UTF-8", null)
                    }
                },
                update = { webView ->
                    // Keep view active
                },
                onRelease = { webView ->
                    try {
                        webView.stopLoading()
                        webView.loadUrl("about:blank")
                        (webView.parent as? ViewGroup)?.removeView(webView)
                        webView.destroy()
                    } catch (_: Exception) {}
                },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
