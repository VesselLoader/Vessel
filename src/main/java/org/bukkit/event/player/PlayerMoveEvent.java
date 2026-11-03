package org.bukkit.event.player;

import java.util.Objects;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Canonical Bukkit {@code PlayerMoveEvent} fired when a player moves or rotates between two spatial
 * {@link Location} coordinates.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public class PlayerMoveEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private Location from;
    private Location to;
    private volatile boolean cancelled;

    public PlayerMoveEvent(Player player, Location from, Location to) {
        this.player = Objects.requireNonNull(player, "player must not be null");
        this.from = Objects.requireNonNull(from, "from must not be null");
        this.to = Objects.requireNonNull(to, "to must not be null");
    }

    public final Player getPlayer() {
        return player;
    }

    public final Location getFrom() {
        return from;
    }

    public final void setFrom(Location from) {
        this.from = Objects.requireNonNull(from, "from must not be null");
    }

    public final Location getTo() {
        return to;
    }

    public final void setTo(Location to) {
        this.to = Objects.requireNonNull(to, "to must not be null");
    }

    @Override
    public final boolean isCancelled() {
        return cancelled;
    }

    @Override
    public final void setCancelled(boolean cancel) {
        this.cancelled = cancel;
    }

    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
