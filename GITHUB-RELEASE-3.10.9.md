# Primo Music 3.10.9

Primo Music 3.10.9 brings the Windows desktop player to a self-contained, release-ready package.

## Highlights

- Windows-native Compose Desktop app.
- Direct YouTube Music / Innertube playback with local NewPipeExtractor fallback.
- mpv 0.41.0 bundled for audio playback.
- Java 21 runtime bundled with the Windows package.
- No `yt-dlp`, no separate Java install and no first-run mpv download.
- Search, queue, previous/next, shuffle, repeat, seek, volume and fullscreen playback.
- Google/YouTube Music session features, history, liked songs and playlists.
- Dark/light themes, lyrics and optional Liquid Glass.
- Public build launches as a normal GUI app without the diagnostic console.

## Portable

Download `PrimoMusic-3.10.9-Portable.zip`, extract the complete folder and open `PrimoMusic.exe`.

Do not move only the EXE out of the extracted folder: the bundled `app` and `runtime` directories are part of the portable application.

## Installers

EXE and MSI packages can also be attached to this release for users who prefer installation through Windows.

## Playback notes

Primo Music first attempts direct Innertube stream resolution. When YouTube does not provide a compatible direct audio URL, the desktop player automatically falls back to the local NewPipeExtractor path and then sends the resolved audio URL directly to mpv.

## Requirements

- Windows 10 or Windows 11 x64.
- Internet connection for online playback.

Java and mpv are already included in the distributed Windows package.

## License

Primo Music is based on the GPLv3 BitChord codebase. See `LICENSE` and `THIRD_PARTY-NOTICES-PRIMO.md` for licensing and attribution details.
