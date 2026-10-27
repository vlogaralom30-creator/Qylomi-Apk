package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import com.example.BuildConfig
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class ImgBbResponse(
    val data: ImgBbData?,
    val success: Boolean,
    val status: Int?
)

@JsonClass(generateAdapter = true)
data class ImgBbData(
    val id: String?,
    val title: String?,
    val url: String?,
    val display_url: String?,
    val delete_url: String?
)

object ImgBbUploader {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
    private val adapter = moshi.adapter(ImgBbResponse::class.java)

    /**
     * Upload an image by Uri to ImgBB and return direct image URL.
     */
    suspend fun uploadImage(context: Context, imageUri: Uri, name: String? = null): Result<String> = withContext(Dispatchers.IO) {
        val apiKey = try {
            val field = BuildConfig::class.java.getField("IMGBB_API_KEY")
            val key = field.get(null) as? String
            key?.takeIf { it.isNotBlank() }
        } catch (_: Exception) {
            null
        }

        if (apiKey.isNullOrBlank()) {
            return@withContext Result.failure(
                IllegalStateException("ImgBB API key is not configured. Please add IMGBB_API_KEY in the Secrets panel.")
            )
        }

        try {
            val inputStream: InputStream? = context.contentResolver.openInputStream(imageUri)
                ?: return@withContext Result.failure(IllegalArgumentException("Cannot open image URI"))

            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()

            if (bitmap == null) {
                return@withContext Result.failure(IllegalArgumentException("Failed to decode image bitmap"))
            }

            // Compress to JPEG
            val outputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
            val bytes = outputStream.toByteArray()
            val base64Image = Base64.encodeToString(bytes, Base64.NO_WRAP)

            uploadBase64(apiKey, base64Image, name)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Upload raw Base64 image data to ImgBB.
     */
    suspend fun uploadBase64(apiKey: String, base64Data: String, name: String? = null): Result<String> = withContext(Dispatchers.IO) {
        try {
            val formBuilder = FormBody.Builder()
                .add("key", apiKey)
                .add("image", base64Data)

            if (!name.isNullOrBlank()) {
                formBuilder.add("name", name)
            }

            val request = Request.Builder()
                .url("https://api.imgbb.com/1/upload")
                .post(formBuilder.build())
                .build()

            val response = client.newCall(request).execute()
            val bodyString = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                return@withContext Result.failure(
                    IllegalStateException("ImgBB upload failed with HTTP ${response.code}: $bodyString")
                )
            }

            val parsed = adapter.fromJson(bodyString)
            val uploadedUrl = parsed?.data?.display_url ?: parsed?.data?.url
            if (uploadedUrl != null) {
                Result.success(uploadedUrl)
            } else {
                Result.failure(IllegalStateException("ImgBB response did not contain image URL"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
