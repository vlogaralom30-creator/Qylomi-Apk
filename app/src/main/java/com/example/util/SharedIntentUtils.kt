package com.example.util

import android.content.Intent
import android.net.Uri
import com.example.video.VideoPlatformRegistry
import java.util.regex.Pattern

data class SharedContentResult(
    val url: String? = null,
    val rawText: String? = null,
    val mediaUri: String? = null,
    val platformId: String = "general",
    val platformDisplayName: String = "Shared Link",
    val isMediaFile: Boolean = false
)

object SharedIntentUtils {

    private val URL_PATTERN = Pattern.compile(
        "(https?://[a-zA-Z0-9\\-._~:/?#\\[\\]@!$&'()*+,;=%]+)",
        Pattern.CASE_INSENSITIVE
    )

    /**
     * Parses an incoming Android [Intent] (e.g. ACTION_SEND or ACTION_SEND_MULTIPLE)
     * and extracts shared URLs, raw text copy, or attached media URIs.
     */
    fun parseSharedIntent(intent: Intent?): SharedContentResult? {
        if (intent == null) return null
        val action = intent.action ?: return null

        if (action != Intent.ACTION_SEND && action != Intent.ACTION_SEND_MULTIPLE) {
            return null
        }

        // 1. Check for shared text or URLs
        val extraText = intent.getStringExtra(Intent.EXTRA_TEXT)
            ?: intent.getStringExtra(Intent.EXTRA_SUBJECT)
            ?: intent.dataString

        if (!extraText.isNullOrBlank()) {
            val extractedUrl = findFirstUrl(extraText)
            if (extractedUrl != null) {
                val handler = VideoPlatformRegistry.resolve(extractedUrl)
                return SharedContentResult(
                    url = extractedUrl,
                    rawText = extraText,
                    platformId = handler.platformId,
                    platformDisplayName = handler.displayName,
                    isMediaFile = false
                )
            } else {
                return SharedContentResult(
                    rawText = extraText,
                    platformId = "text",
                    platformDisplayName = "Text Share",
                    isMediaFile = false
                )
            }
        }

        // 2. Check for shared media file streams (Image / Video)
        val streamUri: Uri? = try {
            intent.getParcelableExtra(Intent.EXTRA_STREAM) ?: intent.data
        } catch (_: Exception) {
            null
        }

        if (streamUri != null) {
            val type = intent.type ?: ""
            val isVideo = type.startsWith("video/")
            val isImage = type.startsWith("image/")
            val platform = if (isVideo) "video_file" else if (isImage) "image_file" else "file"

            return SharedContentResult(
                mediaUri = streamUri.toString(),
                platformId = platform,
                platformDisplayName = if (isVideo) "Shared Video" else "Shared Image",
                isMediaFile = true
            )
        }

        return null
    }

    /**
     * Regex helper to find the first http or https URL in a string.
     */
    fun findFirstUrl(text: String): String? {
        val matcher = URL_PATTERN.matcher(text)
        if (matcher.find()) {
            val found = matcher.group(1) ?: return null
            // Strip trailing punctuation if accidentally matched
            return found.trimEnd('.', ',', ')', ']', '}', '!', ';', '"', '\'')
        }
        return null
    }
}
