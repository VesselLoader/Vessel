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
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

/**
 * Dynamically handles both modern Mojang Server Bundler JARs ({@code META-INF/versions.list},
 * {@code META-INF/libraries.list}, and {@code META-INF/main-class} introduced in Minecraft 1.18+)
 * and legacy monolithic fat server JARs (Minecraft 1.2.5 through 1.17.1, such as 1.16.5, 1.12.2,
 * and 1.8.8), returning the complete list of classpath JARs and dynamically resolving the server
 * entrypoint class (`net.minecraft.server.Main` or `net.minecraft.server.MinecraftServer`).
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.1
 */
public final class MojangBundlerExtractor {

    public static final String DEFAULT_VANILLA_MAIN_CLASS = "net.minecraft.server.Main";
    public static final String LEGACY_MINECRAFT_SERVER_CLASS = "net.minecraft.server.MinecraftServer";
    public static final String BUNDLER_WRAPPER_MAIN_CLASS = "net.minecraft.bundler.Main";

    /**
     * Checks whether the given server JAR uses the Minecraft 1.18+ bundler layout
     * ({@code META-INF/versions.list} or {@code META-INF/libraries.list}) as opposed to a
     * pre-1.18 monolithic server JAR (such as Minecraft 1.16.5 or 1.12.2).
     *
     * @param jarPath path to the downloaded Mojang server JAR
     * @return {@code true} if the archive is a 1.18+ bundler JAR; {@code false} if monolithic
     */
    public boolean isBundlerArchive(Path jarPath) {
        if (jarPath == null || !Files.isRegularFile(jarPath)) {
            return false;
        }
        try (JarFile jarFile = new JarFile(jarPath.toFile())) {
            return jarFile.getJarEntry("META-INF/versions.list") != null
                    || jarFile.getJarEntry("META-INF/libraries.list") != null;
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * Extracts all bundled version JARs and library JARs listed in {@code META-INF/versions.list}
     * and {@code META-INF/libraries.list} from {@code bundlerJarPath} into
     * {@code extractionRootDir}. If the JAR is a non-bundled monolithic server JAR (e.g., 1.16.5 or
     * 1.12.2), returns {@code List.of(bundlerJarPath)} directly.
     *
     * @param bundlerJarPath    the cached Mojang server JAR path
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
     * Dynamically resolves the internal server entrypoint class for any Minecraft era:
     * <ol>
     *   <li>{@code META-INF/main-class} inside 1.18+ bundler JARs.</li>
     *   <li>{@code Main-Class} inside {@code META-INF/MANIFEST.MF} for pre-1.18 monolithic JARs
     *       (such as {@code net.minecraft.server.Main} in 1.16.5 or
     *       {@code net.minecraft.server.MinecraftServer} in 1.12.2 / 1.8.8).</li>
     *   <li>Direct class entry probe inside the JAR ({@code net/minecraft/server/Main.class} or
     *       {@code net/minecraft/server/MinecraftServer.class}).</li>
     * </ol>
     *
     * @param bundlerJarPath the Mojang bundler or monolithic server JAR path
     * @return the resolved fully qualified server main class name
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
            Manifest manifest = jarFile.getManifest();
            if (manifest != null) {
                String manifestMain = manifest.getMainAttributes().getValue(Attributes.Name.MAIN_CLASS);
                if (manifestMain != null
                        && !manifestMain.isBlank()
                        && !BUNDLER_WRAPPER_MAIN_CLASS.equals(manifestMain.trim())) {
                    return manifestMain.trim();
                }
            }
            if (jarFile.getJarEntry("net/minecraft/server/Main.class") != null) {
                return DEFAULT_VANILLA_MAIN_CLASS;
            }
            if (jarFile.getJarEntry("net/minecraft/server/MinecraftServer.class") != null) {
                return LEGACY_MINECRAFT_SERVER_CLASS;
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
