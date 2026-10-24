package com.example.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class AuthSignUpRequest(
    @Json(name = "email") val email: String,
    @Json(name = "password") val password: String,
    @Json(name = "data") val data: Map<String, String>? = null
)

@JsonClass(generateAdapter = true)
data class AuthSignInRequest(
    @Json(name = "email") val email: String,
    @Json(name = "password") val password: String
)

@JsonClass(generateAdapter = true)
data class AuthResponse(
    @Json(name = "access_token") val accessToken: String? = null,
    @Json(name = "token_type") val tokenType: String? = null,
    @Json(name = "expires_in") val expiresIn: Long? = null,
    @Json(name = "refresh_token") val refreshToken: String? = null,
    @Json(name = "user") val user: SupabaseUser? = null
) {
    fun toSession(): Session? {
        val token = accessToken ?: return null
        val uid = user?.id ?: ""
        val mail = user?.email ?: ""
        val refresh = refreshToken ?: ""
        val expiresAt = (System.currentTimeMillis() / 1000) + (expiresIn ?: 3600L)
        return Session(
            userId = uid,
            email = mail,
            accessToken = token,
            refreshToken = refresh,
            expiresAtEpochSec = expiresAt
        )
    }
}

@JsonClass(generateAdapter = true)
data class Session(
    @Json(name = "user_id") val userId: String = "",
    @Json(name = "email") val email: String = "",
    @Json(name = "access_token") val accessToken: String = "",
    @Json(name = "refresh_token") val refreshToken: String = "",
    @Json(name = "expires_at") val expiresAtEpochSec: Long = 0L
)

@JsonClass(generateAdapter = true)
data class UserMetadata(
    @Json(name = "username") val username: String? = null,
    @Json(name = "display_name") val displayName: String? = null,
    @Json(name = "name") val name: String? = null,
    @Json(name = "avatar_url") val avatarUrl: String? = null,
    @Json(name = "email") val email: String? = null,
    @Json(name = "email_verified") val emailVerified: Boolean? = null,
    @Json(name = "phone_verified") val phoneVerified: Boolean? = null,
    @Json(name = "sub") val sub: String? = null
)

@JsonClass(generateAdapter = true)
data class AppMetadata(
    @Json(name = "provider") val provider: String? = null,
    @Json(name = "providers") val providers: List<String>? = null
)

@JsonClass(generateAdapter = true)
data class SupabaseUser(
    @Json(name = "id") val id: String = "",
    @Json(name = "email") val email: String? = null,
    @Json(name = "phone") val phone: String? = null,
    @Json(name = "role") val role: String? = null,
    @Json(name = "user_metadata") val userMetadata: UserMetadata? = null,
    @Json(name = "app_metadata") val appMetadata: AppMetadata? = null,
    @Json(name = "created_at") val createdAt: String? = null,
    @Json(name = "updated_at") val updatedAt: String? = null
) {
    val username: String?
        get() = userMetadata?.username

    val displayName: String?
        get() = userMetadata?.displayName ?: userMetadata?.name
}

