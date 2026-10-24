package com.example.player.resolver

import android.util.Log
import com.example.video.VideoPlatformRegistry
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.util.concurrent.ConcurrentHashMap

object MediaResolver {
    private const val TAG = "MediaResolver"
    private const val MAX_CACHE_ENTRIES = 40
    private const val TIMEOUT_MS = 8000L
    private val RETRY_BACKOFFS_MS = listOf(500L, 1500L)

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // 40-entry LRU Cache implementation over LinkedHashMap
    private val lruCache = object : java.util.LinkedHashMap<MediaKey, ResolvedMedia>(16, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<MediaKey, ResolvedMedia>?): Boolean {
            return size > MAX_CACHE_ENTRIES
        }
    }

    private val inFlightJobs = ConcurrentHashMap<MediaKey, Deferred<ResolvedMedia>>()
    private val backgroundSemaphore = Semaphore(2)

    val defaultTikTokHeaders = mapOf(
        "Referer" to "https://www.tiktok.com/",
        "User-Agent" to "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Mobile Safari/537.36"
    )

    private fun logDebug(msg: String) {
        try {
            Log.d(TAG, msg)
        } catch (_: Throwable) {
            println("$TAG: $msg")
        }
    }

    private fun logWarn(msg: String) {
        try {
            Log.w(TAG, msg)
        } catch (_: Throwable) {
            println("$TAG WARN: $msg")
        }
    }

    private fun logError(msg: String) {
        try {
            Log.e(TAG, msg)
        } catch (_: Throwable) {
            println("$TAG ERROR: $msg")
        }
    }

    suspend fun resolve(
        key: MediaKey,
        force: Boolean = false,
        isHighPriority: Boolean = false
    ): ResolvedMedia = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()

        // 1. Check in-memory LRU cache unless force = true
        if (!force) {
            val cached = synchronized(lruCache) { lruCache[key] }
            if (cached != null) {
                if (!cached.isStale(now)) {
                    logDebug("Cache HIT for ${key.url}")
                    return@withContext cached
                } else {
                    logDebug("Cache STALE for ${key.url}, re-resolving")
                    synchronized(lruCache) { lruCache.remove(key) }
                }
            } else {
                logDebug("Cache MISS for ${key.url}")
            }
        } else {
            logDebug("Force resolve requested for ${key.url}")
            synchronized(lruCache) { lruCache.remove(key) }
        }

        // 2. In-flight de-duplication
        val existingJob = inFlightJobs[key]
        if (existingJob != null && existingJob.isActive) {
            logDebug("Joining existing in-flight extraction job for ${key.url}")
            return@withContext existingJob.await()
        }

        val newJob = scope.async {
            if (isHighPriority) {
                executeExtractionWithRetry(key)
            } else {
                backgroundSemaphore.withPermit {
                    executeExtractionWithRetry(key)
                }
            }
        }

        inFlightJobs[key] = newJob

        try {
            val result = newJob.await()
            synchronized(lruCache) {
                lruCache[key] = result
            }
            result
        } finally {
            inFlightJobs.remove(key)
        }
    }

    fun invalidate(key: MediaKey) {
        synchronized(lruCache) {
            lruCache.remove(key)
        }
        logDebug("Invalidated key: ${key.url}")
    }

    fun putInCache(key: MediaKey, media: ResolvedMedia) {
        synchronized(lruCache) {
            lruCache[key] = media
        }
    }

    fun getCached(key: MediaKey): ResolvedMedia? {
        val now = System.currentTimeMillis()
        return synchronized(lruCache) {
            val entry = lruCache[key]
            if (entry != null && !entry.isStale(now)) entry else null
        }
    }

    private suspend fun executeExtractionWithRetry(key: MediaKey): ResolvedMedia {
        var lastException: Exception? = null
        val startTime = System.currentTimeMillis()

        for (attempt in 0..RETRY_BACKOFFS_MS.size) {
            try {
                return withTimeout(TIMEOUT_MS) {
                    val extracted = VideoPlatformRegistry.extract(key.url).getOrThrow()
                    val directUrl = extracted.directUrl ?: extracted.embedUrl
                    val now = System.currentTimeMillis()
                    val expiresAt = ExpiryParser.calculateExpiryMs(directUrl, now)

                    val resolved = ResolvedMedia(
                        url = directUrl,
                        headers = if (key.platform == "tiktok") defaultTikTokHeaders else (extracted.requestHeaders ?: emptyMap()),
                        resolvedAtMs = now,
                        expiresAtMs = expiresAt,
                        isHls = directUrl.contains(".m3u8", ignoreCase = true),
                        thumbnailUrl = extracted.thumbnailUrl,
                        durationMs = extracted.durationSec?.let { it * 1000L }
                    )

                    val elapsed = System.currentTimeMillis() - startTime
                    logDebug("Successfully resolved ${key.url} in ${elapsed}ms (Attempt ${attempt + 1})")
                    resolved
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                lastException = e
                logWarn("Extraction attempt ${attempt + 1} failed for ${key.url}: ${e.message}")
                if (attempt < RETRY_BACKOFFS_MS.size) {
                    delay(RETRY_BACKOFFS_MS[attempt])
                }
            }
        }

        val elapsed = System.currentTimeMillis() - startTime
        logError("All extraction retries exhausted for ${key.url} after ${elapsed}ms")
        throw lastException ?: IllegalStateException("Failed to resolve media for ${key.url}")
    }
}
