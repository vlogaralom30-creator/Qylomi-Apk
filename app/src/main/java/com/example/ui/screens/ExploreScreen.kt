package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.UserProfile
import com.example.ui.components.CreatorRankChip
import com.example.ui.components.PointsBadge
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
fun ExploreScreen(
    profiles: List<UserProfile>,
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    onProfileClick: (UserProfile) -> Unit,
    onToggleFollow: (UserProfile) -> Unit,
    isFollowing: (String) -> Boolean,
    onOpenSearch: (String) -> Unit = {}
) {
    val trendingTopics = listOf("#ArtificialIntelligence", "#Programming", "#Tech", "#ContentCreator", "#Ideas", "#Design")

    val filteredProfiles = remember(profiles, searchQuery) {
        if (searchQuery.isBlank()) {
            profiles
        } else {
            profiles.filter {
                it.effectiveDisplayName.contains(searchQuery, ignoreCase = true) ||
                        it.effectiveUsername.contains(searchQuery, ignoreCase = true) ||
                        it.headline?.contains(searchQuery, ignoreCase = true) == true ||
                        it.skills?.any { s -> s.contains(searchQuery, ignoreCase = true) } == true
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(QBackground)
    ) {
        // Search bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(QSurface)
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpenSearch(searchQuery) }
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = {
                        onSearchChange(it)
                        onOpenSearch(it)
                    },
                    placeholder = { Text("Search creators, skills, #tags...", fontSize = 14.sp) },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = "Search", tint = QYellowDark)
                    },
                    singleLine = true,
                    enabled = false, // Tap opens global search overlay
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        disabledBorderColor = QCardBorder,
                        disabledContainerColor = QSurfaceVariant,
                        disabledTextColor = QOnSurface,
                        disabledPlaceholderColor = QOnSurfaceVariant
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("explore_search_input")
                )
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 90.dp)
        ) {
            // Trending Topics
            item {
                Column(modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.TrendingUp,
                            contentDescription = null,
                            tint = QYellowDark,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Trending on Q",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = QOnSurface
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        trendingTopics.forEach { topic ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(QSurface)
                                    .border(1.dp, QCardBorder, RoundedCornerShape(16.dp))
                                    .clickable { onOpenSearch(topic) }
                                    .padding(horizontal = 12.dp, vertical = 7.dp)
                            ) {
                                Text(
                                    text = topic,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = QYellowDark
                                )
                            }
                        }
                    }
                }
            }

            // Creators Section Header
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = if (searchQuery.isNotBlank()) "Search Results (${filteredProfiles.size})" else "Discover Creators",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = QOnSurface
                    )
                }
            }

            // Creators List
            items(filteredProfiles, key = { it.id }) { creator ->
                val following = isFollowing(creator.id)

                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = QSurface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, QCardBorder),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 5.dp)
                        .clickable { onProfileClick(creator) }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(14.dp)
                    ) {
                        UserAvatar(
                            avatarUrl = creator.avatarUrl,
                            initials = creator.initials,
                            size = 48.dp
                        )

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = creator.effectiveDisplayName,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = QOnSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            Text(
                                text = "@${creator.effectiveUsername}",
                                fontSize = 12.sp,
                                color = QOnSurfaceVariant
                            )

                            if (!creator.headline.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = creator.headline,
                                    fontSize = 12.sp,
                                    color = QOnSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Spacer(modifier = Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CreatorRankChip(rank = creator.creatorRank, level = creator.creatorLevel)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "${creator.points ?: 0} pts",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = QYellowDark
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        // Follow Button
                        if (following) {
                            OutlinedButton(
                                onClick = { onToggleFollow(creator) },
                                shape = RoundedCornerShape(18.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                modifier = Modifier.height(34.dp)
                            ) {
                                Text("Following", fontSize = 12.sp, color = QOnSurfaceVariant)
                            }
                        } else {
                            Button(
                                onClick = { onToggleFollow(creator) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = QYellowPrimary,
                                    contentColor = QOnYellow
                                ),
                                shape = RoundedCornerShape(18.dp),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
                                modifier = Modifier.height(34.dp)
                            ) {
                                Text("Follow", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}
