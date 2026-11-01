package com.example.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * The 8 strictly permitted user event types tracked by the feed ranking algorithm.
 */
enum class UserEventType(val value: String) {
    IMPRESSION("impression"),
    OPEN("open"),
    LIKE("like"),
    COMMENT("comment"),
    RELAY("relay"),
    BOOKMARK("bookmark"),
    SHARE("share"),
    HIDE("hide");

    companion object {
        fun fromValue(v: String?): UserEventType? = entries.find { it.value.equals(v, ignoreCase = true) }
    }
}

/**
 * Request payload for PostgreSQL RPC `get_feed(p_limit, p_exclude)`.
 */
@JsonClass(generateAdapter = true)
data class GetFeedRpcRequest(
    @Json(name = "p_limit") val limit: Int = 20,
    @Json(name = "p_exclude") val exclude: List<String> = emptyList()
)

/**
 * Result row from `get_feed` RPC.
 */
@JsonClass(generateAdapter = true)
data class FeedRankItem(
    @Json(name = "post_id") val postId: String = "",
    @Json(name = "score") val score: Double = 0.0
)

/**
 * Event row sent in batch array to `/rest/v1/user_events`.
 * Notice: user_id is deliberately omitted so Supabase injects auth.uid().
 */
@JsonClass(generateAdapter = true)
data class UserEventRecord(
    @Json(name = "post_id") val postId: String,
    @Json(name = "event_type") val eventType: String
)

/**
 * Feed navigation presets mirroring the website feed tabs.
 */
enum class FeedTab(val displayName: String) {
    FOR_YOU("For You"),
    LATEST("Latest"),
    TRENDING("Trending"),
    FOLLOWING("Following"),
    VIDEOS("Videos"),
    BANGLA("Bangla");

    companion object {
        fun fromName(name: String?): FeedTab =
            entries.find { it.name.equals(name, ignoreCase = true) || it.displayName.equals(name, ignoreCase = true) } ?: FOR_YOU
    }
}
