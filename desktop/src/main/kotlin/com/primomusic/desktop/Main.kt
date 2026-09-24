package com.primomusic.desktop

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.filled.Download
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
    bg = Color(0xFF07080D), sidebar = Color(0xFF0B0D13), surface = Color(0xFF12151D),
    surfaceAlt = Color(0xFF191D27), border = Color(0xFF303646), accent = Color(0xFF8B5CF6),
    accent2 = Color(0xFF6D3EF2), text = Color(0xFFF7F7FB), muted = Color(0xFFA4A9B8),
)

private val MonochromeDark = Palette(
    bg = Color(0xFF050505), sidebar = Color(0xFF0A0A0A), surface = Color(0xFF111111),
    surfaceAlt = Color(0xFF191919), border = Color(0xFF323232), accent = Color(0xFFF2F2F2),
    accent2 = Color(0xFFB9B9B9), text = Color(0xFFF5F5F5), muted = Color(0xFFA0A0A0),
)

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
    HOME("Home", Icons.Filled.Home),
    SEARCH("Buscar", Icons.Filled.Search),
    LIBRARY("Biblioteca", Icons.Filled.LibraryMusic),
    PLAYLISTS("Playlists", Icons.Filled.QueueMusic),
    DOWNLOADS("Downloads", Icons.Filled.Download),
    SETTINGS("Configurações", Icons.Filled.Settings),
}

fun main() {
    application {
        val windowState = rememberWindowState(width = 1420.dp, height = 900.dp)
        val appIcon = painterResource("branding/p-music-icon.png")
        val player = remember { DesktopAudioPlayer() }
    
        var theme by remember { mutableStateOf(DesktopPreferences.theme()) }
        var liquidGlass by remember { mutableStateOf(DesktopPreferences.liquidGlassEnabled()) }
        var gamerMode by remember { mutableStateOf(DesktopPreferences.gamerModeEnabled()) }

        val basePalette = when (theme) {
            DesktopTheme.MONOCHROME -> MonochromeDark
            DesktopTheme.PURPLE -> PurpleDark
        }
        val effectiveLiquidGlass = liquidGlass && !gamerMode
        val p = if (effectiveLiquidGlass) glassPalette(basePalette) else basePalette

        LaunchedEffect(gamerMode) {
            player.setGamerMode(gamerMode)
        }

        Window(
            onCloseRequest = {
                // Closing Koda Music always stops audio and releases heavy resources.
                player.close()
                DesktopGoogleLogin.shutdown()
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
                PrimoMusicApp(
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
private fun PrimoMusicApp(
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
    var query by remember { mutableStateOf("") }
    var suggestions by remember { mutableStateOf<List<String>>(emptyList()) }
    var results by remember { mutableStateOf<List<YouTubeMusicSearchClient.Track>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
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

    fun search(value: String = query) {
        if (value.isBlank() || loading) return
        query = value
        section = Section.SEARCH
        loading = true
        error = null
        scope.launch {
            runCatching { YouTubeMusicSearchClient.search(value) }
                .onSuccess { results = it }
                .onFailure { error = it.message ?: "Não foi possível pesquisar." }
            loading = false
        }
    }

    fun playTrack(track: YouTubeMusicSearchClient.Track, sourceQueue: List<YouTubeMusicSearchClient.Track> = emptyList()) {
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
                        val track = (results + listOfNotNull(selected)).firstOrNull { it.videoId == videoId }
                        if (track != null && accountLiked.none { it.videoId == videoId }) listOf(track) + accountLiked else accountLiked
                    } else accountLiked.filterNot { it.videoId == videoId }
                }
                .onFailure { accountError = it.message ?: "Não foi possível atualizar Músicas que gostei." }
        }
    }

    LaunchedEffect(accountConnected) {
        if (accountConnected) refreshAccount()
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
                        Box(
                            Modifier
                                .align(Alignment.TopEnd)
                                .offset(x = 110.dp, y = (-150).dp)
                                .width(560.dp)
                                .height(390.dp)
                                .clip(LiquidBlobShape)
                                .blur(92.dp)
                                .background(
                                    Brush.linearGradient(
                                        listOf(
                                            p.accent.copy(alpha = 0.38f),
                                            Color(0xFF536DFF).copy(alpha = 0.18f),
                                            p.accent2.copy(alpha = 0.30f),
                                        ),
                                    ),
                                ),
                        )

                        Box(
                            Modifier
                                .align(Alignment.BottomStart)
                                .offset(x = (-150).dp, y = 105.dp)
                                .width(500.dp)
                                .height(350.dp)
                                .clip(LiquidBlobShape)
                                .blur(105.dp)
                                .background(
                                    Brush.linearGradient(
                                        listOf(
                                            Color(0xFF3A79FF).copy(alpha = 0.20f),
                                            p.accent.copy(alpha = 0.26f),
                                            Color.Transparent,
                                        ),
                                    ),
                                ),
                        )

                        Box(
                            Modifier
                                .align(Alignment.TopCenter)
                                .offset(y = (-44).dp)
                                .fillMaxWidth(0.68f)
                                .height(96.dp)
                                .blur(38.dp)
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(
                                            Color.Transparent,
                                            Color.White.copy(alpha = 0.16f),
                                            Color.Transparent,
                                        ),
                                    ),
                                    RoundedCornerShape(50),
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
                        onSection = { section = it },
                        onLogin = { loginOpen = true },
                    )

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .padding(22.dp),
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
                            when (section) {
                                Section.HOME ->
                                    AccountHomeView(
                                        p = p,
                                        connected = accountConnected,
                                        loading = accountLoading,
                                        error = accountError,
                                        history = accountHistory,
                                        playlists = accountPlaylists,
                                        liked = accountLiked,
                                        onSearch = { section = Section.SEARCH },
                                        onPlay = { playTrack(it, accountHistory) },
                                        onPlayLiked = { playTrack(it, accountLiked) },
                                        onOpenPlaylist = ::openPlaylist,
                                        onRefresh = ::refreshAccount,
                                    )

                                Section.SEARCH ->
                                    ResultsView(
                                        p,
                                        results,
                                        loading,
                                        error,
                                        favorites,
                                        onPlay = { playTrack(it, results) },
                                    )

                                Section.LIBRARY ->
                                    LibraryView(
                                        p,
                                        accountConnected,
                                        accountLoading,
                                        accountError,
                                        accountLiked,
                                        onPlay = { playTrack(it, accountLiked) },
                                        onRefresh = ::refreshAccount,
                                        onLogin = { loginOpen = true },
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

                                Section.DOWNLOADS ->
                                    EmptyView(
                                        p,
                                        Icons.Filled.Download,
                                        "Downloads",
                                        "Downloads offline serão adicionados depois do player online estabilizar.",
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
                            start = 232.dp,
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
                        } else player.toggle { playerState = it }
                    },
                    onSeek = { player.seek(it) { snap -> playerState = snap } },
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
    val sidebarShape = RoundedCornerShape(0.dp, 24.dp, 24.dp, 0.dp)
    Column(
        Modifier.width(210.dp).fillMaxHeight()
            .liquidGlassSurface(liquidGlass, glassBackdrop, sidebarShape, p.sidebar)
            .then(if (liquidGlass) Modifier else Modifier.background(Brush.verticalGradient(listOf(p.sidebar, p.surface))))
            .border(1.dp, p.border.copy(alpha = 0.70f), sidebarShape)
            .padding(16.dp),
    ) {
        Brand(p)
        Spacer(Modifier.height(24.dp))
        Section.entries.filter { it != Section.SETTINGS }.forEach { item ->
            NavButton(p, item, section == item) { onSection(item) }
            Spacer(Modifier.height(6.dp))
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
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
            .background(if (selected) p.accent.copy(alpha = 0.16f) else Color.Transparent)
            .clickable(onClick = onClick).padding(horizontal = 13.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(11.dp),
    ) {
        Icon(item.icon, item.label, tint = if (selected) p.accent else p.muted, modifier = Modifier.size(20.dp))
        Text(item.label, color = if (selected) p.text else p.muted, fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium, fontSize = 13.sp)
    }
}

@Composable
private fun Brand(p: Palette) {
    Image(
        painter = painterResource("branding/p-music-logo.png"),
        contentDescription = "Koda Music",
        modifier = Modifier.width(158.dp).height(46.dp),
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
                    Text(
                        "🎮  Modo Gamer",
                        color = p.text,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 13.dp, vertical = 9.dp),
                    )
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
private fun HomeView(p: Palette, onSearch: () -> Unit) {
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Card(
            modifier = Modifier.fillMaxWidth().height(170.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = p.surface),
            border = BorderStroke(1.dp, p.border.copy(alpha = 0.75f)),
        ) {
            Row(
                Modifier.fillMaxSize().background(Brush.horizontalGradient(listOf(Color(0xFF181B35), p.accent2))).padding(24.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Ouça o que você quiser", color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Black)
                    Text("Pesquisa real e reprodução no Windows.", color = Color.White.copy(alpha = .78f), fontSize = 14.sp)
                    Button(onClick = onSearch, colors = ButtonDefaults.buttonColors(containerColor = Color.White), shape = RoundedCornerShape(14.dp)) {
                        Icon(Icons.Filled.Search, null, tint = p.accent2); Spacer(Modifier.width(8.dp)); Text("Buscar música", color = p.accent2, fontWeight = FontWeight.Bold)
                    }
                }
                Brand(p.copy(text = Color.White, muted = Color.White.copy(alpha = .65f)))
            }
        }
        EmptyView(p, Icons.Filled.AccountCircle, "Sua Home personalizada", "Ao integrar a sessão da conta do YouTube Music, esta área receberá histórico, playlists e recomendações do perfil.")
    }
}

@Composable
private fun ResultsView(
    p: Palette,
    results: List<YouTubeMusicSearchClient.Track>,
    loading: Boolean,
    error: String?,
    favorites: List<String>,
    onPlay: (YouTubeMusicSearchClient.Track) -> Unit,
) {
    Card(modifier = Modifier.fillMaxSize(), shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = p.surface), border = BorderStroke(1.dp, p.border.copy(alpha = 0.75f))) {
        Column(Modifier.fillMaxSize().padding(16.dp)) {
            Text("Resultados", color = p.text, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text("YouTube Music", color = p.muted, fontSize = 11.sp)
            Spacer(Modifier.height(10.dp))
            when {
                loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = p.accent) }
                error != null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(error, color = p.muted) }
                results.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Pesquise uma música para começar.", color = p.muted) }
                else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    items(results, key = { it.videoId }) { track -> TrackRow(p, track, favorites.contains(track.videoId)) { onPlay(track) } }
                }
            }
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

private fun highResolutionThumbnailUrl(url: String?): String? {
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

        return "$base=w1024-h1024-l90-rj"
    }

    return normalized
}

@Composable
private fun Cover(url: String?, title: String?, p: Palette, size: androidx.compose.ui.unit.Dp) {
    Box(
        Modifier.size(size).clip(RoundedCornerShape(12.dp)).background(p.surfaceAlt),
        contentAlignment = Alignment.Center,
    ) {
        val highResUrl = highResolutionThumbnailUrl(url)
        if (!highResUrl.isNullOrBlank()) {
            AsyncImage(
                model = highResUrl,
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
    val playerShape = RoundedCornerShape(24.dp)
    val durationMillis =
        state.durationMillis
            .takeIf { it > 0L }
            ?: durationTextToMillis(track?.durationText)

    LiquidGlassSurface(
        enabled = liquidGlass,
        backdrop = glassBackdrop,
        modifier = Modifier.fillMaxWidth().height(112.dp),
        shape = playerShape,
        solidColor = p.surface,
        tint = p.surface,
        borderColor = p.border.copy(alpha = 0.78f),
        accent = p.accent,
        refractionHeight = 20.dp,
        refractionAmount = 34.dp,
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp, vertical = 9.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Cover(track?.thumbnailUrl, track?.title, p, 56.dp)
                    Spacer(Modifier.width(11.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            track?.title ?: "Nenhuma música tocando",
                            color = p.text,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            when {
                                state.state == DesktopAudioPlayer.State.RESOLVING ->
                                    state.message ?: "Preparando áudio…"
                                state.state == DesktopAudioPlayer.State.ERROR ->
                                    state.message ?: "Erro no player"
                                else ->
                                    track?.artist ?: "Escolha uma música nos resultados"
                            },
                            color =
                                if (state.state == DesktopAudioPlayer.State.ERROR) {
                                    Color(0xFFFF6B6B)
                                } else {
                                    p.muted
                                },
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    IconButton(onClick = onFavorite, enabled = track != null) {
                        Icon(
                            if (track != null && favorites.contains(track.videoId)) {
                                Icons.Filled.Favorite
                            } else {
                                Icons.Filled.FavoriteBorder
                            },
                            null,
                            tint = p.accent,
                        )
                    }
                    IconButton(onClick = onAddToPlaylist, enabled = track != null) {
                        Icon(Icons.Filled.PlaylistAdd, "Adicionar à playlist", tint = p.muted)
                    }
                }

                Row(
                    Modifier.weight(1f),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = onShuffle) {
                        Icon(Icons.Filled.Shuffle, null, tint = if (shuffle) p.accent else p.muted)
                    }
                    IconButton(onClick = onPrevious, enabled = track != null) {
                        Icon(Icons.Filled.SkipPrevious, null, tint = p.text)
                    }
                    Box(
                        Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(if (track != null) p.accent else p.border)
                            .clickable(enabled = track != null, onClick = onToggle),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (state.state == DesktopAudioPlayer.State.RESOLVING) {
                            CircularProgressIndicator(Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                        } else {
                            Icon(
                                if (state.state == DesktopAudioPlayer.State.PLAYING) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                null,
                                tint = Color.White,
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
                        )
                    }
                    IconButton(onClick = onStop, enabled = track != null) {
                        Icon(Icons.Filled.Stop, null, tint = p.muted)
                    }
                }

                Row(
                    Modifier.weight(.8f),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = onMute) {
                        Icon(if (state.muted) Icons.Filled.VolumeOff else Icons.Filled.VolumeUp, null, tint = p.muted)
                    }
                    Slider(value = state.volume.toFloat(), onValueChange = onVolume, modifier = Modifier.width(105.dp))
                    Text(
                        "${(state.volume * 100).roundToInt()}%",
                        color = p.text.copy(alpha = 0.82f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.width(38.dp),
                    )
                    IconButton(onClick = onExpand, enabled = track != null) {
                        Icon(Icons.Filled.Fullscreen, "Abrir player em tela cheia", tint = if (track != null) p.text else p.muted)
                    }
                    IconButton(onClick = onQueue) {
                        Icon(Icons.Filled.QueueMusic, null, tint = p.muted)
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
                    if (found == null) lyricsError = "Letra não encontrada para esta faixa."
                }
                .onFailure { lyricsError = it.message ?: "Não foi possível carregar a letra." }
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
                        .blur(120.dp),
                    contentScale = ContentScale.Crop,
                    alpha = 0.26f,
                )
            }

            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color.Black.copy(alpha = 0.42f),
                                Color.Black.copy(alpha = 0.66f),
                                Color.Black.copy(alpha = 0.92f),
                            ),
                        ),
                    ),
            )

            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                p.accent.copy(alpha = 0.13f),
                                Color.Transparent,
                            ),
                            radius = 1050f,
                        ),
                    ),
            )

            Column(
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = 34.dp, vertical = 26.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Brand(
                            p.copy(
                                text = Color.White,
                                muted = Color.White.copy(alpha = .62f),
                            ),
                        )
                        Spacer(Modifier.width(14.dp))
                        Surface(
                            color = Color.White.copy(alpha = .06f),
                            shape = RoundedCornerShape(50),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = .08f)),
                        ) {
                            Text(
                                "TOCANDO AGORA",
                                color = Color.White.copy(alpha = .78f),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                            )
                        }
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        OutlinedButton(
                            onClick = onLyricsToggle,
                            border = BorderStroke(
                                1.dp,
                                if (lyricsVisible) p.accent else Color.White.copy(alpha = .18f),
                            ),
                            shape = RoundedCornerShape(14.dp),
                        ) {
                            Icon(
                                Icons.Filled.Article,
                                null,
                                tint = if (lyricsVisible) p.accent else Color.White,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                if (lyricsVisible) "Ocultar letras" else "Letras",
                                color = Color.White,
                            )
                        }

                        Surface(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .clickable(onClick = onClose),
                            color = Color.White.copy(alpha = .08f),
                            shape = CircleShape,
                            border = BorderStroke(1.dp, Color.White.copy(alpha = .10f)),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Filled.FullscreenExit,
                                    "Sair da tela cheia",
                                    tint = Color.White,
                                )
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 34.dp),
                    horizontalArrangement = Arrangement.spacedBy(44.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(
                        modifier = Modifier.width(410.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Card(
                            modifier = Modifier.size(390.dp),
                            shape = RoundedCornerShape(32.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF111116)),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = .10f)),
                        ) {
                            Cover(track.thumbnailUrl, track.title, p, 390.dp)
                        }

                        Spacer(Modifier.height(22.dp))

                        Text(
                            track.title,
                            color = Color.White,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Black,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )

                        Spacer(Modifier.height(6.dp))

                        Text(
                            track.artist,
                            color = Color.White.copy(alpha = .62f),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }

                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .padding(vertical = 24.dp),
                        shape = RoundedCornerShape(28.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFF0D0E13).copy(alpha = .84f),
                        ),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = .08f)),
                    ) {
                        if (lyricsVisible) {
                            Column(
                                Modifier
                                    .fillMaxSize()
                                    .padding(26.dp),
                                verticalArrangement = Arrangement.spacedBy(14.dp),
                            ) {
                                Row(
                                    Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Column {
                                        Text(
                                            "Letras",
                                            color = Color.White,
                                            fontSize = 25.sp,
                                            fontWeight = FontWeight.Black,
                                        )
                                        Text(
                                            "Acompanhe a faixa sem sair do player",
                                            color = Color.White.copy(alpha = .52f),
                                            fontSize = 11.sp,
                                        )
                                    }
                                    lyrics?.source?.let {
                                        Text(
                                            it,
                                            color = p.accent,
                                            fontSize = 11.sp,
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
                                                fontSize = 14.sp,
                                            )
                                        }
                                    }

                                    lyrics != null -> {
                                        LyricsBody(
                                            p.copy(
                                                text = Color.White,
                                                muted = Color.White.copy(alpha = .55f),
                                            ),
                                            lyrics!!,
                                            state.positionMillis,
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
                                            )
                                        }
                                    }
                                }
                            }
                        } else {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(34.dp),
                                verticalArrangement = Arrangement.Center,
                            ) {
                                Text(
                                    "KODA MUSIC",
                                    color = p.accent,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black,
                                )

                                Spacer(Modifier.height(14.dp))

                                Text(
                                    track.title,
                                    color = Color.White,
                                    fontSize = 42.sp,
                                    fontWeight = FontWeight.Black,
                                    maxLines = 3,
                                    overflow = TextOverflow.Ellipsis,
                                )

                                Spacer(Modifier.height(10.dp))

                                Text(
                                    track.artist,
                                    color = Color.White.copy(alpha = .62f),
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )

                                state.streamInfo
                                    ?.takeIf { it.isNotBlank() }
                                    ?.let {
                                        Spacer(Modifier.height(18.dp))
                                        Surface(
                                            color = Color.White.copy(alpha = .05f),
                                            shape = RoundedCornerShape(50),
                                        ) {
                                            Text(
                                                it,
                                                color = Color.White.copy(alpha = .56f),
                                                fontSize = 10.sp,
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                                            )
                                        }
                                    }
                            }
                        }
                    }
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF0C0D12).copy(alpha = .94f),
                    ),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = .08f)),
                ) {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 22.dp, vertical = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        PlayerTimeline(
                            p = p.copy(
                                text = Color.White,
                                muted = Color.White.copy(alpha = .55f),
                            ),
                            positionMillis = state.positionMillis,
                            durationMillis = durationMillis,
                            compact = false,
                            onSeek = onSeek,
                        )

                        Row(
                            Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = onFavorite) {
                                    Icon(
                                        if (favorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                                        "Favorito",
                                        tint = if (favorite) p.accent else Color.White.copy(alpha = .56f),
                                    )
                                }
                                IconButton(onClick = onShuffle) {
                                    Icon(
                                        Icons.Filled.Shuffle,
                                        "Aleatório",
                                        tint = if (shuffle) p.accent else Color.White.copy(alpha = .56f),
                                    )
                                }
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                IconButton(onClick = onPrevious) {
                                    Icon(
                                        Icons.Filled.SkipPrevious,
                                        "Anterior",
                                        tint = Color.White,
                                        modifier = Modifier.size(32.dp),
                                    )
                                }

                                Box(
                                    Modifier
                                        .size(62.dp)
                                        .clip(CircleShape)
                                        .background(p.accent)
                                        .clickable(onClick = onToggle),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    if (state.state == DesktopAudioPlayer.State.RESOLVING) {
                                        CircularProgressIndicator(
                                            Modifier.size(25.dp),
                                            color = Color.White,
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
                                            tint = Color.White,
                                            modifier = Modifier.size(34.dp),
                                        )
                                    }
                                }

                                IconButton(onClick = onNext) {
                                    Icon(
                                        Icons.Filled.SkipNext,
                                        "Próxima",
                                        tint = Color.White,
                                        modifier = Modifier.size(32.dp),
                                    )
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = onRepeat) {
                                    Icon(
                                        if (repeat) Icons.Filled.RepeatOne else Icons.Filled.Repeat,
                                        "Repetir",
                                        tint = if (repeat) p.accent else Color.White.copy(alpha = .56f),
                                    )
                                }
                                IconButton(onClick = onMute) {
                                    Icon(
                                        if (state.muted) Icons.Filled.VolumeOff else Icons.Filled.VolumeUp,
                                        "Volume",
                                        tint = Color.White.copy(alpha = .62f),
                                    )
                                }
                                Slider(
                                    value = state.volume.toFloat(),
                                    onValueChange = onVolume,
                                    modifier = Modifier.width(160.dp),
                                )
                                Text(
                                    "${(state.volume * 100).roundToInt()}%",
                                    color = Color.White.copy(alpha = .70f),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.width(38.dp),
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
private fun AccountHomeView(
    p: Palette,
    connected: Boolean,
    loading: Boolean,
    error: String?,
    history: List<YouTubeMusicSearchClient.Track>,
    playlists: List<YouTubeMusicSearchClient.Playlist>,
    liked: List<YouTubeMusicSearchClient.Track>,
    onSearch: () -> Unit,
    onPlay: (YouTubeMusicSearchClient.Track) -> Unit,
    onPlayLiked: (YouTubeMusicSearchClient.Track) -> Unit,
    onOpenPlaylist: (YouTubeMusicSearchClient.Playlist) -> Unit,
    onRefresh: () -> Unit,
) {
    val heroTrack = history.firstOrNull() ?: liked.firstOrNull()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth().height(182.dp),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF090A0F)),
                border = BorderStroke(1.dp, p.border.copy(alpha = .62f)),
            ) {
                Box(Modifier.fillMaxSize()) {
                    heroTrack?.thumbnailUrl?.let { art ->
                        AsyncImage(
                            model = highResolutionThumbnailUrl(art),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                            alpha = .34f,
                        )
                    }

                    Box(
                        Modifier
                            .fillMaxSize()
                            .background(
                                Brush.horizontalGradient(
                                    listOf(
                                        Color(0xFF08090D).copy(alpha = .98f),
                                        Color(0xFF0C0D14).copy(alpha = .78f),
                                        p.accent2.copy(alpha = .22f),
                                    ),
                                ),
                            ),
                    )

                    Column(
                        Modifier
                            .align(Alignment.CenterStart)
                            .padding(horizontal = 26.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                "Ouvir ",
                                color = p.text,
                                fontSize = 30.sp,
                                fontWeight = FontWeight.Black,
                            )
                            Text(
                                "agora",
                                color = p.accent,
                                fontSize = 30.sp,
                                fontWeight = FontWeight.Black,
                            )
                        }
                        Text(
                            "Sua trilha sonora para jogar, focar ou relaxar.",
                            color = p.text.copy(alpha = .74f),
                            fontSize = 13.sp,
                        )
                        Button(
                            onClick = {
                                if (heroTrack != null) onPlay(heroTrack) else onSearch()
                            },
                            shape = RoundedCornerShape(13.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = p.accent),
                        ) {
                            Icon(Icons.Filled.PlayArrow, null, tint = Color.White)
                            Spacer(Modifier.width(6.dp))
                            Text(if (heroTrack != null) "Reproduzir mix" else "Buscar música", fontWeight = FontWeight.Bold)
                        }
                    }

                    if (connected) {
                        OutlinedButton(
                            onClick = onRefresh,
                            enabled = !loading,
                            modifier = Modifier.align(Alignment.TopEnd).padding(14.dp),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = .16f)),
                        ) {
                            Text(if (loading) "Sincronizando…" else "Atualizar", color = Color.White, fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        error?.let { message ->
            item {
                Text(message, color = Color(0xFFFF6B6B), fontSize = 12.sp)
            }
        }

        item {
            KodaSectionTitle(p, "Tocadas recentemente")
            Spacer(Modifier.height(8.dp))
            if (history.isEmpty()) {
                KodaEmptyStrip(
                    p = p,
                    text = if (connected) "Seu histórico aparecerá aqui." else "Entre na sua conta para carregar seu histórico.",
                )
            } else {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(history.take(8), key = { it.videoId }) { track ->
                        KodaTrackCard(p, track) { onPlay(track) }
                    }
                }
            }
        }

        item {
            KodaSectionTitle(p, "Playlists para você")
            Spacer(Modifier.height(8.dp))
            if (playlists.isEmpty()) {
                KodaEmptyStrip(
                    p = p,
                    text = if (connected) "Suas playlists aparecerão aqui." else "Conecte sua conta para carregar playlists.",
                )
            } else {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(playlists.take(7), key = { it.playlistId }) { playlist ->
                        KodaPlaylistCard(p, playlist) { onOpenPlaylist(playlist) }
                    }
                }
            }
        }

        item {
            KodaSectionTitle(p, "Músicas curtidas")
            Spacer(Modifier.height(8.dp))
            if (liked.isEmpty()) {
                KodaEmptyStrip(
                    p = p,
                    text = if (connected) "Suas músicas curtidas aparecerão aqui." else "Conecte sua conta para personalizar esta área.",
                )
            } else {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(liked.take(8), key = { it.videoId }) { track ->
                        KodaTrackCard(p, track) { onPlayLiked(track) }
                    }
                }
            }
        }

        item {
            Spacer(Modifier.height(112.dp))
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
        modifier = Modifier.width(174.dp).height(82.dp).clickable(onClick = onClick),
        shape = RoundedCornerShape(13.dp),
        colors = CardDefaults.cardColors(containerColor = p.surfaceAlt),
        border = BorderStroke(1.dp, p.border.copy(alpha = .45f)),
    ) {
        Row(
            Modifier.fillMaxSize().padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Cover(playlist.thumbnailUrl, playlist.title, p, 54.dp)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    playlist.title,
                    color = p.text,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                playlist.subtitle?.let {
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
private fun LibraryView(
    p: Palette,
    connected: Boolean,
    loading: Boolean,
    error: String?,
    liked: List<YouTubeMusicSearchClient.Track>,
    onPlay: (YouTubeMusicSearchClient.Track) -> Unit,
    onRefresh: () -> Unit,
    onLogin: () -> Unit,
) {
    if (!connected) {
        EmptyView(p, Icons.Filled.LibraryMusic, "Biblioteca", "Conecte sua conta Google para carregar Músicas que gostei e sua biblioteca do YouTube Music.")
        return
    }
    Card(modifier = Modifier.fillMaxSize(), shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = p.surface), border = BorderStroke(1.dp, p.border.copy(alpha = 0.75f))) {
        Column(Modifier.fillMaxSize().padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column { Text("Músicas que gostei", color = p.text, fontSize = 22.sp, fontWeight = FontWeight.Bold); Text("Sincronizadas da sua conta", color = p.muted, fontSize = 12.sp) }
                OutlinedButton(onClick = onRefresh, enabled = !loading, border = BorderStroke(1.dp, p.border.copy(alpha = 0.75f)), shape = RoundedCornerShape(12.dp)) { Text("Atualizar", color = p.text) }
            }
            error?.let { Text(it, color = Color(0xFFFF6B6B), fontSize = 12.sp) }
            if (loading && liked.isEmpty()) Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = p.accent) }
            else if (liked.isEmpty()) Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Nenhuma música curtida foi retornada por esta sessão.", color = p.muted) }
            else LazyColumn(verticalArrangement = Arrangement.spacedBy(7.dp)) { items(liked, key = { it.videoId }) { TrackRow(p, it, true) { onPlay(it) } } }
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
                Text("A playlist é criada na conta Google/YouTube Music conectada, como no BitChord.", color = p.muted, fontSize = 11.sp)
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
    Card(
        modifier = Modifier.fillMaxSize(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF090A0F)),
        border = BorderStroke(1.dp, p.border.copy(alpha = .62f)),
    ) {
        Row(Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .width(154.dp)
                    .fillMaxHeight()
                    .background(Color(0xFF0C0D12))
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                Text(
                    "Configurações",
                    color = p.text,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                )

                KodaSettingsNavItem(p, "Geral", Icons.Filled.Settings, true)
                KodaSettingsNavItem(p, "Aparência", Icons.Filled.DarkMode, false)
                KodaSettingsNavItem(p, "Áudio", Icons.Filled.VolumeUp, false)
                KodaSettingsNavItem(p, "Desempenho", Icons.Filled.Speed, false)
                KodaSettingsNavItem(p, "Atalhos", Icons.Filled.Keyboard, false)
                KodaSettingsNavItem(p, "Avançado", Icons.Filled.Tune, false)
            }

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                item {
                    Text("Geral", color = p.text, fontSize = 22.sp, fontWeight = FontWeight.Black)
                    Text(
                        "Personalize a experiência do Koda Music sem sacrificar desempenho.",
                        color = p.muted,
                        fontSize = 11.sp,
                    )
                }

                item {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.Top,
                    ) {
                        Column(
                            modifier = Modifier.weight(1.15f),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Text("Aparência", color = p.text, fontSize = 14.sp, fontWeight = FontWeight.Bold)

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

                            Text("Efeitos e interface", color = p.text, fontSize = 14.sp, fontWeight = FontWeight.Bold)

                            KodaSettingToggle(
                                p = p,
                                title = "Liquid Glass",
                                subtitle = if (gamerMode) {
                                    "Desativado temporariamente pelo Modo Gamer."
                                } else {
                                    "Refração e transparência sutis na interface."
                                },
                                checked = liquidGlass && !gamerMode,
                                enabled = !gamerMode,
                                onChange = onLiquidGlass,
                            )

                            KodaSettingToggle(
                                p = p,
                                title = "Modo Gamer",
                                subtitle = "Reduz efeitos visuais e a frequência de atualização da interface.",
                                checked = gamerMode,
                                enabled = true,
                                onChange = onGamerMode,
                            )

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
                                        Text("Comportamento do aplicativo", color = p.text, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        Text(
                                            "Ao fechar o Koda Music, o áudio, o mpv e os recursos de login são encerrados.",
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
                                            "Obrigatório",
                                            color = p.accent,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        )
                                    }
                                }
                            }
                        }

                        Column(
                            modifier = Modifier.weight(.85f),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Text("Qualidade de áudio", color = p.text, fontSize = 14.sp, fontWeight = FontWeight.Bold)

                            AudioQuality.entries.forEach { option ->
                                val selected = quality == option
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onQuality(option) },
                                    shape = RoundedCornerShape(13.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (selected) p.surfaceAlt else p.surface,
                                    ),
                                    border = BorderStroke(
                                        1.dp,
                                        if (selected) p.accent else p.border.copy(alpha = .42f),
                                    ),
                                ) {
                                    Row(
                                        Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 11.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                    ) {
                                        Column(Modifier.weight(1f)) {
                                            Text(option.label, color = p.text, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                            Text(
                                                when (option) {
                                                    AudioQuality.AUTO -> "Escolha automática"
                                                    AudioQuality.HIGH -> "Melhor qualidade"
                                                    AudioQuality.BALANCED -> "Bom equilíbrio"
                                                    AudioQuality.DATA_SAVER -> "Menor uso de dados"
                                                },
                                                color = p.muted,
                                                fontSize = 9.sp,
                                            )
                                        }
                                        Surface(
                                            modifier = Modifier.size(16.dp),
                                            color = if (selected) p.accent else Color.Transparent,
                                            shape = CircleShape,
                                            border = BorderStroke(1.dp, if (selected) p.accent else p.muted),
                                        ) {}
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    Spacer(Modifier.height(100.dp))
                }
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
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(11.dp))
            .background(if (selected) p.accent.copy(alpha = .14f) else Color.Transparent)
            .padding(horizontal = 10.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = if (selected) p.accent else p.muted, modifier = Modifier.size(17.dp))
        Spacer(Modifier.width(9.dp))
        Text(label, color = if (selected) p.text else p.muted, fontSize = 11.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
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
        modifier = Modifier.weight(1f).height(72.dp).clickable(onClick = onClick),
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
                                listOf(Color(0xFF090A0F), p.accent2.copy(alpha = .72f))
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
