# Qylomi App — Series Integration: 4 Phase Prompts (matched to the LIVE database)

How to use:
1. For every phase, paste **COMMON BLOCK** first, then that phase's prompt. (Each agent session starts empty, so the common block must be repeated.)
2. Run phases in order: 1 → 2 → 3 → 4. Build and test after each.
3. **The database is already built and tested. Do NOT run any SQL from the agent.** Phase 1 has no SQL at all.
4. First admin (once, in Supabase SQL Editor):
   `update public.profiles set role='admin' where username='YOUR_USERNAME';`
   Usernames: rafi, chatgpt, ronyw, ronyw13, ronyvi.
5. Also attach `QYLOMI_SERIES_TABLES_FOR_APP.md` to every phase.

---

# COMMON BLOCK (paste before every phase)

```
PROJECT: Qylomi Android app (Kotlin, Jetpack Compose, Retrofit + Moshi). Shared Supabase backend.
Supabase URL: https://ffbmozwsasbhevtehbit.supabase.co  (REST: /rest/v1/..., RPC: POST /rest/v1/rpc/<fn>)
The series backend is ALREADY LIVE and tested. It also serves the website. Treat it as a fixed contract.

ABSOLUTE RULES
1. DO NOT write, run or output any SQL, migrations or schema changes. DO NOT create tables, triggers, policies or RPCs. If you think something is missing, STOP, explain exactly what and why, and ask me. Never "fix" the DB yourself.
2. Do not change theme/colors/fonts. Reuse existing tokens (ui/theme/Color.kt) and components (QTopAppBar, QBottomNavBar, sheets, text fields, chips). Do not break feed, like, comment, bookmark, follow, create post, pixel art, multi-account.
3. Layers: data/model -> data/api/SupabaseApi.kt -> NEW data/repository/SeriesRepository.kt (do not grow SocialRepository) -> ViewModel -> UI. Reuse the existing token/session/error handling.
4. Never send counter columns or read-only columns in a write (see WRITE WHITELIST). The server rejects them with 42501/403 "permission denied for column".
5. Use only the table, column and RPC names below. Do not use posts, video_posts, playlists, post_* tables for series.
6. Existing patterns first: read UserProfile.kt, SupabaseApi.kt, SupabaseClient.kt, SocialRepository.kt, MainActivity.kt, the video handler registry. Show a short plan first, then implement.
7. When done list changed files and anything unfinished. Never claim a test passed that you did not run.

HEADERS: apikey: sb_publishable_kzXQl0X6eNtzFrAuY-3EFw_pYh6H-qr · Authorization: Bearer <user access token, or the key itself for guests> · for inserts that need the row back: Prefer: return=representation · upserts: Prefer: resolution=merge-duplicates (+ ?on_conflict=...) · insert-ignore: Prefer: resolution=ignore-duplicates

ROLES: profiles.role = 'user' | 'creator' | 'admin' (column already exists, default 'user'). Only an admin can change a role (RPC set_user_role). Guests = anon.

TABLES (all exist, RLS on)
content_categories(id, slug, name, sort_order, is_active, created_at, updated_at)
series(id, slug, title, original_title, description, poster_url, banner_url, trailer_url, category_id, genres text[], languages text[], subtitle_languages text[], rating numeric(3,1) 0-10, age_rating, is_mature, status 'ongoing'|'completed'|'upcoming', release_year, studio, total_episodes, is_published(default false), is_featured, is_blocked, blocked_reason, created_by, [read-only: avg_user_rating, ratings_count, likes_count, comments_count, views_count, episodes_count, last_episode_at, created_at, updated_at])
series_seasons(id, series_id, season_number, title, description, poster_url)  -- optional; UNIQUE(series_id, season_number)
episodes(id, series_id, season_number default 1, episode_number, title, description, thumbnail_url, duration_seconds, air_date, intro_start_sec, intro_end_sec, is_published(default true), created_by, [read-only: likes_count, comments_count, views_count]) UNIQUE(series_id, season_number, episode_number)
episode_sources(id, episode_id, label, url (must start http:// or https://), platform, language, quality, priority (lower first), is_active, [read-only: fail_count, last_ok_at])
series_shelves(id, slug, title, type 'manual'|'genre'|'category'|'auto_trending'|'latest_episodes'|'top_rated'|'hero', layout 'row'|'hero'|'top10'|'grid', config jsonb, category_id null=all tabs, position, is_active, max_items 1-50)
series_shelf_items(shelf_id, series_id, position)  PK(shelf_id, series_id)
series_likes(series_id,user_id) · episode_likes(episode_id,user_id) · series_comment_likes(comment_id,user_id)
series_ratings(series_id,user_id,rating 1-10) · series_watchlist(user_id,series_id,created_at)
watch_progress(user_id, episode_id, series_id, position_sec, duration_sec, completed, updated_at) PK(user_id,episode_id)
series_comments(id, series_id, episode_id nullable, user_id, parent_id nullable, content 1-3000, [read-only: likes_count, created_at, updated_at])
source_reports(id, source_id, user_id, reason 'broken'|'wrong_episode'|'copyright'|'other', details<=1000, status 'open'|'handled', handled_by, handled_at, created_at)
watch_views: NO client access (written only by record_view).
profiles is publicly readable (id, username, display_name, avatar_url, role, ...).

WRITE WHITELIST (authenticated; sending any other column fails)
series INSERT: age_rating, banner_url, category_id, created_by, description, genres, is_featured, is_mature, is_published, languages, original_title, poster_url, rating, release_year, slug, status, studio, subtitle_languages, title, total_episodes, trailer_url
series UPDATE: same list minus created_by, plus blocked_reason, is_blocked  (is_featured/is_blocked/blocked_reason are ignored unless the caller is admin)
episodes INSERT: air_date, created_by, description, duration_seconds, episode_number, intro_end_sec, intro_start_sec, is_published, season_number, series_id, thumbnail_url, title
episodes UPDATE: air_date, description, duration_seconds, episode_number, intro_end_sec, intro_start_sec, is_published, season_number, thumbnail_url, title
episode_sources INSERT: episode_id, is_active, label, language, platform, priority, quality, url · UPDATE: is_active, label, language, platform, priority, quality, url
content_categories, series_seasons, series_shelves, series_shelf_items: all non-generated columns writable (admin; creators for seasons/shelf_items rules below)
series_comments INSERT: content, episode_id, parent_id, series_id, user_id · UPDATE: content only
series_ratings / watch_progress: upsert all their columns
source_reports INSERT: details, reason, source_id, user_id · UPDATE: status only (admin or the series owner)
Likes / watchlist / comment_likes: INSERT and DELETE only (no update; use insert, and Prefer: resolution=ignore-duplicates for idempotent adds).
Never send: any *_count, avg_user_rating, views_count, last_episode_at, fail_count, last_ok_at, created_at, updated_at, handled_by, handled_at.
Genres/languages/subtitle_languages are normalized by the server (lower-case, trimmed, de-duplicated). Send lower-case anyway and display capitalized.

WHO CAN DO WHAT (server-enforced; the UI must mirror it)
- anon: read published series/episodes/active sources/shelves/comments; RPCs get_series_home, get_related_series, search_series, record_view.
- user: + likes, ratings, watchlist, progress, comments, report a source.
- creator: + create/edit/delete OWN series, seasons, episodes, sources; add OWN series to shelf rows that are type='manual' AND is_active=true (never 'hero', never genre/category/auto shelves).
- admin: everything, incl. categories, shelves, hero, feature/block (takedown), roles, reading all reports.
- Public visibility rule: is_published AND NOT is_blocked (owner/admin see own/all). A blocked series cannot be un-blocked by its creator.

RPCs (POST /rest/v1/rpc/<name>, JSON body)
get_series_home {"p_category":"anime"|null} -> [ { "shelf":{id,slug,title,type,layout,position}, "items":[Card,...] } ]  (one call for the whole home page; empty shelves are omitted; honours category tabs; hero shelf has layout "hero"; top_rated has "top10")
get_related_series {"p_series_id":uuid,"p_limit":12} -> [Card]
get_continue_watching {"p_limit":12} -> [ {series, episode, position_sec, duration_sec, updated_at} ]  (login required; empty for guests)
record_view {"p_series_id":uuid,"p_episode_id":uuid|null,"p_viewer_key":"random id for guests"} -> void (dedup per viewer per episode per hour; call after ~30 s real playback)
search_series {"q":"naruto","p_category":null,"lim":20,"off":0} -> [Card]  (q >= 2 chars)
set_user_role {"p_user_id":uuid,"p_role":"user"|"creator"|"admin"} -> void (ADMIN ONLY; the last admin cannot be demoted; error codes: 42501 not admin, 22023 bad role, P0001 last admin)

Card = {id, slug, title, original_title, description (first 280 chars), poster_url, banner_url, category{id,slug,name}, genres[], languages[], rating, avg_user_rating, ratings_count, status, release_year, age_rating, is_mature, episodes_count, last_episode_at, views_count, likes_count, created_at}
Card does NOT contain studio, subtitle_languages, trailer_url, total_episodes. Load the full row from GET series for the details page.
Every Card/Series field is nullable-safe: use defaults in Moshi models.

REST PATTERNS (PostgREST)
GET series?slug=eq.X&select=*,category:content_categories(slug,name)&limit=1
GET series?category_id=eq.<id>&genres=cs.{horror}&languages=cs.{hindi}&status=eq.ongoing&is_published=eq.true&order=views_count.desc&limit=24&offset=0   (sorts: created_at.desc, rating.desc.nullslast, views_count.desc, title.asc)
GET episodes?series_id=eq.<id>&order=season_number.asc,episode_number.asc
GET episode_sources?episode_id=eq.<id>&is_active=eq.true&order=priority.asc
GET series_comments?series_id=eq.<id>&episode_id=eq.<ep> (or episode_id=is.null)&parent_id=is.null&order=created_at.desc&select=*,profile:profiles(username,display_name,avatar_url)
Upserts: POST watch_progress?on_conflict=user_id,episode_id ; POST series_ratings?on_conflict=series_id,user_id (both with Prefer: resolution=merge-duplicates)
Deletes: DELETE series_likes?series_id=eq.X&user_id=eq.<uid> (same pattern for episode_likes, series_watchlist, series_comment_likes)
Embeds work via foreign keys: series.category_id, episodes.series_id, series_comments.user_id -> profiles.

DATA PRESENT NOW (for testing): categories anime/movie/drama; series naruto-shippuden (S1 E1-10, Hindi dub, direct mp4), attack-on-titan (S1 E1), one-piece (S1 E918); all banner_url are NULL, so the hero must fall back to poster_url. Shelves: featured(hero), trending-now, latest-episodes, top-rated(top10), horror-anime, otherworld-isekai. There is no manual shelf yet and no admin until I create one, and no movie/drama titles yet.
```

---

# PHASE 1 — Data layer + role helpers (NO SQL, NO UI)

```
TASK (PHASE 1 of 4): Build the Android data layer for the Series system against the LIVE backend described in the COMMON BLOCK. No UI. No SQL. No schema changes.

1) ROLE
- Add `role: String = "user"` to UserProfile (Moshi default so old responses still parse). Add helpers isCreator, isAdmin, canCreateSeries (creator or admin). Make sure the existing profile fetch/update code does not send `role` when editing a profile (clients must never write role).

2) MODELS (data/model, Moshi, every field nullable or defaulted; unknown fields ignored)
ContentCategory, Series (full row incl. read-only counters), SeriesCard (the Card JSON), CategoryRef(id,slug,name), Season, Episode, EpisodeSource, Shelf(id,slug,title,type,layout,position), ShelfWithItems(shelf, items: List<SeriesCard>), ContinueWatchingItem(series, episode, position_sec, duration_sec, updated_at), WatchProgress, SeriesComment(+ profile embed), SourceReport, plus request bodies that contain ONLY whitelisted columns (SeriesInsert, SeriesUpdate, EpisodeInsert, EpisodeUpdate, SourceInsert, SourceUpdate, ProgressUpsert, CommentInsert, RatingUpsert, ReportInsert, ShelfInsert/Update, CategoryInsert/Update). Request bodies must omit null optional fields (use Moshi adapter or separate classes) so a PATCH never sends a read-only column.

3) API + REPOSITORY (SupabaseApi additions + NEW SeriesRepository)
Read: getCategories, getHome(category: String?), getSeriesList(category, genre, language, status, sort, limit, offset), searchSeries(q, category, limit, offset), getSeriesBySlug (full row + category embed), getSeasons, getEpisodes(seriesId), getSources(episodeId), getRelated, getContinueWatching, getComments(seriesId, episodeId?, parentId?), getMyWatchlist (with series embed), getMyLikes/isLiked, getMyRating, getMyProgress(seriesId).
User actions: like/unlike series and episode (insert with ignore-duplicates / delete), rate (upsert), watchlist add/remove, upsertProgress (on_conflict user_id,episode_id), recordView(seriesId, episodeId, viewerKey) with a locally generated random guest key stored once, comments (insert, edit content only, delete), comment like/unlike, reportSource(sourceId, reason, details).
Studio (used in Phase 2): createSeries/updateSeries/deleteSeries, listMySeries (created_by = me, includes unpublished), upsertEpisode/deleteEpisode, create/update/delete source (priority swap = two updates), seasons CRUD, listMyEligibleShelves (type=manual, is_active=true), set/remove shelf items, categories CRUD, shelves CRUD, listAllSeries (admin), searchUsers (GET profiles with ilike on username/display_name, limit 20), setUserRole (RPC set_user_role), listSourceReports (open/handled, with source->episode->series embed), markReportHandled (PATCH status), disableSource (is_active=false), blockSeries(id, reason) / unblock.
Slug helper: lowercase, [a-z0-9-] only, collapse dashes, append -2, -3 on 409 conflict.

4) ERRORS
Map HTTP/PostgREST errors to a sealed result type: 401 (session system refreshes; retry once), 403 or code 42501 -> "You don't have permission", 409/23505 -> "Already exists (duplicate slug or episode number)", 23503 -> "Related item no longer exists", 22023/23514 -> validation message, P0001 from set_user_role -> "Cannot remove the last admin", network -> offline. Never crash on empty arrays or on guests (get_continue_watching returns []).

5) TESTS
Unit-test model parsing with these real-shaped samples: a get_series_home response (shelf + items), a Card with null banner_url, a series row with arrays, an episode_sources row, a set_user_role error body. Also test that request bodies never contain read-only fields.

DONE WHEN: project builds; no UI/theme change; no SQL created or run; every function above exists and is documented in one short README section; list changed files; list any backend gap you found WITHOUT trying to fix it.
```

---

# PHASE 2 — Creator Studio + Admin Studio

```
TASK (PHASE 2 of 4): Content management screens using the Phase 1 SeriesRepository. Normal users must never see or reach them. No SQL, no schema changes.

RULES: follow COMMON BLOCK. Overlay screens in MainActivity (same pattern as ProfileScreen). Temporary entry points on the profile screen: "Creator Studio" only if canCreateSeries, "Admin Studio" only if isAdmin (Phase 3 moves them to the drawer). Reuse the existing image uploader (EditProfileScreen) for poster (2:3), banner (16:9), thumbnails. Read CreatePostDialog (extract button + video preview) and the video handler registry. Enforce permission in the UI AND handle server 403 gracefully. Close the studio if the user's role changes while open.

CREATOR STUDIO (creator + admin)
1) My Series: list via listMySeries (poster, title, category, status, Published/Draft badge, episodes_count); actions edit, delete (confirm, cascades to episodes), publish toggle; FAB "New series". A blocked series shows "Blocked by admin: <reason>" and cannot be published.
2) Series form: title (required, 1-200), original title, description, category dropdown (active categories), genres chips (Action, Adventure, Comedy, Drama, Fantasy, Horror, Isekai, Romance, Sci-Fi, Slice of Life, Thriller + custom), languages chips (Hindi, Japanese, English, Bangla, Tamil, Telugu + custom), subtitle languages, rating slider 0-10 step 0.1, age rating + is_mature, status (ongoing/completed/upcoming), release year (1900-2100), studio, total episodes (>=0), poster, banner, trailer URL, published toggle, is_featured toggle visible to admin only. Slug auto-generated from title, editable only on create, unique via the repository slug helper. Draft saving without publishing. Send only whitelisted columns.
   "Show in shelves": list ONLY shelves with type='manual' and is_active=true (use listMyEligibleShelves) as checkboxes with a position number, writing series_shelf_items. If the list is empty show "No manual shelves yet. Ask an admin to create one." Never show hero/genre/category/auto shelves to creators (admins manage those in Admin Studio). Save the series first, then the shelf rows.
3) Seasons (optional): small editor for series_seasons (season number, title, poster).
4) Episodes manager: grouped by season; add/edit/delete; ordered by number. Form: season (default 1), episode number (auto-suggest next free), title, description, thumbnail, duration seconds (auto-fill from the test preview if available), air date, intro start/end seconds (end > start, both >= 0), published toggle. "Bulk add": create N episodes titled "Episode N" in one go (skip numbers that already exist; 409 = skip).
5) Sources editor (inside the episode form): rows with label ("Server 1"), url (must start with http:// or https://), language (Hindi dub, Japanese sub, English sub, ...), quality (360p, 480p, 720p, 1080p, Auto), priority (up/down arrows, lower plays first), is_active toggle, delete. A "Test" button runs the URL through the existing video handler registry and shows a mini preview, a detected platform chip (YouTube / Drive / TikTok / Direct / HLS / Cloudflare) and the duration; store the detected platform in `platform` ('direct','hls','youtube','gdrive','tiktok','cloudflare'). Rule enforced in the UI: an episode needs at least one active source to be published.
   Show fail_count (read-only) next to a source; a high count shows a warning chip.
6) "Preview as viewer" button: hidden until Phase 3 exists.

ADMIN STUDIO (admin only)
- Categories: add, rename, reorder (sort_order), enable/disable (is_active). slug lowercase [a-z0-9-], unique.
- Shelves: create/edit (title, slug, type, layout, config JSON, category scope or all, max_items 1-50, position, is_active). Config rules: genre -> {"genre":"horror"}, category -> {"category":"movie"}, auto_trending -> {"days":7}. Default layouts: hero->"hero", top_rated->"top10", others->"row" (allow "grid"). Manage items for manual and hero shelves (add/remove/reorder series via series_shelf_items.position). Hero shelf = banner list.
- Creators/roles: search users (profiles ilike), show current role chip, change role with confirm dialog via set_user_role RPC ONLY (never PATCH profiles.role: RLS blocks it). Handle errors: 42501, P0001 "last admin", 22023.
- Moderation: list all series (published or not) with unpublish, delete, Block/Unblock with a reason (is_blocked, blocked_reason); list source_reports (filter open/handled) showing reason, details, series > episode link, reporter; actions: Mark handled (PATCH status='handled'), Reopen (status='open'), Disable source (is_active=false on episode_sources), Delete report (admin only).

EDGE CASES: duplicate slug or episode number (409 -> inline message), upload failure with retry, form state survives rotation (ViewModel + rememberSaveable), imePadding on long forms, delete confirmations, creator trying to touch another creator's series (403 -> message), unsaved-changes confirm on back.

DONE WHEN: a creator creates a series with poster/banner, adds episodes with multiple tested sources, picks manual shelves and publishes; an admin manages categories, shelves, roles, moderation (block, reports); normal users cannot see Studio; no SQL used; no theme change; builds. List changed files.
```

---

# PHASE 3 — Drawer + Series Home + Details

```
TASK (PHASE 3 of 4): Browse experience. No SQL, no schema changes.

RULES: follow COMMON BLOCK. New screens are overlays in MainActivity like ProfileScreen; keep the 5-tab bottom bar and feed intact. Home = ONE request (get_series_home). Pull-to-refresh (PullToRefreshBox), skeleton loading, empty/error/offline states, Coil images, posters 2:3. Read QTopAppBar, MainActivity.kt, NavigationBars.kt, SeriesRepository first.

1) DRAWER: hamburger at the start of the top bar (keep logo, search, reels, bell, avatar). ModalNavigationDrawer: header with avatar, name, @username, role chip (Creator/Admin); items Feed, Series (new), Explore, Groups, Reels, Saved, Notifications; divider; "Creator Studio" (canCreateSeries) and "Admin Studio" (isAdmin) (move Phase 2 entry points here); Settings/Logout if they exist. Close on item tap and on back.

2) SERIES HOME
- Top bar: back/menu, "Series", search icon (search_series, 300 ms debounce, min 2 chars, results grid).
- Category chips: All + content_categories (is_active, sort_order). Selecting reloads get_series_home with p_category (null for All).
- Render strictly by the response: iterate the returned array in order. shelf.layout == "hero" -> auto-sliding HorizontalPager (5 s, pause on touch) using banner_url, FALLBACK to poster_url when banner_url is null (blurred poster background + sharp poster), title, rating, genres, languages chips, buttons "Play" and "+ My List". layout "top10" -> numbered row. "row" -> normal LazyRow of poster cards. "grid" -> grid. Do not hardcode shelf slugs or types; do not re-sort.
- Card: poster 2:3, rating badge (rating), "NEW" if last_episode_at within 7 days, language chip, 18+ icon if is_mature, bookmark state.
- Personal rows on top for logged-in users only: "Continue Watching" (get_continue_watching, progress bar = position_sec/duration_sec, tap resumes the episode) and "My List" (series_watchlist). Hide when empty.
- "See all" per shelf opens SeriesListScreen: grid 2-3 columns, filters genre / language / status / category, sort Latest (created_at.desc) | Top rated (rating.desc.nullslast) | Popular (views_count.desc) | A-Z (title.asc), pagination (limit/offset 24). For genre shelves pre-filter genres=cs.{genre} from shelf.config; for category shelves pre-filter that category. For auto_trending use Popular sort; for latest_episodes use last_episode_at.desc.nullslast. (There is no trending RPC for lists.)

3) SERIES DETAILS (open by slug; fetch the FULL series row, the Card lacks studio, trailer, subtitles)
- Header banner/poster (fallback to poster), title, original title, rating star + avg_user_rating (ratings_count), year, status, category, age rating, language + subtitle chips, genres chips, expandable description, studio.
- Buttons: Play (S1E1 or continue from progress), + My List, Like (likes_count), Rate (1-10 sheet, upsert), Share (title + website-style link text), Trailer if trailer_url exists.
- Season selector (seasons come from distinct season_number in episodes; use series_seasons title if present) + episode list: thumbnail, "E3 title", duration, progress bar, check if completed, "NEW" badge; only is_published episodes. Tap opens the player (stub until Phase 4).
- "More like this" row (get_related_series). Comments preview placeholder (Phase 4).
- is_mature: one-time 18+ confirm dialog, remembered locally.
- If the series is blocked or unpublished and I am not owner/admin it simply will not load: show "This title is not available".

4) SHARED STATE: after like / watchlist / rating changes, update every visible card instantly (optimistic UI, rollback on error). Guests tapping like/rate/list get the existing login prompt. Counters shown come from the server (refetch), never computed locally.

DONE WHEN: drawer works with role items; home shows chips, hero and shelves exactly as configured in the DB (order, categories, genres) from one request; See all, search, details, watchlist/like/rate work; empty DB categories (movie, drama) show a friendly empty state; no SQL used; no theme change; builds. List changed files.
```

---

# PHASE 4 — Player + progress + comments + related

```
TASK (PHASE 4 of 4): Episode player page. No SQL, no schema changes.

RULES: follow COMMON BLOCK. Reuse the video handler registry and NativeVideoPlayer (Media3). Add androidx.media3:media3-exoplayer-hls for .m3u8. Cloudflare Stream links (customer-xxx.cloudflarestream.com/ID or videodelivery.net) -> convert to the HLS manifest (.../manifest/video.m3u8) with the iframe URL as embed fallback; Cloudflare R2 / public .mp4 are plain direct links (current data uses https://pub-b3b8e974b48a4470bcaf08e042ed655e.r2.dev/*.mp4). Add as a small handler or DirectHandler extension following the registry pattern. Make the threaded comment UI generic over CommentTarget (Post(id) | Series(seriesId, episodeId)) or build it for series_comments. No notifications for series comments. Read the player/handler code, comments UI, SeriesRepository and the Phase 3 screens first.

LAYOUT (top to bottom)
1) Player 16:9, sticky while the rest scrolls: tap to show/hide controls, play/pause, seek, double-tap +-10 s, mute, fullscreen (landscape, sensor rotation, hide system bars, keep screen on, back exits fullscreen), "Skip intro" while position is between intro_start_sec and intro_end_sec (only if both set), buffering spinner, error card.
2) Info: series title, "S1 - E3: title", views_count, air_date, expandable description.
3) Actions: Like (episode_likes), + My List, Share, Report (sheet: Broken link / Wrong episode / Copyright / Other -> source_reports insert with the CURRENT source id).
4) Source chips "Server 1 - Hindi dub 720p" from episode_sources (is_active, order priority asc). Language filter chips if languages differ; remember the preferred language locally.
5) Prev / Next episode and autoplay-next (5 s cancelable countdown card at the end).
6) Tabs: Episodes (current highlighted, season switcher) | Comments (series_comments for this episode_id; threaded via parent_id; composer pinned at bottom; edit = content only; delete own) | Related (get_related_series).

SOURCE LOGIC
- Order by priority, but prefer the last successful source id for this series (stored locally) and the preferred language.
- Try in order: resolve via registry, start playback; on error or not ready within 10 s show "Switching to Server N..." and move on; when all fail show the error card with Retry, Report and "Open source in browser".
- Each success saves the local last-working source. On a hard failure send reportSource(reason='broken') at most once per user per source per day, silently, ONLY when logged in (guests cannot report). Do not spam: the server increments fail_count per report.
- Embed sources (YouTube/Facebook/Instagram) use the WebView embed in the same frame; native sources use Media3. One player instance at a time; release on leave, rotation and lifecycle stop.

PROGRESS + VIEWS
- Resume from watch_progress for this episode (seek on ready, toast "Resume from 12:30").
- Save every 15 s while playing and on pause/exit/background via upsert (user_id, episode_id, series_id, position_sec, duration_sec, completed) with on_conflict=user_id,episode_id. completed=true at >= 90%.
- recordView once per episode after 30 s of ACTUAL playback; guests pass the stored random viewer key. Guests may watch; progress, likes, comments and reports ask for login (existing prompt).
- Autoplay next marks the previous episode completed and loads the next episode's sources.

OTHER
- Comments preview on the details page uses the same data (episode_id null = whole-series comments; episode comments use the episode id).
- Series details menu has "Report content" (copyright/other). Because source_reports needs a source id, attach it to the first active source of the currently selected episode, and if the series has no source show a message instead.
- Offline/slow states, rotation without restarting playback, picture-in-picture optional.

DONE WHEN: tapping an episode plays video from the DB sources with automatic fallback; progress, resume, skip intro, autoplay-next, episode like, threaded comments, related and report work; fullscreen/rotation correct; no leaks; no SQL used; no theme change; builds debug and release. List changed files and unfinished items.
```

---

## Notes for you (not for the agent)
- Backend gaps already fixed before writing these prompts: admin role change (`set_user_role`) and report handling (`source_reports.status`, admin/owner update, admin delete).
- Still open and NOT in the app prompts: nobody is admin yet; no manual shelf exists, so creators will see an empty "Show in shelves" list until an admin creates one; movies and dramas have no titles yet.
- Security reminder (unrelated to series): wallet/points functions are still callable by any user.
