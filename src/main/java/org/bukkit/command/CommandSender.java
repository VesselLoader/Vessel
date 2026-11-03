package org.bukkit.command;

import org.bukkit.permissions.Permissible;

/**
 * Canonical Bukkit {@code CommandSender} contract representing any actor capable of executing
 * commands and receiving chat/console feedback.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.1
 */
public interface CommandSender extends Permissible {

    String getName();

    void sendMessage(String message);

    @Override
    boolean hasPermission(String name);

    @Override
    boolean isOp();
}
