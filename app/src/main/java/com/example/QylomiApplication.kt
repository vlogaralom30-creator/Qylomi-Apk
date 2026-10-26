package com.example

import android.app.Application
import com.example.data.api.SessionManager

class QylomiApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        try {
            // Initialize SessionManager with application context
            SessionManager.getInstance(this)
        } catch (_: Throwable) {}
    }
}
