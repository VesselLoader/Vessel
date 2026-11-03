package io.papermc.paper.event.player;

import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Canonical Paper {@code AsyncChatEvent} fired when a player sends a chat message, carrying a
 * structured Adventure {@link Component} payload.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public final class AsyncChatEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private Component message;
    private boolean cancelled;

    public AsyncChatEvent(boolean async, Player player, Component message) {
        super(async);
        this.player = player;
        this.message = message;
    }

    public AsyncChatEvent(Player player, Component message) {
        this(true, player, message);
    }

    public Player getPlayer() {
        return player;
    }

    public Component message() {
        return message;
    }

    public void message(Component message) {
        this.message = message;
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
