# AnimeX - Futuristic Native Android Anime Streaming Client

AnimeX is a native Android application built with **Kotlin** and **Jetpack Compose**, featuring a **Cyber-HUD / Neo-Tokyo Minimalist** design system, on-device personalized recommendation engine, real Google Cloud sync, and high-performance HLS playback engine.

---

## Architecture Overview

```
animex/
├── app/
│   ├── src/main/
│   │   ├── AndroidManifest.xml
│   │   └── java/com/animex/app/
│   │       ├── MainActivity.kt               # Edge-to-edge Compose host
│   │       ├── AnimeXApp.kt                  # Application base
│   │       ├── core/
│   │       │   ├── designsystem/             # Cyber-HUD Design System (Void Dark, Neon Cyan & Magenta)
│   │       │   ├── network/                  # Network layer with mirror failover & Referer injector
│   │       │   ├── auth/                     # Google account integration & hidden cloud sync
│   │       │   ├── session/                  # User session store for watchlists and playback progress
│   │       │   └── recommendation/           # On-device interest-based recommendation engine
│   │       ├── feature/
│   │       │   ├── home/                     # Rolling hero recommendations & personalized rails
│   │       │   ├── genre/                    # Interactive genre categories explorer
│   │       │   ├── schedule/                 # Broadcast schedule timeline with time picker
│   │       │   ├── detail/                   # Episode lists & metadata HUD
│   │       │   ├── player/                   # Media3 ExoPlayer with YouTube-style double-tap seeking
│   │       │   ├── search/                   # Real-time search with filter drawer
│   │       │   └── account/                  # 1-tap Google account connection & preferences
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

2. **Native HLS Video Engine:**
   - Powered by **Jetpack Media3 (ExoPlayer)**.
   - Anti-leech referer injection (`Referer: https://zokoanime.video/`) resolving master `.m3u8` streams.
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
