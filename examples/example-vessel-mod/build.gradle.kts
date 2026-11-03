// ==============================================================================
// EXAMPLE VESSEL MOD — Build Gradle (Kotlin DSL)
// Resolves 100% directly from remote Maven Central.
// ==============================================================================

plugins {
    java
    id("io.github.vesselloader.vessel-gradle") version "1.0.0"
}

group = "com.example.mod"
version = "1.0.0"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

repositories {
    mavenCentral()
}

dependencies {
    // 1. Declare Minecraft vanilla version
    minecraft("1.21.1")

    // 2. Resolve VSTM binary transmutation matrix from Maven Central
    mappings(vessel.vstm("1.21.1"))

    // 3. Clean-Room API
    compileOnly("io.github.vesselloader:vstm-api:1.0.0")
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
    options.release.set(21)
}
