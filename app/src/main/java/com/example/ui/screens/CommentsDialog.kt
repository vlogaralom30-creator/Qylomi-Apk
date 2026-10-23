package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Comment
import com.example.data.model.Post
import com.example.data.model.UserProfile
import com.example.ui.components.UserAvatar
import com.example.ui.components.formatTimeAgo
import com.example.ui.theme.QCardBorder
import com.example.ui.theme.QLikeRed
import com.example.ui.theme.QOnSurface
import com.example.ui.theme.QOnSurfaceVariant
import com.example.ui.theme.QOnYellow
import com.example.ui.theme.QSurface
import com.example.ui.theme.QSurfaceVariant
import com.example.ui.theme.QYellowDark
import com.example.ui.theme.QYellowLight
import com.example.ui.theme.QYellowPrimary
import com.example.ui.viewmodel.CommentSortOrder

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommentsBottomSheet(
    post: Post,
    comments: List<Comment>,
    isLoading: Boolean,
    sortOrder: CommentSortOrder = CommentSortOrder.NEWEST,
    replyToComment: Comment? = null,
    editingComment: Comment? = null,
    currentUser: UserProfile?,
    onDismiss: () -> Unit,
    onSortSelected: (CommentSortOrder) -> Unit,
    onReplyTo: (Comment?) -> Unit,
    onStartEditing: (Comment?) -> Unit,
    onAddComment: (String) -> Unit,
    onUpdateComment: (Comment, String) -> Unit,
    onDeleteComment: (Comment) -> Unit,
    onToggleCommentLike: (Comment) -> Unit,
    onOpenCommentLikers: (Comment) -> Unit,
    onReportComment: (Comment) -> Unit,
    onAuthorClick: (UserProfile) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var inputText by remember { mutableStateOf("") }
    var expandedParentIds by remember { mutableStateOf(setOf<String>()) }
    var commentToDelete by remember { mutableStateOf<Comment?>(null) }

    // Synchronize edit text if editing
    var editInputText by remember(editingComment) {
        mutableStateOf(editingComment?.content ?: "")
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = QSurface,
        modifier = Modifier.fillMaxHeight(0.90f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "Comments (${comments.size})",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = QOnSurface,
                    modifier = Modifier.weight(1f)
                )

                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }

            // Sort Filter Chips (Newest, Top, Oldest)
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 2.dp)
            ) {
                FilterChip(
                    selected = sortOrder == CommentSortOrder.NEWEST,
                    onClick = { onSortSelected(CommentSortOrder.NEWEST) },
                    label = { Text("Newest", fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = QYellowPrimary,
                        selectedLabelColor = QOnYellow
                    )
                )
                FilterChip(
                    selected = sortOrder == CommentSortOrder.TOP,
                    onClick = { onSortSelected(CommentSortOrder.TOP) },
                    label = { Text("Top", fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = QYellowPrimary,
                        selectedLabelColor = QOnYellow
                    )
                )
                FilterChip(
                    selected = sortOrder == CommentSortOrder.OLDEST,
                    onClick = { onSortSelected(CommentSortOrder.OLDEST) },
                    label = { Text("Oldest", fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = QYellowPrimary,
                        selectedLabelColor = QOnYellow
                    )
                )
            }

            HorizontalDivider(color = QCardBorder)

            // Comments List Body
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                if (isLoading && comments.isEmpty()) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(40.dp)
                    ) {
                        CircularProgressIndicator(color = QYellowPrimary)
                    }
                } else if (comments.isEmpty()) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(40.dp)
                    ) {
                        Text(
                            text = if (post.allowComments) "No comments yet.\nBe the first to share your thoughts!" else "Comments are turned off for this post.",
                            color = QOnSurfaceVariant,
                            fontSize = 14.sp,
                            lineHeight = 20.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                } else {
                    val topLevelComments = comments.filter { it.parentId == null }
                    val repliesMap = comments.filter { it.parentId != null }.groupBy { it.parentId }

                    LazyColumn(modifier = Modifier.fillMaxWidth()) {
                        items(topLevelComments, key = { it.id }) { parentComment ->
                            val childReplies = repliesMap[parentComment.id] ?: emptyList()
                            val isExpanded = expandedParentIds.contains(parentComment.id)

                            Column(modifier = Modifier.fillMaxWidth()) {
                                // Parent Comment Row
                                CommentRow(
                                    comment = parentComment,
                                    postOwnerId = post.userId,
                                    currentUserId = currentUser?.id,
                                    isReply = false,
                                    onReplyClick = { onReplyTo(parentComment) },
                                    onEditClick = { onStartEditing(parentComment) },
                                    onDeleteClick = { commentToDelete = parentComment },
                                    onToggleLike = { onToggleCommentLike(parentComment) },
                                    onLikersClick = { onOpenCommentLikers(parentComment) },
                                    onReportClick = { onReportComment(parentComment) },
                                    onAuthorClick = onAuthorClick
                                )

                                // Replies Count Toggle Button
                                if (childReplies.isNotEmpty()) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .padding(start = 58.dp, bottom = 6.dp)
                                            .clickable {
                                                expandedParentIds = if (isExpanded) {
                                                    expandedParentIds - parentComment.id
                                                } else {
                                                    expandedParentIds + parentComment.id
                                                }
                                            }
                                            .padding(vertical = 4.dp, horizontal = 6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.Reply,
                                            contentDescription = null,
                                            tint = QYellowDark,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = if (isExpanded) "Hide replies" else "View ${childReplies.size} ${if (childReplies.size == 1) "reply" else "replies"}",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = QYellowDark
                                        )
                                    }

                                    // Render Expanded Replies
                                    if (isExpanded) {
                                        childReplies.forEach { reply ->
                                            CommentRow(
                                                comment = reply,
                                                postOwnerId = post.userId,
                                                currentUserId = currentUser?.id,
                                                isReply = true,
                                                onReplyClick = { onReplyTo(parentComment) },
                                                onEditClick = { onStartEditing(reply) },
                                                onDeleteClick = { commentToDelete = reply },
                                                onToggleLike = { onToggleCommentLike(reply) },
                                                onLikersClick = { onOpenCommentLikers(reply) },
                                                onReportClick = { onReportComment(reply) },
                                                onAuthorClick = onAuthorClick
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            HorizontalDivider(color = QCardBorder)

            // Input Bar / Disabled Banner
            if (!post.allowComments) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(QSurfaceVariant)
                        .padding(14.dp)
                ) {
                    Text(
                        text = "Comments are turned off by the author.",
                        color = QOnSurfaceVariant,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            } else if (editingComment != null) {
                // Editing Comment Input State
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(QYellowLight.copy(alpha = 0.5f))
                        .padding(10.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Editing comment",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = QYellowDark
                        )
                        IconButton(
                            onClick = { onStartEditing(null) },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Cancel Edit", tint = QOnSurfaceVariant)
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = editInputText,
                            onValueChange = { if (it.length <= 3000) editInputText = it },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = QYellowPrimary,
                                unfocusedBorderColor = QCardBorder
                            ),
                            maxLines = 4,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(
                            onClick = {
                                if (editInputText.isNotBlank()) {
                                    onUpdateComment(editingComment, editInputText.trim())
                                }
                            },
                            enabled = editInputText.isNotBlank(),
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(if (editInputText.isNotBlank()) QYellowPrimary else Color(0xFFE2E8F0))
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Save Edit",
                                tint = if (editInputText.isNotBlank()) QOnYellow else Color(0xFF94A3B8)
                            )
                        }
                    }
                }
            } else {
                // Normal Comment Input Bar
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Replying To Header Banner
                    AnimatedVisibility(visible = replyToComment != null) {
                        replyToComment?.let { replyTarget ->
                            val targetAuthor = replyTarget.authorProfile?.effectiveUsername ?: "user"
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(QYellowLight)
                                    .padding(horizontal = 14.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = "Replying to @$targetAuthor",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = QYellowDark
                                )
                                IconButton(
                                    onClick = { onReplyTo(null) },
                                    modifier = Modifier.size(20.dp)
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "Cancel reply", tint = QYellowDark)
                                }
                            }
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        UserAvatar(
                            avatarUrl = currentUser?.avatarUrl,
                            initials = currentUser?.initials ?: "Q",
                            size = 36.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            OutlinedTextField(
                                value = inputText,
                                onValueChange = { if (it.length <= 3000) inputText = it },
                                placeholder = {
                                    val holder = replyToComment?.let { "Reply to @${it.authorProfile?.effectiveUsername}..." } ?: "Add a comment..."
                                    Text(holder, fontSize = 13.sp)
                                },
                                maxLines = 4,
                                shape = RoundedCornerShape(18.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = QYellowPrimary,
                                    unfocusedBorderColor = QCardBorder
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("comment_input_field")
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(
                            onClick = {
                                if (inputText.isNotBlank()) {
                                    onAddComment(inputText.trim())
                                    inputText = ""
                                }
                            },
                            enabled = inputText.isNotBlank(),
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(if (inputText.isNotBlank()) QYellowPrimary else Color(0xFFE2E8F0))
                                .testTag("send_comment_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Send",
                                tint = if (inputText.isNotBlank()) QOnYellow else Color(0xFF94A3B8),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    // Comment Delete Confirmation Dialog
    if (commentToDelete != null) {
        AlertDialog(
            onDismissRequest = { commentToDelete = null },
            title = { Text("Delete Comment?", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to delete this comment?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        val target = commentToDelete!!
                        commentToDelete = null
                        onDeleteComment(target)
                    }
                ) {
                    Text("Delete", color = QLikeRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { commentToDelete = null }) {
                    Text("Cancel", color = QOnSurfaceVariant)
                }
            },
            containerColor = QSurface
        )
    }
}

@Composable
private fun CommentRow(
    comment: Comment,
    postOwnerId: String,
    currentUserId: String?,
    isReply: Boolean,
    onReplyClick: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onToggleLike: () -> Unit,
    onLikersClick: () -> Unit,
    onReportClick: () -> Unit,
    onAuthorClick: (UserProfile) -> Unit
) {
    val author = comment.authorProfile ?: UserProfile(
        id = comment.userId,
        displayName = "Creator",
        username = "creator"
    )

    val isCommentOwner = currentUserId != null && currentUserId == comment.userId
    val isPostOwner = currentUserId != null && currentUserId == postOwnerId
    val canDelete = isCommentOwner || isPostOwner
    val isPostAuthorComment = author.id == postOwnerId

    var showMenu by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = if (isReply) 52.dp else 16.dp,
                end = 16.dp,
                top = 8.dp,
                bottom = 8.dp
            )
    ) {
        UserAvatar(
            avatarUrl = author.avatarUrl,
            initials = author.initials,
            size = if (isReply) 30.dp else 36.dp,
            modifier = Modifier.clickable { onAuthorClick(author) }
        )

        Spacer(modifier = Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = author.effectiveDisplayName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = QOnSurface,
                        modifier = Modifier.clickable { onAuthorClick(author) }
                    )

                    if (isPostAuthorComment) {
                        Spacer(modifier = Modifier.width(5.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(QYellowLight)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "Author",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = QYellowDark
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "• ${formatTimeAgo(comment.createdAt)}${if (comment.isEdited) " (edited)" else ""}",
                        fontSize = 11.sp,
                        color = QOnSurfaceVariant
                    )
                }

                // Options Menu (3-dot)
                Box {
                    IconButton(
                        onClick = { showMenu = true },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Options",
                            tint = QOnSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Reply") },
                            leadingIcon = { Icon(Icons.AutoMirrored.Filled.Reply, contentDescription = null) },
                            onClick = {
                                showMenu = false
                                onReplyClick()
                            }
                        )

                        if (isCommentOwner) {
                            DropdownMenuItem(
                                text = { Text("Edit") },
                                leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                                onClick = {
                                    showMenu = false
                                    onEditClick()
                                }
                            )
                        }

                        if (canDelete) {
                            DropdownMenuItem(
                                text = { Text("Delete", color = QLikeRed) },
                                leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = QLikeRed) },
                                onClick = {
                                    showMenu = false
                                    onDeleteClick()
                                }
                            )
                        }

                        if (!isCommentOwner) {
                            DropdownMenuItem(
                                text = { Text("Report") },
                                leadingIcon = { Icon(Icons.Default.Flag, contentDescription = null) },
                                onClick = {
                                    showMenu = false
                                    onReportClick()
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(3.dp))

            Text(
                text = comment.content,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                color = QOnSurface
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Reply & Like Footer Action Row
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Reply",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = QYellowDark,
                    modifier = Modifier.clickable { onReplyClick() }
                )

                Spacer(modifier = Modifier.width(16.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { onToggleLike() }
                ) {
                    Icon(
                        imageVector = if (comment.isLikedByMe) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Like comment",
                        tint = if (comment.isLikedByMe) QLikeRed else QOnSurfaceVariant,
                        modifier = Modifier.size(14.dp)
                    )
                    if (comment.likesCount > 0) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${comment.likesCount}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (comment.isLikedByMe) QLikeRed else QOnSurfaceVariant,
                            modifier = Modifier.clickable { onLikersClick() }
                        )
                    }
                }
            }
        }
    }
}
