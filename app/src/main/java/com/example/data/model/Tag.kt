package com.example.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Tag(
    @Json(name = "id") val id: String = "",
    @Json(name = "name") val name: String = "",
    @Json(name = "posts_count") val postsCount: Int = 0,
    @Json(name = "created_at") val createdAt: String? = null
)

@JsonClass(generateAdapter = true)
data class SearchRpcRequest(
    @Json(name = "q") val q: String,
    @Json(name = "lim") val lim: Int = 20,
    @Json(name = "off") val off: Int = 0
)

@JsonClass(generateAdapter = true)
data class PostSearchIdResult(
    @Json(name = "id") val id: String
)

@JsonClass(generateAdapter = true)
data class PostTagEmbed(
    @Json(name = "post_id") val postId: String
)
