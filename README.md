<div align="center">

<img src="banner.png" alt="AnimeX Banner" width="100%" style="border-radius: 12px; margin-bottom: 12px;" />

<img src="app_logo.png" alt="AnimeX App Icon" width="110" height="110" style="border-radius: 22px;" />

# AnimeX

**Futuristic Native Android Anime Streaming Client**

[![Release](https://img.shields.io/github/v/release/IIT2011/AnimeX?style=for-the-badge&color=00F0FF&label=Release)](https://github.com/IIT2011/AnimeX/releases/latest)
[![Download APK](https://img.shields.io/badge/Download-AnimeX--v1.1.apk-00F0FF?style=for-the-badge&logo=android)](https://github.com/IIT2011/AnimeX/releases/latest)
[![Platform](https://img.shields.io/badge/Platform-Android-FF007F?style=for-the-badge&logo=android)](https://github.com/IIT2011/AnimeX)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0.21-7F52FF?style=for-the-badge&logo=kotlin)](https://kotlinlang.org)

</div>

---

AnimeX is a high-performance native Android anime streaming client built with **Kotlin** and **Jetpack Compose**, featuring a **Cyber-HUD / Neo-Tokyo Minimalist** design system, batch & individual HLS anime downloader, interactive comments, on-device personalized recommendation engine, real Google Cloud sync, and native offline playback.

---

## What's New in v1.1 ⚡

1. **📥 Batch & Individual Anime Downloader:**
   - **External Downloads:** Saves directly to Android storage at `Downloads/AnimeX/<Anime Name>/<Anime Name S1 EP#>.mp4`.
   - **In-App Downloads:** Saves inside AnimeX for full offline viewing directly from your profile.
   - **Stream Configurator:** Customizable Video Quality (1080p, 720p, 360p), Audio (Sub/Dub), and Subtitles before starting.
   - **Batch Episode Selector:** Download entire seasons or select individual episodes with 1 tap.

2. **💬 Interactive Anime Transmissions (Comments):**
   - Live community commenting on any anime title with timestamping, custom avatars, and like counter.
   - Comments persist locally and survive application restarts.

3. **👤 Profile Alias Customizer & Real-time Availability Check:**
   - Customize your user handle directly from your Profile card.
   - Real-time availability verification checks for reserved or taken usernames before claiming.

4. **📂 Fullscreen Expandable Watchlists & Downloads Manager:**
   - **"Watchlists":** 1-tap expansion from profile covering the full screen with comprehensive 2-column grid and quick play.
   - **"Downloads":** Complete storage manager showing anime groupings, episode counts, disk space used, and 1-tap offline ExoPlayer playback.

---

## Architecture Overview

```
animex/
├── app/
│   ├── src/main/
│   │   ├── AndroidManifest.xml
│   │   └── java/com/animex/app/
│   │       ├── MainActivity.kt               # Edge-to-edge Compose host
│   │       ├── AnimeXApp.kt                  # Application base & repository bootstrapper
│   │       ├── core/
│   │       │   ├── designsystem/             # Cyber-HUD Design System (Void Dark, Neon Cyan & Magenta)
│   │       │   ├── network/                  # Network layer with mirror failover & Referer injector
│   │       │   ├── auth/                     # Google account integration & hidden cloud sync
│   │       │   ├── session/                  # User session store for watchlists, history & username
│   │       │   ├── download/                 # HLS stream segment downloader & task manager
│   │       │   ├── comment/                  # Community transmissions & comments repository
│   │       │   └── recommendation/           # On-device interest-based recommendation engine
│   │       ├── feature/
│   │       │   ├── home/                     # Rolling hero recommendations & personalized rails
│   │       │   ├── genre/                    # Interactive genre categories explorer
│   │       │   ├── schedule/                 # Broadcast schedule timeline with time picker
│   │       │   ├── detail/                   # Episode lists, metadata HUD, batch download & comments
│   │       │   ├── player/                   # Media3 ExoPlayer with online HLS & offline playback
│   │       │   ├── search/                   # Real-time search with filter drawer
│   │       │   └── account/                  # 1-tap Google sync, alias editor, expandable watchlists & downloads
│   │       └── navigation/                   # Compose Navigation graph
│   └── build.gradle.kts
├── gradle/
│   └── libs.versions.toml                    # Version catalog (Compose, Media3, Coil, Kotlinx)
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
3. Connect an Android device (USB debugging enabled) or launch an Android Emulator (API 26+).
4. Click **Run ▶** (`Shift + F10`).
