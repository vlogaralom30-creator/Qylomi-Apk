package com.example.adshield.model

import java.util.UUID

sealed class RequestDecision {
    abstract val url: String
    abstract val host: String
    abstract val reason: String
    abstract val timestamp: Long

    data class Allowed(
        override val url: String,
        override val host: String,
        override val reason: String,
        override val timestamp: Long = System.currentTimeMillis(),
        val isPermittedAd: Boolean = false
    ) : RequestDecision()

    data class Blocked(
        override val url: String,
        override val host: String,
        override val reason: String,
        override val timestamp: Long = System.currentTimeMillis()
    ) : RequestDecision()
}

data class InterceptedLogEntry(
    val id: String = UUID.randomUUID().toString(),
    val url: String,
    val host: String,
    val isBlocked: Boolean,
    val reason: String,
    val timestamp: Long = System.currentTimeMillis()
)
