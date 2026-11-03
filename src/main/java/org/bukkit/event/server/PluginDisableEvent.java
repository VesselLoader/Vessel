package org.bukkit.event.server;

import java.util.Objects;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.Plugin;

/**
 * Bukkit event fired when a plugin is disabled ({@code org.bukkit.event.server.PluginDisableEvent}).
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public final class PluginDisableEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Plugin plugin;

    /**
     * Constructs a new {@code PluginDisableEvent} for the specified disabled plugin.
     *
     * @param plugin disabled plugin
     */
    public PluginDisableEvent(Plugin plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin must not be null");
    }

    public Plugin getPlugin() {
        return plugin;
    }

    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
