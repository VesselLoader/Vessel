package org.bukkit.event.inventory;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.Inventory;

/**
 * Canonical Bukkit {@code InventoryCloseEvent} fired when a player closes an open inventory view.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public final class InventoryCloseEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Inventory inventory;
    private final Player player;

    public InventoryCloseEvent(Inventory inventory, Player player) {
        this.inventory = inventory;
        this.player = player;
    }

    public Inventory getInventory() {
        return inventory;
    }

    public Player getPlayer() {
        return player;
    }

    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
