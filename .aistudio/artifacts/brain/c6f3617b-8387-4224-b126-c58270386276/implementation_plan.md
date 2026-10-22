# Multi-Type Post Creation & Embed Player Upgrade Plan

Upgrade the Qylomi post publishing ecosystem from a single generic post model to a modular, multi-type post system (**Video**, **Photo**, **Text**, and **General Link**) with dedicated relational sub-tables in Supabase (`video_posts`, `photo_posts`, `text_posts`), platform auto-detection (YouTube, TikTok, Facebook, Instagram), live video embed previews, multi-photo galleries, and customizable background color/gradient templates for text posts—all while ensuring **zero data loss** for existing posts.

---

## User Review & Critical Decisions

> [!IMPORTANT]
> The following architectural decisions were confirmed during the interactive planning session:

- **Confirmed Decision 1 (Database Schema Strategy)**: Use **separate relational sub-tables** (`video_posts`, `photo_posts`, `text_posts`) linked to the primary `posts` table via `post_id`. This cleanly isolates type-specific attributes while maintaining backward compatibility and zero data loss for existing posts.
- **Confirmed Decision 2 (Text Post Styling)**: Provide a curated selector of **preset solid background colors and gradient mix templates** (e.g., Sunset Pulse, Neon Cyber, Warm Ember, Deep Emerald) with live text preview.
- **Confirmed Decision 3 (Video Platform Auto-Detection & Player)**: Implement **automatic URL parsing** when a user pastes links from **YouTube, TikTok, Facebook, or Instagram**, displaying live embedded previews and saving the detected platform string (`youtube`, `tiktok`, `facebook`, `instagram`, `direct_url`) in `video_posts.platform`.

---

## 1. Overview & Core Concept

### What It Does
1. **Multi-Type Creation Tabs in Post Dialog**:
   - 📹 **Video Post Tab**: Title, optional thumbnail URL, video URL input with auto-detected platform tag (`youtube`, `tiktok`, `facebook`, `instagram`, `direct`), live interactive embed player preview, category selector, and optional `#tags` / keywords.
   - 📷 **Photo Post Tab**: Main caption, dynamic list of photo image URLs (with a `+ Add Photo` button to attach multiple images), category selector, and `#tags`.
   - ✍️ **Text Post Tab**: Clean long/short text editor, preset background color picker & curated gradient mix cards (with real-time typography styling), category, and `#tags`.
   - 📝 **General Post Tab**: Preserves standard legacy post format.

2. **Feeds & Live Embed Player Integration**:
   - Embedded video renderer that auto-transforms YouTube/TikTok/Facebook/Instagram links into responsive in-feed inline video players.
   - Horizontal multi-photo carousel slider for photo posts.
   - Rich styled cards for text posts with gradient backgrounds and contrast-adjusted typography.

3. **Zero-Data-Loss Architecture**:
   - The primary `posts` table remains the anchor for feed sorting, author details, reactions (`post_reactions`), comments (`comments`), and bookmarks (`saved_posts`).
   - New posts write to `posts` as well as their respective sub-table (`video_posts`, `photo_posts`, `text_posts`).
   - Standard fallback getters guarantee existing legacy posts continue rendering seamlessly without errors.

---

## 2. User Experience & Visual Design

### Key User Flows

1. **Creating a Video Post**:
   - User opens the Create Post sheet and selects the **Video** tab.
   - Pastes a video URL (e.g., YouTube link, TikTok video link, Instagram reel).
   - App automatically detects the platform, displays a platform badge (e.g., `[YouTube]`, `[TikTok]`), and loads a live embed video player preview.
   - User adds Title, Category, and `#tags`, then taps **Publish**.

2. **Creating a Multi-Photo Post**:
   - User selects the **Photo** tab.
   - Types a main caption.
   - Adds multiple photo URLs using `+ Add Photo` with real-time thumbnail preview cards and quick-remove (`X`) buttons.
   - Taps **Publish** to post the photo carousel.

3. **Creating a Styled Text Post**:
   - User selects the **Text** tab.
   - Enters thoughts or long-form copy.
   - Selects a background style (e.g., Midnight Violet, Sunset Amber, Dark Obsidian, Neon Lime).
   - Live card updates previewing the exact card posture before publishing.

### Visual Identity & Theme
- **Theme**: Premium Material 3 with Qylomi Warm Yellow primary accents (`QYellowPrimary`), obsidian cards (`QSurface`), and high-contrast typography.
- **Embedded Players**: Clean rounded player containers (`16.dp` corner radius) with video overlays, platform branding badges, and responsive aspect ratios (16:9 for YouTube/FB, 9:16 vertical for TikTok/Instagram Reels).
- **Text Post Templates**: 6 pre-built gradients with automatic light/dark text contrast matching.

---

## 3. Key Product Decisions & Trade-Offs

- **Sub-Tables vs. Single Dynamic Payload Column**:
  - *Chosen Approach*: Dedicated `video_posts`, `photo_posts`, and `text_posts` tables linked via foreign key `post_id REFERENCES posts(id) ON DELETE CASCADE`.
  - *Why*: Provides clean schema validation, index performance for media searches, and strict relational integrity without mutating the existing `posts` table structure.

- **Auto-Detection Regex Engine**:
  - *Chosen Approach*: Client-side regex platform resolver in `VideoEmbedUtils.kt` that extracts video IDs (YouTube v= or shorts/, TikTok video IDs, Instagram p/ reel/ IDs) and formats embed URLs.
  - *Why*: Provides instantaneous zero-latency live preview inside the creation dialog before network calls.

---

## 4. Technical Architecture & Data Strategy

### System Architecture & Data Flow

```
┌────────────────────────────────────────────────────────────────────────┐
│                        Jetpack Compose UI Layer                        │
│                                                                        │
│   ┌───────────────────┐    ┌────────────────────┐   ┌──────────────┐   │
│   │ CreatePostSheet   │    │ VideoEmbedPlayer   │   │ PhotoGallery │   │
│   │ (4-Tab Creation)  │    │ (YT/TikTok/FB/IG)  │   │  Carousel    │   │
│   └─────────┬─────────┘    └─────────▲──────────┘   └──────▲───────┘   │
└─────────────┼────────────────────────┼─────────────────────┼───────────┘
              │                        │                     │
              ▼                        │                     │
┌──────────────────────────────────────┴─────────────────────┴───────────┐
│                       SocialViewModel & State                          │
│  - feedPosts: StateFlow<List<Post>>                                    │
│  - createVideoPost(), createPhotoPost(), createTextPost()              │
└──────────────────────────────────────┬─────────────────────────────────┘
                                       │
                                       ▼
┌────────────────────────────────────────────────────────────────────────┐
│                          SocialRepository                              │
│  - Joins main posts table with video_posts / photo_posts / text_posts │
└──────────────────────────────────────┬─────────────────────────────────┘
                                       │
                                       ▼
┌────────────────────────────────────────────────────────────────────────┐
│                       Supabase REST Service                            │
│                                                                        │
│  ┌──────────────┐     ┌─────────────┐   ┌─────────────┐  ┌───────────┐ │
│  │    posts     │◄───►│ video_posts │   │ photo_posts │  │text_posts │ │
│  │ (Main Table) │     │ (sub-table) │   │ (sub-table) │  │(sub-table)│ │
│  └──────────────┘     └─────────────┘   └─────────────┘  └───────────┘ │
└────────────────────────────────────────────────────────────────────────┘
```

### Relational Database Schema Definition

#### 1. `video_posts` Sub-Table
```sql
CREATE TABLE IF NOT EXISTS video_posts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    post_id UUID NOT NULL REFERENCES posts(id) ON DELETE CASCADE,
    title TEXT,
    thumbnail_url TEXT,
    video_url TEXT NOT NULL,
    embed_url TEXT,
    platform TEXT, -- 'youtube', 'tiktok', 'facebook', 'instagram', 'direct'
    category TEXT,
    tags TEXT[],
    created_at TIMESTAMPTZ DEFAULT NOW()
);
```

#### 2. `photo_posts` Sub-Table
```sql
CREATE TABLE IF NOT EXISTS photo_posts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    post_id UUID NOT NULL REFERENCES posts(id) ON DELETE CASCADE,
    caption TEXT,
    photo_urls TEXT[] NOT NULL,
    category TEXT,
    tags TEXT[],
    created_at TIMESTAMPTZ DEFAULT NOW()
);
```

#### 3. `text_posts` Sub-Table
```sql
CREATE TABLE IF NOT EXISTS text_posts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    post_id UUID NOT NULL REFERENCES posts(id) ON DELETE CASCADE,
    caption TEXT NOT NULL,
    bg_color TEXT, -- e.g. '#1A1A2E' or gradient token ID 'sunset_pulse'
    font_style TEXT,
    category TEXT,
    tags TEXT[],
    created_at TIMESTAMPTZ DEFAULT NOW()
);
```

---

## Execution Checklist

1. **Database Service Layer**: Add Moshi data models for `VideoPost`, `PhotoPost`, `TextPost`, and their respective request DTOs.
2. **Video Embed Parser**: Implement `VideoEmbedUtils.kt` supporting YouTube embed links, TikTok video embeds, Facebook video players, and Instagram Reels embeds.
3. **Enhanced Create Post BottomSheet**: Redesign `CreatePostBottomSheet.kt` with a 4-tab selector (**Video**, **Photo**, **Text**, **General**).
4. **Feed Card Renderers**: Update `PostCard.kt` to dynamically check and render inline video players, photo galleries, or styled text cards based on post type.
5. **Verification**: Run `compile_applet` to verify zero build errors and pristine compilation state.
