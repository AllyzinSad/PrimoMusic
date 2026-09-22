package com.primomusic.desktop.stream

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.withLock
import org.schabi.newpipe.extractor.MediaFormat
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.exceptions.AccountTerminatedException
import org.schabi.newpipe.extractor.exceptions.AgeRestrictedContentException
import org.schabi.newpipe.extractor.exceptions.GeographicRestrictionException
import org.schabi.newpipe.extractor.exceptions.PaidContentException
import org.schabi.newpipe.extractor.exceptions.PrivateContentException
import org.schabi.newpipe.extractor.exceptions.SoundCloudGoPlusContentException
import org.schabi.newpipe.extractor.exceptions.UnsupportedContentInCountryException
import org.schabi.newpipe.extractor.exceptions.YoutubeMusicPremiumContentException
import org.schabi.newpipe.extractor.stream.AudioStream
import org.schabi.newpipe.extractor.stream.DeliveryMethod
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import java.util.Locale
import kotlin.math.abs

/**
 * Full NewPipeExtractor fallback for Primo Music Desktop.
 *
 * Unlike Piped/Invidious, this runs locally inside Primo Music. NewPipe fetches
 * YouTube's watch/player JavaScript itself and resolves signatureCipher + the
 * throttling `n` parameter before this resolver returns a googlevideo URL.
 */
class NewPipeStreamResolver {
    private enum class Probe {
        OK,
        REFUSED,
        UNREACHABLE,
    }

    private data class MediaHeaders(
        val userAgent: String,
        val origin: String? = null,
        val referrer: String? = null,
    )

    private val probeClient = HttpClient.newBuilder()
        .followRedirects(HttpClient.Redirect.NORMAL)
        .connectTimeout(Duration.ofSeconds(5))
        .build()

    suspend fun resolve(
        videoId: String,
        quality: PipedStreamResolver.QualityPreference,
        excludedUrls: Set<String> = emptySet(),
    ): PipedStreamResolver.Resolution {
        require(VIDEO_ID.matches(videoId)) {
            "videoId inválido para streaming."
        }

        NewPipeRuntime.ensureInitialized()

        var lastFailure: Throwable? = null

        repeat(EXTRACTION_ATTEMPTS) { attempt ->
            if (attempt > 0) {
                delay(EXTRACTION_RETRY_MS * attempt)
            }

            try {
                println(
                    "[NewPipe] Extraindo $videoId " +
                        "(tentativa ${attempt + 1}/$EXTRACTION_ATTEMPTS)"
                )

                return extractOnce(
                    videoId = videoId,
                    quality = quality,
                    excludedUrls = excludedUrls,
                )
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: LinkageError) {
                throw IllegalStateException(
                    "O extrator local não pôde iniciar: ${error.message}",
                    error,
                )
            } catch (error: Throwable) {
                permanentReason(error)?.let { reason ->
                    throw IllegalStateException(reason, error)
                }

                lastFailure = error
                println(
                    "[NewPipe] tentativa ${attempt + 1} falhou -> " +
                        (error.message ?: error::class.simpleName)
                )
            }
        }

        throw StreamResolutionException(
            "O extrator local do YouTube não conseguiu obter o áudio: " +
                (lastFailure?.message ?: "falha desconhecida")
        )
    }

    private suspend fun extractOnce(
        videoId: String,
        quality: PipedStreamResolver.QualityPreference,
        excludedUrls: Set<String>,
    ): PipedStreamResolver.Resolution =
        NewPipeRuntime.extractionMutex.withLock {
            withContext(Dispatchers.IO) {
                val started = System.nanoTime()

                val extractor = ServiceList.YouTube.getStreamExtractor(
                    "https://www.youtube.com/watch?v=$videoId"
                )

                extractor.fetchPage()

                val streams = extractor.audioStreams
                    .asSequence()
                    .filter { stream ->
                        !stream.content.isNullOrBlank() &&
                            stream.deliveryMethod == DeliveryMethod.PROGRESSIVE_HTTP
                    }
                    .mapNotNull { stream ->
                        stream.toModel()
                    }
                    .filterNot { stream ->
                        stream.url in excludedUrls
                    }
                    .distinctBy { stream ->
                        stream.url
                    }
                    .toList()
                    .let { candidates ->
                        rank(candidates, quality)
                    }

                if (streams.isEmpty()) {
                    error("NewPipe não retornou audioStreams progressivos")
                }

                val verified = mutableListOf<PipedStreamResolver.AudioStream>()

                for (stream in streams) {
                    val verdict = probe(stream)

                    println(
                        "[NewPipe] PROBE ${stream.mimeType ?: "audio"} " +
                            "${stream.bitrate / 1000}kbps -> $verdict"
                    )

                    when (verdict) {
                        Probe.OK -> {
                            verified += stream
                            break
                        }
                        Probe.REFUSED -> Unit
                        Probe.UNREACHABLE -> Unit
                    }
                }

                if (verified.isEmpty()) {
                    error("NewPipe resolveu URLs, mas o googlevideo recusou todas")
                }

                val elapsed = (System.nanoTime() - started) / 1_000_000L
                println(
                    "[NewPipe] OK streams=${verified.size} em ${elapsed}ms"
                )

                val durationMillis =
                    runCatching {
                        extractor.length
                    }
                        .getOrNull()
                        ?.takeIf { it > 0L }
                        ?.times(1000L)

                PipedStreamResolver.Resolution(
                    videoId = videoId,
                    streams = verified,
                    durationMillis = durationMillis,
                    instance = "newpipe-local",
                )
            }
        }

    private fun AudioStream.toModel(): PipedStreamResolver.AudioStream? {
        val cleanUrl = content?.trim().orEmpty()

        if (
            !cleanUrl.startsWith("https://") &&
            !cleanUrl.startsWith("http://")
        ) {
            return null
        }

        val headers = headersForUrl(cleanUrl)
        val mime = format?.mimeType
        val formatName = format?.name

        val codec = when (format) {
            MediaFormat.M4A -> "mp4a"
            MediaFormat.WEBMA_OPUS -> "opus"
            else -> null
        }

        return PipedStreamResolver.AudioStream(
            url = cleanUrl,
            mimeType = mime,
            codec = codec,
            format = formatName,
            bitrate = averageBitrate * 1000,
            quality = null,
            instance = "newpipe-local",
            userAgent = headers.userAgent,
            referrer = headers.referrer,
            origin = headers.origin,
        )
    }

    private fun probe(
        stream: PipedStreamResolver.AudioStream,
    ): Probe {
        return try {
            val builder = HttpRequest.newBuilder(URI.create(stream.url))
                .timeout(Duration.ofSeconds(8))
                .GET()
                .header("Range", "bytes=0-${PROBE_RANGE_BYTES - 1}")
                .header(
                    "User-Agent",
                    stream.userAgent ?: NewPipeRuntime.CHROME_USER_AGENT,
                )

            stream.origin?.let { origin ->
                builder.header("Origin", origin)
            }

            stream.referrer?.let { referrer ->
                builder.header("Referer", referrer)
            }

            val response = probeClient.send(
                builder.build(),
                HttpResponse.BodyHandlers.ofInputStream(),
            )

            response.body().use { input ->
                val code = response.statusCode()

                if (code in REFUSAL_CODES) {
                    return Probe.REFUSED
                }

                if (code !in 200..299) {
                    return Probe.UNREACHABLE
                }

                val contentType = response.headers()
                    .firstValue("Content-Type")
                    .orElse("")
                    .lowercase(Locale.ROOT)

                if (!contentType.startsWith("audio/")) {
                    return Probe.REFUSED
                }

                var remaining = PROBE_READ_BYTES
                val buffer = ByteArray(8 * 1024)

                while (remaining > 0) {
                    val read = input.read(
                        buffer,
                        0,
                        minOf(buffer.size, remaining),
                    )

                    if (read < 0) break
                    remaining -= read
                }

                if (remaining <= 0) {
                    Probe.OK
                } else {
                    Probe.UNREACHABLE
                }
            }
        } catch (_: InterruptedException) {
            Thread.currentThread().interrupt()
            Probe.UNREACHABLE
        } catch (_: Throwable) {
            Probe.UNREACHABLE
        }
    }

    private fun headersForUrl(url: String): MediaHeaders {
        val query = runCatching { URI.create(url).rawQuery.orEmpty() }
            .getOrDefault("")

        fun parameter(name: String): String? = query
            .split('&')
            .firstOrNull { part -> part.substringBefore('=') == name }
            ?.substringAfter('=', "")
            ?.takeIf { it.isNotBlank() }

        val client = parameter("c")
            ?.uppercase(Locale.ROOT)
            .orEmpty()

        val version = parameter("cver")

        return when {
            client == "ANDROID_MUSIC" ->
                MediaHeaders(
                    userAgent =
                        "com.google.android.apps.youtube.music/8.39.42 " +
                            "(Linux; U; Android 15; en_US; Pixel 9 Pro; " +
                            "Build/AP4A.250205.002) gzip",
                )

            client == "ANDROID_VR" ->
                MediaHeaders(
                    userAgent =
                        if (version == "1.43.32") {
                            "com.google.android.apps.youtube.vr.oculus/1.43.32 " +
                                "(Linux; U; Android 12; en_US; Quest 3; " +
                                "Build/SQ3A.220605.009.A1; Cronet/107.0.5284.2)"
                        } else {
                            "com.google.android.apps.youtube.vr.oculus/1.65.10 " +
                                "(Linux; U; Android 12L; eureka-user " +
                                "Build/SQ3A.220605.009.A1) gzip"
                        },
                )

            client.startsWith("ANDROID") ->
                MediaHeaders(
                    userAgent =
                        "com.google.android.youtube/21.26.364 " +
                            "(Linux; U; Android 15; en_US; Pixel 9 Pro; " +
                            "Build/AP4A.250205.002; Cronet/132.0.6834.79) gzip",
                )

            client.startsWith("IOS") ->
                MediaHeaders(
                    userAgent =
                        "com.google.ios.youtube/21.29.1 " +
                            "(iPhone16,2; U; CPU iOS 18_5 like Mac OS X;)",
                )

            client.startsWith("TVHTML5") ->
                MediaHeaders(
                    userAgent =
                        "Mozilla/5.0(SMART-TV; Linux; Tizen 4.0.0.2) " +
                            "AppleWebkit/605.1.15 (KHTML, like Gecko) " +
                            "SamsungBrowser/9.2 TV Safari/605.1.15",
                    origin = "https://www.youtube.com",
                    referrer = "https://www.youtube.com/",
                )

            client == "WEB_REMIX" ->
                MediaHeaders(
                    userAgent = NewPipeRuntime.CHROME_USER_AGENT,
                    origin = "https://music.youtube.com",
                    referrer = "https://music.youtube.com/",
                )

            else ->
                MediaHeaders(
                    userAgent = NewPipeRuntime.CHROME_USER_AGENT,
                    origin = "https://www.youtube.com",
                    referrer = "https://www.youtube.com/",
                )
        }
    }

    private fun rank(
        streams: List<PipedStreamResolver.AudioStream>,
        quality: PipedStreamResolver.QualityPreference,
    ): List<PipedStreamResolver.AudioStream> {
        fun codecScore(stream: PipedStreamResolver.AudioStream): Int {
            val mime = stream.mimeType.orEmpty().lowercase(Locale.ROOT)
            val codec = stream.codec.orEmpty().lowercase(Locale.ROOT)
            val format = stream.format.orEmpty().lowercase(Locale.ROOT)

            return when {
                "audio/mp4" in mime || "mp4a" in codec || "m4a" in format -> 40
                "audio/webm" in mime || "opus" in codec || "webm" in format -> 30
                mime.startsWith("audio/") -> 20
                else -> 10
            }
        }

        fun knownBitrate(stream: PipedStreamResolver.AudioStream): Int =
            stream.bitrate.takeIf { it > 0 } ?: 1

        return when (quality) {
            PipedStreamResolver.QualityPreference.HIGH ->
                streams.sortedWith(
                    compareByDescending<PipedStreamResolver.AudioStream> {
                        knownBitrate(it)
                    }.thenByDescending(::codecScore)
                )

            PipedStreamResolver.QualityPreference.BALANCED ->
                streams.sortedWith(
                    compareBy<PipedStreamResolver.AudioStream> {
                        if (it.bitrate > 0) {
                            abs(it.bitrate - 128_000)
                        } else {
                            Int.MAX_VALUE
                        }
                    }.thenByDescending(::codecScore)
                )

            PipedStreamResolver.QualityPreference.DATA_SAVER ->
                streams.sortedWith(
                    compareBy<PipedStreamResolver.AudioStream> {
                        if (it.bitrate > 0) it.bitrate else Int.MAX_VALUE
                    }.thenByDescending(::codecScore)
                )

            PipedStreamResolver.QualityPreference.AUTO ->
                streams.sortedWith(
                    compareByDescending<PipedStreamResolver.AudioStream>(::codecScore)
                        .thenByDescending { knownBitrate(it) }
                )
        }
    }

    private fun permanentReason(error: Throwable): String? =
        when (error) {
            is AgeRestrictedContentException ->
                "Esta faixa é restrita por idade no YouTube."

            is GeographicRestrictionException,
            is UnsupportedContentInCountryException,
            -> "Esta faixa não está disponível no seu país."

            is PaidContentException ->
                "Esta faixa é conteúdo pago."

            is YoutubeMusicPremiumContentException ->
                "Esta faixa exige YouTube Music Premium."

            is PrivateContentException ->
                "Esta faixa é privada."

            is AccountTerminatedException ->
                "O canal desta faixa foi encerrado."

            is SoundCloudGoPlusContentException ->
                "Esta faixa exige SoundCloud Go+."

            else -> error.cause
                ?.takeIf { cause -> cause !== error }
                ?.let(::permanentReason)
        }

    fun close() = Unit

    private companion object {
        val VIDEO_ID = Regex("^[A-Za-z0-9_-]{11}$")

        const val EXTRACTION_ATTEMPTS = 2
        const val EXTRACTION_RETRY_MS = 700L

        val REFUSAL_CODES = setOf(403, 404, 410)
        const val PROBE_RANGE_BYTES = 2L * 1024 * 1024
        const val PROBE_READ_BYTES = 16 * 1024
    }
}
