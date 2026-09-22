plugins {
    id("com.android.application") version "8.13.2" apply false
    id("com.android.library") version "8.13.2" apply false
    id("org.jetbrains.kotlin.android") version "2.2.20" apply false
    id("org.jetbrains.kotlin.kapt") version "2.2.20" apply false
    id("com.google.dagger.hilt.android") version "2.57.2" apply false
    id("io.gitlab.arturbosch.detekt") version "1.23.8" apply false
}

// Reproducible builds: all dependency configurations are locked and the
// generated gradle.lockfile files are committed (SCA / NFR-MAINT).
subprojects {
    dependencyLocking {
        lockAllConfigurations()
    }
}