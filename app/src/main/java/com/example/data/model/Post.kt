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
    @Json(name = "video_posts") val videoPosts: List<VideoPost>? = emptyList(),
    @Json(name = "photo_posts") val photoPosts: List<PhotoPost>? = emptyList(),
    @Json(name = "text_posts") val textPosts: List<TextPost>? = emptyList(),
    @Json(name = "pixel_art") val pixelArt: PixelArtData? = null,
    @Json(name = "allow_comments") val allowComments: Boolean = true,
    @Json(name = "is_pinned") val isPinned: Boolean = false,
    @Json(name = "pinned_at") val pinnedAt: String? = null,
    // Client-side interactions tracked from database
    val isLikedByMe: Boolean = false,
    val isBookmarkedByMe: Boolean = false
) {
    val videoDetails: VideoPost? get() = videoPosts?.firstOrNull()
    val photoDetails: PhotoPost? get() = photoPosts?.firstOrNull()
    val textDetails: TextPost? get() = textPosts?.firstOrNull()

    val effectivePixelArt: PixelArtData?
        get() = pixelArt ?: com.example.util.PixelArtUtils.extractPixelArt(content)

    val isEdited: Boolean
        get() {
            if (createdAt.isNullOrBlank() || updatedAt.isNullOrBlank()) return false
            return try {
                val created = java.time.Instant.parse(createdAt).toEpochMilli()
                val updated = java.time.Instant.parse(updatedAt).toEpochMilli()
                (updated - created) > 60000L
            } catch (_: Exception) {
                false
            }
        }
}

// Sub-table model: video_posts
@JsonClass(generateAdapter = true)
data class VideoPost(
    @Json(name = "id") val id: String = "",
    @Json(name = "post_id") val postId: String = "",
    @Json(name = "external_id") val externalId: String? = null,
    @Json(name = "title") val title: String? = null,
    @Json(name = "thumbnail_url") val thumbnailUrl: String? = null,
    @Json(name = "video_url") val videoUrl: String = "",
    @Json(name = "embed_url") val embedUrl: String? = null,
    @Json(name = "direct_url") val directUrl: String? = null,
    @Json(name = "platform") val platform: String? = "direct",
    @Json(name = "duration_seconds") val durationSeconds: Int? = null,
    @Json(name = "width") val width: Int? = null,
    @Json(name = "height") val height: Int? = null,
    @Json(name = "is_vertical") val isVertical: Boolean? = false,
    @Json(name = "category") val category: String? = null,
    @Json(name = "tags") val tags: List<String>? = emptyList(),
    @Json(name = "created_at") val createdAt: String? = null,
    @Json(name = "posts") val parentPost: Post? = null
)

@JsonClass(generateAdapter = true)
data class CreateVideoPostRequest(
    @Json(name = "post_id") val postId: String,
    @Json(name = "external_id") val externalId: String? = null,
    @Json(name = "title") val title: String? = null,
    @Json(name = "thumbnail_url") val thumbnailUrl: String? = null,
    @Json(name = "video_url") val videoUrl: String,
    @Json(name = "embed_url") val embedUrl: String? = null,
    @Json(name = "direct_url") val directUrl: String? = null,
    @Json(name = "platform") val platform: String? = "direct",
    @Json(name = "duration_seconds") val durationSeconds: Int? = null,
    @Json(name = "width") val width: Int? = null,
    @Json(name = "height") val height: Int? = null,
    @Json(name = "is_vertical") val isVertical: Boolean? = false,
    @Json(name = "category") val category: String? = null,
    @Json(name = "tags") val tags: List<String>? = emptyList()
)

// Sub-table model: photo_posts
@JsonClass(generateAdapter = true)
data class PhotoPost(
    @Json(name = "id") val id: String = "",
    @Json(name = "post_id") val postId: String = "",
    @Json(name = "caption") val caption: String? = null,
    @Json(name = "photo_urls") val photoUrls: List<String> = emptyList(),
    @Json(name = "category") val category: String? = null,
    @Json(name = "tags") val tags: List<String>? = emptyList(),
    @Json(name = "created_at") val createdAt: String? = null
)

@JsonClass(generateAdapter = true)
data class CreatePhotoPostRequest(
    @Json(name = "post_id") val postId: String,
    @Json(name = "caption") val caption: String? = null,
    @Json(name = "photo_urls") val photoUrls: List<String>,
    @Json(name = "category") val category: String? = null,
    @Json(name = "tags") val tags: List<String>? = emptyList()
)

// Sub-table model: text_posts
@JsonClass(generateAdapter = true)
data class TextPost(
    @Json(name = "id") val id: String = "",
    @Json(name = "post_id") val postId: String = "",
    @Json(name = "caption") val caption: String = "",
    @Json(name = "bg_color") val bgColor: String? = "preset_default",
    @Json(name = "font_style") val fontStyle: String? = "default",
    @Json(name = "category") val category: String? = null,
    @Json(name = "tags") val tags: List<String>? = emptyList(),
    @Json(name = "created_at") val createdAt: String? = null
)

@JsonClass(generateAdapter = true)
data class CreateTextPostRequest(
    @Json(name = "post_id") val postId: String,
    @Json(name = "caption") val caption: String,
    @Json(name = "bg_color") val bgColor: String? = "preset_default",
    @Json(name = "font_style") val fontStyle: String? = "default",
    @Json(name = "category") val category: String? = null,
    @Json(name = "tags") val tags: List<String>? = emptyList()
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
    @Json(name = "likes_count") val likesCount: Int = 0,
    @Json(name = "created_at") val createdAt: String? = null,
    @Json(name = "updated_at") val updatedAt: String? = null,
    @Json(name = "profiles") val authorProfile: UserProfile? = null,
    val isLikedByMe: Boolean = false
) {
    val isEdited: Boolean
        get() {
            if (createdAt.isNullOrBlank() || updatedAt.isNullOrBlank()) return false
            return try {
                val created = java.time.Instant.parse(createdAt).toEpochMilli()
                val updated = java.time.Instant.parse(updatedAt).toEpochMilli()
                (updated - created) > 60000L
            } catch (_: Exception) {
                false
            }
        }
}


@JsonClass(generateAdapter = true)
data class UpdateCommentRequest(
    @Json(name = "content") val content: String
)

@JsonClass(generateAdapter = true)
data class CreateReportRequest(
    @Json(name = "reporter_id") val reporterId: String,
    @Json(name = "post_id") val postId: String? = null,
    @Json(name = "comment_id") val commentId: String? = null,
    @Json(name = "reason") val reason: String,
    @Json(name = "details") val details: String? = null,
    @Json(name = "status") val status: String = "pending"
)

@JsonClass(generateAdapter = true)
data class CreateHiddenPostRequest(
    @Json(name = "user_id") val userId: String,
    @Json(name = "post_id") val postId: String
)

@JsonClass(generateAdapter = true)
data class CommentLike(
    @Json(name = "id") val id: String = "",
    @Json(name = "comment_id") val commentId: String = "",
    @Json(name = "user_id") val userId: String = "",
    @Json(name = "created_at") val createdAt: String? = null
)

@JsonClass(generateAdapter = true)
data class CreateCommentLikeRequest(
    @Json(name = "comment_id") val commentId: String,
    @Json(name = "user_id") val userId: String
)

@JsonClass(generateAdapter = true)
data class PostReactionLiker(
    @Json(name = "user_id") val userId: String = "",
    @Json(name = "created_at") val createdAt: String? = null,
    @Json(name = "profiles") val userProfile: UserProfile? = null
)

@JsonClass(generateAdapter = true)
data class CommentLiker(
    @Json(name = "user_id") val userId: String = "",
    @Json(name = "created_at") val createdAt: String? = null,
    @Json(name = "profiles") val userProfile: UserProfile? = null
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
