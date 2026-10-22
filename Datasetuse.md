# 📘 Qylomi Master Database Schema & Dataset Specification (`Datasetuse.md`)

> **Version**: 2.5.0  
> **Backend**: Supabase (PostgreSQL 15+)  
> **Brand / System**: Qylomi Social Platform (by Naxxivo)  
> **Audience**: AI Agents, Backend Engineers, Full-Stack Developers  
> **Primary Purpose**: Complete architectural blueprint of all database tables, columns, constraints, foreign keys, Row-Level Security (RLS) policies, database triggers, RPC stored procedures, and programmatic usage patterns.

---

## 📑 Table of Contents

1. [Architectural Overview & Core Principles](#1-architectural-overview--core-principles)
2. [Primary Table: `profiles` (User Identity & Gamification)](#2-primary-table-profiles)
   - [Column Definitions](#profiles-column-definitions)
   - [RLS Security Policies](#profiles-rls-policies)
   - [Triggers & Lifecycle](#profiles-triggers--lifecycle)
   - [Code Usage Examples](#profiles-code-usage-examples)
3. [Primary Table: `posts` (Content, Media & Embeds)](#3-primary-table-posts)
   - [Column Definitions](#posts-column-definitions)
   - [Content Formatting & Media Token Standards](#posts-content-formatting--media-tokens)
   - [Post Categories & Types (`IdeaType`)](#posts-categories--ideatype)
   - [RLS Security Policies](#posts-rls-policies)
   - [Code Usage Examples](#posts-code-usage-examples)
4. [Taxonomy: `tags` & `post_tags`](#4-taxonomy-tags--post_tags)
5. [Social Engagement Tables](#5-social-engagement-tables)
   - [`post_reactions` (Appreciations / Likes)](#post_reactions)
   - [`comments` (Threaded Replies)](#comments)
   - [`post_relays` (Quote Reposts / Shares)](#post_relays)
   - [`saved_posts` (Private Bookmarks)](#saved_posts)
   - [`follows` (Social Graph: Trackers & Tracking)](#follows)
6. [Notifications: `notifications`](#6-notifications-notifications)
7. [Communities / Groups: `groups` & `group_members`](#7-communities--groups)
8. [Creator Gamification & Wallet: `wallet_transactions`](#8-creator-gamification--wallet)
9. [Monetization & Video Analytics: `post_views`, `creator_monetization`, `creator_payout_requests`](#9-creator-monetization--video-analytics)
10. [Playlists: `playlists`](#10-video--media-playlists-playlists)
11. [Algorithmic Feed Telemetry: `user_events` & `get_feed()`](#11-algorithmic-feed-telemetry-user_events)
12. [Animation Studio: `animations`, `animation_frames`, `animation_subtitles`](#12-animation-studio)
13. [Safety & Moderation: `reports`](#13-safety--moderation-reports)
14. [AI Agent Safe Execution Checklist (বাংলা ও English নির্দেশিকা)](#14-ai-agent-safe-execution-checklist)

---

## 1. Architectural Overview & Core Principles

The Qylomi platform relies on a normalized Supabase PostgreSQL database architecture designed for high scalability, real-time reactive UI, automated counter synchronization, and strict multi-tenant data isolation through Row-Level Security (RLS).

### Core Architectural Laws for AI Agents:
1. **Auth Mirroring**: User identity stems from Supabase Auth (`auth.users`). Every entry in `public.profiles` has its `id` exactly matching `auth.users.id`.
2. **Never Manually Mutate Counts in Posts**: Columns like `appreciations_count`, `replies_count`, and `relays_count` are controlled by PostgreSQL triggers (`trg_post_reaction_count`, etc.). Do NOT issue manual `UPDATE posts SET appreciations_count = ...` from the client.
3. **Idempotent Gamification**: Creator points and wallet transactions use an atomic RPC function (`process_creator_wallet_transaction`) with idempotency keys to prevent double-spending or duplicate reward assignment.
4. **Soft Client Fallbacks**: The frontend maintains local storage fallback caches (`localStorage` and IndexedDB cache in `src/utils/postCache.ts`) so UI components continue functioning seamlessly in offline or demo modes.

---

## 2. Primary Table: `profiles`

The `profiles` table stores user identity, public biographical details, portfolio information, and creator gamification attributes.

### `profiles` Column Definitions

| Column Name | SQL Type | Nullable | Default | Description & Usage |
| :--- | :--- | :--- | :--- | :--- |
| `id` | `UUID` | **NO** | `REFERENCES auth.users(id)` | **Primary Key**. Mirrors Supabase Auth UID. Cascades on user deletion. |
| `username` | `TEXT` | **NO** | *None* | **Unique handle** (3-30 chars, alphanumeric + underscore). Used in routing `/profile/:username`. |
| `display_name` | `TEXT` | **NO** | *None* | Full name or nickname displayed across feeds, headers, and comments. |
| `bio` | `TEXT` | YES | `NULL` | User profile biography or mission statement. |
| `website` | `TEXT` | YES | `NULL` | External website or portfolio URL. |
| `avatar_url` | `TEXT` | YES | `NULL` | Public image URL for avatar picture (hosted on Catbox, Supabase Storage, or external CDN). |
| `cover_url` | `TEXT` | YES | `NULL` | Large banner / header image displayed on top of the profile page. |
| `headline` | `TEXT` | YES | `NULL` | Professional headline or short status tag (e.g., "Full-Stack Dev & AI Researcher"). |
| `age` | `INTEGER` | YES | `NULL` | Optional age parameter. |
| `gender` | `TEXT` | YES | `NULL` | Gender identity string (e.g., 'Male', 'Female', 'Non-Binary', 'Prefer not to say'). |
| `location` | `TEXT` | YES | `NULL` | Geographical location (e.g., "Dhaka, Bangladesh" or "San Francisco, CA"). |
| `work` | `TEXT` | YES | `NULL` | Current occupation or workplace (e.g., "Software Engineer at Naxxivo"). |
| `education` | `TEXT` | YES | `NULL` | School, university, or educational background. |
| `skills` | `TEXT[]` | YES | `NULL` | Array of skill keywords (e.g., `['React', 'TypeScript', 'PostgreSQL', 'Tailwind']`). |
| `social_links` | `JSONB` | YES | `'{}'::jsonb` | Key-value store of social URLs: `{"github": "...", "twitter": "...", "facebook": "...", "youtube": "..."}`. |
| `points` | `INTEGER` | **NO** | `0` | Cumulative gamification XP points earned by the creator. |
| `creator_rank` | `TEXT` | **NO** | `'Novice Explorer'` | Tier title based on XP ('Novice Explorer', 'Active Contributor', 'Idea Pioneer', 'Community Mentor', 'Master Craftsman'). |
| `creator_level` | `INTEGER` | **NO** | `1` | Numerical creator level (1 to 5). |
| `badges` | `JSONB` | **NO** | `'[]'::jsonb` | Array of badge identifier strings: `['first_spark', 'century_club', 'grandmaster']`. |
| `points_history` | `JSONB` | YES | `'[]'::jsonb` | Audit log array storing recent transaction records. |
| `created_at` | `TIMESTAMPTZ` | **NO** | `now()` | Account registration timestamp in UTC. |
| `updated_at` | `TIMESTAMPTZ` | **NO** | `now()` | Last profile modification timestamp. |

### Client-Side Computed Virtual Fields:
When queries retrieve profiles or when `getProfileWithStats` runs, the client joins or calculates:
- `ideas_count`: Total posts authored by the user.
- `trackers_count`: Total followers who follow this user (`SELECT COUNT(*) FROM follows WHERE following_id = id`).
- `tracking_count`: Total accounts this user follows (`SELECT COUNT(*) FROM follows WHERE follower_id = id`).
- `is_tracking`: Boolean indicating if the currently authenticated user tracks this profile.

### `profiles` RLS Policies

```sql
-- 1. Anyone can view profiles (Public read)
CREATE POLICY "Public profiles are viewable by everyone"
    ON public.profiles FOR SELECT USING (true);

-- 2. Authenticated user can create their own profile entry
CREATE POLICY "Users can insert their own profile"
    ON public.profiles FOR INSERT WITH CHECK (auth.uid() = id);

-- 3. Only the profile owner can modify their profile
CREATE POLICY "Users can update own profile"
    ON public.profiles FOR UPDATE USING (auth.uid() = id);
```

### `profiles` Triggers & Lifecycle

When a new user signs up via Supabase Auth, PostgreSQL automatically invokes `handle_new_user()` via trigger `on_auth_user_created`:
```sql
CREATE OR REPLACE FUNCTION public.handle_new_user()
RETURNS TRIGGER AS $$
DECLARE
    clean_username TEXT;
BEGIN
    clean_username := COALESCE(
        NEW.raw_user_meta_data->>'username',
        'user_' || SUBSTRING(NEW.id::TEXT, 1, 8)
    );

    INSERT INTO public.profiles (id, username, display_name, avatar_url)
    VALUES (
        NEW.id,
        clean_username,
        COALESCE(NEW.raw_user_meta_data->>'display_name', clean_username),
        NEW.raw_user_meta_data->>'avatar_url'
    )
    ON CONFLICT (id) DO NOTHING;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;
```

### `profiles` Code Usage Examples

#### Fetch Profile by Username
```typescript
import { supabase } from '../utils/supabase/client';

export async function fetchProfile(username: string) {
  const { data, error } = await supabase
    .from('profiles')
    .select('*')
    .ilike('username', username.trim())
    .maybeSingle();

  if (error) throw error;
  return data;
}
```

#### Update Profile Information
```typescript
export async function updateProfile(userId: string, updates: Partial<Profile>) {
  const { data, error } = await supabase
    .from('profiles')
    .update({
      ...updates,
      updated_at: new Date().toISOString(),
    })
    .eq('id', userId)
    .select()
    .single();

  if (error) throw error;
  return data;
}
```

---

## 3. Primary Table: `posts`

The `posts` table is the core content storage table in Qylomi. It handles regular micro-posts, tutorials, long-form articles, media embeds, TikTok videos, YouTube videos, and pixel-art animations.

### `posts` Column Definitions

| Column Name | SQL Type | Nullable | Default | Description & Usage |
| :--- | :--- | :--- | :--- | :--- |
| `id` | `UUID` | **NO** | `gen_random_uuid()` | **Primary Key**. Globally unique post identifier. |
| `user_id` | `UUID` | **NO** | *None* | **Foreign Key** referencing `public.profiles(id)` on delete cascade. Post author. |
| `group_id` | `UUID` | YES | `NULL` | **Foreign Key** referencing `public.groups(id)`. Set when published inside a community. |
| `type` | `TEXT` | **NO** | `'Post'` | Category tag conforming to `CHECK (type IN (...))`. See list below. |
| `title` | `TEXT` | **NO** | *None* | Post title / headline (3 to 250 characters). |
| `content` | `TEXT` | **NO** | *None* | Main markdown body, embedded tokens, descriptions, code blocks. |
| `external_url` | `TEXT` | YES | `NULL` | Optional link (website, repository, TikTok, YouTube, Drive, direct video URL). |
| `video_preview_urls` | `JSONB` / `TEXT[]` | YES | `NULL` | Resolved direct streamable video URLs or CDN mirrors. |
| `attachments` | `JSONB` | YES | `'[]'::jsonb` | Array of file metadata objects (`PostAttachment`). |
| `pixel_art` | `JSONB` | YES | `NULL` | Stores raw pixel canvas grids, frame sequences, or color IDs. |
| `appreciations_count` | `INTEGER` | **NO** | `0` | **Trigger-Managed**. Total likes/appreciations count. |
| `replies_count` | `INTEGER` | **NO** | `0` | **Trigger-Managed**. Total comment replies count. |
| `relays_count` | `INTEGER` | **NO** | `0` | **Trigger-Managed**. Total shares/reposts count. |
| `created_at` | `TIMESTAMPTZ` | **NO** | `now()` | Timestamp of post creation. |
| `updated_at` | `TIMESTAMPTZ` | **NO** | `now()` | Timestamp of last edit. |

### `posts` Content Formatting & Media Tokens

In Qylomi, media embeds can either be attached via the `attachments` column, `external_url`, or injected directly into `content` using standard token syntax:

1. **Embedded Video Token**:
   ```
   [video:https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4|Demo Title|16:9]
   ```
2. **Embedded Audio Token**:
   ```
   [audio:https://files.catbox.moe/example.mp3|Podcast Episode 1]
   ```
3. **Pixel Art Animation Token**:
   ```
   <!--PIXEL_ART_DATA:{"gridSize":32,"fps":8,"frames":[...]}-->
   ```

### Post Attachment Schema (`PostAttachment` JSONB structure):
```typescript
export interface PostAttachment {
  id: string;               // Unique string (e.g. 'att_1729000000000')
  name: string;             // Display name of the file
  filename?: string;        // Raw filename
  url: string;              // Direct URL (Catbox, Cloudflare R2, Supabase Storage)
  size: number;             // File size in bytes
  type: string;             // MIME type (e.g. 'video/mp4', 'image/jpeg')
  category: 'video' | 'audio' | 'image' | 'document' | 'archive' | 'apk' | 'code' | 'other';
  uploaded_at?: string;     // ISO UTC timestamp
}
```

### `posts` Categories & `IdeaType`

The `type` column must be one of the following string literals:
- `'Post'` - General social micro-post
- `'Movie'` - Movie review, clip, or discussion
- `'Adult'` - 18+ restricted discussion or artistic content
- `'Cartoon'` - Cartoon and 2D animation clips
- `'Anime'` - Anime discussions, amvs, or reviews
- `'News'` - Breaking news, announcements
- `'Restricted'` - Moderated or age-gated post
- `'AI Prompt'` - Prompts for LLMs, Midjourney, Stable Diffusion
- `'Code'` - Snippets, algorithms, development tips
- `'Tip'` - Short productivity advice
- `'Trick'` - Clever hacks or shortcuts
- `'Idea'` - Concept proposals, feature brainstorms
- `'Tutorial'` - Step-by-step guides
- `'Project'` - Showcase of completed or WIP projects
- `'Update'` - Changelogs and platform updates
- `'Resource'` - Free tools, design systems, assets
- `'Question'` - Community questions & troubleshooting
- `'Discovery'` - Novel scientific or creative findings
- `'Learning'` - Study notes and knowledge summaries
- `'General'` - Miscellaneous topics

### `posts` RLS Policies

```sql
-- Public can read all published posts
CREATE POLICY "Posts are viewable by everyone"
    ON public.posts FOR SELECT USING (true);

-- Authenticated users can publish posts authored by their own UID
CREATE POLICY "Authenticated users can create posts"
    ON public.posts FOR INSERT WITH CHECK (auth.uid() = user_id);

-- Authors can modify their own posts
CREATE POLICY "Authors can update own posts"
    ON public.posts FOR UPDATE USING (auth.uid() = user_id);

-- Authors can delete their own posts
CREATE POLICY "Authors can delete own posts"
    ON public.posts FOR DELETE USING (auth.uid() = user_id);
```

### `posts` Code Usage Examples

#### Fetch Feed with Joined Profile, Group, Tags, and Author Info:
```typescript
export async function getFeedPosts(page = 1, pageSize = 20) {
  const from = (page - 1) * pageSize;
  const to = from + pageSize - 1;

  const { data, error } = await supabase
    .from('posts')
    .select(`
      *,
      profiles:user_id (
        id,
        username,
        display_name,
        avatar_url,
        headline
      ),
      groups:group_id (
        id,
        name,
        slug,
        avatar_url,
        privacy
      ),
      post_tags (
        tags:tag_id (
          id,
          name
        )
      )
    `)
    .order('created_at', { ascending: false })
    .range(from, to);

  if (error) throw error;
  return data;
}
```

#### Insert New Post with Attachments:
```typescript
export async function createPost(payload: {
  userId: string;
  type: IdeaType;
  title: string;
  content: string;
  externalUrl?: string;
  attachments?: PostAttachment[];
  groupId?: string;
}) {
  const { data: post, error } = await supabase
    .from('posts')
    .insert({
      user_id: payload.userId,
      type: payload.type || 'Post',
      title: payload.title.trim(),
      content: payload.content.trim(),
      external_url: payload.externalUrl || null,
      attachments: payload.attachments || [],
      group_id: payload.groupId || null,
    })
    .select()
    .single();

  if (error) throw error;
  return post;
}
```

---

## 4. Taxonomy: `tags` & `post_tags`

Qylomi uses an M:N (many-to-many) relationship between `posts` and `tags`.

### `tags` Table Definition
- `id` (`UUID`, PK)
- `name` (`TEXT`, UNIQUE, lowercase alphanumeric e.g. `'react'`, `'typescript'`, `'video'`)
- `posts_count` (`INTEGER`, default 0)
- `created_at` (`TIMESTAMPTZ`, default `now()`)

### `post_tags` Table Definition
- `post_id` (`UUID`, FK to `posts(id)` on delete cascade)
- `tag_id` (`UUID`, FK to `tags(id)` on delete cascade)
- `PRIMARY KEY (post_id, tag_id)`

### RLS Policies:
- `SELECT`: Public (`true`)
- `INSERT`: Authenticated (`auth.role() = 'authenticated'`)

---

## 5. Social Engagement Tables

### `post_reactions` (Appreciations / Likes)
Tracks whether a user has liked/appreciated a post.
- `id` (`UUID`, PK)
- `post_id` (`UUID`, FK to `posts(id)` on delete cascade)
- `user_id` (`UUID`, FK to `profiles(id)` on delete cascade)
- `reaction_type` (`TEXT`, default `'appreciate'`)
- `created_at` (`TIMESTAMPTZ`)
- **Constraint**: `UNIQUE (post_id, user_id, reaction_type)`

#### Reaction Counter Trigger:
```sql
CREATE OR REPLACE FUNCTION public.update_post_reaction_count()
RETURNS TRIGGER AS $$
BEGIN
    IF TG_OP = 'INSERT' THEN
        UPDATE public.posts
        SET appreciations_count = appreciations_count + 1
        WHERE id = NEW.post_id;
    ELSIF TG_OP = 'DELETE' THEN
        UPDATE public.posts
        SET appreciations_count = GREATEST(0, appreciations_count - 1)
        WHERE id = OLD.post_id;
    END IF;
    RETURN NULL;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;
```

---

### `comments` (Threaded Replies)
Supports nested threaded discussions beneath any post.
- `id` (`UUID`, PK)
- `post_id` (`UUID`, FK to `posts(id)` on delete cascade)
- `user_id` (`UUID`, FK to `profiles(id)` on delete cascade)
- `parent_id` (`UUID`, Nullable, FK to `comments(id)` on delete cascade for replies)
- `content` (`TEXT`, 1-3000 chars)
- `created_at` (`TIMESTAMPTZ`)
- `updated_at` (`TIMESTAMPTZ`)

#### Comment Counter Trigger:
- On `INSERT`: increments `posts.replies_count` by 1.
- On `DELETE`: decrements `posts.replies_count` by 1.

---

### `post_relays` (Quote Reposts / Shares)
Allows users to share or quote a post into their followers' streams.
- `id` (`UUID`, PK)
- `post_id` (`UUID`, FK to `posts(id)` on delete cascade)
- `user_id` (`UUID`, FK to `profiles(id)` on delete cascade)
- `quote` (`TEXT`, Nullable commentary)
- `created_at` (`TIMESTAMPTZ`)
- **Constraint**: `UNIQUE (post_id, user_id)`

#### Relay Counter Trigger:
- Automatically increments or decrements `posts.relays_count`.

---

### `saved_posts` (Private Bookmarks)
Stores bookmarked posts per user.
- `id` (`UUID`, PK)
- `post_id` (`UUID`, FK to `posts(id)` on delete cascade)
- `user_id` (`UUID`, FK to `profiles(id)` on delete cascade)
- `created_at` (`TIMESTAMPTZ`)
- **Constraint**: `UNIQUE (post_id, user_id)`

#### RLS Policies:
- Strictly private:
  ```sql
  CREATE POLICY "Users can view their own saved posts"
      ON public.saved_posts FOR SELECT USING (auth.uid() = user_id);
  CREATE POLICY "Users can save posts"
      ON public.saved_posts FOR INSERT WITH CHECK (auth.uid() = user_id);
  CREATE POLICY "Users can remove their saved posts"
      ON public.saved_posts FOR DELETE USING (auth.uid() = user_id);
  ```

---

### `follows` (Social Graph: Trackers & Tracking)
Handles following relationships between users. In Qylomi nomenclature:
- **Tracker** = Follower (`follower_id`)
- **Tracking** = Following (`following_id`)
- Columns: `id`, `follower_id`, `following_id`, `created_at`.
- Constraints: `UNIQUE (follower_id, following_id)` and `CHECK (follower_id <> following_id)`.

---

## 6. Notifications: `notifications`

Stores user notifications for reactions, comments, quote relays, and new followers.
- `id` (`UUID`, PK)
- `recipient_id` (`UUID`, FK to `profiles(id)` on delete cascade)
- `actor_id` (`UUID`, FK to `profiles(id)` on delete cascade)
- `type` (`TEXT`, CHECK `type IN ('appreciate', 'reply', 'relay', 'track')`)
- `post_id` (`UUID`, Nullable, FK to `posts(id)`)
- `comment_id` (`UUID`, Nullable, FK to `comments(id)`)
- `read` (`BOOLEAN`, default `false`)
- `created_at` (`TIMESTAMPTZ`)

### Secure Stored Procedure `send_notification()`
Always invoke the database RPC instead of raw inserts to guarantee validation:
```sql
SELECT public.send_notification(
    p_recipient_id := 'target-user-uuid',
    p_type := 'appreciate',
    p_post_id := 'post-uuid',
    p_comment_id := NULL
);
```

---

## 7. Communities / Groups: `groups` & `group_members`

### `groups` Table Definition
- `id` (`UUID`, PK)
- `name` (`TEXT`, Community title)
- `slug` (`TEXT`, UNIQUE slug e.g. `'web-development'`)
- `description` (`TEXT`)
- `avatar_url` (`TEXT`)
- `cover_url` (`TEXT`)
- `privacy` (`TEXT`, `'public'` | `'restricted'` | `'private'`)
- `category` (`TEXT`, default `'general'`)
- `rules` (`JSONB`, Array of string community rules)
- `creator_id` (`UUID`, FK to `profiles(id)`)
- `members_count` (`INTEGER`, default 1)
- `posts_count` (`INTEGER`, default 0)
- `created_at`, `updated_at`

### `group_members` Table Definition
- `id` (`UUID`, PK)
- `group_id` (`UUID`, FK to `groups(id)` on delete cascade)
- `user_id` (`UUID`, FK to `profiles(id)` on delete cascade)
- `role` (`TEXT`, `'admin'` | `'moderator'` | `'member'`)
- `status` (`TEXT`, `'active'` | `'pending'` | `'banned'`)
- `joined_at` (`TIMESTAMPTZ`)
- **Constraint**: `UNIQUE (group_id, user_id)`

### Automatic Group Triggers:
1. `on_group_created`: Automatically inserts the group creator as an `'admin'` with status `'active'`.
2. `on_group_member_change`: Automatically recalculates and updates `groups.members_count`.

---

## 8. Creator Gamification & Wallet

Qylomi features a gamified points ledger and rank progression engine.

### Rank & Level Hierarchy

| Level | Rank Title | Required XP Points |
| :---: | :--- | :--- |
| **1** | Novice Explorer | 0 - 199 |
| **2** | Active Contributor | 200 - 599 |
| **3** | Idea Pioneer | 600 - 1,499 |
| **4** | Community Mentor | 1,500 - 3,999 |
| **5** | Master Craftsman | 4,000+ |

### Badges:
- `'first_spark'`: Awarded on creating first post.
- `'century_club'`: Awarded on crossing 100 XP.
- `'grandmaster'`: Awarded upon reaching Level 4+.

### `wallet_transactions` Table Definition
- `id` (`UUID`, PK)
- `user_id` (`UUID`, FK to `profiles(id)`)
- `action` (`TEXT`, e.g. `'CREATE_POST'`, `'APPRECIATE_POST'`, `'RECEIVE_APPRECIATION'`)
- `points` (`INTEGER`, positive or negative delta)
- `balance_after` (`INTEGER`, balance after applying transaction)
- `description` (`TEXT`)
- `reference_id` (`TEXT`, e.g. post ID or comment ID)
- `reference_type` (`TEXT`, e.g. `'post'`, `'comment'`)
- `idempotency_key` (`TEXT`, UNIQUE index for deduplication)
- `created_at` (`TIMESTAMPTZ`)

### Atomic RPC Function: `process_creator_wallet_transaction`
```typescript
const { data, error } = await supabase.rpc('process_creator_wallet_transaction', {
  target_user_id: userId,
  points_to_add: 25,
  action_name: 'CREATE_POST',
  action_description: 'Created a new knowledge post',
  ref_id: postId,
  ref_type: 'post',
  idemp_key: `CREATE_POST:${userId}:${postId}`,
});
```

---

## 9. Creator Monetization & Video Analytics

### `post_views` Table Definition
Tracks view impressions and watch duration for monetization and video analytics.
- `id` (`UUID`, PK)
- `post_id` (`UUID`, FK to `posts(id)` on delete cascade)
- `user_id` (`UUID`, Nullable, FK to `profiles(id)`)
- `ip_hash` (`TEXT`, Anonymized IP hash for bot/abuse protection)
- `watch_duration_seconds` (`INTEGER`, total seconds played)
- `created_at` (`TIMESTAMPTZ`)

### `creator_monetization` / `monetization_stats` Table Definition
Stores creator monetization status and revenue metrics.
- `user_id` (`UUID`, PK, FK to `profiles(id)`)
- `status` (`TEXT`, CHECK `status IN ('not_eligible', 'eligible', 'pending_review', 'approved', 'suspended')`)
- `total_views` (`BIGINT`, default 0)
- `total_watch_seconds` (`BIGINT`, default 0)
- `total_monetized_views` (`BIGINT`, default 0)
- `total_earnings_usd` (`NUMERIC(10,2)`, default 0.00)
- `payout_method` (`TEXT`, `'paypal'` | `'crypto'`)
- `payout_details` (`TEXT`, email address or wallet address)
- `applied_at`, `approved_at`, `created_at`, `updated_at`

### Eligibility Criteria:
- **Minimum Followers**: 500 followers (`trackers_count >= 500`)
- **Minimum Views**: 10,000 views (`total_views >= 10,000`)
- **CPM Rate**: $1.00 USD per 1,000 qualified views ($0.001 per view)

### `creator_payout_requests` Table Definition
- `id` (`UUID`, PK)
- `user_id` (`UUID`, FK to `profiles(id)`)
- `amount_usd` (`NUMERIC(10,2)`)
- `method` (`TEXT`)
- `status` (`TEXT`, `'pending'` | `'processed'` | `'rejected'`)
- `created_at` (`TIMESTAMPTZ`)

---

## 10. Video & Media Playlists: `playlists`

Users can curate and save playlists containing videos from YouTube, TikTok, Facebook, Instagram, Google Drive, or Catbox.

### `playlists` Table Definition
- `id` (`UUID`, PK)
- `user_id` (`UUID`, FK to `profiles(id)`)
- `title` (`TEXT`, 1-200 characters)
- `description` (`TEXT`)
- `cover_url` (`TEXT`)
- `is_public` (`BOOLEAN`, default `true`)
- `items` (`JSONB`, Array of `PlaylistItem` objects)
- `created_at`, `updated_at`

### `PlaylistItem` JSONB Object Schema:
```json
{
  "id": "item_1729000000",
  "title": "React 19 Deep Dive",
  "url": "https://www.youtube.com/watch?v=dQw4w9WgXcQ",
  "video_type": "youtube",
  "thumbnail_url": "https://img.youtube.com/vi/dQw4w9WgXcQ/hqdefault.jpg",
  "post_id": "optional-post-uuid",
  "author_name": "Channel Name",
  "duration": "14:20",
  "added_at": "2026-10-22T00:00:00Z"
}
```

---

## 11. Algorithmic Feed Telemetry: `user_events`

Qylomi features an interest-decay feed ranking algorithm (`get_feed` RPC).

### `user_events` Table Definition
- `id` (`BIGINT`, IDENTITY PK)
- `user_id` (`UUID`, FK to `auth.users(id)`)
- `post_id` (`UUID`, FK to `posts(id)`)
- `event_type` (`TEXT`, `'impression'` | `'open'` | `'like'` | `'comment'` | `'relay'` | `'bookmark'` | `'share'` | `'hide'`)
- `created_at` (`TIMESTAMPTZ`)

### Event Weights in Algorithmic Scoring:
- `open`: +1.0
- `like`: +3.0
- `bookmark`: +4.0
- `comment`: +5.0
- `share` / `relay`: +6.0
- `hide`: -8.0
- **Half-Life Decay**: Interest decays by 50% every 14 days. Post freshness decays by 50% every 2 days.

---

## 12. Animation Studio

Tables supporting the built-in 2D/pixel art animation engine:
- `animations`: Stores animation project metadata (FPS, grid size, duration, loop status, views, audio track URL).
- `animation_frames`: Ordered sequence of frame states (`frame_order`, `timestamp_ms`, `pixel_ids[]`, `subtitle_text`).
- `animation_subtitles`: Synchronized subtitles (`start_time_ms`, `end_time_ms`, `speaker_name`, `dialogue_text`).

---

## 13. Safety & Moderation: `reports`

- `id` (`UUID`, PK)
- `reporter_id` (`UUID`, FK to `profiles(id)`)
- `post_id` (`UUID`, FK to `posts(id)`)
- `reason` (`TEXT`, e.g. `'spam'`, `'harassment'`, `'misinformation'`, `'inappropriate'`)
- `details` (`TEXT`)
- `status` (`TEXT`, `'pending'` | `'reviewed'` | `'dismissed'`)
- `created_at` (`TIMESTAMPTZ`)

---

## 14. AI Agent Safe Execution Checklist (বাংলা ও English নির্দেশিকা)

যেকোনো AI Agent বা নতুন ডেভেলপার যখন এই ডাটাবেসে নতুন কোনো ফিচার যোগ করবে বা কুয়েরি লিখবে, তখন নিচের নিয়মগুলো কঠোরভাবে মেনে চলতে হবে:

### 1. `profiles` ব্যবহারের নিয়ম:
- কখনোই সরাসরি `auth.users` টেবিল সিলেক্ট করবে না; সবসময় `public.profiles` টেবিল ব্যবহার করবে।
- ইউজারের নাম খুঁজতে `ilike('username', cleanUsername)` ব্যবহার করবে যাতে কেস-ইনসেনসিটিভ সার্চ ঠিক থাকে।
- নতুন কলাম যোগ করার প্রয়োজন হলে `ALTER TABLE public.profiles ADD COLUMN IF NOT EXISTS ...` কুয়েরি ব্যবহার করবে।

### 2. `posts` ব্যবহারের নিয়ম:
- কোনো পোস্টে ভিডিও থাকলে `external_url` এবং `video_preview_urls` ফিল্ডে লিংক চেক করবে।
- টিকটক ভিডিও সাপোর্ট করার সময় সরাসরি ইউআরএল বা এক্সট্রাক্টেড ডিরেক্ট লিংক প্লেয়ারে ইনপুট দিতে হবে।
- রিঅ্যাকশন কাউন্টার (`appreciations_count`, `replies_count`, `relays_count`) কখনো ক্লায়েন্ট থেকে ম্যানুয়ালি আপডেট করবে না; ডাটাবেস ট্রিগার স্বয়ংক্রিয়ভাবে এগুলো আপডেট করে।

### 3. RLS পলিসির নিয়ম:
- ইউজারের নিজস্ব কোনো ডেটা (যেমন: `saved_posts`, `wallet_transactions`, `notifications`) সিলেক্ট করার সময় সবসময় `auth.uid() = user_id` শর্ত স্বয়ংক্রিয়ভাবে RLS দ্বারা সুরক্ষিত থাকে।
- তাই কোনো অ্যাডমিন ফাংশন বা ব্যাকগ্রাউন্ড স্ক্রিপ্ট লেখার ক্ষেত্রে প্রয়োজনে `SECURITY DEFINER` সমৃদ্ধ RPC তৈরি করবে।

### 4. রিলেশনাল জয়েনিং ফরম্যাট (Supabase JS SDK):
```typescript
// স্ট্যান্ডার্ড কুয়েরি সিনট্যাক্স
const { data, error } = await supabase
  .from('posts')
  .select(`
    id,
    title,
    content,
    type,
    external_url,
    video_preview_urls,
    attachments,
    appreciations_count,
    replies_count,
    relays_count,
    created_at,
    profiles:user_id (
      id,
      username,
      display_name,
      avatar_url,
      headline
    ),
    groups:group_id (
      id,
      name,
      slug
    )
  `)
  .eq('id', targetPostId)
  .maybeSingle();
```

---
*Created and maintained for the Qylomi Platform Ecosystem. Keep this file updated whenever migrations or schema alterations take place.*
