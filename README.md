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
