package com.primomusic.core.music

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import java.util.Locale
import java.security.MessageDigest

/**
 * Desktop search proof using the same YouTube Music WEB_REMIX / Innertube
 * approach as BitChord. No fake results are generated.
 */
object YouTubeMusicSearchClient {
    data class Track(
        val videoId: String,
        val title: String,
        val artist: String,
        val thumbnailUrl: String?,
        val durationText: String?,
    )

    private const val MUSIC_ORIGIN = "https://music.youtube.com"
    private const val MUSIC_BASE = "$MUSIC_ORIGIN/youtubei/v1"
    private const val CLIENT_NAME = "67"
    private const val FALLBACK_CLIENT_VERSION = "1.20250101.01.00"
    private const val USER_AGENT =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
            "(KHTML, like Gecko) Chrome/141.0.0.0 Safari/537.36"

    private val json = Json { ignoreUnknownKeys = true }

    private val client = HttpClient(OkHttp) {
        install(ContentNegotiation) { json(json) }
        install(HttpTimeout) {
            requestTimeoutMillis = 30_000
            connectTimeoutMillis = 15_000
            socketTimeoutMillis = 20_000
        }
        expectSuccess = true
    }


    data class AuthSession(
        val cookie: String,
        val authUser: String? = null,
        val pageId: String? = null,
        val dataSyncId: String? = null,
        val visitorData: String? = null,
        val clientVersion: String? = null,
    )

    data class Playlist(
        val playlistId: String,
        val title: String,
        val thumbnailUrl: String?,
        val subtitle: String?,
    )

    data class AccountProfile(
        val name: String,
        val subtitle: String?,
        val thumbnailUrl: String?,
    )

    @Volatile
    private var authSession: AuthSession? = null

    fun setAuthSession(session: AuthSession?) {
        authSession = session
        cachedClientVersion = session?.clientVersion
        cachedVisitorData = session?.visitorData
    }

    fun currentAuthSession(): AuthSession? = authSession

    fun isAuthenticated(): Boolean = authSession?.cookie?.let(::hasApiSid) == true

    @Volatile
    private var cachedClientVersion: String? = null

    @Volatile
    private var cachedVisitorData: String? = null

    suspend fun search(query: String): List<Track> {
        val normalized = query.trim()
        require(normalized.isNotBlank()) { "A pesquisa não pode estar vazia." }

        val version = ensureShellConfig()
        val language = normalizeLanguage(Locale.getDefault().language)

        val response = client.post("$MUSIC_BASE/search") {
            contentType(ContentType.Application.Json)
            parameter("prettyPrint", "false")
            parameter("hl", language)
            header("User-Agent", USER_AGENT)
            header("Accept-Language", if (language == "en") "en-US,en;q=0.9" else "$language,en-US;q=0.8,en;q=0.7")
            header("X-Origin", MUSIC_ORIGIN)
            header("Origin", MUSIC_ORIGIN)
            header("Referer", "$MUSIC_ORIGIN/")
            applyAuthHeaders()
            header("X-YouTube-Client-Name", CLIENT_NAME)
            header("X-YouTube-Client-Version", version)
            cachedVisitorData?.let { header("X-Goog-Visitor-Id", it) }
            setBody(
                buildJsonObject {
                    putJsonObject("context") {
                        putJsonObject("client") {
                            put("clientName", "WEB_REMIX")
                            put("clientVersion", version)
                            put("hl", language)
                            put("gl", "BR")
                            cachedVisitorData?.let { put("visitorData", it) }
                        }
                        putJsonObject("user") {
                            put("lockedSafetyMode", false)
                            authSession?.dataSyncId?.takeIf { it.isNotBlank() }?.let { put("onBehalfOfUser", it.substringBefore("||")) }
                        }
                        putJsonObject("request") { put("useSsl", true) }
                    }
                    put("query", normalized)
                },
            )
        }.body<JsonObject>()

        if (cachedVisitorData == null) {
            cachedVisitorData = response["responseContext"]?.asObject()
                ?.get("visitorData")?.asString()
        }

        return parseTracks(response)
            .distinctBy { it.videoId }
            .take(30)
    }



    suspend fun accountProfile(): AccountProfile? {
        require(isAuthenticated()) { "Conecte sua conta Google antes de carregar o perfil." }
        val root = accountPost("account/account_menu") { }
        var header: JsonObject? = null
        fun walk(element: JsonElement) {
            if (header != null) return
            when (element) {
                is JsonObject -> {
                    element["activeAccountHeaderRenderer"]?.asObject()?.let { header = it; return }
                    element.values.forEach(::walk)
                }
                is JsonArray -> element.forEach(::walk)
                else -> Unit
            }
        }
        walk(root)
        val h = header ?: return null
        fun textOf(key: String): String? {
            val obj = h[key]?.asObject() ?: return null
            obj["simpleText"]?.asString()?.takeIf { it.isNotBlank() }?.let { return it }
            val runs = obj["runs"]?.asArray().orEmpty()
            return runs.joinToString("") { it.asObject()?.get("text")?.asString().orEmpty() }.trim().takeIf { it.isNotBlank() }
        }
        val name = textOf("accountName") ?: return null
        val subtitle = textOf("email") ?: textOf("channelHandle")
        val thumbnail = h["accountPhoto"]?.asObject()?.get("thumbnails")?.asArray()
            ?.mapNotNull { it.asObject()?.get("url")?.asString() }
            ?.lastOrNull()?.let(::normalizeThumbnailUrl)
        return AccountProfile(name, subtitle, thumbnail)
    }

    suspend fun history(): List<Track> = browseTracks("FEmusic_history")

    suspend fun likedSongs(): List<Track> = browseTracks("FEmusic_liked_videos")

    suspend fun userPlaylists(): List<Playlist> {
        val root = browse("FEmusic_liked_playlists")
        val out = linkedMapOf<String, Playlist>()
        fun walk(element: JsonElement) {
            when (element) {
                is JsonObject -> {
                    val renderer = element["musicTwoRowItemRenderer"]?.asObject()
                        ?: element["musicResponsiveListItemRenderer"]?.asObject()
                    if (renderer != null) {
                        val browseId = findBrowseId(renderer)
                        if (!browseId.isNullOrBlank() && browseId.startsWith("VL")) {
                            val title = findFirstText(renderer)
                            if (!title.isNullOrBlank() && !title.equals("New playlist", true) && !title.equals("Nova playlist", true)) {
                                out[browseId] = Playlist(
                                    playlistId = browseId.removePrefix("VL"),
                                    title = title,
                                    thumbnailUrl = findThumbnail(renderer),
                                    subtitle = findSubtitle(renderer),
                                )
                            }
                        }
                    }
                    element.values.forEach(::walk)
                }
                is JsonArray -> element.forEach(::walk)
                else -> Unit
            }
        }
        walk(root)
        return out.values.toList()
    }

    suspend fun setLiked(videoId: String, liked: Boolean) {
        require(isAuthenticated()) { "Conecte sua conta Google antes de alterar Músicas que gostei." }
        accountPost(if (liked) "like/like" else "like/removelike") {
            putJsonObject("target") { put("videoId", videoId) }
        }
    }

    suspend fun createPlaylist(title: String): String {
        require(isAuthenticated()) { "Conecte sua conta Google antes de criar playlists." }
        val clean = title.trim()
        require(clean.isNotBlank()) { "Digite um nome para a playlist." }
        val response = accountPost("playlist/create") {
            put("title", clean)
            put("description", "")
            put("privacyStatus", "PRIVATE")
        }
        return findStringByKey(response, "playlistId") ?: error("A playlist foi criada, mas o YouTube Music não retornou o ID.")
    }


    suspend fun playlistTracks(playlistId: String): List<Track> {
        val id = playlistId.trim().removePrefix("VL")
        require(id.isNotBlank()) { "Playlist inválida." }
        return browseTracks("VL$id")
    }

    suspend fun addToPlaylist(playlistId: String, videoId: String) {
        require(isAuthenticated()) { "Conecte sua conta Google antes de editar playlists." }
        val id = playlistId.trim().removePrefix("VL")
        require(id.isNotBlank() && videoId.isNotBlank()) { "Playlist ou música inválida." }
        accountPost("playlist/edit") {
            put("playlistId", id)
            put("actions", buildJsonArray {
                add(buildJsonObject {
                    put("action", "ACTION_ADD_VIDEO")
                    put("addedVideoId", videoId)
                })
            })
        }
    }

    private suspend fun accountPost(endpoint: String, payload: kotlinx.serialization.json.JsonObjectBuilder.() -> Unit): JsonObject {
        val version = ensureShellConfig()
        val language = normalizeLanguage(Locale.getDefault().language)
        return client.post("$MUSIC_BASE/$endpoint") {
            contentType(ContentType.Application.Json)
            parameter("prettyPrint", "false")
            header("User-Agent", USER_AGENT)
            header("X-Origin", MUSIC_ORIGIN); header("Origin", MUSIC_ORIGIN); header("Referer", "$MUSIC_ORIGIN/")
            applyAuthHeaders()
            header("X-YouTube-Client-Name", CLIENT_NAME); header("X-YouTube-Client-Version", version)
            cachedVisitorData?.let { header("X-Goog-Visitor-Id", it) }
            setBody(buildJsonObject {
                putJsonObject("context") {
                    putJsonObject("client") {
                        put("clientName", "WEB_REMIX"); put("clientVersion", version); put("hl", language); put("gl", "BR")
                        cachedVisitorData?.let { put("visitorData", it) }
                    }
                    putJsonObject("user") {
                        put("lockedSafetyMode", false)
                        authSession?.dataSyncId?.takeIf { it.isNotBlank() }?.let { put("onBehalfOfUser", it.substringBefore("||")) }
                    }
                    putJsonObject("request") { put("useSsl", true) }
                }
                payload()
            })
        }.body()
    }

    private fun findStringByKey(root: JsonElement, key: String): String? {
        var found: String? = null
        fun walk(element: JsonElement) {
            if (found != null) return
            when (element) {
                is JsonObject -> {
                    element[key].asString()?.takeIf { it.isNotBlank() }?.let { found = it }
                    element.values.forEach(::walk)
                }
                is JsonArray -> element.forEach(::walk)
                else -> Unit
            }
        }
        walk(root); return found
    }

    private suspend fun browseTracks(browseId: String): List<Track> = parseTracks(browse(browseId))
        .distinctBy { it.videoId }
        .take(100)

    private suspend fun browse(browseId: String): JsonObject {
        val version = ensureShellConfig()
        val language = normalizeLanguage(Locale.getDefault().language)
        return client.post("$MUSIC_BASE/browse") {
            contentType(ContentType.Application.Json)
            parameter("prettyPrint", "false")
            parameter("hl", language)
            header("User-Agent", USER_AGENT)
            header("Accept-Language", if (language == "en") "en-US,en;q=0.9" else "$language,en-US;q=0.8,en;q=0.7")
            header("X-Origin", MUSIC_ORIGIN)
            header("Origin", MUSIC_ORIGIN)
            header("Referer", "$MUSIC_ORIGIN/")
            applyAuthHeaders()
            header("X-YouTube-Client-Name", CLIENT_NAME)
            header("X-YouTube-Client-Version", version)
            cachedVisitorData?.let { header("X-Goog-Visitor-Id", it) }
            setBody(buildRequestContext(version, language, browseId))
        }.body()
    }

    private fun buildRequestContext(version: String, language: String, browseId: String? = null): JsonObject =
        buildJsonObject {
            putJsonObject("context") {
                putJsonObject("client") {
                    put("clientName", "WEB_REMIX")
                    put("clientVersion", version)
                    put("hl", language)
                    put("gl", "BR")
                    cachedVisitorData?.let { put("visitorData", it) }
                }
                putJsonObject("user") {
                    put("lockedSafetyMode", false)
                    authSession?.dataSyncId?.takeIf { it.isNotBlank() }?.let { put("onBehalfOfUser", it.substringBefore("||")) }
                }
                putJsonObject("request") { put("useSsl", true) }
            }
            browseId?.let { put("browseId", it) }
        }

    suspend fun suggestions(query: String): List<String> {
        val normalized = query.trim()
        if (normalized.length < 2) return emptyList()

        val version = ensureShellConfig()
        val language = normalizeLanguage(Locale.getDefault().language)
        val response = client.post("$MUSIC_BASE/music/get_search_suggestions") {
            contentType(ContentType.Application.Json)
            parameter("prettyPrint", "false")
            parameter("hl", language)
            header("User-Agent", USER_AGENT)
            header("Accept-Language", if (language == "en") "en-US,en;q=0.9" else "$language,en-US;q=0.8,en;q=0.7")
            header("X-Origin", MUSIC_ORIGIN)
            header("Origin", MUSIC_ORIGIN)
            header("Referer", "$MUSIC_ORIGIN/")
            applyAuthHeaders()
            header("X-YouTube-Client-Name", CLIENT_NAME)
            header("X-YouTube-Client-Version", version)
            cachedVisitorData?.let { header("X-Goog-Visitor-Id", it) }
            setBody(
                buildJsonObject {
                    putJsonObject("context") {
                        putJsonObject("client") {
                            put("clientName", "WEB_REMIX")
                            put("clientVersion", version)
                            put("hl", language)
                            put("gl", "BR")
                            cachedVisitorData?.let { put("visitorData", it) }
                        }
                        putJsonObject("user") {
                            put("lockedSafetyMode", false)
                            authSession?.dataSyncId?.takeIf { it.isNotBlank() }?.let { put("onBehalfOfUser", it.substringBefore("||")) }
                        }
                        putJsonObject("request") { put("useSsl", true) }
                    }
                    put("input", normalized)
                },
            )
        }.body<JsonObject>()

        val out = linkedSetOf<String>()
        fun walk(element: JsonElement) {
            when (element) {
                is JsonObject -> {
                    element["suggestion"]?.asObject()?.get("runs")?.asArray()?.let { runs ->
                        val text = runs.joinToString("") { it.asObject()?.get("text").asString().orEmpty() }.trim()
                        if (text.isNotBlank()) out += text
                    }
                    element["runs"]?.asArray()?.let { runs ->
                        val text = runs.joinToString("") { it.asObject()?.get("text").asString().orEmpty() }.trim()
                        if (text.length >= 2 && text.length <= 120 && text.contains(normalized, ignoreCase = true)) out += text
                    }
                    element.values.forEach(::walk)
                }
                is JsonArray -> element.forEach(::walk)
                else -> Unit
            }
        }
        walk(response)
        return out.filter { it.isNotBlank() }.distinct().take(10)
    }

    private suspend fun ensureShellConfig(): String {
        cachedClientVersion?.let { return it }
        return runCatching {
            val html = client.get("$MUSIC_ORIGIN/") {
                header("User-Agent", USER_AGENT)
                header("Accept-Language", "pt-BR,pt;q=0.9,en-US;q=0.8,en;q=0.7")
                applyAuthHeaders()
            }.bodyAsText()

            val version = Regex("\\\"INNERTUBE_CLIENT_VERSION\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"")
                .find(html)?.groupValues?.getOrNull(1)
                ?.takeIf { it.isNotBlank() }
                ?: FALLBACK_CLIENT_VERSION
            val visitor = Regex("\\\"VISITOR_DATA\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"")
                .find(html)?.groupValues?.getOrNull(1)
                ?.takeIf { it.isNotBlank() }

            cachedVisitorData = visitor
            cachedClientVersion = version
            version
        }.getOrElse {
            cachedClientVersion = FALLBACK_CLIENT_VERSION
            FALLBACK_CLIENT_VERSION
        }
    }


    private fun HttpRequestBuilder.applyAuthHeaders() {
        val session = authSession ?: return
        if (!hasApiSid(session.cookie)) return
        header("Cookie", session.cookie)
        session.authUser?.takeIf { it.isNotBlank() }?.let { header("X-Goog-AuthUser", it) }
        session.pageId?.takeIf { it.isNotBlank() }?.let { header("X-Goog-PageId", it) }
        val sid = cookieValue(session.cookie, "SAPISID")
            ?: cookieValue(session.cookie, "__Secure-3PAPISID")
            ?: cookieValue(session.cookie, "__Secure-1PAPISID")
            ?: return
        val timestamp = System.currentTimeMillis() / 1000
        val hash = sha1("$timestamp $sid $MUSIC_ORIGIN")
        header("Authorization", "SAPISIDHASH ${timestamp}_$hash")
    }

    private fun cookieValue(header: String, name: String): String? = header.split(';')
        .map { it.trim() }
        .firstOrNull { it.substringBefore('=') == name }
        ?.substringAfter('=', "")
        ?.takeIf { it.isNotBlank() }

    private fun hasApiSid(cookieHeader: String): Boolean =
        listOf("SAPISID", "__Secure-3PAPISID", "__Secure-1PAPISID").any { cookieValue(cookieHeader, it) != null }

    private fun sha1(value: String): String = MessageDigest.getInstance("SHA-1")
        .digest(value.toByteArray(Charsets.UTF_8))
        .joinToString("") { "%02x".format(it) }

    private fun parseTracks(root: JsonElement): List<Track> {
        val renderers = mutableListOf<JsonObject>()

        fun walk(element: JsonElement) {
            when (element) {
                is JsonObject -> {
                    element["musicResponsiveListItemRenderer"]?.asObject()?.let(renderers::add)
                    element.values.forEach(::walk)
                }
                is JsonArray -> element.forEach(::walk)
                else -> Unit
            }
        }
        walk(root)

        return renderers.mapNotNull(::rendererToTrack)
    }

    private fun rendererToTrack(renderer: JsonObject): Track? {
        // Browse rows such as artists/albums also contain overlay video ids.
        // Requiring a watchEndpoint in the row's direct play navigation keeps
        // this first desktop milestone music-track focused.
        val videoId = findVideoId(renderer) ?: return null

        val columns = renderer["flexColumns"].asArray().orEmpty()
        val titleRuns = columns.getOrNull(0)
            .asObject()?.get("musicResponsiveListItemFlexColumnRenderer").asObject()
            ?.get("text").asObject()?.get("runs").asArray().orEmpty()
        val subtitleRuns = columns.getOrNull(1)
            .asObject()?.get("musicResponsiveListItemFlexColumnRenderer").asObject()
            ?.get("text").asObject()?.get("runs").asArray().orEmpty()

        val title = titleRuns.joinToString("") { it.asObject()?.get("text").asString().orEmpty() }.trim()
        if (title.isBlank()) return null

        val subtitleParts = subtitleRuns
            .mapNotNull { it.asObject()?.get("text").asString()?.trim()?.takeIf(String::isNotBlank) }
            .filterNot { it == " • " || it == "•" }

        val duration = subtitleParts.lastOrNull { DURATION.matches(it) }
        val artist = subtitleParts
            .firstOrNull { part -> !DURATION.matches(part) && !part.equals("Song", true) && !part.equals("Music", true) }
            ?: "YouTube Music"

        val thumbnail = findThumbnail(renderer)
        return Track(videoId, title, artist, thumbnail, duration)
    }

    private fun findVideoId(root: JsonElement): String? {
        var found: String? = null
        fun walk(element: JsonElement) {
            if (found != null) return
            when (element) {
                is JsonObject -> {
                    val watch = element["watchEndpoint"].asObject()
                    val id = watch?.get("videoId").asString()
                    if (!id.isNullOrBlank()) {
                        found = id
                        return
                    }
                    element.values.forEach(::walk)
                }
                is JsonArray -> element.forEach(::walk)
                else -> Unit
            }
        }
        walk(root)
        return found
    }

    private fun findThumbnail(root: JsonElement): String? {
        var best: Pair<Int, String>? = null
        fun walk(element: JsonElement) {
            when (element) {
                is JsonObject -> {
                    element["thumbnails"].asArray()?.forEach { candidate ->
                        val obj = candidate.asObject() ?: return@forEach
                        val url = obj["url"].asString() ?: return@forEach
                        val width = obj["width"]?.let { (it as? JsonPrimitive)?.contentOrNull?.toIntOrNull() } ?: 0
                        if (best == null || width > best!!.first) best = width to url
                    }
                    element.values.forEach(::walk)
                }
                is JsonArray -> element.forEach(::walk)
                else -> Unit
            }
        }
        walk(root)
        return best?.second?.let(::normalizeThumbnailUrl)
    }



    private fun findBrowseId(root: JsonElement): String? {
        var found: String? = null
        fun walk(element: JsonElement) {
            if (found != null) return
            when (element) {
                is JsonObject -> {
                    element["browseEndpoint"]?.asObject()?.get("browseId").asString()?.let { if (it.isNotBlank()) found = it }
                    element.values.forEach(::walk)
                }
                is JsonArray -> element.forEach(::walk)
                else -> Unit
            }
        }
        walk(root); return found
    }

    private fun findFirstText(root: JsonElement): String? {
        var found: String? = null
        fun walk(element: JsonElement) {
            if (found != null) return
            when (element) {
                is JsonObject -> {
                    element["runs"]?.asArray()?.firstOrNull()?.asObject()?.get("text").asString()?.takeIf { it.isNotBlank() }?.let { found = it }
                    element["simpleText"].asString()?.takeIf { it.isNotBlank() }?.let { found = it }
                    element.values.forEach(::walk)
                }
                is JsonArray -> element.forEach(::walk)
                else -> Unit
            }
        }
        walk(root); return found
    }

    private fun findSubtitle(root: JsonElement): String? {
        val texts = mutableListOf<String>()
        fun walk(element: JsonElement) {
            when (element) {
                is JsonObject -> {
                    element["runs"]?.asArray()?.forEach { it.asObject()?.get("text").asString()?.trim()?.takeIf { t -> t.isNotBlank() && t != "•" }?.let(texts::add) }
                    element.values.forEach(::walk)
                }
                is JsonArray -> element.forEach(::walk)
                else -> Unit
            }
        }
        walk(root)
        return texts.distinct().drop(1).take(3).joinToString(" • ").takeIf { it.isNotBlank() }
    }

    private fun normalizeThumbnailUrl(raw: String): String = when {
        raw.startsWith("//") -> "https:$raw"
        raw.startsWith("http://") -> "https://${raw.removePrefix("http://")}"
        else -> raw
    }

    private fun normalizeLanguage(raw: String): String = when (raw.lowercase(Locale.ROOT)) {
        "iw" -> "he"
        "in" -> "id"
        "ji" -> "yi"
        "" -> "en"
        else -> raw
    }

    private fun JsonElement?.asObject(): JsonObject? = this as? JsonObject
    private fun JsonElement?.asArray(): JsonArray? = this as? JsonArray
    private fun JsonElement?.asString(): String? = (this as? JsonPrimitive)?.contentOrNull

    private val DURATION = Regex("""\\d{1,2}:\\d{2}(?::\\d{2})?""")
}
