package com.example.util

object Constants {
    const val WEB_BASE_URL = "https://qylomi.app"

    fun buildPostWebUrl(postId: String): String {
        return "$WEB_BASE_URL/#/idea/$postId"
    }

    fun buildShareText(title: String?, content: String, postId: String, originalVideoUrl: String? = null): String {
        val cleanExcerpt = stripMarkupAndTokens(content).take(120)
        val header = title?.takeIf { it.isNotBlank() } ?: cleanExcerpt
        val url = buildPostWebUrl(postId)
        return if (!originalVideoUrl.isNullOrBlank()) {
            "$header\n\nVideo: $originalVideoUrl\n\n$url"
        } else {
            "$header\n\n$url"
        }
    }

    fun stripMarkupAndTokens(raw: String): String {
        return raw.replace(Regex("<!--[\\s\\S]*?-->"), "")
            .replace(Regex("\\[PIXEL_ART_DATA[\\s\\S]*?\\]"), "")
            .trim()
    }
}
