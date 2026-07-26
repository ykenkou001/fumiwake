plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
}

repositories {
    mavenCentral()
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

dependencies {
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kaml)

    testImplementation(kotlin("test-junit5"))
    testImplementation(libs.junit.jupiter)
}

tasks.test {
    useJUnitPlatform()
    systemProperty("fumiwake.content.dir", rootDir.resolve("content/exercises").absolutePath)

    // 同梱コンテンツはテストの入力である（ContentExercisesTest / ConceptReachabilityTest が
    // 読む）。宣言しないと、コンテンツだけを変更したときに Gradle が UP-TO-DATE と判断して
    // 検証テストを飛ばしてしまう。コンテンツ追加こそ最も検証が要る変更なので、これは危険。
    inputs.dir(rootDir.resolve("content/exercises"))
        .withPropertyName("bundledContent")
        .withPathSensitivity(PathSensitivity.RELATIVE)
}
