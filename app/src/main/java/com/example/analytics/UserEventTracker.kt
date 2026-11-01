package com.example.analytics

import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Bundle
import android.util.Log
import com.example.data.api.SupabaseClient
import com.example.data.model.UserEventRecord
import com.example.data.model.UserEventType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap

/**
 * Singleton event tracker for the feed algorithm.
 * Batches events every 2 seconds, on queue size 20, or on app backgrounding.
 * Skips guests and disabled personalization. Never throws or blocks UI.
 */
object UserEventTracker {
    private const val TAG = "UserEventTracker"
    private const val FLUSH_DELAY_MS = 2000L
    private const val BATCH_SIZE = 20
    private const val MAX_QUEUE_SIZE = 100
    private const val OPEN_DEBOUNCE_MS = 30_000L

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mutex = Mutex()

    private val queue = mutableListOf<UserEventRecord>()
    private var flushJob: Job? = null

    // In-memory de-duplication
    private val seenImpressionPostIds = ConcurrentHashMap.newKeySet<String>()
    private val lastOpenTimestamps = ConcurrentHashMap<String, Long>()

    private val _queueSize = MutableStateFlow(0)
    val queueSize: StateFlow<Int> = _queueSize.asStateFlow()

    @Volatile
    var isPersonalizedFeedEnabled: Boolean = true

    fun initialize(application: Application) {
        val prefs = application.getSharedPreferences("q_social_prefs", Context.MODE_PRIVATE)
        isPersonalizedFeedEnabled = prefs.getBoolean("personalized_feed_enabled", true)

        application.registerActivityLifecycleCallbacks(object : Application.ActivityLifecycleCallbacks {
            private var startedCount = 0

            override fun onActivityStarted(activity: Activity) {
                startedCount++
            }

            override fun onActivityStopped(activity: Activity) {
                startedCount--
                if (startedCount <= 0) {
                    startedCount = 0
                    // Flush when application moves to background
                    flush()
                }
            }

            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
            override fun onActivityResumed(activity: Activity) {}
            override fun onActivityPaused(activity: Activity) {}
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
            override fun onActivityDestroyed(activity: Activity) {}
        })
    }

    /**
     * Fire-and-forget event tracking.
     */
    fun track(postId: String?, type: UserEventType) {
        if (postId.isNullOrBlank()) return
        if (!isPersonalizedFeedEnabled) return

        // Skip guests (must have session auth token)
        val token = SupabaseClient.getAuthToken()
        if (token.isNullOrBlank()) return

        // Impression de-duplication: at most once per post per app session
        if (type == UserEventType.IMPRESSION) {
            if (!seenImpressionPostIds.add(postId)) {
                return
            }
        }

        // Open de-duplication: at most once per post per 30 seconds
        if (type == UserEventType.OPEN) {
            val now = System.currentTimeMillis()
            val last = lastOpenTimestamps[postId] ?: 0L
            if (now - last < OPEN_DEBOUNCE_MS) {
                return
            }
            lastOpenTimestamps[postId] = now
        }

        scope.launch {
            val shouldFlushImmediate = mutex.withLock {
                queue.add(UserEventRecord(postId = postId, eventType = type.value))
                _queueSize.value = queue.size

                if (queue.size >= BATCH_SIZE) {
                    true
                } else {
                    if (flushJob == null || flushJob?.isActive != true) {
                        flushJob = scope.launch {
                            delay(FLUSH_DELAY_MS)
                            flushInternal()
                        }
                    }
                    false
                }
            }

            if (shouldFlushImmediate) {
                flush()
            }
        }
    }

    fun isImpressionSeen(postId: String): Boolean = seenImpressionPostIds.contains(postId)

    fun flush() {
        scope.launch {
            flushInternal()
        }
    }

    private suspend fun flushInternal() {
        val eventsToSend: List<UserEventRecord>
        mutex.withLock {
            flushJob?.cancel()
            flushJob = null
            if (queue.isEmpty()) return

            eventsToSend = queue.toList()
            queue.clear()
            _queueSize.value = 0
        }

        try {
            val response = SupabaseClient.api.postUserEvents(eventsToSend)
            if (!response.isSuccessful) {
                Log.w(TAG, "Failed to flush user events: HTTP ${response.code()}")
                requeueOnFailure(eventsToSend)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Network exception flushing user events: ${e.message}")
            requeueOnFailure(eventsToSend)
        }
    }

    private suspend fun requeueOnFailure(failedEvents: List<UserEventRecord>) {
        mutex.withLock {
            val combined = failedEvents + queue
            queue.clear()
            queue.addAll(combined.takeLast(MAX_QUEUE_SIZE))
            _queueSize.value = queue.size
        }
    }

    fun clearSessionData() {
        scope.launch {
            mutex.withLock {
                queue.clear()
                _queueSize.value = 0
                flushJob?.cancel()
                flushJob = null
            }
            seenImpressionPostIds.clear()
            lastOpenTimestamps.clear()
        }
    }

    suspend fun resetPersonalization(userId: String): Result<Unit> {
        return try {
            clearSessionData()
            val response = SupabaseClient.api.resetUserEvents("eq.$userId")
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("HTTP ${response.code()}: ${response.errorBody()?.string()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
