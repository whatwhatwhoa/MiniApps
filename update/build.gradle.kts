// :update, updates from GitHub for the series' apps: asks this repo's releases for the newest one
// tagged <app>-v<version>, downloads its APK and hands it to Android's installer. The Stack card and
// banner for it are here too. Every app depends on it (docs/architecture.md *Updates*).
plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "io.github.whatwhatwhoa.update"
    compileSdk = 37
    defaultConfig { minSdk = 31 }
    buildFeatures { compose = true }
}

dependencies {
    implementation(project(":stack"))
}
