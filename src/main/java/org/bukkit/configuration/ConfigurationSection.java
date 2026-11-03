package org.bukkit.configuration;

/**
 * Canonical Bukkit/Spigot/Paper {@code ConfigurationSection} contract extending VESSEL's unified
 * {@link dev.vessel.kernel.config.yaml.ConfigurationSection}.
 *
 * <p>Inherits all path navigation, typed value accessors ({@code getString}, {@code getInt},
 * {@code getBoolean}, {@code getDouble}, {@code getLong}, {@code getStringList}), and section
 * traversal operations.</p>
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public interface ConfigurationSection extends dev.vessel.kernel.config.yaml.ConfigurationSection {
}
