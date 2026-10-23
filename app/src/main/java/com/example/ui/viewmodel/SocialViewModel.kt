package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.Comment
import com.example.data.model.NotificationItem
import com.example.data.model.Post
import com.example.data.model.ProfileUpdateRequest
import com.example.data.model.UserProfile
import com.example.data.repository.SocialRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.example.data.model.SavedAccount
import kotlinx.coroutines.launch

class SocialViewModel(application: Application) : AndroidViewModel(application) {

    val repository = SocialRepository(application)

    val currentUser: StateFlow<UserProfile?> = repository.currentUser
    val isLoggedIn: StateFlow<Boolean> = repository.isLoggedIn
    val savedAccounts: StateFlow<List<SavedAccount>> = repository.savedAccounts

    fun removeSavedAccount(account: SavedAccount) {
        repository.removeSavedAccount(account.email)
    }

    private val _feedPosts = MutableStateFlow<List<Post>>(emptyList())
    val feedPosts: StateFlow<List<Post>> = _feedPosts.asStateFlow()

    private val _savedPosts = MutableStateFlow<List<Post>>(emptyList())
    val savedPosts: StateFlow<List<Post>> = _savedPosts.asStateFlow()

    private val _isFeedLoading = MutableStateFlow(false)
    val isFeedLoading: StateFlow<Boolean> = _isFeedLoading.asStateFlow()

    private val _selectedCategory = MutableStateFlow("All")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    private val _allProfiles = MutableStateFlow<List<UserProfile>>(emptyList())
    val allProfiles: StateFlow<List<UserProfile>> = _allProfiles.asStateFlow()

    private val _viewedProfile = MutableStateFlow<UserProfile?>(null)
    val viewedProfile: StateFlow<UserProfile?> = _viewedProfile.asStateFlow()

    private val _viewedUserPosts = MutableStateFlow<List<Post>>(emptyList())
    val viewedUserPosts: StateFlow<List<Post>> = _viewedUserPosts.asStateFlow()

    private val _trackersCount = MutableStateFlow(0)
    val trackersCount: StateFlow<Int> = _trackersCount.asStateFlow()

    private val _trackingCount = MutableStateFlow(0)
    val trackingCount: StateFlow<Int> = _trackingCount.asStateFlow()

    private val _isProfileLoading = MutableStateFlow(false)
    val isProfileLoading: StateFlow<Boolean> = _isProfileLoading.asStateFlow()

    private val _selectedPostForComments = MutableStateFlow<Post?>(null)
    val selectedPostForComments: StateFlow<Post?> = _selectedPostForComments.asStateFlow()

    private val _comments = MutableStateFlow<List<Comment>>(emptyList())
    val comments: StateFlow<List<Comment>> = _comments.asStateFlow()

    private val _isCommentsLoading = MutableStateFlow(false)
    val isCommentsLoading: StateFlow<Boolean> = _isCommentsLoading.asStateFlow()

    private val _notifications = MutableStateFlow<List<NotificationItem>>(emptyList())
    val notifications: StateFlow<List<NotificationItem>> = _notifications.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _bannerMessage = MutableStateFlow<String?>(null)
    val bannerMessage: StateFlow<String?> = _bannerMessage.asStateFlow()

    init {
        viewModelScope.launch {
            repository.restoreSession()
            loadFeed()
            loadProfiles()
            loadSavedPosts()
        }
    }

    fun clearBanner() {
        _bannerMessage.value = null
    }

    fun setBanner(msg: String) {
        _bannerMessage.value = msg
    }

    fun login(email: String, pass: String, onDone: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            val result = repository.login(email.trim(), pass)
            result.onSuccess {
                loadFeed()
                loadProfiles()
                loadSavedPosts()
                onDone(true, null)
            }.onFailure { e ->
                onDone(false, e.localizedMessage ?: "Login failed")
            }
        }
    }

    fun signUp(email: String, pass: String, username: String, displayName: String, onDone: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            val result = repository.signUp(email.trim(), pass, username.trim(), displayName.trim())
            result.onSuccess {
                loadFeed()
                loadProfiles()
                loadSavedPosts()
                onDone(true, null)
            }.onFailure { e ->
                onDone(false, e.localizedMessage ?: "Registration failed")
            }
        }
    }

    fun loginAsDemoUser(profile: UserProfile) {
        repository.loginAsDemoUser(profile)
        viewModelScope.launch {
            repository.loadUserEngagement(profile.id)
            loadFeed()
            loadProfiles()
            loadSavedPosts()
        }
    }

    fun logout() {
        repository.logout()
        _savedPosts.value = emptyList()
    }

    fun loadFeed() {
        viewModelScope.launch {
            _isFeedLoading.value = true
            val res = repository.getFeedPosts()
            res.onSuccess { list ->
                _feedPosts.value = list
            }
            _isFeedLoading.value = false
        }
    }

    fun loadSavedPosts() {
        viewModelScope.launch {
            val res = repository.getSavedPosts()
            res.onSuccess { list ->
                _savedPosts.value = list
            }
        }
    }

    fun setCategory(category: String) {
        _selectedCategory.value = category
    }

    fun loadProfiles() {
        viewModelScope.launch {
            val res = repository.getAllProfiles()
            res.onSuccess { list ->
                _allProfiles.value = list
            }
        }
    }

    fun createPost(type: String, title: String?, content: String, url: String?, onDone: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            val res = repository.createPost(type, title, content, url)
            res.onSuccess { newPost ->
                _feedPosts.value = listOf(newPost) + _feedPosts.value
                setBanner("Post published!")
                onDone(true, null)
            }.onFailure { e ->
                onDone(false, e.localizedMessage ?: "Failed to post")
            }
        }
    }

    fun createVideoPost(
        title: String?,
        videoUrl: String,
        embedUrl: String? = null,
        directUrl: String? = null,
        platform: String? = null,
        thumbnailUrl: String? = null,
        durationSeconds: Int? = null,
        width: Int? = null,
        height: Int? = null,
        isVertical: Boolean? = false,
        category: String? = null,
        tags: List<String> = emptyList(),
        onDone: (Boolean, String?) -> Unit
    ) {
        viewModelScope.launch {
            val res = repository.createVideoPost(
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
                tags = tags
            )
            res.onSuccess { newPost ->
                _feedPosts.value = listOf(newPost) + _feedPosts.value
                setBanner("Video post published!")
                onDone(true, null)
            }.onFailure { e ->
                onDone(false, e.localizedMessage ?: "Failed to publish video post")
            }
        }
    }

    fun createPhotoPost(
        caption: String?,
        photoUrls: List<String>,
        category: String?,
        tags: List<String>,
        onDone: (Boolean, String?) -> Unit
    ) {
        viewModelScope.launch {
            val res = repository.createPhotoPost(caption, photoUrls, category, tags)
            res.onSuccess { newPost ->
                _feedPosts.value = listOf(newPost) + _feedPosts.value
                setBanner("Photo gallery published!")
                onDone(true, null)
            }.onFailure { e ->
                onDone(false, e.localizedMessage ?: "Failed to publish photo gallery")
            }
        }
    }

    fun createTextPost(
        caption: String,
        templateId: String?,
        category: String?,
        tags: List<String>,
        onDone: (Boolean, String?) -> Unit
    ) {
        viewModelScope.launch {
            val res = repository.createTextPost(caption, templateId, category, tags)
            res.onSuccess { newPost ->
                _feedPosts.value = listOf(newPost) + _feedPosts.value
                setBanner("Thought post published!")
                onDone(true, null)
            }.onFailure { e ->
                onDone(false, e.localizedMessage ?: "Failed to publish thought post")
            }
        }
    }

    // Toggle reaction via post_reactions table
    fun toggleLike(post: Post) {
        val currentIsLiked = post.isLikedByMe
        val newIsLiked = !currentIsLiked
        val newCount = if (newIsLiked) post.appreciationsCount + 1 else (post.appreciationsCount - 1).coerceAtLeast(0)

        // Optimistic UI update
        _feedPosts.value = _feedPosts.value.map {
            if (it.id == post.id) it.copy(isLikedByMe = newIsLiked, appreciationsCount = newCount) else it
        }
        _viewedUserPosts.value = _viewedUserPosts.value.map {
            if (it.id == post.id) it.copy(isLikedByMe = newIsLiked, appreciationsCount = newCount) else it
        }
        _savedPosts.value = _savedPosts.value.map {
            if (it.id == post.id) it.copy(isLikedByMe = newIsLiked, appreciationsCount = newCount) else it
        }

        viewModelScope.launch {
            repository.toggleLike(post.id)
        }
    }

    // Toggle bookmark via saved_posts table
    fun toggleBookmark(post: Post) {
        val currentIsSaved = post.isBookmarkedByMe
        val newIsSaved = !currentIsSaved

        _feedPosts.value = _feedPosts.value.map {
            if (it.id == post.id) it.copy(isBookmarkedByMe = newIsSaved) else it
        }
        _viewedUserPosts.value = _viewedUserPosts.value.map {
            if (it.id == post.id) it.copy(isBookmarkedByMe = newIsSaved) else it
        }

        viewModelScope.launch {
            val isNowSaved = repository.toggleBookmark(post.id)
            setBanner(if (isNowSaved) "Saved to Bookmarks" else "Removed from Bookmarks")
            loadSavedPosts()
        }
    }

    fun openComments(post: Post) {
        _selectedPostForComments.value = post
        _comments.value = emptyList()
        _isCommentsLoading.value = true
        viewModelScope.launch {
            val res = repository.getComments(post.id)
            res.onSuccess {
                _comments.value = it
            }
            _isCommentsLoading.value = false
        }
    }

    fun closeComments() {
        _selectedPostForComments.value = null
        _comments.value = emptyList()
    }

    fun addComment(text: String, onDone: (Boolean) -> Unit) {
        val post = _selectedPostForComments.value ?: return
        if (text.isBlank()) return
        viewModelScope.launch {
            val res = repository.addComment(post.id, text.trim())
            res.onSuccess { comment ->
                _comments.value = _comments.value + comment
                _feedPosts.value = _feedPosts.value.map {
                    if (it.id == post.id) it.copy(repliesCount = it.repliesCount + 1) else it
                }
                onDone(true)
            }.onFailure {
                onDone(false)
            }
        }
    }

    fun viewUserProfile(profile: UserProfile) {
        _viewedProfile.value = profile
        loadUserPosts(profile.id)
        loadSocialGraphCounts(profile.id)
    }

    fun loadUserPosts(userId: String) {
        viewModelScope.launch {
            _isProfileLoading.value = true
            val res = repository.getUserPosts(userId)
            res.onSuccess {
                _viewedUserPosts.value = it
            }
            _isProfileLoading.value = false
        }
    }

    fun loadSocialGraphCounts(userId: String) {
        viewModelScope.launch {
            _trackersCount.value = repository.getFollowersCount(userId)
            _trackingCount.value = repository.getFollowingCount(userId)
        }
    }

    fun updateProfile(userId: String, updates: ProfileUpdateRequest, onDone: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            val res = repository.updateProfile(userId, updates)
            res.onSuccess { updated ->
                _viewedProfile.value = updated
                _allProfiles.value = _allProfiles.value.map { if (it.id == updated.id) updated else it }
                setBanner("Profile updated successfully!")
                onDone(true, null)
            }.onFailure { e ->
                onDone(false, e.localizedMessage ?: "Failed to update profile")
            }
        }
    }

    fun toggleFollow(targetProfile: UserProfile) {
        viewModelScope.launch {
            val isNowFollowing = repository.toggleFollow(targetProfile.id)
            setBanner(if (isNowFollowing) "Tracking @${targetProfile.effectiveUsername}" else "Unfollowed")
            loadProfiles()
            loadSocialGraphCounts(targetProfile.id)
        }
    }

    fun isUserFollowed(userId: String): Boolean = repository.isUserFollowed(userId)

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    private val _groups = MutableStateFlow<List<com.example.data.model.GroupCommunity>>(emptyList())
    val groups: StateFlow<List<com.example.data.model.GroupCommunity>> = _groups.asStateFlow()

    private val _isGroupsLoading = MutableStateFlow(false)
    val isGroupsLoading: StateFlow<Boolean> = _isGroupsLoading.asStateFlow()

    fun editPost(postId: String, newContent: String, newTitle: String? = null, onDone: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            val res = repository.updatePost(postId, newContent, newTitle)
            res.onSuccess { updatedPost ->
                _feedPosts.value = _feedPosts.value.map { if (it.id == postId) updatedPost else it }
                _viewedUserPosts.value = _viewedUserPosts.value.map { if (it.id == postId) updatedPost else it }
                setBanner("Post updated successfully!")
                onDone(true, null)
            }.onFailure { e ->
                onDone(false, e.localizedMessage ?: "Failed to edit post")
            }
        }
    }

    fun deletePost(postId: String, onDone: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            val res = repository.deletePost(postId)
            res.onSuccess {
                _feedPosts.value = _feedPosts.value.filter { it.id != postId }
                _viewedUserPosts.value = _viewedUserPosts.value.filter { it.id != postId }
                _savedPosts.value = _savedPosts.value.filter { it.id != postId }
                setBanner("Post deleted")
                onDone(true, null)
            }.onFailure { e ->
                onDone(false, e.localizedMessage ?: "Failed to delete post")
            }
        }
    }

    fun loadGroups() {
        viewModelScope.launch {
            _isGroupsLoading.value = true
            val res = repository.getGroups()
            res.onSuccess { list ->
                _groups.value = list
            }
            _isGroupsLoading.value = false
        }
    }

    fun createCommunityGroup(name: String, description: String, category: String = "general", onDone: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            val res = repository.createGroup(name, description, category)
            res.onSuccess { newGroup ->
                _groups.value = listOf(newGroup) + _groups.value
                setBanner("Community group created!")
                onDone(true, null)
            }.onFailure { e ->
                onDone(false, e.localizedMessage ?: "Failed to create group")
            }
        }
    }

    fun toggleJoinGroup(group: com.example.data.model.GroupCommunity) {
        val currentJoined = group.isJoinedByMe
        val newJoined = !currentJoined
        val newCount = if (newJoined) group.membersCount + 1 else (group.membersCount - 1).coerceAtLeast(1)

        _groups.value = _groups.value.map {
            if (it.id == group.id) it.copy(isJoinedByMe = newJoined, membersCount = newCount) else it
        }

        viewModelScope.launch {
            if (newJoined) {
                repository.joinGroup(group.id)
                setBanner("Joined ${group.name}")
            } else {
                repository.leaveGroup(group.id)
                setBanner("Left ${group.name}")
            }
        }
    }

    private val _selectedGroup = MutableStateFlow<com.example.data.model.GroupCommunity?>(null)
    val selectedGroup: StateFlow<com.example.data.model.GroupCommunity?> = _selectedGroup.asStateFlow()

    private val _groupPosts = MutableStateFlow<List<Post>>(emptyList())
    val groupPosts: StateFlow<List<Post>> = _groupPosts.asStateFlow()

    private val _groupMembers = MutableStateFlow<List<com.example.data.model.GroupMember>>(emptyList())
    val groupMembers: StateFlow<List<com.example.data.model.GroupMember>> = _groupMembers.asStateFlow()

    fun selectGroup(group: com.example.data.model.GroupCommunity?) {
        _selectedGroup.value = group
        if (group != null) {
            loadGroupPosts(group.id)
            loadGroupMembers(group.id)
        } else {
            _groupPosts.value = emptyList()
            _groupMembers.value = emptyList()
        }
    }

    fun loadGroupPosts(groupId: String) {
        viewModelScope.launch {
            val res = repository.getPostsByGroup(groupId)
            res.onSuccess { list ->
                _groupPosts.value = list
            }.onFailure {
                _groupPosts.value = emptyList()
            }
        }
    }

    fun loadGroupMembers(groupId: String) {
        viewModelScope.launch {
            val res = repository.getGroupMembers(groupId)
            res.onSuccess { list ->
                _groupMembers.value = list
            }.onFailure {
                _groupMembers.value = emptyList()
            }
        }
    }

    fun createVideoPostInGroup(
        groupId: String,
        title: String?,
        videoUrl: String,
        embedUrl: String? = null,
        directUrl: String? = null,
        platform: String? = null,
        thumbnailUrl: String? = null,
        durationSeconds: Int? = null,
        width: Int? = null,
        height: Int? = null,
        isVertical: Boolean? = false,
        category: String? = null,
        tags: List<String> = emptyList(),
        onDone: (Boolean, String?) -> Unit
    ) {
        viewModelScope.launch {
            val res = repository.createVideoPost(
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
                groupId = groupId
            )
            res.onSuccess { newPost ->
                _groupPosts.value = listOf(newPost) + _groupPosts.value
                _feedPosts.value = listOf(newPost) + _feedPosts.value
                setBanner("Posted in community!")
                onDone(true, null)
            }.onFailure { e ->
                onDone(false, e.localizedMessage ?: "Failed to post in group")
            }
        }
    }

    fun createPhotoPostInGroup(
        groupId: String,
        caption: String?,
        photoUrls: List<String>,
        category: String?,
        tags: List<String>,
        onDone: (Boolean, String?) -> Unit
    ) {
        viewModelScope.launch {
            val res = repository.createPhotoPost(caption, photoUrls, category, tags, groupId = groupId)
            res.onSuccess { newPost ->
                _groupPosts.value = listOf(newPost) + _groupPosts.value
                _feedPosts.value = listOf(newPost) + _feedPosts.value
                setBanner("Posted in community!")
                onDone(true, null)
            }.onFailure { e ->
                onDone(false, e.localizedMessage ?: "Failed to post in group")
            }
        }
    }

    fun createTextPostInGroup(
        groupId: String,
        caption: String,
        templateId: String?,
        category: String?,
        tags: List<String>,
        onDone: (Boolean, String?) -> Unit
    ) {
        viewModelScope.launch {
            val res = repository.createTextPost(caption, templateId, category, tags, groupId = groupId)
            res.onSuccess { newPost ->
                _groupPosts.value = listOf(newPost) + _groupPosts.value
                _feedPosts.value = listOf(newPost) + _feedPosts.value
                setBanner("Posted in community!")
                onDone(true, null)
            }.onFailure { e ->
                onDone(false, e.localizedMessage ?: "Failed to post in group")
            }
        }
    }

    fun loadNotifications() {
        viewModelScope.launch {
            val res = repository.getNotifications()
            res.onSuccess {
                _notifications.value = it
            }
        }
    }
}

