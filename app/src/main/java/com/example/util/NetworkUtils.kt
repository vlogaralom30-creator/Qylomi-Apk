package com.example.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities

object NetworkUtils {
    enum class ConnectionType { WIFI, MOBILE, OFFLINE_OR_DATA_SAVER }

    fun getConnectionType(context: Context): ConnectionType {
        try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return ConnectionType.WIFI

            // Check Data Saver
            if (cm.isActiveNetworkMetered && cm.restrictBackgroundStatus == ConnectivityManager.RESTRICT_BACKGROUND_STATUS_ENABLED) {
                return ConnectionType.OFFLINE_OR_DATA_SAVER
            }

            val activeNetwork = cm.activeNetwork ?: return ConnectionType.OFFLINE_OR_DATA_SAVER
            val caps = cm.getNetworkCapabilities(activeNetwork) ?: return ConnectionType.OFFLINE_OR_DATA_SAVER

            return when {
                caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) || caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> ConnectionType.WIFI
                caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> ConnectionType.MOBILE
                else -> ConnectionType.OFFLINE_OR_DATA_SAVER
            }
        } catch (_: Exception) {
            return ConnectionType.WIFI
        }
    }

    fun isOnline(context: Context): Boolean {
        try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return true
            val activeNetwork = cm.activeNetwork ?: return false
            val caps = cm.getNetworkCapabilities(activeNetwork) ?: return false
            return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } catch (_: Exception) {
            return true
        }
    }
}
