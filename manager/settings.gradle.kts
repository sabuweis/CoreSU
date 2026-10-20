@file:Suppress("UnstableApiUsage")

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        maven("https://maven.lsposed.org/releases")
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        maven("https://maven.lsposed.org/releases")
        mavenCentral()
        maven("https://jitpack.io")
    }
}

rootProject.name = "CoreSU"
include(":app")
