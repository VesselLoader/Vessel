package org.bukkit.plugin.java;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginDescriptionFile;

/**
 * Canonical base class (`org.bukkit.plugin.java.JavaPlugin`) extended by all Bukkit, Spigot,
 * Paper, and Folia plugins when loaded into the VESSEL Sovereign Microkernel.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.4
 */
public class JavaPlugin implements Plugin, CommandExecutor, TabCompleter {

    private PluginDescriptionFile description;
    private FileConfiguration config;
    private Server server;
    private Logger logger;
    private boolean enabled;
    private final Map<String, PluginCommand> commands = new ConcurrentHashMap<>();

    /**
     * Default constructor invoked by the plugin classloader when instantiating a plugin main class.
     */
    public JavaPlugin() {
        this.description = new PluginDescriptionFile(
                getClass().getSimpleName(),
                "1.0.0",
                getClass().getName());
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
    public File getDataFolder() {
        File folder = new File("plugins", getName());
        if (!folder.exists()) {
            folder.mkdirs();
        }
        return folder;
    }

    @Override
    public FileConfiguration getConfig() {
        if (config == null) {
            reloadConfig();
        }
        return config;
    }

    @Override
    public void saveConfig() {
        try {
            getConfig().save(new File(getDataFolder(), "config.yml"));
        } catch (Exception ignored) {
        }
    }

    @Override
    public void saveDefaultConfig() {
        File configFile = new File(getDataFolder(), "config.yml");
        if (!configFile.exists()) {
            if (getResource("config.yml") != null) {
                saveResource("config.yml", false);
            } else {
                try {
                    File parent = configFile.getParentFile();
                    if (parent != null && !parent.exists()) {
                        parent.mkdirs();
                    }
                    getConfig().set("plugin.initialized", true);
                    saveConfig();
                } catch (Exception ignored) {
                }
            }
        }
    }

    @Override
    public void reloadConfig() {
        File configFile = new File(getDataFolder(), "config.yml");
        config = YamlConfiguration.loadConfiguration(configFile);
        InputStream defConfigStream = getResource("config.yml");
        if (defConfigStream != null) {
            try {
                String defConfig = new String(defConfigStream.readAllBytes(), StandardCharsets.UTF_8);
                YamlConfiguration defaults = YamlConfiguration.loadConfigurationFromString(defConfig);
                for (Map.Entry<String, Object> entry : defaults.getValues(true).entrySet()) {
                    config.addDefault(entry.getKey(), entry.getValue());
                }
            } catch (Exception ignored) {
            }
        }
    }

    /**
     * Saves an embedded resource file from the plugin JAR into the plugin's data folder.
     *
     * @param resourcePath relative path to embedded resource
     * @param replace      whether to overwrite if file already exists
     */
    public void saveResource(String resourcePath, boolean replace) {
        if (resourcePath == null || resourcePath.isBlank()) {
            return;
        }
        String normalizedPath = resourcePath.replace('\\', '/');
        InputStream in = getResource(normalizedPath);
        if (in == null) {
            return;
        }
        File outFile = new File(getDataFolder(), normalizedPath);
        if (outFile.exists() && !replace) {
            return;
        }
        File outDir = outFile.getParentFile();
        if (outDir != null && !outDir.exists()) {
            outDir.mkdirs();
        }
        try (in; OutputStream out = new FileOutputStream(outFile)) {
            in.transferTo(out);
        } catch (Exception ignored) {
        }
    }

    @Override
    public InputStream getResource(String filename) {
        if (filename == null) {
            return null;
        }
        ClassLoader cl = getClass().getClassLoader();
        return cl != null ? cl.getResourceAsStream(filename) : null;
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

    public PluginCommand getCommand(String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        String key = name.trim().toLowerCase(Locale.ROOT);
        PluginCommand existing = commands.get(key);
        if (existing != null) {
            return existing;
        }
        PluginCommand created = new PluginCommand(key, this);
        PluginCommand prior = commands.putIfAbsent(key, created);
        return prior != null ? prior : created;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        return false;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return List.of();
    }

    @SuppressWarnings("unchecked")
    public static <T extends JavaPlugin> T getPlugin(Class<T> clazz) {
        Objects.requireNonNull(clazz, "clazz must not be null");
        if (!JavaPlugin.class.isAssignableFrom(clazz)) {
            throw new IllegalArgumentException(clazz + " does not extend " + JavaPlugin.class);
        }
        Server s = Bukkit.getServer();
        if (s != null && s.getPluginManager() != null) {
            for (Plugin p : s.getPluginManager().getPlugins()) {
                if (clazz.isInstance(p)) {
                    return (T) p;
                }
            }
        }
        throw new IllegalArgumentException(clazz + " is not currently loaded.");
    }

    @SuppressWarnings("unchecked")
    public static <T extends JavaPlugin> T getProvidingPlugin(Class<?> clazz) {
        Objects.requireNonNull(clazz, "clazz must not be null");
        Server s = Bukkit.getServer();
        if (s != null && s.getPluginManager() != null) {
            for (Plugin p : s.getPluginManager().getPlugins()) {
                if (p.getClass().getClassLoader() == clazz.getClassLoader()) {
                    return (T) p;
                }
            }
        }
        throw new IllegalArgumentException(clazz + " is not provided by a plugin.");
    }
}
