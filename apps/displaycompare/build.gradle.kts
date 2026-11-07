import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

val local = Properties().apply {
    project.file("local.properties").takeIf { it.exists() }?.inputStream()?.use(::load)
}

android {
    namespace = "io.github.whatwhatwhoa.displaycompare"
    compileSdk = 37

    defaultConfig {
        applicationId = "io.github.whatwhatwhoa.displaycompare"
        minSdk = 31
        targetSdk = 37
        versionCode = 2
        versionName = "0.1.1"
    }

    // The release key stays outside the repo; this app's own local.properties (gitignored, next to
    // this file) says where it is. Without it a release build is left unsigned.
    val releaseStore = local.getProperty("release.storeFile")
    signingConfigs {
        if (releaseStore != null) {
            create("release") {
                storeFile = file(releaseStore)
                storePassword = local.getProperty("release.storePassword")
                keyAlias = local.getProperty("release.keyAlias")
                keyPassword = local.getProperty("release.keyPassword")
            }
        }
    }

    buildTypes {
        // On a PC with the release key, debug builds are signed with it too, so a test build and a
        // published release replace each other on a phone without losing the saved displays.
        debug {
            signingConfigs.findByName("release")?.let { signingConfig = it }
        }
        release {
            signingConfig = signingConfigs.findByName("release")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
        }
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    // :stack brings Compose, Material 3 and the Stack look; no other library.
    implementation(project(":stack"))
    // :update: Settings → Updates and the banner, from the series' GitHub releases.
    implementation(project(":update"))
    implementation(libs.activity.compose)
}
