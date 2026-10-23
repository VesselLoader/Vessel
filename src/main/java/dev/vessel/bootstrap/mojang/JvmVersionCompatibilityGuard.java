package dev.vessel.bootstrap.mojang;

import dev.vessel.bootstrap.target.VesselStartupConsoleLogger;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Verifies that the host Java Virtual Machine satisfies both VESSEL's own minimum Java requirement
 * ({@code Java 21 LTS}, class file version {@code 65.0}) and the target Minecraft Dedicated Server's
 * compiled bytecode requirement (for example, Minecraft {@code 26.3} requires {@code Java 25},
 * class file version {@code 69.0}, whereas Minecraft {@code 1.21.4}–{@code 1.21.11} requires
 * {@code Java 21}, class file version {@code 65.0}).
 *
 * <p>When a version mismatch is detected, this guard halts server handoff before a raw
 * {@link UnsupportedClassVersionError} stack trace is thrown and prints a clear, actionable
 * diagnostic banner instructing the user which Java version to install.</p>
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.2
 */
public final class JvmVersionCompatibilityGuard {

    /**
     * Minimum Java feature version required by the VESSEL Microkernel itself (Java 21 LTS).
     */
    public static final int VESSEL_MINIMUM_JAVA_VERSION = 21;

    /**
     * Minimum JVM class file major version required by VESSEL (65 = Java 21).
     */
    public static final int VESSEL_MINIMUM_CLASS_FILE_MAJOR = 65;

    /**
     * Detects the feature version of the currently running Java runtime (e.g., {@code 21} or {@code 25}).
     *
     * @return current JVM feature version
     */
    public int detectRunningJavaFeatureVersion() {
        return Runtime.version().feature();
    }

    /**
     * Converts a JVM class file major version (e.g., {@code 65} or {@code 69}) to the corresponding
     * Java feature version ({@code classFileMajor - 44}).
     *
     * @param classFileMajor class file major version from bytes {@code [6..7]}
     * @return Java feature version (e.g., {@code 21} for {@code 65}, {@code 25} for {@code 69})
     */
    public int classFileMajorToJavaFeatureVersion(int classFileMajor) {
        if (classFileMajor <= 52) {
            return 8;
        }
        return classFileMajor - 44;
    }

    /**
     * Converts a Java feature version (e.g., {@code 21} or {@code 25}) to its JVM class file major
     * version ({@code javaFeatureVersion + 44}).
     *
     * @param javaFeatureVersion Java feature version
     * @return JVM class file major version
     */
    public int javaFeatureToClassFileMajorVersion(int javaFeatureVersion) {
        return javaFeatureVersion + 44;
    }

    /**
     * Inspects the extracted classpath JARs (specifically the unpacked {@code server-<version>.jar}
     * or the JAR containing {@code mainClassName}) and reads bytes {@code [6..7]} of the entrypoint
     * {@code .class} file to determine its compiled class file major version.
     *
     * @param classpathJars extracted Mojang bundler JARs
     * @param mainClassName entrypoint class name (e.g., {@code net.minecraft.server.Main})
     * @return detected class file major version (e.g., {@code 69} for Java 25, {@code 65} for Java 21),
     *         or {@link #VESSEL_MINIMUM_CLASS_FILE_MAJOR} if no entrypoint class is present
     */
    public int detectRequiredClassFileMajorVersion(List<Path> classpathJars, String mainClassName) {
        Objects.requireNonNull(classpathJars, "classpathJars must not be null");
        Objects.requireNonNull(mainClassName, "mainClassName must not be null");

        String entryPath = mainClassName.replace('.', '/') + ".class";
        for (Path jarPath : classpathJars) {
            if (jarPath == null || !Files.isRegularFile(jarPath)) {
                continue;
            }
            int major = readClassEntryMajorVersion(jarPath, entryPath);
            if (major > 0) {
                return major;
            }
        }
        return VESSEL_MINIMUM_CLASS_FILE_MAJOR;
    }

    /**
     * Reads the 8-byte JVM class header (`0xCAFEBABE`, minor, major) of a specific class entry inside
     * a JAR file.
     *
     * @param jarPath   path to the JAR file on disk
     * @param entryPath internal `.class` entry path
     * @return class file major version, or {@code -1} if the entry does not exist in {@code jarPath}
     */
    public int readClassEntryMajorVersion(Path jarPath, String entryPath) {
        if (jarPath == null || !Files.isRegularFile(jarPath)) {
            return -1;
        }
        try (ZipFile zipFile = new ZipFile(jarPath.toFile())) {
            ZipEntry entry = zipFile.getEntry(entryPath);
            if (entry == null) {
                return -1;
            }
            try (InputStream in = zipFile.getInputStream(entry)) {
                byte[] header = in.readNBytes(8);
                if (header.length == 8
                        && (header[0] & 0xFF) == 0xCA
                        && (header[1] & 0xFF) == 0xFE
                        && (header[2] & 0xFF) == 0xBA
                        && (header[3] & 0xFF) == 0xBE) {
                    return ((header[6] & 0xFF) << 8) | (header[7] & 0xFF);
                }
            }
        } catch (Exception ignored) {
            // Ignore unreadable archive during pre-flight inspection
        }
        return -1;
    }

    /**
     * Checks whether the current Java runtime version satisfies both VESSEL's minimum requirement
     * ({@code Java 21 LTS}) and the target Minecraft server JAR's required Java version. If the host
     * JVM is older than required, logs a clear, human-readable error box to {@code logger} and
     * returns {@code false}.
     *
     * @param runningJavaVersion     detected host Java feature version (e.g., {@code 21})
     * @param serverClassFileMajor   compiled class file major version of {@code net.minecraft.server.Main}
     * @param targetMinecraftVersion resolved Minecraft version string (e.g., {@code 26.3})
     * @param mainClassName          server main class name
     * @param logger                 console logger to receive the actionable error report
     * @return {@code true} if the running JVM is compatible; {@code false} if launch must be halted
     */
    public boolean verifyJvmCompatibilityOrLogError(
            int runningJavaVersion,
            int serverClassFileMajor,
            String targetMinecraftVersion,
            String mainClassName,
            VesselStartupConsoleLogger logger) {
        Objects.requireNonNull(targetMinecraftVersion, "targetMinecraftVersion must not be null");
        Objects.requireNonNull(mainClassName, "mainClassName must not be null");
        Objects.requireNonNull(logger, "logger must not be null");

        int runningClassFileMajor = javaFeatureToClassFileMajorVersion(runningJavaVersion);
        int serverRequiredJava = classFileMajorToJavaFeatureVersion(serverClassFileMajor);
        int effectiveRequiredJava = Math.max(VESSEL_MINIMUM_JAVA_VERSION, serverRequiredJava);
        int effectiveRequiredClassMajor = Math.max(VESSEL_MINIMUM_CLASS_FILE_MAJOR, serverClassFileMajor);

        if (runningJavaVersion >= effectiveRequiredJava) {
            return true;
        }

        logger.logError("========================================================================");
        logger.logError(" JAVA RUNTIME VERSION MISMATCH - SERVER LAUNCH HALTED");
        logger.logError("========================================================================");
        logger.logError(" Entrypoint Class : " + mainClassName + " (Minecraft " + targetMinecraftVersion + ")");
        logger.logError(" Current Java VM  : Java " + runningJavaVersion
                + " (recognizes class file versions up to " + runningClassFileMajor + ".0)");
        logger.logError(" VESSEL Minimum   : Java " + VESSEL_MINIMUM_JAVA_VERSION
                + " LTS (class file version " + VESSEL_MINIMUM_CLASS_FILE_MAJOR + ".0)");
        logger.logError(" Server Requires  : Java " + effectiveRequiredJava
                + " or newer (compiled with class file version " + effectiveRequiredClassMajor + ".0)");
        logger.logError("------------------------------------------------------------------------");
        logger.logError(" HOW TO FIX:");
        logger.logError("   1. Install and launch with Java " + effectiveRequiredJava + " or newer to run Minecraft "
                + targetMinecraftVersion + " (VESSEL requires at least Java "
                + VESSEL_MINIMUM_JAVA_VERSION + " LTS), OR");
        logger.logError("   2. To run on your current Java " + runningJavaVersion
                + " runtime, pin a Java " + runningJavaVersion + "-compatible Minecraft version:");
        logger.logError("      java -jar server.jar --vessel-version=1.21.11 --nogui");
        logger.logError("========================================================================");
        return false;
    }
}
