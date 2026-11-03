package com.velocitypowered.api.proxy;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

/**
 * Velocity proxy server API interface ({@code com.velocitypowered.api.proxy.ProxyServer}).
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public interface ProxyServer {

    /**
     * Looks up an online player by UUID.
     *
     * @param uuid player UUID
     * @return optional containing the player if connected
     */
    Optional<Player> getPlayer(UUID uuid);

    /**
     * Looks up an online player by username.
     *
     * @param username username to match
     * @return optional containing the player if connected
     */
    Optional<Player> getPlayer(String username);

    /**
     * Returns an unmodifiable collection of all players currently connected to this proxy.
     *
     * @return connected players
     */
    Collection<Player> getAllPlayers();

    /**
     * Returns the total number of connected players.
     *
     * @return player count
     */
    int getPlayerCount();
}
