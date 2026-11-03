package com.velocitypowered.api.event.connection;

import com.velocitypowered.api.proxy.Player;
import java.util.Objects;

/**
 * Velocity event fired after a player successfully completes authentication and connects to the
 * proxy ({@code com.velocitypowered.api.event.connection.PostLoginEvent}).
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public final class PostLoginEvent {

    private final Player player;

    /**
     * Constructs a new {@code PostLoginEvent} for the specified connected player.
     *
     * @param player connected player
     */
    public PostLoginEvent(Player player) {
        this.player = Objects.requireNonNull(player, "player must not be null");
    }

    /**
     * Returns the player who completed the login sequence.
     *
     * @return player
     */
    public Player getPlayer() {
        return player;
    }
}
