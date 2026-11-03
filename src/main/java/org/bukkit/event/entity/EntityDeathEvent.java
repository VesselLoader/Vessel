package org.bukkit.event.entity;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.bukkit.entity.Entity;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;

/**
 * Canonical Bukkit {@code EntityDeathEvent} fired when an {@link Entity} dies, carrying mutable
 * item drops and dropped experience orbs.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public class EntityDeathEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Entity entity;
    private final List<ItemStack> drops;
    private int droppedExp;

    public EntityDeathEvent(Entity entity, List<ItemStack> drops, int droppedExp) {
        this.entity = Objects.requireNonNull(entity, "entity must not be null");
        this.drops = drops != null ? new ArrayList<>(drops) : new ArrayList<>();
        this.droppedExp = Math.max(0, droppedExp);
    }

    public EntityDeathEvent(Entity entity, List<ItemStack> drops) {
        this(entity, drops, 0);
    }

    public Entity getEntity() {
        return entity;
    }

    public List<ItemStack> getDrops() {
        return drops;
    }

    public int getDroppedExp() {
        return droppedExp;
    }

    public void setDroppedExp(int exp) {
        this.droppedExp = Math.max(0, exp);
    }

    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
