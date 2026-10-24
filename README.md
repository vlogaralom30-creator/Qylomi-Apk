# Qylomi 🌟

Modern social networking Android application built with Jetpack Compose, Kotlin Coroutines, Material Design 3, and Supabase integration.

## ✨ Features
- **Dynamic Community Feed**: Real-time posts, appreciation/like counters, bookmarking, and multi-platform media embeds.
- **Rich Media Embed & Preview**: Direct player preview support for YouTube, TikTok, Google Drive, Facebook, Instagram, and direct images.
- **Long Text Expander**: Collapsible text with "Read more" / "Read less" buttons to keep feed scrolling smooth and fast.
- **Creator Rewards & Leveling**: Creator ranks, points badges, and engagement leaderboards.
- **User Profiles**: Custom avatars, banners, bio editing, follower counts, and saved collections.

---

## 🤖 GitHub Actions Automated APK Builder

This repository is equipped with an automated CI/CD workflow at `.github/workflows/build-apk.yml`.

### How It Works:
1. **Automatic Build on Push & PR**: Whenever changes are pushed to `main` or `master` branches, or a pull request is submitted, the workflow automatically triggers.
2. **Manual Dispatch (On-Demand)**:
   - Go to your repository's **Actions** tab on GitHub.
   - Select **"Build Qylomi APK"** from the left sidebar.
   - Click **Run workflow**, choose your branch, select the desired build type (`debug`, `release_unsigned`, or `both`), and click **Run workflow**.
3. **Download Your APK**:
   - When the workflow run completes (typically in ~2-3 minutes), click into the run details.
   - Scroll down to the **Artifacts** section at the bottom (or summary top).
   - Click **`Qylomi-Android-APK`** to download the zip file containing the ready-to-install Android APK (`app-debug.apk`).

---

## 🎬 Series & Streaming System (Live Supabase Backend)

Qylomi incorporates a dedicated Series, Anime, Movies, and TV Shows system powered by Supabase PostgREST & RPC endpoints:
- **Role-Based Access Control**: `UserProfile.role` (`user`, `creator`, `admin`) with helpers `isCreator`, `isAdmin`, `canCreateSeries`.
- **Series Models (`SeriesModels.kt`)**: `ContentCategory`, `Series`, `SeriesCard`, `Season`, `Episode`, `EpisodeSource`, `Shelf`, `ShelfWithItems`, `ContinueWatchingItem`, `WatchProgress`, `SeriesComment`, `SourceReport`.
- **Strict Whitelist DTOs**: Mutation requests (`SeriesInsert`, `SeriesUpdate`, `EpisodeInsert`, `SourceInsert`, etc.) strictly omit read-only trigger counters (`views_count`, `likes_count`, `avg_user_rating`, etc.) to prevent Postgres 403 / 42501 column permission errors.
- **`SeriesRepository.kt`**:
  - **Catalog & Discovery**: `getCategories`, `getHome` (via `get_series_home` RPC), `getSeriesList`, `searchSeries` (via `search_series` RPC), `getSeriesBySlug`, `getSeriesById`, `getSeasons`, `getEpisodes`, `getSources`, `getRelated` (via `get_related_series` RPC), `getContinueWatching` (via `get_continue_watching` RPC).
  - **Viewer Actions**: `likeSeries`, `unlikeSeries`, `likeEpisode`, `unlikeEpisode`, `rateSeries`, `addToWatchlist`, `removeFromWatchlist`, `upsertProgress`, `recordView` (via `record_view` RPC with persistent guest key fallback), threaded `addComment`, `editCommentContent`, `deleteComment`, `likeComment`, `reportSource`.
  - **Studio & Admin**: `createSeriesWithAutoSlug`, `updateSeries`, `deleteSeries`, `listMySeries`, `listAllSeriesAdmin`, `blockSeries`, `unblockSeries`, `createSeason`, `updateSeason`, `deleteSeason`, `createEpisode`, `updateEpisode`, `deleteEpisode`, `createSource`, `updateSource`, `swapSourcePriority`, `disableSource`, `listMyEligibleShelves`, `setShelfItem`, `removeShelfItem`, `getShelvesAdmin`, `createShelf`, `updateShelf`, `deleteShelf`, `getShelfItems`, `getCategoriesAdmin`, `createCategory`, `updateCategory`, `deleteCategory`, `searchUsers`, `setUserRole` (via `set_user_role` RPC), `listSourceReports`, `markReportHandled`, `deleteSourceReport`.
  - **Error Handling (`SeriesResult.kt`)**: Maps PostgREST SQL states (`42501` permission denied, `23505` duplicate key, `23503` missing reference, `P0001` cannot remove last admin) and HTTP statuses (`401`, `403`, `404`, `409`) to user-friendly error types.

### 🎨 Phase 2 — Creator Studio & Admin Studio
- **Creator Studio (`CreatorStudioScreen.kt`)**:
  - Series management (list via `listMySeries`, create/edit/delete, publish toggle).
  - Blocked status warning: "Blocked by admin: <reason>" disables publishing.
  - Series metadata form (`SeriesFormView.kt`): dynamic genre & language chips, rating slider, age rating, mature content toggle, studio, release year, poster/banner pickers, automated slug generation with conflict retry.
  - Manual shelf selector: creators can only view and select active shelves with `type == 'manual'`.
  - Episode & Source manager (`EpisodesManagerView.kt`): bulk episode addition, video URL test with platform classifier (`direct`, `hls`, `youtube`, `gdrive`, `tiktok`, `cloudflare`), priority swap, and active source validation.
- **Admin Studio (`AdminStudioScreen.kt`)**:
  - Categories manager: add, rename, reorder (`sort_order`), and toggle active status.
  - Shelves manager: configure layouts (`hero`, `top10`, `row`, `grid`), position, type, and item associations (`series_shelf_items`).
  - Roles manager: search profiles, display role chips, and execute role changes exclusively via `set_user_role` RPC with `P0001` last admin protection.
  - Moderation: block/unblock series with custom reason, source reports inbox with "mark handled", "reopen", and "disable source" actions.

### 🧭 Phase 3 — Navigation Drawer, Series Catalog & Details
- **Navigation Drawer (`NavigationDrawer.kt`)**:
  - `ModalNavigationDrawer` with header showing user avatar, name, `@username`, and role badge (`Creator` / `Admin`).
  - Primary navigation links: Feed, Series & Anime, Explore, Groups, Reels, Saved, Notifications.
  - Role-gated studio shortcuts: "Creator Studio" (for `canCreateSeries`) and "Admin Studio" (for `isAdmin`).
  - Hamburger menu trigger on top app bar and series shortcut icon.
- **Series & Anime Catalog (`SeriesHomeScreen.kt`)**:
  - Powered by one request (`get_series_home`).
  - Category tabs ("All", "Anime", "Movie", "Drama") dynamically loaded from `content_categories`.
  - `shelf.layout == "hero"`: Auto-sliding `HeroCarousel` with 5-second interval, blurred poster fallback when banner is null, genres chips, Play and + My List buttons.
  - `shelf.layout == "top10"`: `Top10SeriesCardItem` with large stylized rank numbers.
  - `shelf.layout == "row"`: 2:3 aspect ratio poster cards with rating badges, "NEW" badge for episodes within 7 days, language chips, and 18+ indicators.
  - Personal rows: "Continue Watching" (with watch progress bar) and "My List" for authenticated users.
  - 300ms debounced catalog search.
- **Series Details Screen (`SeriesDetailsScreen.kt`)**:
  - Full metadata row query by slug.
  - Banner/poster header, title, original title, community rating, year, status, studio, audio and subtitle languages.
  - Action buttons: Play, + My List, Like, Rate (1-10 slider modal sheet), Share, and Trailer.
  - 18+ mature content consent dialog remembered locally.
  - Season selector and episode list with thumbnails, durations, and progress tracking.
  - "More Like This" recommendation row via `get_related_series`.
  - Optimistic updates with rollback for likes, watchlist, and ratings.
- **Browse & Filter List (`SeriesListScreen.kt`)**:
  - "See all" per shelf with 3-column poster grid.
  - Filtering by genre, language, and release status.
  - Sorting: Latest, Top Rated, Popular, and A-Z.
  - Offset pagination with "Load more".

