package org.bukkit.event.inventory;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

/**
 * Canonical Bukkit {@code InventoryClickEvent} fired when a player clicks a slot within an open
 * inventory view.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public final class InventoryClickEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Inventory inventory;
    private final Player whoClicked;
    private final int slot;
    private ItemStack currentItem;
    private boolean cancelled;

    public InventoryClickEvent(Inventory inventory, Player whoClicked, int slot, ItemStack currentItem) {
        this.inventory = inventory;
        this.whoClicked = whoClicked;
        this.slot = slot;
        this.currentItem = currentItem;
    }

    public Inventory getInventory() {
        return inventory;
    }

    public Player getWhoClicked() {
        return whoClicked;
    }

    public int getSlot() {
        return slot;
    }

    public ItemStack getCurrentItem() {
        return currentItem;
    }

    public void setCurrentItem(ItemStack currentItem) {
        this.currentItem = currentItem;
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
