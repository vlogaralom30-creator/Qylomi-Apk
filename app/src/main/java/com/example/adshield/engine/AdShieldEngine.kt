package com.example.adshield.engine

import android.webkit.WebResourceResponse
import com.example.adshield.model.AdShieldConfig
import com.example.adshield.model.PlayerProfile
import com.example.adshield.model.RequestDecision
import java.io.ByteArrayInputStream
import java.net.URI
import java.util.Collections
import java.util.LinkedHashMap
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicReference

object AdShieldEngine {

    @Volatile
    var isEnabled: Boolean = true

    private val currentConfig = AtomicReference<AdShieldConfig>(AdShieldConfig())

    // Internal quick lookup data structures derived from config
    private val adPermissionsSet = ConcurrentHashMap.newKeySet<String>()
    private val globalAllowSet = ConcurrentHashMap.newKeySet<String>()
    private val hardBlockPathsList = CopyOnWriteList<String>()
    private val hardBlockQueryKeysList = CopyOnWriteList<String>()

    // Profiles indexed by ID
    private val profilesMap = ConcurrentHashMap<String, PlayerProfile>()

    // Thread-safe LRU Cache for decisions
    private val decisionCache: MutableMap<String, RequestDecision> = Collections.synchronizedMap(
        object : LinkedHashMap<String, RequestDecision>(100, 0.75f, true) {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, RequestDecision>?): Boolean {
                return size > 500
            }
        }
    )

    // Media file extension set
    private val mediaExtensions = setOf("mp4", "m3u8", "ts", "m4s", "mpd", "webm", "vtt")

    init {
        updateConfig(AdShieldConfig())
    }

    fun updateConfig(config: AdShieldConfig) {
        currentConfig.set(config)
        decisionCache.clear()

        adPermissionsSet.clear()
        config.adPermissions.forEach { adPermissionsSet.add(sanitizeHost(it)) }

        globalAllowSet.clear()
        config.globalAllow.forEach { globalAllowSet.add(sanitizeHost(it)) }

        hardBlockPathsList.clear()
        hardBlockPathsList.addAll(config.hardBlockPaths.map { it.lowercase(Locale.ROOT) })

        hardBlockQueryKeysList.clear()
        hardBlockQueryKeysList.addAll(config.hardBlockQueryKeys.map { it.lowercase(Locale.ROOT) })

        profilesMap.clear()
        config.profiles.forEach { profilesMap[it.id] = it }
    }

    fun getConfig(): AdShieldConfig = currentConfig.get()

    /**
     * Core decision method evaluated for every WebView request.
     * Order of evaluation:
     * 1. Data/Blob/About URIs -> PASS (Handled by JS hardening)
     * 2. Host in adPermissions -> ALLOW ("permitted ad")
     * 3. Hard-block path or query rule match -> BLOCK
     * 4. Media stream file extension check -> Must match active profile CDN
     * 5. Active Profile allow list match -> ALLOW
     * 6. Global allow list match -> ALLOW
     * 7. Fallback -> BLOCK (Default Deny)
     */
    fun evaluateRequest(rawUrl: String, activeProfileId: String?): RequestDecision {
        if (!isEnabled) {
            val host = extractHost(rawUrl) ?: "unknown"
            return RequestDecision.Allowed(rawUrl, host, "Ad Shield Disabled")
        }

        val trimmed = rawUrl.trim()
        val lowerUrl = trimmed.lowercase(Locale.ROOT)

        if (lowerUrl.startsWith("data:") || lowerUrl.startsWith("blob:") || lowerUrl.startsWith("about:")) {
            return RequestDecision.Allowed(trimmed, "local", "System Scheme")
        }

        val host = extractHost(trimmed)
            ?: return RequestDecision.Blocked(trimmed, "unknown", "Invalid Host")

        val sanitizedHost = sanitizeHost(host)
        val activeProfile = activeProfileId?.let { profilesMap[it] }

        // 1. Host in adPermissions
        if (adPermissionsSet.contains(sanitizedHost) || matchesHashSetParentDomain(sanitizedHost, adPermissionsSet)) {
            return RequestDecision.Allowed(trimmed, sanitizedHost, "Permitted Ad Host", isPermittedAd = true)
        }

        // 2. Hard-block path or query check
        if (matchesHardBlockPathOrQuery(trimmed, lowerUrl)) {
            return RequestDecision.Blocked(trimmed, sanitizedHost, "Hard Block Rule Match")
        }

        // 3. Media stream check (.mp4, .m3u8, .ts, etc.)
        val extension = extractFileExtension(lowerUrl)
        if (extension != null && mediaExtensions.contains(extension)) {
            val isProfileMediaHost = activeProfile != null && (
                matchesHostList(sanitizedHost, activeProfile.mediaCdnHosts) ||
                matchesHostList(sanitizedHost, activeProfile.allowedHosts)
            )
            return if (isProfileMediaHost) {
                RequestDecision.Allowed(trimmed, sanitizedHost, "Active Profile Media CDN")
            } else {
                RequestDecision.Blocked(trimmed, sanitizedHost, "Unapproved Media Stream Host")
            }
        }

        // 4. Active Profile Allow Match
        if (activeProfile != null && matchesHostList(sanitizedHost, activeProfile.allowedHosts)) {
            return RequestDecision.Allowed(trimmed, sanitizedHost, "Profile Allowed Host")
        }

        // 5. Global Allow Match
        if (globalAllowSet.contains(sanitizedHost) || matchesHashSetParentDomain(sanitizedHost, globalAllowSet)) {
            return RequestDecision.Allowed(trimmed, sanitizedHost, "Global Allow List")
        }

        // 6. Default Deny
        return RequestDecision.Blocked(trimmed, sanitizedHost, "Default Deny Rule")
    }

    /**
     * Resolves the non-null 403 WebResourceResponse for blocked requests.
     */
    fun createBlockResponse(): WebResourceResponse {
        return WebResourceResponse(
            "text/plain",
            "utf-8",
            403,
            "Blocked by Ad Shield v2",
            emptyMap(),
            ByteArrayInputStream(ByteArray(0))
        )
    }

    fun sanitizeHost(rawHost: String): String {
        var h = rawHost.lowercase(Locale.ROOT).trim()
        if (h.startsWith("*.")) h = h.substring(2)
        val portIndex = h.indexOf(':')
        if (portIndex != -1) {
            h = h.substring(0, portIndex)
        }
        return h
    }

    fun extractHost(urlStr: String): String? {
        return try {
            val uri = URI(urlStr)
            val host = uri.host
            if (!host.isNullOrBlank()) {
                sanitizeHost(host)
            } else {
                extractHostManual(urlStr)
            }
        } catch (_: Exception) {
            extractHostManual(urlStr)
        }
    }

    private fun extractHostManual(urlStr: String): String? {
        val schemeIndex = urlStr.indexOf("://")
        if (schemeIndex == -1) return null
        val afterScheme = urlStr.substring(schemeIndex + 3)
        val slashIndex = afterScheme.indexOf('/')
        val hostAndPort = if (slashIndex != -1) afterScheme.substring(0, slashIndex) else afterScheme
        if (hostAndPort.isBlank()) return null
        return sanitizeHost(hostAndPort)
    }

    private fun matchesHostList(host: String, hostPatterns: List<String>): Boolean {
        for (pattern in hostPatterns) {
            val cleanPattern = sanitizeHost(pattern)
            if (host == cleanPattern || (pattern.startsWith("*.") && host.endsWith(".$cleanPattern"))) {
                return true
            }
        }
        return false
    }

    private fun matchesHashSetParentDomain(host: String, set: Set<String>): Boolean {
        var current = host
        while (current.contains(".")) {
            if (set.contains(current)) return true
            val dotPos = current.indexOf('.')
            if (dotPos == -1) break
            current = current.substring(dotPos + 1)
        }
        return set.contains(current)
    }

    private fun matchesHardBlockPathOrQuery(rawUrl: String, lowerUrl: String): Boolean {
        for (pathRule in hardBlockPathsList) {
            if (lowerUrl.contains(pathRule)) return true
        }
        val queryIndex = rawUrl.indexOf('?')
        if (queryIndex != -1) {
            val queryString = lowerUrl.substring(queryIndex + 1)
            for (key in hardBlockQueryKeysList) {
                if (queryString.contains("$key=") || queryString.contains("&$key=")) return true
            }
        }
        return false
    }

    private fun extractFileExtension(lowerUrl: String): String? {
        val cleanUrl = lowerUrl.split('?').first().split('#').first()
        val lastDot = cleanUrl.lastIndexOf('.')
        if (lastDot == -1 || lastDot == cleanUrl.length - 1) return null
        return cleanUrl.substring(lastDot + 1)
    }

    // Thread-safe helper list
    private class CopyOnWriteList<T> {
        private val list = java.util.concurrent.CopyOnWriteArrayList<T>()
        fun clear() = list.clear()
        fun addAll(elements: Collection<T>) = list.addAll(elements)
        operator fun iterator(): Iterator<T> = list.iterator()
    }
}
