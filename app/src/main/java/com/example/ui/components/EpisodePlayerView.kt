package com.example.ui.components

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.media.AudioManager
import android.net.Uri
import android.view.ViewGroup
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.example.ui.components.series.*
import com.example.ui.theme.*
import kotlinx.coroutines.delay

fun resolveStreamUrl(rawUrl: String): String {
    val trimmed = rawUrl.trim()
    if (trimmed.isEmpty()) return ""

    return when {
        trimmed.startsWith("http://") || trimmed.startsWith("https://") -> {
            if ((trimmed.contains("cloudflarestream.com") || trimmed.contains("videodelivery.net")) && !trimmed.endsWith(".m3u8")) {
                val cleanUrl = trimmed.trimEnd('/')
                "$cleanUrl/manifest/video.m3u8"
            } else {
                trimmed
            }
        }
        trimmed.matches(Regex("^[a-zA-Z0-9]{32,}$")) -> {
            "https://customer-cloudflarestream.com/$trimmed/manifest/video.m3u8"
        }
        else -> trimmed
    }
}

fun formatTime(seconds: Int): String {
    val mins = (seconds % 3600) / 60
    val secs = seconds % 60
    val hrs = seconds / 3600
    return if (hrs > 0) {
        String.format("%d:%02d:%02d", hrs, mins, secs)
    } else {
        String.format("%02d:%02d", mins, secs)
    }
}

@OptIn(UnstableApi::class)
@Composable
fun EpisodePlayerView(
    rawVideoUrl: String,
    modifier: Modifier = Modifier,
    initialPositionSec: Int = 0,
    showSkipIntro: Boolean = false,
    introStartSec: Int = 0,
    introEndSec: Int = 0,
    fallbackNotice: String? = null,
    autoplayCountdown: Int? = null,
    nextEpisodeTitle: String? = null,
    episodeNumber: Int? = null,
    episodeTitle: String? = null,
    seriesTitle: String? = null,
    backdropUrl: String? = null,
    serverBadge: String? = null,
    hasPrevEp: Boolean = false,
    hasNextEp: Boolean = false,
    onPositionUpdate: (positionSec: Int, durationSec: Int) -> Unit = { _, _ -> },
    onVideoReady: (durationSec: Int) -> Unit = {},
    onError: (String) -> Unit = {},
    onEnded: () -> Unit = {},
    onSkipIntroClick: () -> Unit = {},
    onPlayPrevClick: (() -> Unit)? = null,
    onPlayNextClick: () -> Unit = {},
    onCancelAutoplayClick: () -> Unit = {},
    onRetrySource: (() -> Unit)? = null,
    onBackClick: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val lifecycleOwner = LocalLifecycleOwner.current

    val resolvedUrl = remember(rawVideoUrl) { resolveStreamUrl(rawVideoUrl) }
    val detectedEmbed = remember(resolvedUrl) { com.example.util.VideoEmbedUtils.parseVideoUrl(resolvedUrl) }
    val isEmbedVideo = remember(detectedEmbed) { detectedEmbed.platform != "direct" }

    var isPlaying by remember { mutableStateOf(true) }
    var isBuffering by remember { mutableStateOf(!isEmbedVideo) }
    var isFirstFrameRendered by remember { mutableStateOf(isEmbedVideo) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var currentPosSec by remember { mutableIntStateOf(initialPositionSec) }
    var totalDurationSec by remember { mutableIntStateOf(0) }
    var isFullscreen by remember { mutableStateOf(false) }
    var showControls by remember { mutableStateOf(true) }
    var isMuted by remember { mutableStateOf(false) }
    var playbackSpeed by remember { mutableFloatStateOf(1.0f) }
    var isFillMode by remember { mutableStateOf(false) }
    var showSpeedMenu by remember { mutableStateOf(false) }
    var showAspectMenu by remember { mutableStateOf(false) }
    var isControlsLocked by remember { mutableStateOf(false) }

    // Scrubbing preview bubble
    var isScrubbing by remember { mutableStateOf(false) }
    var scrubPosSec by remember { mutableIntStateOf(0) }

    // Gesture indicator states
    var gestureFeedbackText by remember { mutableStateOf<String?>(null) }
    var showDoubleTapSeekLeft by remember { mutableStateOf(false) }
    var showDoubleTapSeekRight by remember { mutableStateOf(false) }

    // Audio Manager for volume gesture
    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager }

    val exoPlayer = remember(context) {
        ExoPlayer.Builder(context)
            .setMediaSourceFactory(DefaultMediaSourceFactory(context))
            .build().apply {
                playWhenReady = true
                repeatMode = Player.REPEAT_MODE_OFF
            }
    }

    // Fullscreen back handler
    BackHandler(enabled = isFullscreen) {
        isFullscreen = false
    }

    // Keep screen on while playing
    DisposableEffect(activity, isPlaying) {
        if (isPlaying) {
            activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    // Fullscreen Immersive Mode & Orientation handling
    DisposableEffect(isFullscreen, activity) {
        val window = activity?.window
        if (window != null) {
            val controller = WindowCompat.getInsetsController(window, window.decorView)
            if (isFullscreen) {
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

    // Load media item
    LaunchedEffect(resolvedUrl, isEmbedVideo) {
        if (!isEmbedVideo && resolvedUrl.isNotBlank()) {
            exoPlayer.stop()
            exoPlayer.clearMediaItems()
            isBuffering = true
            isFirstFrameRendered = false
            errorMessage = null
            exoPlayer.setMediaItem(MediaItem.fromUri(resolvedUrl))
            exoPlayer.prepare()
            if (initialPositionSec > 0) {
                exoPlayer.seekTo(initialPositionSec * 1000L)
            }
            exoPlayer.playWhenReady = true
        } else if (isEmbedVideo) {
            isBuffering = false
            isFirstFrameRendered = true
            errorMessage = null
        }
    }

    // Controls auto-hide timer (3s)
    LaunchedEffect(showControls, isPlaying, isControlsLocked, isScrubbing) {
        if (showControls && isPlaying && !isControlsLocked && !isScrubbing) {
            delay(3000)
            showControls = false
        }
    }

    // Position updates ticker
    LaunchedEffect(exoPlayer) {
        while (true) {
            delay(300)
            if (exoPlayer.isPlaying || exoPlayer.playbackState == Player.STATE_READY) {
                val pos = (exoPlayer.currentPosition / 1000).toInt()
                val dur = (exoPlayer.duration / 1000).coerceAtLeast(0).toInt()
                if (!isScrubbing) {
                    currentPosSec = pos
                }
                totalDurationSec = dur
                onPositionUpdate(pos, dur)
            }
        }
    }

    // Double-tap seek dismiss timers
    LaunchedEffect(showDoubleTapSeekLeft) {
        if (showDoubleTapSeekLeft) {
            delay(600)
            showDoubleTapSeekLeft = false
        }
    }
    LaunchedEffect(showDoubleTapSeekRight) {
        if (showDoubleTapSeekRight) {
            delay(600)
            showDoubleTapSeekRight = false
        }
    }

    // Player event listener
    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                when (state) {
                    Player.STATE_BUFFERING -> {
                        isBuffering = true
                        errorMessage = null
                    }
                    Player.STATE_READY -> {
                        isBuffering = false
                        isFirstFrameRendered = true
                        errorMessage = null
                        val durSec = (exoPlayer.duration / 1000).coerceAtLeast(0).toInt()
                        totalDurationSec = durSec
                        onVideoReady(durSec)
                    }
                    Player.STATE_ENDED -> {
                        isBuffering = false
                        isPlaying = false
                        onEnded()
                    }
                    Player.STATE_IDLE -> {}
                }
            }

            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }

            override fun onPlayerError(error: PlaybackException) {
                val causeString = (error.cause?.toString() ?: "") + " " + (error.cause?.javaClass?.name ?: "")
                val msg = error.localizedMessage ?: error.message ?: "Video playback error"

                val isAudioSinkError = causeString.contains("UnexpectedDiscontinuityException") ||
                        causeString.contains("AudioSink") ||
                        causeString.contains("AudioTrack") ||
                        msg.contains("UnexpectedDiscontinuityException") ||
                        msg.contains("AudioSink") ||
                        msg.contains("AudioTrack") ||
                        msg.contains("discontinuity") ||
                        error.errorCode == PlaybackException.ERROR_CODE_AUDIO_TRACK_WRITE_FAILED ||
                        error.errorCode == PlaybackException.ERROR_CODE_AUDIO_TRACK_INIT_FAILED

                if (isAudioSinkError) {
                    errorMessage = null
                    val currentPos = exoPlayer.currentPosition
                    exoPlayer.playWhenReady = false
                    exoPlayer.seekTo(if (currentPos > 100) currentPos else 0L)
                    exoPlayer.prepare()
                    exoPlayer.playWhenReady = true
                } else {
                    isBuffering = false
                    errorMessage = msg
                    onError(msg)
                }
            }
        }
        exoPlayer.addListener(listener)

        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> exoPlayer.pause()
                Lifecycle.Event.ON_RESUME -> if (isPlaying) exoPlayer.play()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            exoPlayer.removeListener(listener)
            exoPlayer.stop()
            exoPlayer.clearMediaItems()
            exoPlayer.release()
        }
    }

    val containerModifier = if (isFullscreen) {
        Modifier.fillMaxSize()
    } else {
        modifier.fillMaxWidth().aspectRatio(16f / 9f)
    }

    Box(
        modifier = containerModifier
            .background(SeriesTokens.StageBlack)
            .pointerInput(isFullscreen, isControlsLocked) {
                detectTapGestures(
                    onDoubleTap = { offset ->
                        if (!isControlsLocked) {
                            val width = size.width
                            if (offset.x < width / 2) {
                                val newPos = (exoPlayer.currentPosition - 10000L).coerceAtLeast(0L)
                                exoPlayer.seekTo(newPos)
                                showDoubleTapSeekLeft = true
                            } else {
                                val newPos = (exoPlayer.currentPosition + 10000L).coerceAtMost(exoPlayer.duration)
                                exoPlayer.seekTo(newPos)
                                showDoubleTapSeekRight = true
                            }
                        }
                    },
                    onTap = {
                        showControls = !showControls
                    },
                    onLongPress = {
                        if (!isControlsLocked) {
                            exoPlayer.playbackParameters = PlaybackParameters(2.0f)
                            gestureFeedbackText = "2x Fast Forward"
                        }
                    }
                )
            }
            .pointerInput(isFullscreen, isControlsLocked) {
                if (isFullscreen && !isControlsLocked) {
                    detectDragGestures(
                        onDragEnd = { gestureFeedbackText = null },
                        onDragCancel = { gestureFeedbackText = null },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            val (x, _) = change.position
                            val width = size.width
                            if (x < width / 2) {
                                // Left half: Brightness
                                activity?.let { act ->
                                    val lp = act.window.attributes
                                    var currentB = if (lp.screenBrightness < 0f) 0.5f else lp.screenBrightness
                                    currentB = (currentB - dragAmount.y / 600f).coerceIn(0.01f, 1f)
                                    lp.screenBrightness = currentB
                                    act.window.attributes = lp
                                    gestureFeedbackText = "Brightness: ${(currentB * 100).toInt()}%"
                                }
                            } else {
                                // Right half: Volume
                                audioManager?.let { am ->
                                    val maxVol = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                                    val currentVol = am.getStreamVolume(AudioManager.STREAM_MUSIC)
                                    val delta = if (dragAmount.y < 0) 1 else if (dragAmount.y > 0) -1 else 0
                                    if (delta != 0) {
                                        val newVol = (currentVol + delta).coerceIn(0, maxVol)
                                        am.setStreamVolume(AudioManager.STREAM_MUSIC, newVol, 0)
                                        val pct = (newVol * 100) / maxVol
                                        gestureFeedbackText = "Volume: $pct%"
                                    }
                                }
                            }
                        }
                    )
                }
            }
            .testTag("episode_player_container")
    ) {
        // Blurred backdrop until first frame is rendered
        if (!isFirstFrameRendered && !backdropUrl.isNullOrBlank()) {
            AsyncImage(
                model = backdropUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .blur(20.dp)
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
            )
        }

        // Video View Surface: Embedded iframe video or ExoPlayer Surface
        if (isEmbedVideo) {
            EmbedVideoPlayer(
                videoUrl = resolvedUrl,
                title = episodeTitle ?: seriesTitle,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        player = exoPlayer
                        useController = false
                        resizeMode = if (isFillMode) AspectRatioFrameLayout.RESIZE_MODE_ZOOM else AspectRatioFrameLayout.RESIZE_MODE_FIT
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                    }
                },
                update = { playerView ->
                    if (playerView.player != exoPlayer) {
                        playerView.player = exoPlayer
                    }
                    playerView.resizeMode = if (isFillMode) AspectRatioFrameLayout.RESIZE_MODE_ZOOM else AspectRatioFrameLayout.RESIZE_MODE_FIT
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        // Floating Gesture Feedback Badge (Brightness / Volume / 2x Speed)
        if (gestureFeedbackText != null) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    color = Color.Black.copy(alpha = 0.8f),
                    shape = SeriesTokens.shapePill,
                    border = BorderStroke(1.dp, QYellowPrimary)
                ) {
                    Text(
                        text = gestureFeedbackText ?: "",
                        color = QYellowPrimary,
                        style = SeriesTypography.title,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)
                    )
                }
            }
        }

        // Double-Tap Seek Ripples (-10s / +10s)
        if (showDoubleTapSeekLeft) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(0.45f)
                    .align(Alignment.CenterStart)
                    .background(Color.White.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Replay10, contentDescription = null, tint = Color.White, modifier = Modifier.size(44.dp))
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("-10s", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
        }
        if (showDoubleTapSeekRight) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(0.45f)
                    .align(Alignment.CenterEnd)
                    .background(Color.White.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Forward10, contentDescription = null, tint = Color.White, modifier = Modifier.size(44.dp))
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("+10s", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
        }

        // Center Controls: Prev Ep, -10s, Large Golden Play/Pause, +10s, Next Ep
        if (showControls && !isControlsLocked && errorMessage == null) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                if (isBuffering) {
                    CircularProgressIndicator(
                        color = QYellowPrimary,
                        strokeWidth = 3.5.dp,
                        modifier = Modifier.size(52.dp)
                    )
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        // Previous Episode
                        if (hasPrevEp && onPlayPrevClick != null) {
                            IconButton(
                                onClick = onPlayPrevClick,
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(Color.Black.copy(alpha = 0.5f))
                            ) {
                                Icon(Icons.Default.SkipPrevious, contentDescription = "Previous episode", tint = Color.White)
                            }
                        }

                        // Rewind 10s
                        IconButton(
                            onClick = {
                                val newPos = (exoPlayer.currentPosition - 10000L).coerceAtLeast(0L)
                                exoPlayer.seekTo(newPos)
                            },
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.5f))
                        ) {
                            Icon(Icons.Default.Replay10, contentDescription = "Rewind 10s", tint = Color.White, modifier = Modifier.size(24.dp))
                        }

                        // Center Golden Play / Pause Button with Spring Bounce
                        Surface(
                            onClick = {
                                if (exoPlayer.isPlaying) exoPlayer.pause() else exoPlayer.play()
                            },
                            shape = CircleShape,
                            color = QYellowPrimary,
                            shadowElevation = 8.dp,
                            modifier = Modifier
                                .size(64.dp)
                                .pressScale(onClick = {
                                    if (exoPlayer.isPlaying) exoPlayer.pause() else exoPlayer.play()
                                })
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = if (isPlaying) "Pause" else "Play",
                                    tint = QOnYellow,
                                    modifier = Modifier.size(38.dp)
                                )
                            }
                        }

                        // Forward 10s
                        IconButton(
                            onClick = {
                                val newPos = (exoPlayer.currentPosition + 10000L).coerceAtMost(exoPlayer.duration)
                                exoPlayer.seekTo(newPos)
                            },
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.5f))
                        ) {
                            Icon(Icons.Default.Forward10, contentDescription = "Forward 10s", tint = Color.White, modifier = Modifier.size(24.dp))
                        }

                        // Next Episode
                        if (hasNextEp) {
                            IconButton(
                                onClick = onPlayNextClick,
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(Color.Black.copy(alpha = 0.5f))
                            ) {
                                Icon(Icons.Default.SkipNext, contentDescription = "Next episode", tint = Color.White)
                            }
                        }
                    }
                }
            }
        }

        // TOP CONTROLS BAR: Back, "Series - E3", Server Badge, Settings, Lock
        AnimatedVisibility(
            visible = showControls,
            enter = fadeIn(animationSpec = tween(180)),
            exit = fadeOut(animationSpec = tween(180)),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SeriesTokens.ScrimTop)
                    .statusBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        IconButton(
                            onClick = {
                                if (isFullscreen) isFullscreen = false
                                else onBackClick?.invoke()
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        // EP Pill
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = QYellowPrimary
                        ) {
                            Text(
                                text = "EP ${episodeNumber ?: 1}",
                                color = QOnYellow,
                                style = SeriesTypography.micro,
                                fontWeight = FontWeight.Black,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        // Series & Episode Title
                        Column(modifier = Modifier.weight(1f)) {
                            if (!seriesTitle.isNullOrBlank()) {
                                Text(
                                    text = seriesTitle,
                                    color = Color.White.copy(alpha = 0.7f),
                                    style = SeriesTypography.micro,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Text(
                                text = episodeTitle ?: "Now Playing",
                                color = Color.White,
                                style = SeriesTypography.label,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // Right Group: Server chip & Aspect Ratio & Lock (fullscreen)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (!serverBadge.isNullOrBlank()) {
                            Surface(
                                shape = SeriesTokens.shapePill,
                                color = Color.Black.copy(alpha = 0.5f),
                                border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.3f)),
                                modifier = Modifier.clickable { showAspectMenu = !showAspectMenu }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = serverBadge,
                                        color = Color.White,
                                        style = SeriesTypography.micro,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        imageVector = if (isFillMode) Icons.Default.AspectRatio else Icons.Default.FitScreen,
                                        contentDescription = "Aspect Ratio",
                                        tint = QYellowPrimary,
                                        modifier = Modifier.size(13.dp)
                                    )
                                }
                            }

                            DropdownMenu(
                                expanded = showAspectMenu,
                                onDismissRequest = { showAspectMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Fit (Original 16:9)") },
                                    onClick = {
                                        isFillMode = false
                                        showAspectMenu = false
                                    },
                                    leadingIcon = {
                                        if (!isFillMode) Icon(Icons.Default.Check, contentDescription = null, tint = QYellowPrimary)
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Fill (Crop to Screen)") },
                                    onClick = {
                                        isFillMode = true
                                        showAspectMenu = false
                                    },
                                    leadingIcon = {
                                        if (isFillMode) Icon(Icons.Default.Check, contentDescription = null, tint = QYellowPrimary)
                                    }
                                )
                            }
                        }

                        if (isFullscreen) {
                            IconButton(
                                onClick = { isControlsLocked = !isControlsLocked },
                                modifier = Modifier.size(34.dp)
                            ) {
                                Icon(
                                    imageVector = if (isControlsLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                                    contentDescription = "Lock controls",
                                    tint = if (isControlsLocked) QYellowPrimary else Color.White
                                )
                            }
                        }
                    }
                }
            }
        }

        // BOTTOM CONTROLS BAR: Skip Intro, Seek Slider with Draggable Thumb & Scrubber Preview, Controls
        AnimatedVisibility(
            visible = showControls && !isControlsLocked,
            enter = fadeIn(animationSpec = tween(180)),
            exit = fadeOut(animationSpec = tween(180)),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SeriesTokens.ScrimBottom)
                    .navigationBarsPadding()
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                // Skip Intro Pill (Slides in between introStartSec and introEndSec)
                if (showSkipIntro && introEndSec > 0 && currentPosSec in introStartSec until introEndSec) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 6.dp),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Surface(
                            onClick = {
                                exoPlayer.seekTo(introEndSec * 1000L)
                                onSkipIntroClick()
                            },
                            shape = SeriesTokens.shapePill,
                            color = QYellowPrimary,
                            shadowElevation = 4.dp
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.FastForward, contentDescription = null, tint = QOnYellow, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "SKIP INTRO",
                                    style = SeriesTypography.micro,
                                    fontWeight = FontWeight.Black,
                                    color = QOnYellow
                                )
                            }
                        }
                    }
                }

                // Scrubbing Time Preview Bubble
                if (isScrubbing) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Surface(
                            shape = SeriesTokens.shapePill,
                            color = Color.Black.copy(alpha = 0.8f),
                            border = BorderStroke(1.dp, QYellowPrimary)
                        ) {
                            Text(
                                text = formatTime(scrubPosSec),
                                color = QYellowPrimary,
                                style = SeriesTypography.label,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                }

                // Golden Scrubber Seekbar
                Slider(
                    value = (if (isScrubbing) scrubPosSec else currentPosSec).toFloat()
                        .coerceIn(0f, totalDurationSec.toFloat().coerceAtLeast(1f)),
                    onValueChange = { newPos ->
                        isScrubbing = true
                        scrubPosSec = newPos.toInt()
                    },
                    onValueChangeFinished = {
                        exoPlayer.seekTo(scrubPosSec * 1000L)
                        currentPosSec = scrubPosSec
                        isScrubbing = false
                    },
                    valueRange = 0f..totalDurationSec.toFloat().coerceAtLeast(1f),
                    colors = SliderDefaults.colors(
                        thumbColor = QYellowPrimary,
                        activeTrackColor = QYellowPrimary,
                        inactiveTrackColor = Color.White.copy(alpha = 0.25f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(18.dp)
                )

                // Time + Controls Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        IconButton(
                            onClick = {
                                if (exoPlayer.isPlaying) exoPlayer.pause() else exoPlayer.play()
                            },
                            modifier = Modifier.size(30.dp)
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = null,
                                tint = Color.White
                            )
                        }

                        IconButton(
                            onClick = {
                                isMuted = !isMuted
                                exoPlayer.volume = if (isMuted) 0f else 1f
                            },
                            modifier = Modifier.size(30.dp)
                        ) {
                            Icon(
                                imageVector = if (isMuted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                                contentDescription = "Mute",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        // Timestamps
                        Text(
                            text = "${formatTime(currentPosSec)} / ${formatTime(totalDurationSec)}",
                            color = Color.White,
                            style = SeriesTypography.label,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Playback Speed Chip
                        Box {
                            Surface(
                                shape = SeriesTokens.shapePill,
                                color = Color.White.copy(alpha = 0.15f),
                                modifier = Modifier.clickable { showSpeedMenu = !showSpeedMenu }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Speed, contentDescription = null, tint = Color.White, modifier = Modifier.size(13.dp))
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = "${if (playbackSpeed == 1.0f) "1" else playbackSpeed}x",
                                        color = Color.White,
                                        style = SeriesTypography.micro,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            DropdownMenu(
                                expanded = showSpeedMenu,
                                onDismissRequest = { showSpeedMenu = false }
                            ) {
                                listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f).forEach { speed ->
                                    DropdownMenuItem(
                                        text = { Text("${speed}x") },
                                        onClick = {
                                            playbackSpeed = speed
                                            exoPlayer.playbackParameters = PlaybackParameters(speed)
                                            showSpeedMenu = false
                                        },
                                        leadingIcon = {
                                            if (playbackSpeed == speed) {
                                                Icon(Icons.Default.Check, contentDescription = null, tint = QYellowPrimary)
                                            }
                                        }
                                    )
                                }
                            }
                        }

                        // Fullscreen Toggle
                        IconButton(
                            onClick = { isFullscreen = !isFullscreen },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = if (isFullscreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                                contentDescription = "Toggle Fullscreen",
                                tint = Color.White
                            )
                        }
                    }
                }
            }
        }

        // Error State Overlay Card
        if (errorMessage != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.9f))
                    .padding(20.dp),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = SeriesTokens.shapeCard,
                    color = QSurface,
                    border = BorderStroke(1.dp, QCardBorder)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = QLikeRed, modifier = Modifier.size(40.dp))
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Playback Interrupted",
                            style = SeriesTypography.title,
                            color = QOnSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = errorMessage ?: "Failed to stream source",
                            style = SeriesTypography.micro,
                            color = QOnSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            if (onRetrySource != null) {
                                OutlinedButton(
                                    onClick = onRetrySource,
                                    shape = SeriesTokens.shapePill
                                ) {
                                    Text("Retry Source", color = QOnSurface)
                                }
                            }
                            Button(
                                onClick = {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(resolvedUrl))
                                    context.startActivity(intent)
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = QYellowPrimary,
                                    contentColor = QOnYellow
                                ),
                                shape = SeriesTokens.shapePill
                            ) {
                                Text("Open in Browser", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // Autoplay Next Episode Countdown Overlay
        if (autoplayCountdown != null && autoplayCountdown > 0) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.88f)),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = SeriesTokens.shapeCard,
                    color = QSurface,
                    border = BorderStroke(1.5.dp, QYellowPrimary)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Countdown Ring
                        RatingRing(
                            rating = autoplayCountdown.toDouble(),
                            maxRating = 10.0,
                            size = 54.dp,
                            strokeWidth = 4.dp
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Up Next in $autoplayCountdown seconds",
                            style = SeriesTypography.title,
                            color = QOnSurface
                        )
                        if (!nextEpisodeTitle.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = nextEpisodeTitle,
                                style = SeriesTypography.label,
                                color = QYellowDark,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedButton(
                                onClick = onCancelAutoplayClick,
                                shape = SeriesTokens.shapePill
                            ) {
                                Text("Cancel", color = QOnSurface)
                            }
                            Button(
                                onClick = onPlayNextClick,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = QYellowPrimary,
                                    contentColor = QOnYellow
                                ),
                                shape = SeriesTokens.shapePill
                            ) {
                                Text("Play Now", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}
