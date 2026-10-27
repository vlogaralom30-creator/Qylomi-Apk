package com.example.ui.screens

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.model.Post
import com.example.data.model.UserProfile
import com.example.ui.components.EmbedVideoPlayer
import com.example.ui.components.NativeVideoPlayer
import com.example.ui.components.ProfileBadge
import com.example.ui.components.UserAvatar
import com.example.ui.theme.QLikeRed
import com.example.ui.theme.QOnYellow
import com.example.ui.theme.QVerifiedBlue
import com.example.ui.theme.QYellowPrimary
import android.util.Log
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import com.example.player.resolver.MediaKey
import com.example.player.resolver.MediaResolver
import com.example.ui.theme.QCardBorder
import com.example.ui.theme.QOnSurface
import com.example.ui.theme.QOnSurfaceVariant
import com.example.ui.theme.QSurface
import com.example.video.VideoPlatformRegistry
import com.example.ui.viewmodel.DirectVideoCache
import com.example.ui.viewmodel.ReelsViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class ReelPlaybackMode {
    RESOLVING,
    NATIVE,
    EMBED,
    FAILED
}

@Composable
fun ReelsScreen(
    initialPost: Post? = null,
    currentUser: UserProfile?,
    onBack: () -> Unit,
    onAuthorClick: (UserProfile) -> Unit,
    onCommentClick: (Post) -> Unit,
    onToggleFollow: (UserProfile) -> Unit,
    isFollowing: (String) -> Boolean,
    onCreateVideoClick: () -> Unit,
    viewModel: ReelsViewModel = viewModel()
) {
    BackHandler { onBack() }
    val coroutineScope = rememberCoroutineScope()

    val reels by viewModel.reels.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isMuted by viewModel.isMuted.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(initialPost) {
        if (initialPost != null) {
            viewModel.setInitialPost(initialPost)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .testTag("reels_screen")
    ) {
        if (isLoading && reels.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = QYellowPrimary,
                    strokeWidth = 3.dp
                )
            }
        } else if (reels.isEmpty()) {
            // Empty State
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "No Reels Yet",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Explore videos from YouTube, TikTok, or post your own short-form content to start the Reels feed.",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 14.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Spacer(modifier = Modifier.height(20.dp))
                Button(
                    onClick = onCreateVideoClick,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = QYellowPrimary,
                        contentColor = QOnYellow
                    ),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Text("Create Video Reel", fontWeight = FontWeight.Bold)
                }
            }
        } else {
            val pagerState = rememberPagerState(
                initialPage = 0,
                pageCount = { reels.size }
            )

            // Listen to settled page
            LaunchedEffect(pagerState) {
                snapshotFlow { pagerState.settledPage }.collect { settledPage ->
                    viewModel.setSettledIndex(settledPage)
                    viewModel.updatePrefetchWindow(settledPage, context)
                }
            }

            VerticalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                beyondViewportPageCount = 1
            ) { page ->
                val reel = reels[page]
                val isCurrentPage = pagerState.currentPage == page && !pagerState.isScrollInProgress

                ReelPageItem(
                    reel = reel,
                    isCurrentPage = isCurrentPage,
                    isMuted = isMuted,
                    onToggleMute = { viewModel.toggleMute() },
                    onLike = { viewModel.toggleLike(reel) },
                    onBookmark = { viewModel.toggleBookmark(reel) },
                    onComment = { onCommentClick(reel) },
                    onAuthorClick = {
                        val author = reel.authorProfile ?: UserProfile(
                            id = reel.userId,
                            displayName = "Creator",
                            username = "creator"
                        )
                        onAuthorClick(author)
                    },
                    onToggleFollow = {
                        val author = reel.authorProfile ?: UserProfile(
                            id = reel.userId,
                            displayName = "Creator",
                            username = "creator"
                        )
                        onToggleFollow(author)
                    },
                    isFollowing = isFollowing(reel.userId),
                    onShare = {
                        val shareUrl = reel.videoDetails?.videoUrl ?: reel.externalUrl ?: "https://qylomi.com"
                        val sendIntent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(Intent.EXTRA_TEXT, "Check out this reel: $shareUrl")
                            type = "text/plain"
                        }
                        context.startActivity(Intent.createChooser(sendIntent, "Share Reel"))
                    },
                    onSkipToNext = {
                        if (page < reels.size - 1) {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(page + 1)
                            }
                        }
                    }
                )
            }
        }

        // Top Navigation Header (Back button + Title)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.4f))
                    .testTag("reels_back_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = "Reels",
                fontSize = 20.sp,
                fontWeight = FontWeight.Black,
                color = Color.White
            )
        }
    }
}

@Composable
fun ReelPageItem(
    reel: Post,
    isCurrentPage: Boolean,
    isMuted: Boolean,
    onToggleMute: () -> Unit,
    onLike: () -> Unit,
    onBookmark: () -> Unit,
    onComment: () -> Unit,
    onAuthorClick: () -> Unit,
    onToggleFollow: () -> Unit,
    isFollowing: Boolean,
    onShare: () -> Unit,
    onSkipToNext: () -> Unit = {}
) {
    val coroutineScope = rememberCoroutineScope()
    var isUserPaused by remember { mutableStateOf(false) }
    var isDescriptionExpanded by remember { mutableStateOf(false) }

    // Double-tap heart pop animation
    var showHeartAnim by remember { mutableStateOf(false) }
    val heartScale = remember { Animatable(0f) }

    val author = reel.authorProfile ?: UserProfile(
        id = reel.userId,
        displayName = "Creator",
        username = "creator"
    )

    val videoDetails = reel.videoDetails
    val sourceUrl = videoDetails?.videoUrl?.takeIf { it.isNotBlank() }
        ?: reel.externalUrl?.takeIf { it.isNotBlank() }
        ?: videoDetails?.directUrl
        ?: ""

    val storedDirect = videoDetails?.directUrl?.takeIf {
        it.isNotBlank() && !it.contains("tiktok.com/embed") && !it.contains("/embed/")
    } ?: DirectVideoCache.get(sourceUrl)

    val extractionUrl = videoDetails?.videoUrl?.takeIf { it.isNotBlank() && !it.endsWith(".mp4") }
        ?: reel.externalUrl?.takeIf { it.isNotBlank() && !it.endsWith(".mp4") }
        ?: sourceUrl

    val isTikTok = remember(sourceUrl, extractionUrl) {
        val lower1 = sourceUrl.lowercase()
        val lower2 = extractionUrl.lowercase()
        val lower3 = (videoDetails?.directUrl ?: "").lowercase()
        lower1.contains("tiktok.com") || lower1.contains("tikwm.com") || lower1.contains("tiklink") ||
                lower2.contains("tiktok.com") || lower2.contains("tikwm.com") || lower2.contains("tiklink") ||
                lower3.contains("tiktok.com") || lower3.contains("tikwm.com") || lower3.contains("tiktokcdn")
    }
    val isDrive = remember(sourceUrl) {
        val lower = sourceUrl.lowercase()
        lower.contains("drive.google.com") || lower.contains("docs.google.com")
    }

    var playbackMode by remember(reel.id) {
        mutableStateOf(if (isTikTok || isDrive) ReelPlaybackMode.RESOLVING else ReelPlaybackMode.NATIVE)
    }
    var activePlayUrl by remember(reel.id) {
        mutableStateOf(storedDirect ?: sourceUrl)
    }
    var activeHeaders by remember(reel.id) {
        mutableStateOf<Map<String, String>>(if (isTikTok) MediaResolver.defaultTikTokHeaders else emptyMap())
    }
    var hasRecoveredOnce by remember(reel.id) { mutableStateOf(false) }
    var retryCount by remember(reel.id) { mutableIntStateOf(0) }
    var skipCountdown by remember(reel.id) { mutableIntStateOf(3) }
    val coverThumbnail = videoDetails?.thumbnailUrl

    // Resolve on page active or retry
    LaunchedEffect(isCurrentPage, reel.id, retryCount) {
        if (!isCurrentPage) {
            isUserPaused = false
            return@LaunchedEffect
        }

        if (isTikTok || isDrive) {
            playbackMode = ReelPlaybackMode.RESOLVING
            val mediaKey = MediaKey(
                url = extractionUrl,
                platform = if (isTikTok) "tiktok" else if (isDrive) "gdrive" else "direct"
            )
            val force = retryCount > 0

            try {
                val resolved = MediaResolver.resolve(mediaKey, force = force, isHighPriority = true)
                if (resolved.isDirectPlayable && resolved.url.isNotBlank()) {
                    activePlayUrl = resolved.url
                    activeHeaders = resolved.headers
                    playbackMode = ReelPlaybackMode.NATIVE
                } else if (storedDirect != null && storedDirect.isNotBlank()) {
                    activePlayUrl = storedDirect
                    if (isTikTok) activeHeaders = MediaResolver.defaultTikTokHeaders
                    playbackMode = ReelPlaybackMode.NATIVE
                } else {
                    activePlayUrl = resolved.embedUrl ?: resolved.url
                    playbackMode = ReelPlaybackMode.EMBED
                }
            } catch (e: Exception) {
                if (storedDirect != null && storedDirect.isNotBlank()) {
                    activePlayUrl = storedDirect
                    if (isTikTok) activeHeaders = MediaResolver.defaultTikTokHeaders
                    playbackMode = ReelPlaybackMode.NATIVE
                } else {
                    val embed = videoDetails?.embedUrl ?: sourceUrl
                    activePlayUrl = embed
                    playbackMode = ReelPlaybackMode.EMBED
                }
            }
        } else {
            activePlayUrl = storedDirect ?: sourceUrl
            playbackMode = ReelPlaybackMode.NATIVE
        }
    }


    // Auto-skip timer when FAILED
    LaunchedEffect(playbackMode, reel.id) {
        if (playbackMode == ReelPlaybackMode.FAILED) {
            skipCountdown = 3
            while (skipCountdown > 0) {
                delay(1000)
                skipCountdown--
            }
            onSkipToNext()
        }
    }

    var triggerRecovery by remember(reel.id) { mutableStateOf(false) }

    LaunchedEffect(triggerRecovery, reel.id) {
        if (triggerRecovery) {
            Log.d("REELS_TT", "Starting single automatic recovery for reel ${reel.id}")
            val mediaKey = MediaKey(
                url = extractionUrl,
                platform = if (isTikTok) "tiktok" else if (isDrive) "gdrive" else "direct"
            )
            try {
                val resolved = MediaResolver.resolve(mediaKey, force = true, isHighPriority = true)
                if (resolved.isDirectPlayable && resolved.url.isNotBlank() && resolved.url != activePlayUrl) {
                    Log.d("REELS_TT", "Auto-recovery obtained fresh directUrl: ${resolved.url}")
                    activePlayUrl = resolved.url
                    activeHeaders = resolved.headers
                    playbackMode = ReelPlaybackMode.NATIVE
                } else {
                    Log.w("REELS_TT", "Auto-recovery failed or non-playable, falling back to EMBED WebView")
                    activePlayUrl = resolved.embedUrl ?: videoDetails?.embedUrl ?: sourceUrl
                    playbackMode = ReelPlaybackMode.EMBED
                }
            } catch (e: Exception) {
                Log.e("REELS_TT", "Auto-recovery exception: ${e.message}, falling back to EMBED WebView")
                activePlayUrl = videoDetails?.embedUrl ?: sourceUrl
                playbackMode = ReelPlaybackMode.EMBED
            } finally {
                triggerRecovery = false
            }
        }
    }

    val handleNativePlaybackError: (String) -> Unit = { err ->
        Log.e("REELS_TT", "Playback error in NativeVideoPlayer: $err, url: $activePlayUrl, hasRecoveredOnce: $hasRecoveredOnce")
        if (!hasRecoveredOnce) {
            hasRecoveredOnce = true
            triggerRecovery = true
        } else {
            Log.w("REELS_TT", "Already attempted recovery once. Falling back to EMBED WebView.")
            activePlayUrl = videoDetails?.embedUrl ?: sourceUrl
            playbackMode = ReelPlaybackMode.EMBED
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(reel.id) {
                detectTapGestures(
                    onDoubleTap = {
                        onLike()
                        coroutineScope.launch {
                            showHeartAnim = true
                            heartScale.snapTo(0.2f)
                            heartScale.animateTo(
                                targetValue = 1.3f,
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessLow
                                )
                            )
                            delay(300)
                            showHeartAnim = false
                        }
                    },
                    onTap = {
                        isUserPaused = !isUserPaused
                    }
                )
            }
    ) {
        // Main Video Stage
        when (playbackMode) {
            ReelPlaybackMode.RESOLVING -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    if (!coverThumbnail.isNullOrBlank()) {
                        AsyncImage(
                            model = coverThumbnail,
                            contentDescription = "Cover",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    CircularProgressIndicator(
                        color = QYellowPrimary,
                        modifier = Modifier.size(44.dp)
                    )
                }
            }

            ReelPlaybackMode.NATIVE -> {
                NativeVideoPlayer(
                    url = activePlayUrl,
                    modifier = Modifier.fillMaxSize(),
                    autoPlay = isCurrentPage && !isUserPaused,
                    loop = true,
                    muted = isMuted,
                    isVertical = true,
                    aspectRatio = 0f,
                    resizeMode = androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_ZOOM,
                    showControls = false,
                    requestHeaders = activeHeaders,
                    thumbnailUrl = coverThumbnail,
                    onError = handleNativePlaybackError
                )
            }

            ReelPlaybackMode.EMBED -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    AndroidView(
                        factory = { ctx ->
                            WebView(ctx).apply {
                                layoutParams = ViewGroup.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT
                                )
                                settings.javaScriptEnabled = true
                                settings.domStorageEnabled = true
                                settings.mediaPlaybackRequiresUserGesture = false
                                settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                                settings.userAgentString = "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Mobile Safari/537.36"
                                webChromeClient = WebChromeClient()
                                webViewClient = object : WebViewClient() {
                                    override fun onReceivedError(
                                        view: WebView?,
                                        errorCode: Int,
                                        description: String?,
                                        failingUrl: String?
                                    ) {
                                        Log.e("REELS_TT", "Embed fallback error: $description")
                                        playbackMode = ReelPlaybackMode.FAILED
                                    }

                                    override fun onRenderProcessGone(
                                        view: WebView?,
                                        detail: android.webkit.RenderProcessGoneDetail?
                                    ): Boolean {
                                        try { view?.destroy() } catch (_: Exception) {}
                                        playbackMode = ReelPlaybackMode.FAILED
                                        return true
                                    }
                                }
                                val embedUrl = if (isTikTok) {
                                    val id = VideoPlatformRegistry.resolve(sourceUrl).parse(sourceUrl)?.videoId
                                    if (id != null) "https://www.tiktok.com/embed/v2/$id" else activePlayUrl
                                } else {
                                    activePlayUrl
                                }
                                val html = """
                                    <!DOCTYPE html>
                                    <html>
                                    <head>
                                        <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0">
                                        <style>
                                            body, html { margin: 0; padding: 0; width: 100%; height: 100%; background-color: black; overflow: hidden; display: flex; justify-content: center; align-items: center; }
                                            iframe, video { width: 100%; height: 100%; border: none; }
                                        </style>
                                    </head>
                                    <body>
                                        <iframe src="$embedUrl" allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture" allowfullscreen></iframe>
                                    </body>
                                    </html>
                                """.trimIndent()
                                loadDataWithBaseURL("https://www.tiktok.com", html, "text/html", "UTF-8", null)
                            }
                        },
                        onRelease = { webView ->
                            try {
                                webView.stopLoading()
                                webView.loadUrl("about:blank")
                                webView.webChromeClient = null
                                (webView.parent as? ViewGroup)?.removeView(webView)
                                webView.destroy()
                            } catch (e: Exception) {
                                Log.e("REELS_TT", "Error releasing WebView: ${e.message}")
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            ReelPlaybackMode.FAILED -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = QSurface),
                        border = BorderStroke(1.dp, QCardBorder),
                        modifier = Modifier
                            .fillMaxWidth(0.85f)
                            .padding(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = Color.Red,
                                modifier = Modifier.size(40.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Video Playback Error",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = QOnSurface
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Could not load video source.",
                                fontSize = 13.sp,
                                color = QOnSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = {
                                    hasRecoveredOnce = false
                                    retryCount++
                                    playbackMode = ReelPlaybackMode.RESOLVING
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = QYellowPrimary,
                                    contentColor = QOnYellow
                                )
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Retry", fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "Auto-skipping in ${skipCountdown}s",
                                fontSize = 11.sp,
                                color = QOnSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // Bottom Gradient Scrim for readable text
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp)
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))
                    )
                )
        )

        // Floating Heart Animation on Double Tap
        if (showHeartAnim) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Favorite,
                    contentDescription = null,
                    tint = QLikeRed,
                    modifier = Modifier
                        .size(110.dp)
                        .scale(heartScale.value)
                )
            }
        }

        // Right Action Column (Like, Comment, Bookmark, Share, Mute)
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(end = 12.dp, bottom = 24.dp)
        ) {
            // Like Button
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clickable { onLike() }
                    .padding(vertical = 6.dp)
            ) {
                Icon(
                    imageVector = if (reel.isLikedByMe) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = "Like",
                    tint = if (reel.isLikedByMe) QLikeRed else Color.White,
                    modifier = Modifier.size(34.dp)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${reel.appreciationsCount}",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Comment Button
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clickable { onComment() }
                    .padding(vertical = 6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ChatBubbleOutline,
                    contentDescription = "Comment",
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${reel.repliesCount}",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Bookmark Button
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clickable { onBookmark() }
                    .padding(vertical = 6.dp)
            ) {
                Icon(
                    imageVector = if (reel.isBookmarkedByMe) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                    contentDescription = "Bookmark",
                    tint = if (reel.isBookmarkedByMe) QYellowPrimary else Color.White,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Save",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Share Button
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clickable { onShare() }
                    .padding(vertical = 6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = "Share",
                    tint = Color.White,
                    modifier = Modifier.size(30.dp)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Share",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Mute / Unmute Button
            IconButton(
                onClick = onToggleMute,
                modifier = Modifier
                    .size(42.dp)
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
        }

        // Bottom-Left Creator & Video Details Overlay
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth(0.78f)
                .navigationBarsPadding()
                .padding(start = 16.dp, bottom = 24.dp)
        ) {
            // Author row + Follow button
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                UserAvatar(
                    avatarUrl = author.avatarUrl,
                    initials = author.initials,
                    size = 40.dp,
                    modifier = Modifier.clickable { onAuthorClick() }
                )

                Spacer(modifier = Modifier.width(8.dp))

                Column(modifier = Modifier.clickable { onAuthorClick() }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = author.effectiveDisplayName,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        ProfileBadge(author, modifier = Modifier.size(13.dp))
                    }

                    Text(
                        text = "@${author.effectiveUsername}",
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                // Follow button if not following
                if (!isFollowing) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .border(1.dp, Color.White, RoundedCornerShape(16.dp))
                            .clickable { onToggleFollow() }
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "Follow",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Caption / Title
            val captionText = videoDetails?.title ?: reel.title ?: reel.content
            if (captionText.isNotBlank()) {
                Text(
                    text = captionText,
                    fontSize = 13.sp,
                    color = Color.White,
                    maxLines = if (isDescriptionExpanded) Int.MAX_VALUE else 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.clickable { isDescriptionExpanded = !isDescriptionExpanded }
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Audio & Platform Chip
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White.copy(alpha = 0.2f))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                val platformName = videoDetails?.platform ?: "Original Audio"
                Text(
                    text = "♫ ${platformName.replaceFirstChar { it.uppercase() }} Stream",
                    fontSize = 11.sp,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
