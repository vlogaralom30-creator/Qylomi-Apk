package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.model.UserProfile
import com.example.ui.components.QBottomNavBar
import com.example.ui.components.QTopAppBar
import com.example.ui.components.SocialTab
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.CommentsBottomSheet
import com.example.ui.screens.CreatePostBottomSheet
import com.example.ui.screens.EditProfileScreen
import com.example.ui.screens.ExploreScreen
import com.example.ui.screens.FeedScreen
import com.example.ui.screens.NotificationsScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.QBackground
import com.example.ui.theme.QOnYellow
import com.example.ui.theme.QYellowDark
import com.example.ui.theme.QYellowPrimary
import com.example.ui.viewmodel.SocialViewModel
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                QSocialApp()
            }
        }
    }
}

@Composable
fun QSocialApp(viewModel: SocialViewModel = viewModel()) {
    val isLoggedIn by viewModel.isLoggedIn.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val feedPosts by viewModel.feedPosts.collectAsState()
    val isFeedLoading by viewModel.isFeedLoading.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val allProfiles by viewModel.allProfiles.collectAsState()
    val viewedProfile by viewModel.viewedProfile.collectAsState()
    val viewedUserPosts by viewModel.viewedUserPosts.collectAsState()
    val selectedPostForComments by viewModel.selectedPostForComments.collectAsState()
    val comments by viewModel.comments.collectAsState()
    val isCommentsLoading by viewModel.isCommentsLoading.collectAsState()
    val notifications by viewModel.notifications.collectAsState()
    val savedPosts by viewModel.savedPosts.collectAsState()
    val trackersCount by viewModel.trackersCount.collectAsState()
    val trackingCount by viewModel.trackingCount.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val bannerMessage by viewModel.bannerMessage.collectAsState()
    val savedAccounts by viewModel.savedAccounts.collectAsState()

    var currentTab by remember { mutableStateOf(SocialTab.FEED) }
    var isEditingProfile by remember { mutableStateOf(false) }
    var isCreatePostOpen by remember { mutableStateOf(false) }
    var selectedUserProfile by remember { mutableStateOf<UserProfile?>(null) }

    // Auto-dismiss banner after 3 seconds
    LaunchedEffect(bannerMessage) {
        if (bannerMessage != null) {
            delay(3000)
            viewModel.clearBanner()
        }
    }

    if (!isLoggedIn) {
        AuthScreen(
            onLogin = { email, pass, onDone ->
                viewModel.login(email, pass, onDone)
            },
            onSignUp = { email, pass, username, displayName, onDone ->
                viewModel.signUp(email, pass, username, displayName, onDone)
            },
            savedAccounts = savedAccounts,
            onSelectSavedAccount = { account ->
                viewModel.login(account.email, account.pass) { success, err ->
                    if (!success) {
                        viewModel.setBanner(err ?: "Login failed for ${account.email}")
                    }
                }
            },
            onRemoveSavedAccount = { account ->
                viewModel.removeSavedAccount(account)
            },
            onContinueAsGuest = {
                val guestProfile = UserProfile(
                    id = "guest_${System.currentTimeMillis()}",
                    username = "guest_explorer",
                    displayName = "Guest Explorer",
                    bio = "Exploring Qylomi in guest mode.",
                    creatorRank = "Novice Explorer",
                    creatorLevel = 1,
                    points = 20
                )
                viewModel.loginAsDemoUser(guestProfile)
            }
        )
    } else {
        Scaffold(
            topBar = {
                if (!isEditingProfile && selectedUserProfile == null) {
                    QTopAppBar(
                        currentUser = currentUser,
                        unreadNotifications = 2,
                        onNotificationsClick = { currentTab = SocialTab.NOTIFICATIONS },
                        onProfileClick = {
                            selectedUserProfile = null
                            currentTab = SocialTab.PROFILE
                        }
                    )
                }
            },
            bottomBar = {
                if (!isEditingProfile && selectedUserProfile == null) {
                    QBottomNavBar(
                        selectedTab = currentTab,
                        onTabSelected = { tab ->
                            if (tab == SocialTab.CREATE) {
                                isCreatePostOpen = true
                            } else {
                                selectedUserProfile = null
                                currentTab = tab
                            }
                        }
                    )
                }
            },
            containerColor = QBackground
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                // Main Content View Routing
                when {
                    isEditingProfile -> {
                        currentUser?.let { user ->
                            EditProfileScreen(
                                currentProfile = user,
                                onBack = { isEditingProfile = false },
                                onSave = { id, updates, onDone ->
                                    viewModel.updateProfile(id, updates, onDone)
                                }
                            )
                        }
                    }

                    selectedUserProfile != null -> {
                        selectedUserProfile?.let { profile ->
                            ProfileScreen(
                                profile = profile,
                                currentUser = currentUser,
                                userPosts = viewedUserPosts,
                                savedPosts = emptyList(),
                                trackersCount = trackersCount,
                                trackingCount = trackingCount,
                                isOwnProfile = profile.id == currentUser?.id,
                                onEditProfileClick = { isEditingProfile = true },
                                onBackClick = { selectedUserProfile = null },
                                onLogoutClick = { viewModel.logout() },
                                onLikePost = { post -> viewModel.toggleLike(post) },
                                onCommentPost = { post -> viewModel.openComments(post) },
                                onBookmarkPost = { post -> viewModel.toggleBookmark(post) },
                                onToggleFollow = { target -> viewModel.toggleFollow(target) },
                                isFollowing = viewModel.isUserFollowed(profile.id)
                            )
                        }
                    }

                    else -> {
                        when (currentTab) {
                            SocialTab.FEED -> {
                                FeedScreen(
                                    posts = feedPosts,
                                    isLoading = isFeedLoading,
                                    currentUser = currentUser,
                                    allCreators = allProfiles,
                                    selectedCategory = selectedCategory,
                                    onCategorySelected = { cat -> viewModel.setCategory(cat) },
                                    onLikeClick = { post -> viewModel.toggleLike(post) },
                                    onCommentClick = { post -> viewModel.openComments(post) },
                                    onBookmarkClick = { post -> viewModel.toggleBookmark(post) },
                                    onAuthorClick = { author ->
                                        selectedUserProfile = author
                                        viewModel.viewUserProfile(author)
                                    },
                                    onRefresh = { viewModel.loadFeed() },
                                    onCreatePostClick = { isCreatePostOpen = true }
                                )
                            }

                            SocialTab.EXPLORE -> {
                                ExploreScreen(
                                    profiles = allProfiles,
                                    searchQuery = searchQuery,
                                    onSearchChange = { q -> viewModel.setSearchQuery(q) },
                                    onProfileClick = { profile ->
                                        selectedUserProfile = profile
                                        viewModel.viewUserProfile(profile)
                                    },
                                    onToggleFollow = { profile -> viewModel.toggleFollow(profile) },
                                    isFollowing = { id -> viewModel.isUserFollowed(id) }
                                )
                            }

                            SocialTab.NOTIFICATIONS -> {
                                NotificationsScreen(
                                    notifications = notifications,
                                    onRefresh = { viewModel.loadNotifications() }
                                )
                            }

                            SocialTab.PROFILE -> {
                                currentUser?.let { user ->
                                    ProfileScreen(
                                        profile = user,
                                        currentUser = currentUser,
                                        userPosts = viewedUserPosts.ifEmpty {
                                            feedPosts.filter { it.userId == user.id }
                                        },
                                        savedPosts = savedPosts,
                                        trackersCount = trackersCount,
                                        trackingCount = trackingCount,
                                        isOwnProfile = true,
                                        onEditProfileClick = { isEditingProfile = true },
                                        onBackClick = { currentTab = SocialTab.FEED },
                                        onLogoutClick = { viewModel.logout() },
                                        onLikePost = { post -> viewModel.toggleLike(post) },
                                        onCommentPost = { post -> viewModel.openComments(post) },
                                        onBookmarkPost = { post -> viewModel.toggleBookmark(post) },
                                        onToggleFollow = { target -> viewModel.toggleFollow(target) },
                                        isFollowing = false
                                    )
                                }
                            }

                            SocialTab.CREATE -> {
                                // handled by modal
                            }
                        }
                    }
                }

                // Global Banner Message
                AnimatedVisibility(
                    visible = bannerMessage != null,
                    enter = fadeIn(),
                    exit = fadeOut(),
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(QYellowPrimary)
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                    ) {
                        Text(
                            text = bannerMessage ?: "",
                            color = QOnYellow,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }

            // Create Post Sheet
            if (isCreatePostOpen) {
                CreatePostBottomSheet(
                    currentUser = currentUser,
                    onDismiss = { isCreatePostOpen = false },
                    onPublishGeneral = { type, title, content, url, onDone ->
                        viewModel.createPost(type, title, content, url, onDone)
                    },
                    onPublishVideo = { title, videoUrl, thumbnailUrl, category, tags, onDone ->
                        viewModel.createVideoPost(title, videoUrl, thumbnailUrl, category, tags, onDone)
                    },
                    onPublishPhoto = { caption, photoUrls, category, tags, onDone ->
                        viewModel.createPhotoPost(caption, photoUrls, category, tags, onDone)
                    },
                    onPublishText = { caption, templateId, category, tags, onDone ->
                        viewModel.createTextPost(caption, templateId, category, tags, onDone)
                    }
                )
            }

            // Comments BottomSheet
            selectedPostForComments?.let { post ->
                CommentsBottomSheet(
                    post = post,
                    comments = comments,
                    isLoading = isCommentsLoading,
                    currentUser = currentUser,
                    onDismiss = { viewModel.closeComments() },
                    onAddComment = { text -> viewModel.addComment(text) {} }
                )
            }
        }
    }
}
