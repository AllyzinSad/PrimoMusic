plugins {
    id("com.android.application")
}
android {
    namespace = "com.koda.cut.beta"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.koda.cut.beta"
        minSdk = 26
        targetSdk = 35
        versionCode = 4
        versionName = "0.3-beta"
    }
}


dependencies {
    implementation("dev.ffmpegkit-maintained:ffmpeg-kit-full:8.1.8")
}
