plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("io.gitlab.arturbosch.detekt")
}

android {
    namespace = "com.visionrt.inference"
    compileSdk = 36

    defaultConfig {
        minSdk = 30
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(project(":core"))
    implementation(project(":perception"))

    // LiteRT / TFLite runtime (ARCHITECTURE §6.5, §11). Package is
    // org.tensorflow.lite for API compatibility; proguard-rules keep those
    // reflective entries already.
    implementation("org.tensorflow:tensorflow-lite:2.16.1")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")

    testImplementation("junit:junit:4.13.2")
    // org.json ships on Android; JVM unit tests need the standalone artifact.
    testImplementation("org.json:json:20240303")
}