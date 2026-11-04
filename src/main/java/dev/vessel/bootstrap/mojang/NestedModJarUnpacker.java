package dev.vessel.bootstrap.mojang;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Objects;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Automatically unpacks nested Jar-in-Jar (JiJ) dependencies (such as Forge/NeoForge
 * {@code META-INF/jarjar/*.jar} and Fabric/Quilt {@code META-INF/jars/*.jar}) from mod archives
 * into a persistent cache directory so that secondary libraries (such as Catnip, Flywheel,
 * Registrate, and Ponder) are accessible to the multi-era classloader.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public final class NestedModJarUnpacker {

    private static final String FORGE_JARJAR_PREFIX = "META-INF/jarjar/";
    private static final String FABRIC_JARS_PREFIX = "META-INF/jars/";
    private static final String JAR_EXTENSION = ".jar";

    public NestedModJarUnpacker() {
    }

    /**
     * Scans the provided mod and plugin JAR paths, extracts all embedded nested JAR archives into
     * the target cache directory, and returns an expanded list containing both original and unpacked JAR paths.
     *
     * @param rootJarPaths   list of root mod/plugin JAR paths
     * @param cacheDirectory target directory where extracted nested JARs will be stored
     * @return an expanded, ordered list of all JAR paths for classloader consumption
     */
    public List<Path> unpackNestedJars(List<Path> rootJarPaths, Path cacheDirectory) {
        Objects.requireNonNull(rootJarPaths, "rootJarPaths must not be null");
        Objects.requireNonNull(cacheDirectory, "cacheDirectory must not be null");

        List<Path> allJarPaths = new ArrayList<>(rootJarPaths);
        try {
            Files.createDirectories(cacheDirectory);
        } catch (IOException e) {
            System.err.println("[VESSEL] Warning: Failed to create nested jars cache directory: " + e.getMessage());
            return allJarPaths;
        }

        for (Path rootJar : rootJarPaths) {
            if (rootJar == null || !Files.isRegularFile(rootJar)) {
                continue;
            }
            try (JarFile jarFile = new JarFile(rootJar.toFile())) {
                Enumeration<JarEntry> entries = jarFile.entries();
                while (entries.hasMoreElements()) {
                    JarEntry entry = entries.nextElement();
                    if (entry.isDirectory()) {
                        continue;
                    }
                    String entryName = entry.getName().replace('\\', '/');
                    if (isNestedJarEntry(entryName)) {
                        Path extractedPath = extractEntryIfAbsent(jarFile, entry, cacheDirectory, rootJar);
                        if (extractedPath != null && !allJarPaths.contains(extractedPath)) {
                            allJarPaths.add(extractedPath);
                            System.out.println("[VESSEL] Discovered and mounted nested mod JAR: " + extractedPath.getFileName());
                        }
                    }
                }
            } catch (Exception e) {
                System.err.println("[VESSEL] Warning: Failed to inspect nested JARs in " + rootJar.getFileName() + ": " + e.getMessage());
            }
        }

        return List.copyOf(allJarPaths);
    }

    private static boolean isNestedJarEntry(String entryName) {
        if (!entryName.endsWith(JAR_EXTENSION)) {
            return false;
        }
        return entryName.startsWith(FORGE_JARJAR_PREFIX) || entryName.startsWith(FABRIC_JARS_PREFIX);
    }

    private static Path extractEntryIfAbsent(JarFile parentJar, JarEntry entry, Path cacheDirectory, Path rootJar) {
        String parentBaseName = getSimpleFileName(rootJar);
        Path subDir = cacheDirectory.resolve(parentBaseName);
        try {
            Files.createDirectories(subDir);
            String entryFileName = Path.of(entry.getName()).getFileName().toString();
            Path targetFile = subDir.resolve(entryFileName);

            long expectedSize = entry.getSize();
            if (Files.exists(targetFile) && expectedSize > 0 && Files.size(targetFile) == expectedSize) {
                return targetFile;
            }

            try (InputStream in = parentJar.getInputStream(entry);
                 OutputStream out = Files.newOutputStream(targetFile, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
                in.transferTo(out);
            }
            return targetFile;
        } catch (IOException e) {
            System.err.println("[VESSEL] Warning: Failed to extract nested JAR entry " + entry.getName() + ": " + e.getMessage());
            return null;
        }
    }

    private static String getSimpleFileName(Path path) {
        String name = path.getFileName().toString();
        int dot = name.lastIndexOf('.');
        return dot > 0 ? name.substring(0, dot) : name;
    }
}
