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
}
