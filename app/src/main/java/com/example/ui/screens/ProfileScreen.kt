package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.PointsHistoryItem
import com.example.data.model.Post
import com.example.data.model.UserProfile
import com.example.ui.components.BadgePill
import com.example.ui.components.CreatorRankChip
import com.example.ui.components.PointsBadge
import com.example.ui.components.PostCard
import com.example.ui.components.UserAvatar
import com.example.ui.components.formatTimeAgo
import com.example.ui.theme.QBackground
import com.example.ui.theme.QCardBorder
import com.example.ui.theme.QLikeRed
import com.example.ui.theme.QOnSurface
import com.example.ui.theme.QOnSurfaceVariant
import com.example.ui.theme.QOnYellow
import com.example.ui.theme.QRelayGreen
import com.example.ui.theme.QSurface
import com.example.ui.theme.QSurfaceVariant
import com.example.ui.theme.QVerifiedBlue
import com.example.ui.theme.QYellowDark
import com.example.ui.theme.QYellowLight
import com.example.ui.theme.QYellowPrimary

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox

@Composable
private fun StatItem(count: Int, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "$count",
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = QOnSurface
        )
        Text(
            text = label,
            fontSize = 11.sp,
            color = QOnSurfaceVariant
        )
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, fontSize = 13.sp, color = QOnSurfaceVariant)
        Text(text = value, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = QOnSurface)
    }
}

@Composable
private fun PointsHistoryCard(item: PointsHistoryItem) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = QSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, QCardBorder),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 4.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(12.dp)
        ) {
            val isPositive = (item.points ?: 0) >= 0
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(if (isPositive) QRelayGreen.copy(alpha = 0.15f) else QLikeRed.copy(alpha = 0.15f))
            ) {
                Text(
                    text = if (isPositive) "+${item.points}" else "${item.points}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isPositive) QRelayGreen else QLikeRed
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.description ?: item.action ?: "Points Update",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = QOnSurface
                )
                Text(
                    text = formatTimeAgo(item.createdAt),
                    fontSize = 11.sp,
                    color = QOnSurfaceVariant
                )
            }

            item.balanceAfter?.let { bal ->
                Text(
                    text = "$bal pts",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = QYellowDark
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ProfileScreen(
    profile: UserProfile,
    currentUser: UserProfile?,
    userPosts: List<Post>,
    savedPosts: List<Post> = emptyList(),
    trackersCount: Int = 0,
    trackingCount: Int = 0,
    isOwnProfile: Boolean,
    isRefreshing: Boolean = false,
    onRefresh: () -> Unit = {},
    onEditProfileClick: () -> Unit,
    onBackClick: () -> Unit,
    onLogoutClick: () -> Unit,
    onLikePost: (Post) -> Unit,
    onCommentPost: (Post) -> Unit,
    onBookmarkPost: (Post) -> Unit,
    onToggleFollow: (UserProfile) -> Unit,
    isFollowing: Boolean,
    onOpenMenu: ((Post) -> Unit)? = null,
    onLikersClick: ((Post) -> Unit)? = null
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabTitles = if (isOwnProfile) listOf("Posts", "Saved", "About", "Points") else listOf("Posts", "About", "Points")

    BackHandler(enabled = !isOwnProfile) {
        onBackClick()
    }

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        modifier = Modifier
            .fillMaxSize()
            .background(QBackground)
            .testTag("profile_screen")
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 90.dp)
        ) {
        // Profile Header (Cover Banner + Overlapping Avatar + Info Card)
        item {
            Box(
                modifier = Modifier.fillMaxWidth()
            ) {
                // 1. Cover Banner Image
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                ) {
                    if (!profile.coverUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = profile.coverUrl,
                            contentDescription = "Cover Banner",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(QYellowPrimary, QYellowDark, Color(0xFFF59E0B))
                                    )
                                )
                        )
                    }

                    // Top Navigation Action Overlay
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        if (!isOwnProfile) {
                            IconButton(
                                onClick = onBackClick,
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(Color.Black.copy(alpha = 0.45f))
                                    .size(38.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    tint = Color.White
                                )
                            }
                        } else {
                            Spacer(modifier = Modifier.width(38.dp))
                        }

                        Row {
                            IconButton(
                                onClick = {
                                    try {
                                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                            type = "text/plain"
                                            putExtra(
                                                Intent.EXTRA_TEXT,
                                                "Check out ${profile.effectiveDisplayName}'s profile on Qylomi: @${profile.effectiveUsername}"
                                            )
                                        }
                                        context.startActivity(Intent.createChooser(shareIntent, "Share Profile"))
                                    } catch (_: Exception) {}
                                },
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(Color.Black.copy(alpha = 0.45f))
                                    .size(38.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = "Share",
                                    tint = Color.White
                                )
                            }

                            if (isOwnProfile) {
                                Spacer(modifier = Modifier.width(8.dp))
                                IconButton(
                                    onClick = onLogoutClick,
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(Color.Black.copy(alpha = 0.45f))
                                        .size(38.dp)
                                        .testTag("profile_logout_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Logout,
                                        contentDescription = "Logout",
                                        tint = Color.White
                                    )
                                }
                            }
                        }
                    }
                }

                // 2. Profile Details Surface Card (Layered starting at y = 120.dp)
                Card(
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                    colors = CardDefaults.cardColors(containerColor = QSurface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 120.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .padding(top = 52.dp, bottom = 16.dp) // Top padding accounts for avatar height
                    ) {
                        // Display Name, Verified Badge, and Username
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = profile.effectiveDisplayName,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 22.sp,
                                color = QOnSurface
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Verified",
                                tint = QVerifiedBlue,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Text(
                            text = "@${profile.effectiveUsername}",
                            fontSize = 14.sp,
                            color = QOnSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Stats Bar
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceAround,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(QSurfaceVariant)
                                .padding(horizontal = 12.dp, vertical = 10.dp)
                        ) {
                            StatItem(count = userPosts.size, label = "Ideas")
                            StatItem(count = trackersCount, label = "Trackers")
                            StatItem(count = trackingCount, label = "Tracking")
                            StatItem(count = profile.points ?: 0, label = "XP Points")
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Creator Rank and Points badges
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CreatorRankChip(rank = profile.creatorRank, level = profile.creatorLevel)
                            PointsBadge(points = profile.points)
                        }

                        // Headline
                        if (!profile.headline.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = profile.headline,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp,
                                color = QOnSurface
                            )
                        }

                        // Bio
                        if (!profile.bio.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = profile.bio,
                                fontSize = 14.sp,
                                lineHeight = 20.sp,
                                color = QOnSurface
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Quick info row: Location, Work, Education, Website
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            if (!profile.location.isNullOrBlank()) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.LocationOn,
                                        contentDescription = null,
                                        tint = QOnSurfaceVariant,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = profile.location,
                                        fontSize = 12.sp,
                                        color = QOnSurfaceVariant
                                    )
                                }
                            }

                            if (!profile.work.isNullOrBlank()) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Business,
                                        contentDescription = null,
                                        tint = QOnSurfaceVariant,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = profile.work,
                                        fontSize = 12.sp,
                                        color = QOnSurfaceVariant
                                    )
                                }
                            }

                            if (!profile.education.isNullOrBlank()) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.School,
                                        contentDescription = null,
                                        tint = QOnSurfaceVariant,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = profile.education,
                                        fontSize = 12.sp,
                                        color = QOnSurfaceVariant
                                    )
                                }
                            }

                            if (!profile.website.isNullOrBlank()) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.clickable {
                                        try {
                                            val url = if (profile.website.startsWith("http")) profile.website else "https://${profile.website}"
                                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                                        } catch (_: Exception) {}
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Language,
                                        contentDescription = null,
                                        tint = QYellowDark,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = profile.website,
                                        fontSize = 12.sp,
                                        color = QYellowDark,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }

                        // Skills Tags Cloud
                        if (!profile.skills.isNullOrEmpty()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Skills & Expertise",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = QOnSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                profile.skills.forEach { skill ->
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(QSurfaceVariant)
                                            .padding(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = skill,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = QOnSurface
                                        )
                                    }
                                }
                            }
                        }

                        // Social Links row
                        if (!profile.socialLinks.isNullOrEmpty()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                profile.socialLinks.forEach { (platform, url) ->
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(QYellowLight)
                                            .clickable {
                                                try {
                                                    val cleanUrl = if (url.startsWith("http")) url else "https://$url"
                                                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(cleanUrl)))
                                                } catch (_: Exception) {}
                                            }
                                            .padding(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = platform.capitalize(),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = QYellowDark
                                        )
                                    }
                                }
                            }
                        }

                        // Badges Row
                        if (!profile.badges.isNullOrEmpty()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                profile.badges.forEach { badge ->
                                    BadgePill(badgeName = badge)
                                }
                            }
                        }
                    }
                }

                // 3. Avatar & Action Button Row (Positioned at y = 76.dp)
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(top = 76.dp)
                ) {
                    // Profile Avatar with thick border background
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(88.dp)
                            .clip(CircleShape)
                            .background(QSurface)
                            .border(3.5.dp, QYellowPrimary, CircleShape)
                            .padding(2.5.dp)
                    ) {
                        UserAvatar(
                            avatarUrl = profile.avatarUrl,
                            initials = profile.initials,
                            size = 82.dp
                        )
                    }

                    // Action Button (Edit Profile or Follow)
                    if (isOwnProfile) {
                        OutlinedButton(
                            onClick = onEditProfileClick,
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier
                                .testTag("edit_profile_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = null,
                                tint = QYellowDark,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Edit Profile",
                                fontWeight = FontWeight.Bold,
                                color = QOnSurface
                            )
                        }
                    } else {
                        if (isFollowing) {
                            OutlinedButton(
                                onClick = { onToggleFollow(profile) },
                                shape = RoundedCornerShape(20.dp)
                            ) {
                                Text("Tracking", color = QOnSurfaceVariant)
                            }
                        } else {
                            Button(
                                onClick = { onToggleFollow(profile) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = QYellowPrimary,
                                    contentColor = QOnYellow
                                ),
                                shape = RoundedCornerShape(20.dp)
                            ) {
                                Text("Track", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // Tabs Row (Posts, Saved, About, Points)
        item {
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = QSurface,
                contentColor = QYellowDark,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = QYellowPrimary,
                        height = 3.dp
                    )
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                tabTitles.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Text(
                                text = title,
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 14.sp
                            )
                        }
                    )
                }
            }
        }

        // Tab Content
        val activeTitle = tabTitles.getOrNull(selectedTab) ?: "Posts"
        when (activeTitle) {
            "Posts" -> {
                if (userPosts.isEmpty()) {
                    item {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(40.dp)
                        ) {
                            Text(
                                text = "No posts published yet.",
                                color = QOnSurfaceVariant,
                                fontSize = 14.sp
                            )
                        }
                    }
                } else {
                    items(userPosts, key = { it.id }) { post ->
                        Box(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) {
                            PostCard(
                                post = post,
                                onLikeClick = { onLikePost(post) },
                                onCommentClick = { onCommentPost(post) },
                                onBookmarkClick = { onBookmarkPost(post) },
                                onAuthorClick = {},
                                currentUserId = currentUser?.id,
                                onOpenMenu = onOpenMenu,
                                onLikersClick = onLikersClick
                            )
                        }
                    }
                }
            }

            "Saved" -> {
                // Table: saved_posts
                if (savedPosts.isEmpty()) {
                    item {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(40.dp)
                        ) {
                            Text(
                                text = "No saved posts yet.\nTap the bookmark icon on any post to save it.",
                                color = QOnSurfaceVariant,
                                fontSize = 14.sp,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                } else {
                    items(savedPosts, key = { it.id }) { post ->
                        Box(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) {
                            PostCard(
                                post = post,
                                onLikeClick = { onLikePost(post) },
                                onCommentClick = { onCommentPost(post) },
                                onBookmarkClick = { onBookmarkPost(post) },
                                onAuthorClick = {},
                                currentUserId = currentUser?.id,
                                onOpenMenu = onOpenMenu,
                                onLikersClick = onLikersClick
                            )
                        }
                    }
                }
            }

            "About" -> {
                item {
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = QSurface),
                        border = androidx.compose.foundation.BorderStroke(1.dp, QCardBorder),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Profile Information", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = QOnSurface)
                            Spacer(modifier = Modifier.height(12.dp))

                            InfoRow(label = "Username", value = "@${profile.effectiveUsername}")
                            InfoRow(label = "Age", value = profile.age?.let { "$it years" } ?: "Not specified")
                            InfoRow(label = "Gender", value = profile.gender ?: "Not specified")
                            InfoRow(label = "Location", value = profile.location ?: "Not specified")
                            InfoRow(label = "Work", value = profile.work ?: "Not specified")
                            InfoRow(label = "Education", value = profile.education ?: "Not specified")
                            InfoRow(label = "Website", value = profile.website ?: "Not specified")
                            InfoRow(label = "Joined Q", value = profile.createdAt?.substringBefore("T") ?: "Recently")
                        }
                    }
                }
            }

            "Points" -> {
                val history = profile.pointsHistory ?: emptyList()
                if (history.isEmpty()) {
                    item {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(40.dp)
                        ) {
                            Text(
                                text = "No points history recorded yet.",
                                color = QOnSurfaceVariant,
                                fontSize = 14.sp
                            )
                        }
                    }
                } else {
                    items(history) { item ->
                        PointsHistoryCard(item)
                    }
                }
            }
        }
    }
}
}


