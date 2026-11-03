package org.bukkit.command;

import java.util.List;

/**
 * Canonical Bukkit {@code TabCompleter} interface for providing command argument tab-completion
 * suggestions.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public interface TabCompleter {

    List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args);
}
