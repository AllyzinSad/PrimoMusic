import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.gradle.api.tasks.bundling.Zip
import org.gradle.jvm.tasks.Jar
import java.net.HttpURLConnection
import java.net.URI
import java.security.MessageDigest
import java.util.zip.ZipInputStream
import java.util.zip.ZipFile

plugins {
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.compose")
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    implementation(project(":core"))
    implementation(compose.desktop.currentOs)
    implementation(compose.material3)
    implementation(compose.materialIconsExtended)

    // Compose Desktop startup uses Lifecycle/SavedState. Keep the matching
    // multiplatform runtime explicit so native distributions are deterministic.
    implementation("org.jetbrains.androidx.lifecycle:lifecycle-runtime:2.9.6")
    implementation("org.jetbrains.androidx.lifecycle:lifecycle-viewmodel:2.9.6")
    implementation("org.jetbrains.androidx.lifecycle:lifecycle-viewmodel-savedstate:2.9.6")
    implementation("org.jetbrains.androidx.savedstate:savedstate:1.3.6")
    implementation("io.coil-kt.coil3:coil-compose:3.0.4")
    implementation("io.coil-kt.coil3:coil-network-okhttp:3.0.4")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
    implementation("io.ktor:ktor-client-core:3.0.3")
    implementation("io.ktor:ktor-client-okhttp:3.0.3")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("io.ktor:ktor-client-content-negotiation:3.0.3")
    implementation("io.ktor:ktor-serialization-kotlinx-json:3.0.3")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")

    // Local YouTube stream extraction/deobfuscation. This is the same
    // NewPipeExtractor line used by the preserved BitChord Android module.
    implementation("com.github.TeamNewPipe:NewPipeExtractor:v0.26.3") {
        isTransitive = false
    }
    implementation("com.github.TeamNewPipe:nanojson:e9d656ddb49a412a5a0a5d5ef20ca7ef09549996")
    implementation("org.jsoup:jsoup:1.22.2")
    implementation("com.google.code.findbugs:jsr305:3.0.2")
    implementation("com.google.protobuf:protobuf-javalite:4.35.0")
    implementation("org.mozilla:rhino:1.8.1")
    implementation("org.mozilla:rhino-engine:1.8.1")

    implementation("me.friwi:jcefmaven:146.0.10")
    implementation("io.github.kashif-mehmood-km:backdrop:0.0.1-alpha02")
}

/*
 * mpv is a build-time dependency of the Windows distribution, not a runtime
 * download. The archive is fetched once while creating/running the Desktop
 * build, verified by SHA-256, and only the required mpv.exe is placed in
 * appResourcesRootDir. End users receive it inside the EXE/MSI installer.
 */
val mpvVersion = "0.41.0"
val mpvArchiveName = "mpv-v$mpvVersion-x86_64-pc-windows-msvc.zip"
val mpvDownloadUrl =
    "https://github.com/mpv-player/mpv/releases/download/v$mpvVersion/$mpvArchiveName"
val mpvArchiveSha256 =
    "4e197f729f5071c6772f35fffd96e0f36e3e8a044bd9479b136bb09b7c6a80ff"

val mpvBuildCacheDir = rootProject.layout.projectDirectory.dir(".primo-cache/mpv").asFile
val packagedResourcesRoot = layout.buildDirectory.dir("packaging-resources")
val bundledMpvExe = packagedResourcesRoot.map { it.file("windows-x64/mpv/mpv.exe") }
val bundledMpvNotice = packagedResourcesRoot.map { it.file("common/THIRD-PARTY-MPV.txt") }

fun sha256(file: File): String {
    val digest = MessageDigest.getInstance("SHA-256")
    file.inputStream().use { input ->
        val buffer = ByteArray(64 * 1024)
        while (true) {
            val read = input.read(buffer)
            if (read <= 0) break
            digest.update(buffer, 0, read)
        }
    }
    return digest.digest().joinToString("") { "%02x".format(it) }
}

val prepareBundledMpv by tasks.registering {
    group = "distribution"
    description = "Downloads and verifies mpv for bundling inside Koda Music."

    inputs.property("mpvVersion", mpvVersion)
    inputs.property("mpvArchiveSha256", mpvArchiveSha256)
    outputs.file(bundledMpvExe)
    outputs.file(bundledMpvNotice)

    doLast {
        val destination = bundledMpvExe.get().asFile
        val cacheDir = mpvBuildCacheDir.apply { mkdirs() }
        if (!destination.isFile || destination.length() == 0L) {
            val archive = File(cacheDir, mpvArchiveName)

            val archiveValid =
                archive.isFile &&
                    sha256(archive).equals(
                        mpvArchiveSha256,
                        ignoreCase = true,
                    )

            if (!archiveValid) {
                archive.delete()
                val temporary = File(cacheDir, "$mpvArchiveName.part")
                temporary.delete()

                println("Koda Music: preparando mpv $mpvVersion para o pacote Windows...")

                val connection =
                    URI(mpvDownloadUrl)
                        .toURL()
                        .openConnection() as HttpURLConnection

                connection.instanceFollowRedirects = true
                connection.connectTimeout = 20_000
                connection.readTimeout = 120_000
                connection.setRequestProperty(
                    "User-Agent",
                    "KodaMusic-Build/3.11.0",
                )

                try {
                    connection.inputStream.use { input ->
                        temporary.outputStream().buffered().use { output ->
                            input.copyTo(output)
                        }
                    }
                } finally {
                    connection.disconnect()
                }

                val actualHash = sha256(temporary)
                check(
                    actualHash.equals(
                        mpvArchiveSha256,
                        ignoreCase = true,
                    )
                ) {
                    "SHA-256 inesperado para mpv. Esperado=$mpvArchiveSha256 recebido=$actualHash"
                }

                temporary.copyTo(archive, overwrite = true)
                temporary.delete()
            }

            destination.parentFile.mkdirs()

            var found = false
            ZipInputStream(archive.inputStream().buffered()).use { zip ->
                while (true) {
                    val entry = zip.nextEntry ?: break
                    if (
                        !entry.isDirectory &&
                        entry.name.substringAfterLast('/').equals(
                            "mpv.exe",
                            ignoreCase = true,
                        )
                    ) {
                        destination.outputStream().buffered().use { output ->
                            zip.copyTo(output)
                        }
                        found = true
                        break
                    }
                }
            }

            check(found && destination.isFile && destination.length() > 0L) {
                "mpv.exe não foi encontrado dentro do pacote verificado."
            }
        }

        val notice = bundledMpvNotice.get().asFile
        notice.parentFile.mkdirs()
        notice.writeText(
            """
            Koda Music bundles mpv $mpvVersion for Windows audio playback.
            Project: https://mpv.io/
            Source: https://github.com/mpv-player/mpv
            Release archive: $mpvArchiveName
            SHA-256: $mpvArchiveSha256

            mpv is free and open-source software. See the mpv project and the
            Koda Music THIRD_PARTY-NOTICES-PRIMO.md file for licensing details.
            """.trimIndent() + "\n"
        )
    }
}


/*
 * Portable packaging has one source of truth:
 * the runtimeClasspath resolved by Gradle and the main JAR produced by :desktop:jar.
 *
 * We deliberately disable Compose Desktop's implicit SourceSet wiring here and feed
 * those files explicitly to jpackage. This avoids the chain of post-build classpath
 * patches used during diagnosis (PrimoMusic-main.jar / portable-libs / cfg rewrites).
 */
val desktopJar = tasks.named<Jar>("jar")
val desktopRuntimeClasspath = configurations.getByName("runtimeClasspath")

compose.desktop {
    application {
        mainClass = "com.primomusic.desktop.MainKt"

        disableDefaultConfiguration()
        fromFiles(desktopRuntimeClasspath)
        fromFiles(desktopJar.flatMap { it.archiveFile })
        mainJar.set(desktopJar.flatMap { it.archiveFile })
        dependsOn("jar", ":core:jar")

        nativeDistributions {
            targetFormats(TargetFormat.Exe, TargetFormat.Msi)
            packageName = "KodaMusic"
            packageVersion = "3.11.0"
            description = "Koda Music desktop player for Windows"
            vendor = "Koda Music"
            licenseFile.set(rootProject.file("LICENSE"))

            // Keep the portable runtime self-contained. The application uses
            // java.net.http and other JDK modules dynamically.
            includeAllModules = true

            // Compose copies common/ + the active target directory into
            // <app>/app/resources and exposes that directory through
            // compose.application.resources.dir. RuntimeTools reads mpv from there.
            appResourcesRootDir.set(packagedResourcesRoot)

            windows {
                iconFile.set(project.file("p-music.ico"))
                menuGroup = "Koda Music"
                perUserInstall = true
                dirChooser = true

                // Public release: run as a normal Windows GUI application.
                console = false
            }
        }
    }
}

// The resource-copy task owns the mpv preparation dependency. This keeps the
// packaging graph linear: prepare mpv -> copy app resources -> jpackage.
tasks.matching { it.name.startsWith("prepare") && it.name.endsWith("AppResources") }
    .configureEach { dependsOn(prepareBundledMpv) }

val portableAppImage = layout.buildDirectory.dir("compose/binaries/main/app/KodaMusic")

/** Returns true when one of [jars] physically contains [entry]. */
fun classExistsInJars(jars: List<File>, entry: String): Boolean =
    jars.any { jar ->
        runCatching {
            ZipFile(jar).use { zip -> zip.getEntry(entry) != null }
        }.getOrDefault(false)
    }

/*
 * Validation only: this task never repairs or rewrites the image.
 * If Compose/jpackage creates an incomplete app, the build stops instead of
 * adding another compatibility layer on top of it.
 */
val verifyPortableImage by tasks.registering {
    group = "verification"
    description = "Verifies the self-contained Koda Music app-image before zipping it."
    dependsOn("createDistributable")

    doLast {
        val image = portableAppImage.get().asFile
        val appDir = File(image, "app")
        val cfg = File(appDir, "KodaMusic.cfg")
        val exe = File(image, "KodaMusic.exe")
        val runtimeModules = File(image, "runtime/lib/modules")
        val jli = File(image, "runtime/bin/jli.dll")
        val jvm = File(image, "runtime/bin/server/jvm.dll")

        check(exe.isFile) { "KodaMusic.exe nao foi gerado: ${exe.absolutePath}" }
        check(cfg.isFile) { "KodaMusic.cfg nao foi gerado: ${cfg.absolutePath}" }
        check(runtimeModules.isFile) { "Runtime Java incompleto: ${runtimeModules.absolutePath}" }

        // These DLLs are Windows-specific. Do not fail Gradle configuration on
        // another OS, but require them on the Windows machine that creates release.
        if (System.getProperty("os.name").startsWith("Windows", ignoreCase = true)) {
            check(jli.isFile) { "Runtime Java sem jli.dll: ${jli.absolutePath}" }
            check(jvm.isFile) { "Runtime Java sem jvm.dll: ${jvm.absolutePath}" }

            val mpv = File(appDir, "resources/mpv/mpv.exe")
            check(mpv.isFile && mpv.length() > 0L) {
                "mpv nao foi empacotado em app/resources/mpv/mpv.exe"
            }
            println("OK: mpv empacotado pelo appResources do Compose (${mpv.length()} bytes).")
        }

        // No repair directories from previous fixes are allowed in the final image.
        check(!File(appDir, "portable-libs").exists()) {
            "Imagem contem portable-libs legado; limpe desktop/build e gere novamente."
        }
        check(!File(appDir, "PrimoMusic-main.jar").exists()) {
            "Imagem contem PrimoMusic-main.jar legado; limpe desktop/build e gere novamente."
        }

        val jars = appDir.walkTopDown()
            .filter { it.isFile && it.extension.equals("jar", ignoreCase = true) }
            .toList()

        check(jars.size >= 5) { "Classpath empacotado suspeito: apenas ${jars.size} JARs." }

        val requiredClasses = listOf(
            "com/primomusic/desktop/MainKt.class",
            "com/primomusic/core/PrimoMusicCore.class",
            "kotlin/NoWhenBranchMatchedException.class",
            "kotlinx/coroutines/CoroutineScope.class",
            "androidx/lifecycle/SavedStateViewModelFactory.class",
            "org/schabi/newpipe/extractor/NewPipe.class",
        )

        requiredClasses.forEach { required ->
            check(classExistsInJars(jars, required)) {
                "Classpath incompleto: $required nao foi encontrado no app-image."
            }
            println("OK: $required")
        }

        val cfgText = cfg.readText(Charsets.UTF_8)
        check(cfgText.contains("app.mainclass=com.primomusic.desktop.MainKt")) {
            "KodaMusic.cfg nao aponta para com.primomusic.desktop.MainKt."
        }
        val classpathCount = cfgText.lineSequence().count { it.startsWith("app.classpath=") }
        check(classpathCount >= 5) {
            "KodaMusic.cfg possui apenas $classpathCount entradas de classpath."
        }

        File(image, "LEIA-ME.txt").writeText(
            """
            Koda Music 3.11.0 - Portable

            1. Mantenha toda esta pasta junta.
            2. Abra KodaMusic.exe.
            3. Java e mpv ja fazem parte do pacote.

            O pacote foi validado antes da criacao do ZIP: MainKt, Kotlin,
            Coroutines, Lifecycle, NewPipe, runtime Java e mpv foram conferidos.
            """.trimIndent() + "\n",
            Charsets.UTF_8,
        )

        println("OK: app-image portatil validada com ${jars.size} JARs e $classpathCount entradas de classpath.")
    }
}

val portableZip by tasks.registering(Zip::class) {
    group = "distribution"
    description = "Builds the verified Koda Music Windows portable ZIP."
    dependsOn(verifyPortableImage)

    from(portableAppImage) {
        into("KodaMusic-3.11.0-Portable")
    }

    archiveFileName.set("KodaMusic-3.11.0-Portable.zip")
    destinationDirectory.set(rootProject.layout.projectDirectory.dir("release"))
    isPreserveFileTimestamps = false
    isReproducibleFileOrder = true

    doLast {
        println("PORTABLE GERADO COM SUCESSO")
        println("ZIP: ${archiveFile.get().asFile.absolutePath}")
    }
}
