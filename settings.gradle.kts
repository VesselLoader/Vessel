// ==============================================================================
// VESSEL — Settings Gradle (Kotlin DSL)
// Version: 1.0.2
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

// Local Composite Build Interconnection with Vessel-api repository (DEC-0004 / DEC-0008)
val localPropsFile = file("gradle.local.properties")
val configuredApiPath = if (localPropsFile.exists()) {
    Properties().apply { localPropsFile.inputStream().use(::load) }
        .getProperty("vessel.api.local.path")
} else {
    null
}

val candidateApiDirs = listOfNotNull(
    configuredApiPath?.let { file(it) },
    file("../Vessel-api"),
    file("Vessel-api")
)

val resolvedApiDir = candidateApiDirs.firstOrNull { it.exists() && file("${it.path}/settings.gradle.kts").exists() }
if (resolvedApiDir != null) {
    includeBuild(resolvedApiDir) {
        dependencySubstitution {
            substitute(module("dev.vessel:vessel-api")).using(project(":"))
        }
    }
}
