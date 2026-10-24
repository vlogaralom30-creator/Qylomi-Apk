package com.example.data.model

import org.json.JSONObject

sealed class SeriesResult<out T> {
    data class Success<out T>(val data: T) : SeriesResult<T>()
    data class Error(
        val message: String,
        val code: String? = null,
        val statusCode: Int? = null,
        val cause: Throwable? = null
    ) : SeriesResult<Nothing>() {
        val isPermissionDenied: Boolean
            get() = statusCode == 403 || code == "42501"

        val isConflict: Boolean
            get() = statusCode == 409 || code == "23505"

        val isNotFoundOrDeleted: Boolean
            get() = statusCode == 404 || code == "23503"

        val isLastAdminError: Boolean
            get() = code == "P0001"
    }

    val isSuccess: Boolean get() = this is Success
    fun getOrNull(): T? = (this as? Success)?.data
    fun getOrDefault(defaultValue: @UnsafeVariance T): T = (this as? Success)?.data ?: defaultValue

    inline fun <R> map(transform: (T) -> R): SeriesResult<R> {
        return when (this) {
            is Success -> Success(transform(data))
            is Error -> this
        }
    }

    companion object {
        fun <T> success(data: T): SeriesResult<T> = Success(data)
        fun failure(
            message: String,
            code: String? = null,
            statusCode: Int? = null,
            cause: Throwable? = null
        ): SeriesResult<Nothing> = Error(message, code, statusCode, cause)

        fun parseError(rawBody: String?, httpStatus: Int, defaultMessage: String = "An error occurred"): Error {
            var code: String? = null
            var parsedMsg: String? = null

            if (!rawBody.isNullOrBlank()) {
                code = "\"code\"\\s*:\\s*\"([^\"]+)\"".toRegex().find(rawBody)?.groupValues?.getOrNull(1)
                parsedMsg = "\"message\"\\s*:\\s*\"([^\"]+)\"".toRegex().find(rawBody)?.groupValues?.getOrNull(1)
                    ?: "\"error_description\"\\s*:\\s*\"([^\"]+)\"".toRegex().find(rawBody)?.groupValues?.getOrNull(1)
                    ?: "\"details\"\\s*:\\s*\"([^\"]+)\"".toRegex().find(rawBody)?.groupValues?.getOrNull(1)

                if (code.isNullOrBlank() || parsedMsg.isNullOrBlank()) {
                    try {
                        val json = JSONObject(rawBody)
                        if (code.isNullOrBlank()) code = json.optString("code").takeIf { it.isNotBlank() }
                        if (parsedMsg.isNullOrBlank()) {
                            parsedMsg = json.optString("message").takeIf { it.isNotBlank() }
                                ?: json.optString("error_description").takeIf { it.isNotBlank() }
                                ?: json.optString("details").takeIf { it.isNotBlank() }
                                ?: json.optString("msg").takeIf { it.isNotBlank() }
                        }
                    } catch (_: Throwable) {
                        if (rawBody.length < 150 && !rawBody.startsWith("<")) {
                            parsedMsg = rawBody
                        }
                    }
                }
            }

            val friendlyMessage = when {
                code == "P0001" || parsedMsg?.contains("last admin", ignoreCase = true) == true ->
                    "Cannot remove the last admin."
                code == "42501" || httpStatus == 403 || parsedMsg?.contains("permission denied", ignoreCase = true) == true ->
                    "You don't have permission to perform this action."
                code == "23505" || httpStatus == 409 || parsedMsg?.contains("duplicate key", ignoreCase = true) == true ->
                    "Already exists (duplicate slug or episode number)."
                code == "23503" || parsedMsg?.contains("foreign key", ignoreCase = true) == true ->
                    "Related item no longer exists."
                code == "22023" || code == "23514" ->
                    parsedMsg ?: "Validation failed: please check input fields."
                httpStatus == 401 ->
                    "Session expired or unauthorized. Please sign in."
                httpStatus in 500..599 ->
                    "Server error. Please try again shortly."
                !parsedMsg.isNullOrBlank() ->
                    parsedMsg
                else ->
                    defaultMessage
            }

            return Error(
                message = friendlyMessage,
                code = code,
                statusCode = httpStatus
            )
        }
    }
}
