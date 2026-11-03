package org.bukkit.event.entity;

import java.util.Objects;
import java.util.UUID;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Canonical Bukkit {@code EntityDamageEvent} fired when an entity sustains damage from a specified
 * {@link DamageCause}.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public class EntityDamageEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final UUID entityUuid;
    private final DamageCause cause;
    private volatile double damage;
    private volatile boolean cancelled;

    public EntityDamageEvent(UUID entityUuid, DamageCause cause, double damage) {
        this.entityUuid = Objects.requireNonNull(entityUuid, "entityUuid must not be null");
        this.cause = Objects.requireNonNull(cause, "cause must not be null");
        this.damage = Math.max(0.0D, damage);
    }

    public final UUID getEntityUuid() {
        return entityUuid;
    }

    public final DamageCause getCause() {
        return cause;
    }

    public final double getDamage() {
        return damage;
    }

    public final void setDamage(double damage) {
        this.damage = Math.max(0.0D, damage);
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
