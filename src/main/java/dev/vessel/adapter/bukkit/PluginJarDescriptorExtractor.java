package dev.vessel.adapter.bukkit;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Extracts and parses command descriptors from a Bukkit plugin's physical JAR file
 * (checking {@code plugin.yml} and {@code paper-plugin.yml}).
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public final class PluginJarDescriptorExtractor {

    private final PluginYmlCommandParser parser = new PluginYmlCommandParser();

    /**
     * Extracts all declared command descriptors from the given plugin JAR path.
     *
     * @param jarPath physical path to the plugin JAR file
     * @return list of parsed command descriptors, or empty list if none found or on read error
     */
    public List<PluginYmlCommandDescriptor> extractCommandsFromJar(Path jarPath) {
        Objects.requireNonNull(jarPath, "jarPath must not be null");
        if (!Files.isRegularFile(jarPath)) {
            return List.of();
        }

        try (JarFile jarFile = new JarFile(jarPath.toFile())) {
            JarEntry entry = jarFile.getJarEntry("plugin.yml");
            if (entry == null) {
                entry = jarFile.getJarEntry("paper-plugin.yml");
            }
            if (entry != null) {
                try (InputStream in = jarFile.getInputStream(entry)) {
                    return parser.parseCommands(in);
                }
            }
        } catch (Exception ignored) {
        }
        return List.of();
    }
}
