package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import com.example.ui.theme.*
import com.example.util.PixelArtUtils

enum class DrawingTool {
    PENCIL, ERASER, EYEDROPPER
}

enum class PaletteChoice(val title: String, val colors: List<String>) {
    NEON("Neon", PixelArtUtils.cyberNeonPalette),
    PICO8("Pico-8", PixelArtUtils.pico8Palette),
    CLASSIC16("Classic", PixelArtUtils.classic16Palette),
    GAMEBOY("GameBoy", PixelArtUtils.gameboyPalette),
    PASTEL("Pastel", PixelArtUtils.pastelPalette)
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
    var selectedPalette by remember { mutableStateOf(PaletteChoice.NEON) }
    var selectedColorHex by remember { mutableStateOf(PaletteChoice.NEON.colors[1]) }
    var showGridLines by remember { mutableStateOf(true) }

    // Stroke ordered index list
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
            // Header: Title & Resolution Selector
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Brush,
                        contentDescription = null,
                        tint = QYellowPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Pixel Art Studio",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = QOnSurface
                    )
                }

                // Grid Size Chips
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf(16, 24, 32).forEach { size ->
                        val isSelected = gridSize == size
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) QYellowPrimary else QSurfaceVariant)
                                .clickable {
                                    if (gridSize != size) {
                                        pushUndoState()
                                        gridSize = size
                                        drawnPixels.clear()
                                        colorMap.clear()
                                    }
                                }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "${size}x${size}",
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) QOnYellow else QOnSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Tool & Action Toolbar
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    // Pencil
                    IconButton(
                        onClick = { currentTool = DrawingTool.PENCIL },
                        modifier = Modifier
                            .size(36.dp)
                            .background(
                                if (currentTool == DrawingTool.PENCIL) QYellowPrimary.copy(alpha = 0.2f) else Color.Transparent,
                                CircleShape
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Pencil",
                            tint = if (currentTool == DrawingTool.PENCIL) QYellowDark else QOnSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Eraser
                    IconButton(
                        onClick = { currentTool = DrawingTool.ERASER },
                        modifier = Modifier
                            .size(36.dp)
                            .background(
                                if (currentTool == DrawingTool.ERASER) QYellowPrimary.copy(alpha = 0.2f) else Color.Transparent,
                                CircleShape
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Eraser",
                            tint = if (currentTool == DrawingTool.ERASER) QYellowDark else QOnSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Eyedropper
                    IconButton(
                        onClick = { currentTool = DrawingTool.EYEDROPPER },
                        modifier = Modifier
                            .size(36.dp)
                            .background(
                                if (currentTool == DrawingTool.EYEDROPPER) QYellowPrimary.copy(alpha = 0.2f) else Color.Transparent,
                                CircleShape
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Colorize,
                            contentDescription = "Eyedropper",
                            tint = if (currentTool == DrawingTool.EYEDROPPER) QYellowDark else QOnSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Grid toggle
                    IconButton(
                        onClick = { showGridLines = !showGridLines },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.GridOn,
                            contentDescription = "Toggle Grid",
                            tint = if (showGridLines) QYellowDark else QOnSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    // Undo
                    IconButton(
                        onClick = {
                            if (undoStack.isNotEmpty()) {
                                redoStack.add(Pair(drawnPixels.toList(), colorMap.toMap()))
                                val prev = undoStack.removeAt(undoStack.size - 1)
                                drawnPixels.clear()
                                drawnPixels.addAll(prev.first)
                                colorMap.clear()
                                colorMap.putAll(prev.second)
                            }
                        },
                        enabled = undoStack.isNotEmpty(),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Undo,
                            contentDescription = "Undo",
                            tint = if (undoStack.isNotEmpty()) QOnSurface else QOnSurfaceVariant.copy(alpha = 0.3f),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Redo
                    IconButton(
                        onClick = {
                            if (redoStack.isNotEmpty()) {
                                undoStack.add(Pair(drawnPixels.toList(), colorMap.toMap()))
                                val next = redoStack.removeAt(redoStack.size - 1)
                                drawnPixels.clear()
                                drawnPixels.addAll(next.first)
                                colorMap.clear()
                                colorMap.putAll(next.second)
                            }
                        },
                        enabled = redoStack.isNotEmpty(),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Redo,
                            contentDescription = "Redo",
                            tint = if (redoStack.isNotEmpty()) QOnSurface else QOnSurfaceVariant.copy(alpha = 0.3f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Canvas Drawing Surface
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF0F172A))
                    .pointerInput(gridSize, currentTool, brushSizePx, selectedColorHex) {
                        detectTapGestures(
                            onPress = { offset ->
                                pushUndoState()
                                val cellX = (offset.x / (size.width / gridSize)).toInt().coerceIn(0, gridSize - 1)
                                val cellY = (offset.y / (size.height / gridSize)).toInt().coerceIn(0, gridSize - 1)
                                lastCellX = null
                                lastCellY = null
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
                                lastCellX = null
                                lastCellY = null
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
                    }
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val cellSize = size.width / gridSize

                    // Render drawn pixels
                    drawnPixels.forEach { idx ->
                        val px = PixelArtUtils.indexToX(idx, gridSize)
                        val py = PixelArtUtils.indexToY(idx, gridSize)
                        val hex = colorMap[idx.toString()] ?: "#FFFFFF"
                        val parsedColor = try {
                            Color(android.graphics.Color.parseColor(hex))
                        } catch (_: Exception) {
                            Color.White
                        }
                        drawRect(
                            color = parsedColor,
                            topLeft = Offset(px * cellSize, py * cellSize),
                            size = Size(cellSize, cellSize)
                        )
                    }

                    // Grid lines overlay
                    if (showGridLines) {
                        val gridStrokeColor = Color.White.copy(alpha = 0.08f)
                        for (i in 0..gridSize) {
                            drawLine(
                                color = gridStrokeColor,
                                start = Offset(i * cellSize, 0f),
                                end = Offset(i * cellSize, size.height),
                                strokeWidth = 1f
                            )
                            drawLine(
                                color = gridStrokeColor,
                                start = Offset(0f, i * cellSize),
                                end = Offset(size.width, i * cellSize),
                                strokeWidth = 1f
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Palette Category Tabs
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                PaletteChoice.values().forEach { palette ->
                    val isChosen = selectedPalette == palette
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isChosen) QYellowPrimary else QSurfaceVariant)
                            .clickable {
                                selectedPalette = palette
                                if (!palette.colors.contains(selectedColorHex)) {
                                    selectedColorHex = palette.colors.first()
                                }
                            }
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = palette.title,
                            fontSize = 11.sp,
                            fontWeight = if (isChosen) FontWeight.Bold else FontWeight.Medium,
                            color = if (isChosen) QOnYellow else QOnSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Color Swatches for the chosen palette
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                selectedPalette.colors.forEach { hex ->
                    val isSelected = selectedColorHex.equals(hex, ignoreCase = true)
                    val swatchColor = try {
                        Color(android.graphics.Color.parseColor(hex))
                    } catch (_: Exception) {
                        Color.White
                    }

                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(swatchColor)
                            .border(
                                width = if (isSelected) 3.dp else 1.dp,
                                color = if (isSelected) QYellowPrimary else Color.White.copy(alpha = 0.3f),
                                shape = CircleShape
                            )
                            .clickable {
                                selectedColorHex = hex
                                currentTool = DrawingTool.PENCIL
                            }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Attach / Export Button
            Button(
                onClick = {
                    val artData = PixelArtData(
                        gridSize = gridSize,
                        pixels = drawnPixels.toList(),
                        colors = colorMap.toMap()
                    )
                    onAttachArt(artData)
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = QYellowPrimary,
                    contentColor = QOnYellow
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Attach Pixel Art (${drawnPixels.size} pixels)", fontWeight = FontWeight.Bold)
            }
        }
    }
}
