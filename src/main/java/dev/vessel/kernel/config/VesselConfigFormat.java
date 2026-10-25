package dev.vessel.kernel.config;

/**
 * Supported configuration file formats across the Minecraft loader ecosystem.
 *
 * @version 1.0.0
 */
public enum VesselConfigFormat {

    /**
     * YAML format primarily used by Bukkit, Spigot, Paper, and Folia plugins.
     */
    YAML,

    /**
     * TOML format primarily used by Forge and NeoForge mods (NightConfig).
     */
    TOML,

    /**
     * JSON / JSON5 format primarily used by Fabric and Quilt mods.
     */
    JSON
}
