enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    repositories {
        mavenLocal()
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
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenLocal()
        google()
        mavenCentral()
        maven("https://jitpack.io")
    }
}

rootProject.name = "RecordingStudio"
include(":di")
include(":entities")
include(":file_import")
include(":recorder:domain")
include(":audio_config:domain:api")
include(":audio_config:domain:impl")
include(":app")
include(":audio_config:data")
include(":audio_config:presentation")
include(":common:ui")
include(":audio_config:ui")
