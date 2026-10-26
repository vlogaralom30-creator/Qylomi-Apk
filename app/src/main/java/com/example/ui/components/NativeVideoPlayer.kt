package com.example.ui.components

import android.view.ViewGroup
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.example.ui.theme.QOnYellow
import com.example.ui.theme.QYellowPrimary
import com.example.util.VideoPlayerCache
import kotlinx.coroutines.delay

@OptIn(UnstableApi::class)
@Composable
fun NativeVideoPlayer(
    url: String = "",
    videoUrl: String = url,
    modifier: Modifier = Modifier,
    autoPlay: Boolean = true,
    loop: Boolean = true,
    muted: Boolean = false,
    thumbnailUrl: String? = null,
    isVertical: Boolean = false,
    aspectRatio: Float? = null,
    resizeMode: Int = AspectRatioFrameLayout.RESIZE_MODE_FIT,
    showControls: Boolean = true,
    onReady: () -> Unit = {},
    onError: (String) -> Unit = {},
    requestHeaders: Map<String, String>? = null
) {
    val playUrl = if (videoUrl.isNotBlank()) videoUrl else url
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var isPlaying by remember { mutableStateOf(autoPlay) }
    var isMuted by remember { mutableStateOf(muted) }
    var isBuffering by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var progress by remember { mutableFloatStateOf(0f) }
    var showPlayPauseIndicator by remember { mutableStateOf(false) }
    var retryTrigger by remember { mutableStateOf(0) }

    val defaultRatio = aspectRatio ?: if (isVertical) (9f / 14f) else (16f / 9f)
    var detectedAspectRatio by remember(playUrl) { mutableFloatStateOf(defaultRatio) }

    val exoPlayer = remember(context, retryTrigger, requestHeaders) {
        val audioAttributes = androidx.media3.common.AudioAttributes.Builder()
            .setUsage(androidx.media3.common.C.USAGE_MEDIA)
            .setContentType(androidx.media3.common.C.AUDIO_CONTENT_TYPE_MOVIE)
            .build()

        val renderersFactory = object : androidx.media3.exoplayer.DefaultRenderersFactory(context) {
            override fun buildAudioSink(
                context: android.content.Context,
                enableFloatOutput: Boolean,
                enableAudioTrackPlaybackParams: Boolean
            ): androidx.media3.exoplayer.audio.AudioSink? {
                return androidx.media3.exoplayer.audio.DefaultAudioSink.Builder(context)
                    .setEnableFloatOutput(enableFloatOutput)
                    .setEnableAudioTrackPlaybackParams(enableAudioTrackPlaybackParams)
                    .build()
            }
        }.apply {
            setEnableAudioTrackPlaybackParams(true)
        }

        val mediaSourceFactory = DefaultMediaSourceFactory(
            VideoPlayerCache.buildCacheDataSourceFactory(context, requestHeaders)
        )
        ExoPlayer.Builder(context, renderersFactory)
            .setMediaSourceFactory(mediaSourceFactory)
            .setAudioAttributes(audioAttributes, true)
            .setHandleAudioBecomingNoisy(true)
            .build()
            .apply {
                repeatMode = if (loop) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
                volume = if (isMuted) 0f else 1f
                playWhenReady = autoPlay
            }
    }

    // Attach Player Listener
    DisposableEffect(exoPlayer, playUrl, retryTrigger) {
        errorMessage = null
        isBuffering = true
        try {
            val mediaItem = MediaItem.fromUri(playUrl)
            exoPlayer.setMediaItem(mediaItem)
            exoPlayer.prepare()
        } catch (e: Exception) {
            errorMessage = e.localizedMessage ?: "Failed to prepare video"
            onError(errorMessage ?: "Error")
        }

        val listener = object : Player.Listener {
            override fun onVideoSizeChanged(videoSize: androidx.media3.common.VideoSize) {
                if (videoSize.width > 0 && videoSize.height > 0) {
                    val rawRatio = videoSize.width.toFloat() / videoSize.height.toFloat()
                    detectedAspectRatio = rawRatio.coerceIn(0.5f, 2.2f)
                }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                when (playbackState) {
                    Player.STATE_BUFFERING -> {
                        isBuffering = true
                    }
                    Player.STATE_READY -> {
                        isBuffering = false
                        errorMessage = null
                        onReady()
                    }
                    Player.STATE_ENDED -> {
                        isBuffering = false
                        if (!loop) isPlaying = false
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
                    // Non-fatal audio track timestamp discontinuity: flush audio sink and resume gracefully
                    errorMessage = null
                    val currentPos = exoPlayer.currentPosition
                    exoPlayer.playWhenReady = false
                    exoPlayer.seekTo(if (currentPos > 100) currentPos else 0L)
                    exoPlayer.prepare()
                    if (autoPlay) {
                        exoPlayer.playWhenReady = true
                    }
                } else {
                    isBuffering = false
                    errorMessage = msg
                    android.util.Log.e("REELS_TT", "Playback error: $msg, code: ${error.errorCode}, cause: $causeString, url: $playUrl")
                    onError(msg)
                }
            }
        }

        exoPlayer.addListener(listener)

        onDispose {
            exoPlayer.removeListener(listener)
            exoPlayer.release()
        }
    }

    // React to autoPlay parameter changes
    LaunchedEffect(autoPlay) {
        exoPlayer.playWhenReady = autoPlay
    }

    // React to muted changes
    LaunchedEffect(isMuted) {
        exoPlayer.volume = if (isMuted) 0f else 1f
    }

    // Track playback progress
    LaunchedEffect(exoPlayer, isPlaying) {
        while (isPlaying) {
            val duration = exoPlayer.duration
            val current = exoPlayer.currentPosition
            if (duration > 0) {
                progress = (current.toFloat() / duration).coerceIn(0f, 1f)
            }
            delay(150)
        }
    }

    // Lifecycle Management: pause on ON_STOP, resume on ON_START if autoPlay
    DisposableEffect(lifecycleOwner, exoPlayer) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE, Lifecycle.Event.ON_STOP -> {
                    exoPlayer.pause()
                }
                Lifecycle.Event.ON_RESUME -> {
                    if (autoPlay && errorMessage == null) {
                        exoPlayer.play()
                    }
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val boxModifier = when {
        aspectRatio != null && aspectRatio > 0f -> modifier.fillMaxWidth().aspectRatio(aspectRatio)
        aspectRatio != null && aspectRatio == 0f -> modifier
        else -> modifier.fillMaxWidth().aspectRatio(detectedAspectRatio)
    }

    Box(
        modifier = boxModifier
            .background(Color.Black)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                if (errorMessage == null) {
                    if (exoPlayer.isPlaying) {
                        exoPlayer.pause()
                    } else {
                        exoPlayer.play()
                    }
                    showPlayPauseIndicator = true
                }
            }
            .testTag("native_video_player"),
        contentAlignment = Alignment.Center
    ) {
        // Video Surface
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = false
                    this.resizeMode = resizeMode
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
            },
            update = { playerView ->
                playerView.player = exoPlayer
                playerView.resizeMode = resizeMode
            },
            modifier = Modifier.fillMaxSize()
        )

        // Buffering Spinner
        if (isBuffering && errorMessage == null) {
            CircularProgressIndicator(
                color = QYellowPrimary,
                strokeWidth = 3.dp,
                modifier = Modifier.size(44.dp)
            )
        }

        // Tap Play/Pause Indicator Animation
        LaunchedEffect(showPlayPauseIndicator) {
            if (showPlayPauseIndicator) {
                delay(600)
                showPlayPauseIndicator = false
            }
        }

        AnimatedVisibility(
            visible = showPlayPauseIndicator,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.6f))
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.PlayArrow else Icons.Default.Pause,
                    contentDescription = if (isPlaying) "Playing" else "Paused",
                    tint = Color.White,
                    modifier = Modifier.size(36.dp)
                )
            }
        }

        // Error State
        if (errorMessage != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.85f))
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Video Playback Error",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = errorMessage ?: "Could not stream video file",
                        color = Color(0xFFEF4444),
                        fontSize = 11.sp,
                        maxLines = 2
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = { retryTrigger++ },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = QYellowPrimary,
                            contentColor = QOnYellow
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Retry", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Controls Overlay (Mute Button & Thin Progress Bar)
        if (showControls && errorMessage == null) {
            // Mute toggle (Top-Right)
            IconButton(
                onClick = { isMuted = !isMuted },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.5f))
            ) {
                Icon(
                    imageVector = if (isMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                    contentDescription = if (isMuted) "Unmute" else "Mute",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Bottom Thin Progress Bar
            LinearProgressIndicator(
                progress = { progress },
                color = QYellowPrimary,
                trackColor = Color.White.copy(alpha = 0.25f),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(2.5.dp)
            )
        }
    }
}
