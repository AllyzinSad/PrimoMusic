package com.primomusic.desktop.stream

import com.primomusic.core.music.YouTubeMusicSearchClient
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import java.net.URI
import java.net.http.HttpClient as JavaHttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.security.MessageDigest
import java.time.Duration
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.abs

/**
 * Primary Primo Music desktop stream resolver.
 *
 * This is a small desktop port of the direct Innertube strategy used by the
 * Android BitChord source included in this project:
 *
 * videoId -> youtubei/v1/player -> adaptiveFormats -> direct googlevideo URL
 *
 * It intentionally tries only client identities that normally return plain
 * `url` fields. Cipher/signature solving is left out of the desktop hot path.
 *
 * NewPipeExtractor runs locally as the secondary fallback in DesktopAudioPlayer.
 */
class DirectInnertubeStreamResolver(
    private val client: HttpClient = defaultClient(),
) {
    private data class ClientIdentity(
        val clientName: String,
        val clientVersion: String,
        val clientId: String,
        val userAgent: String,
        val osName: String? = null,
        val osVersion: String? = null,
        val deviceMake: String? = null,
        val deviceModel: String? = null,
        val androidSdkVersion: Int? = null,
        val origin: String? = null,
    ) {
        val key: String
            get() = "$clientName:$clientVersion"

        val referrer: String?
            get() = origin?.let { "$it/" }
    }

    private data class Candidate(
        val stream: PipedStreamResolver.AudioStream,
        val client: ClientIdentity,
    )

    private enum class Probe { OK, REFUSED, UNREACHABLE }

    private val mediaProbeClient: JavaHttpClient = JavaHttpClient.newBuilder()
        .followRedirects(JavaHttpClient.Redirect.NORMAL)
        .connectTimeout(Duration.ofSeconds(5))
        .build()

    private val unavailableUntil = ConcurrentHashMap<String, Long>()

    @Volatile
    private var preferredClientName: String? = null

    @Volatile
    private var visitorData: String? = null

    suspend fun resolve(
        videoId: String,
        quality: PipedStreamResolver.QualityPreference,
        excludedUrls: Set<String> = emptySet(),
    ): PipedStreamResolver.Resolution {
        require(VIDEO_ID.matches(videoId)) { "videoId inválido para streaming." }

        syncSessionVisitor()
        ensureVisitorData()

        val orderedClients = buildList {
            val preferred = CLIENTS.firstOrNull { it.key == preferredClientName }
            preferred?.let(::add)
            CLIENTS.forEach { if (it !in this) add(it) }
        }

        val now = System.currentTimeMillis()
        val usableClients = orderedClients
            .filter { (unavailableUntil[it.key] ?: 0L) <= now }
            .ifEmpty { orderedClients }

        val failures = mutableListOf<String>()

        for (identity in usableClients) {
            try {
                println("[Innertube] Tentando ${identity.clientName}")

                var response = playerRequest(
                    videoId = videoId,
                    identity = identity,
                    authenticated = false,
                )

                var status = playabilityStatus(response)
                var reason = playabilityReason(response)

                if (status != null && status != "OK") {
                    val session = YouTubeMusicSearchClient.currentAuthSession()

                    if (session != null && (isAgeGate(reason) || looksLikeBotCheck(reason))) {
                        println("[Innertube] ${identity.clientName} pediu sessão; tentando autenticado")
                        response = playerRequest(
                            videoId = videoId,
                            identity = identity,
                            authenticated = true,
                        )
                        status = playabilityStatus(response)
                        reason = playabilityReason(response)
                    } else if (looksLikeBotCheck(reason)) {
                        // A burned visitor id can cause a session-wide bot verdict.
                        ensureVisitorData(refresh = true)
                        response = playerRequest(
                            videoId = videoId,
                            identity = identity,
                            authenticated = false,
                        )
                        status = playabilityStatus(response)
                        reason = playabilityReason(response)
                    }
                }

                if (status != null && status != "OK") {
                    error(reason?.takeIf { it.isNotBlank() } ?: status)
                }

                val rawCandidates = audioCandidates(
                    response = response,
                    identity = identity,
                    excludedUrls = excludedUrls,
                )

                if (rawCandidates.isEmpty()) {
                    error("resposta sem URLs diretas de áudio")
                }

                // BitChord's Android resolver never trusts the raw `n` value
                // carried by googlevideo URLs. YouTube throttles/refuses that
                // URL until the value is transformed with the current player
                // JavaScript. NewPipe's YoutubeJavaScriptPlayerManager does
                // exactly that locally, so the desktop port now performs the
                // same step before the media probe.
                val candidates = mutableListOf<Candidate>()
                for (candidate in rawCandidates) {
                    val originalUrl = candidate.stream.url
                    val unlockedUrl = runCatching {
                        NewPipeRuntime
                            .deobfuscateN(videoId, originalUrl)
                            .let { patchClientVersion(it, identity.clientVersion) }
                    }.onFailure { error ->
                        println(
                            "[Innertube] n-transform falhou em ${identity.clientName}: " +
                                (error.message ?: error::class.simpleName)
                        )
                    }.getOrDefault(originalUrl)

                    if (unlockedUrl != originalUrl) {
                        println("[Innertube] n-transform ${identity.clientName} -> OK")
                    }

                    if (unlockedUrl !in excludedUrls) {
                        candidates += candidate.copy(
                            stream = candidate.stream.copy(url = unlockedUrl),
                        )
                    }
                }

                if (candidates.isEmpty()) {
                    error("nenhuma URL de áudio restou após o desbloqueio")
                }

                val ranked = rank(candidates.map { it.stream }, quality)

                // The Android BitChord resolver does not trust a URL merely
                // because youtubei returned it. googlevideo may still reject
                // the byte request for this client/IP. Probe the exact media
                // request with the same User-Agent/Origin/Referer before
                // declaring the client healthy.
                val verified = mutableListOf<PipedStreamResolver.AudioStream>()
                var lastProbe: Probe? = null

                for (stream in ranked) {
                    val verdict = probe(stream, identity)
                    lastProbe = verdict
                    println(
                        "[Innertube] PROBE ${identity.clientName} " +
                            "${stream.mimeType ?: "audio"} ${stream.bitrate / 1000}kbps -> $verdict"
                    )

                    when (verdict) {
                        Probe.OK -> {
                            verified += stream
                            // One proven URL is enough. A fresh resolve is safer
                            // than keeping stale backup URLs around.
                            break
                        }
                        Probe.REFUSED -> break
                        Probe.UNREACHABLE -> Unit
                    }
                }

                if (verified.isEmpty()) {
                    error(
                        when (lastProbe) {
                            Probe.REFUSED -> "media URL recusada pelo googlevideo"
                            Probe.UNREACHABLE -> "media URL não respondeu ao probe"
                            else -> "nenhuma media URL pôde ser validada"
                        }
                    )
                }

                preferredClientName = identity.key
                unavailableUntil.remove(identity.key)

                val durationMillis = response["videoDetails"]
                    ?.asObject()
                    ?.get("lengthSeconds")
                    .asString()
                    ?.toLongOrNull()
                    ?.takeIf { it > 0L }
                    ?.times(1000L)

                println(
                    "[Innertube] OK ${identity.clientName} " +
                        "streams=${verified.size}"
                )

                return PipedStreamResolver.Resolution(
                    videoId = videoId,
                    streams = verified,
                    durationMillis = durationMillis,
                    instance = "youtubei:${identity.clientName}",
                )
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Throwable) {
                val message = error.message
                    ?.lineSequence()
                    ?.firstOrNull()
                    ?.trim()
                    ?.take(180)
                    ?.takeIf { it.isNotBlank() }
                    ?: error::class.simpleName
                    ?: "erro desconhecido"

                println("[Innertube] FALHOU ${identity.clientName} -> $message")
                failures += "${identity.clientName}: $message"

                val cooldown = when {
                    "bot" in message.lowercase() ||
                        "sign in" in message.lowercase() ||
                        "login" in message.lowercase() ->
                        60_000L

                    "HTTP 429" in message ->
                        2 * 60_000L

                    "HTTP 400" in message ||
                        "HTTP 401" in message ||
                        "HTTP 403" in message ->
                        45_000L

                    "timeout" in message.lowercase() ||
                        "timed out" in message.lowercase() ->
                        30_000L

                    else ->
                        20_000L
                }

                unavailableUntil[identity.key] =
                    System.currentTimeMillis() + cooldown

                delay(60)
            }
        }

        println(
            "[Innertube] Todos os clientes falharam: " +
                failures.joinToString(" | ")
        )

        throw StreamResolutionException(
            "O YouTube não forneceu um stream direto compatível agora."
        )
    }

    private suspend fun playerRequest(
        videoId: String,
        identity: ClientIdentity,
        authenticated: Boolean,
    ): JsonObject {
        val session = YouTubeMusicSearchClient.currentAuthSession()
        val activeVisitor = visitorData ?: session?.visitorData

        val response = client.post("$YOUTUBE_API/player") {
            contentType(ContentType.Application.Json)

            header(HttpHeaders.UserAgent, identity.userAgent)
            header("X-YouTube-Client-Name", identity.clientId)
            header("X-YouTube-Client-Version", identity.clientVersion)

            identity.origin?.let { header("Origin", it) }
            identity.referrer?.let { header("Referer", it) }
            activeVisitor?.let { header("X-Goog-Visitor-Id", it) }

            if (authenticated && session != null) {
                header("Cookie", session.cookie)
                header(
                    "X-Goog-AuthUser",
                    session.authUser?.takeIf { it.isNotBlank() } ?: "0",
                )
                session.pageId
                    ?.takeIf { it.isNotBlank() }
                    ?.let { header("X-Goog-PageId", it) }

                sapisidFrom(session.cookie)?.let { sapisid ->
                    header(
                        "Authorization",
                        sapisidHash(
                            sapisid = sapisid,
                            origin = identity.origin ?: YOUTUBE_ORIGIN,
                        ),
                    )
                }
            }

            setBody(
                buildJsonObject {
                    putJsonObject("context") {
                        putJsonObject("client") {
                            put("clientName", identity.clientName)
                            put("clientVersion", identity.clientVersion)
                            identity.osName?.let { put("osName", it) }
                            identity.osVersion?.let { put("osVersion", it) }
                            identity.deviceMake?.let { put("deviceMake", it) }
                            identity.deviceModel?.let { put("deviceModel", it) }
                            identity.androidSdkVersion?.let {
                                put("androidSdkVersion", it)
                            }
                            put("hl", language())
                            put("gl", "BR")
                            activeVisitor?.let { put("visitorData", it) }
                        }

                        if (authenticated) {
                            session?.dataSyncId
                                ?.substringBefore("||")
                                ?.takeIf { it.isNotBlank() }
                                ?.let { dataSyncId ->
                                    putJsonObject("user") {
                                        put("onBehalfOfUser", dataSyncId)
                                    }
                                }
                        }
                    }

                    put("videoId", videoId)
                    put("contentCheckOk", true)
                    put("racyCheckOk", true)
                }.toString()
            )
        }

        if (!response.status.isSuccess()) {
            error("HTTP ${response.status.value}")
        }

        return JSON
            .parseToJsonElement(response.bodyAsText())
            .jsonObject
    }

    private fun audioCandidates(
        response: JsonObject,
        identity: ClientIdentity,
        excludedUrls: Set<String>,
    ): List<Candidate> {
        val formats = response["streamingData"]
            ?.asObject()
            ?.get("adaptiveFormats")
            ?.asArray()
            .orEmpty()

        return formats.mapNotNull { element ->
            val obj = element.asObject() ?: return@mapNotNull null

            val mimeType = obj["mimeType"].asString().orEmpty()
            if (!mimeType.startsWith("audio/")) return@mapNotNull null

            val url = obj["url"].asString()?.trim().orEmpty()
            if (!url.startsWith("https://") && !url.startsWith("http://")) {
                // Deliberately skip signatureCipher/cipher here. The Android
                // project has a JS signature solver, but the desktop fast path
                // only consumes unciphered clients.
                return@mapNotNull null
            }

            if (url in excludedUrls) return@mapNotNull null

            val bitrate = obj["bitrate"]
                .asString()
                ?.toLongOrNull()
                ?.coerceAtMost(Int.MAX_VALUE.toLong())
                ?.toInt()
                ?: 0

            val codec = CODEC_REGEX
                .find(mimeType)
                ?.groupValues
                ?.getOrNull(1)

            Candidate(
                stream = PipedStreamResolver.AudioStream(
                    url = url,
                    mimeType = mimeType.substringBefore(';').trim(),
                    codec = codec,
                    format = when {
                        "mp4" in mimeType.lowercase(Locale.ROOT) -> "m4a"
                        "webm" in mimeType.lowercase(Locale.ROOT) -> "webm"
                        else -> null
                    },
                    bitrate = bitrate,
                    quality = obj["audioQuality"].asString(),
                    instance = "youtubei:${identity.clientName}",
                    userAgent = identity.userAgent,
                    referrer = identity.referrer,
                    origin = identity.origin,
                ),
                client = identity,
            )
        }.distinctBy { it.stream.url }
    }


    /** Keep googlevideo's cver aligned with the identity that minted the URL. */
    private fun patchClientVersion(
        url: String,
        clientVersion: String,
    ): String =
        if ("cver=" in url) {
            url.replace(
                Regex("cver=[^&]+"),
                "cver=$clientVersion",
            )
        } else {
            url
        }

    private suspend fun probe(
        stream: PipedStreamResolver.AudioStream,
        identity: ClientIdentity,
    ): Probe = withContext(Dispatchers.IO) {
        try {
            val builder = HttpRequest.newBuilder(URI.create(stream.url))
                .timeout(Duration.ofSeconds(8))
                .GET()
                .header("Range", "bytes=0-${PROBE_RANGE_BYTES - 1}")
                .header("User-Agent", identity.userAgent)

            identity.origin?.let { builder.header("Origin", it) }
            identity.referrer?.let { builder.header("Referer", it) }

            val response = mediaProbeClient.send(
                builder.build(),
                HttpResponse.BodyHandlers.ofInputStream(),
            )

            response.body().use { input ->
                val code = response.statusCode()
                if (code in REFUSAL_CODES) return@withContext Probe.REFUSED
                if (code !in 200..299) return@withContext Probe.UNREACHABLE

                val contentType = response.headers()
                    .firstValue("Content-Type")
                    .orElse("")
                    .lowercase(Locale.ROOT)

                if (!contentType.startsWith("audio/")) {
                    return@withContext Probe.REFUSED
                }

                var remaining = PROBE_READ_BYTES
                val buffer = ByteArray(8 * 1024)
                while (remaining > 0) {
                    val read = input.read(buffer, 0, minOf(buffer.size, remaining))
                    if (read < 0) break
                    remaining -= read
                }

                if (remaining <= 0) Probe.OK else Probe.UNREACHABLE
            }
        } catch (_: InterruptedException) {
            Thread.currentThread().interrupt()
            Probe.UNREACHABLE
        } catch (_: Throwable) {
            Probe.UNREACHABLE
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

    private fun playabilityStatus(response: JsonObject): String? =
        response["playabilityStatus"]
            ?.asObject()
            ?.get("status")
            .asString()

    private fun playabilityReason(response: JsonObject): String? {
        val status = response["playabilityStatus"]?.asObject() ?: return null
        status["reason"].asString()?.let { return it }

        var found: String? = null

        fun walk(element: JsonElement) {
            if (found != null) return

            when (element) {
                is JsonObject -> {
                    element["text"].asString()
                        ?.takeIf { it.isNotBlank() }
                        ?.let {
                            found = it
                            return
                        }

                    element.values.forEach(::walk)
                }

                is JsonArray ->
                    element.forEach(::walk)

                else ->
                    Unit
            }
        }

        walk(status)
        return found
    }

    private suspend fun ensureVisitorData(
        refresh: Boolean = false,
    ) {
        if (!refresh && !visitorData.isNullOrBlank()) return

        val sessionVisitor =
            YouTubeMusicSearchClient
                .currentAuthSession()
                ?.visitorData
                ?.takeIf { it.isNotBlank() }

        if (!refresh && sessionVisitor != null) {
            visitorData = sessionVisitor
            return
        }

        runCatching {
            val response = client.get(VISITOR_BOOTSTRAP) {
                header(HttpHeaders.UserAgent, WEB_USER_AGENT)
            }

            if (!response.status.isSuccess()) {
                error("HTTP ${response.status.value}")
            }

            val body = response.bodyAsText()

            VISITOR_DATA_REGEX
                .find(body)
                ?.value
        }
            .onSuccess { minted ->
                if (!minted.isNullOrBlank()) {
                    visitorData = minted
                }
            }
            .onFailure { error ->
                println(
                    "[Innertube] visitorData indisponível: " +
                        (error.message ?: error::class.simpleName)
                )
            }
    }

    private fun syncSessionVisitor() {
        YouTubeMusicSearchClient
            .currentAuthSession()
            ?.visitorData
            ?.takeIf { it.isNotBlank() }
            ?.let {
                visitorData = it
            }
    }

    fun close() {
        client.close()
    }

    private fun language(): String =
        Locale.getDefault()
            .language
            .takeIf { it.isNotBlank() }
            ?: "en"

    private fun isAgeGate(reason: String?): Boolean {
        val value = reason.orEmpty()
        return value.contains("confirm your age", ignoreCase = true) ||
            value.contains("age-restricted", ignoreCase = true) ||
            value.contains("age restricted", ignoreCase = true) ||
            value.contains("inappropriate for some users", ignoreCase = true)
    }

    private fun looksLikeBotCheck(reason: String?): Boolean {
        val value = reason.orEmpty()
        return value.contains("bot", ignoreCase = true) ||
            value.contains("unusual traffic", ignoreCase = true) ||
            value.contains("sign in", ignoreCase = true) ||
            value.contains("login_required", ignoreCase = true) ||
            value.contains("page needs to be reloaded", ignoreCase = true)
    }

    private fun sapisidFrom(cookieHeader: String): String? {
        val jar = cookieHeader
            .split(';')
            .mapNotNull { entry ->
                val name = entry.substringBefore('=').trim()
                val value = entry.substringAfter('=', "").trim()
                if (name.isBlank() || value.isBlank()) null else name to value
            }
            .toMap()

        return SAPISID_NAMES.firstNotNullOfOrNull { jar[it] }
    }

    private fun sapisidHash(
        sapisid: String,
        origin: String,
    ): String {
        val timestamp = System.currentTimeMillis() / 1000
        val digest = MessageDigest
            .getInstance("SHA-1")
            .digest("$timestamp $sapisid $origin".toByteArray())
            .joinToString("") {
                "%02x".format(Locale.ROOT, it)
            }

        return "SAPISIDHASH ${timestamp}_$digest"
    }

    private fun JsonElement?.asObject(): JsonObject? =
        this as? JsonObject

    private fun JsonElement?.asArray(): JsonArray? =
        this as? JsonArray

    private fun JsonElement?.asString(): String? =
        (this as? JsonPrimitive)?.contentOrNull

    companion object {
        private const val YOUTUBE_ORIGIN = "https://www.youtube.com"
        private const val YOUTUBE_API = "$YOUTUBE_ORIGIN/youtubei/v1"

        private const val VISITOR_BOOTSTRAP =
            "https://www.youtube.com/sw.js_data"

        private const val WEB_USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
                "AppleWebKit/537.36 (KHTML, like Gecko) " +
                "Chrome/141.0.0.0 Safari/537.36"

        private val VIDEO_ID =
            Regex("^[A-Za-z0-9_-]{11}$")

        private val VISITOR_DATA_REGEX =
            Regex("""Cg[A-Za-z0-9_%-]{40,}""")

        private val CODEC_REGEX =
            Regex("""codecs="([^"]+)"""")

        private val SAPISID_NAMES =
            listOf(
                "SAPISID",
                "__Secure-3PAPISID",
                "__Secure-1PAPISID",
            )

        private val JSON =
            Json {
                ignoreUnknownKeys = true
                isLenient = true
            }

        private val CLIENTS =
            listOf(
                ClientIdentity(
                    clientName = "ANDROID_MUSIC",
                    clientVersion = "8.39.42",
                    clientId = "21",
                    userAgent =
                        "com.google.android.apps.youtube.music/8.39.42 " +
                            "(Linux; U; Android 15; en_US; Pixel 9 Pro; " +
                            "Build/AP4A.250205.002) gzip",
                    osName = "Android",
                    osVersion = "15",
                    deviceMake = "Google",
                    deviceModel = "Pixel 9 Pro",
                    androidSdkVersion = 35,
                ),
                ClientIdentity(
                    clientName = "TVHTML5",
                    clientVersion = "7.20260707.07.00",
                    clientId = "7",
                    userAgent =
                        "Mozilla/5.0(SMART-TV; Linux; Tizen 4.0.0.2) " +
                            "AppleWebkit/605.1.15 (KHTML, like Gecko) " +
                            "SamsungBrowser/9.2 TV Safari/605.1.15",
                    origin = YOUTUBE_ORIGIN,
                ),
                ClientIdentity(
                    clientName = "ANDROID_VR",
                    clientVersion = "1.65.10",
                    clientId = "28",
                    userAgent =
                        "com.google.android.apps.youtube.vr.oculus/1.65.10 " +
                            "(Linux; U; Android 12L; eureka-user " +
                            "Build/SQ3A.220605.009.A1) gzip",
                    osName = "Android",
                    osVersion = "12L",
                    deviceMake = "Oculus",
                    deviceModel = "Quest 3",
                    androidSdkVersion = 32,
                ),
                ClientIdentity(
                    clientName = "ANDROID_VR",
                    clientVersion = "1.43.32",
                    clientId = "28",
                    userAgent =
                        "com.google.android.apps.youtube.vr.oculus/1.43.32 " +
                            "(Linux; U; Android 12; en_US; Quest 3; " +
                            "Build/SQ3A.220605.009.A1; Cronet/107.0.5284.2)",
                    osName = "Android",
                    osVersion = "12",
                    deviceMake = "Oculus",
                    deviceModel = "Quest 3",
                    androidSdkVersion = 31,
                ),
                ClientIdentity(
                    clientName = "IOS",
                    clientVersion = "21.26.4",
                    clientId = "5",
                    userAgent =
                        "com.google.ios.youtube/21.26.4 " +
                            "(iPhone16,2; U; CPU iOS 18_3_2 like Mac OS X;)",
                    osName = "iPhone",
                    osVersion = "18.3.2.22D82",
                    deviceMake = "Apple",
                    deviceModel = "iPhone16,2",
                ),
                ClientIdentity(
                    clientName = "IOS",
                    clientVersion = "21.29.1",
                    clientId = "5",
                    userAgent =
                        "com.google.ios.youtube/21.29.1 " +
                            "(iPhone16,2; U; CPU iOS 18_5 like Mac OS X;)",
                    osName = "iPhone",
                    osVersion = "18.5.22F70",
                    deviceMake = "Apple",
                    deviceModel = "iPhone16,2",
                ),
            )

        private val REFUSAL_CODES = setOf(403, 404, 410)
        private const val PROBE_RANGE_BYTES = 2L * 1024 * 1024
        private const val PROBE_READ_BYTES = 16 * 1024

        private fun defaultClient() =
            HttpClient(OkHttp) {
                expectSuccess = false

                install(HttpTimeout) {
                    connectTimeoutMillis = 4_500
                    requestTimeoutMillis = 9_000
                    socketTimeoutMillis = 8_000
                }
            }
    }
}
