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

    // 配布用の署名鍵は GitHub Secrets 経由で CI が環境変数として渡す
    // （FUMIWAKE_KEYSTORE_FILE = 復号済み keystore のパス）。鍵はリポジトリに
    // 置かない。ローカルや Secrets 未設定の CI では null になり、release は
    // 未署名になる（その場合 CI は debug ビルドを配布する — ci.yml 参照）。
    val distKeystorePath = System.getenv("FUMIWAKE_KEYSTORE_FILE")

    signingConfigs {
        if (!distKeystorePath.isNullOrBlank()) {
            create("dist") {
                storeFile = file(distKeystorePath)
                storePassword = System.getenv("FUMIWAKE_STORE_PASSWORD")
                keyAlias = System.getenv("FUMIWAKE_KEY_ALIAS")
                // PKCS12 では鍵パスワード = ストアパスワード
                keyPassword = System.getenv("FUMIWAKE_STORE_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            if (!distKeystorePath.isNullOrBlank()) {
                signingConfig = signingConfigs.getByName("dist")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
            // alphaTab's public Kotlin API is annotated @ExperimentalContracts;
            // opt in module-wide instead of per call site
            freeCompilerArgs.add("-opt-in=kotlin.contracts.ExperimentalContracts")
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
    // alphaTab が引き込む alphaSkia 3.4.135 を 16KB ページ対応版に押し上げる
    // （Gradle の競合解決は新しい方を採用する）。libs.versions.toml の
    // alphaskia のコメント参照。
    implementation(libs.alphaskia)
    implementation(libs.alphaskia.android)
    implementation(libs.kaml)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.core)

    testImplementation(libs.junit4)
}
