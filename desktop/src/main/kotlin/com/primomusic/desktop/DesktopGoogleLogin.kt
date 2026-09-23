package com.primomusic.desktop

import com.primomusic.core.music.YouTubeMusicSearchClient
import me.friwi.jcefmaven.CefAppBuilder
import org.cef.CefApp
import org.cef.CefClient
import org.cef.browser.CefBrowser
import org.cef.browser.CefFrame
import org.cef.callback.CefCookieVisitor
import org.cef.handler.CefLoadHandlerAdapter
import org.cef.misc.BoolRef
import org.cef.network.CefCookie
import org.cef.network.CefCookieManager
import java.awt.BorderLayout
import java.awt.Dimension
import java.io.File
import java.util.Base64
import java.util.Properties
import java.util.concurrent.atomic.AtomicBoolean
import javax.swing.JFrame
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.SwingConstants
import javax.swing.SwingUtilities

/**
 * Real Google sign-in hosted inside Chromium (JCEF), mirroring BitChord's
 * Android WebView flow. Passwords and 2FA are entered only into Google's page;
 * Primo Music only reads the completed YouTube Music session cookies.
 */
object DesktopGoogleLogin {
    data class Session(val cookie: String)

    private const val MUSIC_ORIGIN = "https://music.youtube.com"
    private const val LOGIN_URL =
        "https://accounts.google.com/ServiceLogin?ltmpl=music&service=youtube&passive=true" +
            "&continue=https%3A%2F%2Fmusic.youtube.com%2F"

    private val baseDir = File(System.getProperty("user.home"), ".primo-music").apply { mkdirs() }
    private val sessionFile = File(baseDir, "session.properties")

    @Volatile private var cefApp: CefApp? = null
    @Volatile private var client: CefClient? = null
    @Volatile private var activeBrowser: CefBrowser? = null
    @Volatile private var activeFrame: JFrame? = null

    fun restore(): Boolean {
        val session = loadSession() ?: return false
        YouTubeMusicSearchClient.setAuthSession(YouTubeMusicSearchClient.AuthSession(session.cookie))
        return true
    }

    fun hasSession(): Boolean = loadSession() != null

    fun signOut() {
        YouTubeMusicSearchClient.setAuthSession(null)
        sessionFile.delete()
        runCatching { CefCookieManager.getGlobalManager().deleteCookies("", "") }
        runCatching { CefCookieManager.getGlobalManager().flushStore(null) }
    }

    fun open(onConnected: (Session) -> Unit, onError: (String) -> Unit = {}) {
        Thread({
            runCatching {
                val app = ensureCef()
                val cefClient = app.createClient().also { client = it }
                val captured = AtomicBoolean(false)

                cefClient.addLoadHandler(object : CefLoadHandlerAdapter() {
                    override fun onLoadingStateChange(
                        browser: CefBrowser,
                        isLoading: Boolean,
                        canGoBack: Boolean,
                        canGoForward: Boolean,
                    ) {
                        if (isLoading || captured.get()) return
                        val url = browser.url ?: return
                        if (!url.startsWith(MUSIC_ORIGIN)) return
                        captureCookies { cookie ->
                            if (cookie == null || !hasApiSid(cookie)) return@captureCookies
                            if (!captured.compareAndSet(false, true)) return@captureCookies
                            val session = Session(cookie)
                            saveSession(session)
                            YouTubeMusicSearchClient.setAuthSession(YouTubeMusicSearchClient.AuthSession(cookie))
                            onConnected(session)
                            releaseLoginResources()
                        }
                    }
                })

                val browser = cefClient.createBrowser(LOGIN_URL, false, false)
                activeBrowser = browser

                SwingUtilities.invokeLater {
                    val frame = JFrame("Entrar com Google • Primo Music")
                    activeFrame = frame
                    frame.defaultCloseOperation = JFrame.DISPOSE_ON_CLOSE
                    frame.minimumSize = Dimension(900, 680)
                    frame.setSize(1060, 780)
                    frame.setLocationRelativeTo(null)
                    val root = JPanel(BorderLayout())
                    root.add(JLabel("Faça login diretamente no Google. Primo Music não vê sua senha.", SwingConstants.CENTER), BorderLayout.NORTH)
                    root.add(browser.uiComponent, BorderLayout.CENTER)
                    frame.contentPane = root
                    frame.isVisible = true
                    browser.createImmediately()
                }
            }.onFailure { error -> onError(error.message ?: "Não foi possível abrir o login do Google.") }
        }, "PrimoMusic-GoogleLogin").apply { isDaemon = true }.start()
    }


    /**
     * Releases Chromium/JCEF as soon as authentication is finished or when
     * Primo Music exits. JCEF is intentionally not a permanent background
     * service: keeping Chromium alive after sign-in wastes RAM while gaming.
     */
    fun shutdown() {
        releaseLoginResources()
        YouTubeMusicSearchClient.currentAuthSession()
    }

    private fun releaseLoginResources() {
        val browser = activeBrowser
        val frame = activeFrame
        val cefClient = client
        val app = cefApp

        activeBrowser = null
        activeFrame = null
        client = null
        cefApp = null

        SwingUtilities.invokeLater {
            runCatching { frame?.dispose() }
        }

        runCatching {
            browser?.javaClass?.methods
                ?.firstOrNull { it.name == "close" && it.parameterCount == 1 }
                ?.invoke(browser, true)
        }
        runCatching {
            cefClient?.javaClass?.methods
                ?.firstOrNull { it.name == "dispose" && it.parameterCount == 0 }
                ?.invoke(cefClient)
        }
        runCatching {
            app?.javaClass?.methods
                ?.firstOrNull { it.name == "dispose" && it.parameterCount == 0 }
                ?.invoke(app)
        }
    }

    private fun ensureCef(): CefApp {
        cefApp?.let { return it }
        synchronized(this) {
            cefApp?.let { return it }
            val installDir = File(baseDir, "jcef-bundle").apply { mkdirs() }
            val cacheDir = File(baseDir, "browser-profile").apply { mkdirs() }
            val builder = CefAppBuilder()
            builder.setInstallDir(installDir)
            builder.getCefSettings().windowless_rendering_enabled = false
            builder.getCefSettings().cache_path = cacheDir.absolutePath
            builder.getCefSettings().persist_session_cookies = true
            return builder.build().also { cefApp = it }
        }
    }

    private fun captureCookies(callback: (String?) -> Unit) {
        val cookies = linkedMapOf<String, String>()
        val manager = CefCookieManager.getGlobalManager()
        val accepted = manager.visitUrlCookies(MUSIC_ORIGIN, true, object : CefCookieVisitor {
            override fun visit(cookie: CefCookie, count: Int, total: Int, delete: BoolRef): Boolean {
                if (cookie.name.isNotBlank() && cookie.value.isNotBlank()) cookies[cookie.name] = cookie.value
                if (count >= total - 1) {
                    manager.flushStore(null)
                    callback(cookies.entries.joinToString("; ") { "${it.key}=${it.value}" }.takeIf { it.isNotBlank() })
                }
                return true
            }
        })
        if (!accepted) callback(null)
    }

    private fun hasApiSid(cookie: String): Boolean = cookie.split(';').any { entry ->
        val name = entry.substringBefore('=').trim()
        val value = entry.substringAfter('=', "").trim()
        name in setOf("SAPISID", "__Secure-3PAPISID", "__Secure-1PAPISID") && value.isNotEmpty()
    }

    private fun saveSession(session: Session) {
        val props = Properties()
        props["cookie"] = Base64.getEncoder().encodeToString(session.cookie.toByteArray(Charsets.UTF_8))
        sessionFile.outputStream().use { props.store(it, "Primo Music Google session") }
    }

    private fun loadSession(): Session? {
        if (!sessionFile.isFile) return null
        return runCatching {
            val props = Properties().also { p -> sessionFile.inputStream().use(p::load) }
            val encoded = props.getProperty("cookie") ?: return@runCatching null
            val cookie = String(Base64.getDecoder().decode(encoded), Charsets.UTF_8)
            cookie.takeIf(::hasApiSid)?.let(::Session)
        }.getOrNull()
    }
}
