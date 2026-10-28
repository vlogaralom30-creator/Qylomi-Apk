package com.example.data.api

import com.example.data.model.DoodAccountInfoResponse
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

interface DoodStreamApi {
    @GET("account/info")
    suspend fun getAccountInfo(@Query("key") apiKey: String): Response<DoodAccountInfoResponse>

    @GET("upload/url")
    suspend fun remoteUpload(
        @Query("key") apiKey: String,
        @Query("url") url: String,
        @Query("new_title") newTitle: String? = null
    ): Response<Map<String, Any?>>

    @GET("file/list")
    suspend fun getFileList(
        @Query("key") apiKey: String,
        @Query("page") page: Int? = null,
        @Query("per_page") perPage: Int? = null,
        @Query("fld_id") fldId: String? = null
    ): Response<Map<String, Any?>>

    @GET("search/videos")
    suspend fun searchVideos(
        @Query("key") apiKey: String,
        @Query("search_term") searchTerm: String
    ): Response<Map<String, Any?>>

    @GET("file/info")
    suspend fun getFileInfo(
        @Query("key") apiKey: String,
        @Query("file_code") fileCode: String
    ): Response<Map<String, Any?>>
}

object DoodStreamClient {
    private const val BASE_URL = "https://doodapi.co/api/"

    private val logging = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val client = OkHttpClient.Builder()
        .addInterceptor(logging)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    val api: DoodStreamApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create())
            .build()
            .create(DoodStreamApi::class.java)
    }
}
