package com.example.adshield

import com.example.adshield.engine.AdShieldEngine
import com.example.adshield.model.AdShieldConfig
import com.example.adshield.model.PlayerProfile
import com.example.adshield.model.RequestDecision
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AdShieldEngineTest {

    @Before
    fun setUp() {
        AdShieldEngine.isEnabled = true
        val testConfig = AdShieldConfig(
            version = 2,
            adPermissions = listOf("permitted-ad-domain.com"),
            globalAllow = listOf("cdnjs.cloudflare.com", "fonts.googleapis.com"),
            hardBlockPaths = listOf("/ads", "/adv", "/banner", "/pop", "/popunder", "/vast"),
            hardBlockQueryKeys = listOf("ad_id", "pop_id"),
            profiles = listOf(
                PlayerProfile(
                    id = "streamtape",
                    displayName = "Streamtape",
                    hostPatterns = listOf("streamtape.com", "*.streamtape.com", "tapecontent.net", "*.tapecontent.net"),
                    allowedHosts = listOf("streamtape.com", "*.streamtape.com", "tapecontent.net", "*.tapecontent.net"),
                    mediaCdnHosts = listOf("tapecontent.net", "*.tapecontent.net")
                )
            )
        )
        AdShieldEngine.updateConfig(testConfig)
    }

    @Test
    fun testDefaultDenyUnknownDomain() {
        val decision = AdShieldEngine.evaluateRequest("https://unknown-tracker-ad.com/script.js", "streamtape")
        assertTrue("Unknown domains must be blocked under default-deny", decision is RequestDecision.Blocked)
        assertEquals("unknown-tracker-ad.com", decision.host)
    }

    @Test
    fun testAllowedProfileDomain() {
        val decision = AdShieldEngine.evaluateRequest("https://streamtape.com/e/12345", "streamtape")
        assertTrue("Streamtape host must be allowed for streamtape profile", decision is RequestDecision.Allowed)
        assertEquals("streamtape.com", decision.host)
    }

    @Test
    fun testAllowedProfileSubdomain() {
        val decision = AdShieldEngine.evaluateRequest("https://sub.streamtape.com/player.js", "streamtape")
        assertTrue("Streamtape wildcard subdomains must be allowed", decision is RequestDecision.Allowed)
        assertEquals("sub.streamtape.com", decision.host)
    }

    @Test
    fun testHardBlockPathRule() {
        val decision = AdShieldEngine.evaluateRequest("https://streamtape.com/ads/banner.js", "streamtape")
        assertTrue("Hard block path rule must block request even on allowed host", decision is RequestDecision.Blocked)
    }

    @Test
    fun testHardBlockQueryRule() {
        val decision = AdShieldEngine.evaluateRequest("https://streamtape.com/get_video?ad_id=999", "streamtape")
        assertTrue("Hard block query rule must block request", decision is RequestDecision.Blocked)
    }

    @Test
    fun testMediaStreamAllowedFromProfileCdn() {
        val decision = AdShieldEngine.evaluateRequest("https://tapecontent.net/video/stream.mp4", "streamtape")
        assertTrue("Media stream file from profile CDN must be allowed", decision is RequestDecision.Allowed)
    }

    @Test
    fun testMediaStreamBlockedFromUnapprovedDomain() {
        val decision = AdShieldEngine.evaluateRequest("https://thirdparty-ad-network.com/video_ad.mp4", "streamtape")
        assertTrue("Media stream file from unapproved third-party domain must be blocked", decision is RequestDecision.Blocked)
    }

    @Test
    fun testGlobalAllowList() {
        val decision = AdShieldEngine.evaluateRequest("https://cdnjs.cloudflare.com/ajax/libs/jquery/3.6.0/jquery.min.js", "streamtape")
        assertTrue("Global allow list host must be allowed", decision is RequestDecision.Allowed)
    }

    @Test
    fun testPermittedAdHostOverride() {
        val decision = AdShieldEngine.evaluateRequest("https://permitted-ad-domain.com/banner.png", "streamtape")
        assertTrue("Explicitly permitted ad host must be allowed", decision is RequestDecision.Allowed)
        val allowed = decision as RequestDecision.Allowed
        assertTrue(allowed.isPermittedAd)
    }

    @Test
    fun testSanitizeHost() {
        assertEquals("example.com", AdShieldEngine.sanitizeHost("example.com:8080"))
        assertEquals("example.com", AdShieldEngine.sanitizeHost("*.example.com"))
        assertEquals("example.com", AdShieldEngine.sanitizeHost("EXAMPLE.COM"))
        assertEquals("example.com", AdShieldEngine.extractHost("http://example.com:8080"))
    }
}
