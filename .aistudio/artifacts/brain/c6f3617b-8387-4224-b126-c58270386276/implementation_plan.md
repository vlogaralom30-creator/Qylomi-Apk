# Multi-Type Posts, Video URL Extractor, Account Switcher & Qylomi Vector Pixel Art Engine

Comprehensive architectural blueprint and implementation plan to deliver multi-type posts (sub-tables for Video, Photo, and Text), social video extraction and HTML embed playback (TikTok, Instagram, Facebook, YouTube), local account switching with masked passwords, and the Qylomi Vector Pixel Art rendering and stroke-by-stroke replay engine in native Jetpack Compose.

---

## User Review & Confirmed Decisions

> [!IMPORTANT]
> The following key product decisions have been confirmed through Phase 1 user input:

- **Confirmed Decision 1 (Post Sub-Tables)**: Separate sub-tables (`video_posts`, `photo_posts`, `text_posts`) linked to the primary `posts` table via foreign keys to guarantee 100% zero data loss for legacy posts while storing type-specific metadata.
- **Confirmed Decision 2 (Video Extraction & Embedding)**: Video links pasted from TikTok, Instagram, Facebook, and YouTube automatically extract metadata/direct streams or convert into web embed URLs rendered inside an embedded HTML/WebView preview player.
- **Confirmed Decision 3 (Text Post Styling)**: Text-only posts support curated preset background colors and vibrant gradient templates with customizable typography.
- **Confirmed Decision 4 (Account System)**: Local account credentials saved securely in encrypted local preferences for quick 1-tap multi-account switching with masked (`****`) password security and guest exploration mode.
- **Confirmed Decision 5 (Qylomi Pixel Art)**: Native Kotlin pixel art module with vector coordinate matrix system ($N \times N$ grid: 16, 24, 32, 48, 64), stroke-order preservation, real-time replay engine (1x, 2x, 4x speeds), Bresenham line interpolation, and multi-frame animation support.
- **Confirmed Decision 6 (SQL Delivery)**: Clean, executable Supabase PostgreSQL DDL/RLS migration script output in chat for direct execution in Supabase SQL Editor.

---

## 1. Overview & Core Concept

### What It Does
1. **Multi-Type Post Creation**: A multi-tab creation workflow supporting Video posts (with platform detection, thumbnail, categories, tags), Photo posts (multi-image grid/carousel), Text posts (color/gradient templates), and Qylomi Pixel Art posts.
2. **Universal Video Extractor & Embed Player**: Auto-detects video URLs (YouTube, TikTok via TikWM/embeds, Facebook Video plugin, Instagram embeds) and extracts direct video streams or generates embed player HTML for real-time live preview.
3. **Local Multi-Account Manager**: Enables 1-tap switching between multiple saved accounts without re-typing credentials. Shows account emails with masked `****` security and allows guest exploration.
4. **Qylomi Pixel Art & Replay Engine**: Ultra-lightweight vector coordinate renderer (1–5 KB JSON matrix) capable of drawing with zero finger-gap interpolation, saving stroke sequence, replaying creation stroke-by-stroke in real time, and exporting PNGs/animations.

---

## 2. User Experience & Visual Design

### Key User Flows
- **Post Creation Flow**:
  1. Tap FAB (`+`) -> Create Post Dialog opens with 4 tabs: **Video**, **Photo**, **Text**, and **Pixel Art**.
  2. In **Video tab**: User pastes link -> Auto-detects platform (TikTok, IG, FB, YouTube) -> Fetches thumbnail/embed code -> Live preview player renders -> Add title, category, tags.
  3. In **Photo tab**: Multi-photo selector -> Caption, category, keywords.
  4. In **Text tab**: Caption text + Preset color/gradient template palette preview.
  5. In **Pixel Art tab**: Full drawing grid studio -> Drawing tools (1px-4px brush, eraser, color palette, undo/redo, frame animation timeline) -> Replay mode -> Post to feed.
- **Feed Rendering Flow**:
  - Main feed dynamically checks post type and renders either Video Embed Player, Photo Grid/Carousel, Styled Gradient Text Card, or Interactive Qylomi Pixel Art View with a **"Replay Stroke"** button.

### Visual Identity & Material 3 Palette
- **Aesthetic**: Futuristic, sleek social dark/light theme with dynamic Material 3 tokens.
- **Color Tokens**:
  - Primary Accent: `#0EA5E9` (Cyan/Sky)
  - Secondary Accent: `#FF0055` (Neon Pink/TikTok red)
  - Card Surfaces: Modern elevation cards with subtle gradients (`#F5FAFF` light / `#121216` dark).

---

## 3. Key Product Decisions & Trade-Offs

1. **Sub-Tables vs Unified Table Modification**:
   - *Chosen Approach*: Sub-tables `video_posts`, `photo_posts`, and `text_posts` referencing `posts(id)` with ON DELETE CASCADE.
   - *Why*: Prevents breaking schema changes on the primary `posts` table and ensures existing posts remain unaffected.
2. **Video URL Parsing via Embed/TikWM**:
   - *Chosen Approach*: `VideoEmbedUtils.kt` detects YouTube embeds (`youtube.com/embed`), TikTok (`tikwm.com/api` & `tiktok.com/player/v1`), Facebook (`facebook.com/plugins/video.php`), and Instagram (`instagram.com/p/.../embed`).
   - *Why*: Guarantees reliable video playback inside native Android WebViews across all restricted social platforms without encountering expired direct links.

---

## 4. Technical Architecture & System Diagram

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                       ANDROID JETPACK COMPOSE APP                           │
├─────────────────────────────────────────────────────────────────────────────┤
│  UI SCREENS & COMPONENTS                                                    │
│  ├── FeedScreen -> PostCard (Renders Video, Photo, Text, or Pixel Art)      │
│  ├── CreatePostDialog (Video, Photo, Text, Pixel Art Tabs)                  │
│  ├── EmbedVideoPlayer (WebView with TikTok/IG/FB/YouTube player HTML)       │
│  ├── PixelArtCanvas & PixelArtReplayEngine (Qylomi Coordinate Renderer)     │
│  └── AccountManagerDialog (Multi-Account Switcher with masked passwords)   │
├─────────────────────────────────────────────────────────────────────────────┤
│  DATA LAYER & REPOSITORIES                                                  │
│  ├── SocialRepository (Handles posts + video_posts/photo_posts/text_posts)   │
│  ├── LocalAccountRepository (Encrypted SharedPreferences / DataStore)       │
│  ├── VideoEmbedUtils (URL Regex Parser & Embed Generator)                   │
│  └── SupabaseService & Ktor API Client                                      │
└──────────────────────────────────────┬──────────────────────────────────────┘
                                       │
                                       ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                          SUPABASE POSTGRESQL DATABASE                       │
├─────────────────────────────────────────────────────────────────────────────┤
│  TABLES:                                                                    │
│  ├── posts (id, user_id, type, caption, pixel_art, created_at)              │
│  ├── video_posts (id, post_id, title, video_url, embed_url, platform, ...)  │
│  ├── photo_posts (id, post_id, photo_urls, category, tags)                  │
│  └── text_posts (id, post_id, bg_type, bg_color_hex, gradient_json)         │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 5. SQL Migration Script Preview

The following SQL script will be output in the response for direct execution in Supabase SQL Editor:

```sql
-- 1. Video Posts Sub-Table
CREATE TABLE IF NOT EXISTS public.video_posts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    post_id UUID NOT NULL REFERENCES public.posts(id) ON DELETE CASCADE,
    title TEXT NOT NULL,
    thumbnail_url TEXT,
    video_url TEXT NOT NULL,
    embed_url TEXT NOT NULL,
    platform TEXT NOT NULL, -- youtube, tiktok, facebook, instagram, direct
    category TEXT,
    tags TEXT[],
    keywords TEXT[],
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- 2. Photo Posts Sub-Table
CREATE TABLE IF NOT EXISTS public.photo_posts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    post_id UUID NOT NULL REFERENCES public.posts(id) ON DELETE CASCADE,
    photo_urls TEXT[] NOT NULL,
    category TEXT,
    tags TEXT[],
    keywords TEXT[],
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- 3. Text Posts Sub-Table
CREATE TABLE IF NOT EXISTS public.text_posts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    post_id UUID NOT NULL REFERENCES public.posts(id) ON DELETE CASCADE,
    bg_type TEXT NOT NULL DEFAULT 'solid', -- solid, gradient
    bg_color_hex TEXT DEFAULT '#0EA5E9',
    gradient_colors TEXT[] DEFAULT ARRAY['#0EA5E9', '#3B82F6'],
    font_style TEXT DEFAULT 'default',
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- Row Level Security (RLS) Policies
ALTER TABLE public.video_posts ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.photo_posts ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.text_posts ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Public Read Video Posts" ON public.video_posts FOR SELECT USING (true);
CREATE POLICY "Auth Insert Video Posts" ON public.video_posts FOR INSERT WITH CHECK (auth.uid() IS NOT NULL);

CREATE POLICY "Public Read Photo Posts" ON public.photo_posts FOR SELECT USING (true);
CREATE POLICY "Auth Insert Photo Posts" ON public.photo_posts FOR INSERT WITH CHECK (auth.uid() IS NOT NULL);

CREATE POLICY "Public Read Text Posts" ON public.text_posts FOR SELECT USING (true);
CREATE POLICY "Auth Insert Text Posts" ON public.text_posts FOR INSERT WITH CHECK (auth.uid() IS NOT NULL);
```

---

## 6. Implementation Checklist & Verification Strategy

- [ ] **SQL Script Output**: Provide full copy-pasteable SQL migration code in response.
- [ ] **Data Layer**:
  - `VideoPost.kt`, `PhotoPost.kt`, `TextPost.kt`, `PixelArtData.kt` models.
  - `VideoEmbedUtils.kt` with TikTok, Instagram, Facebook, and YouTube URL parsers.
  - `LocalAccountRepository.kt` for saved account credentials and 1-tap switching with masked passwords.
  - `SupabaseApi.kt` & `SocialRepository.kt` sub-table API calls.
- [ ] **UI Layer**:
  - `EmbedVideoPlayer.kt` AndroidView WebView component.
  - `PhotoPostGallery.kt` and `StyledTextPostCard.kt`.
  - `PixelArtCanvas.kt` & `PixelArtStudioDialog.kt` drawing and replay engine.
  - `CreatePostDialog.kt` updated with 4 post tabs.
  - `AccountManagerDialog.kt` for account switching and logout.
- [ ] **Verification**: Execute `compile_applet` to ensure zero compilation errors.
