package net.md_5.bungee.api.connection;

import java.util.UUID;

/**
 * BungeeCord proxied player interface ({@code net.md_5.bungee.api.connection.ProxiedPlayer}).
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public interface ProxiedPlayer {

    /**
     * Returns the player's username.
     *
     * @return username
     */
    String getName();

    /**
     * Returns the player's unique identifier.
     *
     * @return UUID
     */
    UUID getUniqueId();

    /**
     * Checks whether the player is currently connected.
     *
     * @return {@code true} if connected
     */
    boolean isConnected();

    /**
     * Sends a plain text message to this player.
     *
     * @param message text message
     */
    void sendMessage(String message);
}
