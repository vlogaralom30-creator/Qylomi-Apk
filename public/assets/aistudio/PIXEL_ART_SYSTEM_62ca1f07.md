# Qylomi Hand-Drawn Pixel Art & Replay System
**Technical Architecture, Database Schema, Mathematics, and Workflow Documentation**

---

## 1. Executive Summary & Core Philosophy

Instead of relying on heavy image file uploads (PNG, WebP, or JPEG) which require external blob storage buckets, high bandwidth, and offer only static visual representations, **Qylomi** features a proprietary **Vector Coordinate Number Matrix System**.

### Key Advantages:
1. **Ultra-Lightweight Storage**: A complex drawing is stored as an array of integer index numbers (e.g. `[102, 103, 104, 150, 151, ...]`), consuming only **1 to 5 KB** of data.
2. **Instant Live Replay Animation**: Because stroke order is preserved in the array, any viewer can hit **"Play Replay"** to watch the artwork being drawn line-by-line in real time at 1x, 2x, or 4x speed.
3. **Infinite Crisp Resolution**: Renders dynamically via HTML5 Canvas on client devices with zero compression artifacts.
4. **Cross-Platform Compatibility**: Fully touch-optimized for mobile phones (Android, iOS) with a dedicated Full-Screen Mode, as well as desktop mice.

---

## 2. Mathematical Logic & Coordinate Mapping

### A. Coordinate to 1D Index Conversion
A square canvas grid with dimension $N \times N$ (where $N \in \{32, 48, 64, 96, 128\}$) is mapped as follows:

$$\text{Index} = y \times N + x$$

$$\begin{aligned}
x &= \text{Index} \pmod N \\
y &= \lfloor \text{Index} / N \rfloor
\end{aligned}$$

Where:
- $x, y \in [0, N-1]$
- $\text{Index} \in [0, N^2 - 1]$

### B. Bresenham's Line Interpolation (Preventing Gaps)
When a user moves their finger rapidly across a touch screen, native touch event intervals (typically 60Hz or 120Hz) might skip intermediate grid cells.
To guarantee continuous smooth strokes, **Bresenham's integer line algorithm** (`getLinePixelIndices`) calculates all cells along the ray from $(x_1, y_1)$ to $(x_2, y_2)$ before committing to the pixel array:

```typescript
export function getLinePixelIndices(
  x0: number, y0: number,
  x1: number, y1: number,
  gridSize: number
): number[]
```

### C. Multi-Size Brush Footprint (Finger-Friendly)
Human fingertips are typically 30–50px wide. On a $64\times64$ or $128\times128$ grid on a mobile device, a single cell is only 2 to 5px. To make drawing comfortable and natural:
- **1px (Fine)**: Draws exact target $(x, y)$.
- **2px (Finger - Default)**: Draws a $2\times2$ cluster $(x, y), (x+1, y), (x, y+1), (x+1, y+1)$.
- **1px (Fine)**: Draws exact target $(x, y)$ for delicate outlines, text, and fine details.
- **2px (Normal - Default)**: Draws a $2\times2$ cluster $(x, y), (x+1, y), (x, y+1), (x+1, y+1)$ for standard touch drawing.
- **3px (Thick)**: Draws a centered $3\times3$ footprint for bold accents.
- **4px (Bold)**: Draws a $4\times4$ block for rapid filling and heavy borders.

### D. Interactive Brush Size Control & Hotkeys:
The studio toolbar includes dedicated step buttons (`-` and `+`) as well as visual dot indicators matching the actual thickness of each mode.
- **Applies to Both Tools**: Toggling brush size controls both the **Pencil** (draw thickness) and the **Eraser** (erasing radius).
- **Keyboard Shortcuts**:
  - `[` or `-`: Decrease brush thickness
  - `]` or `+`: Increase brush thickness
  - `B` / `P`: Pencil tool
  - `E`: Eraser tool
  - `Ctrl + Z`: Undo stroke
  - `Ctrl + Y` or `Ctrl + Shift + Z`: Redo stroke

---

## 3. High-DPI Mobile Display Normalization

### The Double-Scaling Trap & Solution:
Modern smartphone displays feature high Device Pixel Ratios (**DPR**), such as 2.75x or 3.0x on Android/Retina screens.
- **Problem**: Multiplying `canvas.width = cssDim * DPR` and simultaneously invoking `ctx.scale(DPR, DPR)` causes pixel coordinates to be scaled by $\text{DPR}^2$ (e.g. $2.75 \times 2.75 = 7.56\times$), pushing artwork far outside the canvas boundary and turning it blank.
- **Solution**:
  1. Set CSS display width: `canvas.style.width = cssDim + 'px'`.
  2. Set internal buffer: `canvas.width = Math.round(cssDim * DPR)`.
  3. Calculate buffer cell size directly: `cellSize = canvas.width / gridSize`.
  4. Fill pixels in raw buffer space: `ctx.fillRect(Math.floor(x * cellSize), Math.floor(y * cellSize), Math.ceil(cellSize) + 0.5, Math.ceil(cellSize) + 0.5)`.
  5. Touch events map client coordinates to grid cells via:
     $$\text{cellX} = \lfloor ((clientX - \text{rect.left}) / \text{rect.width}) \times N \rfloor$$
     $$\text{cellY} = \lfloor ((clientY - \text{rect.top}) / \text{rect.height}) \times N \rfloor$$

### Mobile Touch Event Optimization:
To prevent Android Chrome and iOS Safari from interpreting finger strokes as page scrolls or pull-to-refresh gestures, direct touch listeners are attached to the canvas with `{ passive: false }` calling `e.preventDefault()`.

---

## 4. Database Schema & Dual Persistence Strategy

The system uses a **Dual-Layer Architecture** to ensure 100% backward compatibility and zero data loss, even if the database has not run latest migrations:

### Layer 1: Dedicated PostgreSQL / Supabase Column (Fastest)
```sql
-- Migration for Supabase SQL Editor:
ALTER TABLE public.posts 
ADD COLUMN IF NOT EXISTS pixel_art JSONB DEFAULT NULL;

-- Fast index for posts containing pixel art
CREATE INDEX IF NOT EXISTS idx_posts_pixel_art 
ON public.posts USING gin (pixel_art);
```

Stored JSON Structure:
```json
{
  "gridSize": 48,
  "pixels": [102, 103, 104, 150, 151, 152, 198, 246],
  "createdAt": "2026-10-01T11:00:00.000Z"
}
```

### Layer 2: Self-Contained HTML Trailer (Universal Fallback)
When a post is saved, `injectPixelArt` appends a lightweight HTML metadata comment to the end of `posts.content`:
```html
<!-- qylomi:pixelart:v1 {"gridSize":48,"pixels":[...]} -->
```
When rendering a post:
1. `post.pixel_art` is checked first.
2. If null, `extractPixelArt(post.content)` parses the trailer and restores the art seamlessly.
3. The trailer is stripped from the readable text content so users only see clean post text.

---

## 5. Component & File Reference

| File | Purpose |
|---|---|
| `/src/utils/pixelArt.ts` | Mathematical coordinate transforms, line algorithms, HTML trailer injection/extraction. |
| `/src/components/PixelArtCanvas.tsx` | Interactive Drawing Studio with Fullscreen Mode, Brush Sizing, Undo/Redo, and Live Preview. |
| `/src/components/PixelArtViewer.tsx` | Minimalist feed player widget with instant play replay, speed control (1x/2x/4x), and PNG export. |
| `/src/services/posts.ts` | Handles `pixel_art` payload insertion, retry fallbacks, and content trailer encoding. |
| `/src/components/IdeaCard.tsx` | Renders `PixelArtViewer` inside community and user profile feeds. |
| `/src/pages/CreateIdea.tsx` | Entry point for attaching pixel art during post creation. |
| `/src/pages/EditIdea.tsx` | Allows editing and re-drawing existing pixel art on posts. |
| `/src/components/GroupPostComposer.tsx` | Group-level composer integration for pixel art. |

---

## 6. Fullscreen Studio Mode Guide

When creating artwork on a mobile phone, the inline container can feel constrained by page elements.
- **Activation**: Clicking the **`Full Screen`** button switches `isFullscreen` state to `true`.
- **Behavior**:
  - Sets `fixed inset-0 z-50 bg-slate-950` overlay over the whole viewport.
  - Locks document body scrolling (`document.body.style.overflow = 'hidden'`).
  - Computes maximum available square canvas: `Math.min(window.innerWidth - 32, window.innerHeight - 175, 600)`.
  - Provides instant access to Undo, Redo, Clear, Tool switch (Pencil/Eraser), Brush thickness, and Grid Size.
  - Clicking **"Attach Art"** saves the art and closes the modal back into the post composer.

---

## 7. Guidelines for Future AI Agents & Developers

1. **Do NOT Replace with Blob Images**: Do not convert user drawings into Base64 PNGs or Supabase storage uploads for the primary post payload. Vector numbers enable the core "Play Replay" animation.
2. **Preserve Array Ordering**: The order of numbers in `pixels: number[]` dictates stroke creation sequence. Do not sort the array numerically (`pixels.sort()`), or the replay animation will draw mechanically top-to-bottom instead of mimicking the artist's hand strokes.
3. **Maintain Dual Persistence**: Always preserve both `insertPayload.pixel_art` AND `injectPixelArt(content, pixelArt)`. This guarantees cross-compatibility across all environments.

---

## 8. Frame-by-Frame Vector Animation Engine & AI Code Schema

The engine includes a multi-frame animation system where animations are stored purely as frame arrays of 2D grid coordinates.

### A. Animation Data Schema
```typescript
export interface PixelArtData {
  gridSize: number;       // e.g. 32, 48, 64
  pixels: number[];       // Fallback thumbnail (Frame 0 pixels)
  isAnimation?: boolean;  // True when multi-frame
  fps?: number;           // Target playback speed (1 to 24 FPS, default: 5)
  frames?: number[][];    // Array of pixel coordinate arrays, one per frame
  createdAt?: string;
}
```

### B. AI Prompt Workflow (ChatGPT, DeepSeek, Claude, Kimi)
Users can copy a system prompt generated via `generateAIPrompt(subject, gridSize, fps, frameCount)`:
```
You are a pixel art animator for Qylomi's 2D vector coordinate animation engine.
TASK: Create a 6-frame looping pixel art animation of: "<Subject>".
GRID SPECIFICATIONS:
- Dimension: 32x32 grid (Total 1024 cells).
- Coordinate formula: index = y * 32 + x.
- Valid index range: 0 to 1023.
- Frame Rate: 5 FPS.
- Total Frames: 6.
Return ONLY valid JSON:
{
  "isAnimation": true,
  "gridSize": 32,
  "fps": 5,
  "frames": [
    [...frame 1 indices...],
    [...frame 2 indices...],
    ...
  ]
}
```

### C. Parser Flexibility (`parseAnimationCode`)
The parser accepts:
1. Pure JSON objects with `frames: number[][]`.
2. Markdown-wrapped JSON (````json ... ````).
3. Raw 2D arrays: `[[1, 2, 3], [4, 5, 6]]`.
4. Line-by-line frame notations: `Frame 1: 12, 13, 14`.

### D. Live Feed Player
In the feed, `PixelArtViewer` detects `data.isAnimation` and automatically plays a continuous 60fps loop synchronized via `requestAnimationFrame` and target interval `1000 / fps`. Viewers can pause, step forward/backward frame-by-frame, change playback speed (0.5x, 1x, 2x), seek anywhere on the interactive timeline, toggle synchronized subtitles (CC), and download individual frames as PNG.

### E. Long-Form Animations, Ultra 128x128 & Cartoon Subtitles Engine
- **Ultra 128x128 Matrix**: Up to 16,384 addressable pixels for high-detail cartoon characters, faces, facial expressions, and complex background scenes.
- **Long-Form Animations (1 to 5+ Minutes)**: Stored as sequential integer coordinate arrays (or delta diffs) with zero video streaming overhead (~30-80 KB total).
- **Synchronized Dialogue Subtitles (`subtitles: AnimationSubtitle[]`)**: Each frame or millisecond timestamp can carry speaker dialogue (e.g. `[Commander]: 3... 2... 1... Liftoff!`) displayed live in the animated viewer.
- **Database Schema**: Dedicated relational tables and functions defined in `animation_schema.sql` (`public.animations`, `public.animation_frames`, `public.animation_subtitles`, and `get_full_animation_package()`).

---

## 9. Normal Mode vs Pro Studio Mode & Full Color Matrix Engine

### A. Dual Modes Architecture
1. **Normal Mode (Inline in Post Composer)**:
   - Focused, clean, lightweight drawing experience embedded directly inside post creation (`CreateIdea`, `EditIdea`, `GroupPostComposer`).
   - Pure black & white strokes on clean canvas (`#0f172a` ink on `#ffffff` canvas).
   - Core tools: Pen, Eraser, 1px/2px/3px brush size, Undo, Redo, Clear, Grid resolution (16×16 to 64×64).
   - Instant step-by-step stroke replay preview.
   - High-visibility trigger button: `[ ⚡ Open Pro Studio ]` with sparkles icon. Clicking it seamlessly preserves current strokes and expands to full-screen Pro Studio!

2. **Pro Studio Mode (Full-Screen & Dedicated Page at `#/pixel-studio`)**:
   - Canvas display expands up to **600×600 px** with crisp pixelated rendering and high Device Pixel Ratio (DPR) sharpness.
   - Ultra-fine resolutions: 16×16, 24×24, 32×32, 48×48, 64×64, 96×96, and 128×128 (Ultra HD, 16,384 addressable pixels).
   - With 1px brush size, pixels are delightfully small and precise for intricate sprite art, faces, and character details.

### B. Color Matrix Architecture
Instead of storing heavy raster blobs, colors are mapped to pixel coordinates via integer coordinate indexes:
```typescript
export interface PixelArtData {
  gridSize: number;                   // 16, 24, 32, 48, 64, 96, 128
  pixels: number[];                   // Ordered coordinate indexes
  colors?: Record<number, string>;    // Mapping: coordinate index -> hex color (#RRGGBB)
  frameColors?: Record<number, string>[]; // Frame-by-frame color maps for animations
  palette?: string[];                 // Active palette array
  mode?: 'normal' | 'pro';
  isAnimation?: boolean;
  fps?: number;
  frames?: number[][];
  subtitles?: AnimationSubtitle[];
}
```
- **100% Backward Compatible**: Older posts or B&W drawings without `colors` default to `#0f172a`.
- **Live Replay with Color**: In the feed and studio player, each stroke replays in the exact painted color.

### C. Curated Color Palettes & Advanced Pro Tools
- **Palettes Built-In**:
  - `PICO-8 (16)`: Iconic retro demoscene colors.
  - `Cyber Neon (8)`: Glowing high-contrast neon tones.
  - `Pastel Dream (10)`: Soft aesthetic pastel colors.
  - `GameBoy (4)`: Classic 4-shade greenish retro palette.
  - `Monochrome (6)`: Grayscale shading.
  - `Classic 16`: Primary and secondary tones.
  - **Custom Hex Picker**: Real-time `<input type="color">` and Hex code input.
  - **Recent Colors Strip**: Dynamic 1-click access to colors currently used on the canvas.
- **Tools**:
  - **Pen**: 1px (fine pixel), 2px, 3px, 4px.
  - **Eraser**: With matching sizes.
  - **Paint Bucket (Flood Fill)**: `floodFillIndices` algorithm fills connected regions of identical color.
  - **Straight Line Tool**: Point-to-point pixel line rasterizer.
  - **Eyedropper**: Sample color directly from any cell on the canvas.

### D. File Upload & Image-to-Pixel-Art Converter
- Accepts any user PNG, JPG, or WebP file.
- Client-side offscreen canvas downsamples and maps pixel data to the target grid resolution.
- Quantizes RGB channels to nearest palette colors or hex codes.
- Artist can immediately continue drawing and animating on top of the converted sprite!

### E. Dedicated Studio Page (`/pixel-studio` / `#/art-studio`)
- Full creative workspace with "Publish as Post", "Save Artwork", and "Download 780x780 PNG".
- Smooth transfer pipeline: Saves to session storage and loads straight into post composer!

---

## 10. Multi-Modal Pixel Display Engine (100% Free • Client-Side • No Limit)

### A. Media to Pixel (GIF, Video & Image Conversion)
1. **Animated GIF to Pixels**:
   - Uses browser `ImageDecoder` API or client-side canvas sampling.
   - Extracts all GIF frames, downsamples to target grid (16×16 up to 128×128), and maps to active palette colors.
   - Outputs directly into the animation timeline as editable pixel frames.
2. **Short Video to Pixels (MP4 / WebM)**:
   - Client-side HTML5 `<video>` extraction.
   - Configurable sample FPS (4, 6, 8, 12) and duration limit (2s to 8s).
   - Offscreen canvas seeks and downscales frames into integer coordinate arrays with live progress indicator.
   - Zero server encoding or GPU billing; completely free!

### B. Text, Marquee Ticker & Technical Diagrams
1. **8-Bit Pixel Typography**:
   - Converts any user text into retro bitmap typography (3x5 font and 7x7 matrix).
   - Configurable alignment (center, top, bottom), font scale (1x, 2x), and color.
2. **Marquee Ticker (Scrolling LED Display)**:
   - Automatically generates multi-frame scrolling LED sign animations from any sentence.
   - Text smoothly shifts across frames from right to left at chosen speed (6, 8, 12 FPS).
3. **Workflow & Architecture Diagrams**:
   - Templates for **Process Pipeline** (`[INPUT] → [AGENT] → [OUTPUT]`), **Architecture Stack** (`[CLIENT] ↓ [SERVER] ↓ [DATABASE]`), and **Decision Trees**.
   - Draws geometric boxes, directional arrows, and labels directly into pixel cells.

### C. Audio Visualizer & Free Voice-to-Text Subtitles
1. **Web Audio API Frequency Reactive Display**:
   - `AudioContext` + `AnalyserNode` connected to audio files (MP3/WAV/OGG) or microphone.
   - Modes:
     - **Equalizer Bars**: Frequency spectrum columns dancing to bass, mid, treble.
     - **Oscilloscope Wave**: Continuous sine wave audio oscillations.
     - **Beat Pulse**: Expanding radial neon pulse matching audio beats.
   - **Bake to Animation**: Automatically records 36 real-time audio visualizer frames into permanent editable animation frames.
2. **100% Free Voice-to-Text Subtitles (Web Speech API)**:
   - Uses native `webkitSpeechRecognition` in Chrome, Edge, Safari, and Android.
   - Transcribes real-time voice speech into dialogue subtitles synchronized with animation frames.
   - Zero external third-party API dependencies or cost!
3. **Synchronized Feed Player**:
   - `PixelArtViewer` detects `data.audioUrl` and provides audio playback with synchronized frame loop and Mute/Unmute toggle.



