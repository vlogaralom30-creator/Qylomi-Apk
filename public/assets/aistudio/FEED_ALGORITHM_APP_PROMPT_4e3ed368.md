# Website feed algorithm -> Android app

Source: Qylomi website (`algorithm/algorithm.md` v1.0, `get_feed` SQL, `services/feed.ts`, `utils/feedAlgorithm.ts`) checked against the live Supabase database on 2026-10-28.

## 1. How the website algorithm works (summary)

| Part | What it does |
|---|---|
| Ranking (server) | Postgres function `get_feed(p_limit, p_exclude)` ranks posts of the last 30 days (newest 500 candidates). The client never re-ranks, it only restores the returned order. |
| Score | `score = (0.45 interest + 0.15 social + 0.20 popularity + 0.20 freshness + 0.05 jitter) x (seen in last 7 days ? 0.35 : 1)` |
| Interest | Affinity (0..1) to post type (0.30), group (0.20), author (0.20), tag (0.30), built from the user's events of the last 90 days, each event weighted and decayed by `0.5^(age_days/14)` |
| Social | 1 if the user is in the post's group or follows the author |
| Popularity | `min(1, ln(1 + reactions + 2 x comments) / ln(51))` |
| Freshness | `0.5^(age_days/2)` |
| Event weights | impression 0 (only marks "seen"), open +1, like +3, bookmark +4, comment +5, share +6, relay +6, hide -8 (and the post is excluded) |
| Pagination | by exclusion list (`p_exclude` = ids already on screen), never by offset |
| Tracking | table `user_events` (user_id comes from `auth.uid()`, client sends only post_id + event_type), batched every 2 s, fire-and-forget, skipped for guests |
| Feed tabs on the web | For You (ranked by `get_feed`, only when type = All and no tag filter), Latest (chronological), Trending (likes x2 + replies x3), Videos (posts with video content), Bangla (Bengali text/tags), Following (posts of followed users) |
| Fallback | if `get_feed` fails the web falls back to the chronological query |

## 2. Live database status (verified)
- `get_feed(p_limit int default 20, p_exclude uuid[] default '{}')` exists, `SECURITY INVOKER`, `search_path=''`, executable by `anon` and `authenticated`, returns `(post_id uuid, score double precision)`. It uses the denormalized counters and the `follows` table.
- `user_events` exists (508 rows from the website) with RLS: insert own, read own, delete own. Event types allowed: impression, open, like, comment, relay, bookmark, share, hide.
- **No database or SQL work is needed for the app.**

## 3. Current app status (from `q-social (5)`)
- Feed = `getFeedPosts()` -> `getPosts(limit = 50)`, chronological, no pagination, filtered on the client by type chips (All / Post / Idea / Resource / Question).
- No `get_feed` call, no `user_events` tracking, no impressions, no "Reset personalization".
- Already present and reusable: hidden posts (`hidden_posts`, `hidePost`), like/bookmark sets, relay, comments, pull-to-refresh, the session/refresh system.

## 4. Prompt for the app agent

```
TASK
Bring the website's feed algorithm into my Android app (Kotlin, Jetpack Compose, Retrofit/Moshi, Supabase REST). The database side already exists and is live: the Postgres function get_feed and the table user_events. DO NOT create or change any table, column, policy or function, and do not write SQL. Do not change the theme or unrelated screens. Ranking lives in Postgres: the app must never re-rank the For You feed on the client, it only restores the order returned by the server.

First read: SocialRepository.getFeedPosts, SupabaseApi (getPosts and its select string), SupabaseService, SocialViewModel (loadFeed, refreshFeed, feed state), FeedScreen (chips, LazyColumn), PostCard, RelayRepository, PostActionsViewModel/hidePost, CommentsViewModel, ReelsViewModel, the session manager/interceptor, Settings. Show a short plan first.

BACKEND CONTRACT (already live)
1) Ranking RPC: POST {SUPABASE_URL}/rest/v1/rpc/get_feed with JSON {"p_limit": 20, "p_exclude": ["uuid", ...]} -> [{"post_id":"uuid","score":0.83}, ...] in rank order. p_limit is clamped to 1..50. Works for guests (generic ranking) and logged-in users (personalized, RLS applies). Use the normal auth interceptor.
2) Fetch the ranked posts: GET /rest/v1/posts?select=<the same select string the current feed uses, with profiles and the video/photo/text sub tables>&id=in.(id1,id2,...) then sort the rows back into the returned rank order (ids missing from the response are dropped). Apply the existing hydration: isLikedByMe, isBookmarkedByMe, relayed state, and filter hiddenPostIds.
3) Events: POST /rest/v1/user_events with a JSON ARRAY of {"post_id":"uuid","event_type":"impression|open|like|comment|relay|bookmark|share|hide"} (header Prefer: return=minimal). Never send user_id (the database fills it from the token). Allowed event types are exactly those 8.
4) Reset personalization: DELETE /rest/v1/user_events?user_id=eq.<my id> (the policy "user_events delete own" exists).

PART 1 - FEED REPOSITORY AND PAGINATION
- Create FeedRepository (do not grow SocialRepository): getRankedPage(shownIds, pageSize = 20): Result<List<Post>> implementing steps 1-2. Keep the shown-ids set in the ViewModel; send the whole set as p_exclude (cap at the newest 400 ids if it grows larger).
- Pagination is by exclusion list only, never offset: load page 1 with an empty exclude list; load more when the user scrolls within 5 items of the end; hasMore = returned count == pageSize. Pull-to-refresh and the refresh button reset the set and load page 1.
- Race protection: a request counter so stale responses are ignored (fast scrolling, refresh during load); de-duplicate by post id; keep stable keys in LazyColumn.
- FALLBACK: if the RPC or the posts fetch fails (HTTP error, timeout), fall back to the existing chronological getFeedPosts() so the feed never goes blank, and log a short warning. Do not swallow the error as an empty feed: show the existing error state with Retry only when both paths fail.
- Guests: the same call works without a session; do not send events for guests.

PART 2 - FEED TABS (mirror the website)
Add a top tab/chip row above the existing type chips, in this order: For You (default), Latest, Trending, Following, Videos, Bangla. Keep the existing type chips (All / Post / Idea / Resource / Question) as a secondary filter.
- For You: ranked by get_feed, ONLY when the type chip is All (the website also uses it only when no type or tag filter is active). When a type chip other than All is selected, show the chronological list filtered by that type (existing behavior), with a small hint chip "Ranked feed works with All".
- Latest: chronological (existing getPosts), paginated with limit 20 + offset (this list is not ranked, offset is fine here).
- Trending: load a pool of the newest 100 posts and sort by appreciations_count x 2 + replies_count x 3 (descending), exactly like the website.
- Following: posts whose user_id is in my follows (GET follows?follower_id=eq.<me>&select=following_id, then posts?user_id=in.(...)); empty state "Follow creators to see their posts"; hidden for guests (ask to log in).
- Videos: pool of the newest 100 posts, keep those with video content: has a video_posts row, or external_url that parses as a supported video link (use the existing video handler registry), or video preview urls/attachments of category video; newest first.
- Bangla: pool of the newest 100 posts, keep those where title, content, or a tag name contains Bengali characters (Unicode range U+0980..U+09FF) or a tag containing "bangla"; newest first.
- For Trending/Videos/Bangla: if the pool yields fewer than 10 items after filtering, load the next 100 (offset) up to 3 times; pull-to-refresh reloads. Put these pure functions (hasBengaliCharacters, isBanglaPost, isVideoPost, trendingScore, applyPreset) in one file util/FeedPresets.kt with unit tests.
- Remember the last selected tab in DataStore; animate tab changes (fade/slide), keep scroll position per tab while the app is alive.

PART 3 - EVENT TRACKING (UserEventTracker, singleton, never blocks the UI)
- API: track(postId, type). Everything is fire-and-forget: never suspend the caller, never throw, never show an error. Run on an application-scoped coroutine on Dispatchers.IO.
- Queue in memory; flush as ONE batched POST when 2 seconds passed since the first queued event, when the queue reaches 20, and when the app goes to background (ProcessLifecycleOwner ON_STOP). Skip silently when there is no session; on failure keep at most 100 events and retry on the next flush, then drop (never loop).
- Impression de-duplication: at most one impression per post per app session (in-memory set, cleared on logout/account switch). De-duplicate "open" per post per 30 seconds. Clear the queue on logout.
- Impression rule (same as the website): a post card counts as an impression when at least 50% of the card is visible for 1.5 seconds. Implement with LazyListState.layoutInfo visibleItemsInfo (visible fraction = visible pixels / item size) inside snapshotFlow + a per-item timer (cancel when the item drops under 50%); only while the screen is in the foreground (RESUMED). Apply to the feed lists everywhere PostCard is used (feed, profile, group, tag, search); for Reels count an impression after the video was on screen and playing for 1.5 s.
- Where to fire each event, ONLY after the server write succeeded and once per user action:
  * open: when a post details view opens, or when the comments sheet/full post view opens for that post;
  * like: after a successful like (not on un-like); comment: after a successful comment or reply on a post;
  * bookmark: after a successful save (not on un-save); relay: after a successful relay (not undo);
  * share: when the user completes choosing the share action (on tapping Share and launching the chooser);
  * hide: after a successful "Hide this post" (the post then disappears and the server also excludes it).
  Undo actions are not tracked, exactly like the website.
- Event type strings must match the database exactly (8 values). Keep them in a Kotlin enum with a unit test.
- Series, episodes and shelves are NOT part of this algorithm (they have their own shelves); do not send series ids to user_events.

PART 4 - DATA QUALITY (the algorithm only works if posts are saved consistently)
- Check that creating a post (every tab: text, photo, video, pixel art, group posts) always saves type, group_id (when posting in a group) and tags into post_tags with lowercase, trimmed names, because tag/group/type are the interest dimensions. Fix any create path that skips them (do not change the database).

PART 5 - SETTINGS AND PRIVACY
- Settings: "Personalized feed" switch (default ON). When OFF: the For You tab shows Latest, nothing is tracked and no events are sent. "Reset personalization" button with a confirmation dialog -> DELETE /rest/v1/user_events?user_id=eq.<me>, then reload the feed and show a snackbar. One line of explanation: "We use likes, saves, comments and views to rank your feed. Only your own account can see this data."
- Debug builds only: a small overlay on feed cards that shows the returned score, the position and whether the card was already seen (to verify the algorithm), plus the tracker queue size.

PART 6 - QUALITY
- Unit tests: rank-order restoration (ids missing, extra rows), de-duplication, exclusion-list pagination (no repeats across pages), fallback path when the RPC fails, tracker batching/flush rules with MockWebServer (2 s, size 20, background, guest skip), impression de-dup, event enum vs allowed strings, FeedPresets (Bengali detection, video detection, trending score, ordering).
- Performance: no extra recomposition from impression tracking (derive state, avoid per-frame work), the tracker allocates little, Coil/list scrolling unchanged.
- Works with the session/refresh system (401 handling comes from the interceptor); R8 keep rules for any new Moshi models.

DONE WHEN
1. For You shows the server-ranked feed (page size 20, endless scroll without repeats, pull-to-refresh), falls back to Latest if the ranking call fails.
2. Latest, Trending, Following, Videos and Bangla tabs behave like the website.
3. Likes, saves, comments, relays, shares, hides, opens and impressions are sent in batches; guests send nothing; the feed visibly adapts after a few interactions and refresh.
4. Settings switch and Reset work; no database changes were made; tests pass; the project builds in debug and release. List changed files and anything unfinished.
```

## 5. After the app ships (optional checks)
- Interact with a few posts (like, save, comment), pull to refresh: posts of the same type/tag/author should move up; hidden posts never return.
- Server-side tuning later by changing the numbers in `get_feed` (constants are in the `params` CTE), no app update needed.

## 6. Known limits (same as the website, from the spec)
- Ranking pool is the newest 500 posts of the last 30 days, so very old posts do not appear in For You.
- Hide only excludes that single post; relays/bookmarks do not count toward popularity; no diversity control yet (an author can cluster).
- Behavioral tracking: mention it in your privacy policy.
