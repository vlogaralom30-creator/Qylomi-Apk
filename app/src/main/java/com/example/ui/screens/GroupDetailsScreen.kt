package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Rule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.GroupCommunity
import com.example.data.model.GroupMember
import com.example.data.model.Post
import com.example.data.model.UserProfile
import com.example.ui.components.PostCard
import com.example.ui.components.UserAvatar
import com.example.ui.theme.QBackground
import com.example.ui.theme.QCardBorder
import com.example.ui.theme.QOnSurface
import com.example.ui.theme.QOnSurfaceVariant
import com.example.ui.theme.QOnYellow
import com.example.ui.theme.QSurface
import com.example.ui.theme.QSurfaceVariant
import com.example.ui.theme.QYellowDark
import com.example.ui.theme.QYellowPrimary

@Composable
fun GroupDetailsScreen(
    group: GroupCommunity,
    posts: List<Post>,
    members: List<GroupMember>,
    allProfiles: List<UserProfile>,
    currentUser: UserProfile?,
    onBack: () -> Unit,
    onToggleJoin: (GroupCommunity) -> Unit,
    onOpenCreatePostForGroup: (GroupCommunity) -> Unit,
    onLikePost: (Post) -> Unit,
    onCommentPost: (Post) -> Unit,
    onBookmarkPost: (Post) -> Unit,
    onAuthorClick: (UserProfile) -> Unit,
    onEditPost: (Post) -> Unit,
    onDeletePost: (Post) -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Posts (${posts.size})", "Members (${members.size})", "About & Rules")

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(QSurface)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 8.dp)
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = QOnSurface)
                    }
                    Text(
                        text = group.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = QOnSurface,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        },
        floatingActionButton = {
            if (selectedTab == 0 && group.isJoinedByMe) {
                FloatingActionButton(
                    onClick = { onOpenCreatePostForGroup(group) },
                    containerColor = QYellowPrimary,
                    contentColor = QOnYellow,
                    shape = CircleShape,
                    modifier = Modifier.testTag("fab_post_in_group")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Post in Group")
                }
            }
        },
        containerColor = QBackground
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            // Group Header Banner / Info
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(QSurface)
                        .padding(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(64.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(QYellowPrimary.copy(alpha = 0.15f))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Group,
                                contentDescription = null,
                                tint = QYellowDark,
                                modifier = Modifier.size(34.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = group.name,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 20.sp,
                                    color = QOnSurface
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (group.privacy == "private") Icons.Default.Lock else Icons.Default.Public,
                                    contentDescription = null,
                                    tint = QOnSurfaceVariant,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "${group.privacy.capitalize()} • ${group.membersCount} Members",
                                    fontSize = 13.sp,
                                    color = QOnSurfaceVariant
                                )
                            }
                        }

                        if (group.isJoinedByMe) {
                            OutlinedButton(
                                onClick = { onToggleJoin(group) },
                                shape = RoundedCornerShape(20.dp)
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Joined", fontSize = 12.sp)
                            }
                        } else {
                            Button(
                                onClick = { onToggleJoin(group) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = QYellowPrimary,
                                    contentColor = QOnYellow
                                ),
                                shape = RoundedCornerShape(20.dp)
                            ) {
                                Text("Join", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }

                    if (!group.description.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = group.description,
                            fontSize = 14.sp,
                            color = QOnSurfaceVariant
                        )
                    }
                }
            }

            // Tabs Row
            item {
                ScrollableTabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = QSurface,
                    edgePadding = 16.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(width = 0.5.dp, color = QCardBorder)
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = { Text(title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp) },
                            selectedContentColor = QYellowDark,
                            unselectedContentColor = QOnSurfaceVariant
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Tab Content
            when (selectedTab) {
                0 -> {
                    // Posts Tab
                    if (posts.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 40.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No posts in this community yet. Be the first to share!",
                                    color = QOnSurfaceVariant,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    } else {
                        items(posts, key = { it.id }) { post ->
                            Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                                PostCard(
                                    post = post,
                                    currentUserId = currentUser?.id,
                                    onLikeClick = { onLikePost(post) },
                                    onCommentClick = { onCommentPost(post) },
                                    onBookmarkClick = { onBookmarkPost(post) },
                                    onAuthorClick = onAuthorClick,
                                    onEditPostClick = { onEditPost(post) },
                                    onDeletePostClick = { onDeletePost(post) }
                                )
                            }
                        }
                    }
                }

                1 -> {
                    // Members Tab
                    if (members.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 40.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = "Loading members...", color = QOnSurfaceVariant, fontSize = 13.sp)
                            }
                        }
                    } else {
                        items(members, key = { it.id }) { member ->
                            val memberProfile = allProfiles.find { it.id == member.userId }
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = QSurface),
                                border = androidx.compose.foundation.BorderStroke(1.dp, QCardBorder),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 4.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp)
                                ) {
                                    UserAvatar(
                                        avatarUrl = memberProfile?.avatarUrl,
                                        initials = memberProfile?.initials ?: "Q",
                                        size = 40.dp
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = memberProfile?.effectiveDisplayName ?: "Creator",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = QOnSurface
                                        )
                                        Text(
                                            text = "@${memberProfile?.effectiveUsername ?: "user"} • Role: ${member.role}",
                                            fontSize = 12.sp,
                                            color = QOnSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                2 -> {
                    // About & Rules Tab
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                        ) {
                            Card(
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = QSurface),
                                border = androidx.compose.foundation.BorderStroke(1.dp, QCardBorder),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Info, contentDescription = null, tint = QYellowDark)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Community Info",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp,
                                            color = QOnSurface
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = group.description ?: "No description provided.",
                                        fontSize = 14.sp,
                                        color = QOnSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = "Category: ${group.category ?: "General"}",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = QYellowDark
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Card(
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = QSurface),
                                border = androidx.compose.foundation.BorderStroke(1.dp, QCardBorder),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Rule, contentDescription = null, tint = QYellowDark)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Community Rules",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp,
                                            color = QOnSurface
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    val rules = group.rules
                                    if (rules.isNullOrEmpty()) {
                                        Text(
                                            text = "1. Be respectful and constructive.\n2. No spam or self-promotion.\n3. Share high-quality content.",
                                            fontSize = 13.sp,
                                            color = QOnSurfaceVariant
                                        )
                                    } else {
                                        rules.forEachIndexed { index, rule ->
                                            Text(
                                                text = "${index + 1}. $rule",
                                                fontSize = 13.sp,
                                                color = QOnSurfaceVariant,
                                                modifier = Modifier.padding(vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
