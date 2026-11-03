package org.bukkit.configuration.file;

import dev.vessel.kernel.config.yaml.MemoryConfigurationSection;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import org.bukkit.configuration.ConfigurationSection;

/**
 * Base class for file-backed Bukkit/Spigot/Paper configurations ({@code FileConfiguration}),
 * extending {@link MemoryConfigurationSection} and implementing {@link ConfigurationSection}.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public abstract class FileConfiguration extends MemoryConfigurationSection
        implements ConfigurationSection {

    /**
     * Constructs an empty root {@code FileConfiguration}.
     */
    protected FileConfiguration() {
        super();
    }

    /**
     * Serializes this configuration into its text representation.
     *
     * @return serialized configuration string
     */
    public abstract String saveToString();

    /**
     * Populates this configuration from the given text representation.
     *
     * @param contents raw serialized configuration contents
     */
    public abstract void loadFromString(String contents);

    /**
     * Sets a default value at {@code path} if no value is currently set at that path.
     *
     * @param path  dot-separated configuration path
     * @param value default value to assign if unset
     */
    public void addDefault(String path, Object value) {
        Objects.requireNonNull(path, "path must not be null");
        if (!isSet(path)) {
            set(path, value);
        }
    }

    /**
     * Saves this configuration to the specified {@link File} encoded as UTF-8.
     *
     * @param file destination file
     * @throws IOException if an I/O error occurs while writing
     */
    public void save(File file) throws IOException {
        Objects.requireNonNull(file, "file must not be null");
        Path targetPath = file.toPath();
        Path parent = targetPath.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Files.writeString(targetPath, saveToString(), StandardCharsets.UTF_8);
    }

    /**
     * Loads this configuration from the specified {@link File} encoded as UTF-8.
     *
     * @param file source file
     * @throws IOException if an I/O error occurs while reading
     */
    public void load(File file) throws IOException {
        Objects.requireNonNull(file, "file must not be null");
        String contents = Files.readString(file.toPath(), StandardCharsets.UTF_8);
        loadFromString(contents);
    }
}
