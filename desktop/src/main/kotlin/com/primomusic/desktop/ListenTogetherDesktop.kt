package com.primomusic.desktop

import com.primomusic.core.music.YouTubeMusicSearchClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put
import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.net.http.WebSocket
import java.nio.charset.StandardCharsets
import java.time.Duration
import java.util.UUID
import java.util.concurrent.CompletionStage
import java.util.prefs.Preferences

/** Desktop client of backend/README.md. The server, not the local player, owns room state. */
internal class ListenTogetherDesktop(
    private val scope: CoroutineScope,
    private val onState: (Room) -> Unit,
) {
    data class Room(
        val code: String = "",
        val members: List<String> = emptyList(),
        val playback: JsonObject? = null,
        val connected: Boolean = false,
        val error: String? = null,
    )

    private val client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15)).build()
    private val json = Json { ignoreUnknownKeys = true }
    private val preferences = Preferences.userNodeForPackage(ListenTogetherDesktop::class.java)
    val savedServer: String get() = preferences.get("partyServer", "")
    private val deviceId: String = preferences.get("partyDevice", null)
        ?: UUID.randomUUID().toString().also { preferences.put("partyDevice", it) }
    private var server = ""
    private var token = ""
    private var socket: WebSocket? = null
    private var pingJob: Job? = null
    private var lastSeq = -1L
    var room = Room()
        private set

    private fun publish(value: Room) { room = value; onState(value) }

    suspend fun enter(serverUrl: String, code: String?, displayName: String, avatar: String?): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                val base = serverUrl.trim().trimEnd('/')
                require(base.startsWith("https://") || base.startsWith("http://")) { "Informe a URL HTTP(S) do servidor." }
                require(displayName.isNotBlank()) { "Conecte sua conta antes de entrar em uma sala." }
                val cleanedCode = code?.filter { it.isLetterOrDigit() }?.uppercase()
                require(cleanedCode == null || cleanedCode.length == 6) { "O código deve ter seis caracteres." }
                val body = buildJsonObject {
                    put("userId", "koda-${displayName.lowercase().replace(" ", "-")}")
                    put("deviceId", deviceId)
                    put("displayName", displayName)
                    avatar?.let { put("avatarUrl", it) }
                }.toString()
                val endpoint = if (cleanedCode == null) "$base/api/parties" else "$base/api/parties/$cleanedCode/join"
                val request = HttpRequest.newBuilder(URI(endpoint)).timeout(Duration.ofSeconds(20))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body)).build()
                val response = client.send(request, HttpResponse.BodyHandlers.ofString())
                if (response.statusCode() !in 200..299) {
                    val message = runCatching { json.parseToJsonElement(response.body()).jsonObject["message"]?.jsonPrimitive?.contentOrNull }.getOrNull()
                    error(message ?: "Servidor recusou a sala (${response.statusCode()}).")
                }
                val membership = json.parseToJsonElement(response.body()).jsonObject
                leave()
                server = base
                preferences.put("partyServer", base)
                token = membership["token"]!!.jsonPrimitive.content
                val actualCode = membership["code"]!!.jsonPrimitive.content
                lastSeq = -1L
                val party = membership["party"]!!.jsonObject
                publish(Room(actualCode, names(party["members"]), party["playback"]?.jsonObject))
                connect(actualCode)
            }
        }

    private fun connect(code: String) {
        val wsBase = server.replaceFirst("https://", "wss://").replaceFirst("http://", "ws://")
        val uri = URI("$wsBase/ws/parties/$code?token=${URLEncoder.encode(token, StandardCharsets.UTF_8)}")
        client.newWebSocketBuilder().connectTimeout(Duration.ofSeconds(15)).buildAsync(uri, object : WebSocket.Listener {
            private val buffer = StringBuilder()
            override fun onOpen(webSocket: WebSocket) {
                socket = webSocket
                scope.launch { publish(room.copy(connected = true, error = null)) }
                webSocket.request(1)
            }
            override fun onText(webSocket: WebSocket, data: CharSequence, last: Boolean): CompletionStage<*>? {
                buffer.append(data)
                if (last) {
                    val frame = buffer.toString()
                    buffer.setLength(0)
                    scope.launch { receive(frame) }
                }
                webSocket.request(1)
                return null
            }
            override fun onError(webSocket: WebSocket, error: Throwable) {
                scope.launch { publish(room.copy(connected = false, error = error.message ?: "Conexão interrompida.")) }
            }
            override fun onClose(webSocket: WebSocket, statusCode: Int, reason: String): CompletionStage<*>? {
                scope.launch { publish(room.copy(connected = false, error = "Conexão encerrada: $reason")) }
                return null
            }
        }).exceptionally { failure ->
            scope.launch { publish(room.copy(connected = false, error = failure.message ?: "Servidor indisponível.")) }
            null
        }
        pingJob?.cancel()
        pingJob = scope.launch {
            while (isActive && room.code == code) {
                send(buildJsonObject { put("type", "ping"); put("clientMs", System.currentTimeMillis()) })
                delay(10_000)
            }
        }
    }

    private fun receive(raw: String) {
        val frame = runCatching { json.parseToJsonElement(raw).jsonObject }.getOrNull() ?: return
        when (frame["type"]?.jsonPrimitive?.contentOrNull) {
            "welcome" -> {
                val party = frame["party"]?.jsonObject ?: return
                val playback = party["playback"]?.jsonObject
                lastSeq = playback?.get("seq")?.jsonPrimitive?.longOrNull ?: lastSeq
                publish(room.copy(members = names(party["members"]), playback = playback, connected = true))
            }
            "members" -> publish(room.copy(members = names(frame["members"])))
            "state" -> {
                val playback = frame["playback"]?.jsonObject ?: return
                val seq = playback["seq"]?.jsonPrimitive?.longOrNull ?: return
                if (seq > lastSeq) { lastSeq = seq; publish(room.copy(playback = playback)) }
            }
            "error" -> publish(room.copy(error = frame["message"]?.jsonPrimitive?.contentOrNull ?: "Erro na sala."))
        }
    }

    private fun names(element: kotlinx.serialization.json.JsonElement?): List<String> =
        runCatching { element?.jsonArray?.mapNotNull { it.jsonObject["displayName"]?.jsonPrimitive?.contentOrNull } ?: emptyList() }.getOrDefault(emptyList())

    fun control(action: String, positionMs: Long? = null, track: YouTubeMusicSearchClient.Track? = null) {
        if (!room.connected) return
        send(buildJsonObject {
            put("type", "control")
            put("action", action)
            positionMs?.let { put("positionMs", it) }
            track?.let {
                put("track", buildJsonObject {
                    put("videoId", it.videoId)
                    put("title", it.title)
                    put("artist", it.artist)
                    it.thumbnailUrl?.let { url -> put("thumbnailUrl", url) }
                })
                put("isPlaying", true)
            }
        })
    }

    private fun send(value: JsonObject) { socket?.sendText(value.toString(), true) }

    fun leave() {
        val oldCode = room.code
        val oldToken = token
        pingJob?.cancel()
        socket?.sendClose(WebSocket.NORMAL_CLOSURE, "leave")
        socket = null
        token = ""
        publish(Room())
        if (oldCode.isNotBlank() && oldToken.isNotBlank()) scope.launch(Dispatchers.IO) {
            runCatching {
                client.send(HttpRequest.newBuilder(URI("$server/api/parties/$oldCode/leave"))
                    .header("Authorization", "Bearer $oldToken")
                    .POST(HttpRequest.BodyPublishers.noBody()).build(), HttpResponse.BodyHandlers.discarding())
            }
        }
    }
}
