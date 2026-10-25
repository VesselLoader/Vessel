package org.bukkit;

import java.util.Objects;
import java.util.logging.Logger;
import org.bukkit.plugin.PluginManager;

/**
 * Static singleton facade (`org.bukkit.Bukkit`) providing global access to the active VESSEL
 * {@link Server} and {@link PluginManager} for Bukkit, Spigot, Paper, and Folia plugins.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public final class Bukkit {

    private static volatile Server server = new Server();

    private Bukkit() {
    }

    /**
     * Binds the active {@link Server} singleton instance.
     *
     * @param activeServer the server instance to bind
     */
    public static void setServer(Server activeServer) {
        server = Objects.requireNonNull(activeServer, "activeServer must not be null");
    }

    /**
     * Returns the active {@link Server} singleton instance.
     *
     * @return the active server instance
     */
    public static Server getServer() {
        return server;
    }

    public static String getName() {
        return server.getName();
    }

    public static String getVersion() {
        return server.getVersion();
    }

    public static String getBukkitVersion() {
        return server.getBukkitVersion();
    }

    public static PluginManager getPluginManager() {
        return server.getPluginManager();
    }

    public static Logger getLogger() {
        return server.getLogger();
    }
}
