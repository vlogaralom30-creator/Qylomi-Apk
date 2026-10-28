package com.example.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Creator Tiers based on total earned engagement points
 */
enum class CreatorTierLevel(
    val levelNumber: Int,
    val title: String,
    val minPoints: Int,
    val maxPoints: Int,
    val perkDescription: String,
    val iconName: String
) {
    NOVICE(1, "Novice Explorer", 0, 199, "Standard creator tools & post publishing", "AutoAwesome"),
    RISING(2, "Rising Star", 200, 499, "Bronze creator badge & custom cover frame", "Star"),
    VERIFIED(3, "Verified Creator", 500, 1499, "Silver verified star & priority in Explore feed", "Verified"),
    ICON(4, "Community Icon", 1500, 3999, "Gold trophy badge & featured feed banner", "EmojiEvents"),
    MASTER(5, "Master Artisan", 4000, Int.MAX_VALUE, "Diamond crown badge & VIP creator styling", "MilitaryTech");

    companion object {
        fun fromPoints(points: Int): CreatorTierLevel {
            val safe = points.coerceAtLeast(0)
            return entries.lastOrNull { safe >= it.minPoints } ?: NOVICE
        }

        fun nextTier(current: CreatorTierLevel): CreatorTierLevel? {
            val nextOrdinal = current.ordinal + 1
            return if (nextOrdinal < entries.size) entries[nextOrdinal] else null
        }
    }
}

/**
 * Aggregated creator metrics across all authored posts and interactions
 */
@JsonClass(generateAdapter = true)
data class CreatorEngagementMetrics(
    @Json(name = "total_posts") val totalPosts: Int = 0,
    @Json(name = "total_likes_received") val totalLikesReceived: Int = 0,
    @Json(name = "total_shares_received") val totalSharesReceived: Int = 0,
    @Json(name = "total_comments_received") val totalCommentsReceived: Int = 0,
    @Json(name = "total_views_received") val totalViewsReceived: Int = 0,
    @Json(name = "points_from_likes") val pointsFromLikes: Int = 0,
    @Json(name = "points_from_shares") val pointsFromShares: Int = 0,
    @Json(name = "points_from_comments") val pointsFromComments: Int = 0,
    @Json(name = "points_from_views") val pointsFromViews: Int = 0,
    @Json(name = "bonus_milestone_points") val bonusMilestonePoints: Int = 0,
    @Json(name = "points_spent") val pointsSpent: Int = 0
) {
    val totalPointsEarned: Int
        get() = pointsFromLikes + pointsFromShares + pointsFromComments + pointsFromViews + bonusMilestonePoints

    val currentBalance: Int
        get() = (totalPointsEarned - pointsSpent).coerceAtLeast(0)
}

/**
 * Summary state presented in Creator Rewards dashboard
 */
data class CreatorEngagementSummary(
    val userId: String,
    val username: String,
    val displayName: String,
    val avatarUrl: String?,
    val metrics: CreatorEngagementMetrics,
    val tier: CreatorTierLevel,
    val nextTier: CreatorTierLevel?,
    val progressToNextTier: Float, // 0.0f .. 1.0f
    val pointsNeededForNextTier: Int,
    val claimedMilestoneIds: Set<String>,
    val unlockedRewardIds: Set<String>
)

/**
 * Milestone reward target
 */
@JsonClass(generateAdapter = true)
data class MilestoneReward(
    @Json(name = "id") val id: String,
    @Json(name = "title") val title: String,
    @Json(name = "description") val description: String,
    @Json(name = "metric_type") val metricType: MilestoneMetricType,
    @Json(name = "target_count") val targetCount: Int,
    @Json(name = "reward_points") val rewardPoints: Int,
    @Json(name = "current_count") val currentCount: Int = 0,
    @Json(name = "is_claimed") val isClaimed: Boolean = false
) {
    val progress: Float
        get() = if (targetCount > 0) (currentCount.toFloat() / targetCount).coerceIn(0f, 1f) else 1f

    val isAchieved: Boolean
        get() = currentCount >= targetCount
}

enum class MilestoneMetricType {
    LIKES,
    SHARES,
    COMMENTS,
    VIEWS,
    POSTS
}

/**
 * Virtual Reward / Perk available in the Creator Rewards Catalog
 */
@JsonClass(generateAdapter = true)
data class VirtualRewardItem(
    @Json(name = "id") val id: String,
    @Json(name = "title") val title: String,
    @Json(name = "category") val category: String,
    @Json(name = "description") val description: String,
    @Json(name = "cost_points") val costPoints: Int,
    @Json(name = "icon_name") val iconName: String,
    @Json(name = "is_unlocked") val isUnlocked: Boolean = false,
    @Json(name = "badge_tag") val badgeTag: String? = null
)
