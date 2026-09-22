pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // NewPipeExtractor is published via JitPack.
        maven("https://jitpack.io")
    }
}

rootProject.name = "PrimoMusic"

// Primo Music is developed as a Windows desktop application first. The original
// BitChord Android module remains preserved in /app, but it is opt-in so that a
// desktop-only Windows machine does not need Android Studio or an Android SDK
// just to configure and run :desktop.
val includeAndroid = providers
    .gradleProperty("includeAndroid")
    .orNull
    ?.toBooleanStrictOrNull()
    ?: false

include(":core")
include(":desktop")

if (includeAndroid) {
    include(":app")
} else {
    logger.lifecycle(
        "Primo Music desktop mode: loading :core and :desktop only. " +
            "The original Android :app is preserved and can be enabled with -PincludeAndroid=true."
    )
}
