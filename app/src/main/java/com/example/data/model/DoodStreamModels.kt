package com.example.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class DoodAccountInfoResponse(
    val msg: String?,
    val status: Int?,
    @Json(name = "server_time") val serverTime: String?,
    val result: DoodAccountResult?
)

@JsonClass(generateAdapter = true)
data class DoodAccountResult(
    val email: String?,
    val balance: String?,
    @Json(name = "storage_used") val storageUsed: String?,
    @Json(name = "storage_left") val storageLeft: Long?,
    @Json(name = "premim_expire") val premiumExpire: String?
)

@JsonClass(generateAdapter = true)
data class DoodFileItem(
    @Json(name = "file_code") val fileCode: String?,
    val title: String?,
    @Json(name = "download_url") val downloadUrl: String?,
    @Json(name = "single_img") val singleImg: String?,
    @Json(name = "protected_embed") val protectedEmbed: String?,
    val canplay: Int?,
    val length: String?,
    val views: String?,
    val uploaded: String?
)
