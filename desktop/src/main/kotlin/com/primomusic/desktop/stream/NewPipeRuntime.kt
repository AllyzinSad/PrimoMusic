package com.primomusic.desktop.stream

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.ConnectionPool
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.downloader.Request
import org.schabi.newpipe.extractor.downloader.Response
import org.schabi.newpipe.extractor.exceptions.ReCaptchaException
import org.schabi.newpipe.extractor.services.youtube.YoutubeJavaScriptPlayerManager
import java.util.concurrent.TimeUnit

/**
 * Shared NewPipe runtime used by Primo Music Desktop.
 *
 * This mirrors the original BitChord resolver's important behavior:
 * - NewPipeExtractor 0.26.3 is initialized with an OkHttp downloader;
 * - the slow/unneeded /youtubei/v1/next call is answered locally;
 * - player JavaScript operations are serialized because NewPipe keeps
 *   process-wide parser/cache state;
 * - the extractor has a short, dedicated connection pool so a stale socket
 *   cannot keep the fallback hanging indefinitely.
 */
internal object NewPipeRuntime {
    private const val NEXT_ENDPOINT = "/youtubei/v1/next"

    private const val EMPTY_NEXT_RESPONSE =
        """{"responseContext":{},"contents":{},"currentVideoEndpoint":{},"trackingParams":""}"""

    const val CHROME_USER_AGENT =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
            "(KHTML, like Gecko) Chrome/141.0.0.0 Safari/537.36"

    private val extractorClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectionPool(ConnectionPool(4, 30, TimeUnit.SECONDS))
            .pingInterval(5, TimeUnit.SECONDS)
            .callTimeout(7, TimeUnit.SECONDS)
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(7, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .build()
    }

    private class DesktopDownloader : Downloader() {
        override fun execute(request: Request): Response {
            if (NEXT_ENDPOINT in request.url()) {
                return Response(
                    200,
                    "OK",
                    emptyMap(),
                    EMPTY_NEXT_RESPONSE,
                    request.url(),
                )
            }

            val builder = okhttp3.Request.Builder()
                .method(
                    request.httpMethod(),
                    request.dataToSend()?.toRequestBody(),
                )
                .url(request.url())

            var hasUserAgent = false
            request.headers().forEach { (name, values) ->
                if (name.equals("User-Agent", ignoreCase = true) && values.isNotEmpty()) {
                    hasUserAgent = true
                }

                when {
                    values.size > 1 -> {
                        builder.removeHeader(name)
                        values.forEach { value -> builder.addHeader(name, value) }
                    }
                    values.size == 1 -> builder.header(name, values[0])
                }
            }

            if (!hasUserAgent) {
                builder.header("User-Agent", CHROME_USER_AGENT)
            }

            val started = System.nanoTime()
            val response = try {
                extractorClient.newCall(builder.build()).execute()
            } catch (error: Exception) {
                val tookMs = (System.nanoTime() - started) / 1_000_000L
                println(
                    "[NewPipe] HTTP FALHOU ${tookMs}ms ${request.httpMethod()} " +
                        "${safeHost(request.url())}: ${error::class.simpleName}: ${error.message}"
                )
                throw error
            }

            val tookMs = (System.nanoTime() - started) / 1_000_000L
            println(
                "[NewPipe] HTTP ${response.code} ${tookMs}ms ${request.httpMethod()} " +
                    safeHost(request.url())
            )

            if (response.code == 429) {
                response.close()
                throw ReCaptchaException(
                    "reCaptcha Challenge requested",
                    request.url(),
                )
            }

            return Response(
                response.code,
                response.message,
                response.headers.toMultimap(),
                response.body?.string(),
                response.request.url.toString(),
            )
        }
    }

    private val initialized: Unit by lazy {
        NewPipe.init(DesktopDownloader())
    }

    private val jsPlayerMutex = Mutex()
    internal val extractionMutex = Mutex()

    fun ensureInitialized() {
        initialized
    }

    suspend fun deobfuscateN(
        videoId: String,
        url: String,
    ): String {
        if (!hasQueryParameter(url, "n")) return url

        ensureInitialized()

        return jsPlayerMutex.withLock {
            withContext(Dispatchers.IO) {
                YoutubeJavaScriptPlayerManager
                    .getUrlWithThrottlingParameterDeobfuscated(
                        videoId,
                        url,
                    )
            }
        }
    }

    suspend fun deobfuscateSignature(
        videoId: String,
        signature: String,
    ): String {
        ensureInitialized()

        return jsPlayerMutex.withLock {
            withContext(Dispatchers.IO) {
                YoutubeJavaScriptPlayerManager.deobfuscateSignature(
                    videoId,
                    signature,
                )
            }
        }
    }

    suspend fun signatureTimestamp(videoId: String): Int? {
        ensureInitialized()

        return runCatching {
            jsPlayerMutex.withLock {
                withContext(Dispatchers.IO) {
                    YoutubeJavaScriptPlayerManager.getSignatureTimestamp(videoId)
                }
            }
        }
            .onFailure { error ->
                println(
                    "[NewPipe] signatureTimestamp indisponível: " +
                        (error.message ?: error::class.simpleName)
                )
            }
            .getOrNull()
    }

    private fun hasQueryParameter(
        url: String,
        name: String,
    ): Boolean {
        val marker = Regex("(?:[?&])${Regex.escape(name)}=[^&]+")
        return marker.containsMatchIn(url)
    }

    private fun safeHost(url: String): String =
        runCatching {
            java.net.URI.create(url).host ?: "youtube"
        }.getOrDefault("youtube")
}
