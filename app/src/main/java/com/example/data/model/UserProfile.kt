package com.example.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class UserProfile(
    @Json(name = "id") val id: String = "",
    @Json(name = "username") val username: String? = null,
    @Json(name = "display_name") val displayName: String? = null,
    @Json(name = "bio") val bio: String? = null,
    @Json(name = "website") val website: String? = null,
    @Json(name = "avatar_url") val avatarUrl: String? = null,
    @Json(name = "cover_url") val coverUrl: String? = null,
    @Json(name = "headline") val headline: String? = null,
    @Json(name = "age") val age: Int? = null,
    @Json(name = "gender") val gender: String? = null,
    @Json(name = "location") val location: String? = null,
    @Json(name = "work") val work: String? = null,
    @Json(name = "education") val education: String? = null,
    @Json(name = "skills") val skills: List<String>? = null,
    @Json(name = "social_links") val socialLinks: Map<String, String>? = null,
    @Json(name = "points") val points: Int? = 0,
    @Json(name = "creator_rank") val creatorRank: String? = "Novice Explorer",
    @Json(name = "creator_level") val creatorLevel: Int? = 1,
    @Json(name = "badges") val badges: List<String>? = emptyList(),
    @Json(name = "points_history") val pointsHistory: List<PointsHistoryItem>? = emptyList(),
    @Json(name = "created_at") val createdAt: String? = null,
    @Json(name = "updated_at") val updatedAt: String? = null
) {
    val effectiveDisplayName: String
        get() = displayName?.takeIf { it.isNotBlank() }
            ?: username?.takeIf { it.isNotBlank() }
            ?: "Q Creator"

    val effectiveUsername: String
        get() = username?.takeIf { it.isNotBlank() } ?: "user_${id.take(6)}"

    val initials: String
        get() = effectiveDisplayName.split(" ")
            .filter { it.isNotBlank() }
            .take(2)
            .map { it.first().uppercaseChar() }
            .joinToString("")
            .ifEmpty { "Q" }
}

@JsonClass(generateAdapter = true)
data class PointsHistoryItem(
    @Json(name = "id") val id: String? = null,
    @Json(name = "action") val action: String? = null,
    @Json(name = "points") val points: Int? = 0,
    @Json(name = "created_at") val createdAt: String? = null,
    @Json(name = "description") val description: String? = null,
    @Json(name = "reference_id") val referenceId: String? = null,
    @Json(name = "balance_after") val balanceAfter: Int? = null
)

@JsonClass(generateAdapter = true)
data class ProfileUpdateRequest(
    @Json(name = "username") val username: String? = null,
    @Json(name = "display_name") val displayName: String? = null,
    @Json(name = "bio") val bio: String? = null,
    @Json(name = "website") val website: String? = null,
    @Json(name = "avatar_url") val avatarUrl: String? = null,
    @Json(name = "cover_url") val coverUrl: String? = null,
    @Json(name = "headline") val headline: String? = null,
    @Json(name = "age") val age: Int? = null,
    @Json(name = "gender") val gender: String? = null,
    @Json(name = "location") val location: String? = null,
    @Json(name = "work") val work: String? = null,
    @Json(name = "education") val education: String? = null,
    @Json(name = "skills") val skills: List<String>? = null,
    @Json(name = "social_links") val socialLinks: Map<String, String>? = null,
    @Json(name = "updated_at") val updatedAt: String? = null
)
