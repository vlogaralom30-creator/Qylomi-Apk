package com.example.data.api

import android.content.Context
import android.util.Log
import com.example.data.model.AuthResponse
import com.example.data.model.Session
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

sealed class SessionState {
    object SignedOut : SessionState()
    data class Authenticated(val userId: String) : SessionState()
    data class Expired(val email: String) : SessionState()
}

class SessionManager private constructor(context: Context) {
    private val sessionStore = SessionStore(context)
    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    private val authResponseAdapter = moshi.adapter(AuthResponse::class.java)

    private val _sessionState = MutableStateFlow<SessionState>(SessionState.SignedOut)
    val sessionState: StateFlow<SessionState> = _sessionState.asStateFlow()

    private val refreshClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val refreshScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val isRefreshing = AtomicBoolean(false)
    private val refreshMutex = Any()

    companion object {
        @Volatile
        var instance: SessionManager? = null
            private set

        fun getInstance(context: Context): SessionManager {
            return instance ?: synchronized(this) {
                instance ?: SessionManager(context.applicationContext).also { instance = it }
            }
        }
    }

    init {
        val active = sessionStore.getActiveSession()
        if (active != null) {
            _sessionState.value = SessionState.Authenticated(active.userId)
        }
    }

    fun getActiveSession(): Session? = sessionStore.getActiveSession()

    fun getSessionByEmail(email: String): Session? = sessionStore.getSessionByEmail(email)

    fun saveSession(session: Session) {
        sessionStore.saveSession(session)
        _sessionState.value = SessionState.Authenticated(session.userId)
        SupabaseClient.setAuthToken(session.accessToken)
    }

    fun logout() {
        val active = sessionStore.getActiveSession()
        if (active != null) {
            val token = active.accessToken
            refreshScope.launch {
                try {
                    val request = Request.Builder()
                        .url("${SupabaseClient.activeUrl}auth/v1/logout?scope=local")
                        .post("{}".toRequestBody("application/json".toMediaType()))
                        .header("apikey", SupabaseClient.activeKey)
                        .header("Authorization", "Bearer $token")
                        .build()
                    refreshClient.newCall(request).execute()
                } catch (_: Exception) {}
            }
            sessionStore.deleteSession(active.userId)
        }
        _sessionState.value = SessionState.SignedOut
        SupabaseClient.setAuthToken(null)
    }

    fun deleteSession(userId: String) {
        sessionStore.deleteSession(userId)
        if (sessionStore.getActiveUserId() == null) {
            _sessionState.value = SessionState.SignedOut
            SupabaseClient.setAuthToken(null)
        }
    }

    /**
     * Single-flight refresh logic.
     */
    fun refreshSession(): Session? {
        val currentSession = sessionStore.getActiveSession() ?: return null
        
        synchronized(refreshMutex) {
            // Re-check after lock
            val sessionAfterLock = sessionStore.getActiveSession() ?: return null
            if (sessionAfterLock.accessToken != currentSession.accessToken && !isTokenExpired(sessionAfterLock)) {
                return sessionAfterLock
            }

            return performRefresh(sessionAfterLock)
        }
    }

    private fun performRefresh(session: Session): Session? {
        val url = "${SupabaseClient.activeUrl}auth/v1/token?grant_type=refresh_token"
        val json = "{\"refresh_token\": \"${session.refreshToken}\"}"
        val body = json.toRequestBody("application/json".toMediaType())
        
        var attempt = 0
        val maxAttempts = 3
        var lastDelay = 1000L

        while (attempt < maxAttempts) {
            val request = Request.Builder()
                .url(url)
                .post(body)
                .header("apikey", SupabaseClient.activeKey)
                .build()

            try {
                val response = refreshClient.newCall(request).execute()
                if (response.isSuccessful) {
                    val authResponse = response.body?.string()?.let { authResponseAdapter.fromJson(it) }
                    val newSession = authResponse?.toSession()
                    if (newSession != null) {
                        val enriched = newSession.copy(
                            userId = if (newSession.userId.isBlank()) session.userId else newSession.userId,
                            email = if (newSession.email.isBlank()) session.email else newSession.email
                        )
                        saveSession(enriched)
                        return enriched
                    }
                } else if (response.code == 400 || response.code == 401) {
                    // Refresh token dead
                    handleSessionExpired(session)
                    return null
                } else if (response.code == 429 || response.code >= 500) {
                    // Retryable server errors
                } else {
                    return null
                }
            } catch (e: IOException) {
                // Network error - retry
            } catch (e: Exception) {
                Log.e("SessionManager", "Refresh failed unexpectedly", e)
                return null
            }

            attempt++
            if (attempt < maxAttempts) {
                Thread.sleep(lastDelay)
                lastDelay *= 2
            }
        }
        return null
    }

    private fun handleSessionExpired(session: Session) {
        sessionStore.deleteSession(session.userId)
        _sessionState.value = SessionState.Expired(session.email)
        SupabaseClient.setAuthToken(null)
    }

    fun isTokenExpired(session: Session): Boolean {
        // Expired if current time is within 90s of expiry
        return (System.currentTimeMillis() / 1000) > (session.expiresAtEpochSec - 90)
    }

    /**
     * Proactively check and refresh the session if it's expired or about to expire.
     */
    fun checkAndRefresh() {
        val session = sessionStore.getActiveSession() ?: return
        if (isTokenExpired(session)) {
            refreshScope.launch {
                refreshSession()
            }
        }
    }

    /**
     * DEBUG ONLY: Force session expiry for testing.
     */
    fun expireSessionDebug() {
        if (!com.example.BuildConfig.DEBUG) return
        val current = sessionStore.getActiveSession() ?: return
        val expired = current.copy(expiresAtEpochSec = System.currentTimeMillis() / 1000 - 3600)
        sessionStore.saveSession(expired)
        _sessionState.value = SessionState.Expired(current.email)
        SupabaseClient.setAuthToken(null)
    }

    // --- OkHttp Components ---

    val interceptor = Interceptor { chain ->
        val original = chain.request()
        val path = original.url.encodedPath
        
        // Skip injection for certain auth paths
        if (path.contains("/auth/v1/token") || path.contains("/auth/v1/signup") || path.contains("/auth/v1/verify")) {
            return@Interceptor chain.proceed(original)
        }

        val session = sessionStore.getActiveSession()
        val requestBuilder = original.newBuilder()
            .header("apikey", SupabaseClient.activeKey)

        if (session != null) {
            // Proactive refresh
            var currentSession = session
            if (isTokenExpired(currentSession)) {
                val refreshed = refreshSession()
                if (refreshed != null) {
                    currentSession = refreshed
                }
            }
            requestBuilder.header("Authorization", "Bearer ${currentSession.accessToken}")
        } else {
            // Guest mode
            if (original.header("Authorization").isNullOrBlank()) {
                requestBuilder.header("Authorization", "Bearer ${SupabaseClient.activeKey}")
            }
        }

        chain.proceed(requestBuilder.build())
    }

    val authenticator = Authenticator { _, response ->
        if (responseCount(response) >= 2) return@Authenticator null

        val session = sessionStore.getActiveSession() ?: return@Authenticator null
        
        // Check if 401 was due to JWT expired
        val body = response.peekBody(1024).string()
        if (body.contains("PGRST301") || body.contains("PGRST303") || body.contains("JWT expired") || response.code == 401) {
            val refreshed = refreshSession()
            if (refreshed != null) {
                return@Authenticator response.request.newBuilder()
                    .header("Authorization", "Bearer ${refreshed.accessToken}")
                    .build()
            }
        }
        null
    }

    private fun responseCount(response: Response): Int {
        var result = 1
        var r = response
        while (r.priorResponse != null) {
            result++
            r = r.priorResponse!!
        }
        return result
    }
}
