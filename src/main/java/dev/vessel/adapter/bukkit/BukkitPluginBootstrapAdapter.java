package dev.vessel.adapter.bukkit;

import dev.vessel.kernel.config.yaml.MemoryConfigurationSection;
import dev.vessel.kernel.identity.SourceLoaderKind;
import dev.vessel.kernel.identity.VesselModuleIdentifier;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Bootstraps legacy Bukkit {@code plugin.yml} descriptors into canonical
 * {@link VesselModuleIdentifier} records and manages per-plugin {@link MemoryConfigurationSection}
 * instances.
 *
 * @version 1.0.0
 */
public final class BukkitPluginBootstrapAdapter {

    private final Map<String, MemoryConfigurationSection> pluginConfigs =
            new ConcurrentHashMap<>();

    /**
     * Parses a legacy Bukkit {@code plugin.yml} descriptor and creates a {@code BUKKIT}-scoped
     * {@link VesselModuleIdentifier}.
     *
     * @param pluginYamlContent the raw {@code plugin.yml} text
     * @return the canonical module identifier
     */
    public VesselModuleIdentifier loadLegacyBukkitPlugin(String pluginYamlContent) {
        Objects.requireNonNull(pluginYamlContent, "pluginYamlContent must not be null");
        String name = "unknown_plugin";
        String version = "1.0.0";
        String mainClass = "";

        String[] lines = pluginYamlContent.split("\\R");
        for (String rawLine : lines) {
            String line = rawLine.trim();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            int colon = line.indexOf(':');
            if (colon <= 0) {
                continue;
            }
            String key = line.substring(0, colon).trim();
            String value = stripQuotes(line.substring(colon + 1).trim());
            if ("name".equalsIgnoreCase(key) && !value.isEmpty()) {
                name = sanitizeNamespace(value);
            } else if ("version".equalsIgnoreCase(key) && !value.isEmpty()) {
                version = value;
            } else if ("main".equalsIgnoreCase(key) && !value.isEmpty()) {
                mainClass = value;
            }
        }

        MemoryConfigurationSection config = pluginConfigs.computeIfAbsent(
                name, ignored -> new MemoryConfigurationSection());
        config.set("plugin.name", name);
        config.set("plugin.version", version);
        if (!mainClass.isEmpty()) {
            config.set("plugin.main", mainClass);
        }
        return VesselModuleIdentifier.of(SourceLoaderKind.BUKKIT, name, version);
    }

    /**
     * Retrieves or creates the in-memory configuration section for a Bukkit plugin.
     *
     * @param pluginName the plugin name
     * @return the plugin's {@link MemoryConfigurationSection}
     */
    public MemoryConfigurationSection getPluginConfig(String pluginName) {
        Objects.requireNonNull(pluginName, "pluginName must not be null");
        String normalized = sanitizeNamespace(pluginName);
        return pluginConfigs.computeIfAbsent(normalized, ignored -> new MemoryConfigurationSection());
    }

    private static String stripQuotes(String value) {
        if (value.length() >= 2
                && ((value.startsWith("\"") && value.endsWith("\""))
                        || (value.startsWith("'") && value.endsWith("'")))) {
            return value.substring(1, value.length() - 1).trim();
        }
        return value;
    }

    private static String sanitizeNamespace(String raw) {
        String sanitized = raw.trim().toLowerCase().replaceAll("[^a-z0-9_.-]", "_");
        return sanitized.isEmpty() ? "unknown_plugin" : sanitized;
    }
}
