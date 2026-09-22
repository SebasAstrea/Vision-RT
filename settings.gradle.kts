pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "VisionRT"

include(":app")
include(":core")
include(":feature")
include(":perception")
include(":inference")
include(":feedback")
include(":data")
include(":benchmark")