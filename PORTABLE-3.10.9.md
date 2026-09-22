# Primo Music 3.10.9 Portable

The portable build now uses one packaging pipeline instead of post-build repair layers.

- `BUILD-PORTABLE.bat` runs `:desktop:portableZip`.
- Compose Desktop/jpackage receives the Desktop main JAR and Gradle `runtimeClasspath` explicitly.
- `includeAllModules = true` keeps the Java 21 runtime self-contained, including `java.net.http`.
- mpv 0.41.0 is downloaded only at build time, SHA-256 verified, and bundled through Compose application resources.
- `verifyPortableImage` checks MainKt, core, Kotlin, Coroutines, Lifecycle, NewPipe, Java runtime and mpv before any ZIP is accepted.
- No `portable-libs`, copied `PrimoMusic-main.jar`, PowerShell classpath repair, or manual `PrimoMusic.cfg` rewrite is used.
- The final file is `release/PrimoMusic-3.10.9-Portable.zip`.

The user extracts the full ZIP and opens `PrimoMusic.exe`. The runtime and resources must remain beside the launcher.
