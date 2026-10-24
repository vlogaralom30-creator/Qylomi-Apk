# Qylomi — Series / Anime / Movie / TV Show Tables (for App Integration)

Only the tables, functions and rules used by **anime, movies and TV shows**. Nothing else.
Project: `ffbmozwsasbhevtehbit` · Base URL: `https://ffbmozwsasbhevtehbit.supabase.co`
Verified against the live database on 2026-10-24 (updated after migration `series_09`: report workflow + `set_user_role`). The website already uses exactly these tables.

**Changelog (latest):** `source_reports` now has `status`, `handled_by`, `handled_at` + admin/owner update and admin delete · new RPC `set_user_role` (admin only) · column write whitelist documented in section 3.

Headers for every REST call:
```
apikey: sb_publishable_kzXQl0X6eNtzFrAuY-3EFw_pYh6H-qr
Authorization: Bearer <user access_token>   (or the same publishable key for guests)
Content-Type: application/json
```
REST: `/rest/v1/<table>` · RPC: `POST /rest/v1/rpc/<function>`
Same accounts as the social app: the app's existing login token works.

---

## 0. Quick answer: which tables?

**16 tables** (all `public`, all RLS on):

| # | Table | One-line purpose |
|---|---|---|
| 1 | `content_categories` | Anime / Movie / Drama tabs |
| 2 | `series` | the title (anime, movie or show) |
| 3 | `series_seasons` | optional season name/poster |
| 4 | `episodes` | episodes (a movie = 1 episode) |
| 5 | `episode_sources` | video links / servers per episode |
| 6 | `series_shelves` | home page sections (config) |
| 7 | `series_shelf_items` | titles inside manual/hero shelves |
| 8 | `series_likes` | like a series |
| 9 | `episode_likes` | like an episode |
| 10 | `series_ratings` | user rating 1–10 |
| 11 | `series_watchlist` | "My List" |
| 12 | `watch_progress` | resume position / continue watching |
| 13 | `watch_views` | view log (no direct client access) |
| 14 | `series_comments` | threaded comments (series or episode) |
| 15 | `series_comment_likes` | like a comment |
| 16 | `source_reports` | report a broken video |

Plus `profiles` (column `role`) for permissions, and 6 RPCs (section 5).
Do **not** use `posts`, `video_posts`, `playlists`, etc. for series. Those belong to the social side.

---

## 1. Relationships

```
content_categories ─< series ─< series_seasons
                        │  └──< episodes ─< episode_sources ─< source_reports
                        │          ├──< episode_likes
                        │          └──< series_comments (episode_id null = whole series) ─< series_comment_likes
                        ├──< series_likes · series_ratings · series_watchlist
                        ├──< watch_progress (also -> episodes) · watch_views
                        └──< series_shelf_items >─ series_shelves
profiles (id = auth uid) owns: series.created_by, all user_id columns
```
A **movie** = one `series` row (category `movie`) + one `episodes` row (season 1, episode 1) + its source(s).
A **TV show / drama** = one `series` row + many episodes, optionally several seasons.

---

## 2. Tables

### 2.1 content_categories
| Column | Type | Notes |
|---|---|---|
| id | uuid PK | `=gen_random_uuid()` |
| slug | text NN UNIQUE | `^[a-z0-9-]+$` |
| name | text NN | |
| sort_order | int NN | `=0` |
| is_active | bool NN | `=true` |
| created_at, updated_at | timestamptz | |

Seed: `anime`, `movie`, `drama`.

### 2.2 series
| Column | Type | Notes |
|---|---|---|
| id | uuid PK | |
| slug | text NN UNIQUE | `^[a-z0-9-]+$` |
| title | text NN | 1–200 chars |
| original_title | text | |
| description | text | |
| poster_url | text | tall 2:3 |
| banner_url | text | wide 16:9 (currently null for all seeded titles; fall back to poster) |
| trailer_url | text | YouTube watch URL etc. |
| category_id | uuid -> content_categories | ON DELETE SET NULL |
| genres | text[] NN | `={}`; auto lowercase/trim/dedupe |
| languages | text[] NN | audio languages, normalized |
| subtitle_languages | text[] NN | normalized |
| rating | numeric(3,1) | editorial 0–10 |
| age_rating | text | `PG-13`, `18+` |
| is_mature | bool NN | `=false` |
| status | text NN | `ongoing` / `completed` / `upcoming` (`=ongoing`) |
| release_year | int | 1900–2100 |
| studio | text | |
| total_episodes | int | >= 0 |
| is_published | bool NN | `=false` (draft) |
| is_featured | bool NN | admin only |
| is_blocked | bool NN, blocked_reason text | admin takedown |
| created_by | uuid -> profiles | |
| avg_user_rating | numeric(3,1) NN | trigger |
| ratings_count, likes_count, comments_count, episodes_count | int NN | triggers |
| views_count | bigint NN | `record_view()` |
| last_episode_at | timestamptz | trigger |
| created_at, updated_at | timestamptz | |

Visible to public when `is_published = true AND is_blocked = false` (owner and admin see their own/all).

### 2.3 series_seasons
`id uuid PK, series_id -> series (cascade), season_number int NN (>=0), title, description, poster_url, created_at, updated_at`. UNIQUE(series_id, season_number).

### 2.4 episodes
| Column | Type | Notes |
|---|---|---|
| id | uuid PK | |
| series_id | uuid NN -> series | cascade |
| season_number | int NN | `=1`, >= 0 |
| episode_number | int NN | >= 0 (0 = special) |
| title, description, thumbnail_url | text | |
| duration_seconds | int | >= 0 |
| air_date | date | |
| intro_start_sec, intro_end_sec | int | >= 0, for "Skip intro" |
| is_published | bool NN | `=true` |
| likes_count, comments_count | int NN | triggers |
| views_count | bigint NN | `record_view()` |
| created_by | uuid -> profiles | |
| created_at, updated_at | timestamptz | |

UNIQUE(series_id, season_number, episode_number).

### 2.5 episode_sources
| Column | Type | Notes |
|---|---|---|
| id | uuid PK | |
| episode_id | uuid NN -> episodes | cascade |
| label | text NN | `='Server 1'` |
| url | text NN | CHECK `^https?://` |
| platform | text | `direct`, `hls`, `youtube`, `gdrive`, ... (client detects/uses) |
| language | text | `hindi dub`, `english`, `japanese sub` |
| quality | text | `480p`, `720p`, `auto` |
| priority | int NN | `=0`, lower plays first |
| is_active | bool NN | `=true` |
| fail_count | int NN | trigger, +1 per `broken` report |
| last_ok_at | timestamptz | reserved |
| created_at, updated_at | timestamptz | |

Always query with `is_active=eq.true&order=priority.asc`.

### 2.6 series_shelves (home sections)
| Column | Type | Notes |
|---|---|---|
| id, slug (UNIQUE), title | | |
| type | text NN | `manual` `genre` `category` `auto_trending` `latest_episodes` `top_rated` `hero` |
| layout | text NN | `row` `hero` `top10` `grid` (`=row`) |
| config | jsonb NN | genre: `{"genre":"horror"}`, category: `{"category":"movie"}`, trending: `{"days":7}` |
| category_id | uuid -> content_categories | null = all tabs |
| position | int NN | lower = higher |
| is_active | bool NN | |
| max_items | int NN | 1–50, `=15` |

Seeded shelves: `featured` (hero, 0), `trending-now` (10), `latest-episodes` (20), `top-rated` (top10, 30), `horror-anime` (genre, 40), `otherworld-isekai` (genre, 50). Empty shelves are omitted by the RPC.

### 2.7 series_shelf_items
PK(shelf_id -> series_shelves, series_id -> series). `position int NN =0`, `created_at`. Used by `manual` and `hero` shelves.

### 2.8 User activity
| Table | Columns | PK / notes |
|---|---|---|
| series_likes | series_id, user_id, created_at | PK(series_id,user_id), public read |
| episode_likes | episode_id, user_id, created_at | PK(episode_id,user_id), public read |
| series_ratings | series_id, user_id, rating int 1–10, created_at, updated_at | PK(series_id,user_id), own rows only |
| series_watchlist | user_id, series_id, created_at | PK(user_id,series_id), own rows only |
| watch_progress | user_id, episode_id, series_id, position_sec int NN =0, duration_sec int, completed bool NN =false, updated_at | PK(user_id,episode_id), own rows only |
| watch_views | id, user_id null, viewer_key null, series_id, episode_id null, created_at | no client access, written by `record_view` |

### 2.9 series_comments
`id uuid PK, series_id -> series NN, episode_id -> episodes null, user_id -> profiles NN, parent_id -> series_comments null (threaded), content text NN (1–3000), likes_count int NN =0, created_at, updated_at`.
`episode_id` null = comment on the whole series. A reply's parent must be in the same series.

### 2.10 series_comment_likes
PK(comment_id -> series_comments, user_id -> profiles), `created_at`. Count in `series_comments.likes_count` (trigger).

### 2.11 source_reports
`id uuid PK, source_id -> episode_sources NN, user_id -> profiles NN, reason text NN (broken | wrong_episode | copyright | other), details text (<=1000), status text NN (open | handled, `='open'`), handled_by uuid -> profiles (set null), handled_at timestamptz, created_at`.
`handled_by` and `handled_at` are filled by a trigger when `status` becomes `handled` (cleared when reopened). Clients may update **only `status`**.

---

## 3. Who can do what

| Role (`profiles.role`) | Abilities |
|---|---|
| guest (anon) | read published series/episodes/sources/shelves/comments; call home, related, search, record_view |
| `user` | + like, rate, watchlist, progress, comment, report a source |
| `creator` | + create/edit/delete **own** series, seasons, episodes, sources |
| `admin` | everything, plus categories, shelves, hero, feature/block, roles |

Write rules:
| Table | Insert | Update | Delete |
|---|---|---|---|
| content_categories, series_shelves | admin | admin | admin |
| series | creator/admin (`created_by = auth.uid()`) | owner/admin | owner/admin |
| series_seasons, episodes, episode_sources | owner of the series / admin | same | same |
| series_shelf_items | admin, or owner of series on a manual shelf | same | same |
| likes, comment likes, watchlist | self | – | self |
| source_reports | self (`details, reason, source_id, user_id`) | admin or series owner (**`status` only**) | admin only |
| series_ratings, watch_progress | self | self | self |
| series_comments | self | self (`content` only) | self / series owner / admin |
| source_reports | self | – | – |

### Column write whitelist (authenticated). Sending any other column returns 403 / 42501 "permission denied for column"
| Table | INSERT columns | UPDATE columns |
|---|---|---|
| series | age_rating, banner_url, category_id, created_by, description, genres, is_featured, is_mature, is_published, languages, original_title, poster_url, rating, release_year, slug, status, studio, subtitle_languages, title, total_episodes, trailer_url | same, minus created_by, plus blocked_reason, is_blocked |
| episodes | air_date, created_by, description, duration_seconds, episode_number, intro_end_sec, intro_start_sec, is_published, season_number, series_id, thumbnail_url, title | same, minus created_by and series_id |
| episode_sources | episode_id, is_active, label, language, platform, priority, quality, url | is_active, label, language, platform, priority, quality, url |
| content_categories | slug, name, sort_order, is_active (+ id, timestamps) | same |
| series_seasons | series_id, season_number, title, description, poster_url | same |
| series_shelves | slug, title, type, layout, config, category_id, position, is_active, max_items | same |
| series_shelf_items | shelf_id, series_id, position | position |
| series_comments | content, episode_id, parent_id, series_id, user_id | content |
| series_ratings | series_id, user_id, rating | rating |
| watch_progress | user_id, episode_id, series_id, position_sec, duration_sec, completed | same |
| source_reports | details, reason, source_id, user_id | status |
| series_likes, episode_likes, series_comment_likes, series_watchlist | key columns only | – (insert/delete only) |

**Never write these columns from the app** (triggers own them): `views_count`, `likes_count`, `comments_count`, `episodes_count`, `ratings_count`, `avg_user_rating`, `last_episode_at`, `fail_count`, `last_ok_at`, comment `likes_count`, `updated_at`. `is_featured`, `is_blocked`, `blocked_reason` are ignored unless admin.

---

## 4. Triggers (automatic)
| Event | Effect |
|---|---|
| insert/delete series_likes, episode_likes, series_comment_likes | like counters |
| series_ratings change | `avg_user_rating`, `ratings_count` |
| series_comments change | `comments_count` on series + episode |
| episodes change | `episodes_count`, `last_episode_at` (published only) |
| source_reports (broken) | `episode_sources.fail_count + 1` |
| source_reports status change | sets/clears `handled_by`, `handled_at` |
| series insert/update | normalize genres/languages/subtitle_languages; block non-admin featured/blocked |
| most tables | `updated_at` |

---

## 5. RPC functions

| Function | Body (JSON) | Returns |
|---|---|---|
| `get_series_home` | `{"p_category":"anime"}` or `{"p_category":null}` | `[{shelf:{id,slug,title,type,layout,position}, items:[card...]}]` |
| `get_related_series` | `{"p_series_id":"<uuid>","p_limit":12}` | `[card...]` |
| `get_continue_watching` | `{"p_limit":12}` (needs login) | `[{series, episode, position_sec, duration_sec, updated_at}]` |
| `record_view` | `{"p_series_id":"<uuid>","p_episode_id":"<uuid>","p_viewer_key":"<random id>"}` | void |
| `search_series` | `{"q":"naruto","p_category":null,"lim":20,"off":0}` | `[card...]` |
| `set_user_role` (**admin only**) | `{"p_user_id":"<uuid>","p_role":"creator"}` | void. Errors: `42501` not admin, `22023` invalid role, `P0001` last admin cannot be demoted, `P0002` user not found |

`record_view`: call after ~30 s of playback. Guests send a random key saved locally; de-duplicated per viewer per episode per hour.

**Card JSON** (Retrofit/Moshi model `SeriesCard`):
```
id, slug, title, original_title, description (first 280 chars), poster_url, banner_url,
category {id, slug, name}, genres[], languages[], rating, avg_user_rating, ratings_count,
status, release_year, age_rating, is_mature, episodes_count, last_episode_at,
views_count, likes_count, created_at
```

---

## 6. REST cheat sheet (Retrofit-friendly)

```
# categories
GET /rest/v1/content_categories?is_active=eq.true&order=sort_order.asc&select=id,slug,name

# series detail by slug
GET /rest/v1/series?slug=eq.naruto-shippuden&select=*,category:content_categories(slug,name)&limit=1

# browse by genre / category / status
GET /rest/v1/series?genres=cs.{horror}&status=eq.ongoing&order=created_at.desc&select=id,slug,title,poster_url,rating,genres&limit=24&offset=0

# seasons / episodes
GET /rest/v1/series_seasons?series_id=eq.<id>&order=season_number.asc
GET /rest/v1/episodes?series_id=eq.<id>&order=season_number.asc,episode_number.asc

# sources of one episode
GET /rest/v1/episode_sources?episode_id=eq.<epId>&is_active=eq.true&order=priority.asc

# watchlist
GET    /rest/v1/series_watchlist?select=created_at,series:series(id,slug,title,poster_url,rating)&order=created_at.desc
POST   /rest/v1/series_watchlist            {"user_id":"<uid>","series_id":"<id>"}
DELETE /rest/v1/series_watchlist?series_id=eq.<id>&user_id=eq.<uid>

# like / unlike
POST   /rest/v1/series_likes                {"series_id":"<id>","user_id":"<uid>"}
DELETE /rest/v1/series_likes?series_id=eq.<id>&user_id=eq.<uid>

# rating (upsert)
POST /rest/v1/series_ratings?on_conflict=series_id,user_id
     Prefer: resolution=merge-duplicates
     {"series_id":"<id>","user_id":"<uid>","rating":9}

# progress (upsert) — send every ~10 s and on pause/exit
POST /rest/v1/watch_progress?on_conflict=user_id,episode_id
     Prefer: resolution=merge-duplicates
     {"user_id":"<uid>","episode_id":"<ep>","series_id":"<id>","position_sec":312,"duration_sec":1420,"completed":false}

# comments (top level; replies use parent_id=eq.<id>)
GET  /rest/v1/series_comments?series_id=eq.<id>&episode_id=is.null&parent_id=is.null&order=created_at.desc&select=*,profile:profiles(username,display_name,avatar_url)
POST /rest/v1/series_comments  {"series_id":"<id>","episode_id":null,"user_id":"<uid>","parent_id":null,"content":"Great episode"}

# report a broken source
POST /rest/v1/source_reports  {"source_id":"<id>","user_id":"<uid>","reason":"broken"}

# admin: reports inbox, mark handled / reopen / delete
GET    /rest/v1/source_reports?status=eq.open&order=created_at.desc&select=*,source:episode_sources(id,label,url,episode:episodes(episode_number,series:series(slug,title)))
PATCH  /rest/v1/source_reports?id=eq.<id>   {"status":"handled"}
DELETE /rest/v1/source_reports?id=eq.<id>   (admin only)

# admin: change a user's role (never PATCH profiles.role, RLS blocks it)
POST   /rest/v1/rpc/set_user_role   {"p_user_id":"<uuid>","p_role":"creator"}
```
Embeds work through the FKs: `episodes.series_id`, `series.category_id`, `series_comments.user_id -> profiles`.
Use `Prefer: return=representation` when you need the inserted row back.

---

## 7. App integration notes
1. Home screen: one call `get_series_home` per category tab. Render by `shelf.layout`: `hero` carousel, `top10` numbered row, `row` normal, `grid`.
2. Banner is null for current data, so use `poster_url` as fallback for hero.
3. Player: pick the first active source by `priority`; on playback error try the next one. `platform = direct/hls` → ExoPlayer, `youtube` → embed/YouTube player, others → WebView.
4. Movies: open the single episode directly (season 1, episode 1).
5. Show "Skip intro" when `intro_start_sec`/`intro_end_sec` are set.
6. Mature titles (`is_mature`): show an 18+ gate.
7. Guests can browse and watch; likes, ratings, watchlist, progress and comments need login.
8. Counters are read-only; refetch after an action to update the UI.

---

## 8. Data currently in the DB (for testing)

| slug | category | status | episodes | sources |
|---|---|---|---|---|
| naruto-shippuden | anime | completed | S1 E1–E10 | 10 (Hindi dub, direct mp4, Cloudflare R2) |
| attack-on-titan | anime | completed | S1 E1 | 1 (Hindi dub, 480p) |
| one-piece | anime | ongoing | S1 E918 | 1 (English, 360p) |

Totals now: 3 series, 12 episodes, 12 sources, 6 shelves, 3 categories, 3 `featured` shelf items. All published, all shown in the `featured` (hero), `trending-now`, `latest-episodes` and `top-rated` shelves. All 5 users currently have role `user`. No movies or dramas added yet (categories exist and are empty).
No admin user exists yet; add content from the SQL Editor (templates in `HIWATCH_AGENT_PROMPT.md`, appendix) until an admin/creator UI exists.
