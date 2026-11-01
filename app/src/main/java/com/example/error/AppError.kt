package com.example.error

data class AppError(
    val id: String = "ERR-${(100000..999999).random()}",
    val category: AppErrorCategory,
    val title: String,
    val userFriendlyMessage: String,
    val creatorDiagnosticMessage: String,
    val technicalCause: String? = null,
    val suggestedAction: String? = null,
    val affectedResource: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val stackTrace: String? = null
)
