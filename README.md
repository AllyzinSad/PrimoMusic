<div align="center">

<img src="desktop/src/main/resources/branding/p-music-logo.png" alt="Primo Music" width="220" />

# Primo Music

### YouTube Music desktop player for Windows

Primo Music is a Windows desktop adaptation built from the GPLv3 BitChord codebase. The original Android module remains preserved in `app/`, while the Windows app lives in `desktop/` and shares reusable Kotlin logic through `core/`.

</div>

> [!IMPORTANT]
> Primo Music is not affiliated with, endorsed by, or connected to YouTube or Google.

## Current Windows release — 3.10.9

Primo Music 3.10.9 is the first Windows package in this branch validated end-to-end as a self-contained portable build.

Highlights:

- direct YouTube Music / Innertube playback with a local NewPipeExtractor fallback;
- mpv 0.41.0 network playback on Windows, without `yt-dlp` and without temporary media downloads;
- Java 21 runtime bundled with the native Windows package;
- no separate Java, mpv, Kotlin or Gradle installation required for end users;
- search, account/session features, history, liked songs and playlists;
- queue, previous/next, shuffle, repeat, seek, mute and volume;
- fullscreen now-playing view, lyrics, dark/light themes and optional Liquid Glass;
- native Windows launcher with no diagnostic console in the public release.

## Download

For normal users, attach one or more of these generated artifacts to the GitHub Release:

```text
release\PrimoMusic-3.10.9-Portable.zip
release\*.exe
release\*.msi
```

The portable ZIP is the simplest no-install option: extract the full folder and open `PrimoMusic.exe`.

## Build from source

Requirements:

- Windows 10/11 x64;
- JDK 21;
- internet access on the first Gradle build.

Run the desktop app:

```powershell
.\gradlew.bat :desktop:run
```

Generate the verified portable package:

```text
BUILD-PORTABLE.bat
```

Generate EXE and MSI installers:

```text
BUILD-RELEASE.bat
```

## Project layout

```text
app/       Original Android application (preserved, opt-in)
core/      Shared Kotlin logic / YouTube Music account and metadata
desktop/   Windows Compose Desktop app and audio engine
backend/   Original backend-related project files
```

The Android module is opt-in on desktop machines:

```powershell
.\gradlew.bat -PincludeAndroid=true projects
```

## Playback architecture

```text
YouTube Music metadata / account
            ↓
Direct Innertube resolver
            ↓
local player-JS handling
            ↓
NewPipeExtractor local fallback
            ↓
direct googlevideo audio URL
            ↓
mpv network playback
```

Public Piped/Invidious instances are not part of the normal playback path.

## Self-contained Windows packaging

The Windows build uses one Gradle/Compose Desktop packaging pipeline. The classpath is resolved by Gradle and passed directly to `jpackage`; the final image is validated before the portable ZIP is created. There are no post-build classpath rewrites or compatibility repair folders.

The release build verifies the application main class, Kotlin runtime, Coroutines, Lifecycle/SavedState, NewPipeExtractor, Java runtime and bundled `mpv.exe` before producing the portable ZIP.

## License and attribution

Primo Music is derived from BitChord and remains distributed under GPLv3. Keep `LICENSE` and `THIRD_PARTY-NOTICES-PRIMO.md` with redistributed source/binaries and comply with GPLv3 source-distribution requirements.

The original BitChord project and its authors retain their respective copyrights and attribution.
