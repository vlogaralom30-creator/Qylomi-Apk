package com.example.util

import com.example.data.model.FeedTab
import com.example.data.model.Post
import com.example.video.VideoPlatformRegistry

/**
 * Pure functions mirroring the website feed presets and filters.
 */
object FeedPresets {

    private val BENGALI_RANGE = '\u0980'..'\u09FF'

    /**
     * Checks whether the given string contains at least one Bengali Unicode character (U+0980..U+09FF).
     */
    fun hasBengaliCharacters(text: String?): Boolean {
        if (text.isNullOrBlank()) return false
        return text.any { it in BENGALI_RANGE }
    }

    /**
     * Identifies Bangla posts:
     * - Title, content, or tag name contains Bengali characters (Unicode range U+0980..U+09FF)
     * - Or any tag contains "bangla" (case-insensitive)
     */
    fun isBanglaPost(post: Post): Boolean {
        if (hasBengaliCharacters(post.title)) return true
        if (hasBengaliCharacters(post.content)) return true

        // Check sub-table tags & parent attachments
        post.videoPosts?.forEach { vp ->
            if (hasBengaliCharacters(vp.title)) return true
            if (vp.tags?.any { hasBengaliCharacters(it) || it.contains("bangla", ignoreCase = true) } == true) return true
        }
        post.photoPosts?.forEach { pp ->
            if (hasBengaliCharacters(pp.caption)) return true
            if (pp.tags?.any { hasBengaliCharacters(it) || it.contains("bangla", ignoreCase = true) } == true) return true
        }
        post.textPosts?.forEach { tp ->
            if (hasBengaliCharacters(tp.caption)) return true
            if (tp.tags?.any { hasBengaliCharacters(it) || it.contains("bangla", ignoreCase = true) } == true) return true
        }

        // Hashtags extracted from content or title
        val tagsInContent = extractHashtags(post.content) + extractHashtags(post.title ?: "")
        if (tagsInContent.any { hasBengaliCharacters(it) || it.contains("bangla", ignoreCase = true) }) {
            return true
        }

        return false
    }

    /**
     * Identifies Video posts:
     * - Has a row in video_posts
     * - Or external_url parses as supported video link (via VideoPlatformRegistry)
     * - Or video_preview_urls is not empty
     * - Or attachments contains category "video"
     */
    fun isVideoPost(post: Post): Boolean {
        if (!post.videoPosts.isNullOrEmpty()) return true
        if (!post.videoPreviewUrls.isNullOrEmpty()) return true
        if (post.attachments?.any { it.category.equals("video", ignoreCase = true) || it.type?.startsWith("video/") == true } == true) {
            return true
        }
        val url = post.externalUrl
        if (!url.isNullOrBlank()) {
            val handler = VideoPlatformRegistry.resolve(url)
            if (handler.platformId != "direct" || VideoEmbedUtils.isIframeEmbedCode(url)) {
                return true
            }
            if (url.endsWith(".mp4", ignoreCase = true) || url.endsWith(".m3u8", ignoreCase = true) || url.endsWith(".webm", ignoreCase = true)) {
                return true
            }
        }
        return false
    }

    /**
     * Popularity / Trending score formula matching website:
     * appreciations_count * 2 + replies_count * 3
     */
    fun trendingScore(post: Post): Long {
        return (post.appreciationsCount.toLong() * 2L) + (post.repliesCount.toLong() * 3L)
    }

    /**
     * Helper to extract hashtags from text.
     */
    fun extractHashtags(text: String): List<String> {
        val regex = Regex("#(\\w+)")
        return regex.findAll(text).map { it.groupValues[1].lowercase().trim() }.toList()
    }

    /**
     * Applies preset filtering and ordering for a given tab.
     */
    fun applyPreset(
        posts: List<Post>,
        tab: FeedTab,
        followedUserIds: Set<String> = emptySet()
    ): List<Post> {
        return when (tab) {
            FeedTab.FOR_YOU -> posts // Server already ordered
            FeedTab.LATEST -> posts.sortedByDescending { it.createdAt ?: "" }
            FeedTab.TRENDING -> posts.sortedWith(
                compareByDescending<Post> { trendingScore(it) }
                    .thenByDescending { it.createdAt ?: "" }
            )
            FeedTab.FOLLOWING -> posts.filter { followedUserIds.contains(it.userId) }
                .sortedByDescending { it.createdAt ?: "" }
            FeedTab.VIDEOS -> posts.filter { isVideoPost(it) }
                .sortedByDescending { it.createdAt ?: "" }
            FeedTab.BANGLA -> posts.filter { isBanglaPost(it) }
                .sortedByDescending { it.createdAt ?: "" }
        }
    }
}
