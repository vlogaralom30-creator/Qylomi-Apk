package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.UserProfile
import com.example.ui.theme.QBackground
import com.example.ui.theme.QCardBorder
import com.example.ui.theme.QOnSurface
import com.example.ui.theme.QOnSurfaceVariant
import com.example.ui.theme.QOnYellow
import com.example.ui.theme.QSurface
import com.example.ui.theme.QYellowDark
import com.example.ui.theme.QYellowPrimary

enum class SocialTab {
    FEED, EXPLORE, CREATE, NOTIFICATIONS, PROFILE
}

@Composable
fun QTopAppBar(
    currentUser: UserProfile?,
    unreadNotifications: Int = 0,
    onNotificationsClick: () -> Unit,
    onProfileClick: () -> Unit,
    onReelsClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = modifier
            .fillMaxWidth()
            .background(QSurface)
            .statusBarsPadding()
            .border(width = 0.5.dp, color = QCardBorder)
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        // Left Branding
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.testTag("app_branding")
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(QYellowPrimary)
            ) {
                Text(
                    text = "Q",
                    fontWeight = FontWeight.Black,
                    fontSize = 20.sp,
                    color = QOnYellow
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Social",
                fontWeight = FontWeight.ExtraBold,
                fontSize = 18.sp,
                color = QOnSurface,
                letterSpacing = (-0.5).sp
            )
        }

        // Right Action Items
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Reels button
            if (onReelsClick != null) {
                IconButton(
                    onClick = onReelsClick,
                    modifier = Modifier
                        .size(40.dp)
                        .testTag("top_reels_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Movie,
                        contentDescription = "Reels",
                        tint = QYellowDark,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(2.dp))
            }

            // Notifications button with badge
            IconButton(
                onClick = onNotificationsClick,
                modifier = Modifier
                    .size(40.dp)
                    .testTag("top_notifications_button")
            ) {
                BadgedBox(
                    badge = {
                        if (unreadNotifications > 0) {
                            Badge(
                                containerColor = QYellowDark,
                                contentColor = Color.White
                            ) {
                                Text("$unreadNotifications")
                            }
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Notifications,
                        contentDescription = "Notifications",
                        tint = QOnSurface,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(4.dp))

            // User Avatar Clickable to Profile
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clickable { onProfileClick() }
                    .testTag("top_profile_avatar")
            ) {
                UserAvatar(
                    avatarUrl = currentUser?.avatarUrl,
                    initials = currentUser?.initials ?: "Q",
                    size = 36.dp
                )
            }
        }
    }
}

@Composable
fun QBottomNavBar(
    selectedTab: SocialTab,
    onTabSelected: (SocialTab) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(QSurface)
            .border(width = 1.dp, color = QCardBorder)
            .navigationBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp, horizontal = 12.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Feed
            NavTabItem(
                label = "Feed",
                isSelected = selectedTab == SocialTab.FEED,
                selectedIcon = Icons.Filled.Home,
                unselectedIcon = Icons.Outlined.Home,
                onClick = { onTabSelected(SocialTab.FEED) },
                testTag = "nav_tab_feed"
            )

            // Explore
            NavTabItem(
                label = "Explore",
                isSelected = selectedTab == SocialTab.EXPLORE,
                selectedIcon = Icons.Filled.Explore,
                unselectedIcon = Icons.Outlined.Explore,
                onClick = { onTabSelected(SocialTab.EXPLORE) },
                testTag = "nav_tab_explore"
            )

            // Center Create Action (Floating Action Style in Yellow)
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(QYellowPrimary)
                    .clickable { onTabSelected(SocialTab.CREATE) }
                    .testTag("nav_tab_create")
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = "Create Post",
                    tint = QOnYellow,
                    modifier = Modifier.size(26.dp)
                )
            }

            // Notifications
            NavTabItem(
                label = "Activity",
                isSelected = selectedTab == SocialTab.NOTIFICATIONS,
                selectedIcon = Icons.Filled.Notifications,
                unselectedIcon = Icons.Outlined.Notifications,
                onClick = { onTabSelected(SocialTab.NOTIFICATIONS) },
                testTag = "nav_tab_activity"
            )

            // Profile
            NavTabItem(
                label = "Profile",
                isSelected = selectedTab == SocialTab.PROFILE,
                selectedIcon = Icons.Filled.Person,
                unselectedIcon = Icons.Outlined.Person,
                onClick = { onTabSelected(SocialTab.PROFILE) },
                testTag = "nav_tab_profile"
            )
        }
    }
}

@Composable
private fun NavTabItem(
    label: String,
    isSelected: Boolean,
    selectedIcon: androidx.compose.ui.graphics.vector.ImageVector,
    unselectedIcon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    testTag: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 4.dp)
            .testTag(testTag)
    ) {
        Icon(
            imageVector = if (isSelected) selectedIcon else unselectedIcon,
            contentDescription = label,
            tint = if (isSelected) QYellowDark else QOnSurfaceVariant,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) QYellowDark else QOnSurfaceVariant
        )
    }
}
