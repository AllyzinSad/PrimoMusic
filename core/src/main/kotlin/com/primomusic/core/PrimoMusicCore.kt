package com.primomusic.core

/**
 * Shared JVM entry point for code that can be reused by Android and desktop.
 *
 * The first desktop milestone keeps this module deliberately small so the
 * existing Android application remains untouched. Music/networking code will
 * be migrated here incrementally after each build is proven stable.
 */
object PrimoMusicCore {
    const val APP_NAME = "Koda Music"
    const val DESKTOP_STAGE = "Desktop foundation"
}
