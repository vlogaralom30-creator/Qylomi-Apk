package com.example.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Post(
    @Json(name = "id") val id: String = "",
    @Json(name = "user_id") val userId: String = "",
    @Json(name = "group_id") val groupId: String? = null,
    @Json(name = "type") val type: String? = "Post",
    @Json(name = "title") val title: String? = null,
    @Json(name = "content") val content: String = "",
    @Json(name = "external_url") val externalUrl: String? = null,
    @Json(name = "appreciations_count") val appreciationsCount: Int = 0,
    @Json(name = "replies_count") val repliesCount: Int = 0,
    @Json(name = "relays_count") val relaysCount: Int = 0,
    @Json(name = "created_at") val createdAt: String? = null,
    @Json(name = "updated_at") val updatedAt: String? = null,
    @Json(name = "attachments") val attachments: List<PostAttachment>? = emptyList(),
    @Json(name = "video_preview_urls") val videoPreviewUrls: List<String>? = emptyList(),
    @Json(name = "profiles") val authorProfile: UserProfile? = null,
    // Client-side interactions tracked from database
    val isLikedByMe: Boolean = false,
    val isBookmarkedByMe: Boolean = false
)

@JsonClass(generateAdapter = true)
data class PostAttachment(
    @Json(name = "id") val id: String? = null,
    @Json(name = "name") val name: String? = null,
    @Json(name = "url") val url: String = "",
    @Json(name = "size") val size: Long? = 0,
    @Json(name = "type") val type: String? = null,
    @Json(name = "category") val category: String? = "other"
)

@JsonClass(generateAdapter = true)
data class CreatePostRequest(
    @Json(name = "user_id") val userId: String,
    @Json(name = "type") val type: String = "Post",
    @Json(name = "title") val title: String? = null,
    @Json(name = "content") val content: String,
    @Json(name = "external_url") val externalUrl: String? = null,
    @Json(name = "group_id") val groupId: String? = null,
    @Json(name = "attachments") val attachments: List<PostAttachment> = emptyList()
)

// Table: post_reactions
@JsonClass(generateAdapter = true)
data class PostReaction(
    @Json(name = "id") val id: String? = null,
    @Json(name = "post_id") val postId: String,
    @Json(name = "user_id") val userId: String,
    @Json(name = "reaction_type") val reactionType: String = "appreciate",
    @Json(name = "created_at") val createdAt: String? = null
)

// Table: comments
@JsonClass(generateAdapter = true)
data class Comment(
    @Json(name = "id") val id: String = "",
    @Json(name = "post_id") val postId: String = "",
    @Json(name = "user_id") val userId: String = "",
    @Json(name = "parent_id") val parentId: String? = null,
    @Json(name = "content") val content: String = "",
    @Json(name = "created_at") val createdAt: String? = null,
    @Json(name = "updated_at") val updatedAt: String? = null,
    @Json(name = "profiles") val authorProfile: UserProfile? = null
)

@JsonClass(generateAdapter = true)
data class CreateCommentRequest(
    @Json(name = "post_id") val postId: String,
    @Json(name = "user_id") val userId: String,
    @Json(name = "content") val content: String,
    @Json(name = "parent_id") val parentId: String? = null
)

// Table: saved_posts
@JsonClass(generateAdapter = true)
data class SavedPost(
    @Json(name = "id") val id: String = "",
    @Json(name = "post_id") val postId: String = "",
    @Json(name = "user_id") val userId: String = "",
    @Json(name = "created_at") val createdAt: String? = null,
    @Json(name = "posts") val post: Post? = null
)

@JsonClass(generateAdapter = true)
data class CreateSavedPostRequest(
    @Json(name = "post_id") val postId: String,
    @Json(name = "user_id") val userId: String
)

// Table: follows
@JsonClass(generateAdapter = true)
data class Follow(
    @Json(name = "id") val id: String = "",
    @Json(name = "follower_id") val followerId: String = "",
    @Json(name = "following_id") val followingId: String = "",
    @Json(name = "created_at") val createdAt: String? = null
)

@JsonClass(generateAdapter = true)
data class CreateFollowRequest(
    @Json(name = "follower_id") val followerId: String,
    @Json(name = "following_id") val followingId: String
)

// Table: notifications
@JsonClass(generateAdapter = true)
data class NotificationItem(
    @Json(name = "id") val id: String = "",
    @Json(name = "recipient_id") val recipientId: String = "",
    @Json(name = "actor_id") val actorId: String? = null,
    @Json(name = "type") val type: String = "appreciate",
    @Json(name = "post_id") val postId: String? = null,
    @Json(name = "comment_id") val commentId: String? = null,
    @Json(name = "read") val read: Boolean = false,
    @Json(name = "created_at") val createdAt: String? = null,
    @Json(name = "actor") val actorProfile: UserProfile? = null
) {
    val displayMessage: String
        get() {
            val name = actorProfile?.effectiveDisplayName ?: "A creator"
            return when (type) {
                "appreciate" -> "$name appreciated your post"
                "reply" -> "$name replied to your discussion"
                "relay" -> "$name shared your post"
                "track" -> "$name is now tracking your profile"
                else -> "$name interacted with your profile"
            }
        }
}
