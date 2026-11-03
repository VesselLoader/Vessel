package dev.vessel.bootstrap.wave2;

import java.util.List;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Sample Bukkit {@link JavaPlugin} fixture verifying Wave 2 {@link PluginCommand} registration,
 * {@code CommandExecutor}, and {@code TabCompleter} callbacks.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public final class SampleBukkitCommandPluginFixture extends JavaPlugin {

    public SampleBukkitCommandPluginFixture() {
        super();
    }

    @Override
    public void onEnable() {
        PluginCommand cmd = getCommand("vesseltest");
        if (cmd != null) {
            cmd.setExecutor(this);
            cmd.setTabCompleter(this);
        }
    }

    @Override
    public boolean onCommand(
            CommandSender sender,
            Command command,
            String label,
            String[] args) {
        sender.sendMessage("Executed:" + label + ":" + args.length);
        return true;
    }

    @Override
    public List<String> onTabComplete(
            CommandSender sender,
            Command command,
            String alias,
            String[] args) {
        return List.of("arg1", "arg2");
    }
}
