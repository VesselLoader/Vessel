package org.bukkit;

import java.util.Objects;
import java.util.logging.Logger;
import org.bukkit.plugin.PluginManager;

/**
 * Runtime representation of the VESSEL Minecraft Server for the Bukkit, Spigot, Paper, and Folia
 * plugin ecosystems.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public class Server {

    private final String name;
    private final String version;
    private final String bukkitVersion;
    private final PluginManager pluginManager;
    private final Logger logger;

    /**
     * Constructs a default VESSEL Bukkit Server instance targeting the latest Minecraft release.
     */
    public Server() {
        this("VESSEL-Sovereign", "1.21.4-R0.1-SNAPSHOT", "1.21.4-R0.1-SNAPSHOT");
    }

    /**
     * Constructs a VESSEL Bukkit Server instance with explicit brand and version identifiers.
     *
     * @param name          the server brand name
     * @param version       the full server version string
     * @param bukkitVersion the Bukkit API version string
     */
    public Server(String name, String version, String bukkitVersion) {
        this.name = Objects.requireNonNull(name, "name must not be null");
        this.version = Objects.requireNonNull(version, "version must not be null");
        this.bukkitVersion = Objects.requireNonNull(bukkitVersion, "bukkitVersion must not be null");
        this.pluginManager = new PluginManager();
        this.logger = Logger.getLogger("Minecraft.VESSEL");
    }

    public String getName() {
        return name;
    }

    public String getVersion() {
        return version;
    }

    public String getBukkitVersion() {
        return bukkitVersion;
    }

    public PluginManager getPluginManager() {
        return pluginManager;
    }

    public Logger getLogger() {
        return logger;
    }
}
