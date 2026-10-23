package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Tag
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Post
import com.example.data.model.Tag
import com.example.data.model.UserProfile
import com.example.ui.components.CreatorRankChip
import com.example.ui.components.PostCard
import com.example.ui.components.UserAvatar
import com.example.ui.theme.*
import com.example.ui.viewmodel.SearchResultState
import com.example.ui.viewmodel.SearchTab
import com.example.ui.viewmodel.SearchViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlobalSearchOverlay(
    viewModel: SearchViewModel,
    currentUser: UserProfile?,
    onPostClick: (Post) -> Unit,
    onUserClick: (UserProfile) -> Unit,
    onTagClick: (Tag) -> Unit,
    onLikeClick: (String) -> Unit,
    onCommentClick: (Post) -> Unit,
    onShareClick: (Post) -> Unit,
    onOptionClick: (Post) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedTab by viewModel.selectedTab.collectAsState()
    val recentSearches by viewModel.recentSearches.collectAsState()
    val trendingTags by viewModel.trendingTags.collectAsState()

    val postsState by viewModel.postsState.collectAsState()
    val usersState by viewModel.usersState.collectAsState()
    val tagsState by viewModel.tagsState.collectAsState()

    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Surface(
        modifier = modifier
            .fillMaxSize()
            .testTag("global_search_overlay"),
        color = QBackground
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top Header Search Bar
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(QSurface)
                    .statusBarsPadding()
                    .border(width = 0.5.dp, color = QCardBorder)
                    .padding(horizontal = 8.dp, vertical = 8.dp)
            ) {
                IconButton(
                    onClick = {
                        viewModel.closeSearchOverlay()
                        onClose()
                    },
                    modifier = Modifier.testTag("search_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = QOnSurface
                    )
                }

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.onQueryChanged(it) },
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp)
                        .focusRequester(focusRequester)
                        .testTag("search_input_field"),
                    placeholder = {
                        Text(
                            "Search posts, #tags, or @people...",
                            fontSize = 14.sp,
                            color = QOnSurfaceVariant
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.Search,
                            contentDescription = null,
                            tint = QYellowDark,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.onQueryChanged("") }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Clear",
                                    tint = QOnSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = QBackground,
                        unfocusedContainerColor = QBackground,
                        focusedBorderColor = QYellowPrimary,
                        unfocusedBorderColor = QOutline,
                        focusedTextColor = QOnSurface,
                        unfocusedTextColor = QOnSurface
                    ),
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Search
                    ),
                    keyboardActions = KeyboardActions(
                        onSearch = {
                            viewModel.executeSearch()
                        }
                    )
                )
            }

            // Tabs Row (Shown when query is present)
            if (searchQuery.trim().length >= 2) {
                ScrollableTabRow(
                    selectedTabIndex = selectedTab.ordinal,
                    containerColor = QSurface,
                    contentColor = QOnSurface,
                    edgePadding = 16.dp,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedTab.ordinal]),
                            height = 3.dp,
                            color = QYellowPrimary
                        )
                    },
                    divider = { HorizontalDivider(color = QCardBorder, thickness = 0.5.dp) }
                ) {
                    SearchTab.values().forEach { tab ->
                        Tab(
                            selected = selectedTab == tab,
                            onClick = { viewModel.selectTab(tab) },
                            text = {
                                Text(
                                    text = when (tab) {
                                        SearchTab.ALL -> "All"
                                        SearchTab.POSTS -> "Posts"
                                        SearchTab.PEOPLE -> "People"
                                        SearchTab.TAGS -> "Tags"
                                    },
                                    fontWeight = if (selectedTab == tab) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 14.sp,
                                    color = if (selectedTab == tab) QYellowDark else QOnSurfaceVariant
                                )
                            },
                            modifier = Modifier.testTag("search_tab_${tab.name.lowercase()}")
                        )
                    }
                }
            }

            // Content Area
            val cleanQuery = searchQuery.trim()
            if (cleanQuery.length < 2) {
                // Empty state: Recent searches & Trending Tags
                EmptyQueryView(
                    recentSearches = recentSearches,
                    trendingTags = trendingTags,
                    onRecentClick = { query ->
                        viewModel.onQueryChanged(query)
                        viewModel.executeSearch()
                    },
                    onRemoveRecent = { viewModel.removeRecentSearch(it) },
                    onClearAllRecent = { viewModel.clearAllRecentSearches() },
                    onTagClick = { tag ->
                        onTagClick(tag)
                    }
                )
            } else {
                // Search Results based on selected Tab
                when (selectedTab) {
                    SearchTab.ALL -> AllResultsView(
                        query = cleanQuery,
                        postsState = postsState,
                        usersState = usersState,
                        tagsState = tagsState,
                        currentUser = currentUser,
                        onSeeAllPosts = { viewModel.selectTab(SearchTab.POSTS) },
                        onSeeAllPeople = { viewModel.selectTab(SearchTab.PEOPLE) },
                        onSeeAllTags = { viewModel.selectTab(SearchTab.TAGS) },
                        onPostClick = onPostClick,
                        onUserClick = onUserClick,
                        onTagClick = onTagClick,
                        onLikeClick = onLikeClick,
                        onCommentClick = onCommentClick,
                        onShareClick = onShareClick,
                        onOptionClick = onOptionClick
                    )

                    SearchTab.POSTS -> PostsResultsView(
                        query = cleanQuery,
                        postsState = postsState,
                        currentUser = currentUser,
                        onPostClick = onPostClick,
                        onUserClick = onUserClick,
                        onLikeClick = onLikeClick,
                        onCommentClick = onCommentClick,
                        onShareClick = onShareClick,
                        onOptionClick = onOptionClick
                    )

                    SearchTab.PEOPLE -> PeopleResultsView(
                        query = cleanQuery,
                        usersState = usersState,
                        onUserClick = onUserClick
                    )

                    SearchTab.TAGS -> TagsResultsView(
                        query = cleanQuery,
                        tagsState = tagsState,
                        onTagClick = onTagClick
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyQueryView(
    recentSearches: List<String>,
    trendingTags: List<Tag>,
    onRecentClick: (String) -> Unit,
    onRemoveRecent: (String) -> Unit,
    onClearAllRecent: () -> Unit,
    onTagClick: (Tag) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Recent Searches Section
        if (recentSearches.isNotEmpty()) {
            item {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Recent Searches",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = QOnSurface
                        )
                        Text(
                            text = "Clear All",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = QYellowDark,
                            modifier = Modifier
                                .clickable { onClearAllRecent() }
                                .padding(4.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    recentSearches.forEach { search ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onRecentClick(search) }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.History,
                                    contentDescription = null,
                                    tint = QOnSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = search,
                                    fontSize = 14.sp,
                                    color = QOnSurface
                                )
                            }
                            IconButton(
                                onClick = { onRemoveRecent(search) },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Remove",
                                    tint = QOnSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Trending Tags Section
        item {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.Tag,
                        contentDescription = null,
                        tint = QYellowDark,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Trending Tags",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = QOnSurface
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))

                if (trendingTags.isEmpty()) {
                    Text(
                        text = "No trending tags available right now",
                        fontSize = 13.sp,
                        color = QOnSurfaceVariant
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        trendingTags.forEach { tag ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(QSurface)
                                    .border(width = 0.5.dp, color = QCardBorder, shape = RoundedCornerShape(12.dp))
                                    .clickable { onTagClick(tag) }
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "#${tag.name}",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = QYellowDark
                                )
                                Text(
                                    text = "${tag.postsCount} posts",
                                    fontSize = 12.sp,
                                    color = QOnSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AllResultsView(
    query: String,
    postsState: SearchResultState<Post>,
    usersState: SearchResultState<UserProfile>,
    tagsState: SearchResultState<Tag>,
    currentUser: UserProfile?,
    onSeeAllPosts: () -> Unit,
    onSeeAllPeople: () -> Unit,
    onSeeAllTags: () -> Unit,
    onPostClick: (Post) -> Unit,
    onUserClick: (UserProfile) -> Unit,
    onTagClick: (Tag) -> Unit,
    onLikeClick: (String) -> Unit,
    onCommentClick: (Post) -> Unit,
    onShareClick: (Post) -> Unit,
    onOptionClick: (Post) -> Unit
) {
    val isLoading = postsState is SearchResultState.Loading && usersState is SearchResultState.Loading && tagsState is SearchResultState.Loading

    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = QYellowPrimary)
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // 1. People Preview
        if (usersState is SearchResultState.Success && usersState.data.isNotEmpty()) {
            item {
                SectionHeader("People", onSeeAllClick = onSeeAllPeople)
                Spacer(modifier = Modifier.height(8.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    usersState.data.take(3).forEach { user ->
                        UserResultRow(user = user, onClick = { onUserClick(user) })
                    }
                }
            }
        }

        // 2. Tags Preview
        if (tagsState is SearchResultState.Success && tagsState.data.isNotEmpty()) {
            item {
                SectionHeader("Tags", onSeeAllClick = onSeeAllTags)
                Spacer(modifier = Modifier.height(8.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    tagsState.data.take(3).forEach { tag ->
                        TagResultRow(tag = tag, onClick = { onTagClick(tag) })
                    }
                }
            }
        }

        // 3. Posts Preview
        if (postsState is SearchResultState.Success && postsState.data.isNotEmpty()) {
            item {
                SectionHeader("Posts", onSeeAllClick = onSeeAllPosts)
            }
            items(postsState.data.take(3), key = { it.id }) { post ->
                PostCard(
                    post = post,
                    currentUserId = currentUser?.id,
                    onLikeClick = { onLikeClick(post.id) },
                    onCommentClick = { onCommentClick(post) },
                    onAuthorClick = { author -> onUserClick(author) },
                    onBookmarkClick = { },
                    onOpenMenu = onOptionClick
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
        }

        // Empty Search Results Notice
        val isAllEmpty = (postsState is SearchResultState.Success && postsState.data.isEmpty()) &&
                (usersState is SearchResultState.Success && usersState.data.isEmpty()) &&
                (tagsState is SearchResultState.Success && tagsState.data.isEmpty())

        if (isAllEmpty) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Outlined.Search,
                            contentDescription = null,
                            tint = QOnSurfaceVariant,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No results found for \"$query\"",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            color = QOnSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PostsResultsView(
    query: String,
    postsState: SearchResultState<Post>,
    currentUser: UserProfile?,
    onPostClick: (Post) -> Unit,
    onUserClick: (UserProfile) -> Unit,
    onLikeClick: (String) -> Unit,
    onCommentClick: (Post) -> Unit,
    onShareClick: (Post) -> Unit,
    onOptionClick: (Post) -> Unit
) {
    when (postsState) {
        is SearchResultState.Loading -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = QYellowPrimary)
            }
        }

        is SearchResultState.Error -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(text = postsState.message, color = Color.Red, fontSize = 14.sp)
            }
        }

        is SearchResultState.Success -> {
            if (postsState.data.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No posts matched \"$query\"", color = QOnSurfaceVariant, fontSize = 14.sp)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(postsState.data, key = { it.id }) { post ->
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

        SearchResultState.Idle -> {}
    }
}

@Composable
private fun PeopleResultsView(
    query: String,
    usersState: SearchResultState<UserProfile>,
    onUserClick: (UserProfile) -> Unit
) {
    when (usersState) {
        is SearchResultState.Loading -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = QYellowPrimary)
            }
        }

        is SearchResultState.Error -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(text = usersState.message, color = Color.Red, fontSize = 14.sp)
            }
        }

        is SearchResultState.Success -> {
            if (usersState.data.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No people matched \"$query\"", color = QOnSurfaceVariant, fontSize = 14.sp)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(usersState.data, key = { it.id }) { user ->
                        UserResultRow(user = user, onClick = { onUserClick(user) })
                    }
                }
            }
        }

        SearchResultState.Idle -> {}
    }
}

@Composable
private fun TagsResultsView(
    query: String,
    tagsState: SearchResultState<Tag>,
    onTagClick: (Tag) -> Unit
) {
    when (tagsState) {
        is SearchResultState.Loading -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = QYellowPrimary)
            }
        }

        is SearchResultState.Error -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(text = tagsState.message, color = Color.Red, fontSize = 14.sp)
            }
        }

        is SearchResultState.Success -> {
            if (tagsState.data.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No tags matched \"$query\"", color = QOnSurfaceVariant, fontSize = 14.sp)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(tagsState.data, key = { it.id }) { tag ->
                        TagResultRow(tag = tag, onClick = { onTagClick(tag) })
                    }
                }
            }
        }

        SearchResultState.Idle -> {}
    }
}

@Composable
private fun SectionHeader(title: String, onSeeAllClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = QOnSurface
        )
        Text(
            text = "See All",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = QYellowDark,
            modifier = Modifier
                .clickable { onSeeAllClick() }
                .padding(4.dp)
        )
    }
}

@Composable
private fun UserResultRow(user: UserProfile, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(QSurface)
            .border(width = 0.5.dp, color = QCardBorder, shape = RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        UserAvatar(
            avatarUrl = user.avatarUrl,
            initials = user.initials,
            size = 46.dp
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = user.effectiveDisplayName,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = QOnSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.width(6.dp))
                CreatorRankChip(rank = user.creatorRank)
            }
            Text(
                text = "@${user.effectiveUsername}",
                fontSize = 12.sp,
                color = QOnSurfaceVariant,
                maxLines = 1
            )
            if (!user.bio.isNullOrBlank()) {
                Text(
                    text = user.bio,
                    fontSize = 12.sp,
                    color = QOnSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun TagResultRow(tag: Tag, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(QSurface)
            .border(width = 0.5.dp, color = QCardBorder, shape = RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(QYellowPrimary.copy(alpha = 0.2f))
            ) {
                Text("#", color = QYellowDark, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "#${tag.name}",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = QOnSurface
            )
        }
        Text(
            text = "${tag.postsCount} posts",
            fontSize = 12.sp,
            color = QOnSurfaceVariant
        )
    }
}

fun buildHighlightedAnnotatedString(text: String, query: String): AnnotatedString {
    if (query.isBlank()) return AnnotatedString(text)
    return buildAnnotatedString {
        val lowerText = text.lowercase()
        val lowerQuery = query.lowercase().trim()
        var startIndex = 0

        while (startIndex < text.length) {
            val index = lowerText.indexOf(lowerQuery, startIndex)
            if (index == -1) {
                append(text.substring(startIndex))
                break
            } else {
                append(text.substring(startIndex, index))
                withStyle(style = SpanStyle(fontWeight = FontWeight.Bold, color = QYellowDark, background = QYellowPrimary.copy(alpha = 0.2f))) {
                    append(text.substring(index, index + lowerQuery.length))
                }
                startIndex = index + lowerQuery.length
            }
        }
    }
}
