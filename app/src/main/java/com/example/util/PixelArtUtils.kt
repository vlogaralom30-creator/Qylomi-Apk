package com.example.util

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import com.example.data.model.PixelArtData
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.util.regex.Pattern
import kotlin.math.abs

object PixelArtUtils {

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val adapter = moshi.adapter(PixelArtData::class.java)

    // Palette Definitions
    val pico8Palette = listOf(
        "#000000", "#1D2B53", "#7E2553", "#008751",
        "#AB5236", "#5F574F", "#C2C3C7", "#FFF1E8",
        "#FF004D", "#FFA300", "#FFEC27", "#00E436",
        "#29ADFF", "#83769C", "#FF77A8", "#FFCCAA"
    )

    val cyberNeonPalette = listOf(
        "#0F2027", "#00E5FF", "#FF007F", "#7928CA",
        "#FF00D4", "#00FF66", "#FFE600", "#FFFFFF"
    )

    val gameboyPalette = listOf(
        "#0F380F", "#306230", "#8BAC0F", "#9BBC0F"
    )

    val pastelPalette = listOf(
        "#FFB3BA", "#FFDFBA", "#FFFFBA", "#BAFFC9",
        "#BAE1FF", "#E8AEFF", "#FFC6FF", "#333333"
    )

    val classic16Palette = listOf(
        "#000000", "#FFFFFF", "#808080", "#C0C0C0",
        "#800000", "#FF0000", "#808000", "#FFFF00",
        "#008000", "#00FF00", "#008080", "#00FFFF",
        "#000080", "#0000FF", "#800080", "#FF00FF"
    )

    // Mathematical Coordinate Conversions
    fun indexToX(index: Int, gridSize: Int): Int = index % gridSize
    fun indexToY(index: Int, gridSize: Int): Int = index / gridSize
    fun xyToIndex(x: Int, y: Int, gridSize: Int): Int = (y * gridSize) + x

    // Bresenham's Integer Line Algorithm (prevents touch gaps during rapid movement)
    fun getLinePixelIndices(x0: Int, y0: Int, x1: Int, y1: Int, gridSize: Int): List<Int> {
        val result = mutableListOf<Int>()
        var x = x0
        var y = y0
        val dx = abs(x1 - x0)
        val dy = abs(y1 - y0)
        val sx = if (x0 < x1) 1 else -1
        val sy = if (y0 < y1) 1 else -1
        var err = dx - dy

        while (true) {
            if (x in 0 until gridSize && y in 0 until gridSize) {
                result.add(xyToIndex(x, y, gridSize))
            }
            if (x == x1 && y == y1) break
            val e2 = 2 * err
            if (e2 > -dy) {
                err -= dy
                x += sx
            }
            if (e2 < dx) {
                err += dx
                y += sy
            }
        }
        return result
    }

    // Brush Footprint Calculation (1px, 2px, 3px, 4px)
    fun getBrushIndices(centerX: Int, centerY: Int, sizePx: Int, gridSize: Int): List<Int> {
        val list = mutableListOf<Int>()
        when (sizePx) {
            1 -> {
                if (centerX in 0 until gridSize && centerY in 0 until gridSize) {
                    list.add(xyToIndex(centerX, centerY, gridSize))
                }
            }
            2 -> {
                for (dy in 0..1) {
                    for (dx in 0..1) {
                        val cx = centerX + dx
                        val cy = centerY + dy
                        if (cx in 0 until gridSize && cy in 0 until gridSize) {
                            list.add(xyToIndex(cx, cy, gridSize))
                        }
                    }
                }
            }
            3 -> {
                for (dy in -1..1) {
                    for (dx in -1..1) {
                        val cx = centerX + dx
                        val cy = centerY + dy
                        if (cx in 0 until gridSize && cy in 0 until gridSize) {
                            list.add(xyToIndex(cx, cy, gridSize))
                        }
                    }
                }
            }
            else -> { // 4px
                for (dy in -1..2) {
                    for (dx in -1..2) {
                        val cx = centerX + dx
                        val cy = centerY + dy
                        if (cx in 0 until gridSize && cy in 0 until gridSize) {
                            list.add(xyToIndex(cx, cy, gridSize))
                        }
                    }
                }
            }
        }
        return list
    }

    // HTML Trailer Comment Serializer & Parser (qylomi:pixelart:v1)
    fun extractPixelArt(content: String): PixelArtData? {
        val pattern = Pattern.compile("<!--\\s*qylomi:pixelart:v1\\s*(\\{.*?\\})\\s*-->", Pattern.DOTALL)
        val matcher = pattern.matcher(content)
        if (matcher.find()) {
            val jsonStr = matcher.group(1)
            return try {
                adapter.fromJson(jsonStr)
            } catch (_: Exception) {
                null
            }
        }
        return null
    }

    fun injectPixelArt(content: String, data: PixelArtData): String {
        val clean = content.replace(Regex("<!--\\s*qylomi:pixelart:v1\\s*\\{.*?\\}\\s*-->", RegexOption.DOT_MATCHES_ALL), "").trim()
        val jsonStr = try {
            adapter.toJson(data)
        } catch (_: Exception) {
            ""
        }
        return if (jsonStr.isNotBlank()) {
            "$clean\n\n<!-- qylomi:pixelart:v1 $jsonStr -->"
        } else {
            clean
        }
    }

    fun stripPixelArtComment(content: String): String {
        return content.replace(Regex("<!--\\s*qylomi:pixelart:v1\\s*\\{.*?\\}\\s*-->", RegexOption.DOT_MATCHES_ALL), "").trim()
    }

    // High Resolution Bitmap Renderer
    fun renderToBitmap(data: PixelArtData, targetSizePx: Int = 512): Bitmap {
        val bitmap = Bitmap.createBitmap(targetSizePx, targetSizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }

        // Background
        canvas.drawColor(Color.parseColor("#0F172A"))

        val gridSize = data.gridSize.coerceAtLeast(8)
        val cellSize = targetSizePx.toFloat() / gridSize

        // Render pixels
        val pixels = data.pixels
        val colorsMap = data.colors ?: emptyMap()

        pixels.forEach { index ->
            val x = indexToX(index, gridSize)
            val y = indexToY(index, gridSize)
            val hexColor = colorsMap[index.toString()] ?: "#FFFFFF"
            paint.color = try { Color.parseColor(hexColor) } catch (_: Exception) { Color.WHITE }

            val left = x * cellSize
            val top = y * cellSize
            canvas.drawRect(left, top, left + cellSize, top + cellSize, paint)
        }

        return bitmap
    }
}
