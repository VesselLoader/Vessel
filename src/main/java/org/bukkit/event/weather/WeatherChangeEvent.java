package org.bukkit.event.weather;

import java.util.Objects;
import org.bukkit.World;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Canonical Bukkit {@code WeatherChangeEvent} fired when precipitation state changes in a
 * {@link World}.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public final class WeatherChangeEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final World world;
    private final boolean toWeatherState;
    private volatile boolean cancelled;

    public WeatherChangeEvent(World world, boolean toWeatherState) {
        this.world = Objects.requireNonNull(world, "world must not be null");
        this.toWeatherState = toWeatherState;
    }

    public World getWorld() {
        return world;
    }

    public boolean toWeatherState() {
        return toWeatherState;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
    }

    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
