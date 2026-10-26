package com.example.ui.components

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.ui.theme.QCardBorder
import com.example.ui.theme.QOnSurface
import com.example.ui.theme.QOnSurfaceVariant
import com.example.ui.theme.QOnYellow
import com.example.ui.theme.QSurface
import com.example.ui.theme.QSurfaceVariant
import com.example.ui.theme.QYellowDark
import com.example.ui.theme.QYellowPrimary
import com.example.util.ImgBbUploader
import kotlinx.coroutines.launch

@Composable
fun ImgBbUrlInputField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String? = "https://i.ibb.co/...",
    previewAspectRatio: Float? = null,
    previewMaxHeight: Dp = 160.dp,
    testTag: String = "imgbb_url_input"
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var isUploading by remember { mutableStateOf(false) }
    var uploadError by remember { mutableStateOf<String?>(null) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            isUploading = true
            uploadError = null
            coroutineScope.launch {
                val result = ImgBbUploader.uploadImage(context, uri)
                isUploading = false
                result.onSuccess { url ->
                    onValueChange(url)
                    Toast.makeText(context, "Image uploaded to ImgBB!", Toast.LENGTH_SHORT).show()
                }.onFailure { err ->
                    uploadError = err.message ?: "ImgBB upload failed"
                    Toast.makeText(context, "Upload failed: ${err.localizedMessage}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = {
                uploadError = null
                onValueChange(it)
            },
            label = { Text(label) },
            placeholder = placeholder?.let { { Text(it) } },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag(testTag),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = QYellowPrimary,
                unfocusedBorderColor = QCardBorder,
                focusedLabelColor = QYellowPrimary
            ),
            trailingIcon = {
                if (isUploading) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = QYellowPrimary,
                            strokeWidth = 2.5.dp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Uploading...", fontSize = 11.sp, color = QYellowDark, fontWeight = FontWeight.Bold)
                    }
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (value.isNotBlank()) {
                            IconButton(
                                onClick = { onValueChange("") },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Clear URL",
                                    tint = QOnSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        IconButton(
                            onClick = {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            modifier = Modifier
                                .padding(end = 4.dp)
                                .size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudUpload,
                                contentDescription = "Upload Image to ImgBB",
                                tint = QYellowPrimary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            }
        )

        // Error message callout
        if (uploadError != null) {
            Text(
                text = "⚠️ $uploadError",
                color = Color(0xFFFF5252),
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(start = 4.dp)
            )
        }

        // Live Preview Card
        AnimatedVisibility(
            visible = value.trim().isNotBlank(),
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            val previewUrl = value.trim()

            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = QSurface),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, QCardBorder, RoundedCornerShape(12.dp))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                ) {
                    val cardModifier = if (previewAspectRatio != null && previewAspectRatio > 0f) {
                        Modifier
                            .fillMaxWidth()
                            .aspectRatio(previewAspectRatio)
                            .clip(RoundedCornerShape(8.dp))
                            .background(QSurfaceVariant)
                    } else {
                        Modifier
                            .fillMaxWidth()
                            .height(previewMaxHeight)
                            .clip(RoundedCornerShape(8.dp))
                            .background(QSurfaceVariant)
                    }

                    AsyncImage(
                        model = previewUrl,
                        contentDescription = "Image Live Preview",
                        contentScale = ContentScale.Crop,
                        modifier = cardModifier
                    )

                    // Top-Left Source Badge
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(8.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color.Black.copy(alpha = 0.75f))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Image,
                                contentDescription = null,
                                tint = QYellowPrimary,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (previewUrl.contains("ibb.co")) "ImgBB Direct Link" else "Image Preview",
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Top-Right Clear / Remove Button
                    IconButton(
                        onClick = { onValueChange("") },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(6.dp)
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.65f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Remove Image",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}
