// The series' toolchain (versions in gradle/libs.versions.toml). AGP 9 compiles Kotlin itself;
// the Kotlin plugin is declared only to pin its version to the Compose compiler's, which must match.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
}
