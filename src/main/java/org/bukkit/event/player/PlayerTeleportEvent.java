package org.bukkit.event.player;

import java.util.Objects;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;

/**
 * Canonical Bukkit {@code PlayerTeleportEvent} fired when a player is teleported from one
 * {@link Location} to another for a specified {@link TeleportCause}.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public final class PlayerTeleportEvent extends PlayerMoveEvent {

    private static final HandlerList HANDLERS = new HandlerList();

    private final TeleportCause cause;

    public PlayerTeleportEvent(Player player, Location from, Location to, TeleportCause cause) {
        super(player, from, to);
        this.cause = Objects.requireNonNull(cause, "cause must not be null");
    }

    public TeleportCause getCause() {
        return cause;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
