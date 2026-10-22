package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Colorize
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Redo
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.PixelArtData
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

enum class DrawingTool {
    PENCIL, ERASER, EYEDROPPER
}

@Composable
fun PixelArtCanvasStudio(
    initialData: PixelArtData? = null,
    onAttachArt: (PixelArtData) -> Unit,
    modifier: Modifier = Modifier
) {
    var gridSize by remember { mutableStateOf(initialData?.gridSize ?: 32) }
    var currentTool by remember { mutableStateOf(DrawingTool.PENCIL) }
    var brushSizePx by remember { mutableStateOf(2) } // 1px, 2px, 3px, 4px
    var selectedColorHex by remember { mutableStateOf("#00E5FF") }
    var showGridLines by remember { mutableStateOf(true) }

    // Stroke ordered index list (preserves hand movement)
    val drawnPixels = remember { mutableStateListOf<Int>().apply { initialData?.pixels?.let { addAll(it) } } }
    // Color map: Index string -> Hex
    val colorMap = remember { mutableStateMapOf<String, String>().apply { initialData?.colors?.let { putAll(it) } } }

    // Undo / Redo stacks
    val undoStack = remember { mutableStateListOf<Pair<List<Int>, Map<String, String>>>() }
    val redoStack = remember { mutableStateListOf<Pair<List<Int>, Map<String, String>>>() }

    var lastCellX by remember { mutableStateOf<Int?>(null) }
    var lastCellY by remember { mutableStateOf<Int?>(null) }

    fun pushUndoState() {
        undoStack.add(Pair(drawnPixels.toList(), colorMap.toMap()))
        if (undoStack.size > 25) undoStack.removeAt(0)
        redoStack.clear()
    }

    fun applyTouchAtCell(cellX: Int, cellY: Int) {
        if (cellX !in 0 until gridSize || cellY !in 0 until gridSize) return

        when (currentTool) {
            DrawingTool.PENCIL -> {
                val lineIndices = if (lastCellX != null && lastCellY != null) {
                    PixelArtUtils.getLinePixelIndices(lastCellX!!, lastCellY!!, cellX, cellY, gridSize)
                } else {
                    listOf(PixelArtUtils.xyToIndex(cellX, cellY, gridSize))
                }

                lineIndices.forEach { ptIndex ->
                    val cx = PixelArtUtils.indexToX(ptIndex, gridSize)
                    val cy = PixelArtUtils.indexToY(ptIndex, gridSize)
                    val brushIndices = PixelArtUtils.getBrushIndices(cx, cy, brushSizePx, gridSize)
                    brushIndices.forEach { idx ->
                        drawnPixels.remove(idx)
                        drawnPixels.add(idx)
                        colorMap[idx.toString()] = selectedColorHex
                    }
                }
            }
            DrawingTool.ERASER -> {
                val brushIndices = PixelArtUtils.getBrushIndices(cellX, cellY, brushSizePx, gridSize)
                brushIndices.forEach { idx ->
                    drawnPixels.remove(idx)
                    colorMap.remove(idx.toString())
                }
            }
            DrawingTool.EYEDROPPER -> {
                val index = PixelArtUtils.xyToIndex(cellX, cellY, gridSize)
                val sampled = colorMap[index.toString()]
                if (sampled != null) {
                    selectedColorHex = sampled
                    currentTool = DrawingTool.PENCIL
                }
            }
        }
        lastCellX = cellX
        lastCellY = cellY
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = QSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, QCardBorder),
        modifier = modifier
            .fillMaxWidth()
            .testTag("pixel_art_studio_card")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header: Tool Title & Resolution Chips
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Brush, contentDescription = null, tint = QYellowDark, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Pixel Art Studio",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = QOnSurface
                    )
                }

                // Grid Size Selector Chips (16x16, 24x24, 32x32, 48x48, 64x64)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf(16, 24, 32, 48, 64).forEach { sizeOption ->
                        val isSelected = gridSize == sizeOption
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSelected) QYellowPrimary else QSurfaceVariant)
                                .clickable {
                                    pushUndoState()
                                    gridSize = sizeOption
                                }
                                .padding(horizontal = 6.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = "${sizeOption}x${sizeOption}",
                                fontSize = 10.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) QOnYellow else QOnSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Main Interactive Drawing Canvas
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF0F172A))
                    .border(1.dp, QCardBorder, RoundedCornerShape(12.dp))
                    .pointerInput(gridSize, currentTool, brushSizePx, selectedColorHex) {
                        detectTapGestures(
                            onPress = { offset ->
                                pushUndoState()
                                val cellX = (offset.x / (size.width / gridSize)).toInt().coerceIn(0, gridSize - 1)
                                val cellY = (offset.y / (size.height / gridSize)).toInt().coerceIn(0, gridSize - 1)
                                lastCellX = cellX
                                lastCellY = cellY
                                applyTouchAtCell(cellX, cellY)
                            }
                        )
                    }
                    .pointerInput(gridSize, currentTool, brushSizePx, selectedColorHex) {
                        detectDragGestures(
                            onDragStart = { offset ->
                                pushUndoState()
                                val cellX = (offset.x / (size.width / gridSize)).toInt().coerceIn(0, gridSize - 1)
                                val cellY = (offset.y / (size.height / gridSize)).toInt().coerceIn(0, gridSize - 1)
                                lastCellX = cellX
                                lastCellY = cellY
                                applyTouchAtCell(cellX, cellY)
                            },
                            onDragEnd = {
                                lastCellX = null
                                lastCellY = null
                            },
                            onDragCancel = {
                                lastCellX = null
                                lastCellY = null
                            },
                            onDrag = { change, _ ->
                                change.consume()
                                val cellX = (change.position.x / (size.width / gridSize)).toInt().coerceIn(0, gridSize - 1)
                                val cellY = (change.position.y / (size.height / gridSize)).toInt().coerceIn(0, gridSize - 1)
                                applyTouchAtCell(cellX, cellY)
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val cellSize = size.width / gridSize

                    // Render Drawn Pixels
                    drawnPixels.forEach { index ->
                        val x = PixelArtUtils.indexToX(index, gridSize)
                        val y = PixelArtUtils.indexToY(index, gridSize)
                        val hexColor = colorMap[index.toString()] ?: "#FFFFFF"
                        val parsedColor = try {
                            Color(android.graphics.Color.parseColor(hexColor))
                        } catch (_: Exception) {
                            Color.White
                        }

                        drawRect(
                            color = parsedColor,
                            topLeft = Offset(x * cellSize, y * cellSize),
                            size = Size(cellSize, cellSize)
                        )
                    }

                    // Optional Grid Lines
                    if (showGridLines && gridSize <= 64) {
                        val gridColor = Color.White.copy(alpha = 0.08f)
                        for (i in 1 until gridSize) {
                            val pos = i * cellSize
                            drawLine(gridColor, start = Offset(pos, 0f), end = Offset(pos, size.height), strokeWidth = 1f)
                            drawLine(gridColor, start = Offset(0f, pos), end = Offset(size.width, pos), strokeWidth = 1f)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Toolbar 1: Drawing Tools & Brush Size
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Tools (Pencil, Eraser, Eyedropper)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (currentTool == DrawingTool.PENCIL) QYellowPrimary else QSurfaceVariant)
                            .clickable { currentTool = DrawingTool.PENCIL }
                            .padding(8.dp)
                    ) {
                        Icon(Icons.Default.Brush, contentDescription = "Pencil", tint = if (currentTool == DrawingTool.PENCIL) QOnYellow else QOnSurfaceVariant, modifier = Modifier.size(18.dp))
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (currentTool == DrawingTool.ERASER) QYellowPrimary else QSurfaceVariant)
                            .clickable { currentTool = DrawingTool.ERASER }
                            .padding(8.dp)
                    ) {
                        Text("🧹", fontSize = 14.sp)
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (currentTool == DrawingTool.EYEDROPPER) QYellowPrimary else QSurfaceVariant)
                            .clickable { currentTool = DrawingTool.EYEDROPPER }
                            .padding(8.dp)
                    ) {
                        Icon(Icons.Default.Colorize, contentDescription = "Eyedropper", tint = if (currentTool == DrawingTool.EYEDROPPER) QOnYellow else QOnSurfaceVariant, modifier = Modifier.size(18.dp))
                    }
                }

                // Brush Thickness (1px, 2px, 3px, 4px)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text("Thickness:", fontSize = 11.sp, color = QOnSurfaceVariant, fontWeight = FontWeight.Bold)
                    listOf(1, 2, 3, 4).forEach { size ->
                        val isSelected = brushSizePx == size
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(26.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) QYellowPrimary else QSurfaceVariant)
                                .clickable { brushSizePx = size }
                        ) {
                            Text(
                                text = "${size}p",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) QOnYellow else QOnSurfaceVariant
                            )
                        }
                    }
                }

                // Grid Lines & Undo / Redo
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(
                        onClick = { showGridLines = !showGridLines },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.GridOn, contentDescription = "Toggle Grid", tint = if (showGridLines) QYellowPrimary else QOnSurfaceVariant, modifier = Modifier.size(18.dp))
                    }

                    IconButton(
                        onClick = {
                            if (undoStack.isNotEmpty()) {
                                redoStack.add(Pair(drawnPixels.toList(), colorMap.toMap()))
                                val lastState = undoStack.removeAt(undoStack.size - 1)
                                drawnPixels.clear()
                                drawnPixels.addAll(lastState.first)
                                colorMap.clear()
                                colorMap.putAll(lastState.second)
                            }
                        },
                        enabled = undoStack.isNotEmpty(),
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.Undo, contentDescription = "Undo", tint = if (undoStack.isNotEmpty()) QOnSurface else QOnSurfaceVariant.copy(alpha = 0.4f), modifier = Modifier.size(18.dp))
                    }

                    IconButton(
                        onClick = {
                            if (redoStack.isNotEmpty()) {
                                undoStack.add(Pair(drawnPixels.toList(), colorMap.toMap()))
                                val nextState = redoStack.removeAt(redoStack.size - 1)
                                drawnPixels.clear()
                                drawnPixels.addAll(nextState.first)
                                colorMap.clear()
                                colorMap.putAll(nextState.second)
                            }
                        },
                        enabled = redoStack.isNotEmpty(),
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.Redo, contentDescription = "Redo", tint = if (redoStack.isNotEmpty()) QOnSurface else QOnSurfaceVariant.copy(alpha = 0.4f), modifier = Modifier.size(18.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Palette Swatches Strip (PICO-8 & Cyber Neon)
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
            ) {
                PixelArtUtils.cyberNeonPalette.plus(PixelArtUtils.pico8Palette).distinct().forEach { hex ->
                    val parsedColor = try { Color(android.graphics.Color.parseColor(hex)) } catch (_: Exception) { Color.Cyan }
                    val isSelected = selectedColorHex.equals(hex, ignoreCase = true)

                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(parsedColor)
                            .border(
                                width = if (isSelected) 2.5.dp else 1.dp,
                                color = if (isSelected) QYellowPrimary else Color.White.copy(alpha = 0.3f),
                                shape = CircleShape
                            )
                            .clickable {
                                selectedColorHex = hex
                                if (currentTool == DrawingTool.ERASER) currentTool = DrawingTool.PENCIL
                            }
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Footer Action: Attach Art to Post Button
            Button(
                onClick = {
                    val artData = PixelArtData(
                        gridSize = gridSize,
                        pixels = drawnPixels.toList(),
                        colors = colorMap.toMap()
                    )
                    onAttachArt(artData)
                },
                enabled = drawnPixels.isNotEmpty(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = QYellowPrimary,
                    contentColor = QOnYellow
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp)
            ) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Attach Pixel Art (${drawnPixels.size} Strokes)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        }
    }
}
