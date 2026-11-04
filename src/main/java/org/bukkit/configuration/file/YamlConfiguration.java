package org.bukkit.configuration.file;

import dev.vessel.kernel.config.yaml.ConfigurationSection;
import dev.vessel.kernel.config.yaml.SpigotYamlConfigurationAdapter;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Map;

/**
 * Canonical Bukkit/Spigot/Paper {@code YamlConfiguration} implementation backed by VESSEL's
 * clean-room {@link SpigotYamlConfigurationAdapter}.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.1
 */
public final class YamlConfiguration extends FileConfiguration {

    private final YamlConfigurationOptions options = new YamlConfigurationOptions();

    /**
     * Constructs an empty {@code YamlConfiguration}.
     */
    public YamlConfiguration() {
        super();
    }

    @Override
    public YamlConfigurationOptions options() {
        return options;
    }

    @Override
    public void loadFromString(String contents) {
        SpigotYamlConfigurationAdapter adapter = new SpigotYamlConfigurationAdapter();
        adapter.loadFromString(contents);
        values.clear();
        comments.clear();
        for (Map.Entry<String, Object> entry : adapter.getValues(true).entrySet()) {
            set(entry.getKey(), entry.getValue());
        }
    }

    @Override
    public String saveToString() {
        SpigotYamlConfigurationAdapter adapter = new SpigotYamlConfigurationAdapter();
        for (Map.Entry<String, Object> entry : getValues(true).entrySet()) {
            if (!(entry.getValue() instanceof ConfigurationSection)) {
                adapter.set(entry.getKey(), entry.getValue());
            }
        }
        return adapter.saveToString();
    }

    /**
     * Loads a {@code YamlConfiguration} from the given {@link File}. If {@code file} is
     * {@code null} or does not exist as a regular file, returns an empty configuration.
     *
     * @param file source YAML file
     * @return loaded {@code YamlConfiguration}
     */
    public static YamlConfiguration loadConfiguration(File file) {
        YamlConfiguration cfg = new YamlConfiguration();
        if (file != null && file.isFile()) {
            try {
                cfg.load(file);
            } catch (IOException e) {
                throw new UncheckedIOException("Failed to load YAML configuration from " + file, e);
            }
        }
        return cfg;
    }

    /**
     * Loads a {@code YamlConfiguration} directly from a raw YAML string.
     *
     * @param yamlContent raw YAML content string
     * @return loaded {@code YamlConfiguration}
     */
    public static YamlConfiguration loadConfigurationFromString(String yamlContent) {
        YamlConfiguration cfg = new YamlConfiguration();
        cfg.loadFromString(yamlContent);
        return cfg;
    }
}
