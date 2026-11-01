package com.example.error

import com.example.util.ErrorLogger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.PrintWriter
import java.io.StringWriter
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

object AppErrorDispatcher {

    private val _latestError = MutableStateFlow<AppError?>(null)
    val latestError: StateFlow<AppError?> = _latestError.asStateFlow()

    private val _errorHistory = MutableStateFlow<List<AppError>>(emptyList())
    val errorHistory: StateFlow<List<AppError>> = _errorHistory.asStateFlow()

    fun dispatch(error: AppError) {
        ErrorLogger.logMessage("AppError Dispatched [${error.category.name}]: ${error.title} - ${error.technicalCause}", "AppErrorDispatcher")
        _latestError.value = error
        val currentList = _errorHistory.value.toMutableList()
        currentList.add(0, error)
        if (currentList.size > 50) {
            currentList.removeAt(currentList.lastIndex)
        }
        _errorHistory.value = currentList
    }

    fun clearLatestError() {
        _latestError.value = null
    }

    fun clearHistory() {
        _errorHistory.value = emptyList()
        _latestError.value = null
    }

    /**
     * URL / Video Embed Issue Error Dispatcher
     */
    fun dispatchUrlError(
        url: String,
        reason: String,
        isIframeCode: Boolean = false
    ) {
        dispatch(
            AppError(
                category = AppErrorCategory.URL_STREAM,
                title = "Video URL / Stream Source Issue",
                userFriendlyMessage = "Apnar video stream load hocche na. Please search/retry alternate video server.",
                creatorDiagnosticMessage = "Apnar URL problems: ${if (isIframeCode) "Embed code inside episode is malformed or iframe player failed to parse." else "Provided video stream URL returned invalid format or 404 stream stall."} Re-check URL path in Episode Manager.",
                technicalCause = "URL: $url | Reason: $reason",
                suggestedAction = "Apnar episode video URL or iframe code test korun. Change URL in Creator Studio -> Series Episodes.",
                affectedResource = url
            )
        )
    }

    /**
     * Database / Table / Column Schema Error Dispatcher
     */
    fun dispatchDatabaseError(
        message: String,
        tableName: String? = null,
        missingColumn: String? = null,
        cause: Throwable? = null
    ) {
        val category = if (missingColumn != null || message.contains("column", ignoreCase = true) || message.contains("table", ignoreCase = true)) {
            AppErrorCategory.SCHEMA_COLUMN
        } else if (message.contains("permission", ignoreCase = true) || message.contains("denied", ignoreCase = true)) {
            AppErrorCategory.POLICY_PERMISSION
        } else {
            AppErrorCategory.DATABASE
        }

        val creatorMsg = when (category) {
            AppErrorCategory.SCHEMA_COLUMN -> "Apnar ey columns/tables missing: ${missingColumn ?: tableName ?: "Unknown column/table"}. Database schema mismatch."
            AppErrorCategory.POLICY_PERMISSION -> "Apnar policy problems: Database access was denied due to missing role permissions or security rules."
            else -> "Apnar database/server issue: $message"
        }

        dispatch(
            AppError(
                category = category,
                title = "Database / Schema Error",
                userFriendlyMessage = "Database operation failed. Please try again.",
                creatorDiagnosticMessage = creatorMsg,
                technicalCause = message,
                suggestedAction = "Verify table structure (${tableName ?: "all"}), field columns (${missingColumn ?: "schema"}), and Firestore/Supabase rules.",
                affectedResource = tableName,
                stackTrace = cause?.let { getStackTraceString(it) }
            )
        )
    }

    /**
     * Auth & Password Error Dispatcher
     */
    fun dispatchAuthError(
        reason: String,
        isWrongPassword: Boolean = false
    ) {
        dispatch(
            AppError(
                category = AppErrorCategory.AUTH_PASSWORD,
                title = if (isWrongPassword) "Incorrect Password" else "Authentication Failure",
                userFriendlyMessage = if (isWrongPassword) "Rong pass / Incorrect password. Please try again." else "Authentication failed. Please sign in again.",
                creatorDiagnosticMessage = if (isWrongPassword) "Apnar password vul: Provided authentication credentials failed validation check." else "Apnar session/token expired or auth policy rejected credentials.",
                technicalCause = reason,
                suggestedAction = "Reset user password or clear cached session tokens in Account Settings.",
                affectedResource = "User Session / Credentials"
            )
        )
    }

    /**
     * Network & Connectivity Error Dispatcher
     */
    fun dispatchNetworkError(
        reason: String,
        endpoint: String? = null
    ) {
        dispatch(
            AppError(
                category = AppErrorCategory.NETWORK,
                title = "Network Connection Lost",
                userFriendlyMessage = "Network problem. Please check your internet connection.",
                creatorDiagnosticMessage = "Apnar server/network issue: Connection to server or CDN timed out / unreachable.",
                technicalCause = "Endpoint: ${endpoint ?: "Remote API"} | Error: $reason",
                suggestedAction = "Check device internet, DNS settings, or backend server uptime.",
                affectedResource = endpoint
            )
        )
    }

    /**
     * Backend Server 5xx Error Dispatcher
     */
    fun dispatchServerError(
        statusCode: Int,
        message: String,
        endpoint: String? = null
    ) {
        dispatch(
            AppError(
                category = AppErrorCategory.SERVER,
                title = "Server Response Failure (HTTP $statusCode)",
                userFriendlyMessage = "Server issue. Please try again in a few moments.",
                creatorDiagnosticMessage = "Apnar server issue: Remote server returned error status $statusCode ($message).",
                technicalCause = "HTTP $statusCode | $message",
                suggestedAction = "Inspect backend API logs and verify endpoint URL ($endpoint).",
                affectedResource = endpoint
            )
        )
    }

    /**
     * Security Policy Error Dispatcher
     */
    fun dispatchPolicyError(
        policyName: String,
        details: String
    ) {
        dispatch(
            AppError(
                category = AppErrorCategory.POLICY_PERMISSION,
                title = "Security & Access Policy Violation",
                userFriendlyMessage = "Access restricted under current user permissions.",
                creatorDiagnosticMessage = "Apnar policy problems: Action blocked by security policy rule '$policyName'. $details",
                technicalCause = "Policy: $policyName | Details: $details",
                suggestedAction = "Check Admin Studio -> Roles & Permissions policy rules.",
                affectedResource = policyName
            )
        )
    }

    /**
     * Automatic Exception Translator
     */
    fun dispatchException(
        throwable: Throwable,
        context: String? = null,
        affectedResource: String? = null
    ) {
        val msg = throwable.localizedMessage ?: throwable.message ?: "Unknown runtime exception"

        when {
            throwable is UnknownHostException || throwable is SocketTimeoutException || throwable is ConnectException -> {
                dispatchNetworkError(msg, affectedResource)
            }
            msg.contains("password", ignoreCase = true) || msg.contains("auth", ignoreCase = true) -> {
                dispatchAuthError(msg, isWrongPassword = msg.contains("invalid password", ignoreCase = true) || msg.contains("wrong", ignoreCase = true))
            }
            msg.contains("column", ignoreCase = true) || msg.contains("table", ignoreCase = true) || msg.contains("schema", ignoreCase = true) -> {
                dispatchDatabaseError(msg, affectedResource, cause = throwable)
            }
            msg.contains("url", ignoreCase = true) || msg.contains("http", ignoreCase = true) || msg.contains("embed", ignoreCase = true) -> {
                dispatchUrlError(affectedResource ?: "Unknown URL", msg)
            }
            else -> {
                dispatch(
                    AppError(
                        category = AppErrorCategory.GENERAL,
                        title = "Application Exception (${throwable.javaClass.simpleName})",
                        userFriendlyMessage = "An unexpected issue occurred ($context).",
                        creatorDiagnosticMessage = "Apnar unexpected application exception: $msg at $context.",
                        technicalCause = msg,
                        suggestedAction = "Inspect stack trace or report bug to technical admin.",
                        affectedResource = affectedResource,
                        stackTrace = getStackTraceString(throwable)
                    )
                )
            }
        }
    }

    private fun getStackTraceString(throwable: Throwable): String {
        val sw = StringWriter()
        val pw = PrintWriter(sw)
        throwable.printStackTrace(pw)
        return sw.toString()
    }
}
