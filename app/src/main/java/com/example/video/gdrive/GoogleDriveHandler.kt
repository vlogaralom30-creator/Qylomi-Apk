package com.example.video.gdrive

import android.content.Context
import android.content.Intent
import android.net.Uri
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.ui.components.EmbedVideoPlayer
import com.example.ui.components.NativeVideoPlayer
import com.example.ui.theme.QOnYellow
import com.example.ui.theme.QSurfaceVariant
import com.example.ui.theme.QYellowPrimary
import com.example.util.VideoPlayerCache
import com.example.video.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

class GoogleDriveHandler : VideoPlatformHandler {
    override val platformId: String = "gdrive"
    override val displayName: String = "Google Drive"

    companion object {
        private const val METADATA_CACHE_TTL = 30 * 60 * 1000L // 30 minutes
        private val metadataCache = ConcurrentHashMap<String, Pair<ExtractedVideo, Long>>()

        private val FILE_ID_PATTERN = Pattern.compile("[A-Za-z0-9_-]{20,}")

        private val okHttpClient: OkHttpClient by lazy {
            VideoPlayerCache.okHttpClient.newBuilder()
                .connectTimeout(8, TimeUnit.SECONDS)
                .readTimeout(8, TimeUnit.SECONDS)
                .callTimeout(8, TimeUnit.SECONDS)
                .build()
        }

        private fun getApiKey(): String? {
            return try {
                val field = com.example.BuildConfig::class.java.getField("GOOGLE_DRIVE_API_KEY")
                field.get(null) as? String
            } catch (_: Exception) {
                null
            }
        }

        private fun getPackageName(): String {
            return com.example.BuildConfig.APPLICATION_ID
        }

        private fun getCertSha1(): String? {
             return try {
                val field = com.example.BuildConfig::class.java.getField("GOOGLE_DRIVE_SHA1")
                field.get(null) as? String
            } catch (_: Exception) {
                null
            }
        }
    }

    override fun matches(url: String): Boolean {
        val lower = url.lowercase().trim()
        return lower.contains("drive.google.com") || 
               lower.contains("docs.google.com") || 
               lower.contains("drive.usercontent.google.com")
    }

    override fun parse(url: String): ParsedVideo? {
        val trimmed = url.trim()
        val lower = trimmed.lowercase()
        
        // Block folders and docs explicitly
        if (lower.contains("/drive/folders/") || lower.contains("/drive/u/0/folders/")) {
            // We'll let extract handle the error message by passing a dummy ID or a flag
            return ParsedVideo(platformId, trimmed, "ERROR_FOLDER")
        }
        if (lower.contains("/document/d/") || lower.contains("/spreadsheets/d/") || lower.contains("/presentation/d/")) {
            return ParsedVideo(platformId, trimmed, "ERROR_DOC")
        }

        // Try to find the file ID with the regex
        val matcher = FILE_ID_PATTERN.matcher(trimmed)
        if (!matcher.find()) return null
        val fileId = matcher.group()

        // Extract resourcekey if present
        val resourceKey = trimmed.substringAfter("resourcekey=", "").substringBefore("&").substringBefore(" ").takeIf { it.isNotBlank() }

        return ParsedVideo(
            platform = platformId,
            originalUrl = trimmed,
            videoId = if (resourceKey != null) "$fileId|$resourceKey" else fileId
        )
    }

    override suspend fun extract(parsed: ParsedVideo): Result<ExtractedVideo> = withContext(Dispatchers.IO) {
        val idToken = parsed.videoId ?: return@withContext Result.failure(Exception("Missing file ID"))
        
        if (idToken == "ERROR_FOLDER") {
            return@withContext Result.failure(Exception("NOT_A_VIDEO|Please paste a link to a video file, not a folder."))
        }
        if (idToken == "ERROR_DOC") {
            return@withContext Result.failure(Exception("NOT_A_VIDEO|Please paste a link to a video file, not a Google Doc/Sheet/Slide."))
        }

        val fileId = idToken.substringBefore("|")
        val resourceKey = idToken.substringAfter("|", "").takeIf { it.isNotBlank() }

        // Check cache
        metadataCache[idToken]?.let { (video, timestamp) ->
            if (System.currentTimeMillis() - timestamp < METADATA_CACHE_TTL) {
                return@withContext Result.success(video)
            }
        }

        val apiKey = getApiKey()
        if (apiKey.isNullOrBlank()) {
            return@withContext Result.success(createMinimalFallback(parsed, fileId, resourceKey))
        }

        val url = "https://www.googleapis.com/drive/v3/files/$fileId?fields=name,mimeType,size,videoMediaMetadata(width,height,durationMillis)&supportsAllDrives=true&key=$apiKey"
        val requestBuilder = Request.Builder().url(url).get()
        
        // Add resourcekey header
        if (resourceKey != null) {
            requestBuilder.addHeader("X-Goog-Drive-Resource-Keys", "$fileId/$resourceKey")
        }

        // Add Android restriction headers if SHA1 exists
        val cert = getCertSha1()
        if (cert != null) {
            requestBuilder.addHeader("X-Android-Package", getPackageName())
            requestBuilder.addHeader("X-Android-Cert", cert.uppercase().replace(":", ""))
        }

        try {
            val response = okHttpClient.newCall(requestBuilder.build()).execute()
            val body = response.body?.string()
            val code = response.code
            response.close()

            if (code == 200 && body != null) {
                val json = JSONObject(body)
                val mimeType = json.optString("mimeType", "")
                val name = json.optString("name", "Drive Video")
                
                if (!mimeType.startsWith("video/") && mimeType != "application/octet-stream") {
                    return@withContext Result.failure(Exception("NOT_A_VIDEO|Please paste a link to a video file, not a document or folder."))
                }

                val videoMeta = json.optJSONObject("videoMediaMetadata")
                val width = videoMeta?.optInt("width", 0) ?: 0
                val height = videoMeta?.optInt("height", 0) ?: 0
                val durationMs = videoMeta?.optLong("durationMillis", 0) ?: 0
                val size = json.optLong("size", 0).takeIf { it > 0 }
                
                val isVertical = height > width && height > 0
                
                val extracted = ExtractedVideo(
                    platform = platformId,
                    videoId = idToken,
                    originalUrl = parsed.originalUrl,
                    embedUrl = "https://drive.google.com/file/d/$fileId/preview",
                    thumbnailUrl = "https://drive.google.com/thumbnail?id=$fileId&sz=w640",
                    title = name.substringBeforeLast("."),
                    isVertical = isVertical,
                    durationSec = (durationMs / 1000).toInt().takeIf { it > 0 },
                    isNativePlayable = true,
                    requestHeaders = buildHeaders(fileId, resourceKey),
                    fileSize = size,
                    mimeType = mimeType
                )
                
                metadataCache[idToken] = extracted to System.currentTimeMillis()
                Result.success(extracted)
            } else if (code == 404 || code == 403) {
                Result.failure(Exception("NOT_PUBLIC|Set sharing to 'Anyone with the link' (Viewer) in Google Drive, then try again."))
            } else {
                Result.success(createMinimalFallback(parsed, fileId, resourceKey))
            }
        } catch (e: Exception) {
            Result.success(createMinimalFallback(parsed, fileId, resourceKey))
        }
    }

    private fun createMinimalFallback(parsed: ParsedVideo, fileId: String, resourceKey: String?): ExtractedVideo {
        return ExtractedVideo(
            platform = platformId,
            videoId = parsed.videoId,
            originalUrl = parsed.originalUrl,
            embedUrl = "https://drive.google.com/file/d/$fileId/preview",
            thumbnailUrl = "https://drive.google.com/thumbnail?id=$fileId&sz=w640",
            title = "Google Drive Video",
            isNativePlayable = false, // Fallback to embed
            requestHeaders = buildHeaders(fileId, resourceKey)
        )
    }

    private fun buildHeaders(fileId: String, resourceKey: String?): Map<String, String>? {
        val headers = mutableMapOf<String, String>()
        if (resourceKey != null) {
            headers["X-Goog-Drive-Resource-Keys"] = "$fileId/$resourceKey"
        }
        val cert = getCertSha1()
        if (cert != null) {
            headers["X-Android-Package"] = getPackageName()
            headers["X-Android-Cert"] = cert.uppercase().replace(":", "")
        }
        return headers.takeIf { it.isNotEmpty() }
    }

    @Composable
    override fun Player(
        video: ExtractedVideo,
        modifier: Modifier,
        config: PlayerConfig,
        controller: PlayerController?
    ) {
        val context = LocalContext.current
        var playStage by remember { mutableStateOf("A") } // A: Native API, B: Download, C: Embed, D: Error
        var isPlayerActive by remember { mutableStateOf(config.autoPlay) }
        val apiKey = getApiKey()
        val idToken = video.videoId ?: ""
        val fileId = idToken.substringBefore("|")
        val resourceKey = idToken.substringAfter("|", "").takeIf { it.isNotBlank() }

        val stageAUrl = "https://www.googleapis.com/drive/v3/files/$fileId?alt=media&key=$apiKey"
        val stageBUrl = buildString {
            append("https://drive.usercontent.google.com/download?id=$fileId&export=download&confirm=t")
            if (resourceKey != null) {
                append("&resourcekey=$resourceKey")
            }
        }
        
        val embedUrl = buildString {
            append("https://drive.google.com/file/d/$fileId/preview")
            if (resourceKey != null) {
                append("?resourcekey=$resourceKey")
            }
        }

        var isProbing by remember { mutableStateOf(false) }

        LaunchedEffect(playStage, isPlayerActive) {
            if (isPlayerActive && (playStage == "A" || playStage == "B")) {
                isProbing = true
                val url = if (playStage == "A") stageAUrl else stageBUrl
                val ok = probePlayback(url, video.requestHeaders)
                isProbing = false
                if (!ok) {
                    playStage = if (playStage == "A") "B" else "C"
                }
            }
        }

        Box(modifier = modifier.fillMaxWidth().aspectRatio(if (video.isVertical) 9f/16f else 16f/9f)) {
            if (!isPlayerActive) {
                // ... (existing thumbnail code) ...
                Box(modifier = Modifier.fillMaxSize().clickable { isPlayerActive = true }) {
                    AsyncImage(
                        model = video.thumbnailUrl,
                        contentDescription = video.title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.3f)))
                    Box(
                        modifier = Modifier.size(60.dp).clip(RoundedCornerShape(30.dp)).background(QYellowPrimary).align(Alignment.Center),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = "Play", tint = QOnYellow, modifier = Modifier.size(36.dp))
                    }
                }
            } else if (isProbing) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = QYellowPrimary)
                }
            } else {
                when (playStage) {
                    "A" -> {
                        if (apiKey.isNullOrBlank()) {
                            // Already handled by LaunchedEffect transition, but for safety:
                            playStage = "B"
                        } else {
                            NativeVideoPlayer(
                                videoUrl = stageAUrl,
                                requestHeaders = video.requestHeaders,
                                autoPlay = true,
                                modifier = Modifier.fillMaxSize(),
                                onError = { playStage = "B" }
                            )
                        }
                    }
                    "B" -> {
                        NativeVideoPlayer(
                            videoUrl = stageBUrl,
                            autoPlay = true,
                            modifier = Modifier.fillMaxSize(),
                            onError = { playStage = "C" }
                        )
                    }
                    "C" -> {
                        if (config.nativeOnly) {
                            DriveErrorCard(video, onRetry = { playStage = "A" })
                        } else {
                            DriveWebViewEmbed(embedUrl)
                        }
                    }
                    "D" -> {
                        DriveErrorCard(video, onRetry = { playStage = "A" })
                    }
                }
            }
        }
    }

    @Composable
    private fun DriveWebViewEmbed(url: String) {
        val context = LocalContext.current
        androidx.compose.ui.viewinterop.AndroidView(
            factory = { ctx ->
                android.webkit.WebView(ctx).apply {
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.mediaPlaybackRequiresUserGesture = false
                    
                    webViewClient = object : android.webkit.WebViewClient() {
                        override fun onRenderProcessGone(
                            view: android.webkit.WebView?,
                            detail: android.webkit.RenderProcessGoneDetail?
                        ): Boolean {
                            try { view?.destroy() } catch (_: Exception) {}
                            return true
                        }

                        override fun shouldOverrideUrlLoading(
                            view: android.webkit.WebView?,
                            request: android.webkit.WebResourceRequest?
                        ): Boolean {
                            val targetUrl = request?.url?.toString() ?: return false
                            
                            // Only allow drive.google.com and accounts.google.com
                            val host = request.url.host ?: ""
                            if (host.contains("drive.google.com") || host.contains("accounts.google.com") || host.contains("google.com")) {
                                // If it's a "view" or "popout" link, open externally
                                if (targetUrl.contains("/view") || targetUrl.contains("proxy") || !targetUrl.contains("preview")) {
                                    try {
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(targetUrl))
                                        context.startActivity(intent)
                                    } catch (_: Exception) {}
                                    return true
                                }
                                return false
                            }
                            
                            // Block everything else, open in browser
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(targetUrl))
                                context.startActivity(intent)
                            } catch (_: Exception) {}
                            return true
                        }
                    }
                    loadUrl(url)
                }
            },
            modifier = Modifier.fillMaxSize()
        )
    }

    @Composable
    private fun DriveErrorCard(video: ExtractedVideo, onRetry: () -> Unit) {
        val context = LocalContext.current
        Card(
            modifier = Modifier.fillMaxSize(),
            colors = CardDefaults.cardColors(containerColor = QSurfaceVariant)
        ) {
            Column(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Unable to play video", fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = onRetry) { Text("Retry") }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(onClick = {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(video.originalUrl))
                    context.startActivity(intent)
                }) {
                    Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Open in Drive")
                }
            }
        }
    }

    private suspend fun probePlayback(url: String, headers: Map<String, String>?): Boolean = withContext(Dispatchers.IO) {
        try {
            val requestBuilder = Request.Builder().url(url).header("Range", "bytes=0-1").get()
            headers?.forEach { (k, v) -> requestBuilder.addHeader(k, v) }
            
            val response = okHttpClient.newCall(requestBuilder.build()).execute()
            val contentType = response.header("Content-Type")?.lowercase() ?: ""
            val success = response.isSuccessful && (contentType.startsWith("video/") || contentType == "application/octet-stream")
            response.close()
            success
        } catch (_: Exception) {
            false
        }
    }
}
