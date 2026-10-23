package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.data.api.SupabaseClient
import com.example.data.model.AuthResponse
import com.example.data.model.Comment
import com.example.data.model.CreateCommentRequest
import com.example.data.model.CreateFollowRequest
import com.example.data.model.CreatePostRequest
import com.example.data.model.CreateSavedPostRequest
import com.example.data.model.Follow
import com.example.data.model.NotificationItem
import com.example.data.model.Post
import com.example.data.model.PostReaction
import com.example.data.model.ProfileUpdateRequest
import com.example.data.model.SavedPost
import com.example.data.model.UserProfile
import com.example.data.model.CreatePhotoPostRequest
import com.example.data.model.CreateTextPostRequest
import com.example.data.model.CreateVideoPostRequest
import com.example.data.model.PhotoPost
import com.example.data.model.TextPost
import com.example.data.model.VideoPost
import com.example.util.VideoEmbedUtils
import com.example.data.service.SupabaseService
import com.example.data.model.SavedAccount
import org.json.JSONArray
import org.json.JSONObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class SocialRepository(
    context: Context,
    private val service: SupabaseService = SupabaseService.getInstance()
) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("q_social_prefs", Context.MODE_PRIVATE)

    private val _currentUser = MutableStateFlow<UserProfile?>(null)
    val currentUser: StateFlow<UserProfile?> = _currentUser.asStateFlow()

    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    private val _savedAccounts = MutableStateFlow<List<SavedAccount>>(emptyList())
    val savedAccounts: StateFlow<List<SavedAccount>> = _savedAccounts.asStateFlow()

    private val followedUserIds = mutableSetOf<String>()
    private val likedPostIds = mutableSetOf<String>()
    private val savedPostIds = mutableSetOf<String>()
    private val hiddenPostIds = mutableSetOf<String>()
    private val likedCommentIds = mutableSetOf<String>()

    init {
        loadSavedAccounts()
        val savedUserId = prefs.getString("saved_user_id", null)
        val savedToken = prefs.getString("saved_token", null)
        if (!savedToken.isNullOrBlank()) {
            service.setAuthToken(savedToken)
        }
        if (!savedUserId.isNullOrBlank()) {
            _currentUser.value = UserProfile(
                id = savedUserId,
                username = prefs.getString("saved_username", "user"),
                displayName = prefs.getString("saved_display_name", "Q Creator"),
                avatarUrl = prefs.getString("saved_avatar_url", null)
            )
            _isLoggedIn.value = true
        }
    }

    private fun loadSavedAccounts() {
        try {
            val jsonStr = prefs.getString("saved_accounts_list_v1", null)
            if (!jsonStr.isNullOrBlank()) {
                val array = JSONArray(jsonStr)
                val list = mutableListOf<SavedAccount>()
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    list.add(
                        SavedAccount(
                            email = obj.optString("email", ""),
                            pass = obj.optString("pass", ""),
                            displayName = obj.optString("displayName", ""),
                            username = obj.optString("username", ""),
                            avatarUrl = if (obj.has("avatarUrl") && !obj.isNull("avatarUrl")) obj.getString("avatarUrl") else null
                        )
                    )
                }
                _savedAccounts.value = list
            } else {
                _savedAccounts.value = emptyList()
            }
        } catch (_: Exception) {
            _savedAccounts.value = emptyList()
        }
    }

    fun saveAccountCredential(email: String, pass: String, displayName: String, username: String, avatarUrl: String?) {
        if (email.isBlank() || pass.isBlank()) return
        val currentList = _savedAccounts.value.toMutableList()
        currentList.removeAll { it.email.equals(email, ignoreCase = true) }
        currentList.add(
            0,
            SavedAccount(
                email = email,
                pass = pass,
                displayName = displayName.ifBlank { email.substringBefore("@") },
                username = username.ifBlank { email.substringBefore("@") },
                avatarUrl = avatarUrl
            )
        )
        persistSavedAccounts(currentList)
    }

    fun removeSavedAccount(email: String) {
        val currentList = _savedAccounts.value.toMutableList()
        currentList.removeAll { it.email.equals(email, ignoreCase = true) }
        persistSavedAccounts(currentList)
    }

    private fun persistSavedAccounts(list: List<SavedAccount>) {
        _savedAccounts.value = list
        try {
            val array = JSONArray()
            list.forEach { item ->
                val obj = JSONObject()
                obj.put("email", item.email)
                obj.put("pass", item.pass)
                obj.put("displayName", item.displayName)
                obj.put("username", item.username)
                if (item.avatarUrl != null) {
                    obj.put("avatarUrl", item.avatarUrl)
                }
                array.put(obj)
            }
            prefs.edit().putString("saved_accounts_list_v1", array.toString()).apply()
        } catch (_: Exception) {}
    }

    suspend fun restoreSession() = withContext(Dispatchers.IO) {
        val userId = _currentUser.value?.id
        if (!userId.isNullOrBlank()) {
            try {
                val res = service.getProfileById(userId)
                res.getOrNull()?.let { profile ->
                    _currentUser.value = profile
                    saveUserSession(profile, service.getAuthToken())
                }
            } catch (_: Exception) {}

            // Load user engagement state from Supabase tables
            loadUserEngagement(userId)
        }
    }

    suspend fun loadUserEngagement(userId: String) = withContext(Dispatchers.IO) {
        // 1. Load likes from post_reactions
        try {
            val resReactions = service.getUserReactions(userId)
            resReactions.getOrNull()?.let { reactions ->
                likedPostIds.clear()
                reactions.forEach { likedPostIds.add(it.postId) }
            }
        } catch (_: Exception) {}

        // 2. Load bookmarks from saved_posts
        try {
            val resSaved = service.getSavedPosts(userId)
            resSaved.getOrNull()?.let { saved ->
                savedPostIds.clear()
                saved.forEach { savedPostIds.add(it.postId) }
            }
        } catch (_: Exception) {}

        // 3. Load following from follows
        try {
            val resFollows = service.getFollowing(userId)
            resFollows.getOrNull()?.let { follows ->
                followedUserIds.clear()
                follows.forEach { followedUserIds.add(it.followingId) }
            }
        } catch (_: Exception) {}

        // 4. Load hidden posts from hidden_posts
        try {
            val resHidden = SupabaseClient.api.getHiddenPostIds("eq.$userId")
            if (resHidden.isSuccessful && resHidden.body() != null) {
                hiddenPostIds.clear()
                resHidden.body()!!.forEach { map ->
                    map["post_id"]?.let { hiddenPostIds.add(it) }
                }
            }
        } catch (_: Exception) {}

        // 5. Load comment likes from comment_likes
        try {
            val resCommentLikes = SupabaseClient.api.getUserCommentLikes("eq.$userId")
            if (resCommentLikes.isSuccessful && resCommentLikes.body() != null) {
                likedCommentIds.clear()
                resCommentLikes.body()!!.forEach { likedCommentIds.add(it.commentId) }
            }
        } catch (_: Exception) {}
    }

    // 1. Table: profiles & Auth
    suspend fun login(email: String, pass: String): Result<UserProfile> = withContext(Dispatchers.IO) {
        try {
            val authResult = service.signIn(email, pass)
            authResult.fold(
                onSuccess = { body ->
                    val token = body.accessToken
                    val userId = body.user?.id ?: UUID.randomUUID().toString()
                    val profile = fetchOrCreateProfile(
                        userId = userId,
                        email = email,
                        name = body.user?.displayName ?: body.user?.username
                    )
                    _currentUser.value = profile
                    _isLoggedIn.value = true
                    saveUserSession(profile, token)
                    saveAccountCredential(
                        email = email,
                        pass = pass,
                        displayName = profile.effectiveDisplayName,
                        username = profile.effectiveUsername,
                        avatarUrl = profile.avatarUrl
                    )
                    loadUserEngagement(profile.id)
                    Result.success(profile)
                },
                onFailure = { error ->
                    Result.failure(error)
                }
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun signUp(email: String, pass: String, username: String, displayName: String): Result<UserProfile> = withContext(Dispatchers.IO) {
        try {
            val cleanUsername = username.ifBlank { email.substringBefore("@") }
            val cleanDisplayName = displayName.ifBlank { "Q Creator" }
            val authResult = service.signUp(email, pass, cleanUsername, cleanDisplayName)

            authResult.fold(
                onSuccess = { body ->
                    val token = body.accessToken
                    val userId = body.user?.id ?: UUID.randomUUID().toString()
                    val newProfile = UserProfile(
                        id = userId,
                        username = cleanUsername,
                        displayName = cleanDisplayName,
                        bio = "Hello, I am on Qylomi!",
                        creatorRank = "Novice Explorer",
                        creatorLevel = 1,
                        points = 50,
                        badges = listOf("first_spark")
                    )

                    try {
                        service.insertProfile(newProfile)
                    } catch (_: Exception) {}

                    _currentUser.value = newProfile
                    _isLoggedIn.value = true
                    saveUserSession(newProfile, token)
                    saveAccountCredential(
                        email = email,
                        pass = pass,
                        displayName = newProfile.effectiveDisplayName,
                        username = newProfile.effectiveUsername,
                        avatarUrl = newProfile.avatarUrl
                    )
                    loadUserEngagement(newProfile.id)
                    Result.success(newProfile)
                },
                onFailure = { error ->
                    Result.failure(error)
                }
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }


    fun loginAsDemoUser(profile: UserProfile) {
        _currentUser.value = profile
        _isLoggedIn.value = true
        saveUserSession(profile, null)
    }

    fun logout() {
        _currentUser.value = null
        _isLoggedIn.value = false
        SupabaseClient.setAuthToken(null)
        likedPostIds.clear()
        savedPostIds.clear()
        followedUserIds.clear()
        prefs.edit()
            .remove("saved_user_id")
            .remove("saved_username")
            .remove("saved_display_name")
            .remove("saved_avatar_url")
            .remove("saved_token")
            .apply()
    }

    private fun saveUserSession(profile: UserProfile, token: String?) {
        prefs.edit()
            .putString("saved_user_id", profile.id)
            .putString("saved_username", profile.username)
            .putString("saved_display_name", profile.displayName)
            .putString("saved_avatar_url", profile.avatarUrl)
            .putString("saved_token", token)
            .apply()
    }

    private suspend fun fetchOrCreateProfile(userId: String, email: String, name: String?): UserProfile {
        try {
            val res = SupabaseClient.api.getProfileById("eq.$userId")
            if (res.isSuccessful && !res.body().isNullOrEmpty()) {
                return res.body()!!.first()
            }
        } catch (_: Exception) {}

        val generatedName = name ?: email.substringBefore("@").replace(".", " ").capitalize(Locale.ROOT)
        val newProfile = UserProfile(
            id = userId,
            username = email.substringBefore("@"),
            displayName = generatedName,
            bio = "New to Qylomi",
            creatorRank = "Novice Explorer",
            creatorLevel = 1,
            points = 25
        )
        try {
            SupabaseClient.api.insertProfile(newProfile)
        } catch (_: Exception) {}
        return newProfile
    }

    // 2. Table: posts
    suspend fun getFeedPosts(): Result<List<Post>> = withContext(Dispatchers.IO) {
        try {
            val res = SupabaseClient.api.getPosts(limit = 50)
            if (res.isSuccessful && res.body() != null) {
                val posts = res.body()!!
                    .filter { !hiddenPostIds.contains(it.id) }
                    .map { post ->
                        post.copy(
                            isLikedByMe = likedPostIds.contains(post.id),
                            isBookmarkedByMe = savedPostIds.contains(post.id)
                        )
                    }
                Result.success(posts)
            } else {
                // Fallback to legacy posts select query if sub-tables are not created in PostgREST schema yet
                val legacyRes = SupabaseClient.api.getPostsLegacy(limit = 50)
                if (legacyRes.isSuccessful && legacyRes.body() != null) {
                    val posts = legacyRes.body()!!
                        .filter { !hiddenPostIds.contains(it.id) }
                        .map { post ->
                            post.copy(
                                isLikedByMe = likedPostIds.contains(post.id),
                                isBookmarkedByMe = savedPostIds.contains(post.id)
                            )
                        }
                    Result.success(posts)
                } else {
                    Result.failure(Exception(res.errorBody()?.string() ?: "Failed to load posts"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getUserPosts(userId: String): Result<List<Post>> = withContext(Dispatchers.IO) {
        try {
            val res = SupabaseClient.api.getPostsByUser("eq.$userId", limit = 40)
            if (res.isSuccessful && res.body() != null) {
                val posts = res.body()!!
                    .filter { !hiddenPostIds.contains(it.id) }
                    .map { post ->
                        post.copy(
                            isLikedByMe = likedPostIds.contains(post.id),
                            isBookmarkedByMe = savedPostIds.contains(post.id)
                        )
                    }
                    .sortedByDescending { it.isPinned }
                Result.success(posts)
            } else {
                Result.failure(Exception("Failed to load user posts"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createPost(type: String, title: String?, content: String, url: String?): Result<Post> = withContext(Dispatchers.IO) {
        val user = _currentUser.value ?: return@withContext Result.failure(Exception("Not logged in"))
        try {
            val request = CreatePostRequest(
                userId = user.id,
                type = type,
                title = title?.takeIf { it.isNotBlank() },
                content = content,
                externalUrl = url?.takeIf { it.isNotBlank() }
            )
            val res = SupabaseClient.api.createPost(request)
            if (res.isSuccessful && !res.body().isNullOrEmpty()) {
                val created = res.body()!!.first().copy(authorProfile = user)
                Result.success(created)
            } else {
                Result.failure(Exception(res.errorBody()?.string() ?: "Failed to publish post"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createVideoPost(
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
        groupId: String? = null,
        externalId: String? = null
    ): Result<Post> = withContext(Dispatchers.IO) {
        val user = _currentUser.value ?: return@withContext Result.failure(Exception("Not logged in"))
        try {
            val parsedVideo = com.example.video.VideoPlatformRegistry.parse(videoUrl)
            val effectiveEmbedUrl = embedUrl ?: if (parsedVideo.videoId != null && parsedVideo.platform == "youtube") {
                "https://www.youtube.com/embed/${parsedVideo.videoId}"
            } else {
                parsedVideo.originalUrl
            }
            val effectivePlatform = platform ?: parsedVideo.platform
            val effectiveExternalId = externalId ?: parsedVideo.videoId
            val mainRequest = CreatePostRequest(
                userId = user.id,
                type = "Post", // Safe for posts_type_check constraint
                title = title?.takeIf { it.isNotBlank() } ?: "Video Post",
                content = title?.takeIf { it.isNotBlank() } ?: "Shared a video link",
                externalUrl = videoUrl,
                groupId = groupId
            )
            val res = SupabaseClient.api.createPost(mainRequest)
            if (res.isSuccessful && !res.body().isNullOrEmpty()) {
                val createdPost = res.body()!!.first()
                val videoSubRequest = CreateVideoPostRequest(
                    postId = createdPost.id,
                    externalId = effectiveExternalId,
                    title = title?.takeIf { it.isNotBlank() },
                    thumbnailUrl = thumbnailUrl?.takeIf { it.isNotBlank() },
                    videoUrl = videoUrl,
                    embedUrl = effectiveEmbedUrl,
                    directUrl = directUrl,
                    platform = effectivePlatform,
                    durationSeconds = durationSeconds,
                    width = width,
                    height = height,
                    isVertical = isVertical,
                    category = category,
                    tags = tags
                )
                val subTableRecord = try {
                    val subRes = SupabaseClient.api.createVideoPost(videoSubRequest)
                    if (subRes.isSuccessful && !subRes.body().isNullOrEmpty()) subRes.body()!!.first() else null
                } catch (_: Exception) { null }

                val enriched = createdPost.copy(
                    authorProfile = user,
                    groupId = groupId,
                    videoPosts = listOf(
                        subTableRecord ?: VideoPost(
                            postId = createdPost.id,
                            externalId = effectiveExternalId,
                            title = title,
                            thumbnailUrl = thumbnailUrl,
                            videoUrl = videoUrl,
                            embedUrl = effectiveEmbedUrl,
                            directUrl = directUrl,
                            platform = effectivePlatform,
                            durationSeconds = durationSeconds,
                            width = width,
                            height = height,
                            isVertical = isVertical,
                            category = category,
                            tags = tags
                        )
                    )
                )
                Result.success(enriched)
            } else {
                Result.failure(Exception(res.errorBody()?.string() ?: "Failed to create video post"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createPhotoPost(
        caption: String?,
        photoUrls: List<String>,
        category: String?,
        tags: List<String>,
        groupId: String? = null
    ): Result<Post> = withContext(Dispatchers.IO) {
        val user = _currentUser.value ?: return@withContext Result.failure(Exception("Not logged in"))
        if (photoUrls.isEmpty()) return@withContext Result.failure(Exception("Please add at least 1 photo URL"))
        try {
            val mainRequest = CreatePostRequest(
                userId = user.id,
                type = "Photo",
                title = "Photo Gallery",
                content = caption?.takeIf { it.isNotBlank() } ?: "Shared photo gallery",
                externalUrl = photoUrls.firstOrNull(),
                groupId = groupId
            )
            val res = SupabaseClient.api.createPost(mainRequest)
            if (res.isSuccessful && !res.body().isNullOrEmpty()) {
                val createdPost = res.body()!!.first()
                val photoSubRequest = CreatePhotoPostRequest(
                    postId = createdPost.id,
                    caption = caption?.takeIf { it.isNotBlank() },
                    photoUrls = photoUrls,
                    category = category,
                    tags = tags
                )
                val subTableRecord = try {
                    val subRes = SupabaseClient.api.createPhotoPost(photoSubRequest)
                    if (subRes.isSuccessful && !subRes.body().isNullOrEmpty()) subRes.body()!!.first() else null
                } catch (_: Exception) { null }

                val enriched = createdPost.copy(
                    authorProfile = user,
                    groupId = groupId,
                    photoPosts = listOf(
                        subTableRecord ?: PhotoPost(
                            postId = createdPost.id,
                            caption = caption,
                            photoUrls = photoUrls,
                            category = category,
                            tags = tags
                        )
                    )
                )
                Result.success(enriched)
            } else {
                Result.failure(Exception(res.errorBody()?.string() ?: "Failed to create photo post"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createTextPost(
        caption: String,
        templateId: String?,
        category: String?,
        tags: List<String>,
        groupId: String? = null
    ): Result<Post> = withContext(Dispatchers.IO) {
        val user = _currentUser.value ?: return@withContext Result.failure(Exception("Not logged in"))
        if (caption.isBlank()) return@withContext Result.failure(Exception("Content cannot be empty"))
        try {
            val mainRequest = CreatePostRequest(
                userId = user.id,
                type = "Text",
                title = "Thought",
                content = caption,
                groupId = groupId
            )
            val res = SupabaseClient.api.createPost(mainRequest)
            if (res.isSuccessful && !res.body().isNullOrEmpty()) {
                val createdPost = res.body()!!.first()
                val textSubRequest = CreateTextPostRequest(
                    postId = createdPost.id,
                    caption = caption,
                    bgColor = templateId ?: "preset_default",
                    category = category,
                    tags = tags
                )
                val subTableRecord = try {
                    val subRes = SupabaseClient.api.createTextPost(textSubRequest)
                    if (subRes.isSuccessful && !subRes.body().isNullOrEmpty()) subRes.body()!!.first() else null
                } catch (_: Exception) { null }

                val enriched = createdPost.copy(
                    authorProfile = user,
                    textPosts = listOf(
                        subTableRecord ?: TextPost(
                            postId = createdPost.id,
                            caption = caption,
                            bgColor = templateId ?: "preset_default",
                            category = category,
                            tags = tags
                        )
                    )
                )
                Result.success(enriched)
            } else {
                Result.failure(Exception(res.errorBody()?.string() ?: "Failed to create text post"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // 3. Table: post_reactions (Likes / Appreciations via Dedicated Table & Triggers)
    suspend fun toggleLike(postId: String): Boolean = withContext(Dispatchers.IO) {
        val user = _currentUser.value ?: return@withContext false
        val wasLiked = likedPostIds.contains(postId)

        if (wasLiked) {
            likedPostIds.remove(postId)
            try {
                SupabaseClient.api.removeReaction(
                    postIdFilter = "eq.$postId",
                    userIdFilter = "eq.${user.id}"
                )
            } catch (_: Exception) {}
            false
        } else {
            likedPostIds.add(postId)
            try {
                SupabaseClient.api.addReaction(
                    PostReaction(
                        postId = postId,
                        userId = user.id,
                        reactionType = "appreciate"
                    )
                )
            } catch (_: Exception) {}
            true
        }
    }

    // 4. Table: comments (Threaded replies with DB triggers)
    suspend fun getComments(postId: String): Result<List<Comment>> = withContext(Dispatchers.IO) {
        try {
            val res = SupabaseClient.api.getCommentsForPost("eq.$postId")
            if (res.isSuccessful && res.body() != null) {
                Result.success(res.body()!!)
            } else {
                Result.failure(Exception("Failed to fetch comments"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun addComment(postId: String, content: String): Result<Comment> = withContext(Dispatchers.IO) {
        val user = _currentUser.value ?: return@withContext Result.failure(Exception("Not logged in"))
        try {
            val req = CreateCommentRequest(
                postId = postId,
                userId = user.id,
                content = content
            )
            val res = SupabaseClient.api.createComment(req)
            if (res.isSuccessful && !res.body().isNullOrEmpty()) {
                val comment = res.body()!!.first().copy(authorProfile = user)
                Result.success(comment)
            } else {
                Result.failure(Exception(res.errorBody()?.string() ?: "Failed to post comment"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // 5. Table: saved_posts (Private bookmarks table)
    suspend fun toggleBookmark(postId: String): Boolean = withContext(Dispatchers.IO) {
        val user = _currentUser.value ?: return@withContext false
        val wasSaved = savedPostIds.contains(postId)

        if (wasSaved) {
            savedPostIds.remove(postId)
            try {
                SupabaseClient.api.unsavePost(
                    postIdFilter = "eq.$postId",
                    userIdFilter = "eq.${user.id}"
                )
            } catch (_: Exception) {}
            false
        } else {
            savedPostIds.add(postId)
            try {
                SupabaseClient.api.savePost(
                    CreateSavedPostRequest(
                        postId = postId,
                        userId = user.id
                    )
                )
            } catch (_: Exception) {}
            true
        }
    }

    suspend fun getSavedPosts(): Result<List<Post>> = withContext(Dispatchers.IO) {
        val user = _currentUser.value ?: return@withContext Result.success(emptyList())
        try {
            val res = SupabaseClient.api.getSavedPosts("eq.${user.id}")
            if (res.isSuccessful && res.body() != null) {
                val posts = res.body()!!.mapNotNull { it.post }.map { post ->
                    post.copy(
                        isLikedByMe = likedPostIds.contains(post.id),
                        isBookmarkedByMe = true
                    )
                }
                Result.success(posts)
            } else {
                Result.success(emptyList())
            }
        } catch (e: Exception) {
            Result.success(emptyList())
        }
    }

    // 6. Table: follows (Trackers & Tracking social graph)
    suspend fun toggleFollow(targetUserId: String): Boolean = withContext(Dispatchers.IO) {
        val currentUserId = _currentUser.value?.id ?: return@withContext false
        val isFollowing = followedUserIds.contains(targetUserId)
        try {
            if (isFollowing) {
                SupabaseClient.api.unfollowUser("eq.$currentUserId", "eq.$targetUserId")
                followedUserIds.remove(targetUserId)
                false
            } else {
                SupabaseClient.api.followUser(CreateFollowRequest(currentUserId, targetUserId))
                followedUserIds.add(targetUserId)
                true
            }
        } catch (_: Exception) {
            if (isFollowing) followedUserIds.remove(targetUserId) else followedUserIds.add(targetUserId)
            !isFollowing
        }
    }

    suspend fun getFollowersCount(userId: String): Int = withContext(Dispatchers.IO) {
        try {
            val res = SupabaseClient.api.getFollowers("eq.$userId")
            if (res.isSuccessful && res.body() != null) res.body()!!.size else 0
        } catch (_: Exception) {
            0
        }
    }

    suspend fun getFollowingCount(userId: String): Int = withContext(Dispatchers.IO) {
        try {
            val res = SupabaseClient.api.getFollowing("eq.$userId")
            if (res.isSuccessful && res.body() != null) res.body()!!.size else 0
        } catch (_: Exception) {
            0
        }
    }

    fun isUserFollowed(userId: String): Boolean = followedUserIds.contains(userId)

    // Profiles queries
    suspend fun getAllProfiles(): Result<List<UserProfile>> = withContext(Dispatchers.IO) {
        try {
            val res = SupabaseClient.api.getAllProfiles(limit = 60)
            if (res.isSuccessful && res.body() != null) {
                Result.success(res.body()!!)
            } else {
                Result.failure(Exception("Failed to fetch profiles"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getProfile(userId: String): Result<UserProfile> = withContext(Dispatchers.IO) {
        try {
            val res = SupabaseClient.api.getProfileById("eq.$userId")
            if (res.isSuccessful && !res.body().isNullOrEmpty()) {
                Result.success(res.body()!!.first())
            } else {
                Result.failure(Exception("Profile not found"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateProfile(userId: String, updates: ProfileUpdateRequest): Result<UserProfile> = withContext(Dispatchers.IO) {
        try {
            val nowIso = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).format(Date())
            val req = updates.copy(updatedAt = nowIso)
            val res = SupabaseClient.api.updateProfile("eq.$userId", req)
            if (res.isSuccessful && !res.body().isNullOrEmpty()) {
                val updated = res.body()!!.first()
                if (_currentUser.value?.id == userId) {
                    _currentUser.value = updated
                    saveUserSession(updated, SupabaseClient.getAuthToken())
                }
                Result.success(updated)
            } else {
                val refreshed = getProfile(userId).getOrNull()
                if (refreshed != null) {
                    if (_currentUser.value?.id == userId) {
                        _currentUser.value = refreshed
                        saveUserSession(refreshed, SupabaseClient.getAuthToken())
                    }
                    Result.success(refreshed)
                } else {
                    Result.failure(Exception(res.errorBody()?.string() ?: "Failed to update profile"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // 7. Table: notifications (using recipient_id from Datasetuse.md)
    suspend fun getNotifications(): Result<List<NotificationItem>> = withContext(Dispatchers.IO) {
        val userId = _currentUser.value?.id ?: return@withContext Result.success(emptyList())
        try {
            val res = SupabaseClient.api.getNotifications("eq.$userId")
            if (res.isSuccessful && res.body() != null) {
                Result.success(res.body()!!)
            } else {
                Result.success(emptyList())
            }
        } catch (e: Exception) {
            Result.success(emptyList())
        }
    }

    // 8. Edit & Delete Post
    suspend fun updatePost(postId: String, newContent: String, newTitle: String? = null): Result<Post> = withContext(Dispatchers.IO) {
        try {
            val token = SupabaseClient.getAuthToken()?.let { "Bearer $it" }
            val req = com.example.data.model.UpdatePostRequest(title = newTitle, content = newContent)
            val res = SupabaseClient.api.updatePost("eq.$postId", req, userToken = token)
            if (res.isSuccessful && !res.body().isNullOrEmpty()) {
                Result.success(res.body()!!.first())
            } else {
                Result.failure(Exception("Failed to edit post"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deletePost(postId: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val token = SupabaseClient.getAuthToken()?.let { "Bearer $it" }
            val res = SupabaseClient.api.deletePost("eq.$postId", userToken = token)
            if (res.isSuccessful) {
                Result.success(true)
            } else {
                Result.failure(Exception("Failed to delete post"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Post actions & Settings
    suspend fun hidePost(postId: String): Result<Boolean> = withContext(Dispatchers.IO) {
        val user = _currentUser.value ?: return@withContext Result.failure(Exception("Not logged in"))
        hiddenPostIds.add(postId)
        try {
            SupabaseClient.api.hidePost(com.example.data.model.CreateHiddenPostRequest(userId = user.id, postId = postId))
            Result.success(true)
        } catch (e: Exception) {
            Result.success(true) // Optimistic hide
        }
    }

    suspend fun unhidePost(postId: String): Result<Boolean> = withContext(Dispatchers.IO) {
        val user = _currentUser.value ?: return@withContext Result.failure(Exception("Not logged in"))
        hiddenPostIds.remove(postId)
        try {
            SupabaseClient.api.unhidePost("eq.${user.id}", "eq.$postId")
            Result.success(true)
        } catch (e: Exception) {
            Result.success(true)
        }
    }

    suspend fun reportContent(postId: String?, commentId: String?, reason: String, details: String?): Result<Boolean> = withContext(Dispatchers.IO) {
        val user = _currentUser.value ?: return@withContext Result.failure(Exception("Not logged in"))
        try {
            val req = com.example.data.model.CreateReportRequest(
                reporterId = user.id,
                postId = postId,
                commentId = commentId,
                reason = reason,
                details = details
            )
            val res = SupabaseClient.api.createReport(req)
            if (res.isSuccessful) {
                Result.success(true)
            } else {
                val err = res.errorBody()?.string() ?: ""
                if (err.contains("unique") || err.contains("duplicate") || err.contains("23505")) {
                    Result.failure(Exception("You have already reported this item."))
                } else {
                    Result.failure(Exception("Failed to submit report"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun togglePinPost(postId: String, isCurrentlyPinned: Boolean): Result<Post> = withContext(Dispatchers.IO) {
        try {
            val token = SupabaseClient.getAuthToken()?.let { "Bearer $it" }
            val newIsPinned = !isCurrentlyPinned
            val nowIso = if (newIsPinned) java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US).format(java.util.Date()) else null
            val req = com.example.data.model.UpdatePostRequest(isPinned = newIsPinned, pinnedAt = nowIso)
            val res = SupabaseClient.api.updatePost("eq.$postId", req, userToken = token)
            if (res.isSuccessful && !res.body().isNullOrEmpty()) {
                Result.success(res.body()!!.first())
            } else {
                Result.failure(Exception("Failed to update post pin state"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun toggleAllowComments(postId: String, isCurrentlyAllowed: Boolean): Result<Post> = withContext(Dispatchers.IO) {
        try {
            val token = SupabaseClient.getAuthToken()?.let { "Bearer $it" }
            val newAllow = !isCurrentlyAllowed
            val req = com.example.data.model.UpdatePostRequest(allowComments = newAllow)
            val res = SupabaseClient.api.updatePost("eq.$postId", req, userToken = token)
            if (res.isSuccessful && !res.body().isNullOrEmpty()) {
                Result.success(res.body()!!.first())
            } else {
                Result.failure(Exception("Failed to update comment settings"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getPostLikers(postId: String, limit: Int = 30, offset: Int = 0): Result<List<com.example.data.model.PostReactionLiker>> = withContext(Dispatchers.IO) {
        try {
            val res = SupabaseClient.api.getPostLikers("eq.$postId", limit = limit, offset = offset)
            if (res.isSuccessful && res.body() != null) {
                Result.success(res.body()!!)
            } else {
                Result.success(emptyList())
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Comment Likes & Actions
    suspend fun getCommentLikers(commentId: String, limit: Int = 30, offset: Int = 0): Result<List<com.example.data.model.CommentLiker>> = withContext(Dispatchers.IO) {
        try {
            val res = SupabaseClient.api.getCommentLikers("eq.$commentId", limit = limit, offset = offset)
            if (res.isSuccessful && res.body() != null) {
                Result.success(res.body()!!)
            } else {
                Result.success(emptyList())
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun toggleCommentLike(commentId: String): Boolean = withContext(Dispatchers.IO) {
        val user = _currentUser.value ?: return@withContext false
        val wasLiked = likedCommentIds.contains(commentId)
        if (wasLiked) {
            likedCommentIds.remove(commentId)
            try {
                SupabaseClient.api.removeCommentLike("eq.$commentId", "eq.${user.id}")
            } catch (_: Exception) {}
            false
        } else {
            likedCommentIds.add(commentId)
            try {
                SupabaseClient.api.addCommentLike(com.example.data.model.CreateCommentLikeRequest(commentId = commentId, userId = user.id))
            } catch (_: Exception) {}
            true
        }
    }

    fun isCommentLikedByMe(commentId: String): Boolean = likedCommentIds.contains(commentId)

    suspend fun updateComment(commentId: String, content: String): Result<Comment> = withContext(Dispatchers.IO) {
        try {
            val token = SupabaseClient.getAuthToken()?.let { "Bearer $it" }
            val req = com.example.data.model.UpdateCommentRequest(content = content)
            val res = SupabaseClient.api.updateComment("eq.$commentId", req, userToken = token)
            if (res.isSuccessful && !res.body().isNullOrEmpty()) {
                Result.success(res.body()!!.first())
            } else {
                Result.failure(Exception("Failed to edit comment"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteComment(commentId: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val token = SupabaseClient.getAuthToken()?.let { "Bearer $it" }
            val res = SupabaseClient.api.deleteComment("eq.$commentId", userToken = token)
            if (res.isSuccessful) {
                Result.success(true)
            } else {
                Result.failure(Exception("Failed to delete comment"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun sendNotificationRpc(recipientId: String, type: String, postId: String?, commentId: String?) = withContext(Dispatchers.IO) {
        val user = _currentUser.value ?: return@withContext
        if (user.id == recipientId) return@withContext // Do not notify yourself
        try {
            val token = SupabaseClient.getAuthToken()?.let { "Bearer $it" }
            val payload = mapOf(
                "p_recipient_id" to recipientId,
                "p_actor_id" to user.id,
                "p_type" to type,
                "p_post_id" to postId,
                "p_comment_id" to commentId
            )
            SupabaseClient.api.sendNotificationRpc(payload, userToken = token)
        } catch (_: Exception) {}
    }

    // 9. Communities / Groups
    suspend fun getGroups(): Result<List<com.example.data.model.GroupCommunity>> = withContext(Dispatchers.IO) {
        try {
            val res = SupabaseClient.api.getGroups()
            if (res.isSuccessful && res.body() != null) {
                val userId = _currentUser.value?.id
                val groups = res.body()!!
                if (!userId.isNullOrBlank()) {
                    val enriched = groups.map { group ->
                        val memberRes = SupabaseClient.api.getGroupMembers("eq.${group.id}")
                        val isJoined = memberRes.body()?.any { it.userId == userId } == true
                        group.copy(isJoinedByMe = isJoined)
                    }
                    Result.success(enriched)
                } else {
                    Result.success(groups)
                }
            } else {
                Result.success(emptyList())
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createGroup(name: String, description: String, category: String = "general"): Result<com.example.data.model.GroupCommunity> = withContext(Dispatchers.IO) {
        val user = _currentUser.value ?: return@withContext Result.failure(Exception("Must be logged in to create a group"))
        try {
            val slug = name.lowercase().replace(" ", "-").replace("[^a-z0-9-]".toRegex(), "") + "-" + UUID.randomUUID().toString().take(4)
            val req = com.example.data.model.CreateGroupRequest(
                name = name,
                slug = slug,
                description = description,
                category = category,
                creatorId = user.id
            )
            val token = SupabaseClient.getAuthToken()?.let { "Bearer $it" }
            val res = SupabaseClient.api.createGroup(req, userToken = token)
            if (res.isSuccessful && !res.body().isNullOrEmpty()) {
                val newGroup = res.body()!!.first()
                // Auto join creator as admin
                try {
                    SupabaseClient.api.joinGroup(com.example.data.model.JoinGroupRequest(groupId = newGroup.id, userId = user.id, role = "admin"))
                } catch (_: Exception) {}
                Result.success(newGroup.copy(isJoinedByMe = true))
            } else {
                Result.failure(Exception("Failed to create community group"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun joinGroup(groupId: String): Result<Boolean> = withContext(Dispatchers.IO) {
        val userId = _currentUser.value?.id ?: return@withContext Result.failure(Exception("Must be logged in to join group"))
        try {
            val req = com.example.data.model.JoinGroupRequest(groupId = groupId, userId = userId)
            val res = SupabaseClient.api.joinGroup(req)
            if (res.isSuccessful) {
                Result.success(true)
            } else {
                Result.failure(Exception("Failed to join group"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun leaveGroup(groupId: String): Result<Boolean> = withContext(Dispatchers.IO) {
        val userId = _currentUser.value?.id ?: return@withContext Result.failure(Exception("Must be logged in"))
        try {
            val res = SupabaseClient.api.leaveGroup("eq.$groupId", "eq.$userId")
            if (res.isSuccessful) {
                Result.success(true)
            } else {
                Result.failure(Exception("Failed to leave group"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getPostsByGroup(groupId: String): Result<List<Post>> = withContext(Dispatchers.IO) {
        try {
            val res = SupabaseClient.api.getPostsByGroup("eq.$groupId")
            if (res.isSuccessful && res.body() != null) {
                val posts = res.body()!!.map { post ->
                    post.copy(
                        isLikedByMe = likedPostIds.contains(post.id),
                        isBookmarkedByMe = savedPostIds.contains(post.id)
                    )
                }
                Result.success(posts)
            } else {
                Result.success(emptyList())
            }
        } catch (e: Exception) {
            Result.success(emptyList())
        }
    }

    suspend fun getGroupMembers(groupId: String): Result<List<com.example.data.model.GroupMember>> = withContext(Dispatchers.IO) {
        try {
            val res = SupabaseClient.api.getGroupMembers("eq.$groupId")
            if (res.isSuccessful && res.body() != null) {
                Result.success(res.body()!!)
            } else {
                Result.success(emptyList())
            }
        } catch (e: Exception) {
            Result.success(emptyList())
        }
    }

    suspend fun getReels(limit: Int = 20, offset: Int = 0): Result<List<Post>> = withContext(Dispatchers.IO) {
        try {
            val res = SupabaseClient.api.getReels(limit = limit, offset = offset)
            if (res.isSuccessful && !res.body().isNullOrEmpty()) {
                val reels = res.body()!!.mapNotNull { vp ->
                    val parent = vp.parentPost
                    if (parent != null) {
                        parent.copy(
                            videoPosts = listOf(vp),
                            isLikedByMe = likedPostIds.contains(parent.id),
                            isBookmarkedByMe = savedPostIds.contains(parent.id)
                        )
                    } else if (!vp.videoUrl.isNullOrBlank()) {
                        Post(
                            id = vp.postId,
                            userId = "",
                            type = "Video",
                            title = vp.title,
                            content = vp.title ?: "Video Reel",
                            externalUrl = vp.videoUrl,
                            videoPosts = listOf(vp)
                        )
                    } else null
                }
                if (reels.isNotEmpty()) {
                    return@withContext Result.success(reels)
                }
            }

            // Fallback: return any video posts from feed
            val feedRes = getFeedPosts()
            val videoPosts = feedRes.getOrDefault(emptyList()).filter {
                it.videoDetails != null || it.type.equals("Video", ignoreCase = true) ||
                        (it.externalUrl != null && (it.externalUrl.contains("tiktok", true) || it.externalUrl.contains("youtube", true) || it.externalUrl.contains("youtu.be", true)))
            }
            Result.success(videoPosts)
        } catch (e: Exception) {
            // Graceful fallback
            val feedRes = getFeedPosts()
            val videoPosts = feedRes.getOrDefault(emptyList()).filter {
                it.videoDetails != null || it.type.equals("Video", ignoreCase = true)
            }
            Result.success(videoPosts)
        }
    }

    // ==========================================
    // SEARCH & TAGS REPOSITORY METHODS
    // ==========================================

    fun sanitizeSearchQuery(query: String): String {
        return query
            .replace(Regex("[&|!()\\:'\"\\\\]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    suspend fun searchPosts(query: String, limit: Int = 20, offset: Int = 0): Result<List<Post>> = withContext(Dispatchers.IO) {
        try {
            val sanitized = sanitizeSearchQuery(query)
            if (sanitized.isBlank()) return@withContext Result.success(emptyList())

            val rpcRes = SupabaseClient.api.searchPostsRpc(com.example.data.model.SearchRpcRequest(q = sanitized, lim = limit, off = offset))
            if (!rpcRes.isSuccessful || rpcRes.body() == null) {
                return@withContext Result.success(emptyList())
            }

            val idResults = rpcRes.body()!!
            if (idResults.isEmpty()) return@withContext Result.success(emptyList())

            val postIds = idResults.map { it.id }
            val inFilter = "in.(" + postIds.joinToString(",") + ")"
            val postsRes = SupabaseClient.api.getPostsByIds(inFilter)

            if (postsRes.isSuccessful && postsRes.body() != null) {
                val rawPosts = postsRes.body()!!
                val postMap = rawPosts.associateBy { it.id }
                // Preserve RPC ranking order
                val orderedPosts = postIds.mapNotNull { id -> postMap[id] }.map { post ->
                    post.copy(
                        isLikedByMe = likedPostIds.contains(post.id),
                        isBookmarkedByMe = savedPostIds.contains(post.id)
                    )
                }
                Result.success(orderedPosts)
            } else {
                Result.success(emptyList())
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun searchUsers(query: String, limit: Int = 20, offset: Int = 0): Result<List<UserProfile>> = withContext(Dispatchers.IO) {
        try {
            val sanitized = sanitizeSearchQuery(query)
            if (sanitized.isBlank()) return@withContext Result.success(emptyList())

            val rpcRes = SupabaseClient.api.searchUsersRpc(com.example.data.model.SearchRpcRequest(q = sanitized, lim = limit, off = offset))
            if (rpcRes.isSuccessful && rpcRes.body() != null) {
                val users = rpcRes.body()!!
                Result.success(users)
            } else {
                Result.success(emptyList())
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun searchTags(query: String, limit: Int = 20, offset: Int = 0): Result<List<com.example.data.model.Tag>> = withContext(Dispatchers.IO) {
        try {
            val sanitized = sanitizeSearchQuery(query)
            if (sanitized.isBlank()) return@withContext Result.success(emptyList())

            val rpcRes = SupabaseClient.api.searchTagsRpc(com.example.data.model.SearchRpcRequest(q = sanitized, lim = limit, off = offset))
            if (rpcRes.isSuccessful && rpcRes.body() != null) {
                Result.success(rpcRes.body()!!)
            } else {
                Result.success(emptyList())
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getTrendingTags(): Result<List<com.example.data.model.Tag>> = withContext(Dispatchers.IO) {
        try {
            val res = SupabaseClient.api.getTrendingTags()
            if (res.isSuccessful && res.body() != null) {
                Result.success(res.body()!!)
            } else {
                Result.success(emptyList())
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getPostsByTag(tagId: String, tagName: String = "", limit: Int = 20, offset: Int = 0): Result<List<Post>> = withContext(Dispatchers.IO) {
        try {
            var actualTagId = tagId
            if (actualTagId.isBlank() && tagName.isNotBlank()) {
                val tagRes = SupabaseClient.api.getTagByName("eq.${tagName.lowercase().removePrefix("#")}")
                if (tagRes.isSuccessful && !tagRes.body().isNullOrEmpty()) {
                    actualTagId = tagRes.body()!![0].id
                }
            }

            if (actualTagId.isBlank()) {
                return@withContext Result.success(emptyList())
            }

            val embedRes = SupabaseClient.api.getPostIdsByTag(tagIdFilter = "eq.$actualTagId", limit = limit, offset = offset)
            if (!embedRes.isSuccessful || embedRes.body() == null) {
                return@withContext Result.success(emptyList())
            }

            val postIds = embedRes.body()!!.map { it.postId }.distinct()
            if (postIds.isEmpty()) {
                return@withContext Result.success(emptyList())
            }

            val inFilter = "in.(" + postIds.joinToString(",") + ")"
            val postsRes = SupabaseClient.api.getPostsByIds(inFilter)
            if (postsRes.isSuccessful && postsRes.body() != null) {
                val posts = postsRes.body()!!.map { post ->
                    post.copy(
                        isLikedByMe = likedPostIds.contains(post.id),
                        isBookmarkedByMe = savedPostIds.contains(post.id)
                    )
                }
                Result.success(posts)
            } else {
                Result.success(emptyList())
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

