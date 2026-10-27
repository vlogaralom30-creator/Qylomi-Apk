package com.example

import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import com.example.data.api.SessionManager
import com.example.util.VideoPlayerCache
import java.io.File

class QylomiApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        try {
            val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
            Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
                try {
                    android.util.Log.e("QylomiApplication", "Uncaught exception in thread ${thread.name}: ${throwable.message}", throwable)
                } catch (_: Throwable) {}
                defaultHandler?.uncaughtException(thread, throwable)
            }

            // Version change migration & cache clearance
            checkAndClearAppCacheOnVersionUpdate()

            // Initialize SessionManager with application context
            SessionManager.getInstance(this)
        } catch (_: Throwable) {}
    }

    private fun checkAndClearAppCacheOnVersionUpdate() {
        try {
            val prefs = getSharedPreferences("qylomi_app_migration_prefs", Context.MODE_PRIVATE)
            val currentVersionCode = getAppVersionCode()
            val savedVersionCode = prefs.getInt("last_version_code", -1)

            if (savedVersionCode != -1 && savedVersionCode != currentVersionCode) {
                android.util.Log.i("QylomiApplication", "Version change detected ($savedVersionCode -> $currentVersionCode). Clearing stale Media3 & WebView caches.")
                
                // Clear Media3 cache
                VideoPlayerCache.clearCache(this)

                // Clear WebView cache directory & webview DB
                try {
                    deleteRecursivelySafely(File(cacheDir, "WebView"))
                    deleteRecursivelySafely(File(cacheDir, "org.chromium.android_webview"))
                    val webViewDbDir = File(applicationInfo.dataDir, "app_webview")
                    deleteRecursivelySafely(webViewDbDir)
                } catch (_: Exception) {}
            }

            prefs.edit().putInt("last_version_code", currentVersionCode).apply()
        } catch (_: Exception) {}
    }

    private fun getAppVersionCode(): Int {
        return try {
            val pInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                packageManager.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                packageManager.getPackageInfo(packageName, 0)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                pInfo.longVersionCode.toInt()
            } else {
                @Suppress("DEPRECATION")
                pInfo.versionCode
            }
        } catch (_: Exception) {
            1
        }
    }

    private fun deleteRecursivelySafely(file: File) {
        if (file.exists()) {
            file.deleteRecursively()
        }
    }
}
