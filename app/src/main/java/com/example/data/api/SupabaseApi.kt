package com.example.data.api

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
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Query

interface SupabaseApi {

    // Auth (auth.users)
    @POST("auth/v1/signup")
    suspend fun signUp(
        @Body request: AuthSignUpRequest
    ): Response<AuthResponse>

    @POST("auth/v1/token?grant_type=password")
    suspend fun signIn(
        @Body request: AuthSignInRequest
    ): Response<AuthResponse>

    // 1. Table: profiles
    @GET("rest/v1/profiles?select=*")
    suspend fun getAllProfiles(
        @Query("limit") limit: Int = 50
    ): Response<List<UserProfile>>

    @GET("rest/v1/profiles?select=*")
    suspend fun getProfileById(
        @Query("id") idFilter: String
    ): Response<List<UserProfile>>

    @PATCH("rest/v1/profiles")
    suspend fun updateProfile(
        @Query("id") idFilter: String,
        @Body updates: ProfileUpdateRequest,
        @Header("Prefer") prefer: String = "return=representation",
        @Header("Authorization") userToken: String? = null
    ): Response<List<UserProfile>>

    @POST("rest/v1/profiles")
    suspend fun insertProfile(
        @Body profile: UserProfile,
        @Header("Prefer") prefer: String = "return=representation",
        @Header("Authorization") userToken: String? = null
    ): Response<List<UserProfile>>

    // 2. Table: posts
    @GET("rest/v1/posts?select=*,profiles:user_id(*)&order=created_at.desc")
    suspend fun getPosts(
        @Query("limit") limit: Int = 50
    ): Response<List<Post>>

    @GET("rest/v1/posts?select=*,profiles:user_id(*)&order=created_at.desc")
    suspend fun getPostsByUser(
        @Query("user_id") userIdFilter: String,
        @Query("limit") limit: Int = 40
    ): Response<List<Post>>

    @POST("rest/v1/posts")
    suspend fun createPost(
        @Body post: CreatePostRequest,
        @Header("Prefer") prefer: String = "return=representation",
        @Header("Authorization") userToken: String? = null
    ): Response<List<Post>>

    // 3. Table: post_reactions (Likes / Appreciations)
    // Note: Database trigger trg_post_reaction_count handles posts.appreciations_count
    @GET("rest/v1/post_reactions?select=*")
    suspend fun getUserReactions(
        @Query("user_id") userIdFilter: String
    ): Response<List<PostReaction>>

    @POST("rest/v1/post_reactions")
    suspend fun addReaction(
        @Body reaction: PostReaction,
        @Header("Prefer") prefer: String = "return=representation"
    ): Response<List<PostReaction>>

    @DELETE("rest/v1/post_reactions")
    suspend fun removeReaction(
        @Query("post_id") postIdFilter: String,
        @Query("user_id") userIdFilter: String
    ): Response<Unit>

    // 4. Table: comments (Threaded replies)
    // Note: Database trigger automatically updates posts.replies_count
    @GET("rest/v1/comments?select=*,profiles:user_id(*)&order=created_at.asc")
    suspend fun getCommentsForPost(
        @Query("post_id") postIdFilter: String
    ): Response<List<Comment>>

    @POST("rest/v1/comments")
    suspend fun createComment(
        @Body comment: CreateCommentRequest,
        @Header("Prefer") prefer: String = "return=representation",
        @Header("Authorization") userToken: String? = null
    ): Response<List<Comment>>

    // 5. Table: saved_posts (Private bookmarks)
    @GET("rest/v1/saved_posts?select=*,posts(*,profiles:user_id(*))&order=created_at.desc")
    suspend fun getSavedPosts(
        @Query("user_id") userIdFilter: String
    ): Response<List<SavedPost>>

    @POST("rest/v1/saved_posts")
    suspend fun savePost(
        @Body request: CreateSavedPostRequest,
        @Header("Prefer") prefer: String = "return=representation"
    ): Response<List<SavedPost>>

    @DELETE("rest/v1/saved_posts")
    suspend fun unsavePost(
        @Query("post_id") postIdFilter: String,
        @Query("user_id") userIdFilter: String
    ): Response<Unit>

    // 6. Table: follows (Social graph: Trackers & Tracking)
    @GET("rest/v1/follows?select=*")
    suspend fun getFollowing(
        @Query("follower_id") followerIdFilter: String
    ): Response<List<Follow>>

    @GET("rest/v1/follows?select=*")
    suspend fun getFollowers(
        @Query("following_id") followingIdFilter: String
    ): Response<List<Follow>>

    @POST("rest/v1/follows")
    suspend fun followUser(
        @Body follow: CreateFollowRequest,
        @Header("Prefer") prefer: String = "return=representation"
    ): Response<List<Follow>>

    @DELETE("rest/v1/follows")
    suspend fun unfollowUser(
        @Query("follower_id") followerIdFilter: String,
        @Query("following_id") followingIdFilter: String
    ): Response<Unit>

    // 7. Table: notifications
    @GET("rest/v1/notifications?select=*,actor:actor_id(*)&order=created_at.desc")
    suspend fun getNotifications(
        @Query("recipient_id") recipientIdFilter: String,
        @Query("limit") limit: Int = 30
    ): Response<List<NotificationItem>>
}
