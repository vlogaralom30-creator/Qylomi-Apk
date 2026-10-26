package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.Episode
import com.example.data.model.Series
import com.example.ui.theme.QOnYellow
import com.example.ui.theme.QYellowLight
import com.example.ui.theme.QYellowPrimary

import androidx.compose.foundation.border
import androidx.compose.material.icons.filled.PlayArrow

@Composable
fun EpisodeThumbnail(
    episode: Episode,
    series: Series?,
    modifier: Modifier = Modifier,
    showBadges: Boolean = true,
    isPlaying: Boolean = false
) {
    val thumbnailUrl = episode.thumbnailUrl?.takeIf { it.isNotBlank() }
        ?: series?.bannerUrl?.takeIf { it.isNotBlank() }
        ?: series?.posterUrl?.takeIf { it.isNotBlank() }

    val baseModifier = modifier
        .aspectRatio(16f / 9f)
        .clip(RoundedCornerShape(12.dp))
        .background(QYellowLight)

    val finalModifier = if (isPlaying) {
        baseModifier.border(2.dp, QYellowPrimary, RoundedCornerShape(12.dp))
    } else {
        baseModifier
    }

    Box(modifier = finalModifier) {
        if (!thumbnailUrl.isNullOrBlank()) {
            AsyncImage(
                model = thumbnailUrl,
                contentDescription = "Episode thumbnail",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            // Branded placeholder
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "E${episode.episodeNumber}",
                    color = QYellowPrimary,
                    fontSize = 24.sp,
                    modifier = Modifier.padding(8.dp)
                )
            }
        }

        // Soft dark gradient at bottom for text visibility
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.6f))
                    )
                )
        )

        if (isPlaying) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.4f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Playing",
                    tint = QYellowPrimary,
                    modifier = Modifier.size(26.dp)
                )
            }
        }

        if (showBadges) {
            // Episode number badge (top-left)
            Text(
                text = "E${episode.episodeNumber}",
                color = Color.White,
                fontSize = 10.sp,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(4.dp)
                    .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            )

            // Duration badge (bottom-right)
            val duration = episode.durationSeconds ?: 0
            val durationText = "${duration / 60}:${(duration % 60).toString().padStart(2, '0')}"
            Text(
                text = durationText,
                color = Color.White,
                fontSize = 10.sp,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(4.dp)
                    .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            )

            // Watched progress bar (bottom)
            if (episode.watchedProgressPercent > 0) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(3.dp)
                        .background(Color.White.copy(alpha = 0.3f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth((episode.watchedProgressPercent / 100f).toFloat())
                            .fillMaxHeight()
                            .background(QYellowPrimary)
                    )
                }
            }
            
            if (episode.isCompleted) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Completed",
                    tint = QYellowPrimary,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .size(16.dp)
                )
            }
        }
    }
}
