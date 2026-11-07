import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

val local = Properties().apply {
    project.file("local.properties").takeIf { it.exists() }?.inputStream()?.use(::load)
}

android {
    namespace = "io.github.whatwhatwhoa.dailynote"
    compileSdk = 37

    defaultConfig {
        applicationId = "io.github.whatwhatwhoa.dailynote"
        minSdk = 31
        targetSdk = 37
        versionCode = 2
        versionName = "0.2.0"
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
        // published release replace each other on a phone without losing the note.
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
    // :stack (the series' look, with Compose and Material 3) only for the paper sheet and the
    // undo pill; the page itself is a text field on a canvas. No icon pack, no DataStore, no
    // Glance: the APK stays small and starts fast.
    implementation(project(":stack"))
    implementation(libs.activity.compose)
}
