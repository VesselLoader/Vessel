// ==============================================================================
// EXAMPLE VESSEL MOD — Settings Gradle (Kotlin DSL)
// Configured to resolve plugins directly from remote repositories.
// ==============================================================================

pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
}

rootProject.name = "example-vessel-mod"
