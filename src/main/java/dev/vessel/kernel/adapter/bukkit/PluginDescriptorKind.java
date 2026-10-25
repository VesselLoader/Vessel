package dev.vessel.kernel.adapter.bukkit;

/**
 * Enumeration specifying the schema kind of a Bukkit/Paper plugin manifest.
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public enum PluginDescriptorKind {

    /**
     * Legacy Spigot / Bukkit standard descriptor ({@code plugin.yml}).
     */
    BUKKIT_PLUGIN_YML,

    /**
     * Modern Paper plugin descriptor ({@code paper-plugin.yml}).
     */
    PAPER_PLUGIN_YML
}
