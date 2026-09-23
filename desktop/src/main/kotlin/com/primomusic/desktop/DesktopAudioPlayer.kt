package com.primomusic.desktop

import com.primomusic.desktop.stream.DirectInnertubeStreamResolver
import com.primomusic.desktop.stream.NewPipeStreamResolver
import com.primomusic.desktop.stream.PipedStreamResolver
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.cancel
import java.io.File
import java.io.FileOutputStream
import java.nio.charset.StandardCharsets
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong
import kotlin.concurrent.thread

/** Quality preference used by both desktop stream resolvers. */
enum class AudioQuality(
    val label: String,
    internal val piped: PipedStreamResolver.QualityPreference,
) {
    AUTO("Automática", PipedStreamResolver.QualityPreference.AUTO),
    HIGH("Alta", PipedStreamResolver.QualityPreference.HIGH),
    BALANCED("Equilibrada", PipedStreamResolver.QualityPreference.BALANCED),
    DATA_SAVER("Economia", PipedStreamResolver.QualityPreference.DATA_SAVER),
}

/**
 * Windows player architecture:
 *
 * videoId
 *   -> YouTube Innertube direct player API (primary)
 *   -> local NewPipeExtractor + YouTube player JavaScript (fallback)
 *   -> short-lived direct audio URL
 *   -> mpv HTTPS streaming
 *
 * No yt-dlp is invoked and no temporary media file is created.
 *
 * If a media URL expires or fails, the next candidate is tried. When candidates
 * from one provider are exhausted, the next refresh gives priority to the other
 * provider and playback resumes near the last known position.
 */
class DesktopAudioPlayer(
    private val directResolver: DirectInnertubeStreamResolver = DirectInnertubeStreamResolver(),
    private val newPipeResolver: NewPipeStreamResolver = NewPipeStreamResolver(),
) {
    enum class State {
        IDLE,
        RESOLVING,
        PLAYING,
        PAUSED,
        STOPPED,
        ERROR,
    }

    private enum class StreamProvider(
        val label: String,
    ) {
        DIRECT("Innertube"),
        NEWPIPE("NewPipe local"),
    }

    data class Snapshot(
        val state: State = State.IDLE,
        val positionMillis: Long = 0L,
        val durationMillis: Long = 0L,
        val volume: Double = 0.65,
        val muted: Boolean = false,
        val message: String? = null,
        val streamInfo: String? = null,
    )

    @Volatile
    var snapshot: Snapshot = Snapshot()
        private set

    @Volatile
    private var process: Process? = null

    @Volatile
    private var updater: ((Snapshot) -> Unit)? = null

    @Volatile
    private var currentVideoId: String? = null

    @Volatile
    private var currentQuality: AudioQuality = AudioQuality.AUTO

    @Volatile
    private var candidates: List<PipedStreamResolver.AudioStream> = emptyList()

    @Volatile
    private var candidateIndex = -1

    @Volatile
    private var currentUrl: String? = null

    @Volatile
    private var currentStream: PipedStreamResolver.AudioStream? = null

    @Volatile
    private var failedUrls: Set<String> = emptySet()

    @Volatile
    private var refreshRounds = 0

    @Volatile
    private var repeatEnabled = false

    @Volatile
    private var currentProvider: StreamProvider? = null

    /**
     * Provider that should be attempted first on the next fresh resolution.
     *
     * Initial playback starts with direct Innertube. If YouTube returns a
     * URL that needs player-JS work, the local NewPipe fallback is next.
     */
    @Volatile
    private var preferredProvider: StreamProvider = StreamProvider.DIRECT

    @Volatile
    private var basePositionMillis = 0L

    @Volatile
    private var startedAtNanos = 0L

    @Volatile
    private var currentPipePath: String? = null

    @Volatile
    private var expectedStopSerial = -1L

    @Volatile
    private var lastEngineError = ""

    @Volatile
    var onEnd: (() -> Unit)? = null

    private val generation = AtomicLong(0L)
    private val launchSerial = AtomicLong(0L)
    private val volumeGeneration = AtomicLong(0L)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Volatile
    private var activePlayJob: Job? = null

    suspend fun play(
        videoId: String,
        volume: Double,
        quality: AudioQuality,
        durationHintMillis: Long = 0L,
        onUpdate: (Snapshot) -> Unit,
    ) {
        require(videoId.isNotBlank()) {
            "Esta música não possui um videoId reproduzível."
        }

        // Only one user-triggered resolution is allowed to own the player.
        // Clicking another track cancels the previous resolve instead of
        // letting two resolver walks continue in parallel.
        val thisJob = currentCoroutineContext()[Job]
        activePlayJob?.takeIf { it !== thisJob }?.cancel()
        activePlayJob = thisJob

        val myGeneration = generation.incrementAndGet()

        updater = onUpdate
        stopProcess(expected = true)

        currentVideoId = videoId
        currentQuality = quality

        candidates = emptyList()
        candidateIndex = -1
        currentUrl = null
        currentStream = null
        failedUrls = emptySet()
        refreshRounds = 0

        currentProvider = null
        preferredProvider = StreamProvider.DIRECT

        basePositionMillis = 0L
        lastEngineError = ""

        snapshot =
            snapshot.copy(
                state = State.RESOLVING,
                positionMillis = 0L,
                durationMillis = durationHintMillis.coerceAtLeast(0L),
                volume = volume.coerceIn(0.0, 1.0),
                message = "Conectando ao streaming…",
                streamInfo = null,
            )

        emit()

        resolveFresh(
            myGeneration = myGeneration,
            resumeMillis = 0L,
        )
    }

    fun toggle(
        onUpdate: (Snapshot) -> Unit,
    ) {
        updater = onUpdate

        when (snapshot.state) {
            State.PLAYING ->
                pause()

            State.PAUSED ->
                resume()

            State.IDLE,
            State.STOPPED,
            State.ERROR,
            -> {
                val videoId =
                    currentVideoId
                        ?: return

                scope.launch {
                    val myGeneration =
                        generation.incrementAndGet()

                    currentVideoId = videoId

                    candidates = emptyList()
                    candidateIndex = -1
                    currentUrl = null
                    currentStream = null
                    failedUrls = emptySet()
                    refreshRounds = 0

                    currentProvider = null
                    preferredProvider = StreamProvider.DIRECT

                    resolveFresh(
                        myGeneration = myGeneration,
                        resumeMillis = 0L,
                    )
                }
            }

            State.RESOLVING ->
                Unit
        }
    }

    fun stop(
        onUpdate: (Snapshot) -> Unit,
    ) {
        updater = onUpdate

        generation.incrementAndGet()

        stopProcess(
            expected = true,
        )

        basePositionMillis = 0L

        snapshot =
            snapshot.copy(
                state = State.STOPPED,
                positionMillis = 0L,
                message = null,
            )

        emit()
    }

    fun seek(
        progress: Float,
        onUpdate: (Snapshot) -> Unit,
    ) {
        updater = onUpdate

        val duration =
            snapshot.durationMillis

        if (
            duration <= 0L ||
            currentUrl == null
        ) {
            return
        }

        val target =
            (
                duration *
                    progress.coerceIn(
                        0f,
                        1f,
                    )
                ).toLong()

        val wasPlaying =
            snapshot.state ==
                State.PLAYING

        basePositionMillis =
            target

        if (
            process?.isAlive == true &&
            sendJsonIpc(
                """{"command":["seek",${target / 1000.0},"absolute+exact"]}"""
            )
        ) {
            startedAtNanos =
                System.nanoTime()

            snapshot =
                snapshot.copy(
                    positionMillis = target,
                    message = null,
                )

            emit()

            return
        }

        /*
         * If IPC is temporarily unavailable, restart the same HTTP stream at
         * the selected position. Still no media file is written to disk.
         */
        stopProcess(
            expected = true,
        )

        snapshot =
            snapshot.copy(
                positionMillis = target,
                state =
                    if (wasPlaying) {
                        State.RESOLVING
                    } else {
                        State.PAUSED
                    },
                message =
                    if (wasPlaying) {
                        "Reposicionando…"
                    } else {
                        null
                    },
            )

        emit()

        if (wasPlaying) {
            launchCurrent(
                myGeneration = generation.get(),
                startMillis = target,
                status = "Reposicionando…",
            )
        }
    }

    fun setVolume(
        value: Float,
        onUpdate: (Snapshot) -> Unit,
    ) {
        updater = onUpdate

        val normalized =
            value
                .coerceIn(
                    0f,
                    1f,
                )
                .toDouble()

        snapshot =
            snapshot.copy(
                volume = normalized,
                muted =
                    if (normalized > 0.0) {
                        false
                    } else {
                        snapshot.muted
                    },
            )

        emit()

        val ticket =
            volumeGeneration
                .incrementAndGet()

        thread(
            name = "PrimoMusic-volume",
            isDaemon = true,
        ) {
            Thread.sleep(45)

            if (
                volumeGeneration.get() !=
                ticket
            ) {
                return@thread
            }

            val percent =
                (normalized * 100.0)
                    .coerceIn(
                        0.0,
                        100.0,
                    )

            if (
                process?.isAlive == true &&
                sendJsonIpc(
                    """{"command":["set_property","volume",$percent]}"""
                )
            ) {
                return@thread
            }

            if (
                snapshot.state ==
                State.PLAYING &&
                currentUrl != null
            ) {
                restartAt(
                    positionMillis =
                        currentPositionMillis(),
                    status =
                        "Aplicando volume…",
                )
            }
        }
    }

    fun toggleMute(
        onUpdate: (Snapshot) -> Unit,
    ) {
        updater = onUpdate

        val newMuted =
            !snapshot.muted

        snapshot =
            snapshot.copy(
                muted = newMuted,
            )

        emit()

        if (
            process?.isAlive == true &&
            sendJsonIpc(
                """{"command":["set_property","mute",$newMuted]}"""
            )
        ) {
            return
        }

        if (
            snapshot.state ==
            State.PLAYING &&
            currentUrl != null
        ) {
            restartAt(
                positionMillis =
                    currentPositionMillis(),
                status =
                    "Aplicando áudio…",
            )
        }
    }

    fun setRepeat(
        enabled: Boolean,
    ) {
        repeatEnabled =
            enabled
    }

    fun close() {
        generation.incrementAndGet()

        activePlayJob?.cancel()
        activePlayJob = null

        stopProcess(
            expected = true,
        )

        currentVideoId = null
        currentUrl = null
        currentStream = null
        candidates = emptyList()
        failedUrls = emptySet()
        updater = null
        onEnd = null

        directResolver.close()
        newPipeResolver.close()
        scope.cancel()
    }

    /**
     * Resolve a fresh set of direct-media candidates.
     *
     * Direct Innertube is preferred initially. If it cannot produce a proven
     * URL, the local NewPipe extractor is tried in the same call. If a playable
     * URL later expires in mpv, the next refresh flips priority.
     */
    private suspend fun resolveFresh(
        myGeneration: Long,
        resumeMillis: Long,
    ) {
        if (!isCurrent(myGeneration)) {
            return
        }

        val videoId =
            currentVideoId
                ?: return

        snapshot =
            snapshot.copy(
                state = State.RESOLVING,
                positionMillis = resumeMillis,
                message =
                    if (refreshRounds == 0) {
                        "Buscando stream direto…"
                    } else {
                        "Renovando stream…"
                    },
            )

        emit()

        val resolution =
            try {
                resolveWithFallback(
                    videoId = videoId,
                    quality = currentQuality.piped,
                    excludedUrls = failedUrls,
                )
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Throwable) {
                if (!isCurrent(myGeneration)) {
                    return
                }

                snapshot =
                    snapshot.copy(
                        state = State.ERROR,
                        message =
                            error.message
                                ?.takeIf {
                                    it.isNotBlank()
                                }
                                ?.take(520)
                                ?: "Não foi possível obter um stream de áudio.",
                    )

                emit()

                return
            }

        if (!isCurrent(myGeneration)) {
            return
        }

        candidates =
            resolution.streams

        candidateIndex =
            -1

        resolution.durationMillis
            ?.takeIf {
                it > 0L
            }
            ?.let { resolvedDuration ->
                snapshot =
                    snapshot.copy(
                        durationMillis =
                            resolvedDuration,
                    )
            }

        startNextCandidate(
            myGeneration = myGeneration,
            resumeMillis = resumeMillis,
        )
    }

    /**
     * Uses the currently preferred local resolver first and automatically
     * falls back to the other one. No public Piped/Invidious service is on the
     * normal playback path anymore.
     */
    private suspend fun resolveWithFallback(
        videoId: String,
        quality: PipedStreamResolver.QualityPreference,
        excludedUrls: Set<String>,
    ): PipedStreamResolver.Resolution {

        val providers =
            when (preferredProvider) {
                StreamProvider.DIRECT ->
                    listOf(
                        StreamProvider.DIRECT,
                        StreamProvider.NEWPIPE,
                    )

                StreamProvider.NEWPIPE ->
                    listOf(
                        StreamProvider.NEWPIPE,
                        StreamProvider.DIRECT,
                    )
            }

        val failures = mutableListOf<String>()

        for (provider in providers) {
            try {
                println("[Player] Resolvendo via ${provider.label}")

                val resolution =
                    when (provider) {
                        StreamProvider.DIRECT ->
                            directResolver.resolve(
                                videoId = videoId,
                                quality = quality,
                                excludedUrls = excludedUrls,
                            )

                        StreamProvider.NEWPIPE ->
                            newPipeResolver.resolve(
                                videoId = videoId,
                                quality = quality,
                                excludedUrls = excludedUrls,
                            )
                    }

                currentProvider = provider

                println(
                    "[Player] ${provider.label} forneceu " +
                        "${resolution.streams.size} stream(s)"
                )

                return resolution
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Throwable) {
                val message =
                    error.message
                        ?.lineSequence()
                        ?.firstOrNull()
                        ?.trim()
                        ?.take(220)
                        ?.takeIf { it.isNotBlank() }
                        ?: error::class.simpleName
                        ?: "erro desconhecido"

                println("[Player] ${provider.label} indisponível -> $message")
                failures += "${provider.label}: $message"
            }
        }

        println(
            "[Player] Falhas dos resolvers: " +
                failures.joinToString(" | ")
        )

        throw IllegalStateException(
            "Não foi possível conectar a uma fonte de áudio."
        )
    }

    private suspend fun startNextCandidate(
        myGeneration: Long,
        resumeMillis: Long,
    ) {
        if (!isCurrent(myGeneration)) {
            return
        }

        candidateIndex +=
            1

        val stream =
            candidates.getOrNull(
                candidateIndex
            )

        if (stream == null) {

            /*
             * All direct URLs returned by the current provider failed.
             * The next resolution round starts with the other provider.
             */
            preferredProvider =
                when (currentProvider) {
                    StreamProvider.DIRECT ->
                        StreamProvider.NEWPIPE

                    StreamProvider.NEWPIPE ->
                        StreamProvider.DIRECT

                    null ->
                        StreamProvider.DIRECT
                }

            if (
                refreshRounds <
                MAX_REFRESH_ROUNDS
            ) {
                refreshRounds +=
                    1

                delay(
                    300L *
                        refreshRounds
                )

                resolveFresh(
                    myGeneration =
                        myGeneration,
                    resumeMillis =
                        resumeMillis,
                )
            } else {
                snapshot =
                    snapshot.copy(
                        state = State.ERROR,
                        message =
                            buildString {
                                append(
                                    "As fontes responderam, mas nenhum stream conseguiu tocar."
                                )

                                lastEngineError
                                    .takeIf {
                                        it.isNotBlank()
                                    }
                                    ?.let { engineError ->
                                        append(" ")
                                        append(
                                            engineError.take(
                                                260
                                            )
                                        )
                                    }
                            },
                    )

                emit()
            }

            return
        }

        failedUrls =
            failedUrls +
                stream.url

        currentUrl =
            stream.url

        currentStream =
            stream

        snapshot =
            snapshot.copy(
                state = State.RESOLVING,
                positionMillis = resumeMillis,
                message =
                    "Abrindo stream…",
                streamInfo =
                    listOfNotNull(
                        currentProvider?.label,
                        stream.mimeType,
                        stream.codec,
                        stream.bitrate
                            .takeIf {
                                it > 0
                            }
                            ?.let {
                                "${it / 1000} kbps"
                            },
                    )
                        .joinToString(
                            " • "
                        )
                        .ifBlank {
                            null
                        },
            )

        emit()

        val launchError =
            try {
                launchCurrent(
                    myGeneration =
                        myGeneration,
                    startMillis =
                        resumeMillis,
                    status =
                        "Abrindo stream…",
                )

                null
            } catch (error: Throwable) {
                error
            }

        if (launchError != null) {
            lastEngineError =
                launchError.message
                    .orEmpty()

            startNextCandidate(
                myGeneration =
                    myGeneration,
                resumeMillis =
                    resumeMillis,
            )

            return
        }

        /*
         * A dead or rejected direct-media URL generally makes mpv exit quickly.
         * If mpv is still alive after this startup window, playback is considered
         * established.
         */
        delay(
            1_250
        )

        if (!isCurrent(myGeneration)) {
            return
        }

        if (
            process?.isAlive ==
            true
        ) {
            markPlaying(
                resumeMillis
            )
        }
        // If mpv already exited, watchProcess() is the single owner of the
        // failover. Calling startNextCandidate() here as well caused duplicate
        // Piped/Invidious/NewPipe walks in older builds.
    }

    private fun pause() {
        val position =
            currentPositionMillis()

        basePositionMillis =
            position

        if (
            process?.isAlive == true &&
            sendJsonIpc(
                """{"command":["set_property","pause",true]}"""
            )
        ) {
            snapshot =
                snapshot.copy(
                    state = State.PAUSED,
                    positionMillis = position,
                    message = null,
                )

            emit()

            return
        }

        /*
         * Last-resort fallback when Windows blocks a pipe connection.
         */
        stopProcess(
            expected = true,
        )

        snapshot =
            snapshot.copy(
                state = State.PAUSED,
                positionMillis = position,
                message = null,
            )

        emit()
    }

    private fun resume() {
        val position =
            basePositionMillis

        val myGeneration =
            generation.get()

        if (
            process?.isAlive == true &&
            sendJsonIpc(
                """{"command":["set_property","pause",false]}"""
            )
        ) {
            markPlaying(
                position
            )

            return
        }

        launchCurrent(
            myGeneration =
                myGeneration,
            startMillis =
                position,
            status =
                "Continuando…",
        )

        thread(
            name =
                "PrimoMusic-resume",
            isDaemon =
                true,
        ) {
            Thread.sleep(
                850
            )

            if (
                process?.isAlive == true &&
                isCurrent(
                    myGeneration
                )
            ) {
                markPlaying(
                    position
                )
            }
        }
    }

    private fun restartAt(
        positionMillis: Long,
        status: String,
    ) {
        val myGeneration =
            generation.get()

        if (
            !isCurrent(
                myGeneration
            ) ||
            currentUrl == null
        ) {
            return
        }

        stopProcess(
            expected = true,
        )

        launchCurrent(
            myGeneration =
                myGeneration,
            startMillis =
                positionMillis,
            status =
                status,
        )

        thread(
            name =
                "PrimoMusic-restart",
            isDaemon =
                true,
        ) {
            Thread.sleep(
                850
            )

            if (
                process?.isAlive == true &&
                isCurrent(
                    myGeneration
                )
            ) {
                markPlaying(
                    positionMillis
                )
            }
        }
    }

    private fun launchCurrent(
        myGeneration: Long,
        startMillis: Long,
        status: String,
    ) {
        if (!isCurrent(myGeneration)) {
            return
        }

        val url =
            currentUrl
                ?: error(
                    "Nenhum stream disponível."
                )

        val stream =
            currentStream

        val mpv =
            RuntimeTools.ensureMpv()

        stopProcess(
            expected = true,
        )

        basePositionMillis =
            startMillis
                .coerceAtLeast(
                    0L
                )

        snapshot =
            snapshot.copy(
                state = State.RESOLVING,
                positionMillis =
                    basePositionMillis,
                message = status,
            )

        emit()

        val serial =
            launchSerial
                .incrementAndGet()

        val pipe =
            "\\\\.\\pipe\\primo-music-" +
                "${ProcessHandle.current().pid()}-" +
                serial

        val args =
            mutableListOf(
                mpv.absolutePath,
                "--no-config",
                "--no-video",
                "--audio-display=no",
                "--force-window=no",

                /*
                 * The URL is already a direct media URL.
                 * mpv must never invoke yt-dlp for it.
                 */
                "--ytdl=no",

                "--cache=yes",
                "--cache-secs=20",
                "--cache-pause=yes",
                "--network-timeout=12",

                "--input-ipc-server=$pipe",

                "--volume=${
                    (snapshot.volume * 100.0)
                        .coerceIn(
                            0.0,
                            100.0,
                        )
                }",

                "--mute=${
                    if (snapshot.muted) {
                        "yes"
                    } else {
                        "no"
                    }
                }",

                "--user-agent=${stream?.userAgent ?: RuntimeTools.USER_AGENT}",
                "--msg-level=all=warn",
            )

        stream?.referrer?.takeIf { it.isNotBlank() }?.let { referrer ->
            args += "--referrer=$referrer"
        }

        stream?.origin?.takeIf { it.isNotBlank() }?.let { origin ->
            args += "--http-header-fields=Origin: $origin"
        }

        if (stream?.userAgent == null && stream?.referrer == null) {
            args += "--referrer=https://www.youtube.com/"
        }

        if (
            basePositionMillis >
            0L
        ) {
            args +=
                "--start=${
                    basePositionMillis /
                        1000.0
                }"
        }

        /*
         * Prevent a media URL beginning with "-" from being interpreted
         * as an mpv command-line option.
         */
        args +=
            "--"

        args +=
            url

        val launched =
            ProcessBuilder(
                args
            )
                .directory(
                    mpv.parentFile
                )
                .redirectErrorStream(
                    true
                )
                .start()

        process =
            launched

        currentPipePath =
            pipe

        startedAtNanos =
            System.nanoTime()

        expectedStopSerial =
            -1L

        lastEngineError =
            ""

        drainMpvOutput(
            launched =
                launched,
            myGeneration =
                myGeneration,
            serial =
                serial,
        )

        startTicker(
            launched =
                launched,
            myGeneration =
                myGeneration,
            serial =
                serial,
        )

        watchProcess(
            launched =
                launched,
            myGeneration =
                myGeneration,
            serial =
                serial,
        )
    }

    private fun drainMpvOutput(
        launched: Process,
        myGeneration: Long,
        serial: Long,
    ) {
        thread(
            name =
                "PrimoMusic-mpv-log",
            isDaemon =
                true,
        ) {
            val tail =
                ArrayDeque<String>()

            runCatching {
                launched
                    .inputStream
                    .bufferedReader()
                    .useLines { lines ->
                        lines.forEach { raw ->
                            if (
                                !isCurrent(
                                    myGeneration
                                ) ||
                                launchSerial.get() !=
                                serial
                            ) {
                                return@forEach
                            }

                            val safe =
                                raw
                                    .replace(
                                        Regex(
                                            """https?://\S+"""
                                        ),
                                        "[stream-url]",
                                    )
                                    .replace(
                                        Regex(
                                            """(?i)(sig|signature|token)=[^&\s]+"""
                                        ),
                                        "$1=[redacted]",
                                    )
                                    .trim()
                                    .take(
                                        220
                                    )

                            if (
                                safe.isNotBlank()
                            ) {
                                tail.addLast(
                                    safe
                                )

                                while (
                                    tail.size >
                                    6
                                ) {
                                    tail.removeFirst()
                                }

                                lastEngineError =
                                    tail.joinToString(
                                        " | "
                                    )
                            }
                        }
                    }
            }
        }
    }

    private fun startTicker(
        launched: Process,
        myGeneration: Long,
        serial: Long,
    ) {
        thread(
            name =
                "PrimoMusic-position",
            isDaemon =
                true,
        ) {
            while (
                launched.isAlive &&
                isCurrent(
                    myGeneration
                ) &&
                launchSerial.get() ==
                serial
            ) {
                if (
                    snapshot.state ==
                    State.PLAYING
                ) {
                    snapshot =
                        snapshot.copy(
                            positionMillis =
                                currentPositionMillis(),
                        )

                    emit()
                }

                Thread.sleep(
                    250
                )
            }
        }
    }

    private fun watchProcess(
        launched: Process,
        myGeneration: Long,
        serial: Long,
    ) {
        thread(
            name =
                "PrimoMusic-mpv-watch",
            isDaemon =
                true,
        ) {
            val exitCode =
                runCatching {
                    launched.waitFor()
                }
                    .getOrDefault(
                        -1
                    )

            if (
                !isCurrent(
                    myGeneration
                ) ||
                launchSerial.get() !=
                serial ||
                expectedStopSerial ==
                serial
            ) {
                return@thread
            }

            val position =
                currentPositionMillis()

            val duration =
                snapshot.durationMillis

            val nearNaturalEnd =
                duration > 0L &&
                    position >=
                    (
                        duration -
                            2_500L
                        )
                        .coerceAtLeast(
                            0L
                        )

            val unknownDurationNaturalEnd =
                duration <= 0L &&
                    exitCode == 0 &&
                    position > 8_000L

            if (
                (
                    exitCode == 0 &&
                    nearNaturalEnd
                    ) ||
                unknownDurationNaturalEnd
            ) {
                if (repeatEnabled) {
                    scope.launch {
                        failedUrls =
                            emptySet()

                        refreshRounds =
                            0

                        /*
                         * The provider that successfully completed the track
                         * gets first chance again on repeat.
                         */
                        preferredProvider =
                            currentProvider
                                ?: StreamProvider.DIRECT

                        resolveFresh(
                            myGeneration =
                                myGeneration,
                            resumeMillis =
                                0L,
                        )
                    }
                } else {
                    snapshot =
                        snapshot.copy(
                            state =
                                State.STOPPED,
                            positionMillis =
                                if (
                                    duration >
                                    0L
                                ) {
                                    duration
                                } else {
                                    position
                                },
                            message =
                                null,
                        )

                    emit()

                    onEnd
                        ?.invoke()
                }

                return@thread
            }

            println(
                "[mpv] saiu com código $exitCode; posição=${position}ms; " +
                    "detalhe=${lastEngineError.take(500)}"
            )

            /*
             * The short-lived media URL may have expired or the CDN may have
             * rejected it. Try another candidate first. If all candidates fail,
             * startNextCandidate() switches resolver priority automatically.
             */
            scope.launch {
                snapshot =
                    snapshot.copy(
                        state =
                            State.RESOLVING,
                        positionMillis =
                            position,
                        message =
                            "Reconectando stream…",
                    )

                emit()

                startNextCandidate(
                    myGeneration =
                        myGeneration,
                    resumeMillis =
                        position,
                )
            }
        }
    }

    @Synchronized
    private fun stopProcess(
        expected: Boolean,
    ) {
        val active =
            process

        if (
            active != null &&
            active.isAlive
        ) {
            if (expected) {
                expectedStopSerial =
                    launchSerial.get()
            }

            runCatching {
                active.destroy()
            }

            runCatching {
                if (
                    !active.waitFor(
                        450,
                        TimeUnit.MILLISECONDS,
                    )
                ) {
                    active.destroyForcibly()
                }
            }
        }

        process =
            null

        currentPipePath =
            null
    }

    /**
     * mpv's --input-ipc-server is JSON IPC, not input.conf text.
     *
     * Each write below is one valid JSON command followed by a newline.
     */
    private fun sendJsonIpc(
        jsonCommand: String,
    ): Boolean {
        val pipe =
            currentPipePath
                ?: return false

        repeat(
            6
        ) { attempt ->
            try {
                FileOutputStream(
                    pipe
                )
                    .use { output ->
                        output.write(
                            (
                                jsonCommand +
                                    "\n"
                                )
                                .toByteArray(
                                    StandardCharsets.UTF_8
                                )
                        )

                        output.flush()
                    }

                return true
            } catch (_: Throwable) {
                if (
                    attempt <
                    5
                ) {
                    Thread.sleep(
                        35L *
                            (attempt + 1)
                    )
                }
            }
        }

        return false
    }

    private fun markPlaying(
        positionMillis: Long,
    ) {
        basePositionMillis =
            positionMillis
                .coerceAtLeast(
                    0L
                )

        startedAtNanos =
            System.nanoTime()

        snapshot =
            snapshot.copy(
                state =
                    State.PLAYING,
                positionMillis =
                    basePositionMillis,
                message =
                    null,
            )

        emit()
    }

    private fun currentPositionMillis(): Long {
        if (
            snapshot.state !=
            State.PLAYING
        ) {
            return basePositionMillis
                .coerceAtLeast(
                    0L
                )
        }

        val elapsed =
            (
                (
                    System.nanoTime() -
                        startedAtNanos
                    ) /
                    1_000_000L
                )
                .coerceAtLeast(
                    0L
                )

        val raw =
            basePositionMillis +
                elapsed

        return snapshot
            .durationMillis
            .takeIf {
                it > 0L
            }
            ?.let { duration ->
                raw.coerceIn(
                    0L,
                    duration,
                )
            }
            ?: raw
    }

    private fun isCurrent(
        value: Long,
    ): Boolean =
        generation.get() ==
            value

    private fun emit() {
        updater
            ?.invoke(
                snapshot
            )
    }

    private companion object {
        const val MAX_REFRESH_ROUNDS =
            1
    }
}

/**
 * Locates the mpv executable from the single Compose application-resources
 * location used by release builds, with only the Gradle staging path kept for
 * local development. No runtime download or legacy yt-dlp/tool directory is used.
 */
private object RuntimeTools {

    const val USER_AGENT =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
            "AppleWebKit/537.36 " +
            "Chrome/153.0.0.0 " +
            "Safari/537.36"

    fun ensureMpv(): File {
        val packagedResourcesDir =
            System.getProperty("compose.application.resources.dir")
                ?.trim()
                ?.takeIf { it.isNotEmpty() }
                ?.let(::File)

        val workingDir = File(System.getProperty("user.dir"))

        // Release: the Compose Desktop launcher supplies the resource directory.
        // Development: :desktop:run can read the same verified staging output.
        val candidates =
            listOfNotNull(
                packagedResourcesDir?.resolve("mpv/mpv.exe"),
                packagedResourcesDir?.resolve("mpv.exe"),
                workingDir.resolve("desktop/build/packaging-resources/windows-x64/mpv/mpv.exe"),
                workingDir.resolve("build/packaging-resources/windows-x64/mpv/mpv.exe"),
            )

        return candidates.firstOrNull { it.isFile && it.length() > 0L }
            ?: error(
                "O motor de áudio mpv não foi encontrado no pacote. " +
                    "Gere novamente a versão Portable."
            )
    }
}
