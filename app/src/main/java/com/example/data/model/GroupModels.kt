package com.example.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class GroupCommunity(
    @Json(name = "id") val id: String = "",
    @Json(name = "name") val name: String = "",
    @Json(name = "slug") val slug: String = "",
    @Json(name = "description") val description: String? = null,
    @Json(name = "avatar_url") val avatarUrl: String? = null,
    @Json(name = "cover_url") val coverUrl: String? = null,
    @Json(name = "privacy") val privacy: String = "public",
    @Json(name = "category") val category: String? = "general",
    @Json(name = "rules") val rules: List<String>? = emptyList(),
    @Json(name = "creator_id") val creatorId: String? = null,
    @Json(name = "members_count") val membersCount: Int = 1,
    @Json(name = "posts_count") val postsCount: Int = 0,
    @Json(name = "created_at") val createdAt: String? = null,
    val isJoinedByMe: Boolean = false
)

@JsonClass(generateAdapter = true)
data class CreateGroupRequest(
    @Json(name = "name") val name: String,
    @Json(name = "slug") val slug: String,
    @Json(name = "description") val description: String? = null,
    @Json(name = "avatar_url") val avatarUrl: String? = null,
    @Json(name = "cover_url") val coverUrl: String? = null,
    @Json(name = "privacy") val privacy: String = "public",
    @Json(name = "category") val category: String? = "general",
    @Json(name = "creator_id") val creatorId: String
)

@JsonClass(generateAdapter = true)
data class GroupMember(
    @Json(name = "id") val id: String = "",
    @Json(name = "group_id") val groupId: String = "",
    @Json(name = "user_id") val userId: String = "",
    @Json(name = "role") val role: String = "member",
    @Json(name = "status") val status: String = "active",
    @Json(name = "joined_at") val joinedAt: String? = null
)

@JsonClass(generateAdapter = true)
data class JoinGroupRequest(
    @Json(name = "group_id") val groupId: String,
    @Json(name = "user_id") val userId: String,
    @Json(name = "role") val role: String = "member",
    @Json(name = "status") val status: String = "active"
)

@JsonClass(generateAdapter = true)
data class UpdatePostRequest(
    @Json(name = "title") val title: String? = null,
    @Json(name = "content") val content: String? = null,
    @Json(name = "type") val type: String? = null,
    @Json(name = "external_url") val externalUrl: String? = null,
    @Json(name = "allow_comments") val allowComments: Boolean? = null,
    @Json(name = "is_pinned") val isPinned: Boolean? = null,
    @Json(name = "pinned_at") val pinnedAt: String? = null
)
