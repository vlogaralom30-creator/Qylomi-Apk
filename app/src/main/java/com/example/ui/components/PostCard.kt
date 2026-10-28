package com.example.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Share
import com.example.ui.theme.QOnYellow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.HorizontalDivider
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import com.example.util.MediaParser
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Post
import com.example.data.model.UserProfile
import com.example.ui.theme.QCardBorder
import com.example.ui.theme.QLikeRed
import com.example.ui.theme.QOnSurface
import com.example.ui.theme.QOnSurfaceVariant
import com.example.ui.theme.QRelayGreen
import com.example.ui.theme.QSurface
import com.example.ui.theme.QYellowDark
import com.example.ui.theme.QYellowLight
import com.example.ui.theme.QYellowPrimary

@Composable
fun PostCard(
    post: Post,
    onLikeClick: () -> Unit,
    onCommentClick: () -> Unit,
    onAuthorClick: (UserProfile) -> Unit,
    onBookmarkClick: () -> Unit,
    currentUserId: String? = null,
    onEditPostClick: ((Post) -> Unit)? = null,
    onDeletePostClick: ((Post) -> Unit)? = null,
    onOpenReels: ((Post) -> Unit)? = null,
    onOpenMenu: ((Post) -> Unit)? = null,
    onLikersClick: ((Post) -> Unit)? = null,
    onRelayClick: ((Post) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val author = post.authorProfile ?: UserProfile(
        id = post.userId,
        displayName = "Creator",
        username = "creator"
    )

    var isBookmarked by remember { mutableStateOf(post.isBookmarkedByMe) }
    var showMenu by remember { mutableStateOf(false) }
    val isOwner = currentUserId != null && currentUserId == post.userId

    val pixelArt = post.effectivePixelArt
    val videoInfo = post.videoDetails
    val photoInfo = post.photoDetails
    val textInfo = post.textDetails

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(QSurface)
            .testTag("post_card_${post.id}")
    ) {
        // 1. Pic - Name - Badge - 3dot (Header), Title, and Read now / text content (with 16dp horizontal padding)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            // Author row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                UserAvatar(
                    avatarUrl = author.avatarUrl,
                    initials = author.initials,
                    size = 44.dp,
                    modifier = Modifier.clickable { onAuthorClick(author) }
                )

                Spacer(modifier = Modifier.width(10.dp))

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onAuthorClick(author) }
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = author.effectiveDisplayName,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = QOnSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        ProfileBadge(author)
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "@${author.effectiveUsername}",
                            fontSize = 12.sp,
                            color = QOnSurfaceVariant
                        )
                        Text(
                            text = " • ",
                            fontSize = 12.sp,
                            color = QOnSurfaceVariant
                        )
                        Text(
                            text = "${formatTimeAgo(post.createdAt)}${if (post.isEdited) " (edited)" else ""}",
                            fontSize = 12.sp,
                            color = QOnSurfaceVariant
                        )
                    }
                }

                // Pinned Badge
                if (post.isPinned) {
                    Box(
                        modifier = Modifier
                            .padding(end = 6.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(QYellowPrimary)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.PushPin,
                                contentDescription = "Pinned",
                                tint = QOnYellow,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "Pinned",
                                color = QOnYellow,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Post Type Badge
                post.type?.takeIf { it.isNotBlank() }?.let { type ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(QYellowLight)
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = type,
                            color = QYellowDark,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // Action Menu Button
                Box {
                    IconButton(
                        onClick = {
                            if (onOpenMenu != null) {
                                onOpenMenu(post)
                            } else {
                                showMenu = true
                            }
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "More Options",
                            tint = QOnSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    if (onOpenMenu == null && isOwner) {
                        androidx.compose.material3.DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            androidx.compose.material3.DropdownMenuItem(
                                text = { Text("Edit Post") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = null
                                    )
                                },
                                onClick = {
                                    showMenu = false
                                    onEditPostClick?.invoke(post)
                                }
                            )
                            androidx.compose.material3.DropdownMenuItem(
                                text = { Text("Delete Post", color = QLikeRed) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = null,
                                        tint = QLikeRed
                                    )
                                },
                                onClick = {
                                    showMenu = false
                                    onDeletePostClick?.invoke(post)
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Title
            if (!post.title.isNullOrBlank()) {
                Text(
                    text = post.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = QOnSurface,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }

            // Read now / content text
            when {
                pixelArt != null -> {
                    val cleanText = com.example.util.PixelArtUtils.stripPixelArtComment(post.content)
                    if (cleanText.isNotBlank()) {
                        Text(
                            text = cleanText,
                            fontSize = 14.sp,
                            color = QOnSurface,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }
                }
                videoInfo != null || post.type.equals("Video", ignoreCase = true) || (!post.externalUrl.isNullOrBlank() && com.example.video.VideoPlatformRegistry.resolve(post.externalUrl).platformId != "direct") -> {
                    if (post.content.isNotBlank() && post.content != post.title) {
                        Text(
                            text = post.content,
                            fontSize = 14.sp,
                            color = QOnSurface,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }
                }
                photoInfo != null || post.type.equals("Photo", ignoreCase = true) -> {
                    val caption = photoInfo?.caption ?: post.content
                    if (caption.isNotBlank() && caption != post.title) {
                        Text(
                            text = caption,
                            fontSize = 14.sp,
                            color = QOnSurface,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }
                }
                textInfo != null || post.type.equals("Text", ignoreCase = true) -> {
                    // Handled in media section
                }
                else -> {
                    val cleanContent = remember(post.content) {
                        MediaParser.cleanContentText(post.content)
                    }
                    var isExpanded by remember { mutableStateOf(false) }
                    val isLongText = remember(cleanContent) {
                        cleanContent.length > 220 || cleanContent.lines().size > 4
                    }

                    if (cleanContent.isNotBlank()) {
                        Text(
                            text = cleanContent,
                            fontSize = 14.sp,
                            lineHeight = 20.sp,
                            color = QOnSurface,
                            maxLines = if (isExpanded) Int.MAX_VALUE else 4,
                            overflow = TextOverflow.Ellipsis
                        )

                        if (isLongText) {
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (isExpanded) "Show less" else "Read now...",
                                color = QYellowDark,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                modifier = Modifier
                                    .clickable { isExpanded = !isExpanded }
                                    .padding(vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }

        // 2. Video - image - pixels - others 2side 0speach (Edge-to-edge media with 0 side spacing)
        Box(
            modifier = Modifier
                .fillMaxWidth()
        ) {
            when {
                // Pixel Art Viewer
                pixelArt != null -> {
                    PixelArtViewer(
                        data = pixelArt,
                        title = post.title ?: "Hand-Drawn Pixel Art",
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Video Post
                videoInfo != null || post.type.equals("Video", ignoreCase = true) || (!post.externalUrl.isNullOrBlank() && com.example.video.VideoPlatformRegistry.resolve(post.externalUrl).platformId != "direct") -> {
                    val rawUrl = videoInfo?.videoUrl?.takeIf { it.isNotBlank() }
                        ?: post.externalUrl?.takeIf { it.isNotBlank() }
                        ?: videoInfo?.directUrl
                        ?: ""

                    if (rawUrl.isNotBlank()) {
                        val handler = com.example.video.VideoPlatformRegistry.resolve(rawUrl)
                        val parsed = handler.parse(rawUrl)
                        val effectiveId = videoInfo?.externalId ?: parsed?.videoId
                        val isShortOrVertical = videoInfo?.isVertical ?: (parsed?.isShort == true)
                        val fallbackThumb = if (effectiveId != null && handler.platformId == "youtube") {
                            "https://i.ytimg.com/vi/$effectiveId/hqdefault.jpg"
                        } else null
                        val thumb = videoInfo?.thumbnailUrl?.takeIf { it.isNotBlank() } ?: fallbackThumb

                        val extractedVideo = com.example.video.ExtractedVideo(
                            platform = videoInfo?.platform ?: handler.platformId,
                            videoId = effectiveId,
                            originalUrl = rawUrl,
                            embedUrl = videoInfo?.embedUrl ?: if (effectiveId != null && handler.platformId == "youtube") "https://www.youtube.com/embed/$effectiveId" else rawUrl,
                            directUrl = videoInfo?.directUrl,
                            thumbnailUrl = thumb,
                            title = videoInfo?.title ?: post.title,
                            isVertical = isShortOrVertical,
                            isShort = parsed?.isShort == true,
                            durationSec = videoInfo?.durationSeconds,
                            isNativePlayable = !videoInfo?.directUrl.isNullOrBlank(),
                            requestHeaders = if (handler.platformId == "gdrive") {
                                val fileId = effectiveId?.substringBefore("|") ?: ""
                                val resourceKey = effectiveId?.substringAfter("|", "").takeIf { it?.isNotBlank() == true }
                                val headers = mutableMapOf<String, String>()
                                if (resourceKey != null) {
                                    headers["X-Goog-Drive-Resource-Keys"] = "$fileId/$resourceKey"
                                }
                                try {
                                    val fieldSha = com.example.BuildConfig::class.java.getField("GOOGLE_DRIVE_SHA1")
                                    val cert = fieldSha.get(null) as? String
                                    if (cert != null) {
                                        headers["X-Android-Package"] = com.example.BuildConfig.APPLICATION_ID
                                        headers["X-Android-Cert"] = cert.uppercase().replace(":", "")
                                    }
                                } catch (_: Exception) {}
                                headers.takeIf { it.isNotEmpty() }
                            } else null
                        )

                        Box(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            handler.Player(
                                video = extractedVideo,
                                modifier = Modifier.fillMaxWidth(),
                                config = com.example.video.PlayerConfig(
                                    autoPlay = false,
                                    showControls = true
                                ),
                                controller = null
                            )

                            if (isShortOrVertical && onOpenReels != null) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(8.dp)
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(Color.Black.copy(alpha = 0.65f))
                                        .clickable { onOpenReels(post) }
                                        .padding(horizontal = 10.dp, vertical = 5.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Movie,
                                            contentDescription = "Open in Reels",
                                            tint = Color.White,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "Reels",
                                            color = Color.White,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Photo Gallery
                photoInfo != null || post.type.equals("Photo", ignoreCase = true) -> {
                    val urls = photoInfo?.photoUrls?.takeIf { it.isNotEmpty() }
                        ?: listOfNotNull(post.externalUrl?.takeIf { it.isNotBlank() })
                    if (urls.isNotEmpty()) {
                        PhotoPostGallery(
                            photoUrls = urls,
                            caption = photoInfo?.caption ?: post.content,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                // Text Post Card
                textInfo != null || post.type.equals("Text", ignoreCase = true) -> {
                    val caption = textInfo?.caption ?: post.content
                    if (caption.isNotBlank()) {
                        Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                            StyledTextPostCard(
                                content = caption,
                                templateId = textInfo?.bgColor ?: "preset_default",
                                category = textInfo?.category
                            )
                        }
                    }
                }

                // Generic Link
                else -> {
                    val parsedMedia = remember(post) { MediaParser.parseMedia(post) }
                    if (parsedMedia != null) {
                        EmbeddedMediaPlayer(
                            media = parsedMedia,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }

        // 3. Like - Track - Comments - Share (Action bar) with 16dp horizontal padding
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Like / Appreciate button
                val heartTint by animateColorAsState(
                    targetValue = if (post.isLikedByMe) QLikeRed else QOnSurfaceVariant,
                    animationSpec = spring(),
                    label = "heartTint"
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(CircleShape)
                        .padding(horizontal = 4.dp, vertical = 4.dp)
                        .testTag("like_button_${post.id}")
                ) {
                    Icon(
                        imageVector = if (post.isLikedByMe) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Like",
                        tint = heartTint,
                        modifier = Modifier
                            .size(20.dp)
                            .clickable { onLikeClick() }
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = "${post.appreciationsCount}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = heartTint,
                        modifier = Modifier.clickable {
                            if (onLikersClick != null) {
                                onLikersClick(post)
                            } else {
                                onLikeClick()
                            }
                        }
                    )
                }

                // Track / Relay button
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable {
                            onRelayClick?.invoke(post)
                        }
                        .padding(horizontal = 4.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Repeat,
                        contentDescription = "Track",
                        tint = if (post.isRelayedByMe == true) QRelayGreen else QOnSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = "${post.relaysCount}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (post.isRelayedByMe == true) QRelayGreen else QOnSurfaceVariant
                    )
                }

                // Comment button
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable { onCommentClick() }
                        .padding(horizontal = 4.dp, vertical = 4.dp)
                        .testTag("comment_button_${post.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.ChatBubbleOutline,
                        contentDescription = "Comments",
                        tint = QOnSurfaceVariant,
                        modifier = Modifier.size(19.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = "${post.repliesCount}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = QOnSurfaceVariant
                    )
                }

                // Bookmark button
                IconButton(
                    onClick = onBookmarkClick,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = if (post.isBookmarkedByMe) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                        contentDescription = "Bookmark",
                        tint = if (post.isBookmarkedByMe) QYellowPrimary else QOnSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Share button
                IconButton(
                    onClick = {
                        try {
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, "${post.content}\nShared via Qylomi")
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Share"))
                        } catch (_: Exception) {}
                    },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Share",
                        tint = QOnSurfaceVariant,
                        modifier = Modifier.size(19.dp)
                    )
                }
            }
        }

        HorizontalDivider(
            color = QCardBorder.copy(alpha = 0.4f),
            thickness = 0.8.dp
        )
    }
}

fun formatTimeAgo(isoString: String?): String {
    if (isoString.isNullOrBlank()) return "just now"
    return try {
        if (isoString.contains("T")) {
            val datePart = isoString.substringBefore("T")
            datePart.takeLast(5)
        } else {
            "recently"
        }
    } catch (_: Exception) {
        "recently"
    }
}
