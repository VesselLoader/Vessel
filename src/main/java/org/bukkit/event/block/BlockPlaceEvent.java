package org.bukkit.event.block;

import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Canonical Bukkit {@code BlockPlaceEvent} fired when a block is placed by a player.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public final class BlockPlaceEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Block placedBlock;
    private final Player player;
    private boolean canBuild = true;
    private boolean cancelled;

    public BlockPlaceEvent(Block placedBlock, Player player) {
        this.placedBlock = placedBlock;
        this.player = player;
    }

    public Block getBlock() {
        return placedBlock;
    }

    public Block getBlockPlaced() {
        return placedBlock;
    }

    public Player getPlayer() {
        return player;
    }

    public boolean canBuild() {
        return canBuild;
    }

    public void setBuild(boolean canBuild) {
        this.canBuild = canBuild;
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
