package org.bukkit.plugin.java;

import dev.vessel.kernel.config.yaml.MemoryConfigurationSection;
import java.util.Objects;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginDescriptionFile;

/**
 * Canonical base class (`org.bukkit.plugin.java.JavaPlugin`) extended by all Bukkit, Spigot,
 * Paper, and Folia plugins when loaded into the VESSEL Sovereign Microkernel.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public class JavaPlugin implements Plugin {

    private PluginDescriptionFile description;
    private MemoryConfigurationSection config;
    private Server server;
    private Logger logger;
    private boolean enabled;

    /**
     * Default constructor invoked by the plugin classloader when instantiating a plugin main class.
     */
    public JavaPlugin() {
        this.description = new PluginDescriptionFile(
                getClass().getSimpleName(),
                "1.0.0",
                getClass().getName());
        this.config = new MemoryConfigurationSection();
        this.server = Bukkit.getServer();
        this.logger = Logger.getLogger(description.getName());
        this.enabled = false;
    }

    /**
     * Initializes this plugin instance with an explicit {@link PluginDescriptionFile} and
     * {@link Server}.
     *
     * @param description the parsed plugin descriptor
     * @param server      the active Bukkit server facade
     */
    public final void init(PluginDescriptionFile description, Server server) {
        this.description = Objects.requireNonNull(description, "description must not be null");
        this.server = Objects.requireNonNull(server, "server must not be null");
        this.logger = Logger.getLogger(description.getName());
    }

    @Override
    public String getName() {
        return description.getName();
    }

    @Override
    public PluginDescriptionFile getDescription() {
        return description;
    }

    @Override
    public MemoryConfigurationSection getConfig() {
        if (config == null) {
            config = new MemoryConfigurationSection();
        }
        return config;
    }

    @Override
    public void saveDefaultConfig() {
        getConfig().set("plugin.initialized", true);
    }

    @Override
    public void reloadConfig() {
        if (config == null) {
            config = new MemoryConfigurationSection();
        }
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Transitions the plugin's enabled state, automatically invoking {@link #onEnable()} or
     * {@link #onDisable()} when the state changes.
     *
     * @param enabled {@code true} to enable the plugin, {@code false} to disable it
     */
    public final void setEnabled(boolean enabled) {
        if (this.enabled != enabled) {
            this.enabled = enabled;
            if (this.enabled) {
                onEnable();
            } else {
                onDisable();
            }
        }
    }

    @Override
    public void onLoad() {
    }

    @Override
    public void onEnable() {
    }

    @Override
    public void onDisable() {
    }

    @Override
    public Server getServer() {
        return server != null ? server : Bukkit.getServer();
    }

    @Override
    public Logger getLogger() {
        return logger;
    }
}
