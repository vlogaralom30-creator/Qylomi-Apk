package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.analytics.UserEventTracker
import com.example.data.api.SupabaseClient
import com.example.data.model.FeedRankItem
import com.example.data.model.GetFeedRpcRequest
import com.example.data.model.Post
import com.example.data.model.Tag
import com.example.util.FeedPresets
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class FeedRepository(
    private val context: Context,
    private val client: SupabaseClient = SupabaseClient,
    private val socialRepository: SocialRepository = SocialRepository(context)
) {
    companion object {
        private const val TAG = "FeedRepository"
    }

    /**
     * Fetches ranked page from Postgres `get_feed(p_limit, p_exclude)` RPC.
     * Restores server order, hydrates user interactions, and falls back to chronological feed on failure.
     */
    suspend fun getRankedPage(
        shownIds: Set<String>,
        pageSize: Int = 20
    ): Result<List<Post>> = withContext(Dispatchers.IO) {
        val clampedSize = pageSize.coerceIn(1, 50)
        // Send exclusion list capped at the newest 400 IDs
        val excludeList = shownIds.toList().takeLast(400)

        try {
            val rpcResponse = client.api.getFeedRpc(
                GetFeedRpcRequest(limit = clampedSize, exclude = excludeList)
            )

            if (rpcResponse.isSuccessful) {
                val rankItems = rpcResponse.body() ?: emptyList()
                if (rankItems.isEmpty()) {
                    return@withContext Result.success(emptyList())
                }

                val rankIds = rankItems.map { it.postId }.filter { it.isNotBlank() }
                if (rankIds.isEmpty()) {
                    return@withContext Result.success(emptyList())
                }

                val inFilter = "in.(${rankIds.joinToString(",")})"
                val postsResponse = client.api.getPostsByIds(inFilter)

                if (postsResponse.isSuccessful && postsResponse.body() != null) {
                    val postsMap = postsResponse.body()!!.associateBy { it.id }
                    val relayedIds = socialRepository.currentUser.value?.id?.let {
                        RelayRepository { socialRepository.currentUser.value }.getRelayedPostIds(it).toSet()
                    } ?: emptySet()

                    // Restore exact server rank order and drop missing IDs
                    val orderedPosts = rankItems.mapIndexedNotNull { index, item ->
                        val post = postsMap[item.postId] ?: return@mapIndexedNotNull null
                        if (socialRepository.isPostHidden(post.id)) return@mapIndexedNotNull null

                        post.copy(
                            isLikedByMe = socialRepository.isPostLiked(post.id),
                            isBookmarkedByMe = socialRepository.isPostSaved(post.id),
                            isRelayedByMe = relayedIds.contains(post.id),
                            rankScore = item.score,
                            rankPosition = index + 1,
                            isAlreadySeen = UserEventTracker.isImpressionSeen(post.id)
                        )
                    }

                    return@withContext Result.success(orderedPosts)
                } else {
                    Log.w(TAG, "getPostsByIds failed: HTTP ${postsResponse.code()}, falling back to chronological feed")
                    fallbackToChronological()
                }
            } else {
                Log.w(TAG, "get_feed RPC failed: HTTP ${rpcResponse.code()}, falling back to chronological feed")
                fallbackToChronological()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Exception calling get_feed RPC: ${e.message}, falling back to chronological feed", e)
            fallbackToChronological()
        }
    }

    private suspend fun fallbackToChronological(): Result<List<Post>> {
        val fallback = socialRepository.getFeedPosts()
        return if (fallback.isSuccess) {
            fallback
        } else {
            fallback
        }
    }

    /**
     * Fetches chronological page with limit & offset.
     */
    suspend fun getChronologicalPage(
        offset: Int,
        pageSize: Int = 20
    ): Result<List<Post>> = withContext(Dispatchers.IO) {
        try {
            val response = client.api.getPosts(limit = pageSize, offset = offset)
            if (response.isSuccessful && response.body() != null) {
                val relayedIds = socialRepository.currentUser.value?.id?.let {
                    RelayRepository { socialRepository.currentUser.value }.getRelayedPostIds(it).toSet()
                } ?: emptySet()

                val posts = response.body()!!
                    .filter { !socialRepository.isPostHidden(it.id) }
                    .map { post ->
                        post.copy(
                            isLikedByMe = socialRepository.isPostLiked(post.id),
                            isBookmarkedByMe = socialRepository.isPostSaved(post.id),
                            isRelayedByMe = relayedIds.contains(post.id),
                            isAlreadySeen = UserEventTracker.isImpressionSeen(post.id)
                        )
                    }
                Result.success(posts)
            } else {
                Result.failure(Exception("HTTP ${response.code()}: ${response.errorBody()?.string()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Fetches posts from followed creators only.
     */
    suspend fun getFollowingPage(
        myUserId: String,
        offset: Int,
        pageSize: Int = 20
    ): Result<List<Post>> = withContext(Dispatchers.IO) {
        try {
            val followsRes = client.api.getFollowing("eq.$myUserId")
            if (!followsRes.isSuccessful || followsRes.body() == null) {
                return@withContext Result.success(emptyList())
            }

            val followingIds = followsRes.body()!!.map { it.followingId }.distinct()
            if (followingIds.isEmpty()) {
                return@withContext Result.success(emptyList())
            }

            val inFilter = "in.(${followingIds.joinToString(",")})"
            val postsRes = client.api.getPostsByUserIn(inFilter, limit = pageSize, offset = offset)
            if (postsRes.isSuccessful && postsRes.body() != null) {
                val relayedIds = RelayRepository { socialRepository.currentUser.value }.getRelayedPostIds(myUserId).toSet()

                val posts = postsRes.body()!!
                    .filter { !socialRepository.isPostHidden(it.id) }
                    .map { post ->
                        post.copy(
                            isLikedByMe = socialRepository.isPostLiked(post.id),
                            isBookmarkedByMe = socialRepository.isPostSaved(post.id),
                            isRelayedByMe = relayedIds.contains(post.id),
                            isAlreadySeen = UserEventTracker.isImpressionSeen(post.id)
                        )
                    }
                Result.success(posts)
            } else {
                Result.failure(Exception("HTTP ${postsRes.code()}: ${postsRes.errorBody()?.string()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Fetches candidates pool for preset filters (Trending, Videos, Bangla).
     */
    suspend fun getPoolPosts(
        limit: Int = 100,
        offset: Int = 0
    ): Result<List<Post>> = withContext(Dispatchers.IO) {
        try {
            val response = client.api.getPosts(limit = limit, offset = offset)
            if (response.isSuccessful && response.body() != null) {
                val relayedIds = socialRepository.currentUser.value?.id?.let {
                    RelayRepository { socialRepository.currentUser.value }.getRelayedPostIds(it).toSet()
                } ?: emptySet()

                val posts = response.body()!!
                    .filter { !socialRepository.isPostHidden(it.id) }
                    .map { post ->
                        post.copy(
                            isLikedByMe = socialRepository.isPostLiked(post.id),
                            isBookmarkedByMe = socialRepository.isPostSaved(post.id),
                            isRelayedByMe = relayedIds.contains(post.id),
                            isAlreadySeen = UserEventTracker.isImpressionSeen(post.id)
                        )
                    }
                Result.success(posts)
            } else {
                Result.failure(Exception("HTTP ${response.code()}: ${response.errorBody()?.string()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Part 4: Ensures post tags are saved into `tags` and `post_tags` tables with lowercase trimmed names.
     */
    suspend fun syncPostTags(
        postId: String,
        tags: List<String>,
        title: String? = null,
        content: String? = null
    ) = withContext(Dispatchers.IO) {
        if (postId.isBlank()) return@withContext
        try {
            val combined = (tags + FeedPresets.extractHashtags(title ?: "") + FeedPresets.extractHashtags(content ?: ""))
                .map { it.lowercase().trim().removePrefix("#") }
                .filter { it.isNotBlank() }
                .distinct()

            for (tagText in combined) {
                try {
                    var tagId: String? = null
                    val existing = client.api.getTagByName("eq.$tagText")
                    if (existing.isSuccessful && !existing.body().isNullOrEmpty()) {
                        tagId = existing.body()!!.first().id
                    } else {
                        val createRes = client.api.insertTag(mapOf("name" to tagText))
                        if (createRes.isSuccessful && !createRes.body().isNullOrEmpty()) {
                            tagId = createRes.body()!!.first().id
                        }
                    }

                    if (!tagId.isNullOrBlank()) {
                        client.api.insertPostTag(mapOf("post_id" to postId, "tag_id" to tagId))
                    }
                } catch (_: Exception) {}
            }
        } catch (_: Exception) {}
    }
}
