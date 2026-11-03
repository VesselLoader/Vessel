package org.bukkit.event.server;

import java.util.Objects;
import org.bukkit.command.CommandSender;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Canonical Bukkit {@code ServerCommandEvent} fired when a command is dispatched from the server
 * console or a non-player {@link CommandSender}.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public final class ServerCommandEvent extends Event implements Cancellable {

    private static final HandlerList HANDLERS = new HandlerList();

    private final CommandSender sender;
    private String command;
    private boolean cancel;

    public ServerCommandEvent(CommandSender sender, String command) {
        this.sender = Objects.requireNonNull(sender, "sender must not be null");
        this.command = Objects.requireNonNull(command, "command must not be null");
    }

    public CommandSender getSender() {
        return sender;
    }

    public String getCommand() {
        return command;
    }

    public void setCommand(String command) {
        this.command = Objects.requireNonNull(command, "command must not be null");
    }

    @Override
    public boolean isCancelled() {
        return cancel;
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.cancel = cancel;
    }

    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
