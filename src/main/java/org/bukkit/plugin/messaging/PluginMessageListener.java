package org.bukkit.plugin.messaging;

import org.bukkit.entity.Player;

/**
 * Callback interface for receiving custom plugin channel payload packets
 * ({@code org.bukkit.plugin.messaging.PluginMessageListener}).
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
@FunctionalInterface
public interface PluginMessageListener {

    /**
     * Invoked when a custom payload message arrives on a registered incoming plugin channel.
     *
     * @param channel plugin messaging channel identifier (e.g., {@code "BungeeCord"} or {@code "bungeecord:main"})
     * @param player  player connection through which the message was received
     * @param message raw payload bytes
     */
    void onPluginMessageReceived(String channel, Player player, byte[] message);
}
