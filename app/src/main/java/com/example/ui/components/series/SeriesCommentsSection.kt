package com.example.ui.components.series

import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.SeriesComment
import com.example.ui.theme.*

enum class CommentSortOption(val label: String) {
    TOP("Top"),
    NEWEST("Newest"),
    OLDEST("Oldest")
}

/**
 * 10-Star Golden Cinema Rating Bottom Sheet
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SeriesRatingBottomSheet(
    seriesTitle: String,
    initialRating: Int?,
    avgRating: Double?,
    ratingsCount: Int,
    onDismiss: () -> Unit,
    onSubmitRating: (Int) -> Unit
) {
    var selectedScore by remember { mutableIntStateOf(initialRating ?: 8) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = QSurface,
        shape = SeriesTokens.shapeSheet
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Drag handle already rendered by ModalBottomSheet
            Text(
                text = "Rate \"$seriesTitle\"",
                style = SeriesTypography.headline,
                color = QOnSurface
            )

            if (avgRating != null && avgRating > 0) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Community score: ★ ${String.format("%.1f", avgRating)} ($ratingsCount votes)",
                    style = SeriesTypography.label,
                    color = QOnSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Big Score Display with Ring
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                RatingRing(
                    rating = selectedScore.toDouble(),
                    maxRating = 10.0,
                    size = 64.dp,
                    strokeWidth = 5.dp
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = "$selectedScore / 10",
                        style = SeriesTypography.display.copy(fontSize = 32.sp),
                        fontWeight = FontWeight.Black,
                        color = QYellowDark
                    )
                    Text(
                        text = when (selectedScore) {
                            10 -> "Masterpiece"
                            9 -> "Phenomenal"
                            8 -> "Very Good"
                            7 -> "Good"
                            6 -> "Fine"
                            5 -> "Average"
                            else -> "Could be better"
                        },
                        style = SeriesTypography.label,
                        color = QOnSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 10 Star / Step row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                (1..10).forEach { starIndex ->
                    val isFilled = starIndex <= selectedScore
                    IconButton(
                        onClick = { selectedScore = starIndex },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = if (isFilled) Icons.Default.Star else Icons.Outlined.StarOutline,
                            contentDescription = "Rate $starIndex",
                            tint = if (isFilled) QYellowPrimary else QOutline,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            YellowPillButton(
                text = "Submit Rating",
                onClick = {
                    onSubmitRating(selectedScore)
                    onDismiss()
                },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

/**
 * Premium Golden Cinema Series Comments Component
 */
@Composable
fun SeriesCommentsSection(
    comments: List<SeriesComment>,
    isLoading: Boolean,
    currentUserId: String?,
    commentInput: String,
    replyingTo: SeriesComment?,
    editingComment: SeriesComment?,
    seriesCreatorId: String? = null,
    onInputChange: (String) -> Unit,
    onPostComment: () -> Unit,
    onReply: (SeriesComment) -> Unit,
    onEdit: (SeriesComment) -> Unit,
    onDelete: (SeriesComment) -> Unit,
    onToggleLike: (SeriesComment) -> Unit,
    onCancelReplyOrEdit: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedSort by remember { mutableStateOf(CommentSortOption.TOP) }

    // Sort top-level comments
    val sortedTopLevelComments = remember(comments, selectedSort) {
        val top = comments.filter { it.parentId == null }
        when (selectedSort) {
            CommentSortOption.TOP -> top.sortedByDescending { it.likesCount }
            CommentSortOption.NEWEST -> top.sortedByDescending { it.createdAt ?: "" }
            CommentSortOption.OLDEST -> top.sortedBy { it.createdAt ?: "" }
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        // Header with count and sort chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${comments.size} Comments",
                style = SeriesTypography.headline.copy(fontSize = 17.sp),
                color = QOnSurface
            )

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                CommentSortOption.values().forEach { opt ->
                    val isSelected = selectedSort == opt
                    Surface(
                        onClick = { selectedSort = opt },
                        shape = SeriesTokens.shapePill,
                        color = if (isSelected) QYellowPrimary else QSurfaceVariant,
                        border = BorderStroke(0.5.dp, if (isSelected) QYellowPrimary else QCardBorder),
                        modifier = Modifier.pressScale(onClick = { selectedSort = opt })
                    ) {
                        Text(
                            text = opt.label,
                            style = SeriesTypography.micro,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) QOnYellow else QOnSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }

        // Comments List
        if (isLoading) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                repeat(3) {
                    CommentSkeletonRow()
                }
            }
        } else if (comments.isEmpty()) {
            SeriesEmptyState(
                icon = Icons.Outlined.ChatBubbleOutline,
                title = "No comments yet",
                description = "Be the first to share your thoughts on this episode!",
                modifier = Modifier.padding(vertical = 24.dp)
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                sortedTopLevelComments.forEach { comment ->
                    SingleCommentThread(
                        comment = comment,
                        allComments = comments,
                        currentUserId = currentUserId,
                        seriesCreatorId = seriesCreatorId,
                        onReply = onReply,
                        onEdit = onEdit,
                        onDelete = onDelete,
                        onToggleLike = onToggleLike
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Pinned Composer Bar at bottom
        CommentComposerBar(
            commentInput = commentInput,
            replyingTo = replyingTo,
            editingComment = editingComment,
            onInputChange = onInputChange,
            onPostComment = onPostComment,
            onCancelReplyOrEdit = onCancelReplyOrEdit
        )
    }
}

/**
 * Single Comment Thread with 1-level deep child replies and connector line
 */
@Composable
private fun SingleCommentThread(
    comment: SeriesComment,
    allComments: List<SeriesComment>,
    currentUserId: String?,
    seriesCreatorId: String?,
    onReply: (SeriesComment) -> Unit,
    onEdit: (SeriesComment) -> Unit,
    onDelete: (SeriesComment) -> Unit,
    onToggleLike: (SeriesComment) -> Unit
) {
    val replies = remember(allComments, comment.id) {
        allComments.filter { it.parentId == comment.id }.sortedBy { it.createdAt ?: "" }
    }
    var showReplies by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth()) {
        CommentBubble(
            comment = comment,
            currentUserId = currentUserId,
            seriesCreatorId = seriesCreatorId,
            onReply = { onReply(comment) },
            onEdit = { onEdit(comment) },
            onDelete = { onDelete(comment) },
            onToggleLike = { onToggleLike(comment) }
        )

        // View N replies toggle
        if (replies.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .padding(start = 48.dp, top = 6.dp)
                    .clip(SeriesTokens.shapePill)
                    .clickable { showReplies = !showReplies }
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .width(16.dp)
                        .height(1.5.dp)
                        .background(QYellowDark)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (showReplies) "Hide replies" else "View ${replies.size} ${if (replies.size == 1) "reply" else "replies"}",
                    style = SeriesTypography.micro,
                    fontWeight = FontWeight.Bold,
                    color = QYellowDark
                )
            }

            // Expanded Child Replies
            AnimatedVisibility(visible = showReplies) {
                Column(
                    modifier = Modifier
                        .padding(start = 32.dp, top = 8.dp)
                ) {
                    replies.forEach { reply ->
                        Row(modifier = Modifier.fillMaxWidth()) {
                            // Thin yellow left connector line
                            Box(
                                modifier = Modifier
                                    .width(2.dp)
                                    .fillMaxHeight()
                                    .background(QYellowPrimary.copy(alpha = 0.4f))
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            CommentBubble(
                                comment = reply,
                                currentUserId = currentUserId,
                                seriesCreatorId = seriesCreatorId,
                                isReply = true,
                                onReply = { onReply(comment) },
                                onEdit = { onEdit(reply) },
                                onDelete = { onDelete(reply) },
                                onToggleLike = { onToggleLike(reply) }
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}

/**
 * Comment Bubble item
 */
@Composable
private fun CommentBubble(
    comment: SeriesComment,
    currentUserId: String?,
    seriesCreatorId: String?,
    isReply: Boolean = false,
    onReply: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onToggleLike: () -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    val isAuthor = comment.userId == seriesCreatorId
    val isMyComment = comment.userId == currentUserId

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        // User Avatar
        val avatarUrl = comment.profile?.avatarUrl
        if (!avatarUrl.isNullOrBlank()) {
            AsyncImage(
                model = avatarUrl,
                contentDescription = "Avatar",
                modifier = Modifier
                    .size(if (isReply) 28.dp else 36.dp)
                    .clip(CircleShape)
            )
        } else {
            Surface(
                shape = CircleShape,
                color = QYellowLight,
                border = BorderStroke(1.dp, QYellowPrimary.copy(alpha = 0.5f)),
                modifier = Modifier.size(if (isReply) 28.dp else 36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    val initial = (comment.profile?.username ?: comment.userId).take(1).uppercase()
                    Text(
                        text = initial.ifEmpty { "U" },
                        style = SeriesTypography.label,
                        fontWeight = FontWeight.Bold,
                        color = QYellowDark
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            // Name + Badges + Time + Overflow menu
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = comment.profile?.username ?: comment.userId.take(8),
                        style = SeriesTypography.label,
                        fontWeight = FontWeight.Bold,
                        color = QOnSurface
                    )

                    if (isAuthor) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = QYellowDark
                        ) {
                            Text(
                                text = "Author",
                                style = SeriesTypography.micro.copy(fontSize = 9.sp),
                                fontWeight = FontWeight.Bold,
                                color = Color.Black,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }

                    if (!comment.createdAt.isNullOrBlank()) {
                        Text(
                            text = "• ${comment.createdAt.take(10)}",
                            style = SeriesTypography.micro,
                            color = QOnSurfaceVariant
                        )
                    }
                }

                // Overflow Menu
                if (isMyComment) {
                    Box {
                        IconButton(
                            onClick = { showMenu = true },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Options", tint = QOnSurfaceVariant, modifier = Modifier.size(16.dp))
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Edit") },
                                onClick = {
                                    showMenu = false
                                    onEdit()
                                },
                                leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, tint = QOnSurface) }
                            )
                            DropdownMenuItem(
                                text = { Text("Delete", color = QLikeRed) },
                                onClick = {
                                    showMenu = false
                                    onDelete()
                                },
                                leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = QLikeRed) }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(2.dp))

            // Comment text with expand
            Text(
                text = comment.content,
                style = SeriesTypography.body,
                color = QOnSurface,
                maxLines = if (isExpanded) Int.MAX_VALUE else 3,
                overflow = TextOverflow.Ellipsis
            )

            if (comment.content.length > 180) {
                Text(
                    text = if (isExpanded) "Show less" else "Read more",
                    style = SeriesTypography.micro,
                    fontWeight = FontWeight.Bold,
                    color = QYellowDark,
                    modifier = Modifier
                        .clickable { isExpanded = !isExpanded }
                        .padding(vertical = 2.dp)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Actions: HeartBurst Like + Reply button
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(SeriesTokens.shapePill)
                        .clickable { onToggleLike() }
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Icon(
                        imageVector = if (comment.isLiked) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = "Like",
                        tint = if (comment.isLiked) QLikeRed else QOnSurfaceVariant,
                        modifier = Modifier.size(14.dp)
                    )
                    if (comment.likesCount > 0) {
                        Spacer(modifier = Modifier.width(4.dp))
                        AnimatedCount(
                            count = comment.likesCount,
                            color = if (comment.isLiked) QLikeRed else QOnSurfaceVariant,
                            style = SeriesTypography.micro
                        )
                    }
                }

                Text(
                    text = "Reply",
                    style = SeriesTypography.micro,
                    fontWeight = FontWeight.Bold,
                    color = QOnSurfaceVariant,
                    modifier = Modifier
                        .clip(SeriesTokens.shapePill)
                        .clickable { onReply() }
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
    }
}

/**
 * Pinned Composer Bar
 */
@Composable
private fun CommentComposerBar(
    commentInput: String,
    replyingTo: SeriesComment?,
    editingComment: SeriesComment?,
    onInputChange: (String) -> Unit,
    onPostComment: () -> Unit,
    onCancelReplyOrEdit: () -> Unit
) {
    val hasText = commentInput.trim().isNotEmpty()

    Surface(
        color = QSurface,
        border = BorderStroke(1.dp, QOutline),
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        shadowElevation = 4.dp,
        modifier = Modifier
            .fillMaxWidth()
            .imePadding()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Reply / Edit Banner
            if (replyingTo != null || editingComment != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (editingComment != null) "Editing comment" else "Replying to @${replyingTo?.profile?.username ?: "user"}",
                        style = SeriesTypography.micro,
                        fontWeight = FontWeight.Bold,
                        color = QYellowDark
                    )
                    IconButton(
                        onClick = onCancelReplyOrEdit,
                        modifier = Modifier.size(20.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Cancel", tint = QOnSurfaceVariant, modifier = Modifier.size(14.dp))
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = commentInput,
                    onValueChange = onInputChange,
                    placeholder = {
                        Text(
                            text = if (replyingTo != null) "Write a reply..." else "Add a comment...",
                            style = SeriesTypography.body,
                            color = QOnSurfaceVariant
                        )
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = QYellowPrimary,
                        unfocusedBorderColor = QOutline,
                        focusedContainerColor = QSurfaceVariant,
                        unfocusedContainerColor = QSurfaceVariant
                    ),
                    shape = SeriesTokens.shapePill,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("series_comment_input")
                )

                Spacer(modifier = Modifier.width(8.dp))

                val buttonColor by animateColorAsState(
                    targetValue = if (hasText) QYellowPrimary else QSurfaceVariant,
                    label = "sendButtonColor"
                )
                val iconColor by animateColorAsState(
                    targetValue = if (hasText) QOnYellow else QOnSurfaceVariant,
                    label = "sendIconColor"
                )

                IconButton(
                    onClick = onPostComment,
                    enabled = hasText,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(buttonColor)
                        .testTag("series_comment_send")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send",
                        tint = iconColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

/**
 * Skeleton Loader Row for Comments
 */
@Composable
private fun CommentSkeletonRow() {
    Row(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .shimmerEffect()
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Box(
                modifier = Modifier
                    .width(100.dp)
                    .height(14.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .shimmerEffect()
            )
            Spacer(modifier = Modifier.height(6.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .height(12.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .shimmerEffect()
            )
            Spacer(modifier = Modifier.height(4.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.6f)
                    .height(12.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .shimmerEffect()
            )
        }
    }
}
