package org.bukkit.plugin;

import dev.vessel.kernel.config.yaml.MemoryConfigurationSection;
import java.util.logging.Logger;
import org.bukkit.Server;

/**
 * Runtime contract for a Bukkit, Spigot, Paper, or Folia plugin hosted on the VESSEL Sovereign
 * Microkernel.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public interface Plugin {

    String getName();

    PluginDescriptionFile getDescription();

    MemoryConfigurationSection getConfig();

    void saveDefaultConfig();

    void reloadConfig();

    boolean isEnabled();

    void onLoad();

    void onEnable();

    void onDisable();

    Server getServer();

    Logger getLogger();
}
