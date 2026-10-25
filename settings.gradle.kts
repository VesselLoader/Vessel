// ==============================================================================
// VESSEL — Settings Gradle (Kotlin DSL)
// Version: 1.0.0
// Description: Defines the root project name and plugin management repositories.
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

rootProject.name = "vessel-core"
