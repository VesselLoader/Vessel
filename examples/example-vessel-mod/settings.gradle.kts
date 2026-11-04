// ==============================================================================
// EXAMPLE VESSEL MOD — Settings Gradle (Kotlin DSL)
// Configured to resolve plugins directly from remote repositories.
// ==============================================================================

pluginManagement {
    repositories {
        mavenLocal()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        mavenLocal()
        mavenCentral()
    }
}

rootProject.name = "example-vessel-mod"
