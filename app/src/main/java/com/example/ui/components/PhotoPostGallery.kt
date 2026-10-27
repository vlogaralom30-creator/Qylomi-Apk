package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.ui.theme.QYellowPrimary

@Composable
fun PhotoPostGallery(
    photoUrls: List<String>,
    caption: String? = null,
    modifier: Modifier = Modifier
) {
    if (photoUrls.isEmpty()) return

    var selectedIndexForLightbox by remember { mutableStateOf<Int?>(null) }

    val safeCount = photoUrls.size
    val pagerState = rememberPagerState(
        initialPage = 0,
        pageCount = { safeCount }
    )

    Column(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(if (safeCount == 1) 4f / 3f else 1f)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF1E2028))
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                val safeIndex = page.coerceIn(0, (photoUrls.size - 1).coerceAtLeast(0))
                val url = photoUrls.getOrNull(safeIndex) ?: ""
                AsyncImage(
                    model = url,
                    contentDescription = caption ?: "Photo Post Image",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable { selectedIndexForLightbox = safeIndex }
                )
            }

            // Page indicator pill (e.g. 1/3)
            if (safeCount > 1) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.Black.copy(alpha = 0.65f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "${(pagerState.currentPage + 1).coerceAtMost(safeCount)}/$safeCount",
                        color = Color.White,
                        fontSize = 11.sp
                    )
                }

                // Bottom dots indicator
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    repeat(safeCount) { index ->
                        val isSelected = pagerState.currentPage == index
                        Box(
                            modifier = Modifier
                                .size(if (isSelected) 8.dp else 6.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) QYellowPrimary else Color.White.copy(alpha = 0.5f))
                        )
                    }
                }
            }
        }
    }

    // Fullscreen Lightbox Dialog
    selectedIndexForLightbox?.let { initialIndex ->
        val safeInitial = initialIndex.coerceIn(0, (photoUrls.size - 1).coerceAtLeast(0))
        val lightboxPagerState = rememberPagerState(
            initialPage = safeInitial,
            pageCount = { photoUrls.size }
        )

        Dialog(
            onDismissRequest = { selectedIndexForLightbox = null },
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = false
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
            ) {
                HorizontalPager(
                    state = lightboxPagerState,
                    modifier = Modifier.fillMaxSize()
                ) { page ->
                    val url = photoUrls.getOrNull(page) ?: ""
                    AsyncImage(
                        model = url,
                        contentDescription = "Lightbox Image",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // Close Button Top-End
                IconButton(
                    onClick = { selectedIndexForLightbox = null },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .statusBarsPadding()
                        .padding(16.dp)
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.6f))
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close Lightbox",
                        tint = Color.White
                    )
                }

                // Page indicator bottom
                if (photoUrls.size > 1) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .navigationBarsPadding()
                            .padding(bottom = 24.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.Black.copy(alpha = 0.6f))
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "${lightboxPagerState.currentPage + 1} / ${photoUrls.size}",
                            color = Color.White,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }
}
