package com.primomusic.desktop

import java.io.File
import java.util.Properties

internal enum class DesktopTheme {
    MONOCHROME,
    PURPLE,
}

internal object DesktopPreferences {
    private val dir = File(System.getProperty("user.home"), ".primo-music").apply { mkdirs() }
    private val file = File(dir, "settings.properties")

    @Synchronized
    fun liquidGlassEnabled(): Boolean =
        load().getProperty("liquidGlass", "false").toBooleanStrictOrNull() ?: false

    @Synchronized
    fun setLiquidGlassEnabled(enabled: Boolean) =
        edit { setProperty("liquidGlass", enabled.toString()) }

    /**
     * 3.11 replaces the old light/dark switch with two intentionally dark
     * desktop themes. Keep the legacy preference as a migration fallback so an
     * existing install never loses its settings file.
     */
    @Synchronized
    fun theme(): DesktopTheme =
        runCatching {
            DesktopTheme.valueOf(load().getProperty("theme", DesktopTheme.PURPLE.name))
        }.getOrDefault(DesktopTheme.PURPLE)

    @Synchronized
    fun setTheme(value: DesktopTheme) =
        edit { setProperty("theme", value.name) }

    @Synchronized
    fun gamerModeEnabled(): Boolean =
        load().getProperty("gamerMode", "false").toBooleanStrictOrNull() ?: false

    @Synchronized
    fun setGamerModeEnabled(enabled: Boolean) =
        edit { setProperty("gamerMode", enabled.toString()) }

    @Synchronized
    fun reduceAnimationsEnabled(): Boolean =
        load().getProperty("reduceAnimations", "false").toBooleanStrictOrNull() ?: false

    @Synchronized
    fun setReduceAnimationsEnabled(enabled: Boolean) =
        edit { setProperty("reduceAnimations", enabled.toString()) }

    @Synchronized
    fun audioQuality(): AudioQuality = runCatching {
        AudioQuality.valueOf(load().getProperty("audioQuality", AudioQuality.AUTO.name))
    }.getOrDefault(AudioQuality.AUTO)

    @Synchronized
    fun setAudioQuality(value: AudioQuality) =
        edit { setProperty("audioQuality", value.name) }

    // Compatibility for pre-3.11 code paths while the migration branch is tested.
    @Synchronized
    fun darkThemeEnabled(): Boolean = true

    @Synchronized
    fun setDarkThemeEnabled(enabled: Boolean) = Unit

    private fun edit(block: Properties.() -> Unit) {
        val props = load().apply(block)
        file.outputStream().use { props.store(it, "Koda Music desktop settings") }
    }

    private fun load(): Properties = Properties().also { props ->
        if (file.isFile) runCatching { file.inputStream().use(props::load) }
    }
}
