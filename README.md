# Peerless (Client)

> **Bit-Perfect Lossless & Spatial Audio Streaming Client for Telegram MTProto**  
> Cross-platform Kotlin Multiplatform (Android, iOS, Desktop JVM) client for [
`peerless-server`](https://github.com/Hitarashi/peerless-server).

---

## Overview

**Peerless** is an audiophile-focused music player delivering uncompressed lossless audio directly
to Android, iOS, and Desktop. Powered by Telegram MTProto cloud storage via `peerless-server`, it
streams native **Apple Lossless (ALAC up to 24-bit/192kHz)**, **FLAC (up to 24-bit/192kHz)**, and *
*Dolby Atmos (Spatial Audio EC-3 / E-AC-3 JOC)** with sub-200ms latency.

The interface is inspired by **Material 3 Expressive**, featuring liquid glass surfaces, an adaptive
layout conforming to Google Large Screen Guidelines, kinetic synchronized lyrics, real-time FFT
spectrum visualizer, Poweramp-style audio signal path inspection, and Spotify Connect-style
cross-device synchronization.

---

## Key Features

### 🎧 Bit-Perfect Audiophile Playback

- **Direct Lossless Passthrough**: Native decoding of ALAC and FLAC up to 24-bit/192kHz with zero
  server transcoding overhead.
- **Dolby Atmos Spatial Audio**: Native E-AC-3 JOC playback on Android via bundled
  `media3-ffmpeg-decoder` (Jellyfin FFmpeg JNI), AVPlayer on iOS, and libmpv on Desktop.
- **Poweramp-Style Signal Path (Audio Details Modal)**: Interactive vertical flow inspection mapping
  every stage:
  `Source → Track Specs → Engine Decoder → Stream Cache/Pipe → Hardware Output Sink & DAC`.
- **Gapless Preloading**: Multi-track buffer pre-fetching (`prepareNext`) ensuring continuous
  transition across album tracks.

### 🔍 Multi-Provider Canonical Catalog & Deduplication

- **Unified Catalog**: Aggregates Apple Music and Qobuz catalogs into a seamless experience.
- **3-Tier DSU Deduplication (`CanonicalDeduplicator`)**: Merges identical musical works across
  providers using:
    1. Provider Track ID
    2. Universal ISRC (International Standard Recording Code)
    3. Alphanumeric title + artist normalization with duration tolerance ($\le 3\text{s}$)
- **Intelligent Source Resolution**:
    - Spatial audio prioritized when the device supports multi-channel output.
    - Quality ladder ranking (192kHz > 96kHz > 48kHz > 44.1kHz, 24-bit > 16-bit).
    - Instant playback from cached Telegram dump channel (<200ms) with automated background
      acquisition of superior renditions.
- **Minimal Icon-First UI**: Sleek vector silhouettes (``, `qobuz`, `Dolby Atmos`) replacing heavy
  badge pills and text clutter.

### 🎤 XMusic-Inspired Kinetic Synced Lyrics

- **Syllable-Level Timing**: Word-by-word dynamic sweep and line-synced LRC/TTML rendering.
- **Character-Conforming Vocal Glow**: Offscreen `saveLayer` glyph bloom behind actively sung
  syllables with smooth `BlendMode.DstIn` horizontal alpha feathering.
- **Vocal Pulse Physics**: Dynamic glow expansion modulated by vocal duration (
  `vocalPulse = sin(p * PI)`).
- **Luminous Particle Emitter**: Floating sparkle particles originating from the active singing
  needle head.
- **Audio Sink Latency Compensation**: Dynamic audio hardware buffer measurement and interactive
  millisecond sync tuner dialog (`LyricsSyncTunerDialog`).

### 📱 Spotify Connect-Style Cross-Device Playback

- **Full-Duplex WebSocket Synchronization**: Real-time state replication via `/api/v1/ws/playback`.
- **Remote Control & Mirroring**: Passive devices mirror track metadata, queue, and progress without
  local audio output.
- **Active Device Transfer**: 1-tap playback handover across mobile, tablet, and desktop.
- **Playback Device Picker**: Online device discovery and remote control routing.

### 📻 Last.fm Intelligence & Infinite Radio

- **Mandatory Last.fm Login Gate**: Audiophile animated equalizer waveform graphic (
  `ScrobbleWaveformGraphic`) and client-side MD5 signature hashing.
- **Automatic Scrobbling**: Real-time `nowPlaying` and `scrobble` dispatch meeting official Last.fm
  criteria (>30s duration, >=50% or 4 min play time) with single-device scrobble invariants.
- **Infinite Radio**: Dynamic queue generation appending similar tracks via Last.fm when the user
  reaches the end of the queue.
- **Personalized Home Feed**: Recommendations, loved tracks, recent scrobbles, and top artists.

### 🎨 Material 3 Expressive Liquid Glass Shell

- **Google Large Screen Adaptive Layout**:
    - **Compact (<600dp)**: Floating pill navigation dock (Home, Search, Library, Settings) with
      docked MiniPlayer.
    - **Medium (600dp–839dp)**: Vertical Expressive Navigation Rail.
    - **Expanded (≥840dp Desktop/Tablet)**: Supporting Pane Layout (persistent navigation drawer,
      browse/search pane, contextual supporting pane for Queue/Lyrics/Spectrum, and persistent
      bottom
      playback bar).
- **ArchiveTune Wavy Slider (`WavySliderExpressive`)**: Sinusoidal wave progress bar with morphing
  pill thumb during scrubbing.
- **Real-Time Audio Spectrum (FFT)**: Multi-band live frequency visualizer bars reacting dynamically
  to audio output.
- **Dynamic Artwork Glow**: Real-time palette extraction reflecting album hues onto dark translucent
  surfaces.

---

## Architecture & Project Structure

```
Peerless/
├── androidApp/                         # Android application entrypoint & manifest
│   └── src/main/kotlin/org/shilpo/peerless/
│       ├── MainActivity.kt             # Edge-to-edge Activity with window insets
│       └── PeerlessApp.kt              # Application class initializing context
├── desktopApp/                         # Desktop application entrypoint (JVM)
│   └── src/main/kotlin/org/shilpo/peerless/
│       └── Main.kt                     # Compose Desktop window configuration
├── iosApp/                             # iOS Xcode project & SwiftUI bridge
│   └── iosApp/iOSApp.swift             # iOS app wrapper calling Compose UIViewController
├── shared/                             # Kotlin Multiplatform core library
│   ├── commonMain/kotlin/org/shilpo/peerless/
│   │   ├── auth/                       # SessionManager, TokenStorage, DeepLinkHandler
│   │   ├── home/                       # HomeFeedRepository (Last.fm home feed)
│   │   ├── lastfm/                     # LastFmClient, LastFmScrobbler, pure MD5
│   │   ├── library/                    # FavoritesManager (/api/v1/me/favorites)
│   │   ├── lyrics/                     # LyricsLoader, TTML/LRC parsers
│   │   ├── model/                      # DTOs, CanonicalTrack, CanonicalDeduplicator
│   │   ├── network/                    # PeerlessApiClient (Ktor 3.x REST/SSE)
│   │   ├── player/                     # PlayerConnection, RealPlayerConnection, AudioSpectrum
│   │   ├── sync/                       # PlaybackSyncManager (WebSocket Connect)
│   │   ├── tasks/                      # RipCoordinator (SSE progress tracking)
│   │   ├── theme/                      # ExpressiveTheme, LiquidGlass, ColorScheme
│   │   └── ui/                         # Compose Multiplatform UI components & screens
│   ├── androidMain/kotlin/org/shilpo/peerless/
│   │   └── player/                     # AndroidAudioEngine (Media3 + FFmpeg JNI)
│   ├── iosMain/kotlin/org/shilpo/peerless/
│   │   └── player/                     # IosAudioEngine (AVQueuePlayer, NowPlaying)
│   └── jvmMain/kotlin/org/shilpo/peerless/
│       └── player/                     # DesktopAudioEngine (libmpv, MPRIS DBus)
└── docs/                               # Architecture Decision Records (ADRs)
```

---

## Getting Started

### Prerequisites

- **JDK 17+** (JDK 21 recommended)
- **Android SDK** (API 34+) for Android builds
- **Xcode 15+** with CocoaPods for iOS builds
- **libmpv** installed on your system (for Desktop JVM playback):
    - Linux: `sudo apt install libmpv-dev` or `sudo pacman -S mpv`
    - macOS: `brew install mpv`
    - Windows: Place `mpv-2.dll` in your PATH

### Running the Apps

- **Desktop App (Standard)**:
  ```bash
  ./gradlew :desktopApp:run
  ```

- **Desktop App (Hot Reload)**:
  ```bash
  ./gradlew :desktopApp:hotRun --auto
  ```

- **Android App (Debug APK)**:
  ```bash
  ./gradlew :androidApp:assembleDebug
  ```

- **iOS App**:
  Open `iosApp/iosApp.xcworkspace` or `iosApp/` in Xcode and select your target simulator or device.

### Running Tests

```bash
# Common & Desktop Unit Tests
./gradlew :shared:jvmTest

# Desktop Compilation Check
./gradlew :desktopApp:compileKotlin

# Android Host Tests
./gradlew :shared:testAndroidHostTest

# iOS Simulator Tests
./gradlew :shared:iosSimulatorArm64Test
```

---

## Connecting to `peerless-server`

1. Start your `peerless-server` instance (or use a public instance).
2. Open your Telegram bot and send `/stream`.
3. The bot will send you a **Connection Payload** (Base64 string) or an interactive **Open App**
   link.
4. On the Peerless onboarding screen:

- Paste the Connection Payload to automatically configure the server URL and one-time OTP code.
- Or click the Telegram deep link to launch and authenticate automatically (
  `peerless://auth?token=...`).

5. Complete the **Last.fm login gate** to enable full catalog access, scrobbling, and infinite
   radio.

---

## License

This project is licensed under the Apache License 2.0.