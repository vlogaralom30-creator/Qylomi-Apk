package com.example.data.model

data class SavedAccount(
    val email: String,
    val pass: String,
    val displayName: String,
    val username: String,
    val avatarUrl: String? = null
)
