package com.velocitypowered.api.proxy;

import java.util.UUID;
import net.kyori.adventure.text.Component;

/**
 * Velocity player representation ({@code com.velocitypowered.api.proxy.Player}).
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public interface Player {

    /**
     * Returns the player's username.
     *
     * @return username
     */
    String getUsername();

    /**
     * Returns the player's unique identifier.
     *
     * @return UUID
     */
    UUID getUniqueId();

    /**
     * Checks whether the player session remains active on this proxy.
     *
     * @return {@code true} if active
     */
    boolean isActive();

    /**
     * Sends a chat/system message to this player.
     *
     * @param component text component message
     */
    void sendMessage(Component component);
}
