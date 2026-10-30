package com.example.ui.screens

import com.example.ui.components.ImgBbUrlInputField
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.NoteAdd
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch

import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.model.PixelArtData
import com.example.data.model.UserProfile
import com.example.ui.components.EmbedVideoPlayer
import com.example.ui.components.PixelArtCanvasStudio
import com.example.ui.components.PixelArtViewer
import com.example.ui.components.StyledTextPostCard
import com.example.ui.components.TextPostTemplates
import com.example.ui.components.UserAvatar
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
import com.example.util.PixelArtUtils
import com.example.util.VideoEmbedUtils

import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.NativeVideoPlayer
import com.example.ui.viewmodel.ExtractedVideoResult
import com.example.ui.viewmodel.VideoExtractionState
import com.example.ui.viewmodel.VideoExtractorViewModel

enum class PostModeTab {
    VIDEO, PHOTO, TEXT, PIXEL_ART, GENERAL
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreatePostBottomSheet(
    currentUser: UserProfile?,
    initialSharedUrl: String? = null,
    onDismiss: () -> Unit,
    onPublishGeneral: (type: String, title: String?, content: String, url: String?, onDone: (Boolean, String?) -> Unit) -> Unit,
    onPublishVideo: (
        title: String?,
        videoUrl: String,
        embedUrl: String?,
        directUrl: String?,
        platform: String?,
        thumbnailUrl: String?,
        durationSeconds: Int?,
        width: Int?,
        height: Int?,
        isVertical: Boolean?,
        category: String?,
        tags: List<String>,
        onDone: (Boolean, String?) -> Unit
    ) -> Unit,
    onPublishPhoto: (caption: String?, photoUrls: List<String>, category: String?, tags: List<String>, onDone: (Boolean, String?) -> Unit) -> Unit,
    onPublishText: (caption: String, templateId: String?, category: String?, tags: List<String>, onDone: (Boolean, String?) -> Unit) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val clipboardManager = LocalClipboardManager.current
    val videoExtractorViewModel: VideoExtractorViewModel = viewModel()
    val videoExtractionState by videoExtractorViewModel.uiState.collectAsStateWithLifecycle()

    var currentTab by remember { mutableStateOf(PostModeTab.VIDEO) }
    var isPublishing by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()


    // Video Post Form State
    var videoTitle by remember { mutableStateOf("") }
    var videoUrl by remember { mutableStateOf("") }
    var videoThumbnailUrl by remember { mutableStateOf("") }
    var videoCategory by remember { mutableStateOf("Technology") }
    var lastExtractedResult by remember { mutableStateOf<ExtractedVideoResult?>(null) }

    // Auto-populate and extract if shared from external app (TikTok, Facebook, YouTube, etc.)
    androidx.compose.runtime.LaunchedEffect(initialSharedUrl) {
        if (!initialSharedUrl.isNullOrBlank()) {
            val extractedUrl = com.example.util.SharedIntentUtils.findFirstUrl(initialSharedUrl) ?: initialSharedUrl
            if (extractedUrl.isNotBlank()) {
                videoUrl = extractedUrl
                currentTab = PostModeTab.VIDEO
                videoExtractorViewModel.extractVideo(extractedUrl)
            }
        }
    }

    // React to extraction results
    androidx.compose.runtime.LaunchedEffect(videoExtractionState) {
        when (val state = videoExtractionState) {
            is VideoExtractionState.Success -> {
                lastExtractedResult = state.result
                if (videoTitle.isBlank() && !state.result.title.isNullOrBlank()) {
                    videoTitle = state.result.title
                }
                if (videoThumbnailUrl.isBlank() && !state.result.thumbnailUrl.isNullOrBlank()) {
                    videoThumbnailUrl = state.result.thumbnailUrl
                }
                errorMessage = null
            }
            is VideoExtractionState.Error -> {
                lastExtractedResult = state.fallbackResult
                errorMessage = state.message
            }
            else -> {}
        }
    }

    // Photo Post Form State
    var photoCaption by remember { mutableStateOf("") }
    var newPhotoInput by remember { mutableStateOf("") }
    val photoUrlsList = remember { mutableStateListOf<String>() }
    var photoCategory by remember { mutableStateOf("Photography") }

    // Text Post Form State
    var textCaption by remember { mutableStateOf("") }
    var selectedTemplateId by remember { mutableStateOf("preset_default") }
    var textCategory by remember { mutableStateOf("Thoughts") }

    // Pixel Art State
    var attachedPixelArt by remember { mutableStateOf<PixelArtData?>(null) }

    // General Legacy Form State
    var generalType by remember { mutableStateOf("Post") }
    var generalTitle by remember { mutableStateOf("") }
    var generalContent by remember { mutableStateOf("") }
    var generalUrl by remember { mutableStateOf("") }

    // Tags
    var tagsInput by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = QSurface,
        modifier = Modifier.fillMaxHeight(0.92f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 6.dp)
        ) {
            // Sheet Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = QOnSurface)
                }

                Text(
                    text = "Create Content",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = QOnSurface
                )

                Button(
                    onClick = {
                        isPublishing = true
                        errorMessage = null
                        val parsedTags = tagsInput.split(" ", ",")
                            .map { it.trim().removePrefix("#") }
                            .filter { it.isNotBlank() }

                        when (currentTab) {
                            PostModeTab.VIDEO -> {
                                if (videoUrl.isBlank()) {
                                    errorMessage = "Please enter or paste a video URL"
                                    isPublishing = false
                                    return@Button
                                }

                                scope.launch {
                                    var extracted = lastExtractedResult

                                    if (extracted == null) {
                                        try {
                                            val handler = com.example.video.VideoPlatformRegistry.resolve(videoUrl.trim())
                                            val parsedVideo = handler.parse(videoUrl.trim()) ?: com.example.video.ParsedVideo(handler.platformId, videoUrl.trim())
                                            val res = handler.extract(parsedVideo).getOrNull()
                                            if (res != null) {
                                                extracted = ExtractedVideoResult(
                                                    platform = res.platform,
                                                    platformDisplayName = handler.displayName,
                                                    originalUrl = res.originalUrl,
                                                    directVideoUrl = res.directUrl,
                                                    embedUrl = res.embedUrl,
                                                    title = res.title,
                                                    thumbnailUrl = res.thumbnailUrl,
                                                    authorName = res.authorName,
                                                    isDirectPlayable = res.isNativePlayable,
                                                    durationSeconds = res.durationSec,
                                                    isVertical = res.isVertical
                                                )
                                            }
                                        } catch (_: Exception) {}
                                    }

                                    val parsed = VideoEmbedUtils.parseVideoUrl(videoUrl)
                                    val finalEmbed = extracted?.embedUrl ?: parsed.embedUrl
                                    val finalDirect = extracted?.directVideoUrl
                                    val finalPlatform = extracted?.platform ?: parsed.platform
                                    val finalThumb = videoThumbnailUrl.ifBlank { extracted?.thumbnailUrl }
                                    val duration = extracted?.durationSeconds
                                    val width = extracted?.width
                                    val height = extracted?.height
                                    val isVertical = extracted?.isVertical ?: (parsed.platform == "tiktok")

                                    onPublishVideo(
                                        videoTitle.ifBlank { extracted?.title },
                                        videoUrl.trim(),
                                        finalEmbed,
                                        finalDirect,
                                        finalPlatform,
                                        finalThumb,
                                        duration,
                                        width,
                                        height,
                                        isVertical,
                                        videoCategory,
                                        parsedTags
                                    ) { success, err ->
                                        isPublishing = false
                                        if (success) onDismiss() else errorMessage = err ?: "Failed to post video"
                                    }
                                }
                            }

                            PostModeTab.PHOTO -> {
                                if (photoUrlsList.isEmpty()) {
                                    errorMessage = "Please add at least 1 photo URL"
                                    isPublishing = false
                                    return@Button
                                }
                                onPublishPhoto(photoCaption, photoUrlsList, photoCategory, parsedTags) { success, err ->
                                    isPublishing = false
                                    if (success) onDismiss() else errorMessage = err ?: "Failed to post photos"
                                }
                            }
                            PostModeTab.TEXT -> {
                                if (textCaption.isBlank()) {
                                    errorMessage = "Please enter text for your post"
                                    isPublishing = false
                                    return@Button
                                }
                                onPublishText(textCaption, selectedTemplateId, textCategory, parsedTags) { success, err ->
                                    isPublishing = false
                                    if (success) onDismiss() else errorMessage = err ?: "Failed to post text"
                                }
                            }
                            PostModeTab.PIXEL_ART -> {
                                if (attachedPixelArt == null || attachedPixelArt!!.pixels.isEmpty()) {
                                    errorMessage = "Please draw something on the pixel canvas first"
                                    isPublishing = false
                                    return@Button
                                }
                                val injectedContent = PixelArtUtils.injectPixelArt("Hand-Drawn Pixel Art", attachedPixelArt!!)
                                onPublishGeneral("PixelArt", "Pixel Art Drawing", injectedContent, null) { success, err ->
                                    isPublishing = false
                                    if (success) onDismiss() else errorMessage = err ?: "Failed to post pixel art"
                                }
                            }
                            PostModeTab.GENERAL -> {
                                if (generalContent.isBlank()) {
                                    errorMessage = "Please enter post content"
                                    isPublishing = false
                                    return@Button
                                }
                                onPublishGeneral(generalType, generalTitle, generalContent, generalUrl) { success, err ->
                                    isPublishing = false
                                    if (success) onDismiss() else errorMessage = err ?: "Failed to publish post"
                                }
                            }
                        }
                    },
                    enabled = !isPublishing,
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
                        Text("Publish", fontWeight = FontWeight.Bold)
                    }
                }
            }

            HorizontalDivider(color = QCardBorder, modifier = Modifier.padding(vertical = 8.dp))

            // Author Info Header
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
                        text = "@${currentUser?.effectiveUsername ?: "user"} • Public Post",
                        fontSize = 12.sp,
                        color = QOnSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Mode Selector Tabs (Scrollable Segmented Pills)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .clip(RoundedCornerShape(12.dp))
                    .background(QSurfaceVariant)
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                PostModeTab.values().forEach { tab ->
                    val isSelected = currentTab == tab
                    val (label, icon) = when (tab) {
                        PostModeTab.VIDEO -> "Video" to Icons.Default.Movie
                        PostModeTab.PHOTO -> "Photo" to Icons.Default.Image
                        PostModeTab.TEXT -> "Text" to Icons.Default.FormatQuote
                        PostModeTab.PIXEL_ART -> "Pixel Art" to Icons.Default.Brush
                        PostModeTab.GENERAL -> "General" to Icons.Default.NoteAdd
                    }

                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) QYellowPrimary else Color.Transparent)
                            .clickable {
                                currentTab = tab
                                errorMessage = null
                            }
                            .padding(horizontal = 14.dp, vertical = 9.dp)
                            .testTag("tab_${tab.name.lowercase()}")
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = icon,
                                contentDescription = label,
                                tint = if (isSelected) QOnYellow else QOnSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = label,
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) QOnYellow else QOnSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Dynamic Form UI Based on Selected Tab
            when (currentTab) {
                // 1. VIDEO POST FORM
                PostModeTab.VIDEO -> {
                    val isExtracting = videoExtractionState is VideoExtractionState.Extracting

                    Column {
                        // URL Input with Clear & Paste trailing actions
                        OutlinedTextField(
                            value = videoUrl,
                            onValueChange = {
                                videoUrl = it
                                // Typing clears the old preview and resets state
                                lastExtractedResult = null
                                videoExtractorViewModel.reset()
                            },
                            label = { Text("Video URL (TikTok, YouTube, FB, IG, Direct)") },
                            placeholder = { Text("Paste link: youtube.com, youtu.be, tiktok.com...") },
                            leadingIcon = {
                                Icon(Icons.Default.Movie, contentDescription = null, tint = QYellowDark)
                            },
                            trailingIcon = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(end = 4.dp)
                                ) {
                                    if (videoUrl.isNotBlank()) {
                                        IconButton(
                                            onClick = {
                                                videoUrl = ""
                                                lastExtractedResult = null
                                                videoExtractorViewModel.reset()
                                            },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Clear,
                                                contentDescription = "Clear URL",
                                                tint = QOnSurfaceVariant,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                    IconButton(
                                        onClick = {
                                            clipboardManager.getText()?.text?.let { clipText ->
                                                if (clipText.isNotBlank()) {
                                                    videoUrl = clipText.trim()
                                                    lastExtractedResult = null
                                                    videoExtractorViewModel.reset()
                                                }
                                            }
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.ContentPaste,
                                            contentDescription = "Paste from Clipboard",
                                            tint = QYellowDark,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("video_url_input"),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = QYellowPrimary,
                                unfocusedBorderColor = QCardBorder
                            )
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Extract Button Row
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Extracts video player, title & metadata",
                                fontSize = 12.sp,
                                color = QOnSurfaceVariant,
                                modifier = Modifier.weight(1f)
                            )

                            Button(
                                onClick = {
                                    if (videoUrl.isNotBlank()) {
                                        videoExtractorViewModel.extractVideo(videoUrl)
                                    }
                                },
                                enabled = videoUrl.isNotBlank() && !isExtracting,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = QYellowPrimary,
                                    contentColor = QOnYellow,
                                    disabledContainerColor = QSurfaceVariant,
                                    disabledContentColor = QOnSurfaceVariant
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.testTag("extract_video_button")
                            ) {
                                if (isExtracting) {
                                    CircularProgressIndicator(
                                        color = QOnYellow,
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Extracting...", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                } else {
                                    Icon(
                                        Icons.Default.Download,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Extract", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        // Error Banner with Retry Button
                        if (videoExtractionState is VideoExtractionState.Error) {
                            val errorState = videoExtractionState as VideoExtractionState.Error
                            Spacer(modifier = Modifier.height(10.dp))
                            Card(
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(containerColor = QLikeRed.copy(alpha = 0.12f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                ) {
                                    Text(
                                        text = errorState.message,
                                        color = QLikeRed,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Button(
                                        onClick = { videoExtractorViewModel.extractVideo(videoUrl) },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = QLikeRed,
                                            contentColor = Color.White
                                        ),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text("Retry", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }

                        // Extracted Preview Section
                        val result = lastExtractedResult
                        if (result != null) {
                            Spacer(modifier = Modifier.height(12.dp))

                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = QSurfaceVariant),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        // Platform Chip
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(if (result.platform == "gdrive") com.example.ui.theme.QRelayGreen.copy(alpha = 0.2f) else QYellowLight)
                                                .padding(horizontal = 8.dp, vertical = 3.dp)
                                        ) {
                                            Text(
                                                text = result.platformDisplayName.uppercase(),
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (result.platform == "gdrive") com.example.ui.theme.QRelayGreen else QYellowDark
                                            )
                                        }

                                        if (result.platform == "gdrive") {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                result.fileSize?.let { size ->
                                                    Text(
                                                        text = formatFileSize(size),
                                                        fontSize = 11.sp,
                                                        color = QOnSurfaceVariant
                                                    )
                                                    Text(" • ", color = QOnSurfaceVariant, fontSize = 11.sp)
                                                }
                                                result.durationSeconds?.let { dur ->
                                                    Text(
                                                        text = formatDuration(dur),
                                                        fontSize = 11.sp,
                                                        color = QOnSurfaceVariant
                                                    )
                                                }
                                            }
                                        } else if (result.isDirectPlayable && !result.directVideoUrl.isNullOrBlank()) {
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(com.example.ui.theme.QRelayGreen.copy(alpha = 0.2f))
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = if (result.platform == "tiktok") "No Watermark HD" else "Direct Video",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = com.example.ui.theme.QRelayGreen
                                                )
                                            }
                                        }
                                    }

                                    result.noticeMessage?.let { notice ->
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = notice,
                                            fontSize = 11.sp,
                                            color = QOnSurfaceVariant
                                        )
                                        
                                        if (notice.contains("NOT_PUBLIC") || notice.contains("Set sharing")) {
                                            var showHowToShare by remember { mutableStateOf(false) }
                                            Text(
                                                text = "How to make public?",
                                                color = QYellowPrimary,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.clickable { showHowToShare = true }.padding(top = 4.dp)
                                            )
                                            if (showHowToShare) {
                                                HowToShareDriveBottomSheet { showHowToShare = false }
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    // Render via unified VideoPlatformRegistry
                                    val handler = com.example.video.VideoPlatformRegistry.resolve(result.originalUrl)
                                    handler.Player(
                                        video = result.toExtractedVideo(),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp)),
                                        config = com.example.video.PlayerConfig(
                                            autoPlay = false,
                                            showControls = true
                                        ),
                                        controller = null
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        OutlinedTextField(
                            value = videoTitle,
                            onValueChange = { videoTitle = it },
                            label = { Text("Video Title (optional)") },
                            placeholder = { Text("e.g. \"Building a Compose App in 10 minutes\"") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = QYellowPrimary,
                                unfocusedBorderColor = QCardBorder
                            )
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        ImgBbUrlInputField(
                            value = videoThumbnailUrl,
                            onValueChange = { videoThumbnailUrl = it },
                            label = "Custom Cover Thumbnail URL (optional)",
                            placeholder = "https://i.ibb.co/...",
                            previewAspectRatio = 16f / 9f,
                            testTag = "input_video_cover_url"
                        )
                    }
                }

                // 2. PHOTO POST FORM
                PostModeTab.PHOTO -> {
                    Column {
                        OutlinedTextField(
                            value = photoCaption,
                            onValueChange = { photoCaption = it },
                            label = { Text("Caption") },
                            placeholder = { Text("Describe your photo gallery...") },
                            minLines = 2,
                            maxLines = 4,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("photo_caption_input"),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = QYellowPrimary,
                                unfocusedBorderColor = QCardBorder
                            )
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Add Photo Input Row
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            ImgBbUrlInputField(
                                value = newPhotoInput,
                                onValueChange = { newPhotoInput = it },
                                label = "Image URL",
                                placeholder = "https://i.ibb.co/...",
                                modifier = Modifier.weight(1f),
                                testTag = "input_photo_url"
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    if (newPhotoInput.isNotBlank()) {
                                        photoUrlsList.add(newPhotoInput.trim())
                                        newPhotoInput = ""
                                    }
                                },
                                enabled = newPhotoInput.isNotBlank(),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = QYellowPrimary,
                                    contentColor = QOnYellow
                                ),
                                modifier = Modifier.padding(top = 8.dp).height(54.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Add Photo")
                            }
                        }

                        // Photo Thumbnails List
                        if (photoUrlsList.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Attached Photos (${photoUrlsList.size})",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = QOnSurface
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState())
                            ) {
                                photoUrlsList.forEachIndexed { idx, url ->
                                    Box(
                                        modifier = Modifier
                                            .size(90.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(QSurfaceVariant)
                                            .border(1.dp, QCardBorder, RoundedCornerShape(12.dp))
                                    ) {
                                        AsyncImage(
                                            model = ImageRequest.Builder(LocalContext.current)
                                                .data(url)
                                                .crossfade(true)
                                                .build(),
                                            contentDescription = "Photo Preview",
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxWidth()
                                        )

                                        // Remove Button
                                        Box(
                                            contentAlignment = Alignment.Center,
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .padding(4.dp)
                                                .size(22.dp)
                                                .clip(CircleShape)
                                                .background(Color.Black.copy(alpha = 0.7f))
                                                .clickable { photoUrlsList.removeAt(idx) }
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "Remove photo",
                                                tint = Color.White,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // 3. TEXT / THOUGHT POST FORM
                PostModeTab.TEXT -> {
                    Column {
                        OutlinedTextField(
                            value = textCaption,
                            onValueChange = { textCaption = it },
                            label = { Text("What's on your mind?") },
                            placeholder = { Text("Share an idea, quote, or discussion point...") },
                            minLines = 4,
                            maxLines = 8,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("text_post_input"),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = QYellowPrimary,
                                unfocusedBorderColor = QCardBorder
                            )
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Gradient Template Selector
                        Text(
                            text = "Card Background Style",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = QOnSurface
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                        ) {
                            TextPostTemplates.templates.forEach { tmpl ->
                                val isSelected = selectedTemplateId == tmpl.id
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .width(110.dp)
                                        .height(48.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(tmpl.backgroundBrush)
                                        .border(
                                            width = if (isSelected) 2.dp else 1.dp,
                                            color = if (isSelected) QYellowPrimary else QCardBorder,
                                            shape = RoundedCornerShape(10.dp)
                                        )
                                        .clickable { selectedTemplateId = tmpl.id }
                                        .padding(6.dp)
                                ) {
                                    Text(
                                        text = tmpl.name,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = tmpl.textColor
                                    )
                                }
                            }
                        }

                        // Live Card Preview
                        if (textCaption.isNotBlank()) {
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = "Live Card Preview",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = QYellowDark
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            StyledTextPostCard(
                                content = textCaption,
                                templateId = selectedTemplateId,
                                category = textCategory
                            )
                        }
                    }
                }

                // 4. PIXEL ART DRAWING STUDIO FORM
                PostModeTab.PIXEL_ART -> {
                    Column {
                        PixelArtCanvasStudio(
                            initialData = attachedPixelArt,
                            onAttachArt = { art ->
                                attachedPixelArt = art
                            }
                        )

                        if (attachedPixelArt != null) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Attached Pixel Art Preview",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = QYellowDark
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            PixelArtViewer(
                                data = attachedPixelArt!!,
                                title = "Attached Drawing (${attachedPixelArt!!.pixels.size} Strokes)"
                            )
                        }
                    }
                }

                // 5. GENERAL LEGACY POST FORM
                PostModeTab.GENERAL -> {
                    Column {
                        OutlinedTextField(
                            value = generalTitle,
                            onValueChange = { generalTitle = it },
                            placeholder = { Text("Title (e.g. \"Great AI tools to explore\")") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = QYellowPrimary,
                                unfocusedBorderColor = QCardBorder
                            )
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = generalContent,
                            onValueChange = { generalContent = it },
                            placeholder = { Text("Write your post content...") },
                            minLines = 5,
                            maxLines = 8,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = QYellowPrimary,
                                unfocusedBorderColor = QCardBorder
                            )
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = generalUrl,
                            onValueChange = { generalUrl = it },
                            label = { Text("External Link (optional)") },
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
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Tags Input
            OutlinedTextField(
                value = tagsInput,
                onValueChange = { tagsInput = it },
                label = { Text("#Tags & Keywords (optional)") },
                placeholder = { Text("e.g. #android #kotlin #tech") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = QYellowPrimary,
                    unfocusedBorderColor = QCardBorder
                )
            )

            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = errorMessage ?: "",
                    color = QLikeRed,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Creator Reward Banner
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
                    text = "Publishing earns +50 Creator Points towards your rank!",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = QYellowDark
                )
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HowToShareDriveBottomSheet(onDismiss: () -> Unit) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = QSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .navigationBarsPadding()
        ) {
            Text(
                "How to make a Drive video public",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = QOnSurface
            )
            Spacer(modifier = Modifier.height(16.dp))
            
            val steps = listOf(
                "1. Open the file in Google Drive (Web or App).",
                "2. Click 'Share' and change 'Restricted' to 'Anyone with the link'.",
                "3. Make sure the role is set to 'Viewer'. Click 'Done'."
            )
            
            steps.forEach { step ->
                Text(
                    text = step,
                    fontSize = 14.sp,
                    color = QOnSurfaceVariant,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = QYellowPrimary, contentColor = QOnYellow)
            ) {
                Text("Got it")
            }
        }
    }
}

fun formatFileSize(size: Long): String {
    if (size <= 0) return "0 B"
    val units = listOf("B", "KB", "MB", "GB", "TB")
    val digitGroups = (Math.log10(size.toDouble()) / Math.log10(1024.0)).toInt()
    return String.format(java.util.Locale.US, "%.1f %s", size / Math.pow(1024.0, digitGroups.toDouble()), units[digitGroups])
}

fun formatDuration(seconds: Int): String {
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    return if (h > 0) {
        String.format(java.util.Locale.US, "%d:%02d:%02d", h, m, s)
    } else {
        String.format(java.util.Locale.US, "%d:%02d", m, s)
    }
}
