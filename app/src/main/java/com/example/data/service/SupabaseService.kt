package com.example.data.service

import com.example.data.api.SupabaseClient
import com.example.data.model.AuthResponse
import com.example.data.model.AuthSignInRequest
import com.example.data.model.AuthSignUpRequest
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
import org.json.JSONObject

/**
 * Service class handling database and authentication operations with Supabase.
 * Encapsulates Supabase API calls, error parsing, and data mapping.
 */
class SupabaseService(
    private val client: SupabaseClient = SupabaseClient
) {

    companion object {
        @Volatile
        private var instance: SupabaseService? = null

        fun getInstance(): SupabaseService {
            return instance ?: synchronized(this) {
                instance ?: SupabaseService().also { instance = it }
            }
        }
    }

    // -------------------------------------------------------------
    // Authentication Operations
    // -------------------------------------------------------------

    suspend fun signUp(
        email: String,
        pass: String,
        username: String,
        displayName: String
    ): Result<AuthResponse> {
        return try {
            val metadata = mapOf(
                "username" to username.trim(),
                "display_name" to displayName.trim()
            )
            val response = client.api.signUp(
                AuthSignUpRequest(
                    email = email.trim(),
                    password = pass,
                    data = metadata
                )
            )

            if (response.isSuccessful && response.body() != null) {
                val authResponse = response.body()!!
                authResponse.accessToken?.let { token ->
                    client.setAuthToken(token)
                }
                Result.success(authResponse)
            } else {
                val rawError = response.errorBody()?.string()
                Result.failure(Exception(parseErrorMessage(rawError, "Sign up failed")))
            }
        } catch (e: Exception) {
            Result.failure(Exception(e.localizedMessage ?: "Network error during sign up", e))
        }
    }

    suspend fun signIn(email: String, pass: String): Result<AuthResponse> {
        return try {
            val response = client.api.signIn(
                AuthSignInRequest(
                    email = email.trim(),
                    password = pass
                )
            )

            if (response.isSuccessful && response.body() != null) {
                val authResponse = response.body()!!
                authResponse.accessToken?.let { token ->
                    client.setAuthToken(token)
                }
                Result.success(authResponse)
            } else {
                val rawError = response.errorBody()?.string()
                Result.failure(Exception(parseErrorMessage(rawError, "Login failed")))
            }
        } catch (e: Exception) {
            Result.failure(Exception(e.localizedMessage ?: "Network error during login", e))
        }
    }

    fun signOut() {
        client.setAuthToken(null)
    }

    fun setAuthToken(token: String?) {
        client.setAuthToken(token)
    }

    fun getAuthToken(): String? = client.getAuthToken()

    // -------------------------------------------------------------
    // Profile Database Operations (Table: profiles)
    // -------------------------------------------------------------

    suspend fun getProfileById(userId: String): Result<UserProfile?> {
        return try {
            val response = client.api.getProfileById("eq.$userId")
            if (response.isSuccessful) {
                val profile = response.body()?.firstOrNull()
                Result.success(profile)
            } else {
                val rawError = response.errorBody()?.string()
                Result.failure(Exception(parseErrorMessage(rawError, "Failed to fetch profile")))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getAllProfiles(limit: Int = 60): Result<List<UserProfile>> {
        return try {
            val response = client.api.getAllProfiles(limit = limit)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                val rawError = response.errorBody()?.string()
                Result.failure(Exception(parseErrorMessage(rawError, "Failed to load profiles")))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun insertProfile(profile: UserProfile): Result<UserProfile> {
        return try {
            val token = client.getAuthToken()?.let { "Bearer $it" }
            val response = client.api.insertProfile(
                profile = profile,
                userToken = token
            )
            if (response.isSuccessful && !response.body().isNullOrEmpty()) {
                Result.success(response.body()!!.first())
            } else {
                Result.success(profile)
            }
        } catch (e: Exception) {
            Result.success(profile)
        }
    }

    suspend fun updateProfile(userId: String, updates: ProfileUpdateRequest): Result<UserProfile> {
        return try {
            val token = client.getAuthToken()?.let { "Bearer $it" }
            val response = client.api.updateProfile(
                idFilter = "eq.$userId",
                updates = updates,
                userToken = token
            )
            if (response.isSuccessful && !response.body().isNullOrEmpty()) {
                Result.success(response.body()!!.first())
            } else {
                val refreshed = getProfileById(userId).getOrNull()
                if (refreshed != null) {
                    Result.success(refreshed)
                } else {
                    val rawError = response.errorBody()?.string()
                    Result.failure(Exception(parseErrorMessage(rawError, "Failed to update profile")))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // -------------------------------------------------------------
    // Posts Database Operations (Table: posts)
    // -------------------------------------------------------------

    suspend fun getFeedPosts(limit: Int = 50): Result<List<Post>> {
        return try {
            val response = client.api.getPosts(limit = limit)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                val rawError = response.errorBody()?.string()
                Result.failure(Exception(parseErrorMessage(rawError, "Failed to load feed posts")))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getPostsByUser(userId: String, limit: Int = 40): Result<List<Post>> {
        return try {
            val response = client.api.getPostsByUser("eq.$userId", limit = limit)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                val rawError = response.errorBody()?.string()
                Result.failure(Exception(parseErrorMessage(rawError, "Failed to load user posts")))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createPost(request: CreatePostRequest): Result<Post> {
        return try {
            val token = client.getAuthToken()?.let { "Bearer $it" }
            val response = client.api.createPost(request, userToken = token)
            if (response.isSuccessful && !response.body().isNullOrEmpty()) {
                Result.success(response.body()!!.first())
            } else {
                val rawError = response.errorBody()?.string()
                Result.failure(Exception(parseErrorMessage(rawError, "Failed to publish post")))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // -------------------------------------------------------------
    // Post Reactions (Likes) (Table: post_reactions)
    // -------------------------------------------------------------

    suspend fun getUserReactions(userId: String): Result<List<PostReaction>> {
        return try {
            val response = client.api.getUserReactions("eq.$userId")
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.success(emptyList())
            }
        } catch (e: Exception) {
            Result.success(emptyList())
        }
    }

    suspend fun addReaction(postId: String, userId: String): Result<PostReaction?> {
        return try {
            val reaction = PostReaction(
                postId = postId,
                userId = userId,
                reactionType = "appreciate"
            )
            val response = client.api.addReaction(reaction)
            if (response.isSuccessful && !response.body().isNullOrEmpty()) {
                Result.success(response.body()!!.first())
            } else {
                Result.success(reaction)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun removeReaction(postId: String, userId: String): Result<Unit> {
        return try {
            client.api.removeReaction("eq.$postId", "eq.$userId")
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // -------------------------------------------------------------
    // Comments Operations (Table: comments)
    // -------------------------------------------------------------

    suspend fun getComments(postId: String): Result<List<Comment>> {
        return try {
            val response = client.api.getCommentsForPost("eq.$postId")
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Failed to fetch comments"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createComment(postId: String, userId: String, content: String): Result<Comment> {
        return try {
            val token = client.getAuthToken()?.let { "Bearer $it" }
            val request = CreateCommentRequest(
                postId = postId,
                userId = userId,
                content = content
            )
            val response = client.api.createComment(request, userToken = token)
            if (response.isSuccessful && !response.body().isNullOrEmpty()) {
                Result.success(response.body()!!.first())
            } else {
                val rawError = response.errorBody()?.string()
                Result.failure(Exception(parseErrorMessage(rawError, "Failed to submit comment")))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // -------------------------------------------------------------
    // Saved Posts (Bookmarks) (Table: saved_posts)
    // -------------------------------------------------------------

    suspend fun getSavedPosts(userId: String): Result<List<SavedPost>> {
        return try {
            val response = client.api.getSavedPosts("eq.$userId")
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.success(emptyList())
            }
        } catch (e: Exception) {
            Result.success(emptyList())
        }
    }

    suspend fun savePost(postId: String, userId: String): Result<SavedPost?> {
        return try {
            val request = CreateSavedPostRequest(postId = postId, userId = userId)
            val response = client.api.savePost(request)
            if (response.isSuccessful && !response.body().isNullOrEmpty()) {
                Result.success(response.body()!!.first())
            } else {
                Result.success(SavedPost(postId = postId, userId = userId))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun unsavePost(postId: String, userId: String): Result<Unit> {
        return try {
            client.api.unsavePost("eq.$postId", "eq.$userId")
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // -------------------------------------------------------------
    // Social Graph: Follows (Table: follows)
    // -------------------------------------------------------------

    suspend fun getFollowers(userId: String): Result<List<Follow>> {
        return try {
            val response = client.api.getFollowers("eq.$userId")
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.success(emptyList())
            }
        } catch (e: Exception) {
            Result.success(emptyList())
        }
    }

    suspend fun getFollowing(userId: String): Result<List<Follow>> {
        return try {
            val response = client.api.getFollowing("eq.$userId")
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.success(emptyList())
            }
        } catch (e: Exception) {
            Result.success(emptyList())
        }
    }

    suspend fun followUser(followerId: String, followingId: String): Result<Follow?> {
        return try {
            val request = CreateFollowRequest(followerId = followerId, followingId = followingId)
            val response = client.api.followUser(request)
            if (response.isSuccessful && !response.body().isNullOrEmpty()) {
                Result.success(response.body()!!.first())
            } else {
                Result.success(Follow(followerId = followerId, followingId = followingId))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun unfollowUser(followerId: String, followingId: String): Result<Unit> {
        return try {
            client.api.unfollowUser("eq.$followerId", "eq.$followingId")
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // -------------------------------------------------------------
    // Notifications (Table: notifications)
    // -------------------------------------------------------------

    suspend fun getNotifications(recipientId: String, limit: Int = 30): Result<List<NotificationItem>> {
        return try {
            val response = client.api.getNotifications("eq.$recipientId", limit = limit)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.success(emptyList())
            }
        } catch (e: Exception) {
            Result.success(emptyList())
        }
    }

    // -------------------------------------------------------------
    // Error Parser Helper
    // -------------------------------------------------------------

    private fun parseErrorMessage(rawError: String?, defaultMessage: String): String {
        if (rawError.isNullOrBlank()) return defaultMessage
        return try {
            val json = JSONObject(rawError)
            val message = json.optString("error_description").takeIf { it.isNotBlank() }
                ?: json.optString("msg").takeIf { it.isNotBlank() }
                ?: json.optString("message").takeIf { it.isNotBlank() }
                ?: json.optString("error").takeIf { it.isNotBlank() }

            if (!message.isNullOrBlank()) {
                when {
                    message.contains("Invalid login credentials", ignoreCase = true) ->
                        "Invalid email or password. Please verify your credentials."
                    message.contains("already registered", ignoreCase = true) ->
                        "This email is already registered. Please log in instead."
                    message.contains("at least 6 characters", ignoreCase = true) ->
                        "Password must be at least 6 characters."
                    message.contains("Email not confirmed", ignoreCase = true) ->
                        "Email not confirmed yet. Please check your inbox or try signing in."
                    message.contains("rate limit", ignoreCase = true) ->
                        "Too many attempts. Please wait a moment and try again."
                    else -> message
                }
            } else {
                defaultMessage
            }
        } catch (_: Exception) {
            if (rawError.length < 120 && !rawError.startsWith("{") && !rawError.startsWith("<")) {
                rawError
            } else {
                defaultMessage
            }
        }
    }
}
