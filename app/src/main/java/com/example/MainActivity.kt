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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.model.Post
import com.example.data.model.UserProfile
import com.example.ui.components.EditPostBottomSheet
import com.example.ui.components.LikedByBottomSheet
import com.example.ui.components.PostActionMenuBottomSheet
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
import com.example.ui.theme.QYellowPrimary
import androidx.compose.ui.platform.LocalContext
import com.example.ui.screens.GlobalSearchOverlay
import com.example.ui.screens.TagFeedScreen
import com.example.ui.viewmodel.CommentsViewModel
import com.example.ui.viewmodel.PostActionsViewModel
import com.example.ui.viewmodel.SearchViewModel
import com.example.ui.viewmodel.SocialViewModel
import com.example.ui.viewmodel.CreatorStudioViewModel
import com.example.ui.viewmodel.AdminStudioViewModel
import com.example.ui.screens.studio.CreatorStudioScreen
import com.example.ui.screens.studio.AdminStudioScreen
import com.example.ui.viewmodel.SeriesViewModel
import com.example.ui.screens.series.SeriesHomeScreen
import com.example.ui.screens.series.SeriesListScreen
import com.example.ui.screens.series.SeriesDetailsScreen
import com.example.ui.screens.series.EpisodePlayerScreen
import androidx.compose.runtime.mutableIntStateOf
import com.example.ui.components.DrawerDestination
import com.example.ui.components.QNavigationDrawerSheet
import com.example.data.model.ShelfWithItems
import androidx.activity.compose.BackHandler
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.example.ui.components.BrandedSplash
import androidx.compose.runtime.saveable.rememberSaveable


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        
        // Remove system splash immediately to let Compose splash take over
        splashScreen.setOnExitAnimationListener { it.remove() }
        
        enableEdgeToEdge()
        setContent {
            var showSplash by rememberSaveable { mutableStateOf(true) }
            val socialViewModel: SocialViewModel = viewModel()
            val isReady by socialViewModel.isReady.collectAsState()

            MyApplicationTheme {
                Box(modifier = Modifier.fillMaxSize()) {
                    QSocialApp(viewModel = socialViewModel)
                    
                    if (showSplash) {
                        BrandedSplash(
                            isReady = isReady,
                            onSplashFinished = {
                                showSplash = false
                            }
                        )
                    }
                }
            }
        }
    }


}

@Composable
fun QSocialApp(
    viewModel: SocialViewModel = viewModel(),
    postActionsViewModel: PostActionsViewModel = viewModel(),
    commentsViewModel: CommentsViewModel = viewModel(),
    searchViewModel: SearchViewModel = viewModel(factory = ViewModelProvider.AndroidViewModelFactory.getInstance(androidx.compose.ui.platform.LocalContext.current.applicationContext as android.app.Application)),
    creatorStudioViewModel: CreatorStudioViewModel = viewModel(),
    adminStudioViewModel: AdminStudioViewModel = viewModel(),
    seriesViewModel: SeriesViewModel = viewModel()
) {
    val context = LocalContext.current
    val isLoggedIn by viewModel.isLoggedIn.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val feedPosts by viewModel.feedPosts.collectAsState()
    val isFeedLoading by viewModel.isFeedLoading.collectAsState()
    val isFeedRefreshing by viewModel.isFeedRefreshing.collectAsState()
    val isProfileRefreshing by viewModel.isProfileRefreshing.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val allProfiles by viewModel.allProfiles.collectAsState()
    val viewedUserPosts by viewModel.viewedUserPosts.collectAsState()
    val notifications by viewModel.notifications.collectAsState()
    val savedPosts by viewModel.savedPosts.collectAsState()
    val trackersCount by viewModel.trackersCount.collectAsState()
    val trackingCount by viewModel.trackingCount.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val bannerMessage by viewModel.bannerMessage.collectAsState()
    val savedAccounts by viewModel.savedAccounts.collectAsState()
    val groups by viewModel.groups.collectAsState()
    val isGroupsLoading by viewModel.isGroupsLoading.collectAsState()
    val selectedGroup by viewModel.selectedGroup.collectAsState()
    val groupPosts by viewModel.groupPosts.collectAsState()
    val groupMembers by viewModel.groupMembers.collectAsState()

    // Post Actions ViewModel State
    val selectedPostForLikers by postActionsViewModel.selectedPostForLikers.collectAsState()
    val postLikers by postActionsViewModel.postLikers.collectAsState()
    val isLikersLoading by postActionsViewModel.isLikersLoading.collectAsState()

    // Comments ViewModel State
    val selectedPostForComments by commentsViewModel.selectedPost.collectAsState()
    val comments by commentsViewModel.comments.collectAsState()
    val isCommentsLoading by commentsViewModel.isLoading.collectAsState()
    val commentSortOrder by commentsViewModel.sortOrder.collectAsState()
    val replyToComment by commentsViewModel.replyToComment.collectAsState()
    val editingComment by commentsViewModel.editingComment.collectAsState()

    val selectedCommentForLikers by commentsViewModel.selectedCommentForLikers.collectAsState()
    val commentLikers by commentsViewModel.commentLikers.collectAsState()
    val isCommentLikersLoading by commentsViewModel.isCommentLikersLoading.collectAsState()

    // Drawer and Series state
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()

    var isSeriesCatalogOpen by remember { mutableStateOf(false) }
    var selectedSeriesSlug by remember { mutableStateOf<String?>(null) }
    var selectedShelfForList by remember { mutableStateOf<ShelfWithItems?>(null) }
    var activePlayerSeriesId by remember { mutableStateOf<String?>(null) }
    var activePlayerEpisodeId by remember { mutableStateOf<String?>(null) }
    var activePlayerStartPos by remember { mutableIntStateOf(0) }

    // UI state
    var currentTab by remember { mutableStateOf(SocialTab.FEED) }
    var isEditingProfile by remember { mutableStateOf(false) }
    var isCreatePostOpen by remember { mutableStateOf(false) }
    var selectedUserProfile by remember { mutableStateOf<UserProfile?>(null) }
    var isGroupsScreenOpen by remember { mutableStateOf(false) }
    var selectedGroupForPost by remember { mutableStateOf<com.example.data.model.GroupCommunity?>(null) }
    var isReelsScreenOpen by remember { mutableStateOf(false) }
    var selectedReelPost by remember { mutableStateOf<Post?>(null) }
    var isCreatorStudioOpen by remember { mutableStateOf(false) }
    var isAdminStudioOpen by remember { mutableStateOf(false) }

    var selectedPostForMenu by remember { mutableStateOf<Post?>(null) }
    var editingPost by remember { mutableStateOf<Post?>(null) }

    LaunchedEffect(currentUser) {
        if (currentUser == null) {
            isCreatorStudioOpen = false
            isAdminStudioOpen = false
        } else {
            if (!currentUser!!.canCreateSeries) isCreatorStudioOpen = false
            if (!currentUser!!.isAdmin) isAdminStudioOpen = false
        }
    }

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
        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                QNavigationDrawerSheet(
                    currentUser = currentUser,
                    unreadNotifications = 2,
                    selectedDestination = when {
                        isCreatorStudioOpen -> DrawerDestination.CREATOR_STUDIO
                        isAdminStudioOpen -> DrawerDestination.ADMIN_STUDIO
                        isSeriesCatalogOpen -> DrawerDestination.SERIES
                        isGroupsScreenOpen -> DrawerDestination.GROUPS
                        isReelsScreenOpen -> DrawerDestination.REELS
                        currentTab == SocialTab.FEED -> DrawerDestination.FEED
                        currentTab == SocialTab.EXPLORE -> DrawerDestination.EXPLORE
                        currentTab == SocialTab.NOTIFICATIONS -> DrawerDestination.NOTIFICATIONS
                        else -> DrawerDestination.FEED
                    },
                    onDestinationSelect = { dest ->
                        coroutineScope.launch { drawerState.close() }
                        when (dest) {
                            DrawerDestination.FEED -> {
                                isSeriesCatalogOpen = false
                                selectedSeriesSlug = null
                                selectedShelfForList = null
                                isGroupsScreenOpen = false
                                isReelsScreenOpen = false
                                selectedUserProfile = null
                                viewModel.selectGroup(null)
                                currentTab = SocialTab.FEED
                            }
                            DrawerDestination.SERIES -> {
                                selectedSeriesSlug = null
                                selectedShelfForList = null
                                isSeriesCatalogOpen = true
                            }
                            DrawerDestination.EXPLORE -> {
                                isSeriesCatalogOpen = false
                                selectedSeriesSlug = null
                                selectedShelfForList = null
                                isGroupsScreenOpen = false
                                isReelsScreenOpen = false
                                selectedUserProfile = null
                                viewModel.selectGroup(null)
                                currentTab = SocialTab.EXPLORE
                            }
                            DrawerDestination.GROUPS -> {
                                isSeriesCatalogOpen = false
                                selectedSeriesSlug = null
                                selectedShelfForList = null
                                viewModel.loadGroups()
                                isGroupsScreenOpen = true
                            }
                            DrawerDestination.REELS -> {
                                isSeriesCatalogOpen = false
                                selectedSeriesSlug = null
                                selectedShelfForList = null
                                selectedReelPost = null
                                isReelsScreenOpen = true
                            }
                            DrawerDestination.SAVED -> {
                                isSeriesCatalogOpen = false
                                selectedSeriesSlug = null
                                selectedShelfForList = null
                                isGroupsScreenOpen = false
                                isReelsScreenOpen = false
                                selectedUserProfile = null
                                viewModel.selectGroup(null)
                                currentTab = SocialTab.PROFILE
                            }
                            DrawerDestination.NOTIFICATIONS -> {
                                isSeriesCatalogOpen = false
                                selectedSeriesSlug = null
                                selectedShelfForList = null
                                isGroupsScreenOpen = false
                                isReelsScreenOpen = false
                                selectedUserProfile = null
                                viewModel.selectGroup(null)
                                currentTab = SocialTab.NOTIFICATIONS
                            }
                            DrawerDestination.CREATOR_STUDIO -> {
                                if (currentUser?.canCreateSeries == true) {
                                    isCreatorStudioOpen = true
                                }
                            }
                            DrawerDestination.ADMIN_STUDIO -> {
                                if (currentUser?.isAdmin == true) {
                                    isAdminStudioOpen = true
                                }
                            }
                        }
                    },
                    onLogoutClick = {
                        coroutineScope.launch { drawerState.close() }
                        viewModel.logout()
                    },
                    onProfileClick = {
                        coroutineScope.launch { drawerState.close() }
                        selectedUserProfile = null
                        viewModel.selectGroup(null)
                        isGroupsScreenOpen = false
                        currentTab = SocialTab.PROFILE
                    }
                )
            }
        ) {
            BackHandler(enabled = drawerState.isOpen) {
                coroutineScope.launch { drawerState.close() }
            }

            Scaffold(
                topBar = {
                    if (!isEditingProfile && selectedUserProfile == null && selectedGroup == null && !isGroupsScreenOpen && !isReelsScreenOpen && !isSeriesCatalogOpen) {
                        QTopAppBar(
                            currentUser = currentUser,
                            unreadNotifications = 2,
                            onNotificationsClick = { currentTab = SocialTab.NOTIFICATIONS },
                            onMenuClick = {
                                coroutineScope.launch { drawerState.open() }
                            },
                            onSeriesClick = {
                                isSeriesCatalogOpen = true
                            },
                            onReelsClick = {
                                selectedReelPost = null
                                isReelsScreenOpen = true
                            },
                            onSearchClick = {
                                searchViewModel.openSearchOverlay()
                            },
                            onProfileClick = {
                                selectedUserProfile = null
                                viewModel.selectGroup(null)
                                isGroupsScreenOpen = false
                                currentTab = SocialTab.PROFILE
                            }
                        )
                    }
                },
                bottomBar = {
                    if (!isEditingProfile && selectedUserProfile == null && selectedGroup == null && !isGroupsScreenOpen && !isReelsScreenOpen && !isSeriesCatalogOpen) {
                        QBottomNavBar(
                            selectedTab = currentTab,
                            onTabSelected = { tab ->
                                if (tab == SocialTab.CREATE) {
                                    isCreatePostOpen = true
                                } else {
                                    selectedUserProfile = null
                                    viewModel.selectGroup(null)
                                    isGroupsScreenOpen = false
                                    isSeriesCatalogOpen = false
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
                                isRefreshing = isProfileRefreshing,
                                onRefresh = { viewModel.refreshProfile(profile.id) },
                                onEditProfileClick = { isEditingProfile = true },
                                onBackClick = { selectedUserProfile = null },
                                onLogoutClick = { viewModel.logout() },
                                onLikePost = { post -> viewModel.toggleLike(post) },
                                onCommentPost = { post -> commentsViewModel.openComments(post) },
                                onBookmarkPost = { post -> viewModel.toggleBookmark(post) },
                                onToggleFollow = { target -> viewModel.toggleFollow(target) },
                                isFollowing = viewModel.isUserFollowed(profile.id),
                                onOpenMenu = { post -> selectedPostForMenu = post },
                                onLikersClick = { post -> postActionsViewModel.openLikers(post) },
                                onOpenCreatorStudio = if (profile.id == currentUser?.id && currentUser?.canCreateSeries == true) { { isCreatorStudioOpen = true } } else null,
                                onOpenAdminStudio = if (profile.id == currentUser?.id && currentUser?.isAdmin == true) { { isAdminStudioOpen = true } } else null
                            )
                        }
                    }

                    selectedGroup != null -> {
                        com.example.ui.screens.GroupDetailsScreen(
                            group = selectedGroup!!,
                            posts = groupPosts,
                            members = groupMembers,
                            allProfiles = allProfiles,
                            currentUser = currentUser,
                            onBack = { viewModel.selectGroup(null) },
                            onToggleJoin = { group -> viewModel.toggleJoinGroup(group) },
                            onOpenCreatePostForGroup = { group -> selectedGroupForPost = group },
                            onLikePost = { post -> viewModel.toggleLike(post) },
                            onCommentPost = { post -> commentsViewModel.openComments(post) },
                            onBookmarkPost = { post -> viewModel.toggleBookmark(post) },
                            onAuthorClick = { author ->
                                selectedUserProfile = author
                                viewModel.viewUserProfile(author)
                            },
                            onEditPost = { post -> editingPost = post },
                            onDeletePost = { post -> viewModel.deletePost(post.id) { _, _ -> } }
                        )
                    }

                    isGroupsScreenOpen -> {
                        com.example.ui.screens.GroupsScreen(
                            groups = groups,
                            isLoading = isGroupsLoading,
                            onGroupClick = { group ->
                                viewModel.selectGroup(group)
                                isGroupsScreenOpen = false
                            },
                            onToggleJoin = { group -> viewModel.toggleJoinGroup(group) },
                            onCreateGroupClick = {
                                viewModel.createCommunityGroup("New Community", "A community for creators", "General") { _, _ -> }
                            }
                        )
                    }

                    else -> {
                        when (currentTab) {
                            SocialTab.FEED -> {
                                FeedScreen(
                                    posts = feedPosts,
                                    isLoading = isFeedLoading,
                                    isRefreshing = isFeedRefreshing,
                                    currentUser = currentUser,
                                    allCreators = allProfiles,
                                    selectedCategory = selectedCategory,
                                    onCategorySelected = { cat -> viewModel.setCategory(cat) },
                                    onLikeClick = { post -> viewModel.toggleLike(post) },
                                    onCommentClick = { post -> commentsViewModel.openComments(post) },
                                    onBookmarkClick = { post -> viewModel.toggleBookmark(post) },
                                    onAuthorClick = { author ->
                                        selectedUserProfile = author
                                        viewModel.viewUserProfile(author)
                                    },
                                    onRefresh = { viewModel.refreshFeed() },
                                    onCreatePostClick = { isCreatePostOpen = true },
                                    onEditPostClick = { post -> editingPost = post },
                                    onDeletePostClick = { post ->
                                        viewModel.deletePost(post.id) { _, _ -> }
                                    },
                                    onGroupsClick = {
                                        viewModel.loadGroups()
                                        isGroupsScreenOpen = true
                                    },
                                    onOpenReels = { post ->
                                        selectedReelPost = post
                                        isReelsScreenOpen = true
                                    },
                                    onOpenMenu = { post -> selectedPostForMenu = post },
                                    onLikersClick = { post -> postActionsViewModel.openLikers(post) }
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
                                    isFollowing = { id -> viewModel.isUserFollowed(id) },
                                    onOpenSearch = { query -> searchViewModel.openSearchOverlay(query) }
                                )
                            }

                            SocialTab.NOTIFICATIONS -> {
                                NotificationsScreen(
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
                                        isRefreshing = isProfileRefreshing,
                                        onRefresh = { viewModel.refreshProfile(user.id) },
                                        onEditProfileClick = { isEditingProfile = true },
                                        onBackClick = { currentTab = SocialTab.FEED },
                                        onLogoutClick = { viewModel.logout() },
                                        onLikePost = { post -> viewModel.toggleLike(post) },
                                        onCommentPost = { post -> commentsViewModel.openComments(post) },
                                        onBookmarkPost = { post -> viewModel.toggleBookmark(post) },
                                        onToggleFollow = { target -> viewModel.toggleFollow(target) },
                                        isFollowing = false,
                                        onOpenMenu = { post -> selectedPostForMenu = post },
                                        onLikersClick = { post -> postActionsViewModel.openLikers(post) },
                                        onOpenCreatorStudio = if (user.canCreateSeries) { { isCreatorStudioOpen = true } } else null,
                                        onOpenAdminStudio = if (user.isAdmin) { { isAdminStudioOpen = true } } else null
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
            if (isCreatePostOpen || selectedGroupForPost != null) {
                val targetGroup = selectedGroupForPost ?: selectedGroup
                CreatePostBottomSheet(
                    currentUser = currentUser,
                    onDismiss = {
                        isCreatePostOpen = false
                        selectedGroupForPost = null
                    },
                    onPublishGeneral = { type, title, content, url, onDone ->
                        if (targetGroup != null) {
                            viewModel.createTextPostInGroup(targetGroup.id, content, "preset_default", targetGroup.category, emptyList(), onDone)
                        } else {
                            viewModel.createPost(type, title, content, url, onDone)
                        }
                    },
                    onPublishVideo = { title, videoUrl, embedUrl, directUrl, platform, thumbnailUrl, durationSeconds, width, height, isVertical, category, tags, onDone ->
                        if (targetGroup != null) {
                            viewModel.createVideoPostInGroup(
                                groupId = targetGroup.id,
                                title = title,
                                videoUrl = videoUrl,
                                embedUrl = embedUrl,
                                directUrl = directUrl,
                                platform = platform,
                                thumbnailUrl = thumbnailUrl,
                                durationSeconds = durationSeconds,
                                width = width,
                                height = height,
                                isVertical = isVertical,
                                category = category,
                                tags = tags,
                                onDone = onDone
                            )
                        } else {
                            viewModel.createVideoPost(
                                title = title,
                                videoUrl = videoUrl,
                                embedUrl = embedUrl,
                                directUrl = directUrl,
                                platform = platform,
                                thumbnailUrl = thumbnailUrl,
                                durationSeconds = durationSeconds,
                                width = width,
                                height = height,
                                isVertical = isVertical,
                                category = category,
                                tags = tags,
                                onDone = onDone
                            )
                        }
                    },
                    onPublishPhoto = { caption, photoUrls, category, tags, onDone ->
                        if (targetGroup != null) {
                            viewModel.createPhotoPostInGroup(targetGroup.id, caption, photoUrls, category, tags, onDone)
                        } else {
                            viewModel.createPhotoPost(caption, photoUrls, category, tags, onDone)
                        }
                    },
                    onPublishText = { caption, templateId, category, tags, onDone ->
                        if (targetGroup != null) {
                            viewModel.createTextPostInGroup(targetGroup.id, caption, templateId, category, tags, onDone)
                        } else {
                            viewModel.createTextPost(caption, templateId, category, tags, onDone)
                        }
                    }
                )
            }

            // Post Action Menu Sheet
            selectedPostForMenu?.let { post ->
                val author = post.authorProfile
                val isFollowingAuthor = author != null && viewModel.isUserFollowed(author.id)

                PostActionMenuBottomSheet(
                    post = post,
                    currentUserId = currentUser?.id,
                    isAuthorFollowing = isFollowingAuthor,
                    onDismiss = { selectedPostForMenu = null },
                    onEditClick = { editingPost = it },
                    onDeleteClick = { postToDelete ->
                        viewModel.deletePost(postToDelete.id) { success, err ->
                            if (success) {
                                viewModel.setBanner("Post deleted successfully")
                            } else {
                                viewModel.setBanner(err ?: "Failed to delete post")
                            }
                        }
                    },
                    onTogglePin = { targetPost ->
                        postActionsViewModel.togglePin(targetPost) { success, msg ->
                            if (success) {
                                viewModel.setBanner(msg ?: "Pin updated")
                                viewModel.loadFeed()
                            } else {
                                viewModel.setBanner(msg ?: "Failed to pin post")
                            }
                        }
                    },
                    onToggleAllowComments = { targetPost ->
                        postActionsViewModel.toggleAllowComments(targetPost) { success, msg ->
                            if (success) {
                                viewModel.setBanner(msg ?: "Comments setting updated")
                                viewModel.loadFeed()
                            } else {
                                viewModel.setBanner(msg ?: "Failed to update comments setting")
                            }
                        }
                    },
                    onToggleBookmark = { targetPost ->
                        viewModel.toggleBookmark(targetPost)
                    },
                    onToggleFollow = { authorProfile ->
                        viewModel.toggleFollow(authorProfile)
                    },
                    onViewProfile = { authorProfile ->
                        selectedUserProfile = authorProfile
                        viewModel.viewUserProfile(authorProfile)
                    },
                    onHidePost = { targetPost ->
                        postActionsViewModel.hidePost(targetPost) {
                            viewModel.setBanner("Post hidden. Refreshing feed...")
                            viewModel.loadFeed()
                        }
                    },
                    onReportPost = { targetPost, reason, details ->
                        postActionsViewModel.submitReport(targetPost.id, null, reason, details) { success, msg ->
                            viewModel.setBanner(msg)
                        }
                    },
                    onShowBanner = { msg ->
                        viewModel.setBanner(msg)
                    }
                )
            }

            // Edit Post Sheet
            editingPost?.let { post ->
                EditPostBottomSheet(
                    post = post,
                    onDismiss = { editingPost = null },
                    onSaveEdit = { newContent, newTitle ->
                        viewModel.editPost(post.id, newContent, newTitle) { success, err ->
                            if (success) {
                                viewModel.setBanner("Post updated successfully!")
                                editingPost = null
                            } else {
                                viewModel.setBanner(err ?: "Failed to edit post")
                            }
                        }
                    }
                )
            }

            // Post Likers Sheet ("Liked by N")
            selectedPostForLikers?.let { post ->
                val users = postLikers.mapNotNull { it.userProfile }
                LikedByBottomSheet(
                    title = "Liked by (${users.size})",
                    users = users,
                    isLoading = isLikersLoading,
                    currentUserId = currentUser?.id,
                    onDismiss = { postActionsViewModel.closeLikers() },
                    onUserClick = { profile ->
                        selectedUserProfile = profile
                        viewModel.viewUserProfile(profile)
                    },
                    onToggleFollow = { target ->
                        viewModel.toggleFollow(target)
                    },
                    isFollowing = { id -> viewModel.isUserFollowed(id) }
                )
            }

            // Comment Likers Sheet
            selectedCommentForLikers?.let { comment ->
                val users = commentLikers.mapNotNull { it.userProfile }
                LikedByBottomSheet(
                    title = "Comment Liked by (${users.size})",
                    users = users,
                    isLoading = isCommentLikersLoading,
                    currentUserId = currentUser?.id,
                    onDismiss = { commentsViewModel.closeCommentLikers() },
                    onUserClick = { profile ->
                        selectedUserProfile = profile
                        viewModel.viewUserProfile(profile)
                    },
                    onToggleFollow = { target ->
                        viewModel.toggleFollow(target)
                    },
                    isFollowing = { id -> viewModel.isUserFollowed(id) }
                )
            }

            // Comments BottomSheet
            selectedPostForComments?.let { post ->
                CommentsBottomSheet(
                    post = post,
                    comments = comments,
                    isLoading = isCommentsLoading,
                    sortOrder = commentSortOrder,
                    replyToComment = replyToComment,
                    editingComment = editingComment,
                    currentUser = currentUser,
                    onDismiss = { commentsViewModel.closeComments() },
                    onSortSelected = { order -> commentsViewModel.setSortOrder(order) },
                    onReplyTo = { comment -> commentsViewModel.setReplyTo(comment) },
                    onStartEditing = { comment -> commentsViewModel.setEditingComment(comment) },
                    onAddComment = { text ->
                        commentsViewModel.addComment(text) { success, err ->
                            if (success) {
                                viewModel.loadFeed()
                            } else {
                                viewModel.setBanner(err ?: "Failed to send comment")
                            }
                        }
                    },
                    onUpdateComment = { comment, newContent ->
                        commentsViewModel.updateComment(comment.id, newContent) { success, err ->
                            if (success) {
                                viewModel.setBanner("Comment updated")
                            } else {
                                viewModel.setBanner(err ?: "Failed to update comment")
                            }
                        }
                    },
                    onDeleteComment = { comment ->
                        commentsViewModel.deleteComment(comment.id) { success, err ->
                            if (success) {
                                viewModel.setBanner("Comment deleted")
                                viewModel.loadFeed()
                            } else {
                                viewModel.setBanner(err ?: "Failed to delete comment")
                            }
                        }
                    },
                    onToggleCommentLike = { comment ->
                        commentsViewModel.toggleCommentLike(comment)
                    },
                    onOpenCommentLikers = { comment ->
                        commentsViewModel.openCommentLikers(comment)
                    },
                    onReportComment = { comment ->
                        postActionsViewModel.submitReport(null, comment.id, "Inappropriate Comment", null) { _, msg ->
                            viewModel.setBanner(msg)
                        }
                    },
                    onAuthorClick = { author ->
                        commentsViewModel.closeComments()
                        selectedUserProfile = author
                        viewModel.viewUserProfile(author)
                    }
                )
            }

            // Reels Full Screen Overlay
            if (isReelsScreenOpen) {
                com.example.ui.screens.ReelsScreen(
                    initialPost = selectedReelPost,
                    currentUser = currentUser,
                    onBack = {
                        isReelsScreenOpen = false
                        selectedReelPost = null
                    },
                    onAuthorClick = { author ->
                        isReelsScreenOpen = false
                        selectedReelPost = null
                        selectedUserProfile = author
                        viewModel.viewUserProfile(author)
                    },
                    onCommentClick = { post ->
                        commentsViewModel.openComments(post)
                    },
                    onToggleFollow = { author ->
                        viewModel.toggleFollow(author)
                    },
                    isFollowing = { id -> viewModel.isUserFollowed(id) },
                    onCreateVideoClick = {
                        isReelsScreenOpen = false
                        isCreatePostOpen = true
                    }
                )
            }

            // Global Search Full-Screen Overlay
            val isSearchOverlayVisible by searchViewModel.isOverlayVisible.collectAsState()
            if (isSearchOverlayVisible) {
                GlobalSearchOverlay(
                    viewModel = searchViewModel,
                    currentUser = currentUser,
                    onPostClick = { post -> },
                    onUserClick = { user ->
                        searchViewModel.closeSearchOverlay()
                        selectedUserProfile = user
                        viewModel.viewUserProfile(user)
                    },
                    onTagClick = { tag ->
                        searchViewModel.openTagFeed(tag)
                    },
                    onLikeClick = { postId -> viewModel.toggleLike(Post(id = postId)) },
                    onCommentClick = { post -> commentsViewModel.openComments(post) },
                    onShareClick = { post -> postActionsViewModel.sharePost(context, post) },
                    onOptionClick = { post -> selectedPostForMenu = post },
                    onClose = { searchViewModel.closeSearchOverlay() }
                )
            }

            // Tag Feed Full-Screen Overlay
            val selectedTagForFeed by searchViewModel.selectedTagForFeed.collectAsState()
            if (selectedTagForFeed != null) {
                TagFeedScreen(
                    viewModel = searchViewModel,
                    currentUser = currentUser,
                    onPostClick = { post -> },
                    onUserClick = { user ->
                        searchViewModel.closeTagFeed()
                        searchViewModel.closeSearchOverlay()
                        selectedUserProfile = user
                        viewModel.viewUserProfile(user)
                    },
                    onLikeClick = { postId -> viewModel.toggleLike(Post(id = postId)) },
                    onCommentClick = { post -> commentsViewModel.openComments(post) },
                    onShareClick = { post -> postActionsViewModel.sharePost(context, post) },
                    onOptionClick = { post -> selectedPostForMenu = post },
                    onClose = { searchViewModel.closeTagFeed() }
                )
            }

            // Series Catalog Overlays
            if (isSeriesCatalogOpen) {
                when {
                    activePlayerSeriesId != null -> {
                        EpisodePlayerScreen(
                            seriesId = activePlayerSeriesId!!,
                            episodeId = activePlayerEpisodeId,
                            initialPositionSec = activePlayerStartPos,
                            onBackClick = {
                                activePlayerSeriesId = null
                                activePlayerEpisodeId = null
                                activePlayerStartPos = 0
                            },
                            onNavigateToSeries = { nextSlug ->
                                activePlayerSeriesId = null
                                activePlayerEpisodeId = null
                                activePlayerStartPos = 0
                                selectedSeriesSlug = nextSlug
                            },
                            onOpenAuthDialog = {
                                viewModel.setBanner("Please sign in to interact")
                            }
                        )
                    }

                    selectedSeriesSlug != null -> {
                        SeriesDetailsScreen(
                            slug = selectedSeriesSlug!!,
                            viewModel = seriesViewModel,
                            onBack = { selectedSeriesSlug = null },
                            onPlayEpisode = { sId, epId ->
                                activePlayerSeriesId = sId
                                activePlayerEpisodeId = epId
                                activePlayerStartPos = 0
                            },
                            onSeriesClick = { nextSlug ->
                                selectedSeriesSlug = nextSlug
                            },
                            onAuthRequired = {
                                viewModel.setBanner("Please log in to use this feature")
                            }
                        )
                    }

                    selectedShelfForList != null -> {
                        val categoriesState by seriesViewModel.categories.collectAsState()
                        val watchlistIdsState by seriesViewModel.watchlistIds.collectAsState()
                        SeriesListScreen(
                            shelfWithItems = selectedShelfForList,
                            categories = categoriesState,
                            watchlistIds = watchlistIdsState,
                            onSeriesClick = { slug -> selectedSeriesSlug = slug },
                            onWatchlistToggle = { card ->
                                seriesViewModel.toggleWatchlist(card.id) {
                                    viewModel.setBanner("Please log in to save to watchlist")
                                }
                            },
                            onBack = { selectedShelfForList = null }
                        )
                    }

                    else -> {
                        SeriesHomeScreen(
                            viewModel = seriesViewModel,
                            onMenuClick = { coroutineScope.launch { drawerState.open() } },
                            onSeriesClick = { slug -> selectedSeriesSlug = slug },
                            onEpisodePlay = { sId, epId ->
                                activePlayerSeriesId = sId
                                activePlayerEpisodeId = epId
                                activePlayerStartPos = 0
                            },
                            onSeeAllClick = { shelf -> selectedShelfForList = shelf },
                            onAuthRequired = { viewModel.setBanner("Please log in to use this feature") }
                        )
                        BackHandler {
                            isSeriesCatalogOpen = false
                        }
                    }
                }
            }

            // Creator Studio Full Screen Overlay
            if (isCreatorStudioOpen && currentUser != null) {
                CreatorStudioScreen(
                    viewModel = creatorStudioViewModel,
                    currentUser = currentUser!!,
                    onBack = { isCreatorStudioOpen = false }
                )
            }

            // Admin Studio Full Screen Overlay
            if (isAdminStudioOpen && currentUser != null) {
                AdminStudioScreen(
                    viewModel = adminStudioViewModel,
                    currentUser = currentUser!!,
                    onBack = { isAdminStudioOpen = false }
                )
            }
        }
    }
}
}
