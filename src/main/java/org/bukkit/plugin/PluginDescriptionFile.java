package org.bukkit.plugin;

import java.util.Objects;

/**
 * Represents the parsed metadata of a Bukkit/Spigot/Paper plugin's {@code plugin.yml} or
 * {@code paper-plugin.yml} descriptor at runtime.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public final class PluginDescriptionFile {

    private final String name;
    private final String version;
    private final String mainClass;
    private final String apiVersion;

    /**
     * Constructs a plugin description with name, version, and main class.
     *
     * @param name      the plugin name
     * @param version   the plugin version string
     * @param mainClass the fully qualified main class name
     */
    public PluginDescriptionFile(String name, String version, String mainClass) {
        this(name, version, mainClass, "1.21");
    }

    /**
     * Constructs a plugin description with name, version, main class, and API version.
     *
     * @param name       the plugin name
     * @param version    the plugin version string
     * @param mainClass  the fully qualified main class name
     * @param apiVersion the declared Bukkit API version
     */
    public PluginDescriptionFile(
            String name,
            String version,
            String mainClass,
            String apiVersion) {
        this.name = Objects.requireNonNull(name, "name must not be null").trim();
        this.version = Objects.requireNonNull(version, "version must not be null").trim();
        this.mainClass = Objects.requireNonNull(mainClass, "mainClass must not be null").trim();
        this.apiVersion = Objects.requireNonNull(apiVersion, "apiVersion must not be null").trim();
    }

    public String getName() {
        return name;
    }

    public String getVersion() {
        return version;
    }

    public String getMain() {
        return mainClass;
    }

    public String getApiVersion() {
        return apiVersion;
    }

    public String getFullName() {
        return name + " v" + version;
    }
}
