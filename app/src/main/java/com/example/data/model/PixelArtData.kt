package com.example.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class PixelArtData(
    @Json(name = "gridSize") val gridSize: Int = 32,
    @Json(name = "pixels") val pixels: List<Int> = emptyList(),
    @Json(name = "colors") val colors: Map<String, String>? = null,
    @Json(name = "isAnimation") val isAnimation: Boolean = false,
    @Json(name = "fps") val fps: Double = 5.0,
    @Json(name = "frames") val frames: List<List<Int>>? = null,
    @Json(name = "frameColors") val frameColors: List<Map<String, String>>? = null,
    @Json(name = "createdAt") val createdAt: String? = null
)
