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

// Each app is a module under apps/<name>/, built as :<name> (./gradlew :powermeter:assembleDebug).
for (app in listOf("powermeter", "dailynote")) {
    include(":$app")
    project(":$app").projectDir = file("apps/$app")
}
