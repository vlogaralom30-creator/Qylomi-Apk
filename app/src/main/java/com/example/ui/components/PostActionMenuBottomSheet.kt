package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Comment
import androidx.compose.material.icons.filled.CommentsDisabled
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PersonRemove
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Post
import com.example.data.model.UserProfile
import com.example.ui.theme.QCardBorder
import com.example.ui.theme.QLikeRed
import com.example.ui.theme.QOnSurface
import com.example.ui.theme.QOnSurfaceVariant
import com.example.ui.theme.QOnYellow
import com.example.ui.theme.QSurface
import com.example.ui.theme.QYellowDark
import com.example.ui.theme.QYellowPrimary
import com.example.util.Constants

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostActionMenuBottomSheet(
    post: Post,
    currentUserId: String?,
    isAuthorFollowing: Boolean,
    onDismiss: () -> Unit,
    onEditClick: (Post) -> Unit,
    onDeleteClick: (Post) -> Unit,
    onTogglePin: (Post) -> Unit,
    onToggleAllowComments: (Post) -> Unit,
    onToggleBookmark: (Post) -> Unit,
    onToggleFollow: (UserProfile) -> Unit,
    onViewProfile: (UserProfile) -> Unit,
    onHidePost: (Post) -> Unit,
    onReportPost: (Post, reason: String, details: String?) -> Unit,
    onShowBanner: (String) -> Unit
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val isOwner = currentUserId != null && currentUserId == post.userId
    val author = post.authorProfile

    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showReportSheet by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = QSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(vertical = 8.dp)
        ) {
            // Drag Indicator & Header
            Text(
                text = "Post Options",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = QOnSurface,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
            )

            HorizontalDivider(color = QCardBorder, modifier = Modifier.padding(vertical = 4.dp))

            if (isOwner) {
                // OWNER OPTIONS
                ActionMenuItem(
                    icon = Icons.Default.Edit,
                    label = "Edit Post",
                    onClick = {
                        onDismiss()
                        onEditClick(post)
                    }
                )

                ActionMenuItem(
                    icon = Icons.Default.PushPin,
                    label = if (post.isPinned) "Unpin from profile" else "Pin to profile",
                    onClick = {
                        onDismiss()
                        onTogglePin(post)
                    }
                )

                ActionMenuItem(
                    icon = if (post.allowComments) Icons.Default.CommentsDisabled else Icons.Default.Comment,
                    label = if (post.allowComments) "Turn comments off" else "Turn comments on",
                    onClick = {
                        onDismiss()
                        onToggleAllowComments(post)
                    }
                )

                ActionMenuItem(
                    icon = Icons.Default.ContentCopy,
                    label = "Copy link",
                    onClick = {
                        val link = Constants.buildPostWebUrl(post.id)
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("Post Link", link))
                        onDismiss()
                        onShowBanner("Link copied to clipboard!")
                    }
                )

                ActionMenuItem(
                    icon = Icons.Default.Share,
                    label = "Share post",
                    onClick = {
                        val shareText = Constants.buildShareText(
                            title = post.title,
                            content = post.content,
                            postId = post.id,
                            originalVideoUrl = post.videoDetails?.videoUrl ?: post.externalUrl
                        )
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, shareText)
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Share post"))
                        onDismiss()
                    }
                )

                ActionMenuItem(
                    icon = Icons.Default.Delete,
                    label = "Delete post",
                    textColor = QLikeRed,
                    iconTint = QLikeRed,
                    onClick = {
                        showDeleteConfirmDialog = true
                    }
                )
            } else {
                // OTHER USER OPTIONS
                ActionMenuItem(
                    icon = if (post.isBookmarkedByMe) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                    label = if (post.isBookmarkedByMe) "Remove from Bookmarks" else "Save to Bookmarks",
                    iconTint = if (post.isBookmarkedByMe) QYellowPrimary else QOnSurfaceVariant,
                    onClick = {
                        onDismiss()
                        onToggleBookmark(post)
                    }
                )

                author?.let { user ->
                    ActionMenuItem(
                        icon = if (isAuthorFollowing) Icons.Default.PersonRemove else Icons.Default.PersonAdd,
                        label = if (isAuthorFollowing) "Unfollow @${user.effectiveUsername}" else "Follow @${user.effectiveUsername}",
                        onClick = {
                            onDismiss()
                            onToggleFollow(user)
                        }
                    )

                    ActionMenuItem(
                        icon = Icons.Default.Person,
                        label = "View profile (@${user.effectiveUsername})",
                        onClick = {
                            onDismiss()
                            onViewProfile(user)
                        }
                    )
                }

                ActionMenuItem(
                    icon = Icons.Default.ContentCopy,
                    label = "Copy link",
                    onClick = {
                        val link = Constants.buildPostWebUrl(post.id)
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("Post Link", link))
                        onDismiss()
                        onShowBanner("Link copied to clipboard!")
                    }
                )

                ActionMenuItem(
                    icon = Icons.Default.Share,
                    label = "Share post",
                    onClick = {
                        val shareText = Constants.buildShareText(
                            title = post.title,
                            content = post.content,
                            postId = post.id,
                            originalVideoUrl = post.videoDetails?.videoUrl ?: post.externalUrl
                        )
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, shareText)
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Share post"))
                        onDismiss()
                    }
                )

                ActionMenuItem(
                    icon = Icons.Default.VisibilityOff,
                    label = "Hide this post",
                    onClick = {
                        onDismiss()
                        onHidePost(post)
                    }
                )

                ActionMenuItem(
                    icon = Icons.Default.Flag,
                    label = "Report post",
                    textColor = QLikeRed,
                    iconTint = QLikeRed,
                    onClick = {
                        showReportSheet = true
                    }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
        }
    }

    // Delete Confirmation Dialog
    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("Delete this post?", fontWeight = FontWeight.Bold) },
            text = { Text("This post will be permanently removed. This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirmDialog = false
                        onDismiss()
                        onDeleteClick(post)
                    }
                ) {
                    Text("Delete", color = QLikeRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("Cancel", color = QOnSurfaceVariant)
                }
            },
            containerColor = QSurface
        )
    }

    // Report Dialog / Sheet
    if (showReportSheet) {
        ReportPostBottomSheet(
            onDismiss = { showReportSheet = false },
            onSubmitReport = { reason, details ->
                showReportSheet = false
                onDismiss()
                onReportPost(post, reason, details)
            }
        )
    }
}

@Composable
private fun ActionMenuItem(
    icon: ImageVector,
    label: String,
    textColor: Color = QOnSurface,
    iconTint: Color = QOnSurfaceVariant,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = label,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = textColor
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ReportPostBottomSheet(
    onDismiss: () -> Unit,
    onSubmitReport: (reason: String, details: String?) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val reasons = listOf("Spam", "Harassment", "Hate speech", "Misinformation", "Sexual content", "Violence", "Other")
    var selectedReason by remember { mutableStateOf(reasons.first()) }
    var detailsText by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = QSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 10.dp)
        ) {
            Text(
                text = "Report Post",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = QOnSurface
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Why are you reporting this post?",
                fontSize = 13.sp,
                color = QOnSurfaceVariant
            )

            Spacer(modifier = Modifier.height(14.dp))

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                reasons.forEach { reason ->
                    FilterChip(
                        selected = selectedReason == reason,
                        onClick = { selectedReason = reason },
                        label = { Text(reason, fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = QYellowPrimary,
                            selectedLabelColor = QOnYellow
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            OutlinedTextField(
                value = detailsText,
                onValueChange = { detailsText = it },
                label = { Text("Additional details (optional)", color = QOnSurfaceVariant) },
                minLines = 2,
                maxLines = 4,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = QYellowPrimary,
                    unfocusedBorderColor = QCardBorder
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Cancel")
                }

                Button(
                    onClick = {
                        onSubmitReport(selectedReason, detailsText.trim().ifBlank { null })
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = QLikeRed,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Submit Report", fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
