package org.bukkit.event.inventory;

import java.util.Objects;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.Inventory;

/**
 * Fired when a {@link Player} opens an {@link Inventory} view
 * ({@code org.bukkit.event.inventory.InventoryOpenEvent}).
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public final class InventoryOpenEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Inventory inventory;
    private final Player player;
    private boolean cancelled;

    /**
     * Constructs a new {@code InventoryOpenEvent}.
     *
     * @param inventory non-null inventory being opened
     * @param player    non-null player opening the inventory
     */
    public InventoryOpenEvent(Inventory inventory, Player player) {
        this.inventory = Objects.requireNonNull(inventory, "inventory must not be null");
        this.player = Objects.requireNonNull(player, "player must not be null");
    }

    public Inventory getInventory() {
        return inventory;
    }

    public Player getPlayer() {
        return player;
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
