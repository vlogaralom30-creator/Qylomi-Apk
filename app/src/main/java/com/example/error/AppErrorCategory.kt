package com.example.error

enum class AppErrorCategory(
    val displayName: String,
    val iconResName: String,
    val tagColorHex: String
) {
    DATABASE("Database", "storage", "#FF5252"),
    URL_STREAM("URL & Video Stream", "play_circle", "#FF9800"),
    AUTH_PASSWORD("Auth & Password", "lock", "#E91E63"),
    NETWORK("Network Connection", "wifi_off", "#00BCD4"),
    SERVER("Backend Server", "dns", "#9C27B0"),
    POLICY_PERMISSION("Security Policy", "shield", "#F44336"),
    SCHEMA_COLUMN("Database Schema", "table_chart", "#795548"),
    GENERAL("Application Error", "error_outline", "#607D8B")
}
