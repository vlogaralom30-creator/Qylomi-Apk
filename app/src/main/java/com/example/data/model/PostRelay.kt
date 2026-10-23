package com.example.data.model
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class PostRelay(
    @Json(name = "id") val id: String = "",
    @Json(name = "post_id") val postId: String = "",
    @Json(name = "user_id") val userId: String = "",
    @Json(name = "quote") val quote: String? = null,
    @Json(name = "created_at") val createdAt: String? = null
)
