<div align="center">

<img src="desktop/src/main/resources/branding/p-music-logo.png" alt="Koda Music" width="240" />

# Koda Music

### Music player for Windows

Koda Music is the continuation of the project previously released as **Primo Music**. The Windows application lives in `desktop/`, while reusable Kotlin logic is kept in `core/`.

</div>

> **Rebranding notice:** starting with version **3.11.0**, **Primo Music** is now **Koda Music**. It is the same project and history, with a new product identity.

> [!IMPORTANT]
> Koda Music is not affiliated with, endorsed by, or connected to YouTube or Google.

## Koda Music 3.11.0 — in development

3.11.0 is a major Windows redesign. The playback engine and account logic remain separate from the interface so the visual layer can evolve without destabilising music playback.

Current 3.11 work includes:

- a new Koda visual system built for desktop instead of reusing the previous Primo interface;
- near-black themes with restrained purple accents and a monochrome option;
- optional Liquid Glass, disabled automatically by Windows Game Mode;
- richer Home with recent listening, playlists, liked music and real YouTube Music discovery shelves;
- Explore with real YouTube Music moods and genres;
- search filters for songs, videos, albums, artists and playlists;
- artist / album / playlist browse pages with playable results and related shelves when returned by YouTube Music;
- library hub with liked songs, history and playlists;
- queue, previous/next, shuffle, repeat, seek, mute and volume;
- fullscreen now-playing experience with artwork-derived background and lyrics;
- direct Innertube playback with local NewPipeExtractor fallback;
- deterministic shutdown of the mpv process owned by Koda Music.

Downloads, local files and listening Replay/statistics have visible places in the new library architecture, but they are not presented as complete until their Windows implementations are actually validated.

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

Expected portable artifact for 3.11:

```text
release\KodaMusic-3.11.0-Portable.zip
```

Generate EXE and MSI installers:

```text
BUILD-RELEASE.bat
```

## Project layout

```text
app/       Original BitChord Android source preserved for reference / opt-in builds
core/      Shared Kotlin logic and YouTube Music metadata/account functions
desktop/   Koda Music Windows app and audio engine
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

Public Piped/Invidious instances are not part of the normal playback path, and Koda does not use `yt-dlp` for normal playback.

## Performance principles

Koda is designed to look premium without making visual effects the main consumer of resources. Lists are lazy, background work is kept separate from rendering, Liquid Glass is optional, and Windows Game Mode reduces visual overhead without intentionally lowering the selected audio quality.

## Origin, license and attribution

Koda Music is an independent Windows project developed from an open-source codebase originally published by the **BitChord** project.

BitChord does not belong to the maintainers of Koda Music. The original BitChord project and its authors retain their respective copyrights and attribution. Applicable GPLv3 obligations, third-party notices and original licensing requirements are preserved.

Koda Music / Primo Music project history:

```text
Primo Music  →  Koda Music
```

BitChord belongs under origin and attribution, not as a previous Koda product name.

Keep `LICENSE` and the applicable third-party notices with redistributed source/binaries and comply with GPLv3 source-distribution requirements.
