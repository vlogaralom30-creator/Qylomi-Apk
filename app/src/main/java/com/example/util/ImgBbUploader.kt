package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import com.example.util.VideoPlayerCache.okHttpClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.Request
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.InputStream

object ImgBbUploader {
    private const val API_KEY = "69398385bcad0a0ef63957ec1b3b53f4"
    private const val UPLOAD_ENDPOINT = "https://api.imgbb.com/1/upload"

    /**
     * Compress image from [Uri] and upload directly to ImgBB API v1.
     * Returns direct image URL (e.g. https://i.ibb.co/...) on success.
     */
    suspend fun uploadImage(context: Context, imageUri: Uri): Result<String> = withContext(Dispatchers.IO) {
        try {
            val compressedBytes = compressImageUri(context, imageUri)
                ?: return@withContext Result.failure(IllegalArgumentException("Failed to decode or compress selected image"))

            val base64Data = Base64.encodeToString(compressedBytes, Base64.NO_WRAP)

            val formBody = FormBody.Builder()
                .add("key", API_KEY)
                .add("image", base64Data)
                .build()

            val request = Request.Builder()
                .url(UPLOAD_ENDPOINT)
                .post(formBody)
                .build()

            val response = okHttpClient.newCall(request).execute()
            val responseBodyStr = response.body?.string()
            response.close()

            if (!response.isSuccessful || responseBodyStr.isNullOrBlank()) {
                return@withContext Result.failure(Exception("ImgBB upload failed with HTTP status ${response.code}"))
            }

            val json = JSONObject(responseBodyStr)
            val success = json.optBoolean("success", false)

            if (success) {
                val data = json.getJSONObject("data")
                var directUrl = data.optString("url")
                if (directUrl.isBlank()) {
                    directUrl = data.optString("display_url")
                }
                if (directUrl.isNotBlank()) {
                    Result.success(directUrl)
                } else {
                    Result.failure(Exception("ImgBB API returned invalid image URL"))
                }
            } else {
                val statusMsg = json.optJSONObject("error")?.optString("message") ?: "Unknown ImgBB API error"
                Result.failure(Exception(statusMsg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Compress imageUri to JPEG byte array under maxDimensions (1920px) and quality (~82%).
     */
    private fun compressImageUri(context: Context, uri: Uri): ByteArray? {
        return try {
            val contentResolver = context.contentResolver

            // 1. Decode bounds first
            var inputStream: InputStream? = contentResolver.openInputStream(uri)
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeStream(inputStream, null, options)
            inputStream?.close()

            val origWidth = options.outWidth
            val origHeight = options.outHeight

            if (origWidth <= 0 || origHeight <= 0) return null

            // 2. Calculate inSampleSize
            val maxDimension = 1920
            var sampleSize = 1
            if (origWidth > maxDimension || origHeight > maxDimension) {
                val halfWidth = origWidth / 2
                val halfHeight = origHeight / 2
                while ((halfWidth / sampleSize) >= maxDimension || (halfHeight / sampleSize) >= maxDimension) {
                    sampleSize *= 2
                }
            }

            // 3. Decode bitmap with sampleSize
            inputStream = contentResolver.openInputStream(uri)
            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            val originalBitmap = BitmapFactory.decodeStream(inputStream, null, decodeOptions)
            inputStream?.close()

            if (originalBitmap == null) return null

            // 4. Scale down if necessary
            val bitmapToCompress = if (originalBitmap.width > maxDimension || originalBitmap.height > maxDimension) {
                val ratio = Math.min(
                    maxDimension.toFloat() / originalBitmap.width,
                    maxDimension.toFloat() / originalBitmap.height
                )
                val newWidth = Math.round(originalBitmap.width * ratio)
                val newHeight = Math.round(originalBitmap.height * ratio)
                val scaled = Bitmap.createScaledBitmap(originalBitmap, newWidth, newHeight, true)
                if (scaled != originalBitmap) {
                    originalBitmap.recycle()
                }
                scaled
            } else {
                originalBitmap
            }

            // 5. Compress to JPEG
            val baos = ByteArrayOutputStream()
            bitmapToCompress.compress(Bitmap.CompressFormat.JPEG, 82, baos)
            val bytes = baos.toByteArray()
            bitmapToCompress.recycle()
            bytes
        } catch (_: Exception) {
            null
        }
    }
}
