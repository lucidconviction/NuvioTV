pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.10.0"
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven("https://jitpack.io")
    }
}

rootProject.name = "My Application"
include(":app")
include(":baselineprofile")

val useLocalFfmpegDecoder = providers.gradleProperty("useLocalFfmpegDecoder").orNull
    ?: System.getenv("USE_LOCAL_FFMPEG_DECODER")
    ?: java.util.Properties().apply {
        val f = file("local.properties")
        if (f.exists()) load(f.inputStream())
    }.getProperty("USE_LOCAL_FFMPEG_DECODER")

if (useLocalFfmpegDecoder.equals("true", ignoreCase = true)) {
    include(":ffmpeg-decoder-downmix")
}
