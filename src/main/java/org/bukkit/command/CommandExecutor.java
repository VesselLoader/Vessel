package org.bukkit.command;

/**
 * Canonical Bukkit {@code CommandExecutor} interface for handling command execution callbacks.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public interface CommandExecutor {

    boolean onCommand(CommandSender sender, Command command, String label, String[] args);
}
