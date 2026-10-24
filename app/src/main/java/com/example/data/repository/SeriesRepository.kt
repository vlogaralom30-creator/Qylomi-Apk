package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.data.api.SupabaseClient
import com.example.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Response
import java.text.Normalizer
import java.util.Locale
import java.util.UUID

class SeriesRepository(
    context: Context,
    private val client: SupabaseClient = SupabaseClient
) {
    private val seriesPrefs: SharedPreferences =
        context.getSharedPreferences("q_series_prefs", Context.MODE_PRIVATE)

    private val socialPrefs: SharedPreferences =
        context.getSharedPreferences("q_social_prefs", Context.MODE_PRIVATE)

    val currentUserId: String?
        get() = socialPrefs.getString("saved_user_id", null)?.takeIf { it.isNotBlank() }

    val authToken: String?
        get() = client.getAuthToken() ?: socialPrefs.getString("saved_token", null)?.takeIf { it.isNotBlank() }

    val userTokenHeader: String?
        get() = authToken?.let { "Bearer $it" }

    val guestViewerKey: String
        get() {
            var key = seriesPrefs.getString("guest_viewer_key", null)
            if (key.isNullOrBlank()) {
                key = UUID.randomUUID().toString()
                seriesPrefs.edit().putString("guest_viewer_key", key).apply()
            }
            return key
        }

    fun isUserLoggedIn(): Boolean = !currentUserId.isNullOrBlank()

    suspend fun getWatchlist(): SeriesResult<List<SeriesCard>> = withContext(Dispatchers.IO) {
        val uid = currentUserId ?: return@withContext SeriesResult.success(emptyList())
        try {
            val res = client.api.getWatchlist("eq.$uid", userToken = userTokenHeader)
            if (res.isSuccessful) {
                val list = res.body() ?: emptyList()
                val cards = list.mapNotNull { it.series }
                SeriesResult.success(cards)
            } else {
                parseError(res)
            }
        } catch (e: Exception) {
            SeriesResult.failure(e.localizedMessage ?: "Failed to load watchlist", cause = e)
        }
    }

    suspend fun isInWatchlist(seriesId: String): SeriesResult<Boolean> = withContext(Dispatchers.IO) {
        val uid = currentUserId ?: return@withContext SeriesResult.success(false)
        try {
            val res = client.api.getWatchlist("eq.$uid", userToken = userTokenHeader)
            if (res.isSuccessful) {
                val list = res.body() ?: emptyList()
                SeriesResult.success(list.any { it.seriesId == seriesId })
            } else {
                SeriesResult.success(false)
            }
        } catch (_: Exception) {
            SeriesResult.success(false)
        }
    }

    suspend fun getUserRating(seriesId: String): SeriesResult<SeriesRating?> = withContext(Dispatchers.IO) {
        val uid = currentUserId ?: return@withContext SeriesResult.success(null)
        try {
            val res = client.api.getMyRating("eq.$seriesId", "eq.$uid")
            if (res.isSuccessful) {
                SeriesResult.success(res.body()?.firstOrNull())
            } else {
                SeriesResult.success(null)
            }
        } catch (_: Exception) {
            SeriesResult.success(null)
        }
    }

    suspend fun getRelatedSeries(seriesId: String, limit: Int = 12): SeriesResult<List<SeriesCard>> =
        getRelated(seriesId, limit)

    // -------------------------------------------------------------------------
    // Helper: Slug Generator
    // -------------------------------------------------------------------------
    fun generateSlug(input: String): String = Companion.generateSlug(input)

    companion object {
        fun generateSlug(input: String): String {
            val normalized = Normalizer.normalize(input, Normalizer.Form.NFD)
                .replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "")
                .lowercase(Locale.ROOT)
            val slug = normalized.replace("[^a-z0-9]+".toRegex(), "-")
                .replace("-+".toRegex(), "-")
                .trim('-')
            return if (slug.isBlank()) "series-${System.currentTimeMillis() % 100000}" else slug
        }
    }

    // -------------------------------------------------------------------------
    // Response & Error Parsing Helper
    // -------------------------------------------------------------------------
    private fun <T> parseError(response: Response<*>): SeriesResult<T> {
        val raw = try {
            response.errorBody()?.string()
        } catch (_: Exception) {
            null
        }
        return SeriesResult.parseError(raw, response.code())
    }

    // =========================================================================
    // 1. READ OPERATIONS (Public & Authenticated)
    // =========================================================================

    suspend fun getCategories(activeOnly: Boolean = true): SeriesResult<List<ContentCategory>> =
        withContext(Dispatchers.IO) {
            try {
                val filter = if (activeOnly) "eq.true" else null
                val response = client.api.getCategories(isActiveFilter = filter)
                if (response.isSuccessful) {
                    SeriesResult.success(response.body() ?: emptyList())
                } else {
                    parseError(response)
                }
            } catch (e: Exception) {
                SeriesResult.failure(e.localizedMessage ?: "Network error loading categories", cause = e)
            }
        }

    suspend fun getHome(category: String? = null): SeriesResult<List<ShelfWithItems>> =
        withContext(Dispatchers.IO) {
            try {
                val response = client.api.getSeriesHome(GetSeriesHomeRpcRequest(pCategory = category))
                if (response.isSuccessful) {
                    SeriesResult.success(response.body() ?: emptyList())
                } else {
                    parseError(response)
                }
            } catch (e: Exception) {
                SeriesResult.failure(e.localizedMessage ?: "Network error loading home shelves", cause = e)
            }
        }

    suspend fun searchSeries(
        q: String,
        category: String? = null,
        limit: Int = 20,
        offset: Int = 0
    ): SeriesResult<List<SeriesCard>> = withContext(Dispatchers.IO) {
        if (q.trim().length < 2) return@withContext SeriesResult.success(emptyList())
        try {
            val response = client.api.searchSeriesRpc(
                SearchSeriesRpcRequest(
                    q = q.trim(),
                    pCategory = category,
                    lim = limit,
                    off = offset
                )
            )
            if (response.isSuccessful) {
                SeriesResult.success(response.body() ?: emptyList())
            } else {
                parseError(response)
            }
        } catch (e: Exception) {
            SeriesResult.failure(e.localizedMessage ?: "Search failed", cause = e)
        }
    }

    suspend fun getSeriesList(
        category: String? = null,
        genre: String? = null,
        language: String? = null,
        status: String? = null,
        sort: String? = null,
        limit: Int = 24,
        offset: Int = 0
    ): SeriesResult<List<SeriesCard>> = withContext(Dispatchers.IO) {
        try {
            val filters = mutableMapOf<String, String>()
            if (!category.isNullOrBlank()) filters["category_id"] = "eq.$category"
            if (!genre.isNullOrBlank()) filters["genres"] = "cs.{$genre}"
            if (!language.isNullOrBlank()) filters["languages"] = "cs.{$language}"
            if (!status.isNullOrBlank()) filters["status"] = "eq.$status"
            filters["is_published"] = "eq.true"
            filters["is_blocked"] = "eq.false"
            filters["order"] = sort ?: "created_at.desc"
            filters["limit"] = limit.toString()
            filters["offset"] = offset.toString()

            val response = client.api.getSeriesList(filters)
            if (response.isSuccessful) {
                val list = response.body() ?: emptyList()
                SeriesResult.success(list.map { it.toCard() })
            } else {
                parseError(response)
            }
        } catch (e: Exception) {
            SeriesResult.failure(e.localizedMessage ?: "Failed to load series catalog", cause = e)
        }
    }

    suspend fun getSeriesBySlug(slug: String): SeriesResult<Series?> =
        withContext(Dispatchers.IO) {
            try {
                val response = client.api.getSeriesBySlug("eq.$slug")
                if (response.isSuccessful) {
                    SeriesResult.success(response.body()?.firstOrNull())
                } else {
                    parseError(response)
                }
            } catch (e: Exception) {
                SeriesResult.failure(e.localizedMessage ?: "Failed to load series", cause = e)
            }
        }

    suspend fun getSeriesById(id: String): SeriesResult<Series?> =
        withContext(Dispatchers.IO) {
            try {
                val response = client.api.getSeriesById("eq.$id")
                if (response.isSuccessful) {
                    SeriesResult.success(response.body()?.firstOrNull())
                } else {
                    parseError(response)
                }
            } catch (e: Exception) {
                SeriesResult.failure(e.localizedMessage ?: "Failed to load series", cause = e)
            }
        }

    suspend fun getSeasons(seriesId: String): SeriesResult<List<Season>> =
        withContext(Dispatchers.IO) {
            try {
                val response = client.api.getSeasons("eq.$seriesId")
                if (response.isSuccessful) {
                    SeriesResult.success(response.body() ?: emptyList())
                } else {
                    parseError(response)
                }
            } catch (e: Exception) {
                SeriesResult.failure(e.localizedMessage ?: "Failed to load seasons", cause = e)
            }
        }

    suspend fun getEpisodes(seriesId: String, publishedOnly: Boolean = true): SeriesResult<List<Episode>> =
        withContext(Dispatchers.IO) {
            try {
                val pubFilter = if (publishedOnly) "eq.true" else null
                val response = client.api.getEpisodes(
                    seriesIdFilter = "eq.$seriesId",
                    isPublishedFilter = pubFilter
                )
                if (response.isSuccessful) {
                    SeriesResult.success(response.body() ?: emptyList())
                } else {
                    parseError(response)
                }
            } catch (e: Exception) {
                SeriesResult.failure(e.localizedMessage ?: "Failed to load episodes", cause = e)
            }
        }

    suspend fun getSources(episodeId: String, activeOnly: Boolean = true): SeriesResult<List<EpisodeSource>> =
        withContext(Dispatchers.IO) {
            try {
                val actFilter = if (activeOnly) "eq.true" else null
                val response = client.api.getSources(
                    episodeIdFilter = "eq.$episodeId",
                    isActiveFilter = actFilter
                )
                if (response.isSuccessful) {
                    SeriesResult.success(response.body() ?: emptyList())
                } else {
                    parseError(response)
                }
            } catch (e: Exception) {
                SeriesResult.failure(e.localizedMessage ?: "Failed to load episode sources", cause = e)
            }
        }

    suspend fun getRelated(seriesId: String, limit: Int = 12): SeriesResult<List<SeriesCard>> =
        withContext(Dispatchers.IO) {
            try {
                val response = client.api.getRelatedSeriesRpc(
                    GetRelatedSeriesRpcRequest(pSeriesId = seriesId, pLimit = limit)
                )
                if (response.isSuccessful) {
                    SeriesResult.success(response.body() ?: emptyList())
                } else {
                    parseError(response)
                }
            } catch (e: Exception) {
                SeriesResult.failure(e.localizedMessage ?: "Failed to load related series", cause = e)
            }
        }

    suspend fun getContinueWatching(limit: Int = 12): SeriesResult<List<ContinueWatchingItem>> =
        withContext(Dispatchers.IO) {
            if (currentUserId.isNullOrBlank()) return@withContext SeriesResult.success(emptyList())
            try {
                val response = client.api.getContinueWatchingRpc(
                    GetContinueWatchingRpcRequest(pLimit = limit),
                    userToken = userTokenHeader
                )
                if (response.isSuccessful) {
                    SeriesResult.success(response.body() ?: emptyList())
                } else {
                    // Never crash for guests or 401
                    SeriesResult.success(emptyList())
                }
            } catch (e: Exception) {
                SeriesResult.success(emptyList())
            }
        }

    suspend fun getComments(
        seriesId: String,
        episodeId: String? = null,
        parentId: String? = null
    ): SeriesResult<List<SeriesComment>> = withContext(Dispatchers.IO) {
        try {
            val filters = mutableMapOf<String, String>()
            filters["series_id"] = "eq.$seriesId"
            if (episodeId != null) {
                filters["episode_id"] = "eq.$episodeId"
            } else {
                filters["episode_id"] = "is.null"
            }
            if (parentId != null) {
                filters["parent_id"] = "eq.$parentId"
            } else {
                filters["parent_id"] = "is.null"
            }

            val response = client.api.getSeriesComments(filters)
            if (response.isSuccessful) {
                SeriesResult.success(response.body() ?: emptyList())
            } else {
                parseError(response)
            }
        } catch (e: Exception) {
            SeriesResult.failure(e.localizedMessage ?: "Failed to load comments", cause = e)
        }
    }

    suspend fun getMyWatchlist(): SeriesResult<List<SeriesWatchlistItem>> =
        withContext(Dispatchers.IO) {
            val uid = currentUserId ?: return@withContext SeriesResult.success(emptyList())
            try {
                val response = client.api.getWatchlist("eq.$uid", userToken = userTokenHeader)
                if (response.isSuccessful) {
                    SeriesResult.success(response.body() ?: emptyList())
                } else {
                    parseError(response)
                }
            } catch (e: Exception) {
                SeriesResult.failure(e.localizedMessage ?: "Failed to load watchlist", cause = e)
            }
        }

    suspend fun isSeriesLiked(seriesId: String): SeriesResult<Boolean> =
        withContext(Dispatchers.IO) {
            val uid = currentUserId ?: return@withContext SeriesResult.success(false)
            try {
                val response = client.api.getSeriesLikes("eq.$seriesId", "eq.$uid")
                if (response.isSuccessful) {
                    SeriesResult.success(!response.body().isNullOrEmpty())
                } else {
                    SeriesResult.success(false)
                }
            } catch (e: Exception) {
                SeriesResult.success(false)
            }
        }

    suspend fun isEpisodeLiked(episodeId: String): SeriesResult<Boolean> =
        withContext(Dispatchers.IO) {
            val uid = currentUserId ?: return@withContext SeriesResult.success(false)
            try {
                val response = client.api.getEpisodeLikes("eq.$episodeId", "eq.$uid")
                if (response.isSuccessful) {
                    SeriesResult.success(!response.body().isNullOrEmpty())
                } else {
                    SeriesResult.success(false)
                }
            } catch (e: Exception) {
                SeriesResult.success(false)
            }
        }

    suspend fun isCommentLiked(commentId: String): SeriesResult<Boolean> =
        withContext(Dispatchers.IO) {
            val uid = currentUserId ?: return@withContext SeriesResult.success(false)
            try {
                val response = client.api.getSeriesCommentLikes("eq.$commentId", "eq.$uid")
                if (response.isSuccessful) {
                    SeriesResult.success(!response.body().isNullOrEmpty())
                } else {
                    SeriesResult.success(false)
                }
            } catch (e: Exception) {
                SeriesResult.success(false)
            }
        }

    suspend fun getMyRating(seriesId: String): SeriesResult<Int?> =
        withContext(Dispatchers.IO) {
            val uid = currentUserId ?: return@withContext SeriesResult.success(null)
            try {
                val response = client.api.getMyRating("eq.$seriesId", "eq.$uid")
                if (response.isSuccessful) {
                    SeriesResult.success(response.body()?.firstOrNull()?.rating)
                } else {
                    SeriesResult.success(null)
                }
            } catch (e: Exception) {
                SeriesResult.success(null)
            }
        }

    suspend fun getProgress(seriesId: String, episodeId: String? = null): SeriesResult<List<WatchProgress>> =
        withContext(Dispatchers.IO) {
            val uid = currentUserId ?: return@withContext SeriesResult.success(emptyList())
            try {
                val epFilter = episodeId?.let { "eq.$it" }
                val response = client.api.getProgress(
                    userIdFilter = "eq.$uid",
                    seriesIdFilter = "eq.$seriesId",
                    episodeIdFilter = epFilter,
                    userToken = userTokenHeader
                )
                if (response.isSuccessful) {
                    SeriesResult.success(response.body() ?: emptyList())
                } else {
                    SeriesResult.success(emptyList())
                }
            } catch (e: Exception) {
                SeriesResult.success(emptyList())
            }
        }

    // =========================================================================
    // 2. USER ACTIONS (Engagement & Interactions)
    // =========================================================================

    suspend fun likeSeries(seriesId: String): SeriesResult<Unit> =
        withContext(Dispatchers.IO) {
            val uid = currentUserId ?: return@withContext SeriesResult.failure("Please sign in to like titles.")
            try {
                val response = client.api.likeSeries(
                    SeriesLikeInsert(seriesId = seriesId, userId = uid),
                    userToken = userTokenHeader
                )
                if (response.isSuccessful) SeriesResult.success(Unit) else parseError(response)
            } catch (e: Exception) {
                SeriesResult.failure(e.localizedMessage ?: "Failed to like series", cause = e)
            }
        }

    suspend fun unlikeSeries(seriesId: String): SeriesResult<Unit> =
        withContext(Dispatchers.IO) {
            val uid = currentUserId ?: return@withContext SeriesResult.failure("Please sign in.")
            try {
                val response = client.api.unlikeSeries("eq.$seriesId", "eq.$uid", userToken = userTokenHeader)
                if (response.isSuccessful) SeriesResult.success(Unit) else parseError(response)
            } catch (e: Exception) {
                SeriesResult.failure(e.localizedMessage ?: "Failed to unlike series", cause = e)
            }
        }

    suspend fun likeEpisode(episodeId: String): SeriesResult<Unit> =
        withContext(Dispatchers.IO) {
            val uid = currentUserId ?: return@withContext SeriesResult.failure("Please sign in to like episodes.")
            try {
                val response = client.api.likeEpisode(
                    EpisodeLikeInsert(episodeId = episodeId, userId = uid),
                    userToken = userTokenHeader
                )
                if (response.isSuccessful) SeriesResult.success(Unit) else parseError(response)
            } catch (e: Exception) {
                SeriesResult.failure(e.localizedMessage ?: "Failed to like episode", cause = e)
            }
        }

    suspend fun unlikeEpisode(episodeId: String): SeriesResult<Unit> =
        withContext(Dispatchers.IO) {
            val uid = currentUserId ?: return@withContext SeriesResult.failure("Please sign in.")
            try {
                val response = client.api.unlikeEpisode("eq.$episodeId", "eq.$uid", userToken = userTokenHeader)
                if (response.isSuccessful) SeriesResult.success(Unit) else parseError(response)
            } catch (e: Exception) {
                SeriesResult.failure(e.localizedMessage ?: "Failed to unlike episode", cause = e)
            }
        }

    suspend fun rateSeries(seriesId: String, rating: Int): SeriesResult<Unit> =
        withContext(Dispatchers.IO) {
            val uid = currentUserId ?: return@withContext SeriesResult.failure("Please sign in to rate.")
            try {
                val validRating = rating.coerceIn(1, 10)
                val response = client.api.rateSeries(
                    RatingUpsert(seriesId = seriesId, userId = uid, rating = validRating),
                    userToken = userTokenHeader
                )
                if (response.isSuccessful) SeriesResult.success(Unit) else parseError(response)
            } catch (e: Exception) {
                SeriesResult.failure(e.localizedMessage ?: "Failed to save rating", cause = e)
            }
        }

    suspend fun addToWatchlist(seriesId: String): SeriesResult<Unit> =
        withContext(Dispatchers.IO) {
            val uid = currentUserId ?: return@withContext SeriesResult.failure("Please sign in to save to My List.")
            try {
                val response = client.api.addToWatchlist(
                    WatchlistInsert(seriesId = seriesId, userId = uid),
                    userToken = userTokenHeader
                )
                if (response.isSuccessful) SeriesResult.success(Unit) else parseError(response)
            } catch (e: Exception) {
                SeriesResult.failure(e.localizedMessage ?: "Failed to update My List", cause = e)
            }
        }

    suspend fun removeFromWatchlist(seriesId: String): SeriesResult<Unit> =
        withContext(Dispatchers.IO) {
            val uid = currentUserId ?: return@withContext SeriesResult.failure("Please sign in.")
            try {
                val response = client.api.removeFromWatchlist("eq.$seriesId", "eq.$uid", userToken = userTokenHeader)
                if (response.isSuccessful) SeriesResult.success(Unit) else parseError(response)
            } catch (e: Exception) {
                SeriesResult.failure(e.localizedMessage ?: "Failed to remove from My List", cause = e)
            }
        }

    suspend fun upsertProgress(
        seriesId: String,
        episodeId: String,
        positionSec: Int,
        durationSec: Int?,
        completed: Boolean
    ): SeriesResult<Unit> = withContext(Dispatchers.IO) {
        val uid = currentUserId ?: return@withContext SeriesResult.success(Unit)
        try {
            val response = client.api.upsertProgress(
                ProgressUpsert(
                    userId = uid,
                    episodeId = episodeId,
                    seriesId = seriesId,
                    positionSec = positionSec,
                    durationSec = durationSec,
                    completed = completed
                ),
                userToken = userTokenHeader
            )
            if (response.isSuccessful) SeriesResult.success(Unit) else parseError(response)
        } catch (e: Exception) {
            SeriesResult.failure(e.localizedMessage ?: "Failed to sync progress", cause = e)
        }
    }

    suspend fun recordView(seriesId: String, episodeId: String? = null): SeriesResult<Unit> =
        withContext(Dispatchers.IO) {
            try {
                val key = guestViewerKey
                val response = client.api.recordViewRpc(
                    RecordViewRpcRequest(
                        pSeriesId = seriesId,
                        pEpisodeId = episodeId,
                        pViewerKey = key
                    ),
                    userToken = userTokenHeader
                )
                if (response.isSuccessful) SeriesResult.success(Unit) else parseError(response)
            } catch (e: Exception) {
                SeriesResult.failure(e.localizedMessage ?: "Failed to log view", cause = e)
            }
        }

    suspend fun addComment(
        seriesId: String,
        episodeId: String?,
        parentId: String?,
        content: String
    ): SeriesResult<SeriesComment> = withContext(Dispatchers.IO) {
        val uid = currentUserId ?: return@withContext SeriesResult.failure("Please sign in to comment.")
        if (content.trim().isEmpty()) return@withContext SeriesResult.failure("Comment cannot be empty.")
        try {
            val response = client.api.insertSeriesComment(
                CommentInsert(
                    seriesId = seriesId,
                    episodeId = episodeId,
                    parentId = parentId,
                    content = content.trim(),
                    userId = uid
                ),
                userToken = userTokenHeader
            )
            if (response.isSuccessful && !response.body().isNullOrEmpty()) {
                SeriesResult.success(response.body()!!.first())
            } else {
                parseError(response)
            }
        } catch (e: Exception) {
            SeriesResult.failure(e.localizedMessage ?: "Failed to post comment", cause = e)
        }
    }

    suspend fun editCommentContent(commentId: String, content: String): SeriesResult<SeriesComment> =
        withContext(Dispatchers.IO) {
            if (content.trim().isEmpty()) return@withContext SeriesResult.failure("Comment cannot be empty.")
            try {
                val response = client.api.updateSeriesCommentContent(
                    idFilter = "eq.$commentId",
                    updates = CommentUpdate(content = content.trim()),
                    userToken = userTokenHeader
                )
                if (response.isSuccessful && !response.body().isNullOrEmpty()) {
                    SeriesResult.success(response.body()!!.first())
                } else {
                    parseError(response)
                }
            } catch (e: Exception) {
                SeriesResult.failure(e.localizedMessage ?: "Failed to update comment", cause = e)
            }
        }

    suspend fun deleteComment(commentId: String): SeriesResult<Unit> =
        withContext(Dispatchers.IO) {
            try {
                val response = client.api.deleteSeriesComment("eq.$commentId", userToken = userTokenHeader)
                if (response.isSuccessful) SeriesResult.success(Unit) else parseError(response)
            } catch (e: Exception) {
                SeriesResult.failure(e.localizedMessage ?: "Failed to delete comment", cause = e)
            }
        }

    suspend fun likeComment(commentId: String): SeriesResult<Unit> =
        withContext(Dispatchers.IO) {
            val uid = currentUserId ?: return@withContext SeriesResult.failure("Please sign in.")
            try {
                val response = client.api.likeSeriesComment(
                    CommentLikeInsert(commentId = commentId, userId = uid),
                    userToken = userTokenHeader
                )
                if (response.isSuccessful) SeriesResult.success(Unit) else parseError(response)
            } catch (e: Exception) {
                SeriesResult.failure(e.localizedMessage ?: "Failed to like comment", cause = e)
            }
        }

    suspend fun unlikeComment(commentId: String): SeriesResult<Unit> =
        withContext(Dispatchers.IO) {
            val uid = currentUserId ?: return@withContext SeriesResult.failure("Please sign in.")
            try {
                val response = client.api.unlikeSeriesComment("eq.$commentId", "eq.$uid", userToken = userTokenHeader)
                if (response.isSuccessful) SeriesResult.success(Unit) else parseError(response)
            } catch (e: Exception) {
                SeriesResult.failure(e.localizedMessage ?: "Failed to unlike comment", cause = e)
            }
        }

    suspend fun reportSource(
        sourceId: String,
        reason: String,
        details: String? = null
    ): SeriesResult<SourceReport> = withContext(Dispatchers.IO) {
        val uid = currentUserId ?: return@withContext SeriesResult.failure("Please sign in to submit a report.")
        try {
            val response = client.api.reportSource(
                ReportInsert(
                    sourceId = sourceId,
                    userId = uid,
                    reason = reason,
                    details = details
                ),
                userToken = userTokenHeader
            )
            if (response.isSuccessful && !response.body().isNullOrEmpty()) {
                SeriesResult.success(response.body()!!.first())
            } else {
                parseError(response)
            }
        } catch (e: Exception) {
            SeriesResult.failure(e.localizedMessage ?: "Failed to send report", cause = e)
        }
    }

    // =========================================================================
    // 3. STUDIO OPERATIONS (Creator & Admin Studio)
    // =========================================================================

    suspend fun createSeriesWithAutoSlug(baseRequest: SeriesInsert): SeriesResult<Series> =
        withContext(Dispatchers.IO) {
            val baseSlug = if (baseRequest.slug.isNotBlank()) baseRequest.slug else generateSlug(baseRequest.title)
            var currentSlug = baseSlug
            var attempt = 1

            while (attempt <= 5) {
                val request = baseRequest.copy(slug = currentSlug)
                try {
                    val response = client.api.createSeries(request, userToken = userTokenHeader)
                    if (response.isSuccessful && !response.body().isNullOrEmpty()) {
                        return@withContext SeriesResult.success(response.body()!!.first())
                    }
                    if (response.code() == 409) {
                        attempt++
                        currentSlug = "$baseSlug-$attempt"
                    } else {
                        return@withContext parseError(response)
                    }
                } catch (e: Exception) {
                    return@withContext SeriesResult.failure(e.localizedMessage ?: "Failed to create series", cause = e)
                }
            }
            SeriesResult.failure("A series with this slug already exists. Please choose a different title or slug.")
        }

    suspend fun updateSeries(id: String, updates: SeriesUpdate): SeriesResult<Series> =
        withContext(Dispatchers.IO) {
            try {
                val response = client.api.updateSeries("eq.$id", updates, userToken = userTokenHeader)
                if (response.isSuccessful && !response.body().isNullOrEmpty()) {
                    SeriesResult.success(response.body()!!.first())
                } else {
                    parseError(response)
                }
            } catch (e: Exception) {
                SeriesResult.failure(e.localizedMessage ?: "Failed to update series", cause = e)
            }
        }

    suspend fun deleteSeries(id: String): SeriesResult<Unit> =
        withContext(Dispatchers.IO) {
            try {
                val response = client.api.deleteSeries("eq.$id", userToken = userTokenHeader)
                if (response.isSuccessful) SeriesResult.success(Unit) else parseError(response)
            } catch (e: Exception) {
                SeriesResult.failure(e.localizedMessage ?: "Failed to delete series", cause = e)
            }
        }

    suspend fun listMySeries(): SeriesResult<List<Series>> =
        withContext(Dispatchers.IO) {
            val uid = currentUserId ?: return@withContext SeriesResult.success(emptyList())
            try {
                val response = client.api.getMySeries("eq.$uid", userToken = userTokenHeader)
                if (response.isSuccessful) {
                    SeriesResult.success(response.body() ?: emptyList())
                } else {
                    parseError(response)
                }
            } catch (e: Exception) {
                SeriesResult.failure(e.localizedMessage ?: "Failed to load creator series", cause = e)
            }
        }

    suspend fun listAllSeriesAdmin(): SeriesResult<List<Series>> =
        withContext(Dispatchers.IO) {
            try {
                val response = client.api.getAllSeriesAdmin(userToken = userTokenHeader)
                if (response.isSuccessful) {
                    SeriesResult.success(response.body() ?: emptyList())
                } else {
                    parseError(response)
                }
            } catch (e: Exception) {
                SeriesResult.failure(e.localizedMessage ?: "Failed to load all series", cause = e)
            }
        }

    suspend fun blockSeries(id: String, reason: String): SeriesResult<Series> =
        updateSeries(id, SeriesUpdate(isBlocked = true, blockedReason = reason))

    suspend fun unblockSeries(id: String): SeriesResult<Series> =
        updateSeries(id, SeriesUpdate(isBlocked = false, blockedReason = null))

    // Seasons CRUD
    suspend fun createSeason(request: SeasonInsert): SeriesResult<Season> =
        withContext(Dispatchers.IO) {
            try {
                val response = client.api.createSeason(request, userToken = userTokenHeader)
                if (response.isSuccessful && !response.body().isNullOrEmpty()) {
                    SeriesResult.success(response.body()!!.first())
                } else {
                    parseError(response)
                }
            } catch (e: Exception) {
                SeriesResult.failure(e.localizedMessage ?: "Failed to create season", cause = e)
            }
        }

    suspend fun updateSeason(id: String, updates: SeasonUpdate): SeriesResult<Season> =
        withContext(Dispatchers.IO) {
            try {
                val response = client.api.updateSeason("eq.$id", updates, userToken = userTokenHeader)
                if (response.isSuccessful && !response.body().isNullOrEmpty()) {
                    SeriesResult.success(response.body()!!.first())
                } else {
                    parseError(response)
                }
            } catch (e: Exception) {
                SeriesResult.failure(e.localizedMessage ?: "Failed to update season", cause = e)
            }
        }

    suspend fun deleteSeason(id: String): SeriesResult<Unit> =
        withContext(Dispatchers.IO) {
            try {
                val response = client.api.deleteSeason("eq.$id", userToken = userTokenHeader)
                if (response.isSuccessful) SeriesResult.success(Unit) else parseError(response)
            } catch (e: Exception) {
                SeriesResult.failure(e.localizedMessage ?: "Failed to delete season", cause = e)
            }
        }

    // Episodes CRUD
    suspend fun createEpisode(request: EpisodeInsert): SeriesResult<Episode> =
        withContext(Dispatchers.IO) {
            try {
                val response = client.api.createEpisode(request, userToken = userTokenHeader)
                if (response.isSuccessful && !response.body().isNullOrEmpty()) {
                    SeriesResult.success(response.body()!!.first())
                } else {
                    parseError(response)
                }
            } catch (e: Exception) {
                SeriesResult.failure(e.localizedMessage ?: "Failed to create episode", cause = e)
            }
        }

    suspend fun updateEpisode(id: String, updates: EpisodeUpdate): SeriesResult<Episode> =
        withContext(Dispatchers.IO) {
            try {
                val response = client.api.updateEpisode("eq.$id", updates, userToken = userTokenHeader)
                if (response.isSuccessful && !response.body().isNullOrEmpty()) {
                    SeriesResult.success(response.body()!!.first())
                } else {
                    parseError(response)
                }
            } catch (e: Exception) {
                SeriesResult.failure(e.localizedMessage ?: "Failed to update episode", cause = e)
            }
        }

    suspend fun deleteEpisode(id: String): SeriesResult<Unit> =
        withContext(Dispatchers.IO) {
            try {
                val response = client.api.deleteEpisode("eq.$id", userToken = userTokenHeader)
                if (response.isSuccessful) SeriesResult.success(Unit) else parseError(response)
            } catch (e: Exception) {
                SeriesResult.failure(e.localizedMessage ?: "Failed to delete episode", cause = e)
            }
        }

    // Sources CRUD
    suspend fun createSource(request: SourceInsert): SeriesResult<EpisodeSource> =
        withContext(Dispatchers.IO) {
            try {
                val response = client.api.createSource(request, userToken = userTokenHeader)
                if (response.isSuccessful && !response.body().isNullOrEmpty()) {
                    SeriesResult.success(response.body()!!.first())
                } else {
                    parseError(response)
                }
            } catch (e: Exception) {
                SeriesResult.failure(e.localizedMessage ?: "Failed to create video source", cause = e)
            }
        }

    suspend fun updateSource(id: String, updates: SourceUpdate): SeriesResult<EpisodeSource> =
        withContext(Dispatchers.IO) {
            try {
                val response = client.api.updateSource("eq.$id", updates, userToken = userTokenHeader)
                if (response.isSuccessful && !response.body().isNullOrEmpty()) {
                    SeriesResult.success(response.body()!!.first())
                } else {
                    parseError(response)
                }
            } catch (e: Exception) {
                SeriesResult.failure(e.localizedMessage ?: "Failed to update video source", cause = e)
            }
        }

    suspend fun deleteSource(id: String): SeriesResult<Unit> =
        withContext(Dispatchers.IO) {
            try {
                val response = client.api.deleteSource("eq.$id", userToken = userTokenHeader)
                if (response.isSuccessful) SeriesResult.success(Unit) else parseError(response)
            } catch (e: Exception) {
                SeriesResult.failure(e.localizedMessage ?: "Failed to delete video source", cause = e)
            }
        }

    suspend fun swapSourcePriority(sourceA: EpisodeSource, sourceB: EpisodeSource): SeriesResult<Unit> =
        withContext(Dispatchers.IO) {
            try {
                val priorityA = sourceA.priority
                val priorityB = sourceB.priority
                val resA = updateSource(sourceA.id, SourceUpdate(priority = priorityB))
                if (!resA.isSuccess) return@withContext SeriesResult.failure("Failed to swap priorities")
                val resB = updateSource(sourceB.id, SourceUpdate(priority = priorityA))
                if (!resB.isSuccess) return@withContext SeriesResult.failure("Failed to swap priorities")
                SeriesResult.success(Unit)
            } catch (e: Exception) {
                SeriesResult.failure(e.localizedMessage ?: "Priority swap failed", cause = e)
            }
        }

    suspend fun disableSource(sourceId: String): SeriesResult<EpisodeSource> =
        updateSource(sourceId, SourceUpdate(isActive = false))

    // Shelves & Shelf Items
    suspend fun listMyEligibleShelves(): SeriesResult<List<Shelf>> =
        withContext(Dispatchers.IO) {
            try {
                val response = client.api.getShelves(isActiveFilter = "eq.true", typeFilter = "eq.manual")
                if (response.isSuccessful) {
                    SeriesResult.success(response.body() ?: emptyList())
                } else {
                    parseError(response)
                }
            } catch (e: Exception) {
                SeriesResult.failure(e.localizedMessage ?: "Failed to load shelves", cause = e)
            }
        }

    suspend fun setShelfItem(shelfId: String, seriesId: String, position: Int = 0): SeriesResult<Unit> =
        withContext(Dispatchers.IO) {
            try {
                val response = client.api.upsertShelfItem(
                    ShelfItemInsert(shelfId = shelfId, seriesId = seriesId, position = position),
                    userToken = userTokenHeader
                )
                if (response.isSuccessful) SeriesResult.success(Unit) else parseError(response)
            } catch (e: Exception) {
                SeriesResult.failure(e.localizedMessage ?: "Failed to link series to shelf", cause = e)
            }
        }

    suspend fun removeShelfItem(shelfId: String, seriesId: String): SeriesResult<Unit> =
        withContext(Dispatchers.IO) {
            try {
                val response = client.api.removeShelfItem("eq.$shelfId", "eq.$seriesId", userToken = userTokenHeader)
                if (response.isSuccessful) SeriesResult.success(Unit) else parseError(response)
            } catch (e: Exception) {
                SeriesResult.failure(e.localizedMessage ?: "Failed to remove series from shelf", cause = e)
            }
        }

    suspend fun getShelvesAdmin(): SeriesResult<List<Shelf>> =
        withContext(Dispatchers.IO) {
            try {
                val response = client.api.getShelves()
                if (response.isSuccessful) {
                    SeriesResult.success(response.body() ?: emptyList())
                } else {
                    parseError(response)
                }
            } catch (e: Exception) {
                SeriesResult.failure(e.localizedMessage ?: "Failed to load shelves", cause = e)
            }
        }

    suspend fun createShelf(request: ShelfInsert): SeriesResult<Shelf> =
        withContext(Dispatchers.IO) {
            try {
                val response = client.api.createShelf(request, userToken = userTokenHeader)
                if (response.isSuccessful && !response.body().isNullOrEmpty()) {
                    SeriesResult.success(response.body()!!.first())
                } else {
                    parseError(response)
                }
            } catch (e: Exception) {
                SeriesResult.failure(e.localizedMessage ?: "Failed to create shelf", cause = e)
            }
        }

    suspend fun updateShelf(id: String, updates: ShelfUpdate): SeriesResult<Shelf> =
        withContext(Dispatchers.IO) {
            try {
                val response = client.api.updateShelf("eq.$id", updates, userToken = userTokenHeader)
                if (response.isSuccessful && !response.body().isNullOrEmpty()) {
                    SeriesResult.success(response.body()!!.first())
                } else {
                    parseError(response)
                }
            } catch (e: Exception) {
                SeriesResult.failure(e.localizedMessage ?: "Failed to update shelf", cause = e)
            }
        }

    suspend fun deleteShelf(id: String): SeriesResult<Unit> =
        withContext(Dispatchers.IO) {
            try {
                val response = client.api.deleteShelf("eq.$id", userToken = userTokenHeader)
                if (response.isSuccessful) SeriesResult.success(Unit) else parseError(response)
            } catch (e: Exception) {
                SeriesResult.failure(e.localizedMessage ?: "Failed to delete shelf", cause = e)
            }
        }

    suspend fun getShelfItems(shelfId: String): SeriesResult<List<ShelfItem>> =
        withContext(Dispatchers.IO) {
            try {
                val response = client.api.getShelfItems("eq.$shelfId")
                if (response.isSuccessful) {
                    SeriesResult.success(response.body() ?: emptyList())
                } else {
                    parseError(response)
                }
            } catch (e: Exception) {
                SeriesResult.failure(e.localizedMessage ?: "Failed to load shelf items", cause = e)
            }
        }

    // Categories CRUD (Admin)
    suspend fun getCategoriesAdmin(): SeriesResult<List<ContentCategory>> =
        getCategories(activeOnly = false)

    suspend fun createCategory(request: CategoryInsert): SeriesResult<ContentCategory> =
        withContext(Dispatchers.IO) {
            try {
                val response = client.api.createCategory(request, userToken = userTokenHeader)
                if (response.isSuccessful && !response.body().isNullOrEmpty()) {
                    SeriesResult.success(response.body()!!.first())
                } else {
                    parseError(response)
                }
            } catch (e: Exception) {
                SeriesResult.failure(e.localizedMessage ?: "Failed to create category", cause = e)
            }
        }

    suspend fun updateCategory(id: String, updates: CategoryUpdate): SeriesResult<ContentCategory> =
        withContext(Dispatchers.IO) {
            try {
                val response = client.api.updateCategory("eq.$id", updates, userToken = userTokenHeader)
                if (response.isSuccessful && !response.body().isNullOrEmpty()) {
                    SeriesResult.success(response.body()!!.first())
                } else {
                    parseError(response)
                }
            } catch (e: Exception) {
                SeriesResult.failure(e.localizedMessage ?: "Failed to update category", cause = e)
            }
        }

    suspend fun deleteCategory(id: String): SeriesResult<Unit> =
        withContext(Dispatchers.IO) {
            try {
                val response = client.api.deleteCategory("eq.$id", userToken = userTokenHeader)
                if (response.isSuccessful) SeriesResult.success(Unit) else parseError(response)
            } catch (e: Exception) {
                SeriesResult.failure(e.localizedMessage ?: "Failed to delete category", cause = e)
            }
        }

    // User Roles & Management (Admin)
    suspend fun searchUsers(query: String): SeriesResult<List<UserProfile>> =
        withContext(Dispatchers.IO) {
            if (query.trim().isEmpty()) return@withContext SeriesResult.success(emptyList())
            try {
                val q = query.trim()
                val filter = "(username.ilike.*$q*,display_name.ilike.*$q*)"
                val response = client.api.searchProfiles(filter, userToken = userTokenHeader)
                if (response.isSuccessful) {
                    SeriesResult.success(response.body() ?: emptyList())
                } else {
                    parseError(response)
                }
            } catch (e: Exception) {
                SeriesResult.failure(e.localizedMessage ?: "Failed to search users", cause = e)
            }
        }

    suspend fun setUserRole(userId: String, role: String): SeriesResult<Unit> =
        withContext(Dispatchers.IO) {
            try {
                val response = client.api.setUserRoleRpc(
                    SetUserRoleRpcRequest(pUserId = userId, pRole = role.lowercase(Locale.ROOT)),
                    userToken = userTokenHeader
                )
                if (response.isSuccessful) {
                    SeriesResult.success(Unit)
                } else {
                    parseError(response)
                }
            } catch (e: Exception) {
                SeriesResult.failure(e.localizedMessage ?: "Failed to set user role", cause = e)
            }
        }

    // Source Reports Moderation
    suspend fun listSourceReports(status: String? = null): SeriesResult<List<SourceReport>> =
        withContext(Dispatchers.IO) {
            try {
                val statusFilter = status?.let { "eq.$it" }
                val response = client.api.getSourceReports(statusFilter = statusFilter, userToken = userTokenHeader)
                if (response.isSuccessful) {
                    SeriesResult.success(response.body() ?: emptyList())
                } else {
                    parseError(response)
                }
            } catch (e: Exception) {
                SeriesResult.failure(e.localizedMessage ?: "Failed to load reports", cause = e)
            }
        }

    suspend fun markReportHandled(reportId: String, status: String = "handled"): SeriesResult<SourceReport> =
        withContext(Dispatchers.IO) {
            try {
                val response = client.api.updateSourceReportStatus(
                    idFilter = "eq.$reportId",
                    updates = ReportUpdateStatus(status = status),
                    userToken = userTokenHeader
                )
                if (response.isSuccessful && !response.body().isNullOrEmpty()) {
                    SeriesResult.success(response.body()!!.first())
                } else {
                    parseError(response)
                }
            } catch (e: Exception) {
                SeriesResult.failure(e.localizedMessage ?: "Failed to update report status", cause = e)
            }
        }

    suspend fun deleteSourceReport(reportId: String): SeriesResult<Unit> =
        withContext(Dispatchers.IO) {
            try {
                val response = client.api.deleteSourceReport("eq.$reportId", userToken = userTokenHeader)
                if (response.isSuccessful) SeriesResult.success(Unit) else parseError(response)
            } catch (e: Exception) {
                SeriesResult.failure(e.localizedMessage ?: "Failed to delete report", cause = e)
            }
        }
}
