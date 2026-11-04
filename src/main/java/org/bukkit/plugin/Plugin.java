package org.bukkit.plugin;

import java.io.File;
import java.io.InputStream;
import java.util.logging.Logger;
import org.bukkit.Server;
import org.bukkit.configuration.file.FileConfiguration;

/**
 * Runtime contract for a Bukkit, Spigot, Paper, or Folia plugin hosted on the VESSEL Sovereign
 * Microkernel.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.1
 */
public interface Plugin {

    String getName();

    PluginDescriptionFile getDescription();

    FileConfiguration getConfig();

    File getDataFolder();

    void saveConfig();

    void saveDefaultConfig();

    void reloadConfig();

    InputStream getResource(String filename);

    boolean isEnabled();

    void onLoad();

    void onEnable();

    void onDisable();

    Server getServer();

    Logger getLogger();
}
