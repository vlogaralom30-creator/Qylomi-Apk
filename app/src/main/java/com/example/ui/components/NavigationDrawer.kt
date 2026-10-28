package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.UserProfile
import com.example.ui.theme.*

enum class DrawerDestination {
    FEED, SERIES, EXPLORE, GROUPS, REELS, SAVED, NOTIFICATIONS, CREATOR_STUDIO, ADMIN_STUDIO, SETTINGS
}

@Composable
fun QNavigationDrawerSheet(
    currentUser: UserProfile?,
    unreadNotifications: Int,
    selectedDestination: DrawerDestination,
    onDestinationSelect: (DrawerDestination) -> Unit,
    onLogoutClick: () -> Unit,
    onProfileClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    ModalDrawerSheet(
        modifier = modifier
            .width(300.dp)
            .fillMaxHeight(),
        drawerContainerColor = MaterialTheme.colorScheme.surface,
        drawerContentColor = MaterialTheme.colorScheme.onSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 16.dp)
        ) {
            // 1. User Profile Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onProfileClick() }
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                UserAvatar(
                    avatarUrl = currentUser?.avatarUrl,
                    initials = currentUser?.initials ?: "Q",
                    size = 50.dp
                )

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = currentUser?.displayName ?: "Guest User",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (currentUser != null) "@${currentUser.username}" else "Sign in for full access",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Role Chip if creator or admin
                    if (currentUser != null && (currentUser.canCreateSeries || currentUser.isAdmin)) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (currentUser.isAdmin) Color(0xFFD32F2F).copy(alpha = 0.2f) else QYellowPrimary.copy(alpha = 0.2f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = if (currentUser.isAdmin) "Admin" else "Creator",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (currentUser.isAdmin) Color(0xFFD32F2F) else QYellowDark
                            )
                        }
                    }
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
            )

            // 2. Primary Navigation Destinations
            DrawerItem(
                label = "Feed",
                icon = Icons.Outlined.Home,
                selectedIcon = Icons.Filled.Home,
                isSelected = selectedDestination == DrawerDestination.FEED,
                onClick = { onDestinationSelect(DrawerDestination.FEED) },
                testTag = "drawer_item_feed"
            )

            DrawerItem(
                label = "Series & Anime",
                icon = Icons.Outlined.VideoLibrary,
                selectedIcon = Icons.Filled.VideoLibrary,
                isSelected = selectedDestination == DrawerDestination.SERIES,
                badgeText = "NEW",
                onClick = { onDestinationSelect(DrawerDestination.SERIES) },
                testTag = "drawer_item_series"
            )

            DrawerItem(
                label = "Explore",
                icon = Icons.Outlined.Explore,
                selectedIcon = Icons.Filled.Explore,
                isSelected = selectedDestination == DrawerDestination.EXPLORE,
                onClick = { onDestinationSelect(DrawerDestination.EXPLORE) },
                testTag = "drawer_item_explore"
            )

            DrawerItem(
                label = "Groups & Communities",
                icon = Icons.Outlined.Group,
                selectedIcon = Icons.Filled.Group,
                isSelected = selectedDestination == DrawerDestination.GROUPS,
                onClick = { onDestinationSelect(DrawerDestination.GROUPS) },
                testTag = "drawer_item_groups"
            )

            DrawerItem(
                label = "Reels",
                icon = Icons.Outlined.Movie,
                selectedIcon = Icons.Filled.Movie,
                isSelected = selectedDestination == DrawerDestination.REELS,
                onClick = { onDestinationSelect(DrawerDestination.REELS) },
                testTag = "drawer_item_reels"
            )

            DrawerItem(
                label = "Saved",
                icon = Icons.Outlined.Bookmark,
                selectedIcon = Icons.Filled.Bookmark,
                isSelected = selectedDestination == DrawerDestination.SAVED,
                onClick = { onDestinationSelect(DrawerDestination.SAVED) },
                testTag = "drawer_item_saved"
            )

            DrawerItem(
                label = "Notifications",
                icon = Icons.Outlined.Notifications,
                selectedIcon = Icons.Filled.Notifications,
                isSelected = selectedDestination == DrawerDestination.NOTIFICATIONS,
                badgeCount = unreadNotifications,
                onClick = { onDestinationSelect(DrawerDestination.NOTIFICATIONS) },
                testTag = "drawer_item_notifications"
            )

            // 3. Studio Links (Creator & Admin)
            if (currentUser != null && (currentUser.canCreateSeries || currentUser.isAdmin)) {
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                )

                if (currentUser.canCreateSeries) {
                    DrawerItem(
                        label = "Creator Studio",
                        icon = Icons.Outlined.PlayArrow,
                        selectedIcon = Icons.Filled.PlayArrow,
                        isSelected = selectedDestination == DrawerDestination.CREATOR_STUDIO,
                        accentColor = QYellowDark,
                        onClick = { onDestinationSelect(DrawerDestination.CREATOR_STUDIO) },
                        testTag = "drawer_item_creator_studio"
                    )
                }

                if (currentUser.isAdmin) {
                    DrawerItem(
                        label = "Admin Studio",
                        icon = Icons.Outlined.AdminPanelSettings,
                        selectedIcon = Icons.Filled.AdminPanelSettings,
                        isSelected = selectedDestination == DrawerDestination.ADMIN_STUDIO,
                        accentColor = Color(0xFFD32F2F),
                        onClick = { onDestinationSelect(DrawerDestination.ADMIN_STUDIO) },
                        testTag = "drawer_item_admin_studio"
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
            )

            // Quick Dark/Light Theme toggle
            val currentTheme = LocalThemeMode.current
            val setTheme = LocalSetThemeMode.current
            val context = LocalContext.current
            val isDark = when (currentTheme) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 2.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable {
                        val nextMode = if (isDark) ThemeMode.LIGHT else ThemeMode.DARK
                        setTheme(nextMode)
                        ThemePreferences.setThemeMode(context, nextMode)
                    }
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (isDark) Icons.Filled.DarkMode else Icons.Filled.LightMode,
                        contentDescription = "Theme",
                        tint = QYellowDark,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Text(
                        text = if (isDark) "Dark Theme" else "Light Theme",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Switch(
                    checked = isDark,
                    onCheckedChange = { checked ->
                        val nextMode = if (checked) ThemeMode.DARK else ThemeMode.LIGHT
                        setTheme(nextMode)
                        ThemePreferences.setThemeMode(context, nextMode)
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = QYellowPrimary,
                        checkedTrackColor = QYellowDark.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier.testTag("drawer_theme_switch")
                )
            }

            // Settings item
            DrawerItem(
                label = "Settings",
                icon = Icons.Outlined.Settings,
                selectedIcon = Icons.Filled.Settings,
                isSelected = selectedDestination == DrawerDestination.SETTINGS,
                onClick = { onDestinationSelect(DrawerDestination.SETTINGS) },
                testTag = "drawer_item_settings"
            )

            // 4. Logout / Bottom Footer
            if (currentUser != null) {
                DrawerItem(
                    label = "Log Out",
                    icon = Icons.AutoMirrored.Outlined.Logout,
                    selectedIcon = Icons.AutoMirrored.Filled.Logout,
                    isSelected = false,
                    accentColor = QLikeRed,
                    onClick = onLogoutClick,
                    testTag = "drawer_item_logout"
                )
            }
        }
    }
}

@Composable
private fun DrawerItem(
    label: String,
    icon: ImageVector,
    selectedIcon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    testTag: String,
    accentColor: Color? = null,
    badgeText: String? = null,
    badgeCount: Int = 0
) {
    val tint = accentColor ?: if (isSelected) QYellowPrimary else MaterialTheme.colorScheme.onSurface

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 2.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) QYellowPrimary.copy(alpha = 0.12f) else Color.Transparent)
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 12.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (isSelected) selectedIcon else icon,
            contentDescription = label,
            tint = tint,
            modifier = Modifier.size(22.dp)
        )

        Spacer(modifier = Modifier.width(14.dp))

        Text(
            text = label,
            fontSize = 14.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = tint,
            modifier = Modifier.weight(1f)
        )

        if (badgeText != null) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(QYellowDark)
                    .padding(horizontal = 5.dp, vertical = 2.dp)
            ) {
                Text(
                    text = badgeText,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.Black
                )
            }
        } else if (badgeCount > 0) {
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(QYellowDark)
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "$badgeCount",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
            }
        }
    }
}
