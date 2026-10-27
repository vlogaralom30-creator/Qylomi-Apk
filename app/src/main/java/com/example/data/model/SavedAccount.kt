package com.example.data.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class SavedAccount(
    val email: String,
    val username: String,
    val displayName: String,
    val avatarUrl: String? = null,
    val refreshToken: String? = null,
    val accessToken: String? = null,
    val lastActiveTimestamp: Long = System.currentTimeMillis()
)
