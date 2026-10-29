// Build logic for the website, as an included build (see includeBuild in ../settings.gradle.kts).

pluginManagement {
    repositories {
        mavenCentral() // Kotlin's own artifacts
        gradlePluginPortal() // kotlin-dsl
    }
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
}

rootProject.name = "build-logic"
