package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class CreatorRewardsRepository(
    private val context: Context,
    private val socialRepository: SocialRepository
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("creator_rewards_prefs", Context.MODE_PRIVATE)

    companion object {
        const val POINTS_PER_LIKE = 5
        const val POINTS_PER_SHARE = 15
        const val POINTS_PER_COMMENT = 10
        const val POINTS_PER_5_VIEWS = 1 // 1 pt per 5 views

        private var instance: CreatorRewardsRepository? = null
        fun getInstance(context: Context, socialRepository: SocialRepository): CreatorRewardsRepository {
            return instance ?: synchronized(this) {
                instance ?: CreatorRewardsRepository(context.applicationContext, socialRepository).also {
                    instance = it
                }
            }
        }
    }

    private val _summary = MutableStateFlow<CreatorEngagementSummary?>(null)
    val summary: StateFlow<CreatorEngagementSummary?> = _summary.asStateFlow()

    private val _milestones = MutableStateFlow<List<MilestoneReward>>(emptyList())
    val milestones: StateFlow<List<MilestoneReward>> = _milestones.asStateFlow()

    private val _rewardsCatalog = MutableStateFlow<List<VirtualRewardItem>>(emptyList())
    val rewardsCatalog: StateFlow<List<VirtualRewardItem>> = _rewardsCatalog.asStateFlow()

    private val _pointsHistory = MutableStateFlow<List<PointsHistoryItem>>(emptyList())
    val pointsHistory: StateFlow<List<PointsHistoryItem>> = _pointsHistory.asStateFlow()

    private val claimedMilestoneIds = mutableSetOf<String>()
    private val unlockedRewardIds = mutableSetOf<String>()

    init {
        loadLocalRewardsData()
    }

    private fun loadLocalRewardsData() {
        val savedMilestones = prefs.getStringSet("claimed_milestones", emptySet()) ?: emptySet()
        claimedMilestoneIds.clear()
        claimedMilestoneIds.addAll(savedMilestones)

        val savedRewards = prefs.getStringSet("unlocked_rewards", emptySet()) ?: emptySet()
        unlockedRewardIds.clear()
        unlockedRewardIds.addAll(savedRewards)

        val historyJson = prefs.getString("points_history_v1", null)
        val historyList = mutableListOf<PointsHistoryItem>()
        if (!historyJson.isNullOrBlank()) {
            try {
                val array = JSONArray(historyJson)
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    historyList.add(
                        PointsHistoryItem(
                            id = obj.optString("id", UUID.randomUUID().toString()),
                            action = obj.optString("action", "Points"),
                            points = obj.optInt("points", 0),
                            createdAt = obj.optString("createdAt", ""),
                            description = obj.optString("description", ""),
                            referenceId = obj.optString("referenceId", null),
                            balanceAfter = obj.optInt("balanceAfter", 0)
                        )
                    )
                }
            } catch (_: Exception) {}
        }
        _pointsHistory.value = historyList
    }

    private fun saveLocalRewardsData() {
        prefs.edit()
            .putStringSet("claimed_milestones", claimedMilestoneIds)
            .putStringSet("unlocked_rewards", unlockedRewardIds)
            .apply()

        try {
            val array = JSONArray()
            _pointsHistory.value.take(100).forEach { item ->
                val obj = JSONObject()
                obj.put("id", item.id)
                obj.put("action", item.action)
                obj.put("points", item.points)
                obj.put("createdAt", item.createdAt)
                obj.put("description", item.description)
                obj.put("referenceId", item.referenceId)
                obj.put("balanceAfter", item.balanceAfter)
                array.put(obj)
            }
            prefs.edit().putString("points_history_v1", array.toString()).apply()
        } catch (_: Exception) {}
    }

    /**
     * Compute engagement metrics and rewards summary for the target user (or current user)
     */
    suspend fun computeEngagementSummary(targetUserId: String? = null): CreatorEngagementSummary = withContext(Dispatchers.IO) {
        val user = if (targetUserId != null) {
            socialRepository.getProfile(targetUserId).getOrNull()
        } else {
            socialRepository.currentUser.value
        } ?: UserProfile(id = targetUserId ?: "guest", username = "creator", displayName = "Creator")

        // Fetch authored posts
        val allPostsResult = socialRepository.getFeedPosts()
        val posts = if (allPostsResult.isSuccess) {
            allPostsResult.getOrDefault(emptyList()).filter { it.userId == user.id }
        } else {
            emptyList()
        }

        // Aggregate raw engagement metrics
        val totalPosts = posts.size
        val totalLikes = posts.sumOf { it.appreciationsCount }
        val totalShares = posts.sumOf { it.relaysCount }
        val totalComments = posts.sumOf { it.repliesCount }
        val totalViews = posts.sumOf { (it.appreciationsCount * 4 + it.relaysCount * 8 + it.repliesCount * 5 + 15) }

        // Calculate points based on formula
        val pointsFromLikes = totalLikes * POINTS_PER_LIKE
        val pointsFromShares = totalShares * POINTS_PER_SHARE
        val pointsFromComments = totalComments * POINTS_PER_COMMENT
        val pointsFromViews = (totalViews / 5) * POINTS_PER_5_VIEWS

        // Milestones definitions & current state
        val milestoneDefinitions = getMilestoneDefinitions(
            likes = totalLikes,
            shares = totalShares,
            comments = totalComments,
            views = totalViews,
            posts = totalPosts
        )
        _milestones.value = milestoneDefinitions

        val bonusMilestonePoints = milestoneDefinitions
            .filter { it.isClaimed }
            .sumOf { it.rewardPoints }

        // Calculate rewards catalog costs spent
        val catalog = getRewardsCatalogDefinitions()
        _rewardsCatalog.value = catalog

        val pointsSpent = catalog
            .filter { it.isUnlocked }
            .sumOf { it.costPoints }

        val metrics = CreatorEngagementMetrics(
            totalPosts = totalPosts,
            totalLikesReceived = totalLikes,
            totalSharesReceived = totalShares,
            totalCommentsReceived = totalComments,
            totalViewsReceived = totalViews,
            pointsFromLikes = pointsFromLikes,
            pointsFromShares = pointsFromShares,
            pointsFromComments = pointsFromComments,
            pointsFromViews = pointsFromViews,
            bonusMilestonePoints = bonusMilestonePoints,
            pointsSpent = pointsSpent
        )

        val totalPointsEarned = metrics.totalPointsEarned
        val currentBalance = metrics.currentBalance
        val tier = CreatorTierLevel.fromPoints(totalPointsEarned)
        val nextTier = CreatorTierLevel.nextTier(tier)

        val progressToNextTier: Float
        val pointsNeeded: Int
        if (nextTier != null) {
            val tierRange = nextTier.minPoints - tier.minPoints
            val earnedInTier = (totalPointsEarned - tier.minPoints).coerceAtLeast(0)
            progressToNextTier = if (tierRange > 0) (earnedInTier.toFloat() / tierRange).coerceIn(0f, 1f) else 1f
            pointsNeeded = (nextTier.minPoints - totalPointsEarned).coerceAtLeast(0)
        } else {
            progressToNextTier = 1f
            pointsNeeded = 0
        }

        val summary = CreatorEngagementSummary(
            userId = user.id,
            username = user.effectiveUsername,
            displayName = user.effectiveDisplayName,
            avatarUrl = user.avatarUrl,
            metrics = metrics,
            tier = tier,
            nextTier = nextTier,
            progressToNextTier = progressToNextTier,
            pointsNeededForNextTier = pointsNeeded,
            claimedMilestoneIds = claimedMilestoneIds.toSet(),
            unlockedRewardIds = unlockedRewardIds.toSet()
        )

        _summary.value = summary

        // Sync with user profile in background
        if (user.id == socialRepository.currentUser.value?.id) {
            syncProfileWithRewards(user, currentBalance, tier)
        }

        summary
    }

    private suspend fun syncProfileWithRewards(user: UserProfile, balance: Int, tier: CreatorTierLevel) {
        val currentPoints = user.points ?: 0
        val currentRank = user.creatorRank ?: ""
        val currentLevel = user.creatorLevel ?: 1

        if (currentPoints != balance || currentRank != tier.title || currentLevel != tier.levelNumber) {
            try {
                // Update local UserProfile state
                val updatedProfile = user.copy(
                    points = balance,
                    creatorRank = tier.title,
                    creatorLevel = tier.levelNumber,
                    pointsHistory = _pointsHistory.value.take(20)
                )
                socialRepository.updateProfile(
                    userId = user.id,
                    updates = ProfileUpdateRequest(
                        headline = user.headline,
                        bio = user.bio
                    )
                )
                // Note: SocialRepository currentUser can also be updated directly
                socialRepository.loginAsDemoUser(updatedProfile)
            } catch (_: Exception) {}
        }
    }

    /**
     * Claim milestone bonus points
     */
    suspend fun claimMilestone(milestoneId: String): Result<MilestoneReward> = withContext(Dispatchers.IO) {
        val currentList = _milestones.value
        val milestone = currentList.find { it.id == milestoneId }
            ?: return@withContext Result.failure(IllegalArgumentException("Milestone not found"))

        if (claimedMilestoneIds.contains(milestoneId)) {
            return@withContext Result.failure(IllegalStateException("Milestone already claimed"))
        }

        if (!milestone.isAchieved) {
            return@withContext Result.failure(IllegalStateException("Milestone goal not yet reached"))
        }

        claimedMilestoneIds.add(milestoneId)
        saveLocalRewardsData()

        // Log transaction
        logPointsHistory(
            action = "Milestone: ${milestone.title}",
            points = milestone.rewardPoints,
            description = "Completed milestone '${milestone.title}'"
        )

        // Refresh calculations
        computeEngagementSummary()

        Result.success(milestone.copy(isClaimed = true))
    }

    /**
     * Redeem a virtual perk from the rewards shop
     */
    suspend fun redeemVirtualReward(rewardId: String): Result<VirtualRewardItem> = withContext(Dispatchers.IO) {
        val currentSummary = _summary.value ?: computeEngagementSummary()
        val currentBalance = currentSummary.metrics.currentBalance

        val reward = _rewardsCatalog.value.find { it.id == rewardId }
            ?: return@withContext Result.failure(IllegalArgumentException("Reward item not found"))

        if (unlockedRewardIds.contains(rewardId)) {
            return@withContext Result.failure(IllegalStateException("Reward already unlocked"))
        }

        if (currentBalance < reward.costPoints) {
            val diff = reward.costPoints - currentBalance
            return@withContext Result.failure(IllegalStateException("Insufficient Q-Points! You need $diff more points."))
        }

        unlockedRewardIds.add(rewardId)
        saveLocalRewardsData()

        // Log transaction (negative points for spend)
        logPointsHistory(
            action = "Unlocked: ${reward.title}",
            points = -reward.costPoints,
            description = "Redeemed virtual perk '${reward.title}'"
        )

        // Refresh calculations
        computeEngagementSummary()

        Result.success(reward.copy(isUnlocked = true))
    }

    private fun logPointsHistory(action: String, points: Int, description: String) {
        val now = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()).format(Date())
        val currentBalance = _summary.value?.metrics?.currentBalance ?: 0
        val newBalance = (currentBalance + points).coerceAtLeast(0)

        val item = PointsHistoryItem(
            id = UUID.randomUUID().toString(),
            action = action,
            points = points,
            createdAt = now,
            description = description,
            balanceAfter = newBalance
        )

        val updated = listOf(item) + _pointsHistory.value
        _pointsHistory.value = updated
        saveLocalRewardsData()
    }

    private fun getMilestoneDefinitions(
        likes: Int,
        shares: Int,
        comments: Int,
        views: Int,
        posts: Int
    ): List<MilestoneReward> {
        return listOf(
            MilestoneReward(
                id = "milestone_first_appreciation",
                title = "First Appreciations",
                description = "Receive 10 total likes across your content",
                metricType = MilestoneMetricType.LIKES,
                targetCount = 10,
                rewardPoints = 50,
                currentCount = likes,
                isClaimed = claimedMilestoneIds.contains("milestone_first_appreciation")
            ),
            MilestoneReward(
                id = "milestone_share_dynamo",
                title = "Share Dynamo",
                description = "Have your ideas shared or relayed 5 times",
                metricType = MilestoneMetricType.SHARES,
                targetCount = 5,
                rewardPoints = 100,
                currentCount = shares,
                isClaimed = claimedMilestoneIds.contains("milestone_share_dynamo")
            ),
            MilestoneReward(
                id = "milestone_community_star",
                title = "Community Star",
                description = "Earn 50 likes on your posts and video series",
                metricType = MilestoneMetricType.LIKES,
                targetCount = 50,
                rewardPoints = 200,
                currentCount = likes,
                isClaimed = claimedMilestoneIds.contains("milestone_community_star")
            ),
            MilestoneReward(
                id = "milestone_viral_relay",
                title = "Viral Relay",
                description = "Receive 15 shares & relays from the community",
                metricType = MilestoneMetricType.SHARES,
                targetCount = 15,
                rewardPoints = 300,
                currentCount = shares,
                isClaimed = claimedMilestoneIds.contains("milestone_viral_relay")
            ),
            MilestoneReward(
                id = "milestone_discussion_starter",
                title = "Conversation Spark",
                description = "Receive 20 engaging comments and replies",
                metricType = MilestoneMetricType.COMMENTS,
                targetCount = 20,
                rewardPoints = 150,
                currentCount = comments,
                isClaimed = claimedMilestoneIds.contains("milestone_discussion_starter")
            ),
            MilestoneReward(
                id = "milestone_prolific_creator",
                title = "Content Machine",
                description = "Publish 5 high-quality posts, videos, or series",
                metricType = MilestoneMetricType.POSTS,
                targetCount = 5,
                rewardPoints = 120,
                currentCount = posts,
                isClaimed = claimedMilestoneIds.contains("milestone_prolific_creator")
            ),
            MilestoneReward(
                id = "milestone_grand_audience",
                title = "Grand Audience",
                description = "Amass 300 impressions & video plays",
                metricType = MilestoneMetricType.VIEWS,
                targetCount = 300,
                rewardPoints = 250,
                currentCount = views,
                isClaimed = claimedMilestoneIds.contains("milestone_grand_audience")
            )
        )
    }

    private fun getRewardsCatalogDefinitions(): List<VirtualRewardItem> {
        return listOf(
            VirtualRewardItem(
                id = "reward_gold_spark",
                title = "Golden Spark Frame",
                category = "Avatar Frame",
                description = "Surround your avatar with a prestigious gold ring on all posts and comments",
                costPoints = 150,
                iconName = "AutoAwesome",
                isUnlocked = unlockedRewardIds.contains("reward_gold_spark"),
                badgeTag = "gold_frame"
            ),
            VirtualRewardItem(
                id = "reward_verified_glow",
                title = "Neon Pulse Flare",
                category = "Feed Styling",
                description = "Adds an illuminated neon highlight border to your posts in community feeds",
                costPoints = 300,
                iconName = "Flare",
                isUnlocked = unlockedRewardIds.contains("reward_verified_glow"),
                badgeTag = "neon_flare"
            ),
            VirtualRewardItem(
                id = "reward_custom_banner",
                title = "Sunset Gradient Cover",
                category = "Profile Skin",
                description = "Unlocks an aesthetic animated twilight gradient banner on your profile",
                costPoints = 400,
                iconName = "Palette",
                isUnlocked = unlockedRewardIds.contains("reward_custom_banner"),
                badgeTag = "sunset_skin"
            ),
            VirtualRewardItem(
                id = "reward_spotlight_boost",
                title = "Explore Spotlight Pass",
                category = "Boost",
                description = "Pins your latest post or series to the featured shelf in Explore for 48 hours",
                costPoints = 500,
                iconName = "RocketLaunch",
                isUnlocked = unlockedRewardIds.contains("reward_spotlight_boost"),
                badgeTag = "explore_spotlight"
            ),
            VirtualRewardItem(
                id = "reward_diamond_flair",
                title = "Diamond Crown VIP Flair",
                category = "VIP Badge",
                description = "Displays an exclusive diamond crown badge next to your handle everywhere",
                costPoints = 800,
                iconName = "Diamond",
                isUnlocked = unlockedRewardIds.contains("reward_diamond_flair"),
                badgeTag = "diamond_crown"
            )
        )
    }
}
