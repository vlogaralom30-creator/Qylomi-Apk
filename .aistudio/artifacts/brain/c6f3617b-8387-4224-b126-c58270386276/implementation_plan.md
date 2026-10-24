# Supabase Token Refresh System & TikTok Video Embed Enhancements

Automatic background Supabase access token refresh on 401/JWT errors, proactive token expiry management, and direct embedded TikTok / Tiklink video player support with browser fallback across Qylomi Social Feed, Reels, and Series streaming.

## User Review & Critical Decisions

> [!IMPORTANT]
> The following choices were confirmed by the user in Phase 1:

- **Confirmed Decision 1**: **Automatic Background Token Refresh**: When Supabase API returns 401 / PGRST301 (JWT expired), the system automatically performs a single-flight token refresh using `refresh_token`, updates saved credentials in `SessionStore` and `SupabaseClient`, and retries the failed API call seamlessly without logging out the user or interrupting playback.
- **Confirmed Decision 2**: **Direct Embedded TikTok Player with Fallback**: TikTok / Tiklink video posts (`vt.tiktok.com`, `vm.tiktok.com`, `tiktok.com/@user/video/id`, and shortlinks) extract direct HD video stream URLs via fallback providers (TikWM/TikDrop/SSSTik) to render in native Compose video player (`NativeVideoPlayer`), falling back gracefully to TikTok Web Embed iframe (`EmbedVideoPlayer`) and an "Open in App / Browser" external link button.
- **Confirmed Decision 3**: **App-wide System Integration**: Both automatic token refresh and TikTok video link parsing apply across all features—Social Feed, Reels video feed, Series streaming, and `SessionManager` / `SupabaseClient` authentication layers.

---

## 1. Overview & Core Concept

- **What It Does**:
  1. Prevents unexpected session logouts and API permission errors by silently refreshing expired Supabase access tokens in the background via an OkHttp `Authenticator` & `Interceptor` pipeline.
  2. Ensures TikTok and "Tiklink" shortlink video posts play smoothly in-app across the Feed, Reels, and Video views using direct video extraction with HTML5 Web Embed fallbacks.
- **Target Audience / Persona**: All active Qylomi users (creators, viewers, and guests) who interact with video feeds and long-lived authenticated sessions.
- **Key Value**: Continuous, uninterrupted video watching and social interactions without authentication session dropouts or broken TikTok links.

---

## 2. User Experience & Visual Design

- **Key User Flows**:
  - **Token Refresh Flow**:
    1. User makes any authenticated call (posting, liking, commenting, watching series, fetching private feed).
    2. If token is near expiration or returns HTTP 401, OkHttp interceptor/authenticator pauses the request thread, performs `POST /auth/v1/token?grant_type=refresh_token`, saves new `access_token` and `refresh_token` to `SessionStore`, and retries original request with new token header.
    3. User experiences seamless operation with zero error banners or disconnections.
  - **TikTok / Tiklink Video Flow**:
    1. User pastes or shares any TikTok link (including `vt.tiktok.com/XYZ` shortlinks or standard video URLs) in Create Post or Feed.
    2. `TikTokLegacyHandler` resolves redirects to extract video ID and attempts multi-provider direct MP4 extraction.
    3. On Feed and Reels:
       - Direct HD MP4 is rendered with `NativeVideoPlayer` (muted/autoplay controls).
       - If direct stream extraction is unavailable, `EmbedVideoPlayer` renders an interactive Web iframe with branded TikTok accent badge and an "Open in TikTok / Browser" action button.

- **Visual Identity & Theme**:
  - *Aesthetic Direction*: Dark mode Material Design 3 matching Qylomi design tokens (`QBackground` `#0F0F12`, `QSurface` `#1A1A22`, `QYellowPrimary` `#FFD700`, TikTok cyan accent `#00F2FE`).
  - *Micro-Interactions*: Smooth loading spinners during background link parsing and non-blocking retry toast notifications if session refresh fails permanently.

---

## 3. Key Product Decisions & Trade-Offs

- **Decision 1: OkHttp Authenticator + Interceptor for Supabase Client**:
  - *Chosen Approach*: Attach `SessionManager.interceptor` and `SessionManager.authenticator` directly to `SupabaseClient.okHttpClient`. Also normalize `SeriesRepository`, `SocialRepository`, and `RelayRepository` so they read live token from `SessionManager` rather than baking stale token strings into method parameters.
  - *Why*: Eliminates 401 PGRST301 "JWT expired" errors completely without modifying every UI ViewModel.
  - *Alternatives Considered*: Manual try-catch in each ViewModel (rejected: repetitive and error-prone).

- **Decision 2: Multi-stage TikTok / Tiklink Extractor Pipeline**:
  - *Chosen Approach*: Resolve shortened URLs (`vt.tiktok.com`, `vm.tiktok.com`) using HEAD/GET redirect tracking before querying TikWM/fallback endpoints.
  - *Why*: TikTok frequently uses shortlinks which fail simple regex matching if not resolved first.
  - *Alternatives Considered*: Pure Webview embed without direct URL extraction (rejected: poor performance in vertical Reels list).

---

## 4. Technical Architecture & Data Strategy (Technical Reference)

### System Architecture Diagram

```
┌────────────────────────────────────────────────────────────────────────┐
│                        Qylomi Application UI                           │
│     (FeedScreen / ReelsScreen / EpisodePlayerScreen / CreatePost)      │
└──────────────────────────────────┬─────────────────────────────────────┘
                                   │
             ┌─────────────────────┴──────────────────────┐
             ▼                                            ▼
┌──────────────────────────┐                ┌────────────────────────────┐
│   VideoPlatformRegistry   │                │     Supabase Retrofit      │
│  (TikTokLegacyHandler)   │                │         API Calls          │
└────────────┬─────────────┘                └─────────────┬──────────────┘
             │                                            │
             ▼                                            ▼
┌──────────────────────────┐                ┌────────────────────────────┐
│ Redirect Resolver +      │                │  SupabaseClient OkHttp     │
│ Multi-Provider Extractor │                │ Interceptor & Authenticator│
└────────────┬─────────────┘                └─────────────┬──────────────┘
             │                                            │
             ▼                                            ▼
┌──────────────────────────┐                ┌────────────────────────────┐
│ Native / Embed Video UI  │                │ SessionManager & Store     │
│  (ExoPlayer / WebView)   │                │ (Token Refresh & Storage)  │
└──────────────────────────┘                └────────────────────────────┘
```

### Key Components To Implement / Modify

1. **`SupabaseClient.kt`**:
   - Register `SessionManager.getInstance(context).interceptor` and `SessionManager.getInstance(context).authenticator` in `okHttpClient`.
   - Dynamic authorization header resolution using current session token.

2. **`SessionManager.kt`**:
   - Ensure single-flight `refreshSession()` correctly updates `SupabaseClient.setAuthToken()` and broadcasts updated state to observers.
   - Graceful handling of expired refresh tokens with clean user notification.

3. **`TikTokLegacyHandler.kt` & `VideoEmbedUtils.kt`**:
   - Enhanced redirect resolution for `vt.tiktok.com`, `vm.tiktok.com`, `m.tiktok.com`, and `tiktok.com/t/` shortlinks.
   - Robust JSON extraction fallback using HTML embed iframe (`https://www.tiktok.com/embed/v2/{id}`) when direct MP4 extraction is restricted.

4. **`EmbedVideoPlayer.kt`**:
   - Custom WebView settings (`domStorageEnabled`, `javaScriptEnabled`, user-agent header) to support TikTok embed player smooth playback.
   - Clear "Open in Browser / TikTok App" action button.

5. **`SeriesRepository.kt` & Social Repositories**:
   - Ensure all network calls utilize dynamic headers provided by `SessionManager` / `SupabaseClient` interceptor.

---

## Verification Plan

### Automated Tests
- Run Gradle unit tests: `gradle :app:testDebugUnitTest`
- Verify token expiration mock handling and video URL regex parsing.

### Manual Verification
- Test user session token refresh under expired JWT conditions.
- Create video posts with standard TikTok URLs (`https://www.tiktok.com/@user/video/123456789`) and TikTok shortlinks (`https://vt.tiktok.com/ABCDEF/`).
- Confirm playback on Feed, Reels screen, and EmbedVideoPlayer card.
