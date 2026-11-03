// ==============================================================================
// VESSEL — Settings Gradle (Kotlin DSL)
// Version: 1.0.1
// Description: Defines the root project name, plugin management repositories,
//              and local Gradle Composite Build interconnection with Vessel-api.
// ==============================================================================

import java.util.Properties

pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
        mavenLocal()
    }
}

rootProject.name = "vessel-core"

// Local Composite Build Interconnection with sibling Vessel-api repository (DEC-0004 / DEC-0008)
val localPropsFile = file("gradle.local.properties")
val configuredApiPath = if (localPropsFile.exists()) {
    Properties().apply { localPropsFile.inputStream().use(::load) }
        .getProperty("vessel.api.local.path")
} else {
    null
}
val vesselApiDir = file(configuredApiPath ?: "../Vessel-api")
if (vesselApiDir.exists() && file("${vesselApiDir.path}/settings.gradle.kts").exists()) {
    includeBuild(vesselApiDir) {
        dependencySubstitution {
            substitute(module("dev.vessel:vessel-api")).using(project(":"))
        }
    }
}
