package com.example.ui.player

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.example.player.resolver.MediaKey
import com.example.player.resolver.MediaResolver
import com.example.player.resolver.ResolvedMedia
import com.example.ui.theme.QYellowPrimary
import com.example.util.MediaParser
import com.example.util.MediaType
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

@Composable
fun QVideoPlayer(
    url: String,
    modifier: Modifier = Modifier,
    thumbnailUrl: String? = null,
    autoPlay: Boolean = true,
    isMuted: Boolean = false,
    onMuteToggle: ((Boolean) -> Unit)? = null,
    showControls: Boolean = true,
    isLooping: Boolean = false,
    aspectRatio: Float? = null,
    onVideoEnded: (() -> Unit)? = null,
    onFullscreenToggle: (() -> Unit)? = null,
    isFullscreen: Boolean = false,
    customOverlay: (@Composable BoxScope.() -> Unit)? = null
) {
    val context = LocalContext.current
    var resolvedMedia by remember(url) { mutableStateOf<ResolvedMedia?>(null) }
    var resolveFailed by remember(url) { mutableStateOf(false) }
    var isLoadingMedia by remember(url) { mutableStateOf(true) }

    val isYouTube = remember(url) {
        val parsed = MediaParser.parseSingleUrl(url)
        parsed?.type == MediaType.YOUTUBE || url.contains("youtube.com") || url.contains("youtu.be")
    }

    LaunchedEffect(url) {
        if (url.isBlank()) {
            isLoadingMedia = false
            return@LaunchedEffect
        }
        if (isYouTube) {
            isLoadingMedia = false
            return@LaunchedEffect
        }
        isLoadingMedia = true
        resolveFailed = false
        try {
            val key = MediaKey(url = url, platform = if (url.contains("tiktok.com")) "tiktok" else "direct")
            val media = MediaResolver.resolve(key)
            resolvedMedia = media
            isLoadingMedia = false
        } catch (_: Exception) {
            resolveFailed = true
            isLoadingMedia = false
        }
    }

    val boxModifier = if (aspectRatio != null) {
        modifier.aspectRatio(aspectRatio)
    } else {
        modifier
    }

    Box(
        modifier = boxModifier
            .background(Color.Black)
            .clip(RoundedCornerShape(0.dp))
    ) {
        if (isYouTube) {
            YouTubeEmbedPlayer(
                url = url,
                autoPlay = autoPlay,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            val streamUrl = resolvedMedia?.url ?: url
            if (!resolveFailed && streamUrl.isNotBlank()) {
                DirectStreamPlayer(
                    streamUrl = streamUrl,
                    headers = resolvedMedia?.headers ?: emptyMap(),
                    autoPlay = autoPlay,
                    isMuted = isMuted,
                    isLooping = isLooping,
                    showControls = showControls,
                    thumbnailUrl = thumbnailUrl ?: resolvedMedia?.thumbnailUrl,
                    onVideoEnded = onVideoEnded,
                    onFullscreenToggle = onFullscreenToggle,
                    isFullscreen = isFullscreen,
                    modifier = Modifier.fillMaxSize()
                )
            } else if (resolveFailed) {
                // Fallback: Webview embed or retry
                FallbackEmbedPlayer(
                    url = url,
                    thumbnailUrl = thumbnailUrl,
                    modifier = Modifier.fillMaxSize(),
                    onRetry = {
                        isLoadingMedia = true
                        resolveFailed = false
                    }
                )
            }
        }

        if (isLoadingMedia) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = QYellowPrimary,
                    modifier = Modifier.size(40.dp),
                    strokeWidth = 3.dp
                )
            }
        }

        customOverlay?.invoke(this)
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun YouTubeEmbedPlayer(
    url: String,
    autoPlay: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var webViewRef by remember { mutableStateOf<WebView?>(null) }

    val videoId = remember(url) {
        MediaParser.parseSingleUrl(url)?.id ?: extractYouTubeId(url) ?: ""
    }

    DisposableEffect(lifecycleOwner, videoId) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE, Lifecycle.Event.ON_STOP -> {
                    webViewRef?.evaluateJavascript("if (player && player.pauseVideo) { player.pauseVideo(); }", null)
                    webViewRef?.onPause()
                    webViewRef?.pauseTimers()
                }
                Lifecycle.Event.ON_RESUME -> {
                    webViewRef?.onResume()
                    webViewRef?.resumeTimers()
                }
                Lifecycle.Event.ON_DESTROY -> {
                    webViewRef?.stopLoading()
                    webViewRef?.destroy()
                    webViewRef = null
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            webViewRef?.let { wv ->
                wv.evaluateJavascript("if (player && player.pauseVideo) { player.pauseVideo(); }", null)
                wv.stopLoading()
                wv.destroy()
            }
            webViewRef = null
        }
    }

    val htmlContent = remember(videoId, autoPlay) {
        """
        <!DOCTYPE html>
        <html>
        <head>
            <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
            <style>
                * { margin: 0; padding: 0; box-sizing: border-box; }
                body, html { width: 100%; height: 100%; background-color: #000; overflow: hidden; }
                #player { width: 100%; height: 100%; border: none; }
            </style>
        </head>
        <body>
            <div id="player"></div>
            <script>
                var tag = document.createElement('script');
                tag.src = "https://www.youtube.com/iframe_api";
                var firstScriptTag = document.getElementsByTagName('script')[0];
                firstScriptTag.parentNode.insertBefore(tag, firstScriptTag);

                var player;
                function onYouTubeIframeAPIReady() {
                    player = new YT.Player('player', {
                        videoId: '$videoId',
                        playerVars: {
                            'autoplay': ${if (autoPlay) 1 else 0},
                            'playsinline': 1,
                            'controls': 1,
                            'rel': 0,
                            'modestbranding': 1,
                            'fs': 1
                        },
                        events: {
                            'onReady': onPlayerReady
                        }
                    });
                }
                function onPlayerReady(event) {
                    if (${autoPlay}) {
                        event.target.playVideo();
                    }
                }
            </script>
        </body>
        </html>
        """.trimIndent()
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
                    cacheMode = WebSettings.LOAD_DEFAULT
                    allowContentAccess = true
                }
                webChromeClient = WebChromeClient()
                webViewClient = WebViewClient()
                loadDataWithBaseURL("https://www.youtube.com", htmlContent, "text/html", "UTF-8", null)
                webViewRef = this
            }
        },
        update = { wv ->
            webViewRef = wv
        },
        modifier = modifier
    )
}

@OptIn(UnstableApi::class)
@Composable
fun DirectStreamPlayer(
    streamUrl: String,
    headers: Map<String, String>,
    autoPlay: Boolean,
    isMuted: Boolean,
    isLooping: Boolean,
    showControls: Boolean,
    thumbnailUrl: String?,
    onVideoEnded: (() -> Unit)?,
    onFullscreenToggle: (() -> Unit)?,
    isFullscreen: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var isPlaying by remember { mutableStateOf(autoPlay) }
    var isBuffering by remember { mutableStateOf(true) }
    var currentPositionMs by remember { mutableLongStateOf(0L) }
    var durationMs by remember { mutableLongStateOf(0L) }
    var bufferedPositionMs by remember { mutableLongStateOf(0L) }
    var areControlsVisible by remember { mutableStateOf(false) }
    var playbackSpeed by remember { mutableFloatStateOf(1.0f) }
    var speedMenuOpen by remember { mutableStateOf(false) }

    val okHttpClient = remember {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .build()
    }

    val exoPlayer = remember(context, streamUrl) {
        val httpDataSourceFactory = if (headers.isNotEmpty()) {
            val okHttpFactory = OkHttpDataSource.Factory(okHttpClient)
            if (headers.isNotEmpty()) {
                okHttpFactory.setDefaultRequestProperties(headers)
            }
            okHttpFactory
        } else {
            DefaultHttpDataSource.Factory()
                .setUserAgent("Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36")
        }

        val mediaSourceFactory = DefaultMediaSourceFactory(httpDataSourceFactory)

        ExoPlayer.Builder(context)
            .setMediaSourceFactory(mediaSourceFactory)
            .build().apply {
                val mediaItem = MediaItem.Builder()
                    .setUri(Uri.parse(streamUrl))
                    .build()
                setMediaItem(mediaItem)
                repeatMode = if (isLooping) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
                volume = if (isMuted) 0f else 1f
                playWhenReady = autoPlay
                prepare()
            }
    }

    LaunchedEffect(isMuted) {
        exoPlayer.volume = if (isMuted) 0f else 1f
    }

    LaunchedEffect(autoPlay) {
        exoPlayer.playWhenReady = autoPlay
    }

    LaunchedEffect(isLooping) {
        exoPlayer.repeatMode = if (isLooping) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
    }

    // Monitor ExoPlayer events
    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                when (playbackState) {
                    Player.STATE_BUFFERING -> isBuffering = true
                    Player.STATE_READY -> {
                        isBuffering = false
                        durationMs = exoPlayer.duration.coerceAtLeast(0L)
                    }
                    Player.STATE_ENDED -> {
                        isBuffering = false
                        isPlaying = false
                        onVideoEnded?.invoke()
                    }
                    Player.STATE_IDLE -> {
                        isBuffering = false
                    }
                }
            }

            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }
        }
        exoPlayer.addListener(listener)

        onDispose {
            exoPlayer.removeListener(listener)
            exoPlayer.stop()
            exoPlayer.release()
        }
    }

    // Lifecycle observer to pause ExoPlayer when app or screen moves to background
    DisposableEffect(lifecycleOwner, exoPlayer) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE, Lifecycle.Event.ON_STOP -> {
                    exoPlayer.playWhenReady = false
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Polling progress ticker
    LaunchedEffect(exoPlayer) {
        while (isActive) {
            if (exoPlayer.isPlaying) {
                currentPositionMs = exoPlayer.currentPosition.coerceAtLeast(0L)
                bufferedPositionMs = exoPlayer.bufferedPosition.coerceAtLeast(0L)
                durationMs = exoPlayer.duration.coerceAtLeast(0L)
            }
            delay(500)
        }
    }

    // Auto-hide controls after 3.5s
    LaunchedEffect(areControlsVisible, isPlaying) {
        if (areControlsVisible && isPlaying) {
            delay(3500)
            areControlsVisible = false
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                if (showControls) {
                    areControlsVisible = !areControlsVisible
                }
            }
    ) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = false
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Poster / Thumbnail if buffering at start
        if (isBuffering && currentPositionMs == 0L && !thumbnailUrl.isNullOrBlank()) {
            AsyncImage(
                model = thumbnailUrl,
                contentDescription = "Video Poster",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }

        // Buffering Indicator
        if (isBuffering) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = QYellowPrimary,
                    modifier = Modifier.size(48.dp),
                    strokeWidth = 3.dp
                )
            }
        }

        // Custom M3 Controls Overlay
        if (showControls) {
            AnimatedVisibility(
                visible = areControlsVisible || !isPlaying,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.fillMaxSize()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.45f))
                ) {
                    // Center Big Play/Pause/Replay Button
                    IconButton(
                        onClick = {
                            if (exoPlayer.playbackState == Player.STATE_ENDED) {
                                exoPlayer.seekTo(0)
                                exoPlayer.playWhenReady = true
                            } else {
                                exoPlayer.playWhenReady = !isPlaying
                            }
                        },
                        modifier = Modifier
                            .size(64.dp)
                            .align(Alignment.Center)
                            .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                    ) {
                        val icon = when {
                            exoPlayer.playbackState == Player.STATE_ENDED -> Icons.Default.Replay
                            isPlaying -> Icons.Default.Pause
                            else -> Icons.Default.PlayArrow
                        }
                        Icon(
                            imageVector = icon,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint = Color.White,
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    // Top Bar: Speed & Fullscreen Quick Toggles
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.TopCenter)
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Playback Speed dropdown
                        Box {
                            TextButton(
                                onClick = { speedMenuOpen = true },
                                colors = ButtonDefaults.textButtonColors(contentColor = Color.White)
                            ) {
                                Text(
                                    text = "${playbackSpeed}x",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                            DropdownMenu(
                                expanded = speedMenuOpen,
                                onDismissRequest = { speedMenuOpen = false }
                            ) {
                                listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f).forEach { speed ->
                                    DropdownMenuItem(
                                        text = { Text("${speed}x") },
                                        onClick = {
                                            playbackSpeed = speed
                                            exoPlayer.playbackParameters = PlaybackParameters(speed)
                                            speedMenuOpen = false
                                        }
                                    )
                                }
                            }
                        }

                        if (onFullscreenToggle != null) {
                            IconButton(onClick = onFullscreenToggle) {
                                Icon(
                                    imageVector = if (isFullscreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                                    contentDescription = "Fullscreen",
                                    tint = Color.White
                                )
                            }
                        }
                    }

                    // Bottom Bar: Progress Slider + Time
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter)
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = formatTime(currentPositionMs),
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )

                            Text(
                                text = formatTime(durationMs),
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 12.sp
                            )
                        }

                        val progress = if (durationMs > 0) {
                            (currentPositionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
                        } else 0f

                        Slider(
                            value = progress,
                            onValueChange = { newProgress ->
                                val target = (newProgress * durationMs).toLong()
                                currentPositionMs = target
                                exoPlayer.seekTo(target)
                            },
                            colors = SliderDefaults.colors(
                                thumbColor = QYellowPrimary,
                                activeTrackColor = QYellowPrimary,
                                inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FallbackEmbedPlayer(
    url: String,
    thumbnailUrl: String?,
    modifier: Modifier = Modifier,
    onRetry: () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF1E1E24)),
        contentAlignment = Alignment.Center
    ) {
        if (!thumbnailUrl.isNullOrBlank()) {
            AsyncImage(
                model = thumbnailUrl,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.65f))
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.PlayCircleOutline,
                contentDescription = "Media Playback",
                tint = QYellowPrimary,
                modifier = Modifier.size(52.dp)
            )

            Text(
                text = "Video Stream Ready",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )

            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(containerColor = QYellowPrimary, contentColor = Color.Black)
            ) {
                Text("Play Stream")
            }
        }
    }
}

private fun formatTime(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d", minutes, seconds)
}

private fun extractYouTubeId(url: String): String? {
    val patterns = listOf(
        "(?:youtu\\.be/|youtube\\.com/(?:embed/|v/|watch\\?v=|watch\\?.+&v=))([\\w-]{11})",
        "youtube\\.com/shorts/([\\w-]{11})"
    )
    for (p in patterns) {
        val regex = Regex(p)
        val match = regex.find(url)
        if (match != null && match.groupValues.size > 1) {
            return match.groupValues[1]
        }
    }
    return null
}
