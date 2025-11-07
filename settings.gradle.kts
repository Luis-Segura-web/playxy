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
        maven { url = uri("https://jitpack.io") }
        // VideoLAN Maven repository for VLC
        maven { url = uri("https://download.videolan.org/pub/videolan/vlc-android/maven") }
    }
}

rootProject.name = "playxy"
include(":app")
 