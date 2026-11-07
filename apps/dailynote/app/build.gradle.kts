import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

val local = Properties().apply {
    rootProject.file("local.properties").takeIf { it.exists() }?.inputStream()?.use(::load)
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

    // The release key stays outside the repo; local.properties says where it is. Without it a
    // release build is left unsigned.
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
    // Material 3 only for the paper sheet and the undo pill; the page itself is a text field on
    // a canvas. No icon pack, no DataStore, no Glance: the APK stays small and starts fast.
    implementation(platform("androidx.compose:compose-bom:2026.09.00"))
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.activity:activity-compose:1.13.0")
}
