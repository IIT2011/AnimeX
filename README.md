<div align="center">

<img src="banner.png" alt="AnimeX Banner" width="100%" style="border-radius: 12px; margin-bottom: 12px;" />

<img src="app_logo.png" alt="AnimeX App Icon" width="110" height="110" style="border-radius: 22px;" />

# AnimeX

**Futuristic Native Android & Android TV Anime Streaming Client**

[![Release](https://img.shields.io/github/v/release/IIT2011/AnimeX?style=for-the-badge&color=00F0FF&label=Release)](https://github.com/IIT2011/AnimeX/releases/latest)
[![Download Mobile APK](https://img.shields.io/badge/Mobile%20APK-AnimeX--V1.2.apk-00F0FF?style=for-the-badge&logo=android)](https://github.com/IIT2011/AnimeX/releases/download/AnimeX-V1.2/AnimeX-V1.2.apk)
[![Download TV APK](https://img.shields.io/badge/Android%20TV%20APK-AnimeX--tv--v1.1.apk-FF0055?style=for-the-badge&logo=androidtv)](https://github.com/IIT2011/AnimeX/releases/download/AnimeX-V1.2/AnimeX-tv-v1.1.apk)
[![Platform](https://img.shields.io/badge/Platform-Android%20%7C%20Android%20TV-FF007F?style=for-the-badge&logo=android)](https://github.com/IIT2011/AnimeX)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0.21-7F52FF?style=for-the-badge&logo=kotlin)](https://kotlinlang.org)

</div>

---

AnimeX is a high-performance native Android & Android TV anime streaming client built with **Kotlin** and **Jetpack Compose**, featuring a **Cyber-HUD / Neo-Tokyo Minimalist** design system, a dedicated 10-foot D-pad Leanback experience for Android TVs, high-speed multi-threaded batch anime downloader with background service, interactive comments, on-device personalized recommendation engine, real Google Cloud sync, companion subtitles for external players (VLC), and native offline playback.

---

## 📺 Dedicated Android TV Experience (`:tv`)

AnimeX now features a standalone, optimized **Android TV & Google TV** module (`tv/`) crafted specifically for 10-foot viewing and remote control (D-pad) navigation:

- **10-Foot Cyber-HUD UI:** Immersive 1080p/4K landscape UI with high-contrast neon focus states (1.08x scale-up, glowing cyan/magenta border transitions).
- **Dynamic Hero Billboard:** Top billboard dynamically updates backdrops, ratings, synopsis, and metadata as you navigate across shelves below.
- **Horizontal Content Shelves:** "Trending On AnimeX", "Recommended For You", "Latest Broadcasts", "Top Rated Airing", and "All-Time Popular".
- **Leanback TV Video Player:** Full D-pad remote mapping:
  - `OK / Center`: Play / Pause toggle with HUD auto-hide after 4 seconds.
  - `◀ / ▶`: Instant 10-second seek backward / forward with on-screen HUD badge.
  - `▲ Up`: In-player quick horizontal Episode Drawer (switch episodes without leaving playback).
  - `▼ Down`: Stream resolution (1080p / 720p / 480p) & audio selector.
- **On-Screen D-Pad Keypad & Search:** Grid keyboard designed for TV remote navigation with instant live search suggestions.
- **Android TV Launcher Integration:** Dedicated `LEANBACK_LAUNCHER` intent and 16:9 high-res TV banner (`tv_banner.png`).
- **TV Auto-Updater:** Automated startup check with D-pad focusable "UPDATE NOW" dialog.

---

## What's New in v1.2 ⚡

1. **⚡ High-Speed Multi-Threaded HLS Segment Downloader:**
   - Concurrent sliding-window chunk downloading (saturates 180+ Mbps / ~22.5 MB/s fiber connections).
   - High-throughput OkHttp dispatcher with connection pooling to eliminate TCP latency.

2. **🛡️ Lock Screen & Background Download Resilience:**
   - Android Foreground Service (`DownloadService`) with real-time notification progress bar.
   - CPU `PARTIAL_WAKE_LOCK` prevents downloads from pausing, timing out, or erroring when the phone is locked or sent to the background.

3. **🎬 VLC & External Player Subtitles Engine:**
   - Companion SubRip (`.srt`) and WebVTT (`.vtt`) files generated beside videos in public `Download/AnimeX/<Anime Name>/`.
   - Automatic registration with Android `MediaScannerConnection` and `MediaStore.Downloads` so VLC on Android immediately detects and displays subtitles.

4. **🚀 Universal Auto-Updater (Mobile & TV):**
   - Startup release verification comparing installed APK versions against the public GitHub repository.
   - Clean Cyber-HUD "UPDATE NOW" dialog with direct APK downloading via `DownloadManager`.
   - Manual "Check for Updates" trigger in Account & Settings.

---

## Architecture Overview

```
animex/
├── app/                              # Mobile Android Application (Phone & Tablet)
│   ├── src/main/
│   │   ├── AndroidManifest.xml
│   │   └── java/com/animex/app/
│   │       ├── MainActivity.kt       # Edge-to-edge Compose host
│   │       ├── AnimeXApp.kt          # Application base & repository bootstrapper
│   │       ├── core/                 # Design system, network, download, comment & sync
│   │       ├── feature/              # Home, genre, schedule, detail, player, search & account
│   │       └── navigation/           # Compose Navigation graph
│   └── build.gradle.kts
├── tv/                               # Android TV / Google TV 10-Foot Application
│   ├── src/main/
│   │   ├── AndroidManifest.xml       # Leanback launcher & TV features
│   │   ├── java/com/animex/tv/
│   │   │   ├── TVMainActivity.kt     # D-pad remote navigation & leanback host
│   │   │   ├── AnimeXTVApp.kt        # TV application base
│   │   │   ├── theme/TVTheme.kt      # Cyber TV OLED theme & remote focus modifiers
│   │   │   ├── components/           # TVTopNav, TVCard, TVHeroBillboard, TVEpisodeCard
│   │   │   ├── ui/home/              # Dynamic billboard & horizontal content shelves
│   │   │   ├── ui/detail/            # 16:9 split detail view & episode carousel
│   │   │   ├── ui/player/            # 10-foot remote ExoPlayer (Seek, Up/Down drawers)
│   │   │   ├── ui/search/            # D-pad virtual keyboard & live search grid
│   │   │   ├── ui/genre/             # TV genre pills & archive browser
│   │   │   ├── ui/schedule/          # Broadcast schedule tracker
│   │   │   └── ui/watchlist/         # Saved watchlist grid
│   │   └── res/drawable/tv_banner.png# 16:9 Leanback banner for Android TV home
│   └── build.gradle.kts
├── gradle/
│   └── libs.versions.toml            # Version catalog (Compose, Media3, Coil, Kotlinx)
├── build.gradle.kts
└── settings.gradle.kts
```

---

## Key Features

1. **Cyber-HUD Design System:**
   - **Void Black (`#07080D`) & Obsidian Surfaces:** True OLED blacks optimized for AMOLED displays.
   - **Neon Accents:** Electric Cyan (`#00F0FF`), Neon Magenta (`#FF007F`), and Amber.
   - **Cyber Modifiers:** Animated glowing scanlines, pulse borders, and tactile click micro-animations.

2. **Native HLS & Offline Video Engine:**
   - Powered by **Jetpack Media3 (ExoPlayer)**.
   - Supports online HLS streams with anti-leech referer injection and offline playback for in-app downloads.
   - YouTube-style double-tap seeking ($\pm 10$s, $\pm 20$s, $\pm 30$s looping with hidden controls during seeking).
   - Sensor-based landscape orientation locking, resolution switcher (1080p / 720p / 360p), multi-language subtitle tracks, and audio switcher.

3. **On-Device Personalized Recommendations:**
   - Learns dynamically from genres watched, search queries, and watch completion percentages.
   - Produces fine-grained affinity match scores (`95% MATCH`, `88% MATCH`).
   - Persists intelligence locally in internal sandbox storage (`.animex_user_intel.txt`).

4. **Google Cloud AppData Sync:**
   - Seamless 1-tap device account connection, native Android account picker, and direct email binding.
   - Automatically synchronizes watchlist entries, episode playback progress, and user preferences into your private Google space (`.animex_google_sync.txt`).

5. **Airing Timings & Schedule Tracker:**
   - Day-by-day broadcast timeline showing episode drop times (`18:30`, `00:30`, etc.).

---

## How to Build & Run

1. Clone the repository:
   ```bash
   git clone https://github.com/IIT2011/AnimeX.git
   ```
2. Open the project folder in **Android Studio** (Koala, Ladybug, or newer).
3. Connect an Android phone, tablet, or Android TV device / emulator (API 26+).
4. Select the target run configuration:
   - **`app`**: Mobile & tablet Android application
   - **`tv`**: 10-foot Android TV & Google TV application
5. Click **Run ▶** (`Shift + F10`) or assemble via CLI:
   ```bash
   ./gradlew :app:assembleRelease   # Generates mobile APK
   ./gradlew :tv:assembleRelease    # Generates Android TV APK
   ```
