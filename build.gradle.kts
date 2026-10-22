// ==============================================================================
// VESSEL — Build Gradle (Kotlin DSL)
// Version: 1.0.0
// Description: Core build configuration for VESSEL Microkernel Engine.
// Architecture: Sovereign Standalone Server (Java 21 LTS Toolchain).
// ==============================================================================

plugins {
    `java-library`
}

group = "dev.vessel"
version = "1.0.0-SNAPSHOT"
description = "Virtualized Ecosystem Shared Server & Engine Loader — Sovereign Microkernel Core"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

repositories {
    mavenCentral()
}

dependencies {
    // ASM 9.7 — Bytecode Manipulation & ASM Tree for VesselModuleIR
    implementation("org.ow2.asm:asm:9.7")
    implementation("org.ow2.asm:asm-tree:9.7")
    implementation("org.ow2.asm:asm-commons:9.7")

    // Logging Abstraction (SLF4J)
    implementation("org.slf4j:slf4j-api:2.0.13")

    // Testing Framework (JUnit 5 + AssertJ)
    testImplementation(platform("org.junit:junit-bom:5.10.2"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.assertj:assertj-core:3.26.0")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release.set(21)
    options.compilerArgs.addAll(listOf("-parameters", "-Xlint:all", "-Xlint:-processing"))
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
        showStandardStreams = true
    }
}

tasks.jar {
    manifest {
        attributes(
            "Specification-Title" to "VESSEL Sovereign Microkernel Core",
            "Specification-Version" to project.version,
            "Specification-Vendor" to "VesselLoader Team",
            "Implementation-Title" to project.name,
            "Implementation-Version" to project.version,
            "Implementation-Vendor" to "VesselLoader Team"
        )
    }
}
