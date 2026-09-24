plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("io.gitlab.arturbosch.detekt")
    id("org.jetbrains.kotlin.kapt")
    id("com.google.dagger.hilt.android")
}

android {
    namespace = "com.visionrt.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.visionrt.app"
        minSdk = 30
        targetSdk = 36
        versionCode = 7
        versionName = "0.7.0-M7"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        // Installable debug-like APK without src/debug's HiltTestApplication
        // override (that override is required only for connectedAndroidTest).
        create("demo") {
            initWith(getByName("debug"))
            signingConfig = signingConfigs.getByName("debug")
            matchingFallbacks += listOf("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        viewBinding = true
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(project(":core"))
    implementation(project(":feature"))
    implementation(project(":data"))
    implementation(project(":perception"))
    implementation(project(":inference"))
    implementation(project(":feedback"))
    implementation(project(":benchmark"))

    implementation("androidx.lifecycle:lifecycle-process:2.9.4")

    implementation("androidx.core:core-ktx:1.17.0")
    implementation("androidx.appcompat:appcompat:1.8.0")
    implementation("com.google.android.material:material:1.14.0")
    implementation("androidx.navigation:navigation-fragment-ktx:2.10.1")
    implementation("androidx.navigation:navigation-ui-ktx:2.10.1")

    implementation("com.google.dagger:hilt-android:2.57.2")
    kapt("com.google.dagger:hilt-android-compiler:2.57.2")

    // Makes HiltTestApplication (declared in src/debug/AndroidManifest.xml for
    // @HiltAndroidTest instrumented tests) resolvable to lint and the debug build.
    debugImplementation("com.google.dagger:hilt-android-testing:2.57.2")
    // hilt-android-testing pulls androidx.test:core 1.4.0; consistent-resolution
    // would then clash with the 1.7.0 required by androidTest deps. Upgrade it.
    debugImplementation("androidx.test:core:1.7.0")
    testImplementation("junit:junit:4.13.2")

    // Explicit hamcrest for the instrumented test APK: with the debug variant now
    // bundling androidx.test via hilt-android-testing, consistent resolution can
    // drop the hamcrest org.hamcrest.Matchers the Espresso runner expects.
    androidTestImplementation("org.hamcrest:hamcrest:2.2")

    androidTestImplementation("androidx.test.ext:junit:1.3.0")
    androidTestImplementation("androidx.test:core:1.7.0")
    androidTestImplementation("androidx.test:rules:1.7.0")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.7.0")
    androidTestImplementation("androidx.test.espresso:espresso-accessibility:3.7.0")
    androidTestImplementation("com.google.dagger:hilt-android-testing:2.57.2")
    kaptAndroidTest("com.google.dagger:hilt-android-compiler:2.57.2")
}