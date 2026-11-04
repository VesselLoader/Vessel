package org.bukkit.event.world;

import java.util.Objects;
import org.bukkit.World;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Bukkit event fired when a {@link World} saves its data to disk.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.1
 */
public final class WorldSaveEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final World world;

    public WorldSaveEvent(World world) {
        this.world = Objects.requireNonNull(world, "world must not be null");
    }

    public World getWorld() {
        return world;
    }

    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
