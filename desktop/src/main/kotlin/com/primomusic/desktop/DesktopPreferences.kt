package com.primomusic.desktop

import java.io.File
import java.util.Properties

internal object DesktopPreferences {
    private val dir = File(System.getProperty("user.home"), ".primo-music").apply { mkdirs() }
    private val file = File(dir, "settings.properties")

    @Synchronized
    fun liquidGlassEnabled(): Boolean = load().getProperty("liquidGlass", "false").toBooleanStrictOrNull() ?: false

    @Synchronized
    fun setLiquidGlassEnabled(enabled: Boolean) = edit { setProperty("liquidGlass", enabled.toString()) }

    @Synchronized
    fun darkThemeEnabled(): Boolean = load().getProperty("darkTheme", "true").toBooleanStrictOrNull() ?: true

    @Synchronized
    fun setDarkThemeEnabled(enabled: Boolean) = edit { setProperty("darkTheme", enabled.toString()) }

    @Synchronized
    fun audioQuality(): AudioQuality = runCatching {
        AudioQuality.valueOf(load().getProperty("audioQuality", AudioQuality.AUTO.name))
    }.getOrDefault(AudioQuality.AUTO)

    @Synchronized
    fun setAudioQuality(value: AudioQuality) = edit { setProperty("audioQuality", value.name) }

    private fun edit(block: Properties.() -> Unit) {
        val props = load().apply(block)
        file.outputStream().use { props.store(it, "Primo Music desktop settings") }
    }

    private fun load(): Properties = Properties().also { props ->
        if (file.isFile) runCatching { file.inputStream().use(props::load) }
    }
}
