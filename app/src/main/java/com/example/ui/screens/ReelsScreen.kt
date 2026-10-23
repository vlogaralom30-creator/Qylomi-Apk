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
import com.example.ui.viewmodel.ReelsViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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
                }
            }

            VerticalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                beyondViewportPageCount = 1
            ) { page ->
                val reel = reels[page]
                val isCurrentPage = pagerState.settledPage == page

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
    onShare: () -> Unit
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
    val directUrl = videoDetails?.directUrl?.takeIf { it.isNotBlank() }
    val videoUrl = directUrl ?: videoDetails?.videoUrl ?: reel.externalUrl ?: ""

    // When page changes, reset user pause state
    LaunchedEffect(isCurrentPage) {
        if (!isCurrentPage) {
            isUserPaused = false
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
        val handler = remember(videoUrl) { com.example.video.VideoPlatformRegistry.resolve(videoUrl) }
        val extractedVideo = remember(reel, videoUrl) {
            com.example.video.ExtractedVideo(
                platform = handler.platformId,
                videoId = reel.videoDetails?.externalId ?: handler.parse(videoUrl)?.videoId,
                originalUrl = videoUrl,
                embedUrl = reel.videoDetails?.embedUrl ?: videoUrl,
                directUrl = directUrl,
                thumbnailUrl = reel.videoDetails?.thumbnailUrl,
                title = reel.videoDetails?.title ?: reel.title,
                isVertical = true,
                isNativePlayable = directUrl != null,
                requestHeaders = if (handler.platformId == "gdrive") {
                    // We need to rebuild headers here if it's Drive and not yet extracted
                    // But usually Reels are extracted or have directUrl
                    null 
                } else null
            )
        }

        handler.Player(
            video = extractedVideo,
            modifier = Modifier.fillMaxSize(),
            config = com.example.video.PlayerConfig(
                autoPlay = isCurrentPage && !isUserPaused,
                muted = isMuted,
                loop = true,
                showControls = false,
                nativeOnly = handler.platformId == "gdrive"
            ),
            controller = null
        )

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
