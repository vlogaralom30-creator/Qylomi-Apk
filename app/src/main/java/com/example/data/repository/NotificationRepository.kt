package com.example.data.repository

import android.content.Context
import com.example.data.model.NotificationItem
import com.example.data.service.SupabaseService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.util.Log

class NotificationRepository(
    private val service: SupabaseService = SupabaseService.getInstance()
) {

    suspend fun notify(recipientId: String, type: String, postId: String?, commentId: String?) = withContext(Dispatchers.IO) {
        // Skips self-notification logic happens in RPC SECURITY DEFINER, 
        // but we can add a basic check here for efficiency.
        try {
            val payload = mapOf(
                "p_recipient_id" to recipientId,
                "p_type" to type,
                "p_post_id" to postId,
                "p_comment_id" to commentId
            )
            val res = service.sendNotificationRpc(payload)
            res.onFailure {
                Log.e("NotificationRepo", "Failed to send notification", it)
            }
        } catch (e: Exception) {
            Log.e("NotificationRepo", "Error sending notification", e)
        }
    }
}
