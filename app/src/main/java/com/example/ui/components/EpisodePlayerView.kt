package com.example.ui.components

import android.app.Activity
import android.content.pm.ActivityInfo
import android.view.ViewGroup
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
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
    introEndSec: Int = 0,
    fallbackNotice: String? = null,
    autoplayCountdown: Int? = null,
    nextEpisodeTitle: String? = null,
    onPositionUpdate: (positionSec: Int, durationSec: Int) -> Unit = { _, _ -> },
    onVideoReady: (durationSec: Int) -> Unit = {},
    onError: (String) -> Unit = {},
    onEnded: () -> Unit = {},
    onSkipIntroClick: () -> Unit = {},
    onPlayNextClick: () -> Unit = {},
    onCancelAutoplayClick: () -> Unit = {},
    onBackClick: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val lifecycleOwner = LocalLifecycleOwner.current

    val resolvedUrl = remember(rawVideoUrl) { resolveStreamUrl(rawVideoUrl) }

    var isPlaying by remember { mutableStateOf(true) }
    var isBuffering by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var currentPosSec by remember { mutableIntStateOf(initialPositionSec) }
    var totalDurationSec by remember { mutableIntStateOf(0) }
    var isFullscreen by remember { mutableStateOf(false) }
    var showControls by remember { mutableStateOf(true) }
    var isMuted by remember { mutableStateOf(false) }

    // Persistent ExoPlayer instance tied to Context lifecycle (prevents recreate & freeze)
    val exoPlayer = remember(context) {
        ExoPlayer.Builder(context)
            .setMediaSourceFactory(DefaultMediaSourceFactory(context))
            .build().apply {
                playWhenReady = true
                repeatMode = Player.REPEAT_MODE_OFF
            }
    }

    // Safely update media item when URL changes (Fixes background audio & freeze)
    LaunchedEffect(resolvedUrl) {
        if (resolvedUrl.isNotBlank()) {
            exoPlayer.stop()
            exoPlayer.clearMediaItems()
            isBuffering = true
            errorMessage = null
            exoPlayer.setMediaItem(MediaItem.fromUri(resolvedUrl))
            exoPlayer.prepare()
            if (initialPositionSec > 0) {
                exoPlayer.seekTo(initialPositionSec * 1000L)
            }
            exoPlayer.playWhenReady = true
        }
    }

    // Controls auto-hide timer
    LaunchedEffect(showControls, isPlaying) {
        if (showControls && isPlaying) {
            delay(4000)
            showControls = false
        }
    }

    // Position updates ticker
    LaunchedEffect(exoPlayer) {
        while (true) {
            delay(500)
            if (exoPlayer.isPlaying || exoPlayer.playbackState == Player.STATE_READY) {
                val pos = (exoPlayer.currentPosition / 1000).toInt()
                val dur = (exoPlayer.duration / 1000).coerceAtLeast(0).toInt()
                currentPosSec = pos
                totalDurationSec = dur
                onPositionUpdate(pos, dur)
            }
        }
    }

    // ExoPlayer & Lifecycle Binding
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
                Lifecycle.Event.ON_PAUSE -> {
                    exoPlayer.pause()
                }
                Lifecycle.Event.ON_RESUME -> {
                    if (isPlaying) exoPlayer.play()
                }
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
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(16f / 9f)
            .background(QDarkObsidian)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                showControls = !showControls
            }
            .testTag("episode_player_container")
    ) {
        // Player Surface View with active update binding
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = false
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
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
            },
            modifier = Modifier.fillMaxSize()
        )

        // Buffering & Loading Spinner Overlay
        if (isBuffering && errorMessage == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = QCrunchyOrange, modifier = Modifier.size(48.dp))
                    if (!fallbackNotice.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color.Black.copy(alpha = 0.7f)
                        ) {
                            Text(
                                text = fallbackNotice,
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }
        }

        // Playback Error Overlay
        if (errorMessage != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(QDarkObsidian.copy(alpha = 0.92f))
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Error",
                        tint = Color.Red,
                        modifier = Modifier.size(40.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = errorMessage ?: "Playback error",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = {
                            exoPlayer.prepare()
                            exoPlayer.play()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = QCrunchyOrange, contentColor = Color.Black)
                    ) {
                        Text("Retry Playback", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Crunchyroll Overlay Controls & Top/Bottom Bars
        AnimatedVisibility(
            visible = showControls,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color.Black.copy(alpha = 0.7f),
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.85f)
                            )
                        )
                    )
            ) {
                // Top Bar: Back Button, Title, Mute Toggle
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopStart)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (onBackClick != null) {
                            IconButton(
                                onClick = onBackClick,
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color.Black.copy(alpha = 0.4f))
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    tint = Color.White
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                        }

                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = QCrunchyOrange,
                            modifier = Modifier.padding(end = 6.dp)
                        ) {
                            Text(
                                text = "CRUNCHYROLL STREAM",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.Black,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        IconButton(
                            onClick = {
                                isMuted = !isMuted
                                exoPlayer.volume = if (isMuted) 0f else 1f
                            },
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.4f))
                        ) {
                            Icon(
                                imageVector = if (isMuted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                                contentDescription = "Mute",
                                tint = Color.White
                            )
                        }
                    }
                }

                // Center Play/Pause & Fast Rewind / Seek Controls
                Row(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalArrangement = Arrangement.spacedBy(32.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Rewind 10s
                    IconButton(
                        onClick = {
                            val newPos = (exoPlayer.currentPosition - 10000L).coerceAtLeast(0L)
                            exoPlayer.seekTo(newPos)
                        },
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.5f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Replay10,
                            contentDescription = "Rewind 10s",
                            tint = Color.White,
                            modifier = Modifier.size(26.dp)
                        )
                    }

                    // Main Play / Pause Button
                    IconButton(
                        onClick = {
                            if (isPlaying) {
                                exoPlayer.pause()
                            } else {
                                exoPlayer.play()
                            }
                            isPlaying = !isPlaying
                        },
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(QCrunchyOrange)
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint = Color.Black,
                            modifier = Modifier.size(38.dp)
                        )
                    }

                    // Forward 10s
                    IconButton(
                        onClick = {
                            val newPos = (exoPlayer.currentPosition + 10000L).coerceAtMost(exoPlayer.duration)
                            exoPlayer.seekTo(newPos)
                        },
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.5f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Forward10,
                            contentDescription = "Forward 10s",
                            tint = Color.White,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }

                // Bottom Controls: Scrubber, Timers, Skip Intro, Fullscreen
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    // Skip Intro Overlay Button if available
                    if (showSkipIntro && introEndSec > 0) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 6.dp),
                            horizontalArrangement = Arrangement.End
                        ) {
                            Button(
                                onClick = {
                                    exoPlayer.seekTo(introEndSec * 1000L)
                                    onSkipIntroClick()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = QCrunchyOrange, contentColor = Color.Black),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                            ) {
                                Icon(imageVector = Icons.Default.FastForward, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("SKIP INTRO", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // Scrubber Slider
                    Slider(
                        value = currentPosSec.toFloat().coerceIn(0f, totalDurationSec.toFloat().coerceAtLeast(1f)),
                        onValueChange = { newPos ->
                            currentPosSec = newPos.toInt()
                        },
                        onValueChangeFinished = {
                            exoPlayer.seekTo(currentPosSec * 1000L)
                        },
                        valueRange = 0f..totalDurationSec.toFloat().coerceAtLeast(1f),
                        colors = SliderDefaults.colors(
                            thumbColor = QCrunchyOrange,
                            activeTrackColor = QCrunchyOrange,
                            inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(18.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${formatTime(currentPosSec)} / ${formatTime(totalDurationSec)}",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Next Episode Quick Button
                            IconButton(
                                onClick = onPlayNextClick,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SkipNext,
                                    contentDescription = "Next Episode",
                                    tint = Color.White
                                )
                            }

                            // Fullscreen Toggle
                            IconButton(
                                onClick = {
                                    isFullscreen = !isFullscreen
                                    activity?.requestedOrientation = if (isFullscreen) {
                                        ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                                    } else {
                                        ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                                    }
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = if (isFullscreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                                    contentDescription = "Fullscreen",
                                    tint = Color.White
                                )
                            }
                        }
                    }
                }
            }
        }

        // Auto-play Next Episode Overlay Box
        if (autoplayCountdown != null && autoplayCountdown > 0) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.85f)),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = QDarkSurface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, QCrunchyOrange)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Next Episode starting in $autoplayCountdown s...",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = Color.White
                        )
                        if (!nextEpisodeTitle.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = nextEpisodeTitle,
                                fontSize = 13.sp,
                                color = QCrunchyOrange
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedButton(
                                onClick = onCancelAutoplayClick,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Cancel", color = Color.White)
                            }
                            Button(
                                onClick = onPlayNextClick,
                                colors = ButtonDefaults.buttonColors(containerColor = QCrunchyOrange, contentColor = Color.Black),
                                shape = RoundedCornerShape(8.dp)
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
