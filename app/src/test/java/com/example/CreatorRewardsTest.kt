package com.example

import com.example.data.model.*
import org.junit.Assert.*
import org.junit.Test

class CreatorRewardsTest {

    @Test
    fun testTierProgressionLevels() {
        assertEquals(CreatorTierLevel.NOVICE, CreatorTierLevel.fromPoints(0))
        assertEquals(CreatorTierLevel.NOVICE, CreatorTierLevel.fromPoints(150))
        assertEquals(CreatorTierLevel.RISING, CreatorTierLevel.fromPoints(200))
        assertEquals(CreatorTierLevel.RISING, CreatorTierLevel.fromPoints(499))
        assertEquals(CreatorTierLevel.VERIFIED, CreatorTierLevel.fromPoints(500))
        assertEquals(CreatorTierLevel.VERIFIED, CreatorTierLevel.fromPoints(1200))
        assertEquals(CreatorTierLevel.ICON, CreatorTierLevel.fromPoints(1500))
        assertEquals(CreatorTierLevel.ICON, CreatorTierLevel.fromPoints(3999))
        assertEquals(CreatorTierLevel.MASTER, CreatorTierLevel.fromPoints(4000))
        assertEquals(CreatorTierLevel.MASTER, CreatorTierLevel.fromPoints(10000))

        assertEquals(CreatorTierLevel.RISING, CreatorTierLevel.nextTier(CreatorTierLevel.NOVICE))
        assertEquals(CreatorTierLevel.VERIFIED, CreatorTierLevel.nextTier(CreatorTierLevel.RISING))
        assertEquals(CreatorTierLevel.ICON, CreatorTierLevel.nextTier(CreatorTierLevel.VERIFIED))
        assertEquals(CreatorTierLevel.MASTER, CreatorTierLevel.nextTier(CreatorTierLevel.ICON))
        assertNull(CreatorTierLevel.nextTier(CreatorTierLevel.MASTER))
    }

    @Test
    fun testEngagementMetricsPointsCalculation() {
        val likes = 20 // 20 * 5 = 100
        val shares = 6  // 6 * 15 = 90
        val comments = 8 // 8 * 10 = 80
        val views = 100 // (100 / 5) * 1 = 20

        val metrics = CreatorEngagementMetrics(
            totalPosts = 4,
            totalLikesReceived = likes,
            totalSharesReceived = shares,
            totalCommentsReceived = comments,
            totalViewsReceived = views,
            pointsFromLikes = likes * 5,
            pointsFromShares = shares * 15,
            pointsFromComments = comments * 10,
            pointsFromViews = (views / 5) * 1,
            bonusMilestonePoints = 50,
            pointsSpent = 150
        )

        assertEquals(100, metrics.pointsFromLikes)
        assertEquals(90, metrics.pointsFromShares)
        assertEquals(80, metrics.pointsFromComments)
        assertEquals(20, metrics.pointsFromViews)
        // Total earned: 100 + 90 + 80 + 20 + 50 = 340
        assertEquals(340, metrics.totalPointsEarned)
        // Current balance: 340 - 150 = 190
        assertEquals(190, metrics.currentBalance)
    }

    @Test
    fun testMilestoneAchievementAndProgress() {
        val milestone = MilestoneReward(
            id = "test_milestone",
            title = "Test Likes",
            description = "Get 10 likes",
            metricType = MilestoneMetricType.LIKES,
            targetCount = 10,
            rewardPoints = 50,
            currentCount = 7,
            isClaimed = false
        )

        assertFalse(milestone.isAchieved)
        assertEquals(0.7f, milestone.progress, 0.01f)

        val completedMilestone = milestone.copy(currentCount = 12)
        assertTrue(completedMilestone.isAchieved)
        assertEquals(1.0f, completedMilestone.progress, 0.01f)
    }

    @Test
    fun testVirtualRewardItemProperties() {
        val item = VirtualRewardItem(
            id = "gold_frame",
            title = "Golden Spark Frame",
            category = "Avatar Frame",
            description = "Gold frame for profile",
            costPoints = 150,
            iconName = "AutoAwesome",
            isUnlocked = false
        )

        assertEquals("gold_frame", item.id)
        assertEquals(150, item.costPoints)
        assertFalse(item.isUnlocked)

        val unlockedItem = item.copy(isUnlocked = true)
        assertTrue(unlockedItem.isUnlocked)
    }
}
