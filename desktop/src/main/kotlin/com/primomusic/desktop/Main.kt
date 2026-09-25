package com.primomusic.desktop

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Login
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.DialogWindow
import androidx.compose.ui.window.rememberDialogState
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import coil3.compose.AsyncImage
import com.kashif_e.backdrop.Backdrop
import com.kashif_e.backdrop.backdrops.layerBackdrop
import com.kashif_e.backdrop.backdrops.rememberLayerBackdrop
import com.kashif_e.backdrop.drawBackdrop
import com.kashif_e.backdrop.effects.blur as backdropBlur
import com.kashif_e.backdrop.effects.lens
import com.kashif_e.backdrop.effects.vibrancy
import com.kashif_e.backdrop.highlight.Highlight
import com.primomusic.core.music.YouTubeMusicSearchClient
import com.primomusic.core.music.LyricsClient
import com.primomusic.desktop.ui.LiquidGlassSurface
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.longOrNull
import java.awt.Desktop
import java.net.URI
import kotlin.math.roundToInt

private data class Palette(
    val bg: Color,
    val sidebar: Color,
    val surface: Color,
    val surfaceAlt: Color,
    val border: Color,
    val accent: Color,
    val accent2: Color,
    val text: Color,
    val muted: Color,
)

private val PurpleDark = Palette(
    bg = Color(0xFF08080C), sidebar = Color(0xFF101016), surface = Color(0xFF15141D),
    surfaceAlt = Color(0xFF201A2B), border = Color(0xFF393046), accent = Color(0xFFAD68FF),
    accent2 = Color(0xFF7636E9), text = Color(0xFFF9F6FF), muted = Color(0xFFAAA2B8),
)

private val MonochromeDark = Palette(
    bg = Color(0xFF050505), sidebar = Color(0xFF0A0A0A), surface = Color(0xFF111111),
    surfaceAlt = Color(0xFF191919), border = Color(0xFF323232), accent = Color(0xFFF2F2F2),
    accent2 = Color(0xFFB9B9B9), text = Color(0xFFF5F5F5), muted = Color(0xFFA0A0A0),
)

private fun Palette.onAccent(): Color =
    if (accent.red > .8f && accent.green > .8f && accent.blue > .8f) Color(0xFF101010) else Color.White

private fun glassPalette(base: Palette): Palette = base.copy(
    // Alpha is preserved by the surfaces below. Earlier builds accidentally
    // overwrote these values with copy(alpha=.90), which made the toggle look
    // almost identical to the solid theme.
    sidebar = base.sidebar.copy(alpha = 0.62f),
    surface = base.surface.copy(alpha = 0.58f),
    surfaceAlt = base.surfaceAlt.copy(alpha = 0.48f),
    border = base.text.copy(alpha = 0.18f),
)


/**
 * Organic mask used by the optional glass backdrop. The shape is intentionally
 * static: refraction-looking depth comes from overlapping translucent layers,
 * not an always-running animation, keeping GPU cost predictable on Windows.
 */
private object LiquidBlobShape : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val w = size.width
        val h = size.height
        val path = Path().apply {
            moveTo(w * 0.12f, h * 0.18f)
            cubicTo(w * 0.30f, -h * 0.04f, w * 0.76f, h * 0.02f, w * 0.91f, h * 0.27f)
            cubicTo(w * 1.03f, h * 0.48f, w * 0.83f, h * 0.68f, w * 0.70f, h * 0.82f)
            cubicTo(w * 0.52f, h * 1.02f, w * 0.18f, h * 0.98f, w * 0.06f, h * 0.72f)
            cubicTo(-w * 0.05f, h * 0.48f, -w * 0.03f, h * 0.32f, w * 0.12f, h * 0.18f)
            close()
        }
        return Outline.Generic(path)
    }
}


/**
 * Real Skia-backed refraction for Desktop. This is used only when the user
 * enables Liquid Glass. The backdrop library samples pixels rendered behind
 * the element, then applies blur + lens distortion + a specular highlight.
 */
private fun Modifier.liquidGlassSurface(
    enabled: Boolean,
    backdrop: Backdrop,
    shape: Shape,
    tint: Color,
): Modifier {
    if (!enabled) return this
    return drawBackdrop(
        backdrop = backdrop,
        shape = { shape },
        effects = {
            vibrancy()
            backdropBlur(4.dp.toPx())
            lens(
                refractionHeight = 16.dp.toPx(),
                refractionAmount = 26.dp.toPx(),
                chromaticAberration = true,
            )
        },
        highlight = { Highlight.Ambient },
        onDrawSurface = {
            drawRect(tint.copy(alpha = 0.13f))
        },
    )
}

private enum class Section(val label: String, val icon: ImageVector) {
    HOME("Ouvir agora", Icons.Filled.Home),
    EXPLORE("Explorar", Icons.Filled.MusicNote),
    SEARCH("Buscar", Icons.Filled.Search),
    LIBRARY("Biblioteca", Icons.Filled.LibraryMusic),
    PLAYLISTS("Playlists", Icons.Filled.QueueMusic),
    LIKED("Curtidas", Icons.Filled.Favorite),
    HISTORY("Histórico", Icons.Filled.Article),
    TOGETHER("Ouvir juntos", Icons.Filled.LibraryMusic),
    SETTINGS("Configurações", Icons.Filled.Settings),
}

private enum class SettingsPanel(
    val label: String,
    val icon: ImageVector,
) {
    GENERAL("Geral", Icons.Filled.Settings),
    APPEARANCE("Aparência", Icons.Filled.DarkMode),
    AUDIO("Áudio", Icons.Filled.VolumeUp),
    PERFORMANCE("Desempenho", Icons.Filled.Speed),
    SHORTCUTS("Atalhos", Icons.Filled.Keyboard),
    ADVANCED("Avançado", Icons.Filled.Tune),
}

fun main() {
    application {
        val windowState = rememberWindowState(width = 1600.dp, height = 900.dp)
        val appIcon = painterResource("branding/p-music-icon.png")
        val player = remember { DesktopAudioPlayer() }
    
        var theme by remember { mutableStateOf(DesktopPreferences.theme()) }
        var liquidGlass by remember { mutableStateOf(DesktopPreferences.liquidGlassEnabled()) }
        var gamerMode by remember { mutableStateOf(DesktopPreferences.gamerModeEnabled()) }

        val basePalette = when (theme) {
            DesktopTheme.MONOCHROME -> MonochromeDark
            DesktopTheme.PURPLE -> PurpleDark
        }
        // The glass layer obscured artwork and text in the Windows player.
        val effectiveLiquidGlass = false
        val p = if (effectiveLiquidGlass) glassPalette(basePalette) else basePalette

        LaunchedEffect(gamerMode) {
            player.setGamerMode(gamerMode)
        }

        Window(
            onCloseRequest = {
                // Close is final: stop our mpv first, release login resources,
                // then leave Compose even if one cleanup path reports an error.
                runCatching { player.close() }
                runCatching { DesktopGoogleLogin.shutdown() }
                exitApplication()
            },
            title = "Koda Music",
            state = windowState,
            icon = appIcon,
        ) {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    primary = p.accent,
                    background = p.bg,
                    surface = p.surface,
                ),
            ) {
                KodaMusicApp(
                    p = p,
                    darkTheme = true,
                    liquidGlass = effectiveLiquidGlass,
                    player = player,
                    theme = theme,
                    gamerMode = gamerMode,
                    onTheme = { selected ->
                        theme = selected
                        DesktopPreferences.setTheme(selected)
                    },
                    onGamerMode = { enabled ->
                        gamerMode = enabled
                        DesktopPreferences.setGamerModeEnabled(enabled)
                    },
                    onLiquidGlass = { enabled ->
                        liquidGlass = enabled
                        DesktopPreferences.setLiquidGlassEnabled(enabled)
                    },
                    onFullscreen = { enabled ->
                        windowState.placement =
                            if (enabled) WindowPlacement.Fullscreen else WindowPlacement.Floating
                    },
                )
            }
        }
    }
}

@Composable
private fun KodaMusicApp(
    p: Palette,
    darkTheme: Boolean,
    liquidGlass: Boolean,
    player: DesktopAudioPlayer,
    theme: DesktopTheme,
    gamerMode: Boolean,
    onTheme: (DesktopTheme) -> Unit,
    onGamerMode: (Boolean) -> Unit,
    onLiquidGlass: (Boolean) -> Unit,
    onFullscreen: (Boolean) -> Unit,
) {
    var section by remember { mutableStateOf(Section.HOME) }
    var settingsOpen by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    var suggestions by remember { mutableStateOf<List<String>>(emptyList()) }
    var results by remember { mutableStateOf<List<YouTubeMusicSearchClient.Track>>(emptyList()) }
    var richResults by remember { mutableStateOf<List<YouTubeMusicSearchClient.SearchEntry>>(emptyList()) }
    var searchFilter by remember { mutableStateOf(YouTubeMusicSearchClient.SearchFilter.ALL) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    var discoveryShelves by remember { mutableStateOf<List<YouTubeMusicSearchClient.HomeShelf>>(emptyList()) }
    var discoveryLoading by remember { mutableStateOf(false) }
    var discoveryError by remember { mutableStateOf<String?>(null) }

    var exploreSections by remember { mutableStateOf<List<YouTubeMusicSearchClient.MoodGenreSection>>(emptyList()) }
    var exploreLoading by remember { mutableStateOf(false) }
    var exploreError by remember { mutableStateOf<String?>(null) }
    var openedMood by remember { mutableStateOf<YouTubeMusicSearchClient.MoodGenre?>(null) }
    var openedMoodShelves by remember { mutableStateOf<List<YouTubeMusicSearchClient.HomeShelf>>(emptyList()) }
    var openedMoodLoading by remember { mutableStateOf(false) }

    var openedBrowse by remember { mutableStateOf<YouTubeMusicSearchClient.BrowseItem?>(null) }
    var openedBrowseTracks by remember { mutableStateOf<List<YouTubeMusicSearchClient.Track>>(emptyList()) }
    var openedBrowseShelves by remember { mutableStateOf<List<YouTubeMusicSearchClient.HomeShelf>>(emptyList()) }
    var openedBrowseLoading by remember { mutableStateOf(false) }
    var openedBrowseError by remember { mutableStateOf<String?>(null) }
    var loginOpen by remember { mutableStateOf(false) }
    var accountConnected by remember { mutableStateOf(DesktopGoogleLogin.restore()) }
    var accountError by remember { mutableStateOf<String?>(null) }
    var accountLoading by remember { mutableStateOf(false) }
    var accountHistory by remember { mutableStateOf<List<YouTubeMusicSearchClient.Track>>(emptyList()) }
    var accountLiked by remember { mutableStateOf<List<YouTubeMusicSearchClient.Track>>(emptyList()) }
    var accountPlaylists by remember { mutableStateOf<List<YouTubeMusicSearchClient.Playlist>>(emptyList()) }
    var accountProfile by remember { mutableStateOf<YouTubeMusicSearchClient.AccountProfile?>(null) }
    var audioQuality by remember { mutableStateOf(DesktopPreferences.audioQuality()) }
    var newPlaylistOpen by remember { mutableStateOf(false) }
    var newPlaylistTitle by remember { mutableStateOf("") }
    var playlistActionError by remember { mutableStateOf<String?>(null) }
    var queueOpen by remember { mutableStateOf(false) }
    var playbackQueue by remember { mutableStateOf<List<YouTubeMusicSearchClient.Track>>(emptyList()) }
    var openedPlaylist by remember { mutableStateOf<YouTubeMusicSearchClient.Playlist?>(null) }
    var openedPlaylistTracks by remember { mutableStateOf<List<YouTubeMusicSearchClient.Track>>(emptyList()) }
    var openedPlaylistLoading by remember { mutableStateOf(false) }
    var playlistPickerOpen by remember { mutableStateOf(false) }
    var playlistPickerError by remember { mutableStateOf<String?>(null) }
    var selected by remember { mutableStateOf<YouTubeMusicSearchClient.Track?>(null) }
    var playerState by remember { mutableStateOf(DesktopAudioPlayer.Snapshot()) }
    var shuffle by remember { mutableStateOf(false) }
    var repeat by remember { mutableStateOf(false) }
    var fullPlayerOpen by remember { mutableStateOf(false) }
    var lyricsVisible by remember { mutableStateOf(false) }
    val favorites = remember { mutableStateListOf<String>() }
    val scope = rememberCoroutineScope()
    var partyRoom by remember { mutableStateOf(ListenTogetherDesktop.Room()) }
    val party = remember { ListenTogetherDesktop(scope) { partyRoom = it } }
    var partyServer by remember { mutableStateOf(party.savedServer) }
    var partyCode by remember { mutableStateOf("") }
    var partyBusy by remember { mutableStateOf(false) }
    var partyError by remember { mutableStateOf<String?>(null) }

    // The room's numbered playback frame is authoritative. The local player
    // follows it, including when a device joins an already running song.
    LaunchedEffect(partyRoom.playback, partyRoom.connected) {
        if (!partyRoom.connected) return@LaunchedEffect
        val playback = partyRoom.playback ?: return@LaunchedEffect
        val remote = runCatching { playback["track"]?.jsonObject }.getOrNull() ?: return@LaunchedEffect
        val videoId = remote["videoId"]?.jsonPrimitive?.contentOrNull ?: return@LaunchedEffect
        val playing = playback["isPlaying"]?.jsonPrimitive?.contentOrNull == "true"
        val anchor = playback["anchorMs"]?.jsonPrimitive?.longOrNull ?: System.currentTimeMillis()
        val base = playback["positionMs"]?.jsonPrimitive?.longOrNull ?: 0L
        val expected = base + if (playing) (System.currentTimeMillis() - anchor).coerceAtLeast(0L) else 0L
        if (selected?.videoId != videoId) {
            selected = YouTubeMusicSearchClient.Track(
                videoId, remote["title"]?.jsonPrimitive?.contentOrNull ?: "Música",
                remote["artist"]?.jsonPrimitive?.contentOrNull ?: "",
                remote["thumbnailUrl"]?.jsonPrimitive?.contentOrNull, null,
            )
            if (playing) {
                runCatching { player.play(videoId, playerState.volume, audioQuality, 0L) { playerState = it } }
                if (expected > 0 && playerState.durationMillis > 0) player.seek((expected.toFloat() / playerState.durationMillis).coerceIn(0f, 1f)) { playerState = it }
            }
        } else {
            if (playing && playerState.state != DesktopAudioPlayer.State.PLAYING) {
                if (playerState.state == DesktopAudioPlayer.State.PAUSED) player.toggle { playerState = it }
                else runCatching { player.play(videoId, playerState.volume, audioQuality, 0L) { playerState = it } }
            } else if (!playing && playerState.state == DesktopAudioPlayer.State.PLAYING) player.toggle { playerState = it }
            if (playerState.durationMillis > 0 && kotlin.math.abs(playerState.positionMillis - expected) > 1800L) {
                player.seek((expected.toFloat() / playerState.durationMillis).coerceIn(0f, 1f)) { playerState = it }
            }
        }
    }

    fun search(
        value: String = query,
        filter: YouTubeMusicSearchClient.SearchFilter = searchFilter,
    ) {
        if (value.isBlank() || loading) return
        query = value
        searchFilter = filter
        section = Section.SEARCH
        openedBrowse = null
        loading = true
        error = null
        scope.launch {
            runCatching { YouTubeMusicSearchClient.searchRich(value, filter) }
                .onSuccess { entries ->
                    richResults = entries
                    results = entries.mapNotNull { it.track }.distinctBy { it.videoId }
                }
                .onFailure {
                    richResults = emptyList()
                    results = emptyList()
                    error = it.message ?: "Não foi possível pesquisar."
                }
            loading = false
        }
    }

    fun playTrack(track: YouTubeMusicSearchClient.Track, sourceQueue: List<YouTubeMusicSearchClient.Track> = emptyList()) {
        party.control("setTrack", 0L, track)
        selected = track
        lyricsVisible = false
        val normalizedQueue = sourceQueue.distinctBy { it.videoId }
        playbackQueue = when {
            normalizedQueue.isNotEmpty() -> normalizedQueue
            playbackQueue.any { it.videoId == track.videoId } -> playbackQueue
            else -> playbackQueue + track
        }
        scope.launch {
            runCatching { player.play(track.videoId, playerState.volume, audioQuality, durationTextToMillis(track.durationText)) { playerState = it } }
                .onFailure { playerState = playerState.copy(state = DesktopAudioPlayer.State.ERROR, message = it.message) }
        }
    }

    fun refreshAccount() {
        if (!accountConnected || accountLoading) return
        accountLoading = true
        accountError = null
        scope.launch {
            runCatching {
                val profile = YouTubeMusicSearchClient.accountProfile()
                val playlists = YouTubeMusicSearchClient.userPlaylists()
                val history = YouTubeMusicSearchClient.history()
                val liked = YouTubeMusicSearchClient.likedSongs()
                arrayOf(profile, playlists, history, liked)
            }.onSuccess { data ->
                accountProfile = data[0] as YouTubeMusicSearchClient.AccountProfile?
                @Suppress("UNCHECKED_CAST") val playlists = data[1] as List<YouTubeMusicSearchClient.Playlist>
                @Suppress("UNCHECKED_CAST") val history = data[2] as List<YouTubeMusicSearchClient.Track>
                @Suppress("UNCHECKED_CAST") val liked = data[3] as List<YouTubeMusicSearchClient.Track>
                accountPlaylists = playlists
                accountHistory = history
                accountLiked = liked
                favorites.clear()
                favorites.addAll(liked.map { it.videoId }.distinct())
            }.onFailure {
                accountError = it.message ?: "Não foi possível sincronizar a conta do YouTube Music."
            }
            accountLoading = false
        }
    }

    fun toggleFavorite(videoId: String) {
        val wantLiked = !favorites.contains(videoId)
        if (!accountConnected) {
            if (wantLiked) favorites.add(videoId) else favorites.remove(videoId)
            return
        }
        scope.launch {
            runCatching { YouTubeMusicSearchClient.setLiked(videoId, wantLiked) }
                .onSuccess {
                    if (wantLiked) { if (!favorites.contains(videoId)) favorites.add(videoId) }
                    else favorites.remove(videoId)
                    accountLiked = if (wantLiked) {
                        val track = (
                            results +
                                richResults.mapNotNull { it.track } +
                                accountHistory +
                                accountLiked +
                                listOfNotNull(selected)
                            ).firstOrNull { it.videoId == videoId }
                        if (track != null && accountLiked.none { it.videoId == videoId }) listOf(track) + accountLiked else accountLiked
                    } else accountLiked.filterNot { it.videoId == videoId }
                }
                .onFailure { accountError = it.message ?: "Não foi possível atualizar Músicas que gostei." }
        }
    }

    LaunchedEffect(accountConnected) {
        if (accountConnected) {
            refreshAccount()

            // The signed-in home is personalised, so refresh discovery after a
            // login/restore rather than keeping the guest feed for the session.
            discoveryLoading = true
            runCatching {
                val home = YouTubeMusicSearchClient.homeShelves()
                val releases = YouTubeMusicSearchClient.newReleaseShelves()
                (home + releases).distinctBy { it.title }.take(14)
            }.onSuccess {
                discoveryShelves = it
                discoveryError = null
            }.onFailure {
                discoveryError = it.message ?: "Não foi possível atualizar recomendações."
            }
            discoveryLoading = false
        }
    }

    fun indexOfSelected(): Int = selected?.let { s -> playbackQueue.indexOfFirst { it.videoId == s.videoId } } ?: -1
    fun nextTrack() {
        if (playbackQueue.isEmpty()) return
        val current = indexOfSelected()
        val next = if (shuffle && playbackQueue.size > 1) playbackQueue.indices.filter { it != current }.random()
        else if (current < 0 || current + 1 >= playbackQueue.size) 0 else current + 1
        playTrack(playbackQueue[next], playbackQueue)
    }
    fun previousTrack() {
        if (playbackQueue.isEmpty()) return
        val current = indexOfSelected()
        val prev = if (current <= 0) playbackQueue.lastIndex else current - 1
        playTrack(playbackQueue[prev], playbackQueue)
    }

    fun openPlaylist(playlist: YouTubeMusicSearchClient.Playlist) {
        openedPlaylist = playlist
        openedPlaylistTracks = emptyList()
        openedPlaylistLoading = true
        accountError = null
        scope.launch {
            runCatching { YouTubeMusicSearchClient.playlistTracks(playlist.playlistId) }
                .onSuccess { openedPlaylistTracks = it }
                .onFailure { accountError = it.message ?: "Não foi possível abrir a playlist." }
            openedPlaylistLoading = false
        }
    }


    fun openBrowse(item: YouTubeMusicSearchClient.BrowseItem) {
        openedBrowse = item
        openedBrowseTracks = emptyList()
        openedBrowseShelves = emptyList()
        openedBrowseError = null
        openedBrowseLoading = true
        scope.launch {
            runCatching {
                val tracks = YouTubeMusicSearchClient.browseTracks(item.browseId)
                val shelves = YouTubeMusicSearchClient.categoryShelves(item.browseId)
                tracks to shelves
            }.onSuccess { (tracks, shelves) ->
                openedBrowseTracks = tracks
                openedBrowseShelves = shelves
            }.onFailure {
                openedBrowseError = it.message ?: "Não foi possível abrir esta página."
            }
            openedBrowseLoading = false
        }
    }

    fun playShelfItem(
        item: YouTubeMusicSearchClient.ShelfItem,
        source: List<YouTubeMusicSearchClient.ShelfItem>,
    ) {
        val queue = source.mapNotNull { candidate ->
            candidate.videoId?.let { videoId ->
                YouTubeMusicSearchClient.Track(
                    videoId = videoId,
                    title = candidate.title,
                    artist = candidate.subtitle?.substringBefore(" • ")?.takeIf { it.isNotBlank() } ?: "YouTube Music",
                    thumbnailUrl = candidate.thumbnailUrl,
                    durationText = null,
                )
            }
        }
        val track = queue.firstOrNull { it.videoId == item.videoId }
        if (track != null) {
            playTrack(track, queue)
            return
        }

        item.browseId?.let { browseId ->
            openBrowse(
                YouTubeMusicSearchClient.BrowseItem(
                    browseId = browseId,
                    title = item.title,
                    subtitle = item.subtitle,
                    thumbnailUrl = item.thumbnailUrl,
                    kind = when {
                        browseId.startsWith("UC") -> YouTubeMusicSearchClient.BrowseKind.ARTIST
                        browseId.startsWith("MPRE") || browseId.startsWith("MPR") -> YouTubeMusicSearchClient.BrowseKind.ALBUM
                        browseId.startsWith("VL") -> YouTubeMusicSearchClient.BrowseKind.PLAYLIST
                        else -> YouTubeMusicSearchClient.BrowseKind.OTHER
                    },
                ),
            )
        }
    }

    fun openMood(mood: YouTubeMusicSearchClient.MoodGenre) {
        openedMood = mood
        openedMoodShelves = emptyList()
        openedMoodLoading = true
        exploreError = null
        scope.launch {
            runCatching { YouTubeMusicSearchClient.categoryShelves(mood.browseId, mood.params) }
                .onSuccess { openedMoodShelves = it }
                .onFailure { exploreError = it.message ?: "Não foi possível abrir esta categoria." }
            openedMoodLoading = false
        }
    }

    LaunchedEffect(Unit) {
        discoveryLoading = true
        exploreLoading = true

        runCatching {
            val home = YouTubeMusicSearchClient.homeShelves()
            val releases = YouTubeMusicSearchClient.newReleaseShelves()
            (home + releases).distinctBy { it.title }.take(14)
        }.onSuccess {
            discoveryShelves = it
        }.onFailure {
            discoveryError = it.message ?: "Não foi possível carregar recomendações."
        }
        discoveryLoading = false

        runCatching { YouTubeMusicSearchClient.moodAndGenres() }
            .onSuccess { exploreSections = it }
            .onFailure { exploreError = it.message ?: "Não foi possível carregar o Explorar." }
        exploreLoading = false
    }

    LaunchedEffect(player) {
        player.onEnd = { scope.launch { nextTrack() } }
    }

    val panelBackdrop = rememberLayerBackdrop()
    val playerBackdrop = rememberLayerBackdrop()

    LaunchedEffect(query) {
        val q = query.trim()
        if (q.length < 2) suggestions = emptyList()
        else {
            delay(220)
            suggestions = runCatching { YouTubeMusicSearchClient.suggestions(q) }.getOrDefault(emptyList())
        }
    }

    Surface(Modifier.fillMaxSize(), color = p.bg) {
        Box(Modifier.fillMaxSize()) {

            /*
             * Two-stage backdrop architecture:
             *
             * panelBackdrop  -> decorative background sampled by sidebar/search/content glass.
             * playerBackdrop -> captures the COMPLETE rendered app scene below the mini player.
             *
             * The mini player is deliberately outside playerBackdrop so it can refract the
             * actual cards, covers and text that are physically behind it.
             */
            Box(
                Modifier
                    .fillMaxSize()
                    .then(
                        if (liquidGlass) {
                            Modifier.layerBackdrop(playerBackdrop)
                        } else {
                            Modifier
                        },
                    ),
            ) {

                // Decorative scene used by the normal glass panels.
                Box(
                    Modifier
                        .fillMaxSize()
                        .then(
                            if (liquidGlass) {
                                Modifier.layerBackdrop(panelBackdrop)
                            } else {
                                Modifier
                            },
                        )
                        .background(
                            if (liquidGlass) {
                                Brush.linearGradient(
                                    listOf(
                                        p.bg,
                                        p.accent2.copy(alpha = if (darkTheme) 0.14f else 0.08f),
                                        p.bg,
                                    ),
                                )
                            } else {
                                Brush.linearGradient(listOf(p.bg, p.bg))
                            },
                        ),
                ) {
                    if (liquidGlass) {
                        // The reference uses a near-black canvas. Keep glass depth
                        // subtle so the content, covers and typography remain the
                        // visual focus instead of large decorative blobs.
                        Box(
                            Modifier
                                .align(Alignment.TopEnd)
                                .offset(x = 70.dp, y = (-80).dp)
                                .width(430.dp)
                                .height(250.dp)
                                .blur(40.dp)
                                .background(
                                    Brush.radialGradient(
                                        listOf(
                                            p.accent.copy(alpha = .16f),
                                            p.accent2.copy(alpha = .08f),
                                            Color.Transparent,
                                        ),
                                    ),
                                ),
                        )
                    }
                }

                /*
                 * Main UI is inside playerBackdrop.
                 *
                 * Therefore the mini player can sample the FINAL rendered scene:
                 * sidebar, cards, covers, list rows and text.
                 */
                Row(Modifier.fillMaxSize()) {
                    Sidebar(
                        p = p,
                        section = section,
                        profile = accountProfile,
                        liquidGlass = liquidGlass,
                        glassBackdrop = panelBackdrop,
                        onSection = { destination ->
                            if (destination == Section.SETTINGS) settingsOpen = true
                            else section = destination
                        },
                        onLogin = { loginOpen = true },
                    )

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .padding(start = 18.dp, top = 18.dp, end = 18.dp, bottom = 142.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        KodaTopBar(
                            p = p,
                            query = query,
                            suggestions = suggestions,
                            loading = loading,
                            gamerMode = gamerMode,
                            profile = accountProfile,
                            onQuery = { query = it },
                            onSearch = { search() },
                            onSuggestion = { search(it) },
                            onTheme = {
                                onTheme(
                                    if (theme == DesktopTheme.PURPLE) DesktopTheme.MONOCHROME
                                    else DesktopTheme.PURPLE
                                )
                            },
                            onLogin = { loginOpen = true },
                        )

                        val contentShape = RoundedCornerShape(22.dp)

                        Box(
                            Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .liquidGlassSurface(
                                    enabled = liquidGlass,
                                    backdrop = panelBackdrop,
                                    shape = contentShape,
                                    tint = p.surface,
                                ),
                        ) {
                            if (openedBrowse != null) {
                                KodaBrowseDetailView(
                                    p = p,
                                    item = openedBrowse!!,
                                    loading = openedBrowseLoading,
                                    error = openedBrowseError,
                                    tracks = openedBrowseTracks,
                                    shelves = openedBrowseShelves,
                                    onBack = {
                                        openedBrowse = null
                                        openedBrowseTracks = emptyList()
                                        openedBrowseShelves = emptyList()
                                        openedBrowseError = null
                                    },
                                    onPlay = { playTrack(it, openedBrowseTracks) },
                                    onShelfItem = { item, source -> playShelfItem(item, source) },
                                )
                            } else {
                                when (section) {
                                    Section.HOME ->
                                        KodaHomeView(
                                            p = p,
                                            connected = accountConnected,
                                            loading = accountLoading,
                                            error = accountError,
                                            history = accountHistory,
                                            playlists = accountPlaylists,
                                            liked = accountLiked,
                                            discoveryShelves = discoveryShelves,
                                            discoveryLoading = discoveryLoading,
                                            discoveryError = discoveryError,
                                            onSearch = { section = Section.SEARCH },
                                            onExplore = { section = Section.EXPLORE },
                                            onPlay = { playTrack(it, accountHistory) },
                                            onPlayLiked = { playTrack(it, accountLiked) },
                                            onOpenPlaylist = { playlist ->
                                                openPlaylist(playlist)
                                                section = Section.PLAYLISTS
                                            },
                                            onShelfItem = { item, source -> playShelfItem(item, source) },
                                            onRefresh = ::refreshAccount,
                                        )

                                    Section.EXPLORE ->
                                        if (openedMood != null) {
                                            KodaMoodDetailView(
                                                p = p,
                                                mood = openedMood!!,
                                                loading = openedMoodLoading,
                                                error = exploreError,
                                                shelves = openedMoodShelves,
                                                onBack = {
                                                    openedMood = null
                                                    openedMoodShelves = emptyList()
                                                    exploreError = null
                                                },
                                                onShelfItem = { item, source -> playShelfItem(item, source) },
                                            )
                                        } else {
                                            KodaExploreView(
                                                p = p,
                                                loading = exploreLoading,
                                                error = exploreError,
                                                sections = exploreSections,
                                                onOpen = ::openMood,
                                            )
                                        }

                                    Section.SEARCH ->
                                        KodaSearchView(
                                            p = p,
                                            query = query,
                                            filter = searchFilter,
                                            results = richResults,
                                            loading = loading,
                                            error = error,
                                            favorites = favorites,
                                            onFilter = { filter ->
                                                searchFilter = filter
                                                if (query.isNotBlank()) search(query, filter)
                                            },
                                            onPlay = { track ->
                                                playTrack(
                                                    track,
                                                    richResults.mapNotNull { it.track },
                                                )
                                            },
                                            onBrowse = ::openBrowse,
                                        )

                                    Section.LIBRARY ->
                                        KodaLibraryHubView(
                                            p = p,
                                            connected = accountConnected,
                                            loading = accountLoading,
                                            history = accountHistory,
                                            liked = accountLiked,
                                            playlists = accountPlaylists,
                                            onNavigate = { section = it },
                                            onLogin = { loginOpen = true },
                                            onRefresh = ::refreshAccount,
                                        )

                                    Section.PLAYLISTS ->
                                        if (openedPlaylist != null) {
                                            PlaylistDetailView(
                                                p = p,
                                                playlist = openedPlaylist!!,
                                                loading = openedPlaylistLoading,
                                                error = accountError,
                                                tracks = openedPlaylistTracks,
                                                onBack = {
                                                    openedPlaylist = null
                                                    openedPlaylistTracks = emptyList()
                                                },
                                                onPlay = { playTrack(it, openedPlaylistTracks) },
                                            )
                                        } else {
                                            PlaylistsView(
                                                p,
                                                accountConnected,
                                                accountLoading,
                                                accountError,
                                                accountPlaylists,
                                                onRefresh = ::refreshAccount,
                                                onLogin = { loginOpen = true },
                                                onCreate = { newPlaylistOpen = true },
                                                onOpen = ::openPlaylist,
                                            )
                                        }

                                    Section.LIKED ->
                                        KodaTrackCollectionView(
                                            p = p,
                                            title = "Músicas curtidas",
                                            subtitle = "Sua coleção sincronizada com o YouTube Music",
                                            tracks = accountLiked,
                                            loading = accountLoading,
                                            connected = accountConnected,
                                            emptyMessage = "Suas músicas curtidas aparecerão aqui.",
                                            onPlay = { playTrack(it, accountLiked) },
                                            onLogin = { loginOpen = true },
                                            onRefresh = ::refreshAccount,
                                        )

                                    Section.HISTORY ->
                                        KodaTrackCollectionView(
                                            p = p,
                                            title = "Histórico",
                                            subtitle = "O que você ouviu recentemente",
                                            tracks = accountHistory,
                                            loading = accountLoading,
                                            connected = accountConnected,
                                            emptyMessage = "Seu histórico de reprodução aparecerá aqui.",
                                            onPlay = { playTrack(it, accountHistory) },
                                            onLogin = { loginOpen = true },
                                            onRefresh = ::refreshAccount,
                                        )

                                    Section.TOGETHER ->
                                        ListenTogetherView(
                                            p = p,
                                            server = partyServer,
                                            onServer = { partyServer = it },
                                            code = partyCode,
                                            onCode = { partyCode = it },
                                            room = partyRoom,
                                            busy = partyBusy,
                                            error = partyError,
                                            onEnter = { joinCode ->
                                                partyBusy = true
                                                partyError = null
                                                scope.launch {
                                                    party.enter(partyServer, joinCode, accountProfile?.name ?: if (accountConnected) "Koda" else "", accountProfile?.thumbnailUrl)
                                                        .onFailure { partyError = it.message }
                                                    partyBusy = false
                                                }
                                            },
                                            onLeave = { party.leave() },
                                        )

                                    Section.SETTINGS ->
                                        SettingsView(
                                            p = p,
                                            theme = theme,
                                            onTheme = onTheme,
                                            gamerMode = gamerMode,
                                            onGamerMode = onGamerMode,
                                            liquidGlass = liquidGlass,
                                            onLiquidGlass = onLiquidGlass,
                                            quality = audioQuality,
                                            onQuality = {
                                                audioQuality = it
                                                DesktopPreferences.setAudioQuality(it)
                                            },
                                        )
                                }
                            }
                        }
                    }
                }
            }

            /*
             * IMPORTANT:
             * MiniPlayer is a sibling ABOVE playerBackdrop, not a child of it.
             * This is what makes the bar sample/refract the real app behind it.
             *
             * It intentionally floats over the lower part of the content.
             */
            if (!fullPlayerOpen) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(
                            start = 242.dp,
                            end = 22.dp,
                            bottom = 22.dp,
                        ),
                ) {
                    MiniPlayer(
                        p = p,
                        track = selected,
                        state = playerState,
                        favorites = favorites,
                        liquidGlass = liquidGlass,
                        glassBackdrop = playerBackdrop,
                        shuffle = shuffle,
                        repeat = repeat,
                        onShuffle = { shuffle = !shuffle },
                        onRepeat = {
                            repeat = !repeat
                            player.setRepeat(repeat)
                        },
                        onPrevious = ::previousTrack,
                        onNext = ::nextTrack,
                        onToggle = {
                            if (
                                playerState.state in setOf(
                                    DesktopAudioPlayer.State.STOPPED,
                                    DesktopAudioPlayer.State.ERROR,
                                    DesktopAudioPlayer.State.IDLE,
                                )
                            ) {
                                selected?.let {
                                    playTrack(it, playbackQueue)
                                }
                            } else {
                                party.control(if (playerState.state == DesktopAudioPlayer.State.PLAYING) "pause" else "play", playerState.positionMillis)
                                player.toggle {
                                    playerState = it
                                }
                            }
                        },
                        onStop = {
                            player.stop {
                                playerState = it
                            }
                        },
                        onSeek = {
                            party.control("seek", (playerState.durationMillis * it).toLong())
                            player.seek(it) { snap ->
                                playerState = snap
                            }
                        },
                        onVolume = {
                            player.setVolume(it) { snap ->
                                playerState = snap
                            }
                        },
                        onMute = {
                            player.toggleMute { snap ->
                                playerState = snap
                            }
                        },
                        onQueue = {
                            queueOpen = !queueOpen
                        },
                        onFavorite = {
                            selected?.videoId?.let(::toggleFavorite)
                        },
                        onAddToPlaylist = {
                            if (selected != null && accountConnected) {
                                playlistPickerError = null
                                playlistPickerOpen = true
                            } else {
                                loginOpen = true
                            }
                        },
                        onExpand = {
                            if (selected != null) {
                                lyricsVisible = true
                                fullPlayerOpen = true
                                onFullscreen(true)
                            }
                        },
                    )
                }
            }

            if (fullPlayerOpen && selected != null) {
                FullPlayerScreen(
                    p = p,
                    track = selected!!,
                    state = playerState,
                    lyricsVisible = lyricsVisible,
                    favorite = favorites.contains(selected!!.videoId),
                    shuffle = shuffle,
                    repeat = repeat,
                    onClose = {
                        fullPlayerOpen = false
                        lyricsVisible = false
                        onFullscreen(false)
                    },
                    onLyricsToggle = { lyricsVisible = !lyricsVisible },
                    onFavorite = { selected?.videoId?.let(::toggleFavorite) },
                    onShuffle = { shuffle = !shuffle },
                    onRepeat = { repeat = !repeat; player.setRepeat(repeat) },
                    onPrevious = ::previousTrack,
                    onNext = ::nextTrack,
                    onToggle = {
                        if (playerState.state in setOf(DesktopAudioPlayer.State.STOPPED, DesktopAudioPlayer.State.ERROR, DesktopAudioPlayer.State.IDLE)) {
                            selected?.let { playTrack(it, playbackQueue) }
                        } else {
                            party.control(if (playerState.state == DesktopAudioPlayer.State.PLAYING) "pause" else "play", playerState.positionMillis)
                            player.toggle { playerState = it }
                        }
                    },
                    onSeek = {
                        party.control("seek", (playerState.durationMillis * it).toLong())
                        player.seek(it) { snap -> playerState = snap }
                    },
                    onVolume = { player.setVolume(it) { snap -> playerState = snap } },
                    onMute = { player.toggleMute { snap -> playerState = snap } },
                )
            }
            if (queueOpen && !fullPlayerOpen) QueuePanel(p, playbackQueue, selected, onClose = { queueOpen = false }, onPlay = { playTrack(it, playbackQueue) })
            if (playlistPickerOpen && !fullPlayerOpen && selected != null) AddToPlaylistDialog(
                p = p,
                playlists = accountPlaylists,
                error = playlistPickerError,
                onClose = { playlistPickerOpen = false },
                onSelect = { playlist ->
                    scope.launch {
                        runCatching { YouTubeMusicSearchClient.addToPlaylist(playlist.playlistId, selected!!.videoId) }
                            .onSuccess { playlistPickerOpen = false; playlistPickerError = null }
                            .onFailure { playlistPickerError = it.message ?: "Não foi possível adicionar à playlist." }
                    }
                },
            )
            if (loginOpen && !fullPlayerOpen) AccountDialog(
                p = p,
                connected = accountConnected,
                profile = accountProfile,
                error = accountError,
                onClose = { loginOpen = false },
                onLogin = {
                    accountError = null
                    DesktopGoogleLogin.open(
                        onConnected = {
                            accountConnected = true
                            accountError = null
                            loginOpen = false
                            refreshAccount()
                        },
                        onError = { accountError = it },
                    )
                },
                onSignOut = {
                    DesktopGoogleLogin.signOut()
                    accountConnected = false
                    accountProfile = null
                    accountPlaylists = emptyList(); accountHistory = emptyList(); accountLiked = emptyList(); favorites.clear()
                },
            )
            if (newPlaylistOpen && !fullPlayerOpen) NewPlaylistDialog(
                p = p,
                title = newPlaylistTitle,
                error = playlistActionError,
                onTitle = { newPlaylistTitle = it },
                onClose = { newPlaylistOpen = false; playlistActionError = null },
                onCreate = {
                    val title = newPlaylistTitle.trim()
                    if (title.isBlank()) playlistActionError = "Digite um nome para a playlist."
                    else scope.launch {
                        runCatching { YouTubeMusicSearchClient.createPlaylist(title) }
                            .onSuccess {
                                newPlaylistTitle = ""; playlistActionError = null; newPlaylistOpen = false; refreshAccount()
                            }
                            .onFailure { playlistActionError = it.message ?: "Não foi possível criar a playlist." }
                    }
                },
            )
        }
    }

    if (settingsOpen) {
        DialogWindow(
            onCloseRequest = { settingsOpen = false },
            title = "Koda Music · Configurações",
            state = rememberDialogState(width = 980.dp, height = 680.dp),
        ) {
            MaterialTheme(colorScheme = darkColorScheme(primary = p.accent, background = p.bg, surface = p.surface)) {
                Box(Modifier.fillMaxSize().background(p.bg).padding(14.dp)) {
                    SettingsView(
                        p = p,
                        theme = theme,
                        onTheme = onTheme,
                        gamerMode = gamerMode,
                        onGamerMode = onGamerMode,
                        liquidGlass = liquidGlass,
                        onLiquidGlass = onLiquidGlass,
                        quality = audioQuality,
                        onQuality = {
                            audioQuality = it
                            DesktopPreferences.setAudioQuality(it)
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun Sidebar(
    p: Palette,
    section: Section,
    profile: YouTubeMusicSearchClient.AccountProfile?,
    liquidGlass: Boolean,
    glassBackdrop: Backdrop,
    onSection: (Section) -> Unit,
    onLogin: () -> Unit,
) {
    val sidebarShape = RoundedCornerShape(0.dp, 16.dp, 16.dp, 0.dp)
    Column(
        Modifier.width(224.dp).fillMaxHeight()
            .background(p.sidebar)
            .border(1.dp, p.border.copy(alpha = 0.36f), sidebarShape)
            .padding(14.dp),
    ) {
        Brand(p)
        Spacer(Modifier.height(28.dp))
        Section.entries.filter { it != Section.SETTINGS }.forEach { item ->
            NavButton(p, item, section == item) { onSection(item) }
            Spacer(Modifier.height(5.dp))
        }
        Spacer(Modifier.weight(1f))
        NavButton(p, Section.SETTINGS, section == Section.SETTINGS) { onSection(Section.SETTINGS) }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = onLogin, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), border = BorderStroke(1.dp, p.border.copy(alpha = 0.75f))) {
            if (!profile?.thumbnailUrl.isNullOrBlank()) {
                AsyncImage(model = profile?.thumbnailUrl, contentDescription = "Foto da conta", modifier = Modifier.size(22.dp).clip(CircleShape), contentScale = ContentScale.Crop)
            } else {
                Icon(Icons.Filled.AccountCircle, null, tint = p.accent, modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text(profile?.name ?: "Conta Google", color = p.text, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(profile?.subtitle ?: "Gerenciar conta", color = p.muted, fontSize = 9.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun NavButton(p: Palette, item: Section, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(11.dp))
            .background(if (selected) p.accent.copy(alpha = 0.19f) else Color.Transparent)
            .clickable(onClick = onClick).padding(horizontal = 12.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(11.dp),
    ) {
        Icon(item.icon, item.label, tint = if (selected) p.accent else p.muted, modifier = Modifier.size(19.dp))
        Text(item.label, color = if (selected) p.text else p.muted, fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium, fontSize = 12.sp)
    }
}

@Composable
private fun Brand(p: Palette) {
    Image(
        painter = painterResource("branding/p-music-logo.png"),
        contentDescription = "Koda Music",
        modifier = Modifier.width(194.dp).height(58.dp),
        contentScale = ContentScale.Fit,
    )
}

@Composable
private fun KodaTopBar(
    p: Palette,
    query: String,
    suggestions: List<String>,
    loading: Boolean,
    gamerMode: Boolean,
    profile: YouTubeMusicSearchClient.AccountProfile?,
    onQuery: (String) -> Unit,
    onSearch: () -> Unit,
    onSuggestion: (String) -> Unit,
    onTheme: () -> Unit,
    onLogin: () -> Unit,
) {
    Column(Modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().height(52.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            TextField(
                value = query,
                onValueChange = onQuery,
                modifier = Modifier.weight(1f).fillMaxHeight(),
                singleLine = true,
                leadingIcon = { Icon(Icons.Filled.Search, null, tint = p.muted) },
                trailingIcon = {
                    IconButton(onClick = onSearch, enabled = query.isNotBlank() && !loading) {
                        if (loading) {
                            CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = p.accent)
                        } else {
                            Icon(Icons.Filled.Search, "Pesquisar", tint = p.text)
                        }
                    }
                },
                placeholder = { Text("Buscar músicas, artistas, álbuns...", color = p.muted) },
                shape = RoundedCornerShape(15.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = p.surface,
                    unfocusedContainerColor = p.surface,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedTextColor = p.text,
                    unfocusedTextColor = p.text,
                ),
            )

            if (gamerMode) {
                Surface(
                    color = p.surface,
                    shape = RoundedCornerShape(50),
                    border = BorderStroke(1.dp, p.accent.copy(alpha = .55f)),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(7.dp),
                    ) {
                        Image(painterResource("branding/p-music-icon.png"), null, Modifier.size(18.dp))
                        Text("Modo Gamer", color = p.text, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Box(Modifier.size(6.dp).background(Color(0xFF67DE9B), CircleShape))
                    }
                }
            }

            IconButton(onClick = onTheme) {
                Icon(Icons.Filled.DarkMode, "Alternar tema", tint = p.text)
            }

            IconButton(onClick = onLogin) {
                if (!profile?.thumbnailUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = profile?.thumbnailUrl,
                        contentDescription = "Conta",
                        modifier = Modifier.size(32.dp).clip(CircleShape),
                        contentScale = ContentScale.Crop,
                    )
                } else {
                    Icon(Icons.Filled.AccountCircle, "Conta", tint = p.text, modifier = Modifier.size(28.dp))
                }
            }
        }

        if (suggestions.isNotEmpty() && query.length >= 2) {
            Card(
                modifier = Modifier.fillMaxWidth(0.72f).padding(top = 6.dp),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = p.surface),
                border = BorderStroke(1.dp, p.border.copy(alpha = 0.75f)),
            ) {
                Column(Modifier.padding(vertical = 5.dp)) {
                    suggestions.take(5).forEach { suggestion ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable { onSuggestion(suggestion) }
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(Icons.Filled.Search, null, tint = p.muted, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(10.dp))
                            Text(suggestion, color = p.text, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun KodaHomeView(
    p: Palette,
    connected: Boolean,
    loading: Boolean,
    error: String?,
    history: List<YouTubeMusicSearchClient.Track>,
    playlists: List<YouTubeMusicSearchClient.Playlist>,
    liked: List<YouTubeMusicSearchClient.Track>,
    discoveryShelves: List<YouTubeMusicSearchClient.HomeShelf>,
    discoveryLoading: Boolean,
    discoveryError: String?,
    onSearch: () -> Unit,
    onExplore: () -> Unit,
    onPlay: (YouTubeMusicSearchClient.Track) -> Unit,
    onPlayLiked: (YouTubeMusicSearchClient.Track) -> Unit,
    onOpenPlaylist: (YouTubeMusicSearchClient.Playlist) -> Unit,
    onShelfItem: (YouTubeMusicSearchClient.ShelfItem, List<YouTubeMusicSearchClient.ShelfItem>) -> Unit,
    onRefresh: () -> Unit,
) {
    val heroTrack = history.firstOrNull() ?: liked.firstOrNull()
    val heroArt = heroTrack?.thumbnailUrl
        ?: discoveryShelves.firstOrNull()?.items?.firstOrNull()?.thumbnailUrl

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 6.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.fillMaxWidth()) {
            Card(
                modifier = Modifier.weight(1f).height(236.dp),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF090A0F)),
                border = BorderStroke(1.dp, p.border.copy(alpha = .55f)),
            ) {
                Box(Modifier.fillMaxSize()) {
                    heroArt?.let { art ->
                        AsyncImage(
                            model = highResolutionThumbnailUrl(art),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                            alpha = .35f,
                        )
                    }

                    Box(
                        Modifier
                            .fillMaxSize()
                            .background(
                                Brush.horizontalGradient(
                                    listOf(
                                        Color(0xFF07080C).copy(alpha = .99f),
                                        Color(0xFF090A10).copy(alpha = .82f),
                        p.accent2.copy(alpha = .25f),
                                    ),
                                ),
                            ),
                    )

                    Image(
                        painter = painterResource("branding/p-music-logo.png"),
                        contentDescription = "Identidade oficial Koda Music",
                        modifier = Modifier.align(Alignment.TopEnd).padding(18.dp).width(235.dp).height(56.dp),
                        contentScale = ContentScale.Fit,
                    )

                    Column(
                        Modifier
                            .align(Alignment.CenterStart)
                            .padding(horizontal = 26.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text("Ouvir ", color = p.text, fontSize = 30.sp, fontWeight = FontWeight.Black)
                            Text("agora", color = p.accent, fontSize = 30.sp, fontWeight = FontWeight.Black)
                        }
                        Text(
                            "Sua música, sua biblioteca e novas descobertas em um só lugar.",
                            color = p.text.copy(alpha = .72f),
                            fontSize = 13.sp,
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = {
                                    if (heroTrack != null) onPlay(heroTrack) else onSearch()
                                },
                                shape = RoundedCornerShape(13.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = p.accent),
                            ) {
                                Icon(Icons.Filled.PlayArrow, null, tint = p.onAccent())
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    if (heroTrack != null) "Reproduzir mix" else "Buscar música",
                                    fontWeight = FontWeight.Bold,
                                )
                            }

                            OutlinedButton(
                                onClick = onExplore,
                                shape = RoundedCornerShape(13.dp),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = .16f)),
                            ) {
                                Text("Explorar", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                }
            }
            Card(
                modifier = Modifier.width(282.dp).height(236.dp),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF101016)),
                border = BorderStroke(1.dp, p.border.copy(alpha = .65f)),
            ) {
                Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Continue de onde parou", color = p.text, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        if (connected) Text("●", color = p.accent, fontSize = 11.sp)
                    }
                    if (history.isEmpty()) {
                        Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                            Text("Suas músicas recentes aparecerão aqui.", color = p.muted, fontSize = 11.sp)
                        }
                    } else {
                        history.take(3).forEach { track ->
                            Row(
                                Modifier.fillMaxWidth().weight(1f)
                                    .clip(RoundedCornerShape(10.dp)).clickable { onPlay(track) }.padding(4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Cover(track.thumbnailUrl, track.title, p, 46.dp)
                                Spacer(Modifier.width(9.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(track.title, color = p.text, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(track.artist, color = p.muted, fontSize = 9.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                                Icon(Icons.Filled.PlayArrow, "Reproduzir", tint = p.accent, modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                    if (connected) Text(
                        if (loading) "Sincronizando…" else "Atualizar biblioteca",
                        modifier = Modifier.clickable(enabled = !loading, onClick = onRefresh).padding(top = 2.dp),
                        color = p.muted, fontSize = 10.sp,
                    )
                }
            }
            }
        }

        error?.let { message ->
            item { Text(message, color = Color(0xFFFF6B6B), fontSize = 11.sp) }
        }

        item {
            KodaSectionTitle(p, "Tocadas recentemente")
            Spacer(Modifier.height(8.dp))
            if (history.isEmpty()) {
                KodaEmptyStrip(
                    p = p,
                    text = if (connected) {
                        "Seu histórico aparecerá aqui."
                    } else {
                        "Entre na sua conta para trazer seu histórico e biblioteca."
                    },
                )
            } else {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(history.take(9), key = { it.videoId }) { track ->
                        KodaTrackCard(p, track) { onPlay(track) }
                    }
                }
            }
        }

        if (playlists.isNotEmpty()) {
            item {
                KodaSectionTitle(p, "Suas playlists")
                Spacer(Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(playlists.take(8), key = { it.playlistId }) { playlist ->
                        KodaPlaylistCard(p, playlist) { onOpenPlaylist(playlist) }
                    }
                }
            }
        }

        if (discoveryLoading && discoveryShelves.isEmpty()) {
            item {
                KodaSectionTitle(p, "Descobrir")
                Spacer(Modifier.height(8.dp))
                KodaEmptyStrip(p, "Carregando recomendações do YouTube Music…")
            }
        } else {
            discoveryShelves.take(4).forEach { shelf ->
                item(key = "home-${shelf.title}") {
                    KodaDiscoveryShelf(
                        p = p,
                        shelf = shelf,
                        onItem = { onShelfItem(it, shelf.items) },
                    )
                }
            }
        }

        discoveryError?.let { message ->
            item { Text(message, color = p.muted, fontSize = 10.sp) }
        }

        if (liked.isNotEmpty()) {
            item {
                KodaSectionTitle(p, "Músicas curtidas")
                Spacer(Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(liked.take(9), key = { it.videoId }) { track ->
                        KodaTrackCard(p, track) { onPlayLiked(track) }
                    }
                }
            }
        }

        item { Spacer(Modifier.height(104.dp)) }
    }
}

@Composable
private fun KodaExploreView(
    p: Palette,
    loading: Boolean,
    error: String?,
    sections: List<YouTubeMusicSearchClient.MoodGenreSection>,
    onOpen: (YouTubeMusicSearchClient.MoodGenre) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 4.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        item {
            Column(Modifier.padding(horizontal = 4.dp, vertical = 4.dp)) {
                Text("Explorar", color = p.text, fontSize = 27.sp, fontWeight = FontWeight.Black)
                Text(
                    "Estados, momentos e gêneros do YouTube Music.",
                    color = p.muted,
                    fontSize = 11.sp,
                )
            }
        }

        when {
            loading && sections.isEmpty() -> item {
                Box(Modifier.fillMaxWidth().height(220.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = p.accent)
                }
            }

            error != null && sections.isEmpty() -> item {
                KodaEmptyStrip(p, error)
            }

            sections.isEmpty() -> item {
                KodaEmptyStrip(p, "Nenhuma categoria foi retornada agora.")
            }

            else -> sections.forEach { section ->
                item(key = "explore-title-${section.title}") {
                    Text(section.title, color = p.text, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                }
                section.items.chunked(3).forEachIndexed { rowIndex, row ->
                    item(key = "explore-${section.title}-$rowIndex") {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            row.forEach { mood ->
                                KodaMoodCard(
                                    p = p,
                                    mood = mood,
                                    genre = section.title.contains("gênero", ignoreCase = true),
                                    modifier = Modifier.weight(1f),
                                    onClick = { onOpen(mood) },
                                )
                            }
                            repeat(3 - row.size) {
                                Spacer(Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }

        item { Spacer(Modifier.height(104.dp)) }
    }
}

@Composable
private fun KodaMoodCard(
    p: Palette,
    mood: YouTubeMusicSearchClient.MoodGenre,
    genre: Boolean = false,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val title = mood.title.lowercase()
    val illustration = when {
        listOf("sono", "noite", "relax", "calma").any(title::contains) -> Icons.Filled.DarkMode
        listOf("amor", "romance", "coração").any(title::contains) -> Icons.Filled.Favorite
        listOf("game", "energia", "treino", "foco").any(title::contains) -> Icons.Filled.Speed
        listOf("sol", "alegre", "verão", "dia").any(title::contains) -> Icons.Filled.LightMode
        else -> Icons.Filled.MusicNote
    }
    val artwork = when (illustration) {
        Icons.Filled.DarkMode -> "illustrations/night.jpg"
        Icons.Filled.Favorite -> "illustrations/heart.jpg"
        Icons.Filled.Speed -> "illustrations/energy.jpg"
        Icons.Filled.LightMode -> "illustrations/sun.jpg"
        else -> "illustrations/music.jpg"
    }

    Card(
        modifier = modifier.height(116.dp).clickable(onClick = onClick),
        shape = RoundedCornerShape(15.dp),
        colors = CardDefaults.cardColors(containerColor = p.surface),
        border = BorderStroke(1.dp, p.border.copy(alpha = .42f)),
    ) {
        Box(Modifier.fillMaxSize()) {
            if (genre) {
                KodaGenreArtwork(mood.title)
            } else {
                Image(painterResource(artwork), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            }
            Box(Modifier.fillMaxSize().background(Brush.horizontalGradient(listOf(Color(0xE8090911), Color(0x93090911), Color.Transparent))))
            Text(
                mood.title,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Black,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.align(Alignment.TopStart).padding(15.dp).fillMaxWidth(.66f),
            )
        }
    }
}

/** Lightweight, deterministic artwork: every genre gets its own palette and waveform. */
@Composable
private fun KodaGenreArtwork(title: String) {
    val seed = title.fold(0L) { value, char -> (value * 31 + char.code) and 0x7FFFFFFF }
    val colors = listOf(
        Color(0xFF844AF1), Color(0xFFEF697D), Color(0xFF4FC7D6),
        Color(0xFFF3A14C), Color(0xFF78BD8B), Color(0xFF627ADF),
        Color(0xFFD77AC7), Color(0xFFDBB65B), Color(0xFF5CACB1),
    )
    val primary = colors[(seed % colors.size).toInt()]
    val secondary = colors[((seed / 7 + 3) % colors.size).toInt()]
    Canvas(Modifier.fillMaxSize()) {
        drawRect(brush = Brush.linearGradient(listOf(Color(0xFF14131E), primary.copy(alpha = .68f), secondary.copy(alpha = .85f))))
        val unit = size.minDimension
        val center = Offset(size.width * (.72f + (seed % 5) * .035f), size.height * .67f)
        for (ring in 0..3) {
            drawCircle(
                color = Color.White.copy(alpha = .09f + ring * .03f),
                radius = unit * (.25f + ring * .18f),
                center = center,
                style = Stroke(width = 1.5f + ring),
            )
        }
        val bars = 12 + (seed % 9).toInt()
        for (index in 0 until bars) {
            val x = size.width * (.32f + index.toFloat() / bars * .68f)
            val variation = ((seed / (index + 1) + index * 17) % 71).toFloat() / 100f
            val half = size.height * (.10f + variation * .26f)
            drawLine(
                color = Color.White.copy(alpha = .18f + variation * .24f),
                start = Offset(x, size.height * .58f - half),
                end = Offset(x, size.height * .58f + half),
                strokeWidth = unit * .035f,
                cap = androidx.compose.ui.graphics.StrokeCap.Round,
            )
        }
        drawCircle(primary.copy(alpha = .25f), unit * .45f, Offset(size.width * .95f, 0f))
    }
}

@Composable
private fun KodaMoodDetailView(
    p: Palette,
    mood: YouTubeMusicSearchClient.MoodGenre,
    loading: Boolean,
    error: String?,
    shelves: List<YouTubeMusicSearchClient.HomeShelf>,
    onBack: () -> Unit,
    onShelfItem: (YouTubeMusicSearchClient.ShelfItem, List<YouTubeMusicSearchClient.ShelfItem>) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Filled.ArrowBack, "Voltar", tint = p.text)
                }
                Column {
                    Text(mood.title, color = p.text, fontSize = 25.sp, fontWeight = FontWeight.Black)
                    Text("Explorar por clima, momento ou gênero", color = p.muted, fontSize = 10.sp)
                }
            }
        }

        when {
            loading -> item {
                Box(Modifier.fillMaxWidth().height(220.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = p.accent)
                }
            }
            error != null && shelves.isEmpty() -> item { KodaEmptyStrip(p, error) }
            shelves.isEmpty() -> item { KodaEmptyStrip(p, "Esta categoria não retornou conteúdo agora.") }
            else -> shelves.forEach { shelf ->
                item(key = "mood-${shelf.title}") {
                    KodaDiscoveryShelf(
                        p = p,
                        shelf = shelf,
                        onItem = { onShelfItem(it, shelf.items) },
                    )
                }
            }
        }

        item { Spacer(Modifier.height(104.dp)) }
    }
}

@Composable
private fun KodaSearchView(
    p: Palette,
    query: String,
    filter: YouTubeMusicSearchClient.SearchFilter,
    results: List<YouTubeMusicSearchClient.SearchEntry>,
    loading: Boolean,
    error: String?,
    favorites: List<String>,
    onFilter: (YouTubeMusicSearchClient.SearchFilter) -> Unit,
    onPlay: (YouTubeMusicSearchClient.Track) -> Unit,
    onBrowse: (YouTubeMusicSearchClient.BrowseItem) -> Unit,
) {
    Column(
        Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(
                    if (query.isBlank()) "Buscar" else "Resultados",
                    color = p.text,
                    fontSize = 23.sp,
                    fontWeight = FontWeight.Black,
                )
                Text(
                    if (query.isBlank()) "Músicas, artistas, álbuns e playlists"
                    else "“$query”",
                    color = p.muted,
                    fontSize = 11.sp,
                )
            }
        }

        LazyRow(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            items(YouTubeMusicSearchClient.SearchFilter.entries.toList()) { option ->
                val selected = option == filter
                Surface(
                    modifier = Modifier.clickable { onFilter(option) },
                    color = if (selected) p.accent.copy(alpha = .18f) else p.surface,
                    shape = RoundedCornerShape(50),
                    border = BorderStroke(
                        1.dp,
                        if (selected) p.accent else p.border.copy(alpha = .55f),
                    ),
                ) {
                    Text(
                        option.label,
                        color = if (selected) p.text else p.muted,
                        fontSize = 10.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                    )
                }
            }
        }

        when {
            loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = p.accent)
            }

            error != null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(error, color = p.muted)
            }

            query.isBlank() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Digite algo na busca acima para começar.", color = p.muted)
            }

            results.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Nenhum resultado encontrado.", color = p.muted)
            }

            else -> LazyColumn(
                verticalArrangement = Arrangement.spacedBy(7.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                items(results, key = { it.stableId }) { entry ->
                    entry.track?.let { track ->
                        TrackRow(
                            p = p,
                            track = track,
                            favorite = favorites.contains(track.videoId),
                            onPlay = { onPlay(track) },
                        )
                    }
                    entry.browse?.let { item ->
                        KodaBrowseResultRow(
                            p = p,
                            item = item,
                            onClick = { onBrowse(item) },
                        )
                    }
                }
                item { Spacer(Modifier.height(104.dp)) }
            }
        }
    }
}

@Composable
private fun KodaBrowseResultRow(
    p: Palette,
    item: YouTubeMusicSearchClient.BrowseItem,
    onClick: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(15.dp))
            .background(p.surfaceAlt)
            .border(1.dp, p.border.copy(alpha = .30f), RoundedCornerShape(15.dp))
            .clickable(onClick = onClick)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(54.dp)
                .clip(if (item.kind == YouTubeMusicSearchClient.BrowseKind.ARTIST) CircleShape else RoundedCornerShape(11.dp))
                .background(p.surface),
            contentAlignment = Alignment.Center,
        ) {
            if (!item.thumbnailUrl.isNullOrBlank()) {
                AsyncImage(
                    model = thumbnailUrlForSize(item.thumbnailUrl, 180),
                    contentDescription = item.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            } else {
                Icon(Icons.Filled.MusicNote, null, tint = p.accent)
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                item.title,
                color = p.text,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                item.subtitle ?: when (item.kind) {
                    YouTubeMusicSearchClient.BrowseKind.ARTIST -> "Artista"
                    YouTubeMusicSearchClient.BrowseKind.ALBUM -> "Álbum"
                    YouTubeMusicSearchClient.BrowseKind.PLAYLIST -> "Playlist"
                    YouTubeMusicSearchClient.BrowseKind.OTHER -> "YouTube Music"
                },
                color = p.muted,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Text(
            when (item.kind) {
                YouTubeMusicSearchClient.BrowseKind.ARTIST -> "ARTISTA"
                YouTubeMusicSearchClient.BrowseKind.ALBUM -> "ÁLBUM"
                YouTubeMusicSearchClient.BrowseKind.PLAYLIST -> "PLAYLIST"
                YouTubeMusicSearchClient.BrowseKind.OTHER -> "ABRIR"
            },
            color = p.accent,
            fontSize = 9.sp,
            fontWeight = FontWeight.Black,
        )
    }
}

@Composable
private fun KodaLibraryHubView(
    p: Palette,
    connected: Boolean,
    loading: Boolean,
    history: List<YouTubeMusicSearchClient.Track>,
    liked: List<YouTubeMusicSearchClient.Track>,
    playlists: List<YouTubeMusicSearchClient.Playlist>,
    onNavigate: (Section) -> Unit,
    onLogin: () -> Unit,
    onRefresh: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text("Biblioteca", color = p.text, fontSize = 27.sp, fontWeight = FontWeight.Black)
                    Text("Tudo que é seu, organizado em um só lugar.", color = p.muted, fontSize = 11.sp)
                }
                if (connected) {
                    OutlinedButton(
                        onClick = onRefresh,
                        enabled = !loading,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, p.border.copy(alpha = .65f)),
                    ) {
                        Text(if (loading) "Sincronizando…" else "Atualizar", color = p.text, fontSize = 10.sp)
                    }
                } else {
                    Button(
                        onClick = onLogin,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = p.accent),
                    ) {
                        Text("Conectar conta", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth().height(98.dp),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = p.surface),
                border = BorderStroke(1.dp, p.border.copy(alpha = .42f)),
            ) {
                Row(
                    Modifier
                        .fillMaxSize()
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    p.accent2.copy(alpha = .22f),
                                    p.surface,
                                ),
                            ),
                        )
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Seu Replay", color = p.text, fontSize = 18.sp, fontWeight = FontWeight.Black)
                        Text(
                            "Estatísticas locais de escuta entrarão aqui sem mandar seus dados para fora do PC.",
                            color = p.muted,
                            fontSize = 10.sp,
                        )
                    }
                    Surface(
                        color = p.accent.copy(alpha = .13f),
                        shape = RoundedCornerShape(50),
                        border = BorderStroke(1.dp, p.accent.copy(alpha = .32f)),
                    ) {
                        Text(
                            "EM DESENVOLVIMENTO",
                            color = p.accent,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Black,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        )
                    }
                }
            }
        }

        item {
            Text("Sua coleção", color = p.text, fontSize = 17.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                KodaLibraryTile(
                    p = p,
                    title = "Curtidas",
                    subtitle = "${liked.size} músicas",
                    icon = Icons.Filled.Favorite,
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigate(Section.LIKED) },
                )
                KodaLibraryTile(
                    p = p,
                    title = "Histórico",
                    subtitle = "${history.size} itens recentes",
                    icon = Icons.Filled.Article,
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigate(Section.HISTORY) },
                )
                KodaLibraryTile(
                    p = p,
                    title = "Playlists",
                    subtitle = "${playlists.size} coleções",
                    icon = Icons.Filled.QueueMusic,
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigate(Section.PLAYLISTS) },
                )
            }
        }

        item {
            Text("Em breve", color = p.text, fontSize = 17.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                KodaLibraryTile(
                    p = p,
                    title = "Replay",
                    subtitle = "Estatísticas de escuta",
                    icon = Icons.Filled.MusicNote,
                    modifier = Modifier.weight(1f),
                    status = "PLANEJADO",
                    onClick = null,
                )
            }
        }

        item { Spacer(Modifier.height(104.dp)) }
    }
}

@Composable
private fun KodaLibraryTile(
    p: Palette,
    title: String,
    subtitle: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    status: String? = null,
    onClick: (() -> Unit)?,
) {
    Card(
        modifier = modifier
            .height(128.dp)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = p.surface),
        border = BorderStroke(1.dp, p.border.copy(alpha = .42f)),
    ) {
        Column(
            Modifier.fillMaxSize().padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Surface(
                modifier = Modifier.size(38.dp),
                color = p.accent.copy(alpha = .14f),
                shape = RoundedCornerShape(12.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, null, tint = p.accent, modifier = Modifier.size(20.dp))
                }
            }

            Column {
                Text(title, color = p.text, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Text(subtitle, color = p.muted, fontSize = 9.sp)
                status?.let {
                    Spacer(Modifier.height(4.dp))
                    Text(it, color = p.accent, fontSize = 7.sp, fontWeight = FontWeight.Black)
                }
            }
        }
    }
}

@Composable
private fun KodaTrackCollectionView(
    p: Palette,
    title: String,
    subtitle: String,
    tracks: List<YouTubeMusicSearchClient.Track>,
    loading: Boolean,
    connected: Boolean,
    emptyMessage: String,
    onPlay: (YouTubeMusicSearchClient.Track) -> Unit,
    onLogin: () -> Unit,
    onRefresh: () -> Unit,
) {
    Column(
        Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(title, color = p.text, fontSize = 24.sp, fontWeight = FontWeight.Black)
                Text(subtitle, color = p.muted, fontSize = 10.sp)
            }
            if (connected) {
                OutlinedButton(
                    onClick = onRefresh,
                    enabled = !loading,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, p.border.copy(alpha = .60f)),
                ) {
                    Text("Atualizar", color = p.text, fontSize = 10.sp)
                }
            } else {
                Button(
                    onClick = onLogin,
                    colors = ButtonDefaults.buttonColors(containerColor = p.accent),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Text("Conectar conta")
                }
            }
        }

        when {
            loading && tracks.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = p.accent)
            }
            tracks.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(emptyMessage, color = p.muted)
            }
            else -> LazyColumn(
                verticalArrangement = Arrangement.spacedBy(7.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                items(tracks, key = { it.videoId }) { track ->
                    TrackRow(p, track, title.contains("curt", ignoreCase = true)) { onPlay(track) }
                }
                item { Spacer(Modifier.height(104.dp)) }
            }
        }
    }
}

@Composable
private fun ListenTogetherView(
    p: Palette,
    server: String,
    onServer: (String) -> Unit,
    code: String,
    onCode: (String) -> Unit,
    room: ListenTogetherDesktop.Room,
    busy: Boolean,
    error: String?,
    onEnter: (String?) -> Unit,
    onLeave: () -> Unit,
) {
    Column(Modifier.fillMaxSize().padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("Ouvir juntos", color = p.text, fontSize = 26.sp, fontWeight = FontWeight.Bold)
        Text("Compartilhe um código e controle a reprodução com outras pessoas.", color = p.muted)
        if (room.code.isNotBlank()) {
            Text("Código da sala: ${room.code}", color = p.accent, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Text(if (room.connected) "Conectado" else "Reconecte à sala", color = p.muted)
            room.members.forEach { Text(it, color = p.text) }
            room.error?.let { Text(it, color = Color(0xFFFF8080)) }
            OutlinedButton(onClick = onLeave) { Text("Sair da sala") }
        } else {
            TextField(value = server, onValueChange = onServer, label = { Text("Endereço do servidor de salas") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            Text("Use o endereço HTTPS do servidor Ouvir Juntos do Koda.", color = p.muted, fontSize = 11.sp)
            Button(onClick = { onEnter(null) }, enabled = !busy && server.isNotBlank()) { Text("Criar sala") }
            TextField(value = code, onValueChange = { onCode(it.take(6).uppercase()) }, label = { Text("Código de seis caracteres") }, singleLine = true)
            OutlinedButton(onClick = { onEnter(code) }, enabled = !busy && code.length == 6 && server.isNotBlank()) { Text("Entrar na sala") }
        }
        error?.let { Text(it, color = Color(0xFFFF8080)) }
    }
}

@Composable
private fun KodaBrowseDetailView(
    p: Palette,
    item: YouTubeMusicSearchClient.BrowseItem,
    loading: Boolean,
    error: String?,
    tracks: List<YouTubeMusicSearchClient.Track>,
    shelves: List<YouTubeMusicSearchClient.HomeShelf>,
    onBack: () -> Unit,
    onPlay: (YouTubeMusicSearchClient.Track) -> Unit,
    onShelfItem: (YouTubeMusicSearchClient.ShelfItem, List<YouTubeMusicSearchClient.ShelfItem>) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Filled.ArrowBack, "Voltar", tint = p.text)
                }
                Box(
                    Modifier
                        .size(104.dp)
                        .clip(
                            if (item.kind == YouTubeMusicSearchClient.BrowseKind.ARTIST) {
                                CircleShape
                            } else {
                                RoundedCornerShape(18.dp)
                            },
                        )
                        .background(p.surfaceAlt),
                    contentAlignment = Alignment.Center,
                ) {
                    if (!item.thumbnailUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = thumbnailUrlForSize(item.thumbnailUrl, 420),
                            contentDescription = item.title,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                        )
                    } else {
                        Icon(Icons.Filled.MusicNote, null, tint = p.accent, modifier = Modifier.size(36.dp))
                    }
                }
                Spacer(Modifier.width(18.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        when (item.kind) {
                            YouTubeMusicSearchClient.BrowseKind.ARTIST -> "ARTISTA"
                            YouTubeMusicSearchClient.BrowseKind.ALBUM -> "ÁLBUM"
                            YouTubeMusicSearchClient.BrowseKind.PLAYLIST -> "PLAYLIST"
                            YouTubeMusicSearchClient.BrowseKind.OTHER -> "YOUTUBE MUSIC"
                        },
                        color = p.accent,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                    )
                    Text(
                        item.title,
                        color = p.text,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Black,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    item.subtitle?.let {
                        Text(it, color = p.muted, fontSize = 11.sp, maxLines = 2)
                    }
                    if (tracks.isNotEmpty()) {
                        Spacer(Modifier.height(10.dp))
                        Button(
                            onClick = { onPlay(tracks.first()) },
                            colors = ButtonDefaults.buttonColors(containerColor = p.accent),
                            shape = RoundedCornerShape(13.dp),
                        ) {
                            Icon(Icons.Filled.PlayArrow, null)
                            Spacer(Modifier.width(6.dp))
                            Text("Tocar")
                        }
                    }
                }
            }
        }

        error?.let { message ->
            item { Text(message, color = Color(0xFFFF6B6B), fontSize = 11.sp) }
        }

        if (loading) {
            item {
                Box(Modifier.fillMaxWidth().height(220.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = p.accent)
                }
            }
        } else {
            if (tracks.isNotEmpty()) {
                item {
                    Text(
                        if (item.kind == YouTubeMusicSearchClient.BrowseKind.ARTIST) "Músicas mais tocadas" else "Faixas",
                        color = p.text,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
                items(tracks.take(20), key = { "detail-${it.videoId}" }) { track ->
                    TrackRow(p, track, false) { onPlay(track) }
                }
            }

            shelves.take(5).forEach { shelf ->
                item(key = "detail-shelf-${shelf.title}") {
                    KodaDiscoveryShelf(
                        p = p,
                        shelf = shelf,
                        onItem = { onShelfItem(it, shelf.items) },
                    )
                }
            }

            if (tracks.isEmpty() && shelves.isEmpty() && error == null) {
                item { KodaEmptyStrip(p, "Esta página não retornou conteúdo reproduzível agora.") }
            }
        }

        item { Spacer(Modifier.height(104.dp)) }
    }
}

@Composable
private fun KodaDiscoveryShelf(
    p: Palette,
    shelf: YouTubeMusicSearchClient.HomeShelf,
    onItem: (YouTubeMusicSearchClient.ShelfItem) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Column {
            Text(shelf.title, color = p.text, fontSize = 17.sp, fontWeight = FontWeight.Bold)
            shelf.subtitle?.takeIf { it.isNotBlank() }?.let {
                Text(it, color = p.muted, fontSize = 9.sp, maxLines = 1)
            }
        }

        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            itemsIndexed(shelf.items.take(10)) { index, item ->
                KodaShelfItemCard(
                    p = p,
                    item = item,
                    onClick = { onItem(item) },
                )
            }
        }
    }
}

@Composable
private fun KodaShelfItemCard(
    p: Palette,
    item: YouTubeMusicSearchClient.ShelfItem,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier.width(142.dp).clickable(onClick = onClick),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Card(
            modifier = Modifier.size(142.dp),
            shape = RoundedCornerShape(13.dp),
            colors = CardDefaults.cardColors(containerColor = p.surfaceAlt),
            border = BorderStroke(1.dp, p.border.copy(alpha = .36f)),
        ) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                if (!item.thumbnailUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = thumbnailUrlForSize(item.thumbnailUrl, 320),
                        contentDescription = item.title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )
                } else {
                    Icon(Icons.Filled.MusicNote, null, tint = p.accent, modifier = Modifier.size(28.dp))
                }

                if (!item.videoId.isNullOrBlank()) {
                    Surface(
                        modifier = Modifier.align(Alignment.BottomEnd).padding(7.dp).size(30.dp),
                        color = Color.Black.copy(alpha = .68f),
                        shape = CircleShape,
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Filled.PlayArrow,
                                null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                }
            }
        }
        Text(
            item.title,
            color = p.text,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        item.subtitle?.let {
            Text(
                it,
                color = p.muted,
                fontSize = 9.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun TrackRow(p: Palette, track: YouTubeMusicSearchClient.Track, favorite: Boolean, onPlay: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(15.dp)).background(Brush.horizontalGradient(listOf(p.surfaceAlt, p.surfaceAlt))).border(1.dp, p.border.copy(alpha = 0.30f), RoundedCornerShape(15.dp)).clickable(onClick = onPlay).padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Cover(track.thumbnailUrl, track.title, p, 52.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(track.title, color = p.text, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(track.artist, color = p.muted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        track.durationText?.let { Text(it, color = p.muted, fontSize = 11.sp) }
        Spacer(Modifier.width(10.dp))
        Icon(if (favorite) Icons.Filled.Favorite else Icons.Filled.PlayArrow, null, tint = p.accent)
    }
}

private fun thumbnailUrlForSize(
    url: String?,
    targetPx: Int,
): String? {
    val raw = url
        ?.trim()
        ?.takeIf { it.isNotBlank() }
        ?: return null

    val normalized = when {
        raw.startsWith("//") -> "https:$raw"
        raw.startsWith("http://") -> "https://${raw.removePrefix("http://")}"
        else -> raw
    }

    val lower = normalized.lowercase()
    val lastSlash = normalized.lastIndexOf('/')
    val lastEquals = normalized.lastIndexOf('=')

    if (
        "googleusercontent.com" in lower ||
        "ggpht.com" in lower
    ) {
        val base =
            if (lastEquals > lastSlash) {
                normalized.substring(0, lastEquals)
            } else {
                normalized
            }

        val safe = targetPx.coerceIn(96, 1024)
        return "$base=w$safe-h$safe-l90-rj"
    }

    return normalized
}

private fun highResolutionThumbnailUrl(url: String?): String? =
    thumbnailUrlForSize(url, 1024)

@Composable
private fun Cover(url: String?, title: String?, p: Palette, size: androidx.compose.ui.unit.Dp) {
    Box(
        Modifier.size(size).clip(RoundedCornerShape(12.dp)).background(p.surfaceAlt),
        contentAlignment = Alignment.Center,
    ) {
        val cardUrl = thumbnailUrlForSize(url, 320)
        if (!cardUrl.isNullOrBlank()) {
            AsyncImage(
                model = cardUrl,
                contentDescription = title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        } else {
            Icon(Icons.Filled.MusicNote, null, tint = p.accent)
        }
    }
}

@Composable
private fun PlayerTimeline(
    p: Palette,
    positionMillis: Long,
    durationMillis: Long,
    compact: Boolean,
    onSeek: (Float) -> Unit,
) {
    val safeDuration = durationMillis.coerceAtLeast(0L)
    val safePosition =
        if (safeDuration > 0L) {
            positionMillis.coerceIn(0L, safeDuration)
        } else {
            positionMillis.coerceAtLeast(0L)
        }

    val liveValue =
        if (safeDuration > 0L) {
            (safePosition.toFloat() / safeDuration.toFloat()).coerceIn(0f, 1f)
        } else {
            0f
        }

    var dragging by remember { mutableStateOf(false) }
    var dragValue by remember { mutableStateOf(liveValue) }

    LaunchedEffect(liveValue, dragging) {
        if (!dragging) {
            dragValue = liveValue
        }
    }

    val displayedPosition =
        if (dragging && safeDuration > 0L) {
            (safeDuration * dragValue).toLong()
        } else {
            safePosition
        }

    val timeColor = p.text.copy(alpha = 0.86f)
    val timeSize = if (compact) 12.sp else 13.sp
    val labelWidth = if (compact) 48.dp else 58.dp

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            formatTime(displayedPosition),
            color = timeColor,
            fontSize = timeSize,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.width(labelWidth),
        )

        Slider(
            value = if (dragging) dragValue else liveValue,
            onValueChange = { value ->
                dragValue = value.coerceIn(0f, 1f)
                dragging = true
            },
            onValueChangeFinished = {
                if (safeDuration > 0L) {
                    onSeek(dragValue.coerceIn(0f, 1f))
                }
                dragging = false
            },
            enabled = safeDuration > 0L,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = if (compact) 8.dp else 12.dp),
        )

        Text(
            if (safeDuration > 0L) formatTime(safeDuration) else "—:—",
            color = timeColor,
            fontSize = timeSize,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.width(labelWidth),
        )
    }
}


@Composable
private fun MiniPlayer(
    p: Palette,
    track: YouTubeMusicSearchClient.Track?,
    state: DesktopAudioPlayer.Snapshot,
    favorites: List<String>,
    liquidGlass: Boolean,
    glassBackdrop: Backdrop,
    shuffle: Boolean,
    repeat: Boolean,
    onShuffle: () -> Unit,
    onRepeat: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onToggle: () -> Unit,
    onStop: () -> Unit,
    onSeek: (Float) -> Unit,
    onVolume: (Float) -> Unit,
    onMute: () -> Unit,
    onQueue: () -> Unit,
    onFavorite: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onExpand: () -> Unit,
) {
    val playerShape = RoundedCornerShape(18.dp)
    val durationMillis =
        state.durationMillis
            .takeIf { it > 0L }
            ?: durationTextToMillis(track?.durationText)

    // Solid controls keep the artwork, title and timeline legible.
    LiquidGlassSurface(
        enabled = false,
        backdrop = glassBackdrop,
        modifier = Modifier.fillMaxWidth().height(112.dp),
        shape = playerShape,
        solidColor = Color(0xFF0B0C11),
        tint = p.surface,
        borderColor = p.border.copy(alpha = 0.72f),
        accent = p.accent,
        refractionHeight = 14.dp,
        refractionAmount = 24.dp,
    ) {
        Column(
            Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 7.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                Modifier.fillMaxWidth().height(58.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    Modifier.weight(1.1f),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Cover(track?.thumbnailUrl, track?.title, p, 48.dp)
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            track?.title ?: "Nenhuma música tocando",
                            color = p.text,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            when {
                                state.state == DesktopAudioPlayer.State.RESOLVING -> state.message ?: "Preparando áudio…"
                                state.state == DesktopAudioPlayer.State.ERROR -> state.message ?: "Erro no player"
                                else -> track?.artist ?: "Escolha uma música"
                            },
                            color = if (state.state == DesktopAudioPlayer.State.ERROR) Color(0xFFFF6B6B) else p.muted,
                            fontSize = 9.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    IconButton(onClick = onFavorite, enabled = track != null) {
                        Icon(
                            if (track != null && favorites.contains(track.videoId)) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                            "Favorito",
                            tint = if (track != null && favorites.contains(track.videoId)) p.accent else p.muted,
                            modifier = Modifier.size(19.dp),
                        )
                    }
                }

                Row(
                    Modifier.weight(1f),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = onShuffle) {
                        Icon(Icons.Filled.Shuffle, null, tint = if (shuffle) p.accent else p.muted, modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = onPrevious, enabled = track != null) {
                        Icon(Icons.Filled.SkipPrevious, null, tint = p.text)
                    }
                    Box(
                        Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(if (track != null) p.accent else p.border)
                            .clickable(enabled = track != null, onClick = onToggle),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (state.state == DesktopAudioPlayer.State.RESOLVING) {
                            CircularProgressIndicator(Modifier.size(18.dp), color = p.onAccent(), strokeWidth = 2.dp)
                        } else {
                            Icon(
                                if (state.state == DesktopAudioPlayer.State.PLAYING) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                null,
                                tint = p.onAccent(),
                                modifier = Modifier.size(24.dp),
                            )
                        }
                    }
                    IconButton(onClick = onNext, enabled = track != null) {
                        Icon(Icons.Filled.SkipNext, null, tint = p.text)
                    }
                    IconButton(onClick = onRepeat) {
                        Icon(
                            if (repeat) Icons.Filled.RepeatOne else Icons.Filled.Repeat,
                            null,
                            tint = if (repeat) p.accent else p.muted,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }

                Row(
                    Modifier.weight(.9f),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = onMute) {
                        Icon(
                            if (state.muted) Icons.Filled.VolumeOff else Icons.Filled.VolumeUp,
                            null,
                            tint = p.muted,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                    Slider(
                        value = state.volume.toFloat(),
                        onValueChange = onVolume,
                        modifier = Modifier.width(88.dp),
                    )
                    IconButton(onClick = onExpand, enabled = track != null) {
                        Icon(Icons.Filled.Fullscreen, "Tela cheia", tint = p.text, modifier = Modifier.size(19.dp))
                    }
                    IconButton(onClick = onQueue) {
                        Icon(Icons.Filled.QueueMusic, "Fila", tint = p.muted, modifier = Modifier.size(19.dp))
                    }
                }
            }

            PlayerTimeline(
                p = p,
                positionMillis = state.positionMillis,
                durationMillis = durationMillis,
                compact = true,
                onSeek = onSeek,
            )
        }
    }
}

@Composable
private fun FullPlayerScreen(
    p: Palette,
    track: YouTubeMusicSearchClient.Track,
    state: DesktopAudioPlayer.Snapshot,
    lyricsVisible: Boolean,
    favorite: Boolean,
    shuffle: Boolean,
    repeat: Boolean,
    onClose: () -> Unit,
    onLyricsToggle: () -> Unit,
    onFavorite: () -> Unit,
    onShuffle: () -> Unit,
    onRepeat: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onToggle: () -> Unit,
    onSeek: (Float) -> Unit,
    onVolume: (Float) -> Unit,
    onMute: () -> Unit,
) {
    var lyrics by remember(track.videoId) { mutableStateOf<LyricsClient.Lyrics?>(null) }
    var lyricsLoading by remember(track.videoId) { mutableStateOf(false) }
    var lyricsLoaded by remember(track.videoId) { mutableStateOf(false) }
    var lyricsError by remember(track.videoId) { mutableStateOf<String?>(null) }

    val durationMillis =
        state.durationMillis
            .takeIf { it > 0L }
            ?: durationTextToMillis(track.durationText)

    LaunchedEffect(lyricsVisible, track.videoId) {
        if (lyricsVisible && !lyricsLoaded && !lyricsLoading) {
            lyricsLoading = true
            lyricsError = null
            runCatching { LyricsClient.fetch(track.title, track.artist) }
                .onSuccess { found ->
                    lyrics = found
                    if (found == null) {
                        lyricsError = "Letra não encontrada para esta faixa."
                    }
                }
                .onFailure {
                    lyricsError = it.message ?: "Não foi possível carregar a letra."
                }
            lyricsLoaded = true
            lyricsLoading = false
        }
    }

    val artwork = highResolutionThumbnailUrl(track.thumbnailUrl)

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFF050506),
    ) {
        Box(Modifier.fillMaxSize()) {
            if (!artwork.isNullOrBlank()) {
                AsyncImage(
                    model = artwork,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .blur(12.dp),
                    contentScale = ContentScale.Crop,
                    alpha = .82f,
                )
            }

            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                Color.Black.copy(alpha = .68f),
                                Color.Black.copy(alpha = .30f),
                                Color.Black.copy(alpha = .56f),
                            ),
                        ),
                    ),
            )

            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color.Black.copy(alpha = .22f),
                                Color.Black.copy(alpha = .08f),
                                Color.Black.copy(alpha = .75f),
                            ),
                        ),
                    ),
            )

            Column(
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = 28.dp, vertical = 22.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Brand(
                        p.copy(
                            text = Color.White,
                            muted = Color.White.copy(alpha = .64f),
                        ),
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        OutlinedButton(
                            onClick = onLyricsToggle,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(
                                1.dp,
                                if (lyricsVisible) p.accent else Color.White.copy(alpha = .20f),
                            ),
                        ) {
                            Icon(
                                Icons.Filled.Article,
                                null,
                                tint = if (lyricsVisible) p.accent else Color.White,
                                modifier = Modifier.size(17.dp),
                            )
                            Spacer(Modifier.width(7.dp))
                            Text("Letras", color = Color.White, fontSize = 11.sp)
                        }

                        Surface(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .clickable(onClick = onClose),
                            color = Color.Black.copy(alpha = .34f),
                            shape = CircleShape,
                            border = BorderStroke(1.dp, Color.White.copy(alpha = .16f)),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                Icons.Filled.FullscreenExit,
                                    "Fechar janela do player",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(34.dp, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(
                        modifier = Modifier.width(370.dp),
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Card(
                            modifier = Modifier.size(310.dp),
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF111116)),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = .13f)),
                        ) {
                            if (!artwork.isNullOrBlank()) {
                                AsyncImage(
                                    model = artwork,
                                    contentDescription = track.title,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop,
                                )
                            } else {
                                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Filled.MusicNote,
                                        null,
                                        tint = p.accent,
                                        modifier = Modifier.size(58.dp),
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(18.dp))

                        Text(
                            track.title,
                            color = Color.White,
                            fontSize = 29.sp,
                            fontWeight = FontWeight.Black,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )

                        Spacer(Modifier.height(5.dp))

                        Text(
                            track.artist,
                            color = Color.White.copy(alpha = .66f),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )

                        Spacer(Modifier.height(10.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Surface(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .clickable(onClick = onFavorite),
                                color = Color.Black.copy(alpha = .32f),
                                shape = CircleShape,
                                border = BorderStroke(1.dp, Color.White.copy(alpha = .14f)),
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        if (favorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                                        "Favorito",
                                        tint = if (favorite) p.accent else Color.White,
                                        modifier = Modifier.size(19.dp),
                                    )
                                }
                            }

                            state.streamInfo
                                ?.takeIf { it.isNotBlank() }
                                ?.let { info ->
                                    Surface(
                                        color = Color.Black.copy(alpha = .28f),
                                        shape = RoundedCornerShape(50),
                                        border = BorderStroke(1.dp, Color.White.copy(alpha = .10f)),
                                    ) {
                                        Text(
                                            info,
                                            color = Color.White.copy(alpha = .58f),
                                            fontSize = 9.sp,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        )
                                    }
                                }
                        }
                    }

                    if (lyricsVisible) {
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(.94f),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFF08090D).copy(alpha = .74f),
                            ),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = .14f)),
                        ) {
                            Column(
                                Modifier
                                    .fillMaxSize()
                                    .padding(20.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                Row(
                                    Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        "Letras",
                                        color = Color.White,
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Black,
                                    )
                                    lyrics?.source?.let { source ->
                                        Text(
                                            source,
                                            color = p.accent,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                        )
                                    }
                                }

                                when {
                                    lyricsLoading -> {
                                        Box(
                                            Modifier.fillMaxSize(),
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            CircularProgressIndicator(color = p.accent)
                                        }
                                    }

                                    lyricsError != null -> {
                                        Box(
                                            Modifier.fillMaxSize(),
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            Text(
                                                lyricsError ?: "",
                                                color = Color.White.copy(alpha = .58f),
                                                fontSize = 12.sp,
                                            )
                                        }
                                    }

                                    lyrics != null -> {
                                        LyricsBody(
                                            p = p.copy(
                                                text = Color.White,
                                                muted = Color.White.copy(alpha = .50f),
                                            ),
                                            lyrics = lyrics!!,
                                            positionMillis = state.positionMillis,
                                        )
                                    }

                                    else -> {
                                        Box(
                                            Modifier.fillMaxSize(),
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            Text(
                                                "As letras aparecerão aqui.",
                                                color = Color.White.copy(alpha = .52f),
                                                fontSize = 12.sp,
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        Card(
                            modifier = Modifier.weight(1f).fillMaxHeight(.94f),
                            shape = RoundedCornerShape(22.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF101018).copy(alpha = .78f)),
                            border = BorderStroke(1.dp, p.accent.copy(alpha = .24f)),
                        ) {
                          Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            if (!artwork.isNullOrBlank()) {
                                AsyncImage(
                                    model = artwork,
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize().blur(14.dp),
                                    contentScale = ContentScale.Crop,
                                    alpha = .28f,
                                )
                            }
                            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF15101F).copy(alpha = .55f), Color(0xFF08080D).copy(alpha = .93f)))))
                            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                Image(painterResource("branding/p-music-logo.png"), "Koda Music", modifier = Modifier.width(270.dp).height(72.dp), contentScale = ContentScale.Fit)
                                Text("Sua música ocupa o centro da cena.", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                OutlinedButton(onClick = onLyricsToggle, border = BorderStroke(1.dp, p.accent.copy(alpha = .65f))) {
                                    Icon(Icons.Filled.Article, null, tint = p.accent, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text("Mostrar letras", color = Color.White)
                                }
                            }
                          }
                        }
                    }
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF090A0E).copy(alpha = .86f),
                    ),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = .12f)),
                ) {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        PlayerTimeline(
                            p = p.copy(
                                text = Color.White,
                                muted = Color.White.copy(alpha = .55f),
                            ),
                            positionMillis = state.positionMillis,
                            durationMillis = durationMillis,
                            compact = true,
                            onSeek = onSeek,
                        )

                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = onShuffle) {
                                    Icon(
                                        Icons.Filled.Shuffle,
                                        "Aleatório",
                                        tint = if (shuffle) p.accent else Color.White.copy(alpha = .56f),
                                        modifier = Modifier.size(19.dp),
                                    )
                                }
                                IconButton(onClick = onRepeat) {
                                    Icon(
                                        if (repeat) Icons.Filled.RepeatOne else Icons.Filled.Repeat,
                                        "Repetir",
                                        tint = if (repeat) p.accent else Color.White.copy(alpha = .56f),
                                        modifier = Modifier.size(19.dp),
                                    )
                                }
                            }

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                IconButton(onClick = onPrevious) {
                                    Icon(
                                        Icons.Filled.SkipPrevious,
                                        "Anterior",
                                        tint = Color.White,
                                        modifier = Modifier.size(27.dp),
                                    )
                                }

                                Box(
                                    Modifier
                                        .size(50.dp)
                                        .clip(CircleShape)
                                        .background(Color.White)
                                        .clickable(onClick = onToggle),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    if (state.state == DesktopAudioPlayer.State.RESOLVING) {
                                        CircularProgressIndicator(
                                            Modifier.size(20.dp),
                                            color = p.accent,
                                            strokeWidth = 2.dp,
                                        )
                                    } else {
                                        Icon(
                                            if (state.state == DesktopAudioPlayer.State.PLAYING) {
                                                Icons.Filled.Pause
                                            } else {
                                                Icons.Filled.PlayArrow
                                            },
                                            "Play/Pause",
                                            tint = Color.Black,
                                            modifier = Modifier.size(28.dp),
                                        )
                                    }
                                }

                                IconButton(onClick = onNext) {
                                    Icon(
                                        Icons.Filled.SkipNext,
                                        "Próxima",
                                        tint = Color.White,
                                        modifier = Modifier.size(27.dp),
                                    )
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = onMute) {
                                    Icon(
                                        if (state.muted) Icons.Filled.VolumeOff else Icons.Filled.VolumeUp,
                                        "Volume",
                                        tint = Color.White.copy(alpha = .68f),
                                        modifier = Modifier.size(19.dp),
                                    )
                                }
                                Slider(
                                    value = state.volume.toFloat(),
                                    onValueChange = onVolume,
                                    modifier = Modifier.width(118.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LyricsBody(p: Palette, lyrics: LyricsClient.Lyrics, positionMillis: Long) {
    if (lyrics.syncedLines.isNotEmpty()) {
        val active = lyrics.syncedLines.indexOfLast { it.timeMillis <= positionMillis }.coerceAtLeast(0)
        LazyColumn(verticalArrangement = Arrangement.spacedBy(13.dp)) {
            itemsIndexed(lyrics.syncedLines) { index, line ->
                Text(
                    line.text,
                    color = if (index == active) p.accent else p.text.copy(alpha = if (index < active) .55f else .82f),
                    fontSize = if (index == active) 24.sp else 18.sp,
                    fontWeight = if (index == active) FontWeight.Bold else FontWeight.Medium,
                    lineHeight = 30.sp,
                )
            }
        }
    } else {
        val lines = lyrics.plainText.orEmpty().lineSequence().filter { it.isNotBlank() }.toList()
        LazyColumn(verticalArrangement = Arrangement.spacedBy(11.dp)) {
            items(lines) { line ->
                Text(line, color = p.text.copy(alpha = .88f), fontSize = 18.sp, lineHeight = 28.sp)
            }
        }
    }
}


@Composable
private fun KodaSectionTitle(
    p: Palette,
    title: String,
) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, color = p.text, fontSize = 17.sp, fontWeight = FontWeight.Bold)
        Text("Ver tudo  ›", color = p.muted, fontSize = 10.sp)
    }
}

@Composable
private fun KodaTrackCard(
    p: Palette,
    track: YouTubeMusicSearchClient.Track,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier.width(142.dp).clickable(onClick = onClick),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Card(
            modifier = Modifier.size(142.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = p.surfaceAlt),
            border = BorderStroke(1.dp, p.border.copy(alpha = .42f)),
        ) {
            Cover(track.thumbnailUrl, track.title, p, 142.dp)
        }
        Text(
            track.title,
            color = p.text,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            track.artist,
            color = p.muted,
            fontSize = 10.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun KodaPlaylistCard(
    p: Palette,
    playlist: YouTubeMusicSearchClient.Playlist,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier.width(148.dp).height(108.dp).clickable(onClick = onClick),
        shape = RoundedCornerShape(13.dp),
        colors = CardDefaults.cardColors(containerColor = p.surfaceAlt),
        border = BorderStroke(1.dp, p.border.copy(alpha = .45f)),
    ) {
        Box(Modifier.fillMaxSize().background(Brush.linearGradient(listOf(p.accent.copy(alpha = .38f), p.surfaceAlt, p.accent2.copy(alpha = .28f))))) {
            playlist.thumbnailUrl?.takeIf { it.isNotBlank() }?.let { art ->
                AsyncImage(model = art, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop, alpha = .34f)
            }
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = .88f)))))
            Icon(Icons.Filled.LibraryMusic, null, tint = p.accent, modifier = Modifier.align(Alignment.TopStart).padding(12.dp).size(27.dp))
            Column(Modifier.align(Alignment.BottomStart).padding(10.dp)) {
                Text(playlist.title, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                playlist.subtitle?.let { Text(it, color = Color.White.copy(alpha = .65f), fontSize = 9.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) }
            }
        }
    }
}

@Composable
private fun KodaEmptyStrip(
    p: Palette,
    text: String,
) {
    Card(
        modifier = Modifier.fillMaxWidth().height(72.dp),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = p.surface.copy(alpha = .75f)),
        border = BorderStroke(1.dp, p.border.copy(alpha = .36f)),
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.CenterStart) {
            Text(text, color = p.muted, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 16.dp))
        }
    }
}

@Composable
private fun PlaylistsView(
    p: Palette,
    connected: Boolean,
    loading: Boolean,
    error: String?,
    playlists: List<YouTubeMusicSearchClient.Playlist>,
    onRefresh: () -> Unit,
    onLogin: () -> Unit,
    onCreate: () -> Unit,
    onOpen: (YouTubeMusicSearchClient.Playlist) -> Unit,
) {
    if (!connected) {
        EmptyView(p, Icons.Filled.QueueMusic, "Playlists", "Conecte sua conta Google para carregar suas playlists do YouTube Music.")
        return
    }
    Card(modifier = Modifier.fillMaxSize(), shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = p.surface), border = BorderStroke(1.dp, p.border.copy(alpha = 0.75f))) {
        Column(Modifier.fillMaxSize().padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column { Text("Suas playlists", color = p.text, fontSize = 22.sp, fontWeight = FontWeight.Bold); Text("Biblioteca da conta Google conectada", color = p.muted, fontSize = 12.sp) }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = onCreate, enabled = !loading, shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = p.accent)) { Text("+ Nova playlist", fontSize = 12.sp) }
                    OutlinedButton(onClick = onRefresh, enabled = !loading, border = BorderStroke(1.dp, p.border.copy(alpha = 0.75f)), shape = RoundedCornerShape(12.dp)) { Text(if (loading) "Sincronizando…" else "Atualizar", color = p.text) }
                }
            }
            error?.let { Text(it, color = Color(0xFFFF6B6B), fontSize = 12.sp) }
            if (loading && playlists.isEmpty()) Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = p.accent) }
            else if (playlists.isEmpty()) Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Nenhuma playlist foi retornada por esta sessão.", color = p.muted) }
            else LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(playlists, key = { it.playlistId }) { item ->
                    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(p.surfaceAlt).clickable { onOpen(item) }.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Cover(item.thumbnailUrl, item.title, p, 54.dp); Spacer(Modifier.width(12.dp)); Column(Modifier.weight(1f)) { Text(item.title, color = p.text, fontWeight = FontWeight.Bold); item.subtitle?.let { Text(it, color = p.muted, fontSize = 11.sp, maxLines = 1) } }
                        Icon(Icons.Filled.QueueMusic, null, tint = p.accent)
                    }
                }
            }
        }
    }
}

@Composable
private fun PlaylistDetailView(
    p: Palette,
    playlist: YouTubeMusicSearchClient.Playlist,
    loading: Boolean,
    error: String?,
    tracks: List<YouTubeMusicSearchClient.Track>,
    onBack: () -> Unit,
    onPlay: (YouTubeMusicSearchClient.Track) -> Unit,
) {
    Card(modifier = Modifier.fillMaxSize(), shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = p.surface), border = BorderStroke(1.dp, p.border.copy(alpha = 0.75f))) {
        Column(Modifier.fillMaxSize().padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, "Voltar", tint = p.text) }
                Cover(playlist.thumbnailUrl, playlist.title, p, 58.dp)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(playlist.title, color = p.text, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    playlist.subtitle?.let { Text(it, color = p.muted, fontSize = 12.sp) }
                }
                if (tracks.isNotEmpty()) Button(onClick = { onPlay(tracks.first()) }, colors = ButtonDefaults.buttonColors(containerColor = p.accent), shape = RoundedCornerShape(12.dp)) { Text("Reproduzir") }
            }
            error?.let { Text(it, color = Color(0xFFFF6B6B), fontSize = 12.sp) }
            when {
                loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = p.accent) }
                tracks.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Nenhuma faixa foi retornada para esta playlist.", color = p.muted) }
                else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    items(tracks, key = { it.videoId }) { track -> TrackRow(p, track, false) { onPlay(track) } }
                }
            }
        }
    }
}

@Composable
private fun AddToPlaylistDialog(
    p: Palette,
    playlists: List<YouTubeMusicSearchClient.Playlist>,
    error: String?,
    onClose: () -> Unit,
    onSelect: (YouTubeMusicSearchClient.Playlist) -> Unit,
) {
    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = .58f)), contentAlignment = Alignment.Center) {
        Card(modifier = Modifier.width(460.dp), shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = p.surface), border = BorderStroke(1.dp, p.border.copy(alpha = 0.75f))) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Adicionar à playlist", color = p.text, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    IconButton(onClick = onClose) { Icon(Icons.Filled.Close, null, tint = p.muted) }
                }
                error?.let { Text(it, color = Color(0xFFFF6B6B), fontSize = 12.sp) }
                if (playlists.isEmpty()) Text("Nenhuma playlist disponível. Crie uma playlist primeiro.", color = p.muted)
                else LazyColumn(modifier = Modifier.height(360.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    items(playlists, key = { it.playlistId }) { playlist ->
                        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(p.surfaceAlt).clickable { onSelect(playlist) }.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Cover(playlist.thumbnailUrl, playlist.title, p, 44.dp)
                            Spacer(Modifier.width(10.dp))
                            Text(playlist.title, color = p.text, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NewPlaylistDialog(
    p: Palette,
    title: String,
    error: String?,
    onTitle: (String) -> Unit,
    onClose: () -> Unit,
    onCreate: () -> Unit,
) {
    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = .58f)), contentAlignment = Alignment.Center) {
        Card(modifier = Modifier.width(430.dp), shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = p.surface), border = BorderStroke(1.dp, p.border.copy(alpha = 0.75f))) {
            Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Nova playlist", color = p.text, fontSize = 21.sp, fontWeight = FontWeight.Bold)
                    IconButton(onClick = onClose) { Icon(Icons.Filled.Close, null, tint = p.muted) }
                }
                TextField(
                    value = title,
                    onValueChange = onTitle,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    placeholder = { Text("Nome da playlist", color = p.muted) },
                    shape = RoundedCornerShape(14.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = p.surfaceAlt, unfocusedContainerColor = p.surfaceAlt,
                        focusedTextColor = p.text, unfocusedTextColor = p.text,
                        focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent,
                    ),
                )
                error?.let { Text(it, color = Color(0xFFFF6B6B), fontSize = 12.sp) }
                Button(onClick = onCreate, modifier = Modifier.fillMaxWidth().height(46.dp), shape = RoundedCornerShape(14.dp), colors = ButtonDefaults.buttonColors(containerColor = p.accent)) {
                    Text("Criar playlist", fontWeight = FontWeight.Bold)
                }
                Text("A playlist será criada na sua conta Google/YouTube Music conectada.", color = p.muted, fontSize = 11.sp)
            }
        }
    }
}

@Composable
private fun EmptyView(p: Palette, icon: ImageVector, title: String, message: String) {
    Card(modifier = Modifier.fillMaxSize(), shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = p.surface), border = BorderStroke(1.dp, p.border.copy(alpha = 0.75f))) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(icon, null, tint = p.accent, modifier = Modifier.size(40.dp)); Text(title, color = p.text, fontWeight = FontWeight.Bold, fontSize = 20.sp); Text(message, color = p.muted, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun SettingsView(
    p: Palette,
    theme: DesktopTheme,
    onTheme: (DesktopTheme) -> Unit,
    gamerMode: Boolean,
    onGamerMode: (Boolean) -> Unit,
    liquidGlass: Boolean,
    onLiquidGlass: (Boolean) -> Unit,
    quality: AudioQuality,
    onQuality: (AudioQuality) -> Unit,
) {
    var panel by remember { mutableStateOf(SettingsPanel.GENERAL) }

    Card(
        modifier = Modifier.fillMaxSize(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF090A0F)),
        border = BorderStroke(1.dp, p.border.copy(alpha = .62f)),
    ) {
        Row(Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .width(204.dp)
                    .fillMaxHeight()
                    .background(Color(0xFF0C0D12))
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    "Configurações",
                    color = p.text,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                )

                SettingsPanel.entries.forEach { option ->
                    KodaSettingsNavItem(
                        p = p,
                        label = option.label,
                        icon = option.icon,
                        selected = panel == option,
                        onClick = { panel = option },
                    )
                }
            }

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                item {
                    Text(panel.label, color = p.text, fontSize = 23.sp, fontWeight = FontWeight.Black)
                    Text(
                        when (panel) {
                            SettingsPanel.GENERAL -> "Comportamento principal do Koda Music."
                            SettingsPanel.APPEARANCE -> "Identidade visual bonita sem transformar efeitos em desperdício de GPU."
                            SettingsPanel.AUDIO -> "Qualidade do streaming e comportamento do motor de áudio."
                            SettingsPanel.PERFORMANCE -> "Controles para reduzir custo visual enquanto você joga."
                            SettingsPanel.SHORTCUTS -> "Atalhos de teclado e controles multimídia do Windows."
                            SettingsPanel.ADVANCED -> "Informações técnicas e regras de encerramento do aplicativo."
                        },
                        color = p.muted,
                        fontSize = 10.sp,
                    )
                }

                when (panel) {
                    SettingsPanel.GENERAL -> {
                        item {
                            Text("Tema", color = p.text, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                KodaThemeCard(p, "Preto & Branco", theme == DesktopTheme.MONOCHROME) { onTheme(DesktopTheme.MONOCHROME) }
                                KodaThemeCard(p, "Preto & Roxo", theme == DesktopTheme.PURPLE) { onTheme(DesktopTheme.PURPLE) }
                            }
                        }
                        item {
                            Text("Efeitos e interface", color = p.text, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(8.dp))
                            KodaSettingToggle(p, "Modo Gamer", "Reduz efeitos visuais sem alterar a qualidade de áudio.", gamerMode, true, onGamerMode)
                        }
                        item {
                            Text("Áudio", color = p.text, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(8.dp))
                            KodaSettingsInfoCard(p, "Qualidade de áudio", "${quality.label} · altere na seção Áudio.")
                        }
                    }

                    SettingsPanel.APPEARANCE -> {
                        item {
                            Text("Tema", color = p.text, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                KodaThemeCard(
                                    p = p,
                                    label = "Preto & Branco",
                                    selected = theme == DesktopTheme.MONOCHROME,
                                    onClick = { onTheme(DesktopTheme.MONOCHROME) },
                                )
                                KodaThemeCard(
                                    p = p,
                                    label = "Preto & Roxo",
                                    selected = theme == DesktopTheme.PURPLE,
                                    onClick = { onTheme(DesktopTheme.PURPLE) },
                                )
                            }
                        }
                    }

                    SettingsPanel.AUDIO -> {
                        items(AudioQuality.entries.toList()) { option ->
                            val selected = quality == option
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onQuality(option) },
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (selected) p.surfaceAlt else p.surface,
                                ),
                                border = BorderStroke(
                                    1.dp,
                                    if (selected) p.accent else p.border.copy(alpha = .42f),
                                ),
                            ) {
                                Row(
                                    Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Column(Modifier.weight(1f)) {
                                        Text(option.label, color = p.text, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        Text(
                                            when (option) {
                                                AudioQuality.AUTO -> "Escolha automática conforme o stream disponível"
                                                AudioQuality.HIGH -> "Prioriza a melhor qualidade disponível"
                                                AudioQuality.BALANCED -> "Equilíbrio entre qualidade e consumo"
                                                AudioQuality.DATA_SAVER -> "Reduz uso de dados quando possível"
                                            },
                                            color = p.muted,
                                            fontSize = 9.sp,
                                        )
                                    }
                                    Surface(
                                        modifier = Modifier.size(17.dp),
                                        color = if (selected) p.accent else Color.Transparent,
                                        shape = CircleShape,
                                        border = BorderStroke(1.dp, if (selected) p.accent else p.muted),
                                    ) {}
                                }
                            }
                        }
                        item {
                            KodaSettingsInfoCard(
                                p = p,
                                title = "Motor de reprodução",
                                text = "Innertube direto → NewPipe local como fallback → mpv. O Koda não usa yt-dlp na reprodução normal.",
                            )
                        }
                    }

                    SettingsPanel.PERFORMANCE -> {
                        item {
                            KodaSettingToggle(
                                p = p,
                                title = "Modo Gamer",
                                subtitle = "Mantém a qualidade de áudio escolhida e reduz efeitos, frequência de atualização visual e trabalho não essencial.",
                                checked = gamerMode,
                                enabled = true,
                                onChange = onGamerMode,
                            )
                        }
                        item {
                            KodaSettingsInfoCard(
                                p = p,
                                title = "Bonito sem pesar",
                                text = "Listas são carregadas conforme necessário e a interface evita efeitos permanentes que competem com o jogo por recursos.",
                            )
                        }
                    }

                    SettingsPanel.SHORTCUTS -> {
                        item {
                            KodaSettingsInfoCard(
                                p = p,
                                title = "Controles multimídia",
                                text = "A integração dedicada com teclas de mídia e controles do Windows está planejada para a etapa seguinte. Esta tela não finge atalhos que ainda não foram implementados.",
                            )
                        }
                    }

                    SettingsPanel.ADVANCED -> {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = p.surface),
                                border = BorderStroke(1.dp, p.border.copy(alpha = .42f)),
                            ) {
                                Row(
                                    Modifier.fillMaxWidth().padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Column(Modifier.weight(1f)) {
                                        Text("Encerramento obrigatório", color = p.text, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        Text(
                                            "Fechar o Koda Music encerra o áudio, o processo mpv criado pelo Koda e os recursos de login.",
                                            color = p.muted,
                                            fontSize = 10.sp,
                                        )
                                    }
                                    Surface(
                                        color = p.accent.copy(alpha = .16f),
                                        shape = RoundedCornerShape(50),
                                        border = BorderStroke(1.dp, p.accent.copy(alpha = .38f)),
                                    ) {
                                        Text(
                                            "SEMPRE ATIVO",
                                            color = p.accent,
                                            fontSize = 8.sp,
                                            fontWeight = FontWeight.Black,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        )
                                    }
                                }
                            }
                        }
                        item {
                            KodaSettingsInfoCard(
                                p = p,
                                title = "Arquitetura",
                                text = "A interface permanece separada do player, da conta e da resolução de streams para que o visual possa evoluir sem reabrir bugs do mecanismo.",
                            )
                        }
                    }
                }

                item { Spacer(Modifier.height(100.dp)) }
            }
        }
    }
}

@Composable
private fun KodaSettingsNavItem(
    p: Palette,
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(11.dp))
            .background(if (selected) p.accent.copy(alpha = .14f) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = if (selected) p.accent else p.muted, modifier = Modifier.size(17.dp))
        Spacer(Modifier.width(9.dp))
        Text(
            label,
            color = if (selected) p.text else p.muted,
            fontSize = 11.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
        )
    }
}

@Composable
private fun KodaSettingsInfoCard(
    p: Palette,
    title: String,
    text: String,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = p.surface),
        border = BorderStroke(1.dp, p.border.copy(alpha = .36f)),
    ) {
        Column(
            Modifier.fillMaxWidth().padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Text(title, color = p.text, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            Text(text, color = p.muted, fontSize = 10.sp)
        }
    }
}

@Composable
private fun KodaThemeCard(
    p: Palette,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier.width(160.dp).height(72.dp).clickable(onClick = onClick),
        shape = RoundedCornerShape(13.dp),
        colors = CardDefaults.cardColors(containerColor = p.surface),
        border = BorderStroke(1.dp, if (selected) p.accent else p.border.copy(alpha = .5f)),
    ) {
        Box(Modifier.fillMaxSize()) {
            Box(
                Modifier
                    .align(Alignment.TopStart)
                    .fillMaxWidth()
                    .height(30.dp)
                    .background(
                        Brush.horizontalGradient(
                            if (label.contains("Roxo")) {
                                listOf(Color(0xFF160A25), Color(0xFF843DEE), Color(0xFFB958F6))
                            } else {
                                listOf(Color(0xFF070707), Color(0xFF494949))
                            },
                        ),
                    ),
            )
            Text(
                label,
                color = p.text,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.BottomStart).padding(10.dp),
            )
            if (selected) {
                Surface(
                    modifier = Modifier.align(Alignment.TopEnd).padding(8.dp).size(14.dp),
                    color = p.accent,
                    shape = CircleShape,
                ) {}
            }
        }
    }
}

@Composable
private fun KodaSettingToggle(
    p: Palette,
    title: String,
    subtitle: String,
    checked: Boolean,
    enabled: Boolean,
    onChange: (Boolean) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(13.dp),
        colors = CardDefaults.cardColors(containerColor = p.surface),
        border = BorderStroke(1.dp, p.border.copy(alpha = .36f)),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 13.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(Modifier.weight(1f)) {
                Text(title, color = p.text, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Text(subtitle, color = p.muted, fontSize = 9.sp)
            }
            Switch(checked = checked, enabled = enabled, onCheckedChange = onChange)
        }
    }
}

@Composable
private fun QueuePanel(p: Palette, results: List<YouTubeMusicSearchClient.Track>, selected: YouTubeMusicSearchClient.Track?, onClose: () -> Unit, onPlay: (YouTubeMusicSearchClient.Track) -> Unit) {
    Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.End) {
        Card(modifier = Modifier.width(390.dp).fillMaxHeight().padding(16.dp), shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = p.surface), border = BorderStroke(1.dp, p.border.copy(alpha = 0.75f))) {
            Column(Modifier.fillMaxSize().padding(16.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Fila", color = p.text, fontSize = 20.sp, fontWeight = FontWeight.Bold); IconButton(onClick = onClose) { Icon(Icons.Filled.Close, null, tint = p.muted) }
                }
                LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(results, key = { it.videoId }) { t ->
                        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(if (selected?.videoId == t.videoId) p.accent.copy(alpha=.13f) else p.surfaceAlt).clickable { onPlay(t) }.padding(9.dp), verticalAlignment = Alignment.CenterVertically) {
                            Cover(t.thumbnailUrl, t.title, p, 40.dp); Spacer(Modifier.width(9.dp)); Column(Modifier.weight(1f)) { Text(t.title, color=p.text, fontSize=12.sp, maxLines=1); Text(t.artist,color=p.muted,fontSize=10.sp,maxLines=1) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AccountDialog(
    p: Palette,
    connected: Boolean,
    profile: YouTubeMusicSearchClient.AccountProfile?,
    error: String?,
    onClose: () -> Unit,
    onLogin: () -> Unit,
    onSignOut: () -> Unit,
) {
    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = .58f)), contentAlignment = Alignment.Center) {
        Card(modifier = Modifier.width(500.dp), shape = RoundedCornerShape(28.dp), colors = CardDefaults.cardColors(containerColor = p.surface), border = BorderStroke(1.dp, p.border.copy(alpha = 0.75f))) {
            Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) { IconButton(onClick = onClose) { Icon(Icons.Filled.Close, null, tint = p.muted) } }
                Image(
                    painter = painterResource("branding/p-music-logo.png"),
                    contentDescription = "Koda Music",
                    modifier = Modifier.width(280.dp).height(96.dp),
                    contentScale = ContentScale.Fit,
                )
                if (connected && !profile?.thumbnailUrl.isNullOrBlank()) {
                    AsyncImage(model = profile?.thumbnailUrl, contentDescription = "Foto da conta", modifier = Modifier.size(72.dp).clip(CircleShape), contentScale = ContentScale.Crop)
                }
                Text(if (connected) (profile?.name ?: "Google conectado") else "Entrar no Koda Music", color = p.text, fontSize = 24.sp, fontWeight = FontWeight.Black)
                if (connected && !profile?.subtitle.isNullOrBlank()) Text(profile?.subtitle.orEmpty(), color = p.muted, fontSize = 12.sp)
                Text(
                    if (connected) "Sua sessão do Google está conectada ao Koda Music e pode autenticar as chamadas do YouTube Music."
                    else "O login abre o Google dentro do Koda Music. Sua senha e verificação em duas etapas são digitadas diretamente na página oficial do Google; o app captura apenas a sessão concluída do YouTube Music.",
                    color = p.muted, fontSize = 12.sp, lineHeight = 18.sp,
                )
                error?.let { Text(it, color = Color(0xFFFF6B6B), fontSize = 12.sp) }
                if (!connected) {
                    Button(
                        onClick = onLogin,
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(14.dp), colors = ButtonDefaults.buttonColors(containerColor = p.accent),
                    ) { Icon(Icons.Filled.Login, null); Spacer(Modifier.width(8.dp)); Text("Continuar com Google", fontWeight = FontWeight.Bold) }
                } else {
                    OutlinedButton(
                        onClick = onSignOut,
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(14.dp), border = BorderStroke(1.dp, p.border.copy(alpha = 0.75f)),
                    ) { Text("Sair da conta", color = p.text) }
                }
                Text("Koda Music não contém anúncios próprios.", color = p.muted, fontSize = 11.sp)
            }
        }
    }
}

private fun durationTextToMillis(text: String?): Long {
    val parts = text?.trim()?.split(":")?.mapNotNull { it.toLongOrNull() }.orEmpty()
    if (parts.isEmpty()) return 0L
    val seconds = when (parts.size) {
        3 -> parts[0] * 3600 + parts[1] * 60 + parts[2]
        2 -> parts[0] * 60 + parts[1]
        1 -> parts[0]
        else -> return 0L
    }
    return seconds.coerceAtLeast(0L) * 1000L
}

private fun formatTime(ms: Long): String {
    if (ms <= 0L) return "0:00"
    val total = ms / 1000
    return "%d:%02d".format(total / 60, total % 60)
}
