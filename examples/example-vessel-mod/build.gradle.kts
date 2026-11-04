// ==============================================================================
// EXAMPLE VESSEL MOD — Build Gradle (Kotlin DSL)
// Demonstrates pure VESSEL Universal Modding Super-API (VUSA) 1.1.0
// Runs natively across Fabric, Forge, NeoForge, Quilt, Paper, Spigot, Folia.
// ==============================================================================

plugins {
    java
}

group = "com.example.mod"
version = "1.0.0"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
    // Clean-Room Universal Modding Super-API (VUSA)
    compileOnly("dev.vessel:vessel-api:1.1.0")
    testImplementation("dev.vessel:vessel-api:1.1.0")
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
    options.release.set(21)
    options.compilerArgs.addAll(listOf("-parameters"))
}
