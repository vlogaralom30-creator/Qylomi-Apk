package com.example.data.api

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.Authenticator
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

object SupabaseClient {
    const val SUPABASE_URL = "https://ffbmozwsasbhevtehbit.supabase.co/"
    const val SUPABASE_KEY = "sb_publishable_kzXQl0X6eNtzFrAuY-3EFw_pYh6H-qr"

    @Volatile
    var activeUrl: String = SUPABASE_URL
        private set

    @Volatile
    var activeKey: String = SUPABASE_KEY
        private set

    @Volatile
    private var authToken: String? = null

    @Volatile
    private var retrofitApi: SupabaseApi? = null

    /**
     * Initialize or update the Supabase client with custom URL and API key.
     */
    fun initialize(url: String, key: String) {
        activeUrl = if (url.endsWith("/")) url else "$url/"
        activeKey = key.trim()
        synchronized(this) {
            retrofitApi = null
        }
    }

    fun setAuthToken(token: String?) {
        authToken = token
    }

    fun getAuthToken(): String? = authToken

    private val authInterceptor = Interceptor { chain ->
        val original = chain.request()
        val path = original.url.encodedPath

        val requestBuilder = original.newBuilder()
            .header("apikey", activeKey)
            .header("Content-Type", "application/json")

        if (!path.contains("/auth/v1/token")) {
            val sm = SessionManager.instance
            val activeSession = sm?.getActiveSession()
            val validToken = if (activeSession != null) {
                // If token is expiring within 90s, do a proactive refresh
                if (sm.isTokenExpired(activeSession)) {
                    val refreshed = sm.refreshSession()
                    refreshed?.accessToken ?: activeSession.accessToken
                } else {
                    activeSession.accessToken
                }
            } else {
                authToken
            }

            if (!validToken.isNullOrBlank()) {
                requestBuilder.header("Authorization", "Bearer $validToken")
            } else {
                requestBuilder.header("Authorization", "Bearer $activeKey")
            }
        }

        chain.proceed(requestBuilder.build())
    }

    private val auth401Authenticator = Authenticator { _, response ->
        if (responseCount(response) >= 2) return@Authenticator null

        val sm = SessionManager.instance ?: return@Authenticator null
        val body = try { response.peekBody(1024).string() } catch (_: Exception) { "" }

        if (response.code == 401 || body.contains("PGRST301") || body.contains("JWT expired")) {
            val refreshed = sm.refreshSession()
            if (refreshed != null) {
                setAuthToken(refreshed.accessToken)
                return@Authenticator response.request.newBuilder()
                    .header("Authorization", "Bearer ${refreshed.accessToken}")
                    .build()
            }
        }
        null
    }

    private fun responseCount(response: Response): Int {
        var count = 1
        var prior = response.priorResponse
        while (prior != null) {
            count++
            prior = prior.priorResponse
        }
        return count
    }

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(authInterceptor)
        .authenticator(auth401Authenticator)
        .addInterceptor(loggingInterceptor)
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .build()

    private val moshi: Moshi = Moshi.Builder()
        .add(FlexibleBooleanAdapter())
        .add(FlexibleStringAdapter())
        .addLast(KotlinJsonAdapterFactory())
        .build()

    val api: SupabaseApi
        get() {
            return retrofitApi ?: synchronized(this) {
                retrofitApi ?: Retrofit.Builder()
                    .baseUrl(activeUrl)
                    .client(okHttpClient)
                    .addConverterFactory(MoshiConverterFactory.create(moshi))
                    .build()
                    .create(SupabaseApi::class.java)
                    .also { retrofitApi = it }
            }
        }

}
