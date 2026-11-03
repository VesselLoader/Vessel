package org.bukkit.command;

import java.util.List;
import org.bukkit.plugin.Plugin;

/**
 * Canonical Bukkit {@code PluginCommand} bound to an owning {@link Plugin} with delegating
 * {@link CommandExecutor} and {@link TabCompleter} hooks.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public final class PluginCommand extends Command {

    private final Plugin owningPlugin;
    private volatile CommandExecutor executor;
    private volatile TabCompleter completer;

    public PluginCommand(String name, Plugin owningPlugin) {
        super(name);
        this.owningPlugin = owningPlugin;
        if (owningPlugin instanceof CommandExecutor ce) {
            this.executor = ce;
        }
        if (owningPlugin instanceof TabCompleter tc) {
            this.completer = tc;
        }
    }

    public Plugin getPlugin() {
        return owningPlugin;
    }

    public void setExecutor(CommandExecutor executor) {
        this.executor = executor;
    }

    public CommandExecutor getExecutor() {
        return executor;
    }

    public void setTabCompleter(TabCompleter completer) {
        this.completer = completer;
    }

    public TabCompleter getTabCompleter() {
        return completer;
    }

    @Override
    public boolean execute(CommandSender sender, String commandLabel, String[] args) {
        CommandExecutor activeExecutor = this.executor;
        if (activeExecutor != null) {
            return activeExecutor.onCommand(sender, this, commandLabel, args);
        }
        return false;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String alias, String[] args) {
        TabCompleter activeCompleter = this.completer;
        if (activeCompleter != null) {
            return activeCompleter.onTabComplete(sender, this, alias, args);
        }
        return super.tabComplete(sender, alias, args);
    }
}
