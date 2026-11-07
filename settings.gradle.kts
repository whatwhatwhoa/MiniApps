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
    }
}

rootProject.name = "MiniApps"

// :stack is the shared Stack look, :update the updates from GitHub; every app uses both. Every
// folder under apps/ with a build.gradle.kts is an app, built as :<folder> (./gradlew :powermeter:assembleDebug): a new app needs no line here.
include(":stack")
include(":update")
file("apps").listFiles { f -> f.isDirectory && f.resolve("build.gradle.kts").isFile }
    ?.sortedBy { it.name }
    ?.forEach { dir ->
        include(":${dir.name}")
        project(":${dir.name}").projectDir = dir
    }
