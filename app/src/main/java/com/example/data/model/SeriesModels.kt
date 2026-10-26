package com.example.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

// ---------------------------------------------------------------------------
// 1. Content Categories
// ---------------------------------------------------------------------------

@JsonClass(generateAdapter = true)
data class ContentCategory(
    @Json(name = "id") val id: String = "",
    @Json(name = "slug") val slug: String = "",
    @Json(name = "name") val name: String = "",
    @Json(name = "sort_order") val sortOrder: Int = 0,
    @Json(name = "is_active") val isActive: Boolean = true,
    @Json(name = "created_at") val createdAt: String? = null,
    @Json(name = "updated_at") val updatedAt: String? = null
)

@JsonClass(generateAdapter = true)
data class CategoryRef(
    @Json(name = "id") val id: String? = null,
    @Json(name = "slug") val slug: String = "",
    @Json(name = "name") val name: String = ""
)

// ---------------------------------------------------------------------------
// 2. Series (Full row) & SeriesCard (Card JSON returned by RPCs)
// ---------------------------------------------------------------------------

@JsonClass(generateAdapter = true)
data class Series(
    @Json(name = "id") val id: String = "",
    @Json(name = "slug") val slug: String = "",
    @Json(name = "title") val title: String = "",
    @Json(name = "original_title") val originalTitle: String? = null,
    @Json(name = "description") val description: String? = null,
    @Json(name = "poster_url") val posterUrl: String? = null,
    @Json(name = "banner_url") val bannerUrl: String? = null,
    @Json(name = "trailer_url") val trailerUrl: String? = null,
    @Json(name = "category_id") val categoryId: String? = null,
    @Json(name = "category") val category: CategoryRef? = null,
    @Json(name = "genres") val genres: List<String> = emptyList(),
    @Json(name = "languages") val languages: List<String> = emptyList(),
    @Json(name = "subtitle_languages") val subtitleLanguages: List<String> = emptyList(),
    @Json(name = "rating") val rating: Double? = null,
    @Json(name = "age_rating") val ageRating: String? = null,
    @Json(name = "is_mature") val isMature: Boolean = false,
    @Json(name = "status") val status: String = "ongoing",
    @Json(name = "release_year") val releaseYear: Int? = null,
    @Json(name = "studio") val studio: String? = null,
    @Json(name = "total_episodes") val totalEpisodes: Int? = null,
    @Json(name = "is_published") val isPublished: Boolean = false,
    @Json(name = "is_featured") val isFeatured: Boolean = false,
    @Json(name = "is_blocked") val isBlocked: Boolean = false,
    @Json(name = "blocked_reason") val blockedReason: String? = null,
    @Json(name = "created_by") val createdBy: String? = null,
    // Read-only server triggers
    @Json(name = "avg_user_rating") val avgUserRating: Double? = null,
    @Json(name = "ratings_count") val ratingsCount: Int = 0,
    @Json(name = "likes_count") val likesCount: Int = 0,
    @Json(name = "comments_count") val commentsCount: Int = 0,
    @Json(name = "views_count") val viewsCount: Long = 0L,
    @Json(name = "episodes_count") val episodesCount: Int = 0,
    @Json(name = "last_episode_at") val lastEpisodeAt: String? = null,
    @Json(name = "created_at") val createdAt: String? = null,
    @Json(name = "updated_at") val updatedAt: String? = null
) {
    val effectiveBannerUrl: String?
        get() = bannerUrl?.takeIf { it.isNotBlank() } ?: posterUrl

    fun toCard(): SeriesCard = SeriesCard(
        id = id,
        slug = slug,
        title = title,
        originalTitle = originalTitle,
        description = description?.take(280),
        posterUrl = posterUrl,
        bannerUrl = bannerUrl,
        category = category,
        genres = genres,
        languages = languages,
        rating = rating,
        avgUserRating = avgUserRating,
        ratingsCount = ratingsCount,
        status = status,
        releaseYear = releaseYear,
        ageRating = ageRating,
        isMature = isMature,
        episodesCount = episodesCount,
        lastEpisodeAt = lastEpisodeAt,
        viewsCount = viewsCount,
        likesCount = likesCount,
        createdAt = createdAt
    )
}

@JsonClass(generateAdapter = true)
data class SeriesCard(
    @Json(name = "id") val id: String = "",
    @Json(name = "slug") val slug: String = "",
    @Json(name = "title") val title: String = "",
    @Json(name = "original_title") val originalTitle: String? = null,
    @Json(name = "description") val description: String? = null,
    @Json(name = "poster_url") val posterUrl: String? = null,
    @Json(name = "banner_url") val bannerUrl: String? = null,
    @Json(name = "category") val category: CategoryRef? = null,
    @Json(name = "genres") val genres: List<String> = emptyList(),
    @Json(name = "languages") val languages: List<String> = emptyList(),
    @Json(name = "rating") val rating: Double? = null,
    @Json(name = "avg_user_rating") val avgUserRating: Double? = null,
    @Json(name = "ratings_count") val ratingsCount: Int = 0,
    @Json(name = "status") val status: String = "ongoing",
    @Json(name = "release_year") val releaseYear: Int? = null,
    @Json(name = "age_rating") val ageRating: String? = null,
    @Json(name = "is_mature") val isMature: Boolean = false,
    @Json(name = "episodes_count") val episodesCount: Int = 0,
    @Json(name = "last_episode_at") val lastEpisodeAt: String? = null,
    @Json(name = "views_count") val viewsCount: Long = 0L,
    @Json(name = "likes_count") val likesCount: Int = 0,
    @Json(name = "created_at") val createdAt: String? = null
) {
    val effectiveBannerUrl: String?
        get() = bannerUrl?.takeIf { it.isNotBlank() } ?: posterUrl
}

// ---------------------------------------------------------------------------
// 3. Seasons & Episodes
// ---------------------------------------------------------------------------

@JsonClass(generateAdapter = true)
data class Season(
    @Json(name = "id") val id: String = "",
    @Json(name = "series_id") val seriesId: String = "",
    @Json(name = "season_number") val seasonNumber: Int = 1,
    @Json(name = "title") val title: String? = null,
    @Json(name = "description") val description: String? = null,
    @Json(name = "poster_url") val posterUrl: String? = null,
    @Json(name = "created_at") val createdAt: String? = null,
    @Json(name = "updated_at") val updatedAt: String? = null
)

@JsonClass(generateAdapter = true)
data class Episode(
    @Json(name = "id") val id: String = "",
    @Json(name = "series_id") val seriesId: String = "",
    @Json(name = "season_number") val seasonNumber: Int = 1,
    @Json(name = "episode_number") val episodeNumber: Int = 1,
    @Json(name = "title") val title: String? = null,
    @Json(name = "description") val description: String? = null,
    @Json(name = "thumbnail_url") val thumbnailUrl: String? = null,
    @Json(name = "duration_seconds") val durationSeconds: Int? = null,
    @Json(name = "air_date") val airDate: String? = null,
    @Json(name = "intro_start_sec") val introStartSec: Int? = null,
    @Json(name = "intro_end_sec") val introEndSec: Int? = null,
    @Json(name = "is_published") val isPublished: Boolean = true,
    @Json(name = "created_by") val createdBy: String? = null,
    // Read-only server triggers
    @Json(name = "likes_count") val likesCount: Int = 0,
    @Json(name = "comments_count") val commentsCount: Int = 0,
    @Json(name = "views_count") val viewsCount: Long = 0L,
    @Json(name = "created_at") val createdAt: String? = null,
    @Json(name = "updated_at") val updatedAt: String? = null,
    // Optional embed
    @Json(name = "series") val series: SeriesCard? = null,
    val watchedProgressPercent: Double = 0.0,
    val isCompleted: Boolean = false
)

@JsonClass(generateAdapter = true)
data class EpisodeSource(
    @Json(name = "id") val id: String = "",
    @Json(name = "episode_id") val episodeId: String = "",
    @Json(name = "label") val label: String = "Server 1",
    @Json(name = "url") val url: String = "",
    @Json(name = "platform") val platform: String? = null,
    @Json(name = "language") val language: String? = null,
    @Json(name = "quality") val quality: String? = null,
    @Json(name = "priority") val priority: Int = 0,
    @Json(name = "is_active") val isActive: Boolean = true,
    @Json(name = "fail_count") val failCount: Int = 0,
    @Json(name = "last_ok_at") val lastOkAt: String? = null,
    @Json(name = "created_at") val createdAt: String? = null,
    @Json(name = "updated_at") val updatedAt: String? = null
)

// ---------------------------------------------------------------------------
// 4. Shelves & Home Page Sections
// ---------------------------------------------------------------------------

@JsonClass(generateAdapter = true)
data class Shelf(
    @Json(name = "id") val id: String = "",
    @Json(name = "slug") val slug: String = "",
    @Json(name = "title") val title: String = "",
    @Json(name = "type") val type: String = "manual",
    @Json(name = "layout") val layout: String = "row",
    @Json(name = "config") val config: Map<String, Any?>? = null,
    @Json(name = "category_id") val categoryId: String? = null,
    @Json(name = "position") val position: Int = 0,
    @Json(name = "is_active") val isActive: Boolean = true,
    @Json(name = "max_items") val maxItems: Int = 15
)

@JsonClass(generateAdapter = true)
data class ShelfWithItems(
    @Json(name = "shelf") val shelf: Shelf,
    @Json(name = "items") val items: List<SeriesCard> = emptyList()
)

// ---------------------------------------------------------------------------
// 5. User Activity: Watch Progress, Ratings, Watchlist, Likes
// ---------------------------------------------------------------------------

@JsonClass(generateAdapter = true)
data class ContinueWatchingItem(
    @Json(name = "series") val series: SeriesCard? = null,
    @Json(name = "episode") val episode: Episode? = null,
    @Json(name = "position_sec") val positionSec: Int = 0,
    @Json(name = "duration_sec") val durationSec: Int = 0,
    @Json(name = "updated_at") val updatedAt: String? = null
)

@JsonClass(generateAdapter = true)
data class WatchProgress(
    @Json(name = "user_id") val userId: String = "",
    @Json(name = "episode_id") val episodeId: String = "",
    @Json(name = "series_id") val seriesId: String = "",
    @Json(name = "position_sec") val positionSec: Int = 0,
    @Json(name = "duration_sec") val durationSec: Int? = null,
    @Json(name = "completed") val completed: Boolean = false,
    @Json(name = "updated_at") val updatedAt: String? = null
)

@JsonClass(generateAdapter = true)
data class SeriesRating(
    @Json(name = "series_id") val seriesId: String = "",
    @Json(name = "user_id") val userId: String = "",
    @Json(name = "rating") val rating: Int = 1,
    @Json(name = "created_at") val createdAt: String? = null,
    @Json(name = "updated_at") val updatedAt: String? = null
)

@JsonClass(generateAdapter = true)
data class SeriesWatchlistItem(
    @Json(name = "user_id") val userId: String = "",
    @Json(name = "series_id") val seriesId: String = "",
    @Json(name = "created_at") val createdAt: String? = null,
    @Json(name = "series") val series: SeriesCard? = null
)

@JsonClass(generateAdapter = true)
data class SeriesLikeItem(
    @Json(name = "series_id") val seriesId: String = "",
    @Json(name = "user_id") val userId: String = "",
    @Json(name = "created_at") val createdAt: String? = null
)

@JsonClass(generateAdapter = true)
data class EpisodeLikeItem(
    @Json(name = "episode_id") val episodeId: String = "",
    @Json(name = "user_id") val userId: String = "",
    @Json(name = "created_at") val createdAt: String? = null
)

// ---------------------------------------------------------------------------
// 6. Comments & Threaded Discussions
// ---------------------------------------------------------------------------

@JsonClass(generateAdapter = true)
data class SeriesComment(
    @Json(name = "id") val id: String = "",
    @Json(name = "series_id") val seriesId: String = "",
    @Json(name = "episode_id") val episodeId: String? = null,
    @Json(name = "user_id") val userId: String = "",
    @Json(name = "parent_id") val parentId: String? = null,
    @Json(name = "content") val content: String = "",
    @Json(name = "likes_count") val likesCount: Int = 0,
    @Json(name = "created_at") val createdAt: String? = null,
    @Json(name = "updated_at") val updatedAt: String? = null,
    @Json(name = "profile") val profile: UserProfile? = null,
    val isLiked: Boolean = false
)

@JsonClass(generateAdapter = true)
data class SeriesCommentLike(
    @Json(name = "comment_id") val commentId: String = "",
    @Json(name = "user_id") val userId: String = "",
    @Json(name = "created_at") val createdAt: String? = null
)

// ---------------------------------------------------------------------------
// 7. Source Reports & Moderation
// ---------------------------------------------------------------------------

@JsonClass(generateAdapter = true)
data class SourceReportEpisodeEmbed(
    @Json(name = "episode_number") val episodeNumber: Int = 1,
    @Json(name = "series") val series: SeriesCard? = null
)

@JsonClass(generateAdapter = true)
data class SourceReportEmbed(
    @Json(name = "id") val id: String = "",
    @Json(name = "label") val label: String = "",
    @Json(name = "url") val url: String = "",
    @Json(name = "episode") val episode: SourceReportEpisodeEmbed? = null
)

@JsonClass(generateAdapter = true)
data class SourceReport(
    @Json(name = "id") val id: String = "",
    @Json(name = "source_id") val sourceId: String = "",
    @Json(name = "user_id") val userId: String = "",
    @Json(name = "reason") val reason: String = "broken",
    @Json(name = "details") val details: String? = null,
    @Json(name = "status") val status: String = "open",
    @Json(name = "handled_by") val handledBy: String? = null,
    @Json(name = "handled_at") val handledAt: String? = null,
    @Json(name = "created_at") val createdAt: String? = null,
    @Json(name = "source") val source: SourceReportEmbed? = null,
    @Json(name = "profile") val profile: UserProfile? = null
)

@JsonClass(generateAdapter = true)
data class ShelfItem(
    @Json(name = "shelf_id") val shelfId: String = "",
    @Json(name = "series_id") val seriesId: String = "",
    @Json(name = "position") val position: Int = 0,
    @Json(name = "created_at") val createdAt: String? = null
)

// ===========================================================================
// WRITE WHITELIST REQUEST BODIES (Authenticated mutations only)
// Note: Read-only and trigger-maintained columns are strictly omitted!
// ===========================================================================

@JsonClass(generateAdapter = true)
data class SeriesInsert(
    @Json(name = "age_rating") val ageRating: String? = null,
    @Json(name = "banner_url") val bannerUrl: String? = null,
    @Json(name = "category_id") val categoryId: String? = null,
    @Json(name = "created_by") val createdBy: String,
    @Json(name = "description") val description: String? = null,
    @Json(name = "genres") val genres: List<String> = emptyList(),
    @Json(name = "is_featured") val isFeatured: Boolean = false,
    @Json(name = "is_mature") val isMature: Boolean = false,
    @Json(name = "is_published") val isPublished: Boolean = false,
    @Json(name = "languages") val languages: List<String> = emptyList(),
    @Json(name = "original_title") val originalTitle: String? = null,
    @Json(name = "poster_url") val posterUrl: String? = null,
    @Json(name = "rating") val rating: Double? = null,
    @Json(name = "release_year") val releaseYear: Int? = null,
    @Json(name = "slug") val slug: String,
    @Json(name = "status") val status: String = "ongoing",
    @Json(name = "studio") val studio: String? = null,
    @Json(name = "subtitle_languages") val subtitleLanguages: List<String> = emptyList(),
    @Json(name = "title") val title: String,
    @Json(name = "total_episodes") val totalEpisodes: Int? = null,
    @Json(name = "trailer_url") val trailerUrl: String? = null
)

@JsonClass(generateAdapter = true)
data class SeriesUpdate(
    @Json(name = "age_rating") val ageRating: String? = null,
    @Json(name = "banner_url") val bannerUrl: String? = null,
    @Json(name = "category_id") val categoryId: String? = null,
    @Json(name = "description") val description: String? = null,
    @Json(name = "genres") val genres: List<String>? = null,
    @Json(name = "is_featured") val isFeatured: Boolean? = null,
    @Json(name = "is_mature") val isMature: Boolean? = null,
    @Json(name = "is_published") val isPublished: Boolean? = null,
    @Json(name = "languages") val languages: List<String>? = null,
    @Json(name = "original_title") val originalTitle: String? = null,
    @Json(name = "poster_url") val posterUrl: String? = null,
    @Json(name = "rating") val rating: Double? = null,
    @Json(name = "release_year") val releaseYear: Int? = null,
    @Json(name = "slug") val slug: String? = null,
    @Json(name = "status") val status: String? = null,
    @Json(name = "studio") val studio: String? = null,
    @Json(name = "subtitle_languages") val subtitleLanguages: List<String>? = null,
    @Json(name = "title") val title: String? = null,
    @Json(name = "total_episodes") val totalEpisodes: Int? = null,
    @Json(name = "trailer_url") val trailerUrl: String? = null,
    @Json(name = "is_blocked") val isBlocked: Boolean? = null,
    @Json(name = "blocked_reason") val blockedReason: String? = null
)

@JsonClass(generateAdapter = true)
data class EpisodeInsert(
    @Json(name = "air_date") val airDate: String? = null,
    @Json(name = "created_by") val createdBy: String,
    @Json(name = "description") val description: String? = null,
    @Json(name = "duration_seconds") val durationSeconds: Int? = null,
    @Json(name = "episode_number") val episodeNumber: Int,
    @Json(name = "intro_end_sec") val introEndSec: Int? = null,
    @Json(name = "intro_start_sec") val introStartSec: Int? = null,
    @Json(name = "is_published") val isPublished: Boolean = true,
    @Json(name = "season_number") val seasonNumber: Int = 1,
    @Json(name = "series_id") val seriesId: String,
    @Json(name = "thumbnail_url") val thumbnailUrl: String? = null,
    @Json(name = "title") val title: String? = null
)

@JsonClass(generateAdapter = true)
data class EpisodeUpdate(
    @Json(name = "air_date") val airDate: String? = null,
    @Json(name = "description") val description: String? = null,
    @Json(name = "duration_seconds") val durationSeconds: Int? = null,
    @Json(name = "episode_number") val episodeNumber: Int? = null,
    @Json(name = "intro_end_sec") val introEndSec: Int? = null,
    @Json(name = "intro_start_sec") val introStartSec: Int? = null,
    @Json(name = "is_published") val isPublished: Boolean? = null,
    @Json(name = "season_number") val seasonNumber: Int? = null,
    @Json(name = "thumbnail_url") val thumbnailUrl: String? = null,
    @Json(name = "title") val title: String? = null
)

@JsonClass(generateAdapter = true)
data class SourceInsert(
    @Json(name = "episode_id") val episodeId: String,
    @Json(name = "is_active") val isActive: Boolean = true,
    @Json(name = "label") val label: String = "Server 1",
    @Json(name = "language") val language: String? = null,
    @Json(name = "platform") val platform: String? = null,
    @Json(name = "priority") val priority: Int = 0,
    @Json(name = "quality") val quality: String? = null,
    @Json(name = "url") val url: String
)

@JsonClass(generateAdapter = true)
data class SourceUpdate(
    @Json(name = "is_active") val isActive: Boolean? = null,
    @Json(name = "label") val label: String? = null,
    @Json(name = "language") val language: String? = null,
    @Json(name = "platform") val platform: String? = null,
    @Json(name = "priority") val priority: Int? = null,
    @Json(name = "quality") val quality: String? = null,
    @Json(name = "url") val url: String? = null
)

@JsonClass(generateAdapter = true)
data class ProgressUpsert(
    @Json(name = "user_id") val userId: String,
    @Json(name = "episode_id") val episodeId: String,
    @Json(name = "series_id") val seriesId: String,
    @Json(name = "position_sec") val positionSec: Int,
    @Json(name = "duration_sec") val durationSec: Int? = null,
    @Json(name = "completed") val completed: Boolean = false
)

@JsonClass(generateAdapter = true)
data class CommentInsert(
    @Json(name = "content") val content: String,
    @Json(name = "episode_id") val episodeId: String? = null,
    @Json(name = "parent_id") val parentId: String? = null,
    @Json(name = "series_id") val seriesId: String,
    @Json(name = "user_id") val userId: String
)

@JsonClass(generateAdapter = true)
data class CommentUpdate(
    @Json(name = "content") val content: String
)

@JsonClass(generateAdapter = true)
data class RatingUpsert(
    @Json(name = "series_id") val seriesId: String,
    @Json(name = "user_id") val userId: String,
    @Json(name = "rating") val rating: Int
)

@JsonClass(generateAdapter = true)
data class ReportInsert(
    @Json(name = "details") val details: String? = null,
    @Json(name = "reason") val reason: String,
    @Json(name = "source_id") val sourceId: String,
    @Json(name = "user_id") val userId: String
)

@JsonClass(generateAdapter = true)
data class ReportUpdateStatus(
    @Json(name = "status") val status: String
)

@JsonClass(generateAdapter = true)
data class ShelfInsert(
    @Json(name = "slug") val slug: String,
    @Json(name = "title") val title: String,
    @Json(name = "type") val type: String,
    @Json(name = "layout") val layout: String = "row",
    @Json(name = "config") val config: Map<String, Any?> = emptyMap(),
    @Json(name = "category_id") val categoryId: String? = null,
    @Json(name = "position") val position: Int = 0,
    @Json(name = "is_active") val isActive: Boolean = true,
    @Json(name = "max_items") val maxItems: Int = 15
)

@JsonClass(generateAdapter = true)
data class ShelfUpdate(
    @Json(name = "slug") val slug: String? = null,
    @Json(name = "title") val title: String? = null,
    @Json(name = "type") val type: String? = null,
    @Json(name = "layout") val layout: String? = null,
    @Json(name = "config") val config: Map<String, Any?>? = null,
    @Json(name = "category_id") val categoryId: String? = null,
    @Json(name = "position") val position: Int? = null,
    @Json(name = "is_active") val isActive: Boolean? = null,
    @Json(name = "max_items") val maxItems: Int? = null
)

@JsonClass(generateAdapter = true)
data class ShelfItemInsert(
    @Json(name = "shelf_id") val shelfId: String,
    @Json(name = "series_id") val seriesId: String,
    @Json(name = "position") val position: Int = 0
)

@JsonClass(generateAdapter = true)
data class ShelfItemUpdate(
    @Json(name = "position") val position: Int
)

@JsonClass(generateAdapter = true)
data class CategoryInsert(
    @Json(name = "slug") val slug: String,
    @Json(name = "name") val name: String,
    @Json(name = "sort_order") val sortOrder: Int = 0,
    @Json(name = "is_active") val isActive: Boolean = true
)

@JsonClass(generateAdapter = true)
data class CategoryUpdate(
    @Json(name = "slug") val slug: String? = null,
    @Json(name = "name") val name: String? = null,
    @Json(name = "sort_order") val sortOrder: Int? = null,
    @Json(name = "is_active") val isActive: Boolean? = null
)

@JsonClass(generateAdapter = true)
data class SeasonInsert(
    @Json(name = "series_id") val seriesId: String,
    @Json(name = "season_number") val seasonNumber: Int,
    @Json(name = "title") val title: String? = null,
    @Json(name = "description") val description: String? = null,
    @Json(name = "poster_url") val posterUrl: String? = null
)

@JsonClass(generateAdapter = true)
data class SeasonUpdate(
    @Json(name = "season_number") val seasonNumber: Int? = null,
    @Json(name = "title") val title: String? = null,
    @Json(name = "description") val description: String? = null,
    @Json(name = "poster_url") val posterUrl: String? = null
)

@JsonClass(generateAdapter = true)
data class WatchlistInsert(
    @Json(name = "series_id") val seriesId: String,
    @Json(name = "user_id") val userId: String
)

@JsonClass(generateAdapter = true)
data class SeriesLikeInsert(
    @Json(name = "series_id") val seriesId: String,
    @Json(name = "user_id") val userId: String
)

@JsonClass(generateAdapter = true)
data class EpisodeLikeInsert(
    @Json(name = "episode_id") val episodeId: String,
    @Json(name = "user_id") val userId: String
)

@JsonClass(generateAdapter = true)
data class CommentLikeInsert(
    @Json(name = "comment_id") val commentId: String,
    @Json(name = "user_id") val userId: String
)

// ---------------------------------------------------------------------------
// RPC Request Payloads
// ---------------------------------------------------------------------------

@JsonClass(generateAdapter = true)
data class SetUserRoleRpcRequest(
    @Json(name = "p_user_id") val pUserId: String,
    @Json(name = "p_role") val pRole: String
)

@JsonClass(generateAdapter = true)
data class RecordViewRpcRequest(
    @Json(name = "p_series_id") val pSeriesId: String,
    @Json(name = "p_episode_id") val pEpisodeId: String? = null,
    @Json(name = "p_viewer_key") val pViewerKey: String
)

@JsonClass(generateAdapter = true)
data class SearchSeriesRpcRequest(
    @Json(name = "q") val q: String,
    @Json(name = "p_category") val pCategory: String? = null,
    @Json(name = "lim") val lim: Int = 20,
    @Json(name = "off") val off: Int = 0
)

@JsonClass(generateAdapter = true)
data class GetSeriesHomeRpcRequest(
    @Json(name = "p_category") val pCategory: String? = null
)

@JsonClass(generateAdapter = true)
data class GetRelatedSeriesRpcRequest(
    @Json(name = "p_series_id") val pSeriesId: String,
    @Json(name = "p_limit") val pLimit: Int = 12
)

@JsonClass(generateAdapter = true)
data class GetContinueWatchingRpcRequest(
    @Json(name = "p_limit") val pLimit: Int = 12
)
