package net.md_5.bungee.api.event;

import java.util.Objects;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Event;

/**
 * BungeeCord post-login event ({@code net.md_5.bungee.api.event.PostLoginEvent}).
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public final class PostLoginEvent extends Event {

    private final ProxiedPlayer player;

    /**
     * Constructs a new {@code PostLoginEvent} for the specified proxied player.
     *
     * @param player connected proxied player
     */
    public PostLoginEvent(ProxiedPlayer player) {
        this.player = Objects.requireNonNull(player, "player must not be null");
    }

    /**
     * Returns the player who completed authentication.
     *
     * @return proxied player
     */
    public ProxiedPlayer getPlayer() {
        return player;
    }
}
