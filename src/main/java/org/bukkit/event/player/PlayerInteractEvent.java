package org.bukkit.event.player;

import java.util.Objects;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.ItemStack;

/**
 * Canonical Bukkit {@code PlayerInteractEvent} fired when a player interacts with a block or air
 * using an optional held item.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public final class PlayerInteractEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final Action action;
    private final ItemStack item;
    private final Block clickedBlock;
    private volatile boolean cancelled;

    public PlayerInteractEvent(Player player, Action action, ItemStack item, Block clickedBlock) {
        this.player = Objects.requireNonNull(player, "player must not be null");
        this.action = Objects.requireNonNull(action, "action must not be null");
        this.item = item;
        this.clickedBlock = clickedBlock;
    }

    public Player getPlayer() {
        return player;
    }

    public Action getAction() {
        return action;
    }

    public ItemStack getItem() {
        return item;
    }

    public boolean hasItem() {
        return item != null;
    }

    public Block getClickedBlock() {
        return clickedBlock;
    }

    public boolean hasBlock() {
        return clickedBlock != null;
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
