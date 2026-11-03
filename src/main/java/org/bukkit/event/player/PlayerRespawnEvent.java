package org.bukkit.event.player;

import java.util.Objects;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Canonical Bukkit {@code PlayerRespawnEvent} fired when a {@link Player} respawns after death.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public final class PlayerRespawnEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private Location respawnLocation;
    private final boolean isBedSpawn;
    private final boolean isAnchorSpawn;

    public PlayerRespawnEvent(
            Player respawnPlayer,
            Location respawnLocation,
            boolean isBedSpawn,
            boolean isAnchorSpawn) {
        this.player = Objects.requireNonNull(respawnPlayer, "respawnPlayer must not be null");
        this.respawnLocation = Objects.requireNonNull(respawnLocation, "respawnLocation must not be null");
        this.isBedSpawn = isBedSpawn;
        this.isAnchorSpawn = isAnchorSpawn;
    }

    public PlayerRespawnEvent(Player respawnPlayer, Location respawnLocation, boolean isBedSpawn) {
        this(respawnPlayer, respawnLocation, isBedSpawn, false);
    }

    public Player getPlayer() {
        return player;
    }

    public Location getRespawnLocation() {
        return respawnLocation;
    }

    public void setRespawnLocation(Location respawnLocation) {
        this.respawnLocation = Objects.requireNonNull(respawnLocation, "respawnLocation must not be null");
    }

    public boolean isBedSpawn() {
        return isBedSpawn;
    }

    public boolean isAnchorSpawn() {
        return isAnchorSpawn;
    }

    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
