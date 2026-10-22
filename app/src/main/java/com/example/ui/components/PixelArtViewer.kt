package com.example.ui.components

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.example.data.model.PixelArtData
import com.example.ui.theme.QCardBorder
import com.example.ui.theme.QOnSurface
import com.example.ui.theme.QOnSurfaceVariant
import com.example.ui.theme.QOnYellow
import com.example.ui.theme.QSurface
import com.example.ui.theme.QYellowDark
import com.example.ui.theme.QYellowLight
import com.example.ui.theme.QYellowPrimary
import com.example.util.PixelArtUtils
import kotlinx.coroutines.delay
import java.io.File
import java.io.FileOutputStream

@Composable
fun PixelArtViewer(
    data: PixelArtData,
    modifier: Modifier = Modifier,
    title: String? = null
) {
    val context = LocalContext.current
    val totalPixels = data.pixels.size
    val isAnimation = data.isAnimation && !data.frames.isNullOrEmpty()

    var isPlaying by remember { mutableStateOf(false) }
    var currentPixelIndex by remember { mutableStateOf(totalPixels) }
    var currentFrameIndex by remember { mutableStateOf(0) }
    var playbackSpeed by remember { mutableStateOf(1f) } // 1x, 2x, 4x

    // 1. Replay Engine Coroutine Loop
    LaunchedEffect(isPlaying, playbackSpeed, isAnimation) {
        if (isPlaying) {
            if (isAnimation) {
                // Multi-frame animation loop
                val frameDelayMs = (1000.0 / (data.fps * playbackSpeed)).toLong().coerceAtLeast(30L)
                val totalFrames = data.frames?.size ?: 1
                while (isPlaying) {
                    delay(frameDelayMs)
                    currentFrameIndex = (currentFrameIndex + 1) % totalFrames
                }
            } else {
                // Stroke-by-stroke replay loop
                if (currentPixelIndex >= totalPixels) {
                    currentPixelIndex = 0
                }
                val stepDelayMs = (25.0 / playbackSpeed).toLong().coerceAtLeast(5L)
                val chunkSize = (1 * playbackSpeed).toInt().coerceAtLeast(1)
                while (isPlaying && currentPixelIndex < totalPixels) {
                    delay(stepDelayMs)
                    currentPixelIndex = (currentPixelIndex + chunkSize).coerceAtMost(totalPixels)
                }
                if (currentPixelIndex >= totalPixels) {
                    isPlaying = false
                }
            }
        }
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = QSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, QCardBorder),
        modifier = modifier
            .fillMaxWidth()
            .testTag("pixel_art_viewer_card")
    ) {
        Column {
            // Header Bar
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.3f))
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(QYellowPrimary)
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = if (isAnimation) "PIXEL ANIMATION" else "VECTOR PIXEL ART",
                        color = QOnYellow,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = title ?: "${data.gridSize}x${data.gridSize} Grid (${totalPixels} Strokes)",
                    color = QOnSurface,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    modifier = Modifier.weight(1f)
                )

                // Speed Switcher
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(QYellowLight)
                        .clickable {
                            playbackSpeed = when (playbackSpeed) {
                                1f -> 2f
                                2f -> 4f
                                else -> 1f
                            }
                        }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "${playbackSpeed.toInt()}x Speed",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = QYellowDark
                    )
                }
            }

            // High DPI Vector Pixel Canvas
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .background(Color(0xFF0F172A)),
                contentAlignment = Alignment.Center
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("pixel_canvas_render")
                ) {
                    val gridSize = data.gridSize.coerceAtLeast(8)
                    val cellSize = size.width / gridSize

                    val pixelsToDraw = if (isAnimation) {
                        data.frames?.getOrNull(currentFrameIndex) ?: emptyList()
                    } else {
                        data.pixels.take(currentPixelIndex)
                    }

                    val colorsMap = if (isAnimation) {
                        data.frameColors?.getOrNull(currentFrameIndex) ?: data.colors ?: emptyMap()
                    } else {
                        data.colors ?: emptyMap()
                    }

                    pixelsToDraw.forEach { index ->
                        val x = PixelArtUtils.indexToX(index, gridSize)
                        val y = PixelArtUtils.indexToY(index, gridSize)
                        val hexColor = colorsMap[index.toString()] ?: "#FFFFFF"
                        val parsedColor = try {
                            val colorInt = android.graphics.Color.parseColor(hexColor)
                            Color(colorInt)
                        } catch (_: Exception) {
                            Color.White
                        }

                        drawRect(
                            color = parsedColor,
                            topLeft = Offset(x * cellSize, y * cellSize),
                            size = Size(cellSize, cellSize)
                        )
                    }
                }
            }

            // Interactive Controls Footer
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                // Replay Progress Slider
                if (!isAnimation && totalPixels > 0) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "$currentPixelIndex/$totalPixels",
                            fontSize = 11.sp,
                            color = QOnSurfaceVariant,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Slider(
                            value = currentPixelIndex.toFloat(),
                            onValueChange = {
                                currentPixelIndex = it.toInt()
                                isPlaying = false
                            },
                            valueRange = 0f..totalPixels.toFloat(),
                            colors = SliderDefaults.colors(
                                thumbColor = QYellowPrimary,
                                activeTrackColor = QYellowPrimary,
                                inactiveTrackColor = QCardBorder
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Action Buttons Bar
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Play / Pause Replay Button
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(QYellowPrimary)
                            .clickable {
                                if (currentPixelIndex >= totalPixels && !isAnimation) {
                                    currentPixelIndex = 0
                                }
                                isPlaying = !isPlaying
                            }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Pause Replay" else "Play Replay",
                            tint = QOnYellow,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isPlaying) "Pause" else (if (isAnimation) "Play Anim" else "Play Replay"),
                            fontWeight = FontWeight.Bold,
                            color = QOnYellow,
                            fontSize = 12.sp
                        )
                    }

                    // Reset Replay
                    IconButton(
                        onClick = {
                            isPlaying = false
                            currentPixelIndex = totalPixels
                            currentFrameIndex = 0
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Reset",
                            tint = QOnSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Export PNG Image
                    IconButton(
                        onClick = {
                            try {
                                val bitmap = PixelArtUtils.renderToBitmap(data, targetSizePx = 600)
                                val cachePath = File(context.cacheDir, "images").apply { mkdirs() }
                                val file = File(cachePath, "pixel_art_${System.currentTimeMillis()}.png")
                                FileOutputStream(file).use { out ->
                                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                                }
                                val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = "image/png"
                                    putExtra(Intent.EXTRA_STREAM, uri)
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(Intent.createChooser(intent, "Export Pixel Art"))
                            } catch (_: Exception) {}
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = "Export PNG",
                            tint = QYellowDark,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}
