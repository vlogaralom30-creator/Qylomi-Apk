package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Tag
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Post
import com.example.data.model.Tag
import com.example.data.model.UserProfile
import com.example.ui.components.PostCard
import com.example.ui.theme.*
import com.example.ui.viewmodel.SearchViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TagFeedScreen(
    viewModel: SearchViewModel,
    currentUser: UserProfile?,
    onPostClick: (Post) -> Unit,
    onUserClick: (UserProfile) -> Unit,
    onLikeClick: (String) -> Unit,
    onCommentClick: (Post) -> Unit,
    onShareClick: (Post) -> Unit,
    onOptionClick: (Post) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tag by viewModel.selectedTagForFeed.collectAsState()
    val posts by viewModel.tagFeedPosts.collectAsState()
    val isLoading by viewModel.isTagFeedLoading.collectAsState()
    val isRefreshing by viewModel.isTagFeedRefreshing.collectAsState()

    if (tag == null) return

    val currentTag = tag!!

    Surface(
        modifier = modifier
            .fillMaxSize()
            .testTag("tag_feed_screen"),
        color = QBackground
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top App Bar
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(QSurface)
                    .statusBarsPadding()
                    .border(width = 0.5.dp, color = QCardBorder)
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                IconButton(
                    onClick = {
                        viewModel.closeTagFeed()
                        onClose()
                    },
                    modifier = Modifier.testTag("tag_feed_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = QOnSurface
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "#${currentTag.name}",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = QYellowDark
                        )
                    }
                    if (currentTag.postsCount > 0) {
                        Text(
                            text = "${currentTag.postsCount} posts",
                            fontSize = 12.sp,
                            color = QOnSurfaceVariant
                        )
                    }
                }
            }

            // Feed Content with PullToRefreshBox
            PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = { viewModel.loadTagFeed(currentTag, isRefresh = true) },
                modifier = Modifier.fillMaxSize()
            ) {
                if (isLoading && posts.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = QYellowPrimary)
                    }
                } else if (!isLoading && posts.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Outlined.Tag,
                                contentDescription = null,
                                tint = QOnSurfaceVariant,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "No posts found for #${currentTag.name}",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium,
                                color = QOnSurfaceVariant
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(posts, key = { it.id }) { post ->
                            PostCard(
                                post = post,
                                currentUserId = currentUser?.id,
                                onLikeClick = { onLikeClick(post.id) },
                                onCommentClick = { onCommentClick(post) },
                                onAuthorClick = { author -> onUserClick(author) },
                                onBookmarkClick = { },
                                onOpenMenu = onOptionClick
                            )
                        }
                    }
                }
            }
        }
    }
}
