package com.example.util

import android.util.Log

object ErrorLogger {
    private const val TAG = "QylomiErrorTracker"

    fun logException(throwable: Throwable, tag: String = TAG, message: String? = null) {
        val msg = message ?: throwable.localizedMessage ?: "Unhandled runtime exception"
        Log.e(tag, "FATAL/ERROR: $msg", throwable)
        // Centralized tracking point for production logging or reporting
    }

    fun logMessage(message: String, tag: String = TAG) {
        Log.i(tag, message)
    }
}
