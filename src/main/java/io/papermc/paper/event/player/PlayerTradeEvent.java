package io.papermc.paper.event.player;

import java.util.Objects;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;

/**
 * Paper player trade event ({@code io.papermc.paper.event.player.PlayerTradeEvent}).
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public final class PlayerTradeEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final ItemStack offered;
    private final ItemStack result;
    private boolean cancelled;

    /**
     * Constructs a new {@code PlayerTradeEvent} for player, offered item, and trade result.
     *
     * @param player  player completing trade
     * @param offered offered item
     * @param result  result item
     */
    public PlayerTradeEvent(Player player, ItemStack offered, ItemStack result) {
        this.player = Objects.requireNonNull(player, "player must not be null");
        this.offered = offered;
        this.result = result;
    }

    public Player getPlayer() {
        return player;
    }

    public ItemStack getOffered() {
        return offered;
    }

    public ItemStack getResult() {
        return result;
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
