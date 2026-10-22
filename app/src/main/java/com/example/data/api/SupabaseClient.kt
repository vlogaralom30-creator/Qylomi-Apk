package com.example.data.api

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.Interceptor
import okhttp3.OkHttpClient
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
        val requestBuilder = original.newBuilder()
            .header("apikey", activeKey)
            .header("Content-Type", "application/json")

        // If no Authorization header is present on the request, add default
        val existingAuth = original.header("Authorization")
        if (existingAuth.isNullOrBlank()) {
            val token = authToken
            if (!token.isNullOrBlank()) {
                requestBuilder.header("Authorization", "Bearer $token")
            } else {
                requestBuilder.header("Authorization", "Bearer $activeKey")
            }
        }

        chain.proceed(requestBuilder.build())
    }

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(authInterceptor)
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
