// ==============================================================================
// VESSEL — Build Gradle (Kotlin DSL)
// Version: 1.0.5
// Description: Core build configuration for VESSEL Microkernel Engine.
// Architecture: Sovereign Standalone Server (Java 21 LTS Toolchain).
// ==============================================================================

import java.io.ByteArrayOutputStream
import java.io.File
import java.util.jar.JarEntry
import java.util.jar.JarFile
import java.util.jar.JarOutputStream
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.commons.ClassRemapper
import org.objectweb.asm.commons.Remapper

buildscript {
    repositories {
        mavenCentral()
    }
    dependencies {
        classpath("org.ow2.asm:asm:9.7")
        classpath("org.ow2.asm:asm-commons:9.7")
    }
}

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

sourceSets {
    main {
        java.srcDir("vstm-distribution/cli/src/main/java")
    }
    test {
        java.srcDir("vstm-distribution/cli/src/test/java")
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
    options.compilerArgs.addAll(listOf("-parameters", "-Xlint:all", "-Xlint:-processing", "--enable-preview"))
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
    jvmArgs("--enable-preview")
    testLogging {
        events("passed", "skipped", "failed")
        showStandardStreams = true
    }
}

// Package Relocation (Shading): org.objectweb.asm -> dev.vessel.shadow.asm
// Produces a single runnable server.jar artifact.
tasks.jar {
    archiveFileName.set("server.jar")
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE

    from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) }) {
        exclude("META-INF/*.SF", "META-INF/*.DSA", "META-INF/*.RSA", "module-info.class", "org/slf4j/**")
    }

    manifest {
        attributes(
            "Main-Class" to "dev.vessel.bootstrap.target.VesselServerLauncher",
            "Premain-Class" to "dev.vessel.bootstrap.target.VesselInstrumentationAgent",
            "Agent-Class" to "dev.vessel.bootstrap.target.VesselInstrumentationAgent",
            "Can-Redefine-Classes" to "true",
            "Can-Retransform-Classes" to "true",
            "Enable-Native-Access" to "ALL-UNNAMED",
            "Specification-Title" to "VESSEL Sovereign Microkernel Core",
            "Specification-Version" to project.version,
            "Specification-Vendor" to "VesselLoader Team",
            "Implementation-Title" to project.name,
            "Implementation-Version" to project.version,
            "Implementation-Vendor" to "VesselLoader Team",
            "Shaded-ASM-Relocation" to "org.objectweb.asm->dev.vessel.shadow.asm"
        )
    }

    val outputJarProvider = archiveFile
    doLast {
        val jarFile: File = outputJarProvider.get().asFile
        val tempRelocated = File(jarFile.parentFile, "server-relocated.tmp")
        val asmPrefix = "org/objectweb/asm/"
        val shadowPrefix = "dev/vessel/shadow/asm/"
        val remapper = object : Remapper() {
            override fun map(internalName: String): String {
                if (internalName.startsWith(asmPrefix)) {
                    return shadowPrefix + internalName.substring(asmPrefix.length)
                }
                return internalName
            }
        }

        JarFile(jarFile).use { sourceJar ->
            JarOutputStream(tempRelocated.outputStream()).use { targetOut ->
                val seenEntries = HashSet<String>()
                val entries = sourceJar.entries()
                while (entries.hasMoreElements()) {
                    val entry = entries.nextElement()
                    if (entry.isDirectory) {
                        continue
                    }
                    val rawName = entry.name
                    val mappedName = if (rawName.startsWith(asmPrefix)) {
                        shadowPrefix + rawName.substring(asmPrefix.length)
                    } else {
                        rawName
                    }
                    if (!seenEntries.add(mappedName)) {
                        continue
                    }
                    val rawBytes = sourceJar.getInputStream(entry).use { it.readBytes() }
                    val finalBytes = if (mappedName.endsWith(".class") && !mappedName.endsWith("module-info.class")) {
                        val cr = ClassReader(rawBytes)
                        val cw = ClassWriter(0)
                        val classRemapper = ClassRemapper(cw, remapper)
                        cr.accept(classRemapper, 0)
                        cw.toByteArray()
                    } else {
                        rawBytes
                    }
                    val newEntry = JarEntry(mappedName)
                    targetOut.putNextEntry(newEntry)
                    targetOut.write(finalBytes)
                    targetOut.closeEntry()
                }
            }
        }
        if (jarFile.exists()) {
            jarFile.delete()
        }
        tempRelocated.renameTo(jarFile)
    }
}

// Standalone CLI Executable Jar (vstm-cli.jar) Packaging
tasks.register<Jar>("vstmCliJar") {
    archiveFileName.set("vstm-cli.jar")
    destinationDirectory.set(file("vstm-distribution/cli/build"))
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE

    from(sourceSets.main.get().output)
    from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) }) {
        exclude("META-INF/*.SF", "META-INF/*.DSA", "META-INF/*.RSA")
    }

    manifest {
        attributes(
            "Main-Class" to "dev.vessel.vstm.cli.VstmCliMain",
            "Specification-Title" to "VESSEL VSTM CLI Standalone Transmutation Tool",
            "Specification-Version" to project.version,
            "Implementation-Title" to "vstm-cli",
            "Implementation-Version" to project.version
        )
    }
}
