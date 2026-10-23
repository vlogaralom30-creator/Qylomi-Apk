package com.example.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import com.example.util.MediaParser
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.example.ui.theme.QSurfaceVariant
import com.example.ui.theme.QVerifiedBlue
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

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = QSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, QCardBorder, RoundedCornerShape(16.dp))
            .testTag("post_card_${post.id}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // Author row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                UserAvatar(
                    avatarUrl = author.avatarUrl,
                    initials = author.initials,
                    size = 46.dp,
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
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Verified Creator",
                            tint = QVerifiedBlue,
                            modifier = Modifier.size(14.dp)
                        )
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
                            text = formatTimeAgo(post.createdAt),
                            fontSize = 12.sp,
                            color = QOnSurfaceVariant
                        )
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

                // Owner Actions Menu (Edit & Delete)
                if (isOwner) {
                    Box {
                        IconButton(
                            onClick = { showMenu = true },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = androidx.compose.material.icons.Icons.Default.MoreVert,
                                contentDescription = "More Options",
                                tint = QOnSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        androidx.compose.material3.DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            androidx.compose.material3.DropdownMenuItem(
                                text = { Text("Edit Post") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = androidx.compose.material.icons.Icons.Default.Edit,
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
                                        imageVector = androidx.compose.material.icons.Icons.Default.Delete,
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

            Spacer(modifier = Modifier.height(10.dp))

            // Post Title (if available)
            if (!post.title.isNullOrBlank()) {
                Text(
                    text = post.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = QOnSurface,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
            }

            // Post Content / Sub-Table Type Specific Rendering
            val pixelArt = post.effectivePixelArt
            val videoInfo = post.videoDetails
            val photoInfo = post.photoDetails
            val textInfo = post.textDetails

            when {
                // Pixel Art Vector Canvas
                pixelArt != null -> {
                    val cleanText = com.example.util.PixelArtUtils.stripPixelArtComment(post.content)
                    if (cleanText.isNotBlank()) {
                        Text(
                            text = cleanText,
                            fontSize = 14.sp,
                            color = QOnSurface,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }
                    PixelArtViewer(
                        data = pixelArt,
                        title = post.title ?: "Hand-Drawn Pixel Art"
                    )
                }

                // 1. Video Post Sub-Table (or Type == "Video")
                videoInfo != null || post.type.equals("Video", ignoreCase = true) -> {
                    val directUrl = videoInfo?.directUrl?.takeIf { it.isNotBlank() }
                    val urlToPlay = directUrl ?: videoInfo?.videoUrl ?: post.externalUrl ?: ""

                    if (urlToPlay.isNotBlank()) {
                        Spacer(modifier = Modifier.height(6.dp))

                        if (directUrl != null) {
                            Box(modifier = Modifier.fillMaxWidth()) {
                                NativeVideoPlayer(
                                    videoUrl = directUrl,
                                    thumbnailUrl = videoInfo.thumbnailUrl,
                                    isVertical = videoInfo.isVertical ?: false,
                                    autoPlay = false,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(if (videoInfo.isVertical == true) 360.dp else 220.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                )

                                // Open in Reels badge for vertical or direct video posts
                                if (onOpenReels != null) {
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
                        } else {
                            EmbedVideoPlayer(
                                videoUrl = urlToPlay,
                                title = videoInfo?.title ?: post.title
                            )
                        }
                    }
                }

                // 2. Photo Post Sub-Table (or Type == "Photo")
                photoInfo != null || post.type.equals("Photo", ignoreCase = true) -> {
                    val urls = photoInfo?.photoUrls?.takeIf { it.isNotEmpty() }
                        ?: listOfNotNull(post.externalUrl?.takeIf { it.isNotBlank() })
                    if (urls.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        PhotoPostGallery(
                            photoUrls = urls,
                            caption = photoInfo?.caption ?: post.content
                        )
                    }
                }

                // 3. Text / Thought Post Sub-Table (or Type == "Text")
                textInfo != null || post.type.equals("Text", ignoreCase = true) -> {
                    val caption = textInfo?.caption ?: post.content
                    if (caption.isNotBlank()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        StyledTextPostCard(
                            content = caption,
                            templateId = textInfo?.bgColor ?: "preset_default",
                            category = textInfo?.category
                        )
                    }
                }

                // 4. Legacy / General Post Format
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
                                text = if (isExpanded) "Show less" else "Read more...",
                                color = QYellowDark,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                modifier = Modifier
                                    .clickable { isExpanded = !isExpanded }
                                    .padding(vertical = 2.dp)
                            )
                        }
                    }

                    // Embedded Media Player fallback (YouTube, TikTok, Drive, Direct Video/Image)
                    val parsedMedia = remember(post) { MediaParser.parseMedia(post) }
                    if (parsedMedia != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        EmbeddedMediaPlayer(media = parsedMedia)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Interactions row
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
                        .clickable { onLikeClick() }
                        .padding(horizontal = 6.dp, vertical = 6.dp)
                        .testTag("like_button_${post.id}")
                ) {
                    Icon(
                        imageVector = if (post.isLikedByMe) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Like",
                        tint = heartTint,
                        modifier = Modifier.size(19.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = "${post.appreciationsCount}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = heartTint
                    )
                }

                // Comment button
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable { onCommentClick() }
                        .padding(horizontal = 6.dp, vertical = 6.dp)
                        .testTag("comment_button_${post.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.ChatBubbleOutline,
                        contentDescription = "Comments",
                        tint = QOnSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = "${post.repliesCount}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = QOnSurfaceVariant
                    )
                }

                // Relay / Repost button
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable {
                            // Quick share trigger
                            try {
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, "${post.title?.let { "$it: " } ?: ""}${post.content}\n- Via Qylomi")
                                }
                                context.startActivity(Intent.createChooser(shareIntent, "Share post"))
                            } catch (_: Exception) {}
                        }
                        .padding(horizontal = 6.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Repeat,
                        contentDescription = "Relay",
                        tint = QRelayGreen,
                        modifier = Modifier.size(19.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = "${post.relaysCount}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = QOnSurfaceVariant
                    )
                }

                // Bookmark button (saved_posts table)
                IconButton(
                    onClick = onBookmarkClick,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = if (post.isBookmarkedByMe) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                        contentDescription = "Bookmark",
                        tint = if (post.isBookmarkedByMe) QYellowPrimary else QOnSurfaceVariant,
                        modifier = Modifier.size(19.dp)
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
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

fun formatTimeAgo(isoString: String?): String {
    if (isoString.isNullOrBlank()) return "just now"
    return try {
        // Simple relative parser fallback
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
