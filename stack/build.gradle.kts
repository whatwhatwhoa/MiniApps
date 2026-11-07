// :stack, the Stack look for the series' apps: color tokens, type, theme and components, after
// the Stack Design Guide (docs/stack-android.md). Every app depends on it; nothing else does.
plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "io.github.whatwhatwhoa.stack"
    compileSdk = 37
    defaultConfig { minSdk = 31 }
    buildFeatures { compose = true }
}

dependencies {
    // api: an app that uses :stack gets Compose and Material 3 with it, at the series' versions.
    api(platform(libs.compose.bom))
    api(libs.compose.material3)
    api(libs.compose.ui)
}
