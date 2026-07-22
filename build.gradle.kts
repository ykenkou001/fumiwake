// NOTE: No plugins are declared at the root, intentionally.
//
// 1. com.android.application (AGP) cannot be resolved in the local dev
//    environment (no Android SDK; dl.google.com / maven.google.com artifact
//    fetches are blocked), so it must only be requested from app/build.gradle.kts,
//    which is never evaluated locally (:app is excluded by settings.gradle.kts
//    when no Android SDK is present).
// 2. The Kotlin plugins must NOT be declared here either (even with
//    `apply false`): that loads them in the root build classpath while AGP
//    lives only in :app's classpath, and kotlin-android then fails with
//    "Could not generate a decorated class for KotlinAndroidTarget:
//    com/android/build/gradle/api/BaseVariant". Declaring AGP and
//    kotlin-android together in app/build.gradle.kts keeps them in one
//    classloader.
//
// See docs/IMPLEMENTATION_PLAN.md §1/§2.
