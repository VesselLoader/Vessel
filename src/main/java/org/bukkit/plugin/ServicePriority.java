package org.bukkit.plugin;

/**
 * Represents the priority tier of a service provider registered with {@link ServicesManager}
 * ({@code org.bukkit.plugin.ServicePriority}).
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public enum ServicePriority {
    Lowest,
    Low,
    Normal,
    High,
    Highest
}
