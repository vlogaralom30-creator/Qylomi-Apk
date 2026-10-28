package com.example

import android.app.Activity
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.model.Post
import com.example.data.model.UserProfile
import com.example.ui.components.BrandedSplash
import com.example.ui.components.DrawerDestination
import com.example.ui.components.EditPostBottomSheet
import com.example.ui.components.LikedByBottomSheet
import com.example.ui.components.PostActionMenuBottomSheet
import com.example.ui.components.QBottomNavBar
import com.example.ui.components.QNavigationDrawerSheet
import com.example.ui.components.QTopAppBar
import com.example.ui.components.RelayBottomSheet
import com.example.ui.components.SocialTab
import com.example.ui.navigation.AppDestination
import com.example.ui.navigation.AppTransitions
import com.example.ui.navigation.NavScreenKey
import com.example.ui.navigation.rememberAppNavigator
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.CommentsBottomSheet
import com.example.ui.screens.CreatePostBottomSheet
import com.example.ui.screens.EditProfileScreen
import com.example.ui.screens.ExploreScreen
import com.example.ui.screens.FeedScreen
import com.example.ui.screens.GlobalSearchOverlay
import com.example.ui.screens.GroupDetailsScreen
import com.example.ui.screens.GroupsScreen
import com.example.ui.screens.NotificationsScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.ReelsScreen
import com.example.ui.screens.TagFeedScreen
import com.example.ui.screens.series.EpisodePlayerScreen
import com.example.ui.screens.series.SeriesDetailsScreen
import com.example.ui.screens.series.SeriesHomeScreen
import com.example.ui.screens.series.SeriesListScreen
import com.example.ui.screens.series.SeriesPlayerScreen
import com.example.ui.screens.studio.AdminStudioScreen
import com.example.ui.screens.studio.CreatorStudioScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.QBackground
import com.example.ui.theme.QOnYellow
import com.example.ui.theme.QYellowPrimary
import com.example.ui.viewmodel.AdminStudioViewModel
import com.example.ui.viewmodel.CommentsViewModel
import com.example.ui.viewmodel.CreatorStudioViewModel
import com.example.ui.viewmodel.PlayerViewModel
import com.example.ui.viewmodel.PostActionsViewModel
import com.example.ui.viewmodel.SearchViewModel
import com.example.ui.viewmodel.SeriesViewModel
import com.example.ui.viewmodel.RelayViewModel
import com.example.ui.viewmodel.SocialViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            enableEdgeToEdge()
        } catch (_: Throwable) {}

        setContent {
            var showSplash by rememberSaveable { mutableStateOf(true) }
            val app = (LocalContext.current.applicationContext as? android.app.Application)
                ?: application
            val appFactory = remember(app) { ViewModelProvider.AndroidViewModelFactory.getInstance(app) }
            val socialViewModel: SocialViewModel = viewModel(factory = appFactory)
            val isReady by socialViewModel.isReady.collectAsState()

            MyApplicationTheme {
                Box(modifier = Modifier.fillMaxSize()) {
                    QSocialApp(
                        app = app,
                        appFactory = appFactory,
                        viewModel = socialViewModel
                    )

                    if (showSplash) {
                        BrandedSplash(
                            isReady = isReady,
                            onSplashFinished = { showSplash = false }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun QSocialApp(
    app: android.app.Application = (LocalContext.current.applicationContext as? android.app.Application)
        ?: error("Application not found"),
    appFactory: ViewModelProvider.Factory = remember(app) { ViewModelProvider.AndroidViewModelFactory.getInstance(app) },
    viewModel: SocialViewModel = viewModel(factory = appFactory),
    postActionsViewModel: PostActionsViewModel = viewModel(factory = appFactory),
    commentsViewModel: CommentsViewModel = viewModel(factory = appFactory),
    searchViewModel: SearchViewModel = viewModel(factory = appFactory),
    creatorStudioViewModel: CreatorStudioViewModel = viewModel(factory = appFactory),
    adminStudioViewModel: AdminStudioViewModel = viewModel(factory = appFactory),
    seriesViewModel: SeriesViewModel = viewModel(factory = appFactory),
    relayViewModel: RelayViewModel = viewModel(factory = appFactory)
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val isLoggedIn by viewModel.isLoggedIn.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val feedPosts by viewModel.feedPosts.collectAsState()
    val isFeedLoading by viewModel.isFeedLoading.collectAsState()
    val isFeedRefreshing by viewModel.isFeedRefreshing.collectAsState()
    val feedError by viewModel.feedError.collectAsState()
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

    // Drawer and Unified Navigator
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()
    val navigator = rememberAppNavigator()
    val saveableStateHolder = rememberSaveableStateHolder()

    var selectedPostForMenu by remember { mutableStateOf<Post?>(null) }
    var selectedPostForRelay by remember { mutableStateOf<Post?>(null) }
    var editingPost by remember { mutableStateOf<Post?>(null) }
    var lastBackPressTime by remember { mutableLongStateOf(0L) }

    // Auto-dismiss banner after 3 seconds
    LaunchedEffect(bannerMessage) {
        if (bannerMessage != null) {
            delay(3000)
            viewModel.clearBanner()
        }
    }

    // Single Ordered BackHandler (Priority 1..8)
    BackHandler(enabled = true) {
        when {
            // 1. Comments sheet open -> close
            selectedPostForComments != null -> commentsViewModel.closeComments()
            // 2. Likers sheet open -> close
            selectedCommentForLikers != null -> commentsViewModel.closeCommentLikers()
            selectedPostForLikers != null -> postActionsViewModel.closeLikers()
            // 3. Relay sheet open -> close
            selectedPostForRelay != null -> selectedPostForRelay = null
            // 4. Post action menu open -> close
            selectedPostForMenu != null -> selectedPostForMenu = null
            // 5. Edit post open -> close
            editingPost != null -> editingPost = null
            // 5. Drawer open -> close drawer
            drawerState.isOpen -> coroutineScope.launch { drawerState.close() }
            // 6. Navigation stack has screens -> pop top screen
            navigator.canPop -> navigator.pop()
            // 7. Root tab is not FEED -> return to FEED
            navigator.rootTab.value != SocialTab.FEED -> navigator.switchTab(SocialTab.FEED)
            // 8. At FEED root -> Double-tap within 2s to exit
            else -> {
                val now = System.currentTimeMillis()
                if (now - lastBackPressTime < 2000L) {
                    activity?.finish()
                } else {
                    lastBackPressTime = now
                    Toast.makeText(context, "Press back again to exit", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    if (!isLoggedIn) {
        AuthScreen(
            onLogin = { email, pass, onDone -> viewModel.login(email, pass, onDone) },
            onSignUp = { email, pass, username, displayName, onDone ->
                viewModel.signUp(email, pass, username, displayName, onDone)
            },
            savedAccounts = savedAccounts,
            onSelectSavedAccount = { account ->
                val matchingProfile = allProfiles.find {
                    it.username.equals(account.username, ignoreCase = true) ||
                    it.displayName.equals(account.displayName, ignoreCase = true)
                } ?: UserProfile(
                    id = "user_${account.username}",
                    username = account.username,
                    displayName = account.displayName,
                    avatarUrl = account.avatarUrl
                )
                viewModel.loginAsDemoUser(matchingProfile)
            },
            onRemoveSavedAccount = { account -> viewModel.removeSavedAccount(account) },
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
        val currentTop = navigator.top
        val isRootScreen = currentTop == null

        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                val drawerSelectedDest = when (currentTop) {
                    is AppDestination.CreatorStudio -> DrawerDestination.CREATOR_STUDIO
                    is AppDestination.AdminStudio -> DrawerDestination.ADMIN_STUDIO
                    is AppDestination.SeriesHome, is AppDestination.SeriesDetails, is AppDestination.SeriesList, is AppDestination.EpisodePlayer -> DrawerDestination.SERIES
                    is AppDestination.Groups, is AppDestination.GroupDetails -> DrawerDestination.GROUPS
                    is AppDestination.Reels -> DrawerDestination.REELS
                    is AppDestination.Settings -> DrawerDestination.SETTINGS
                    else -> when (navigator.rootTab.value) {
                        SocialTab.FEED -> DrawerDestination.FEED
                        SocialTab.EXPLORE -> DrawerDestination.EXPLORE
                        SocialTab.NOTIFICATIONS -> DrawerDestination.NOTIFICATIONS
                        SocialTab.PROFILE -> DrawerDestination.SAVED
                        else -> DrawerDestination.FEED
                    }
                }

                QNavigationDrawerSheet(
                    currentUser = currentUser,
                    unreadNotifications = 2,
                    selectedDestination = drawerSelectedDest,
                    onDestinationSelect = { dest ->
                        coroutineScope.launch { drawerState.close() }
                        when (dest) {
                            DrawerDestination.FEED -> navigator.switchTab(SocialTab.FEED)
                            DrawerDestination.EXPLORE -> navigator.switchTab(SocialTab.EXPLORE)
                            DrawerDestination.NOTIFICATIONS -> navigator.switchTab(SocialTab.NOTIFICATIONS)
                            DrawerDestination.SAVED -> navigator.switchTab(SocialTab.PROFILE)
                            DrawerDestination.SERIES -> navigator.replaceAll(AppDestination.SeriesHome)
                            DrawerDestination.GROUPS -> {
                                viewModel.loadGroups()
                                navigator.replaceAll(AppDestination.Groups)
                            }
                            DrawerDestination.REELS -> navigator.replaceAll(AppDestination.Reels())
                            DrawerDestination.CREATOR_STUDIO -> {
                                if (currentUser?.canCreateSeries == true) {
                                    navigator.replaceAll(AppDestination.CreatorStudio)
                                }
                            }
                            DrawerDestination.ADMIN_STUDIO -> {
                                if (currentUser?.isAdmin == true) {
                                    navigator.replaceAll(AppDestination.AdminStudio)
                                }
                            }
                            DrawerDestination.SETTINGS -> navigator.push(AppDestination.Settings)
                        }
                    },
                    onLogoutClick = {
                        coroutineScope.launch { drawerState.close() }
                        viewModel.logout()
                    },
                    onProfileClick = {
                        coroutineScope.launch { drawerState.close() }
                        navigator.switchTab(SocialTab.PROFILE)
                    }
                )
            }
        ) {
            Scaffold(
                topBar = {
                    if (isRootScreen) {
                        QTopAppBar(
                            currentUser = currentUser,
                            unreadNotifications = 2,
                            onNotificationsClick = { navigator.switchTab(SocialTab.NOTIFICATIONS) },
                            onMenuClick = { coroutineScope.launch { drawerState.open() } },
                            onSeriesClick = { navigator.push(AppDestination.SeriesHome) },
                            onReelsClick = { navigator.push(AppDestination.Reels()) },
                            onSearchClick = { navigator.push(AppDestination.Search()) },
                            onProfileClick = { navigator.switchTab(SocialTab.PROFILE) }
                        )
                    }
                },
                bottomBar = {
                    if (isRootScreen) {
                        QBottomNavBar(
                            selectedTab = navigator.rootTab.value,
                            onTabSelected = { tab ->
                                if (tab == SocialTab.CREATE) {
                                    navigator.push(AppDestination.CreatePost())
                                } else {
                                    navigator.switchTab(tab)
                                }
                            }
                        )
                    }
                },
                containerColor = MaterialTheme.colorScheme.background
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    val currentScreenKey = remember(navigator.top, navigator.rootTab.value) {
                        val topDest = navigator.top
                        if (topDest != null) {
                            NavScreenKey.DestKey(topDest)
                        } else {
                            NavScreenKey.TabKey(navigator.rootTab.value)
                        }
                    }
                    val isAnimationsDisabled = AppTransitions.areAnimationsDisabled()

                    AnimatedContent(
                        targetState = currentScreenKey,
                        transitionSpec = {
                            AppTransitions.getTransition(initialState, targetState, isAnimationsDisabled)
                        },
                        label = "NavTransition"
                    ) { targetKey ->
                        saveableStateHolder.SaveableStateProvider(targetKey.saveableKey) {
                            when (targetKey) {
                                is NavScreenKey.TabKey -> {
                                    when (targetKey.tab) {
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
                                                    viewModel.viewUserProfile(author)
                                                    navigator.push(AppDestination.UserProfile(author.id))
                                                },
                                                onRefresh = { viewModel.refreshFeed() },
                                                onCreatePostClick = { navigator.push(AppDestination.CreatePost()) },
                                                onEditPostClick = { post -> editingPost = post },
                                                onDeletePostClick = { post -> viewModel.deletePost(post.id) { _, _ -> } },
                                                onGroupsClick = {
                                                    viewModel.loadGroups()
                                                    navigator.push(AppDestination.Groups)
                                                },
                                                onOpenReels = { post -> navigator.push(AppDestination.Reels(post?.id)) },
                                                onOpenMenu = { post -> selectedPostForMenu = post },
                                                onLikersClick = { post -> postActionsViewModel.openLikers(post) },
                                                onRelayClick = { post -> selectedPostForRelay = post },
                                                feedError = feedError,
                                                onRetryFeed = { viewModel.loadFeed() }
                                            )
                                        }

                                        SocialTab.EXPLORE -> {
                                            ExploreScreen(
                                                profiles = allProfiles,
                                                searchQuery = searchQuery,
                                                onSearchChange = { q -> viewModel.setSearchQuery(q) },
                                                onProfileClick = { profile ->
                                                    viewModel.viewUserProfile(profile)
                                                    navigator.push(AppDestination.UserProfile(profile.id))
                                                },
                                                onToggleFollow = { profile -> viewModel.toggleFollow(profile) },
                                                isFollowing = { id -> viewModel.isUserFollowed(id) },
                                                onOpenSearch = { query -> navigator.push(AppDestination.Search(query)) }
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
                                                    onEditProfileClick = { navigator.push(AppDestination.EditProfile) },
                                                    onBackClick = { navigator.switchTab(SocialTab.FEED) },
                                                    onLogoutClick = { viewModel.logout() },
                                                    onLikePost = { post -> viewModel.toggleLike(post) },
                                                    onCommentPost = { post -> commentsViewModel.openComments(post) },
                                                    onBookmarkPost = { post -> viewModel.toggleBookmark(post) },
                                                    onToggleFollow = { target -> viewModel.toggleFollow(target) },
                                                    isFollowing = false,
                                                    onOpenMenu = { post -> selectedPostForMenu = post },
                                                    onLikersClick = { post -> postActionsViewModel.openLikers(post) },
                                                    onOpenCreatorStudio = if (user.canCreateSeries) { { navigator.push(AppDestination.CreatorStudio) } } else null,
                                                    onOpenAdminStudio = if (user.isAdmin) { { navigator.push(AppDestination.AdminStudio) } } else null
                                                )
                                            }
                                        }

                                        SocialTab.CREATE -> {}
                                    }
                                }

                                is NavScreenKey.DestKey -> {
                                    when (val dest = targetKey.dest) {
                                        is AppDestination.UserProfile -> {
                                            val profile = allProfiles.find { it.id == dest.userId }
                                                ?: (if (currentUser?.id == dest.userId) currentUser else null)
                                                ?: UserProfile(id = dest.userId, username = "creator", displayName = "Creator")
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
                                                onEditProfileClick = { navigator.push(AppDestination.EditProfile) },
                                                onBackClick = { navigator.pop() },
                                                onLogoutClick = { viewModel.logout() },
                                                onLikePost = { post -> viewModel.toggleLike(post) },
                                                onCommentPost = { post -> commentsViewModel.openComments(post) },
                                                onBookmarkPost = { post -> viewModel.toggleBookmark(post) },
                                                onToggleFollow = { target -> viewModel.toggleFollow(target) },
                                                isFollowing = viewModel.isUserFollowed(profile.id),
                                                onOpenMenu = { post -> selectedPostForMenu = post },
                                                onLikersClick = { post -> postActionsViewModel.openLikers(post) },
                                                onOpenCreatorStudio = if (profile.id == currentUser?.id && currentUser?.canCreateSeries == true) { { navigator.push(AppDestination.CreatorStudio) } } else null,
                                                onOpenAdminStudio = if (profile.id == currentUser?.id && currentUser?.isAdmin == true) { { navigator.push(AppDestination.AdminStudio) } } else null
                                            )
                                        }

                                        is AppDestination.EditProfile -> {
                                            currentUser?.let { user ->
                                                EditProfileScreen(
                                                    currentProfile = user,
                                                    onBack = { navigator.pop() },
                                                    onSave = { id, updates, onDone ->
                                                        viewModel.updateProfile(id, updates, onDone)
                                                    }
                                                )
                                            }
                                        }

                                        is AppDestination.Groups -> {
                                            GroupsScreen(
                                                groups = groups,
                                                isLoading = isGroupsLoading,
                                                onGroupClick = { g -> navigator.push(AppDestination.GroupDetails(g.id)) },
                                                onToggleJoin = { g -> viewModel.toggleJoinGroup(g) },
                                                onCreateGroupClick = {
                                                    viewModel.createCommunityGroup("New Community", "A community for creators", "General") { _, _ -> }
                                                },
                                                onBack = { navigator.pop() }
                                            )
                                        }

                                        is AppDestination.GroupDetails -> {
                                            val group = groups.find { it.id == dest.groupId }
                                            if (group != null) {
                                                GroupDetailsScreen(
                                                    group = group,
                                                    posts = groupPosts,
                                                    members = groupMembers,
                                                    allProfiles = allProfiles,
                                                    currentUser = currentUser,
                                                    onBack = { navigator.pop() },
                                                    onToggleJoin = { g -> viewModel.toggleJoinGroup(g) },
                                                    onOpenCreatePostForGroup = { g -> navigator.push(AppDestination.CreatePost(g.id)) },
                                                    onLikePost = { post -> viewModel.toggleLike(post) },
                                                    onCommentPost = { post -> commentsViewModel.openComments(post) },
                                                    onBookmarkPost = { post -> viewModel.toggleBookmark(post) },
                                                    onAuthorClick = { author ->
                                                        viewModel.viewUserProfile(author)
                                                        navigator.push(AppDestination.UserProfile(author.id))
                                                    },
                                                    onEditPost = { post -> editingPost = post },
                                                    onDeletePost = { post -> viewModel.deletePost(post.id) { _, _ -> } }
                                                )
                                            } else {
                                                navigator.pop()
                                            }
                                        }

                                        is AppDestination.Reels -> {
                                            val initialPost = dest.initialPostId?.let { id -> feedPosts.find { it.id == id } }
                                            ReelsScreen(
                                                initialPost = initialPost,
                                                currentUser = currentUser,
                                                onBack = { navigator.pop() },
                                                onAuthorClick = { author ->
                                                    viewModel.viewUserProfile(author)
                                                    navigator.push(AppDestination.UserProfile(author.id))
                                                },
                                                onCommentClick = { post -> commentsViewModel.openComments(post) },
                                                onToggleFollow = { author -> viewModel.toggleFollow(author) },
                                                isFollowing = { id -> viewModel.isUserFollowed(id) },
                                                onCreateVideoClick = { navigator.push(AppDestination.CreatePost()) }
                                            )
                                        }

                                        is AppDestination.SeriesHome -> {
                                            SeriesHomeScreen(
                                                viewModel = seriesViewModel,
                                                onMenuClick = { coroutineScope.launch { drawerState.open() } },
                                                onSeriesClick = { slug -> navigator.push(AppDestination.SeriesDetails(slug)) },
                                                onEpisodePlay = { sId, epId -> navigator.push(AppDestination.EpisodePlayer(sId, epId)) },
                                                onSeeAllClick = { shelf -> navigator.push(AppDestination.SeriesList(shelf.shelf.slug)) },
                                                onAuthRequired = { viewModel.setBanner("Please log in to use this feature") }
                                            )
                                        }

                                        is AppDestination.SeriesList -> {
                                            val categoriesState by seriesViewModel.categories.collectAsState()
                                            val watchlistIdsState by seriesViewModel.watchlistIds.collectAsState()
                                            val shelvesState by seriesViewModel.shelves.collectAsState()
                                            val shelf = shelvesState.find { it.shelf.slug == dest.shelfSlug }
                                            SeriesListScreen(
                                                shelfWithItems = shelf,
                                                categories = categoriesState,
                                                watchlistIds = watchlistIdsState,
                                                onSeriesClick = { slug -> navigator.push(AppDestination.SeriesDetails(slug)) },
                                                onWatchlistToggle = { card ->
                                                    seriesViewModel.toggleWatchlist(card.id) {
                                                        viewModel.setBanner("Please log in to save to watchlist")
                                                    }
                                                },
                                                onBack = { navigator.pop() }
                                            )
                                        }

                                        is AppDestination.SeriesDetails -> {
                                            SeriesDetailsScreen(
                                                slug = dest.slug,
                                                viewModel = seriesViewModel,
                                                onBack = { navigator.pop() },
                                                onPlayEpisode = { sId, epId -> navigator.push(AppDestination.EpisodePlayer(sId, epId)) },
                                                onSeriesClick = { nextSlug -> navigator.push(AppDestination.SeriesDetails(nextSlug)) },
                                                onAuthRequired = { viewModel.setBanner("Please log in to use this feature") }
                                            )
                                        }

                                        is AppDestination.EpisodePlayer -> {
                                            SeriesPlayerScreen(
                                                seriesId = dest.seriesId,
                                                episodeId = dest.episodeId,
                                                initialPositionSec = dest.startPositionSec,
                                                onBackClick = { navigator.pop() },
                                                onNavigateToSeries = { nextSlug -> navigator.push(AppDestination.SeriesDetails(nextSlug)) },
                                                onOpenAuthDialog = { viewModel.setBanner("Please sign in to interact") }
                                            )
                                        }

                                        is AppDestination.CreatorStudio -> {
                                            if (currentUser != null) {
                                                CreatorStudioScreen(
                                                    viewModel = creatorStudioViewModel,
                                                    currentUser = currentUser!!,
                                                    onBack = { navigator.pop() }
                                                )
                                            } else {
                                                navigator.pop()
                                            }
                                        }

                                        is AppDestination.AdminStudio -> {
                                            if (currentUser != null) {
                                                AdminStudioScreen(
                                                    viewModel = adminStudioViewModel,
                                                    currentUser = currentUser!!,
                                                    onBack = { navigator.pop() }
                                                )
                                            } else {
                                                navigator.pop()
                                            }
                                        }

                                        is AppDestination.CreatePost -> {
                                            // Handled via modal sheet below
                                        }

                                        is AppDestination.TagFeed -> {
                                            TagFeedScreen(
                                                viewModel = searchViewModel,
                                                currentUser = currentUser,
                                                onPostClick = {},
                                                onUserClick = { user ->
                                                    viewModel.viewUserProfile(user)
                                                    navigator.push(AppDestination.UserProfile(user.id))
                                                },
                                                onLikeClick = { postId -> viewModel.toggleLike(Post(id = postId)) },
                                                onCommentClick = { post -> commentsViewModel.openComments(post) },
                                                onShareClick = { post -> postActionsViewModel.sharePost(context, post) },
                                                onOptionClick = { post -> selectedPostForMenu = post },
                                                onClose = { navigator.pop() }
                                            )
                                        }

                                        is AppDestination.Search -> {
                                            GlobalSearchOverlay(
                                                viewModel = searchViewModel,
                                                currentUser = currentUser,
                                                onPostClick = {},
                                                onUserClick = { user ->
                                                    viewModel.viewUserProfile(user)
                                                    navigator.push(AppDestination.UserProfile(user.id))
                                                },
                                                onTagClick = { tag -> navigator.push(AppDestination.TagFeed(tag.name)) },
                                                onLikeClick = { postId -> viewModel.toggleLike(Post(id = postId)) },
                                                onCommentClick = { post -> commentsViewModel.openComments(post) },
                                                onShareClick = { post -> postActionsViewModel.sharePost(context, post) },
                                                onOptionClick = { post -> selectedPostForMenu = post },
                                                onClose = { navigator.pop() }
                                            )
                                        }

                                        is AppDestination.Settings -> {
                                            SettingsScreen(
                                                currentUser = currentUser,
                                                onBack = { navigator.pop() },
                                                onLogout = { viewModel.logout() }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Global Floating Banner
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

                // Modal: Create Post Bottom Sheet
                val createPostDest = navigator.top as? AppDestination.CreatePost
                if (createPostDest != null) {
                    val targetGroup = createPostDest.targetGroupId?.let { gid -> groups.find { it.id == gid } }
                    CreatePostBottomSheet(
                        currentUser = currentUser,
                        onDismiss = { navigator.pop() },
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

                // Modal: Post Action Menu Sheet
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
                        onToggleBookmark = { targetPost -> viewModel.toggleBookmark(targetPost) },
                        onToggleFollow = { authorProfile -> viewModel.toggleFollow(authorProfile) },
                        onViewProfile = { authorProfile ->
                            viewModel.viewUserProfile(authorProfile)
                            navigator.push(AppDestination.UserProfile(authorProfile.id))
                        },
                        onHidePost = { targetPost ->
                            postActionsViewModel.hidePost(targetPost) {
                                viewModel.setBanner("Post hidden. Refreshing feed...")
                                viewModel.loadFeed()
                            }
                        },
                        onReportPost = { targetPost, reason, details ->
                            postActionsViewModel.submitReport(targetPost.id, null, reason, details) { _, msg ->
                                viewModel.setBanner(msg)
                            }
                        },
                        onShowBanner = { msg -> viewModel.setBanner(msg) }
                    )
                }

                // Modal: Edit Post Sheet
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

                // Modal: Post Likers Sheet
                selectedPostForLikers?.let { post ->
                    val users = postLikers.mapNotNull { it.userProfile }
                    LikedByBottomSheet(
                        title = "Liked by (${users.size})",
                        users = users,
                        isLoading = isLikersLoading,
                        currentUserId = currentUser?.id,
                        onDismiss = { postActionsViewModel.closeLikers() },
                        onUserClick = { profile ->
                            viewModel.viewUserProfile(profile)
                            navigator.push(AppDestination.UserProfile(profile.id))
                        },
                        onToggleFollow = { target -> viewModel.toggleFollow(target) },
                        isFollowing = { id -> viewModel.isUserFollowed(id) }
                    )
                }

                // Modal: Comment Likers Sheet
                selectedCommentForLikers?.let { comment ->
                    val users = commentLikers.mapNotNull { it.userProfile }
                    LikedByBottomSheet(
                        title = "Comment Liked by (${users.size})",
                        users = users,
                        isLoading = isCommentLikersLoading,
                        currentUserId = currentUser?.id,
                        onDismiss = { commentsViewModel.closeCommentLikers() },
                        onUserClick = { profile ->
                            viewModel.viewUserProfile(profile)
                            navigator.push(AppDestination.UserProfile(profile.id))
                        },
                        onToggleFollow = { target -> viewModel.toggleFollow(target) },
                        isFollowing = { id -> viewModel.isUserFollowed(id) }
                    )
                }

                // Modal: Comments Bottom Sheet
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
                        onToggleCommentLike = { comment -> commentsViewModel.toggleCommentLike(comment) },
                        onOpenCommentLikers = { comment -> commentsViewModel.openCommentLikers(comment) },
                        onReportComment = { comment ->
                            postActionsViewModel.submitReport(null, comment.id, "Inappropriate Comment", null) { _, msg ->
                                viewModel.setBanner(msg)
                            }
                        },
                        onAuthorClick = { author ->
                            commentsViewModel.closeComments()
                            viewModel.viewUserProfile(author)
                            navigator.push(AppDestination.UserProfile(author.id))
                        }
                    )
                }

                // Modal: Relay Bottom Sheet
                selectedPostForRelay?.let { post ->
                    RelayBottomSheet(
                        post = post,
                        onDismiss = { selectedPostForRelay = null },
                        onRelay = { quote ->
                            relayViewModel.toggleRelay(post.id, quote) { success ->
                                selectedPostForRelay = null
                                if (success) {
                                    viewModel.setBanner("Post relayed successfully!")
                                    viewModel.loadFeed()
                                } else {
                                    viewModel.setBanner("Failed to relay post")
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}
