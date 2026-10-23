package com.example.data.repository

import android.content.Context
import com.example.data.api.SupabaseClient
import com.example.data.model.Post
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class RelayRepository(
    private val currentUserProvider: () -> com.example.data.model.UserProfile?
) {

    suspend fun toggleRelay(postId: String, quote: String?): Result<Boolean> = withContext(Dispatchers.IO) {
        val user = currentUserProvider() ?: return@withContext Result.failure(Exception("Not logged in"))
        try {
            // Check if already relayed
            val exists = checkRelayExists(postId, user.id)
            if (exists) {
                SupabaseClient.api.deleteRelay("eq.$postId", "eq.${user.id}")
                Result.success(false)
            } else {
                SupabaseClient.api.insertRelay(
                    mapOf(
                        "post_id" to postId,
                        "user_id" to user.id,
                        "quote" to quote
                    )
                )
                Result.success(true)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun checkRelayExists(postId: String, userId: String): Boolean {
        try {
            val res = SupabaseClient.api.getRelay("eq.$postId", "eq.$userId")
            return res.isSuccessful && !res.body().isNullOrEmpty()
        } catch (e: Exception) {
            return false
        }
    }

    suspend fun getRelayedPostIds(userId: String): List<String> = withContext(Dispatchers.IO) {
        try {
            val res = SupabaseClient.api.getRelaysByUser("eq.$userId")
            if (res.isSuccessful && res.body() != null) {
                res.body()!!.map { it.postId }
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
}
