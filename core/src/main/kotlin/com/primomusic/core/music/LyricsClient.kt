package com.primomusic.core.music

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

/**
 * Lyrics are fetched only when the user explicitly opens the Lyrics panel.
 * LRCLIB exact lookup is tried first; search is the fallback because YouTube
 * titles rarely include the exact album/duration metadata required by /api/get.
 */
object LyricsClient {
    data class Lyrics(
        val plainText: String?,
        val syncedLines: List<SyncedLine>,
        val source: String = "LRCLIB",
    )

    data class SyncedLine(val timeMillis: Long, val text: String)

    private val json = Json { ignoreUnknownKeys = true }
    private val client = HttpClient(OkHttp) {
        install(HttpTimeout) {
            requestTimeoutMillis = 20_000
            connectTimeoutMillis = 10_000
            socketTimeoutMillis = 15_000
        }
        expectSuccess = false
    }

    suspend fun fetch(title: String, artist: String): Lyrics? {
        if (title.isBlank() || artist.isBlank()) return null
        val cleanTitle = cleanup(title)
        val cleanArtist = cleanupArtist(artist)

        exact(cleanTitle, cleanArtist)?.let { return it }
        search(cleanTitle, cleanArtist)?.let { return it }

        // A final broader search helps when the result row contains collaborators.
        val firstArtist = cleanArtist.substringBefore(',').substringBefore('&').trim()
        if (firstArtist != cleanArtist) search(cleanTitle, firstArtist)?.let { return it }
        return null
    }

    private suspend fun exact(title: String, artist: String): Lyrics? {
        val response = client.get("https://lrclib.net/api/get") {
            parameter("track_name", title)
            parameter("artist_name", artist)
        }
        if (response.status.value !in 200..299) return null
        val root = runCatching { json.parseToJsonElement(response.bodyAsText()) as? JsonObject }.getOrNull() ?: return null
        return parse(root)
    }

    private suspend fun search(title: String, artist: String): Lyrics? {
        val response = client.get("https://lrclib.net/api/search") {
            parameter("track_name", title)
            parameter("artist_name", artist)
        }
        if (response.status.value !in 200..299) return null
        val array = runCatching { json.parseToJsonElement(response.bodyAsText()) as? JsonArray }.getOrNull() ?: return null
        return array
            .mapNotNull { it as? JsonObject }
            .sortedByDescending { score(it, title, artist) }
            .firstNotNullOfOrNull(::parse)
    }

    private fun score(obj: JsonObject, title: String, artist: String): Int {
        val t = (obj["trackName"] as? JsonPrimitive)?.contentOrNull.orEmpty()
        val a = (obj["artistName"] as? JsonPrimitive)?.contentOrNull.orEmpty()
        var score = 0
        if (t.equals(title, true)) score += 6 else if (t.contains(title, true) || title.contains(t, true)) score += 3
        if (a.equals(artist, true)) score += 5 else if (a.contains(artist, true) || artist.contains(a, true)) score += 2
        if ((obj["syncedLyrics"] as? JsonPrimitive)?.contentOrNull?.isNotBlank() == true) score += 2
        return score
    }

    private fun parse(root: JsonObject): Lyrics? {
        val plain = (root["plainLyrics"] as? JsonPrimitive)?.contentOrNull?.takeIf { it.isNotBlank() }
        val synced = (root["syncedLyrics"] as? JsonPrimitive)?.contentOrNull
            ?.takeIf { it.isNotBlank() }
            ?.let(::parseSynced)
            .orEmpty()
        if (plain == null && synced.isEmpty()) return null
        return Lyrics(plainText = plain, syncedLines = synced)
    }

    private fun cleanup(value: String): String = value
        .replace(Regex("\\s*\\([^)]*(official|video|audio|lyrics?|visualizer|remaster(?:ed)?)[^)]*\\)", RegexOption.IGNORE_CASE), "")
        .replace(Regex("\\s*\\[[^]]*(official|video|audio|lyrics?|visualizer|remaster(?:ed)?)[^]]*\\]", RegexOption.IGNORE_CASE), "")
        .trim()

    private fun cleanupArtist(value: String): String = value
        .replace(" • ", ", ")
        .replace(Regex("\\s+&\\s+"), " & ")
        .trim()

    private fun parseSynced(raw: String): List<SyncedLine> = raw.lineSequence().mapNotNull { line ->
        val match = LRC.find(line) ?: return@mapNotNull null
        val minutes = match.groupValues[1].toLongOrNull() ?: return@mapNotNull null
        val seconds = match.groupValues[2].toDoubleOrNull() ?: return@mapNotNull null
        val text = match.groupValues[3].trim()
        if (text.isBlank()) return@mapNotNull null
        SyncedLine((minutes * 60_000 + seconds * 1_000).toLong(), text)
    }.toList()

    private val LRC = Regex("""^\\[(\\d{1,2}):(\\d{2}(?:\\.\\d{1,3})?)][ \\t]*(.*)$""")
}
