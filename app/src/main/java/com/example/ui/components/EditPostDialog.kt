package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Post
import com.example.ui.theme.QCardBorder
import com.example.ui.theme.QOnSurface
import com.example.ui.theme.QOnSurfaceVariant
import com.example.ui.theme.QOnYellow
import com.example.ui.theme.QSurface
import com.example.ui.theme.QYellowDark
import com.example.ui.theme.QYellowPrimary

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EditPostBottomSheet(
    post: Post,
    onDismiss: () -> Unit,
    onSaveEdit: (newContent: String, newTitle: String?) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var titleText by remember { mutableStateOf(post.title ?: "") }
    var contentText by remember { mutableStateOf(post.content) }
    var selectedType by remember { mutableStateOf(post.type ?: "Post") }
    var isSubmitting by remember { mutableStateOf(false) }

    val allowedTypes = listOf(
        "Post", "Tip", "Trick", "Idea", "Tutorial", "Project",
        "Update", "Resource", "Question", "Discovery", "Learning", "Code", "AI Prompt"
    )

    val isTitleValid = titleText.length <= 150
    val isContentValid = contentText.isNotBlank() && contentText.length <= 5000
    val isChanged = titleText.trim() != (post.title ?: "") || contentText.trim() != post.content || selectedType != (post.type ?: "Post")
    val canSave = isTitleValid && isContentValid && isChanged && !isSubmitting

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = QSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 10.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit Post",
                        tint = QYellowDark
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Edit Post (${post.type ?: "Post"})",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = QOnSurface
                    )
                }

                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = QOnSurface)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Post Type Chip Selector
            Text(
                text = "Post Category",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = QOnSurfaceVariant
            )
            Spacer(modifier = Modifier.height(6.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                allowedTypes.forEach { typeOption ->
                    FilterChip(
                        selected = selectedType == typeOption,
                        onClick = { selectedType = typeOption },
                        label = { Text(typeOption, fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = QYellowPrimary,
                            selectedLabelColor = QOnYellow
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Title input
            OutlinedTextField(
                value = titleText,
                onValueChange = { if (it.length <= 150) titleText = it },
                label = { Text("Title (Max 150 chars)", color = QOnSurfaceVariant) },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = QYellowPrimary,
                    unfocusedBorderColor = QCardBorder
                ),
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = "${titleText.length}/150",
                fontSize = 10.sp,
                color = if (isTitleValid) QOnSurfaceVariant else androidx.compose.ui.graphics.Color.Red,
                modifier = Modifier.align(Alignment.End).padding(top = 2.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Content input
            OutlinedTextField(
                value = contentText,
                onValueChange = { if (it.length <= 5000) contentText = it },
                label = { Text("Post Content (Max 5000 chars)", color = QOnSurfaceVariant) },
                minLines = 4,
                maxLines = 10,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = QYellowPrimary,
                    unfocusedBorderColor = QCardBorder
                ),
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = "${contentText.length}/5000",
                fontSize = 10.sp,
                color = if (isContentValid) QOnSurfaceVariant else androidx.compose.ui.graphics.Color.Red,
                modifier = Modifier.align(Alignment.End).padding(top = 2.dp)
            )

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
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
                        if (canSave) {
                            isSubmitting = true
                            onSaveEdit(contentText.trim(), titleText.trim().ifBlank { null })
                        }
                    },
                    enabled = canSave,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = QYellowPrimary,
                        contentColor = QOnYellow
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    if (isSubmitting) {
                        CircularProgressIndicator(color = QOnYellow, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                    } else {
                        Text("Save Changes", fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}
