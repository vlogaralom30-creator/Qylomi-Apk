package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Link
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.UserProfile
import com.example.ui.components.EmbeddedMediaPlayer
import com.example.ui.components.UserAvatar
import com.example.util.MediaParser
import com.example.ui.theme.QCardBorder
import com.example.ui.theme.QOnSurface
import com.example.ui.theme.QOnSurfaceVariant
import com.example.ui.theme.QOnYellow
import com.example.ui.theme.QSurface
import com.example.ui.theme.QSurfaceVariant
import com.example.ui.theme.QYellowDark
import com.example.ui.theme.QYellowLight
import com.example.ui.theme.QYellowPrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreatePostBottomSheet(
    currentUser: UserProfile?,
    onDismiss: () -> Unit,
    onPublish: (type: String, title: String?, content: String, url: String?, onDone: (Boolean, String?) -> Unit) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val types = listOf("Post", "Idea", "Tutorial", "Project", "Code", "Resource", "Question", "Tip", "Discovery")
    var selectedType by remember { mutableStateOf("Post") }
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var externalUrl by remember { mutableStateOf("") }
    var isPublishing by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = QSurface,
        modifier = Modifier.fillMaxHeight(0.9f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 6.dp)
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }

                Text(
                    text = "Create on Q",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = QOnSurface
                )

                Button(
                    onClick = {
                        if (content.isBlank()) {
                            errorMessage = "Please enter some content for your post"
                            return@Button
                        }
                        isPublishing = true
                        errorMessage = null
                        onPublish(
                            selectedType,
                            title.takeIf { it.isNotBlank() },
                            content,
                            externalUrl.takeIf { it.isNotBlank() }
                        ) { success, err ->
                            isPublishing = false
                            if (success) {
                                onDismiss()
                            } else {
                                errorMessage = err ?: "Failed to publish"
                            }
                        }
                    },
                    enabled = !isPublishing && content.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = QYellowPrimary,
                        contentColor = QOnYellow
                    ),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.testTag("publish_post_button")
                ) {
                    if (isPublishing) {
                        CircularProgressIndicator(
                            color = QOnYellow,
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Post", fontWeight = FontWeight.Bold)
                    }
                }
            }

            HorizontalDivider(color = QCardBorder, modifier = Modifier.padding(vertical = 8.dp))

            // Author info
            Row(verticalAlignment = Alignment.CenterVertically) {
                UserAvatar(
                    avatarUrl = currentUser?.avatarUrl,
                    initials = currentUser?.initials ?: "Q",
                    size = 40.dp
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = currentUser?.effectiveDisplayName ?: "Creator",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = QOnSurface
                    )
                    Text(
                        text = "@${currentUser?.effectiveUsername ?: "user"} • Public",
                        fontSize = 12.sp,
                        color = QOnSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Post Type Chips (IdeaType from Datasetuse.md)
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
            ) {
                types.forEach { type ->
                    val isSelected = selectedType == type
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) QYellowPrimary else QSurfaceVariant)
                            .clickable { selectedType = type }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = type,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) QOnYellow else QOnSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Title Field
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                placeholder = { Text("Title (e.g. \"Great AI alternatives to explore\")") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("post_title_input"),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = QYellowPrimary,
                    unfocusedBorderColor = QCardBorder
                )
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Content Field
            OutlinedTextField(
                value = content,
                onValueChange = { content = it },
                placeholder = { Text("What do you want to share with Q Creators?") },
                minLines = 5,
                maxLines = 10,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("post_content_input"),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = QYellowPrimary,
                    unfocusedBorderColor = QCardBorder
                )
            )

            Spacer(modifier = Modifier.height(10.dp))

            // External Link
            OutlinedTextField(
                value = externalUrl,
                onValueChange = { externalUrl = it },
                label = { Text("Video / Image / Embed URL (optional)") },
                placeholder = { Text("YouTube, TikTok, Drive, or image/video link") },
                leadingIcon = {
                    Icon(Icons.Default.Link, contentDescription = null, tint = QYellowDark)
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = QYellowPrimary,
                    unfocusedBorderColor = QCardBorder
                )
            )

            // Live Media Preview if valid URL
            if (externalUrl.isNotBlank()) {
                val liveParsed = remember(externalUrl) { MediaParser.parseSingleUrl(externalUrl) }
                if (liveParsed != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Live Embed Preview",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = QYellowDark
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    EmbeddedMediaPlayer(media = liveParsed)
                }
            }

            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = errorMessage ?: "",
                    color = androidx.compose.ui.graphics.Color.Red,
                    fontSize = 12.sp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Points reward banner
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(QYellowLight)
                    .border(1.dp, QYellowPrimary.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                    .padding(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = QYellowDark,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Publishing earns +50 Creator Points towards your next rank!",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = QYellowDark
                )
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}
