package com.primomusic.desktop.stream

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.isSuccess
import kotlinx.coroutines.delay
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.abs

/**
 * Resolves short-lived audio URLs through Piped without downloading media.
 *
 * The returned URL is consumed immediately by the desktop audio engine.
 * Public instances are fallbacks only; they are not an SLA-backed service.
 */
class PipedStreamResolver(
    private val client: HttpClient = defaultClient(),
    private val instances: List<String> = DEFAULT_INSTANCES,
) {
    enum class QualityPreference { AUTO, HIGH, BALANCED, DATA_SAVER }

    data class AudioStream(
        val url: String,
        val mimeType: String?,
        val codec: String?,
        val format: String?,
        val bitrate: Int,
        val quality: String?,
        val instance: String,
        val userAgent: String? = null,
        val referrer: String? = null,
        val origin: String? = null,
    )

    data class Resolution(
        val videoId: String,
        val streams: List<AudioStream>,
        val durationMillis: Long?,
        val instance: String,
    )

    private val unavailableUntil = ConcurrentHashMap<String, Long>()

    @Volatile
    private var lastHealthyInstance: String? = null

    suspend fun resolve(
        videoId: String,
        quality: QualityPreference,
        excludedUrls: Set<String> = emptySet(),
    ): Resolution {
        require(VIDEO_ID.matches(videoId)) { "videoId inválido para streaming." }

        val now = System.currentTimeMillis()
        val ordered = buildList {
            lastHealthyInstance?.takeIf { it in instances }?.let(::add)
            instances.distinct().forEach { if (it !in this) add(it) }
        }
        val providers = ordered.filter { (unavailableUntil[it] ?: 0L) <= now }.ifEmpty { ordered }
        val failures = mutableListOf<String>()

        for (base in providers) {
            try {
                println("[Piped] Tentando ${hostLabel(base)}")
                val resolution = requestInstance(base.trimEnd('/'), videoId, quality, excludedUrls)
                lastHealthyInstance = base
                unavailableUntil.remove(base)
                println("[Piped] OK ${hostLabel(base)} streams=${resolution.streams.size}")
                return resolution
            } catch (error: Throwable) {
                val message = error.message
                    ?.lineSequence()
                    ?.firstOrNull()
                    ?.trim()
                    ?.take(180)
                    ?.takeIf { it.isNotBlank() }
                    ?: error::class.simpleName
                    ?: "erro desconhecido"

                println("[Piped] FALHOU ${hostLabel(base)} -> $message")
                failures += "${hostLabel(base)}: $message"

                val cooldown = when {
                    "HTTP 525" in message || "HTTP 526" in message -> 5 * 60_000L
                    "HTTP 502" in message || "HTTP 503" in message || "HTTP 504" in message -> 2 * 60_000L
                    "timeout" in message.lowercase() || "timed out" in message.lowercase() -> 60_000L
                    else -> 30_000L
                }
                unavailableUntil[base] = System.currentTimeMillis() + cooldown
                delay(80)
            }
        }

        println("[Piped] Todas as instâncias falharam: ${failures.joinToString(" | ")}")
        throw StreamResolutionException(
            "Não foi possível conectar a uma fonte Piped no momento."
        )
    }

    private suspend fun requestInstance(
        base: String,
        videoId: String,
        quality: QualityPreference,
        excludedUrls: Set<String>,
    ): Resolution {
        val response = client.get("$base/streams/$videoId") {
            header(HttpHeaders.Accept, "application/json")
            header(HttpHeaders.UserAgent, USER_AGENT)
        }
        if (!response.status.isSuccess()) {
            error("HTTP ${response.status.value}")
        }

        val payload = JSON.decodeFromString<PipedResponse>(response.bodyAsText())
        val ranked = payload.audioStreams
            .asSequence()
            .mapNotNull { dto -> dto.toModel(base) }
            .filterNot { it.url in excludedUrls }
            .distinctBy { it.url }
            .toList()
            .let { rank(it, quality) }

        if (ranked.isEmpty()) error("resposta sem audioStreams compatíveis")

        return Resolution(
            videoId = videoId,
            streams = ranked,
            durationMillis = payload.duration?.takeIf { it > 0 }?.times(1000L),
            instance = base,
        )
    }

    private fun PipedAudioStream.toModel(instance: String): AudioStream? {
        val cleanUrl = url.trim()
        if (!cleanUrl.startsWith("https://") && !cleanUrl.startsWith("http://")) return null
        return AudioStream(
            url = cleanUrl,
            mimeType = mimeType,
            codec = codec,
            format = format,
            bitrate = bitrate ?: 0,
            quality = quality,
            instance = instance,
        )
    }

    private fun rank(streams: List<AudioStream>, quality: QualityPreference): List<AudioStream> {
        fun codecScore(s: AudioStream): Int {
            val mime = s.mimeType.orEmpty().lowercase()
            val codec = s.codec.orEmpty().lowercase()
            val format = s.format.orEmpty().lowercase()
            return when {
                "audio/mp4" in mime || "mp4a" in codec || "m4a" in format -> 40
                "audio/webm" in mime || "opus" in codec || "webm" in format -> 30
                mime.startsWith("audio/") -> 20
                else -> 10
            }
        }

        fun knownBitrate(s: AudioStream): Int = s.bitrate.takeIf { it > 0 } ?: 1
        return when (quality) {
            QualityPreference.HIGH -> streams.sortedWith(
                compareByDescending<AudioStream> { knownBitrate(it) }.thenByDescending(::codecScore),
            )
            QualityPreference.BALANCED -> streams.sortedWith(
                compareBy<AudioStream> { if (it.bitrate > 0) abs(it.bitrate - 128_000) else Int.MAX_VALUE }
                    .thenByDescending(::codecScore),
            )
            QualityPreference.DATA_SAVER -> streams.sortedWith(
                compareBy<AudioStream> { if (it.bitrate > 0) it.bitrate else Int.MAX_VALUE }
                    .thenByDescending(::codecScore),
            )
            QualityPreference.AUTO -> streams.sortedWith(
                compareByDescending<AudioStream>(::codecScore).thenByDescending { knownBitrate(it) },
            )
        }
    }

    fun close() = client.close()

    companion object {
        /**
         * Current public instances listed by TeamPiped with CDN support.
         * Public infrastructure can still go offline, hence failover is mandatory.
         */
        val DEFAULT_INSTANCES = listOf(
            "https://pipedapi.leptons.xyz",
            "https://pipedapi.nosebs.ru",
            "https://pipedapi.kavin.rocks",
        )

        private const val USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/153.0.0.0 Safari/537.36"
        private val VIDEO_ID = Regex("^[A-Za-z0-9_-]{11}$")
        private val JSON = Json {
            ignoreUnknownKeys = true
            isLenient = true
        }

        private fun defaultClient() = HttpClient(OkHttp) {
            expectSuccess = false
            install(HttpTimeout) {
                connectTimeoutMillis = 2_800
                requestTimeoutMillis = 5_500
                socketTimeoutMillis = 5_000
            }
        }

        private fun hostLabel(url: String) = url
            .removePrefix("https://")
            .removePrefix("http://")
            .substringBefore('/')
    }
}

class StreamResolutionException(message: String) : IllegalStateException(message)

@Serializable
private data class PipedResponse(
    val audioStreams: List<PipedAudioStream> = emptyList(),
    val duration: Long? = null,
)

@Serializable
private data class PipedAudioStream(
    val url: String = "",
    val format: String? = null,
    val quality: String? = null,
    val bitrate: Int? = null,
    val codec: String? = null,
    @SerialName("mimeType") val mimeType: String? = null,
)
