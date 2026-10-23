package dev.vessel.bootstrap.mojang;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Unpacks official Mojang Server Bundler JARs ({@code META-INF/versions.list},
 * {@code META-INF/libraries.list}, and {@code META-INF/main-class}) introduced in Minecraft 1.18+
 * into a local cache directory and returns the complete list of extracted server and library JARs
 * for Tier-1 Game Layer classloading.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public final class MojangBundlerExtractor {

    public static final String DEFAULT_VANILLA_MAIN_CLASS = "net.minecraft.server.Main";

    /**
     * Extracts all bundled version JARs and library JARs listed in {@code META-INF/versions.list}
     * and {@code META-INF/libraries.list} from {@code bundlerJarPath} into
     * {@code extractionRootDir}. If the JAR is a non-bundled standalone server JAR, returns
     * {@code List.of(bundlerJarPath)} directly.
     *
     * @param bundlerJarPath    the cached Mojang server bundler JAR path
     * @param extractionRootDir the directory where bundled JARs should be unpacked
     * @return the ordered list of extracted classpath JARs (server JAR followed by libraries)
     */
    public List<Path> extractBundlerJar(Path bundlerJarPath, Path extractionRootDir) {
        Objects.requireNonNull(bundlerJarPath, "bundlerJarPath must not be null");
        Objects.requireNonNull(extractionRootDir, "extractionRootDir must not be null");
        if (!Files.exists(bundlerJarPath)) {
            return List.of();
        }

        List<Path> extractedJars = new ArrayList<>();
        try (JarFile jarFile = new JarFile(bundlerJarPath.toFile())) {
            JarEntry versionsListEntry = jarFile.getJarEntry("META-INF/versions.list");
            JarEntry librariesListEntry = jarFile.getJarEntry("META-INF/libraries.list");

            if (versionsListEntry == null && librariesListEntry == null) {
                extractedJars.add(bundlerJarPath);
                return List.copyOf(extractedJars);
            }

            if (versionsListEntry != null) {
                extractListedEntries(
                        jarFile,
                        versionsListEntry,
                        "META-INF/versions/",
                        extractionRootDir.resolve("versions"),
                        extractedJars);
            }
            if (librariesListEntry != null) {
                extractListedEntries(
                        jarFile,
                        librariesListEntry,
                        "META-INF/libraries/",
                        extractionRootDir.resolve("libraries"),
                        extractedJars);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to extract Mojang bundler JAR: " + bundlerJarPath, e);
        }
        return List.copyOf(extractedJars);
    }

    /**
     * Reads the target internal main class declared in {@code META-INF/main-class} inside a Mojang
     * bundler JAR, or defaults to {@code net.minecraft.server.Main}.
     *
     * @param bundlerJarPath the Mojang bundler JAR path
     * @return the fully qualified main class name
     */
    public String readBundlerMainClass(Path bundlerJarPath) {
        Objects.requireNonNull(bundlerJarPath, "bundlerJarPath must not be null");
        if (!Files.exists(bundlerJarPath)) {
            return DEFAULT_VANILLA_MAIN_CLASS;
        }
        try (JarFile jarFile = new JarFile(bundlerJarPath.toFile())) {
            JarEntry mainClassEntry = jarFile.getJarEntry("META-INF/main-class");
            if (mainClassEntry != null) {
                try (InputStream in = jarFile.getInputStream(mainClassEntry)) {
                    String mainClass = new String(in.readAllBytes(), StandardCharsets.UTF_8).trim();
                    if (!mainClass.isEmpty()) {
                        return mainClass;
                    }
                }
            }
        } catch (IOException ignored) {
            // Fall back to default main class
        }
        return DEFAULT_VANILLA_MAIN_CLASS;
    }

    private static void extractListedEntries(
            JarFile jarFile,
            JarEntry listEntry,
            String jarEntryPrefix,
            Path targetBaseDir,
            List<Path> outExtracted) throws IOException {
        String listText;
        try (InputStream in = jarFile.getInputStream(listEntry)) {
            listText = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
        String[] lines = listText.split("\\R");
        for (String rawLine : lines) {
            String line = rawLine.trim();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            String[] columns = line.split("\\s+");
            String relativeArchivePath = columns[columns.length - 1].trim();
            if (relativeArchivePath.isEmpty()) {
                continue;
            }
            JarEntry bundledJarEntry = jarFile.getJarEntry(jarEntryPrefix + relativeArchivePath);
            if (bundledJarEntry == null) {
                continue;
            }
            Path destination = targetBaseDir.resolve(relativeArchivePath);
            Files.createDirectories(destination.getParent());
            try (InputStream bundledIn = jarFile.getInputStream(bundledJarEntry)) {
                Files.copy(bundledIn, destination, StandardCopyOption.REPLACE_EXISTING);
            }
            outExtracted.add(destination);
        }
    }
}
