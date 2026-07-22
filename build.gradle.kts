// NOTE: com.android.application (AGP) is intentionally NOT declared here.
// This repo's local dev environment has no Android SDK and dl.google.com /
// maven.google.com artifact fetches are blocked, so resolving the AGP plugin
// marker fails even with `apply false`. AGP is applied directly in
// app/build.gradle.kts instead; since :app is excluded from settings.gradle.kts
// when no Android SDK is present, that file (and the AGP resolution it
// triggers) is never evaluated locally. See docs/IMPLEMENTATION_PLAN.md §1/§2.
plugins {
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.kotlin.compose) apply false
}
