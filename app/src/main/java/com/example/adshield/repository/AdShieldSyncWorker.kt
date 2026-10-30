package com.example.adshield.repository

import android.content.Context
import android.util.Log
import com.example.adshield.model.AdShieldConfig
import com.example.util.VideoPlayerCache
import okhttp3.Request
import java.io.InputStream

class AdShieldSyncWorker {

    companion object {
        private const val TAG = "AdShieldSyncWorker"
        private const val MAX_CONFIG_SIZE_BYTES = 256 * 1024 // 256 KB Limit

        suspend fun syncRemoteConfig(context: Context, remoteUrl: String): Boolean {
            if (remoteUrl.isBlank() || !remoteUrl.startsWith("https://")) {
                return false
            }

            return try {
                val request = Request.Builder()
                    .url(remoteUrl)
                    .header("User-Agent", "AdShieldv2/1.0 (Android)")
                    .get()
                    .build()

                val response = VideoPlayerCache.okHttpClient.newCall(request).execute()
                if (!response.isSuccessful) {
                    response.close()
                    return false
                }

                val body = response.body
                val contentLength = body?.contentLength() ?: -1L
                if (contentLength > MAX_CONFIG_SIZE_BYTES) {
                    Log.w(TAG, "Remote config exceeds 256KB limit: $contentLength bytes")
                    response.close()
                    return false
                }

                val inputStream: InputStream = body?.byteStream() ?: return false
                val bytes = inputStream.readNBytes(MAX_CONFIG_SIZE_BYTES + 1)
                response.close()

                if (bytes.size > MAX_CONFIG_SIZE_BYTES) {
                    Log.w(TAG, "Downloaded config exceeds 256KB limit")
                    return false
                }

                val jsonString = String(bytes, Charsets.UTF_8)
                val newConfig = AdShieldConfig.parseJson(jsonString)
                AdShieldConfigRepository.saveConfig(context, newConfig)
                Log.i(TAG, "Successfully updated AdShield config from remote URL. Version: ${newConfig.version}")
                true
            } catch (e: Exception) {
                Log.e(TAG, "Failed remote config sync", e)
                false
            }
        }
    }
}
