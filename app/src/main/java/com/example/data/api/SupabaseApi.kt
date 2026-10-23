package com.example.data.api

import com.example.data.model.CreatePhotoPostRequest
import com.example.data.model.CreateTextPostRequest
import com.example.data.model.CreateVideoPostRequest
import com.example.data.model.PhotoPost
import com.example.data.model.TextPost
import com.example.data.model.VideoPost
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

    // 2. Table: posts & sub-tables (video_posts, photo_posts, text_posts)
    @GET("rest/v1/posts?select=*,profiles:user_id(*),video_posts(*),photo_posts(*),text_posts(*)&order=created_at.desc")
    suspend fun getPosts(
        @Query("limit") limit: Int = 50
    ): Response<List<Post>>

    @GET("rest/v1/posts?select=*,profiles:user_id(*)&order=created_at.desc")
    suspend fun getPostsLegacy(
        @Query("limit") limit: Int = 50
    ): Response<List<Post>>

    @GET("rest/v1/posts?select=*,profiles:user_id(*),video_posts(*),photo_posts(*),text_posts(*)&order=created_at.desc")
    suspend fun getPostsByUser(
        @Query("user_id") userIdFilter: String,
        @Query("limit") limit: Int = 40
    ): Response<List<Post>>

    @GET("rest/v1/posts?select=*,profiles:user_id(*),video_posts(*),photo_posts(*),text_posts(*)&order=created_at.desc")
    suspend fun getPostsByGroup(
        @Query("group_id") groupIdFilter: String,
        @Query("limit") limit: Int = 40
    ): Response<List<Post>>

    @POST("rest/v1/posts")
    suspend fun createPost(
        @Body post: CreatePostRequest,
        @Header("Prefer") prefer: String = "return=representation",
        @Header("Authorization") userToken: String? = null
    ): Response<List<Post>>

    @POST("rest/v1/video_posts")
    suspend fun createVideoPost(
        @Body request: CreateVideoPostRequest,
        @Header("Prefer") prefer: String = "return=representation",
        @Header("Authorization") userToken: String? = null
    ): Response<List<VideoPost>>

    @GET("rest/v1/video_posts?select=*,posts!inner(id,user_id,title,content,appreciations_count,replies_count,relays_count,group_id,created_at,profiles:user_id(*))&or=(direct_url.not.is.null,platform.eq.tiktok)&order=created_at.desc")
    suspend fun getReels(
        @Query("limit") limit: Int = 10,
        @Query("offset") offset: Int = 0
    ): Response<List<VideoPost>>

    @PATCH("rest/v1/video_posts")
    suspend fun updateVideoPostDirectUrl(
        @Query("id") idFilter: String,
        @Body updates: Map<String, String>,
        @Header("Prefer") prefer: String = "return=representation"
    ): Response<List<VideoPost>>

    @POST("rest/v1/photo_posts")
    suspend fun createPhotoPost(
        @Body request: CreatePhotoPostRequest,
        @Header("Prefer") prefer: String = "return=representation",
        @Header("Authorization") userToken: String? = null
    ): Response<List<PhotoPost>>

    @POST("rest/v1/text_posts")
    suspend fun createTextPost(
        @Body request: CreateTextPostRequest,
        @Header("Prefer") prefer: String = "return=representation",
        @Header("Authorization") userToken: String? = null
    ): Response<List<TextPost>>

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

    @GET("rest/v1/post_reactions?select=user_id,created_at,profiles:user_id(*)&order=created_at.desc")
    suspend fun getPostLikers(
        @Query("post_id") postIdFilter: String,
        @Query("limit") limit: Int = 30,
        @Query("offset") offset: Int = 0
    ): Response<List<com.example.data.model.PostReactionLiker>>

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

    @PATCH("rest/v1/comments")
    suspend fun updateComment(
        @Query("id") idFilter: String,
        @Body updates: com.example.data.model.UpdateCommentRequest,
        @Header("Prefer") prefer: String = "return=representation",
        @Header("Authorization") userToken: String? = null
    ): Response<List<Comment>>

    @DELETE("rest/v1/comments")
    suspend fun deleteComment(
        @Query("id") idFilter: String,
        @Header("Authorization") userToken: String? = null
    ): Response<Unit>

    // Reports & Hidden Posts
    @POST("rest/v1/reports")
    suspend fun createReport(
        @Body report: com.example.data.model.CreateReportRequest,
        @Header("Prefer") prefer: String = "return=representation"
    ): Response<Unit>

    @POST("rest/v1/hidden_posts")
    suspend fun hidePost(
        @Body request: com.example.data.model.CreateHiddenPostRequest,
        @Header("Prefer") prefer: String = "return=representation"
    ): Response<Unit>

    @DELETE("rest/v1/hidden_posts")
    suspend fun unhidePost(
        @Query("user_id") userIdFilter: String,
        @Query("post_id") postIdFilter: String
    ): Response<Unit>

    @GET("rest/v1/hidden_posts?select=post_id")
    suspend fun getHiddenPostIds(
        @Query("user_id") userIdFilter: String
    ): Response<List<Map<String, String>>>

    // Comment Likes
    @GET("rest/v1/comment_likes?select=*")
    suspend fun getUserCommentLikes(
        @Query("user_id") userIdFilter: String
    ): Response<List<com.example.data.model.CommentLike>>

    @POST("rest/v1/comment_likes")
    suspend fun addCommentLike(
        @Body like: com.example.data.model.CreateCommentLikeRequest,
        @Header("Prefer") prefer: String = "return=representation"
    ): Response<Unit>

    @DELETE("rest/v1/comment_likes")
    suspend fun removeCommentLike(
        @Query("comment_id") commentIdFilter: String,
        @Query("user_id") userIdFilter: String
    ): Response<Unit>

    @GET("rest/v1/comment_likes?select=user_id,created_at,profiles:user_id(*)&order=created_at.desc")
    suspend fun getCommentLikers(
        @Query("comment_id") commentIdFilter: String,
        @Query("limit") limit: Int = 30,
        @Query("offset") offset: Int = 0
    ): Response<List<com.example.data.model.CommentLiker>>

    // RPC: Notifications
    @POST("rest/v1/rpc/send_notification")
    suspend fun sendNotificationRpc(
        @Body payload: Map<String, Any?>,
        @Header("Authorization") userToken: String? = null
    ): Response<Unit>

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

    // 8. Post Edit & Delete
    @PATCH("rest/v1/posts")
    suspend fun updatePost(
        @Query("id") idFilter: String,
        @Body updates: com.example.data.model.UpdatePostRequest,
        @Header("Prefer") prefer: String = "return=representation",
        @Header("Authorization") userToken: String? = null
    ): Response<List<Post>>

    @DELETE("rest/v1/posts")
    suspend fun deletePost(
        @Query("id") idFilter: String,
        @Header("Authorization") userToken: String? = null
    ): Response<Unit>

    // 9. Table: groups & group_members (Communities)
    @GET("rest/v1/groups?select=*&order=members_count.desc")
    suspend fun getGroups(
        @Query("limit") limit: Int = 30
    ): Response<List<com.example.data.model.GroupCommunity>>

    @GET("rest/v1/groups?select=*")
    suspend fun getGroupById(
        @Query("id") idFilter: String
    ): Response<List<com.example.data.model.GroupCommunity>>

    @POST("rest/v1/groups")
    suspend fun createGroup(
        @Body group: com.example.data.model.CreateGroupRequest,
        @Header("Prefer") prefer: String = "return=representation",
        @Header("Authorization") userToken: String? = null
    ): Response<List<com.example.data.model.GroupCommunity>>

    @GET("rest/v1/group_members?select=*")
    suspend fun getGroupMembers(
        @Query("group_id") groupIdFilter: String
    ): Response<List<com.example.data.model.GroupMember>>

    @POST("rest/v1/group_members")
    suspend fun joinGroup(
        @Body request: com.example.data.model.JoinGroupRequest,
        @Header("Prefer") prefer: String = "return=representation"
    ): Response<List<com.example.data.model.GroupMember>>

    @DELETE("rest/v1/group_members")
    suspend fun leaveGroup(
        @Query("group_id") groupIdFilter: String,
        @Query("user_id") userIdFilter: String
    ): Response<Unit>

    // 10. Search & Tag Endpoints
    @POST("rest/v1/rpc/search_posts")
    suspend fun searchPostsRpc(
        @Body request: com.example.data.model.SearchRpcRequest
    ): Response<List<com.example.data.model.PostSearchIdResult>>

    @POST("rest/v1/rpc/search_users")
    suspend fun searchUsersRpc(
        @Body request: com.example.data.model.SearchRpcRequest
    ): Response<List<UserProfile>>

    @POST("rest/v1/rpc/search_tags")
    suspend fun searchTagsRpc(
        @Body request: com.example.data.model.SearchRpcRequest
    ): Response<List<com.example.data.model.Tag>>

    @GET("rest/v1/tags?select=*&order=posts_count.desc&limit=15")
    suspend fun getTrendingTags(): Response<List<com.example.data.model.Tag>>

    @GET("rest/v1/tags?select=*")
    suspend fun getTagByName(
        @Query("name") nameFilter: String
    ): Response<List<com.example.data.model.Tag>>

    @GET("rest/v1/posts?select=*,profiles:user_id(*),video_posts(*),photo_posts(*),text_posts(*)")
    suspend fun getPostsByIds(
        @Query("id") idInFilter: String
    ): Response<List<Post>>

    @GET("rest/v1/post_tags?select=post_id&order=created_at.desc")
    suspend fun getPostIdsByTag(
        @Query("tag_id") tagIdFilter: String,
        @Query("limit") limit: Int = 20,
        @Query("offset") offset: Int = 0
    ): Response<List<com.example.data.model.PostTagEmbed>>
}
