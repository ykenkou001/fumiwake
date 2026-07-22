plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "dev.fumiwake.app"
    // 36 required: androidx.core 1.17 (pulled transitively by alphaTab) refuses compileSdk < 36
    compileSdk = 36

    defaultConfig {
        applicationId = "dev.fumiwake.app"
        minSdk = 26
        targetSdk = 35
        // CI 採番で上書きアップデートを可能にする（ローカルビルドは 1）
        versionCode = System.getenv("GITHUB_RUN_NUMBER")?.toIntOrNull() ?: 1
        versionName = "0.1.${System.getenv("GITHUB_RUN_NUMBER") ?: "dev"}"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            // 配布用の署名は未設定（配布は当面 debug ビルドで行う — ci.yml 参照）。
            // 署名を固定する場合は GitHub Secrets 経由で signingConfig を追加する。
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }

    buildFeatures {
        compose = true
    }

    sourceSets["main"].assets.srcDirs("../content/exercises")
}

dependencies {
    implementation(project(":core"))

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling.preview)
    debugImplementation(libs.compose.ui.tooling)
    implementation(libs.compose.material3)
    implementation(libs.activity.compose)
    implementation(libs.navigation.compose)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.core.ktx)
    implementation(libs.appcompat)

    implementation(libs.alphatab)
    implementation(libs.kaml)
    implementation(libs.kotlinx.serialization.json)

    testImplementation(libs.junit4)
}
