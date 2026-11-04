package dev.vessel.adapter.bukkit;

import dev.vessel.command.VesselCommandRootRegistry;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import org.bukkit.command.CommandMap;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Automatically creates and registers {@link PluginCommand} instances into the server's
 * {@link CommandMap} for all declared {@link PluginYmlCommandDescriptor} items of a plugin,
 * while mirroring them to {@link VesselCommandRootRegistry} for instant console command routing.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.1
 */
public final class PluginCommandAutoRegistrar {

    /**
     * Registers all parsed commands of {@code plugin} into the given {@code commandMap}.
     *
     * @param plugin      the active plugin instance
     * @param descriptors list of command descriptors parsed from descriptor YAML
     * @param commandMap  the active server command map
     * @return the number of commands successfully registered
     */
    public int registerPluginCommands(
            JavaPlugin plugin,
            List<PluginYmlCommandDescriptor> descriptors,
            CommandMap commandMap) {
        Objects.requireNonNull(plugin, "plugin must not be null");
        Objects.requireNonNull(descriptors, "descriptors must not be null");
        Objects.requireNonNull(commandMap, "commandMap must not be null");

        int registeredCount = 0;
        String fallbackPrefix = plugin.getName();

        for (PluginYmlCommandDescriptor desc : descriptors) {
            if (desc == null || desc.getName().isBlank()) {
                continue;
            }
            PluginCommand cmd = plugin.getCommand(desc.getName());
            if (cmd != null) {
                if (!desc.getDescription().isEmpty()) {
                    cmd.setDescription(desc.getDescription());
                }
                if (!desc.getUsage().isEmpty()) {
                    cmd.setUsage(desc.getUsage());
                }
                if (!desc.getAliases().isEmpty()) {
                    cmd.setAliases(desc.getAliases());
                }
                commandMap.register(fallbackPrefix, cmd);
                String rootName = desc.getName().trim().toLowerCase(Locale.ROOT);
                VesselCommandRootRegistry.registerBukkitRoot(rootName);
                for (String alias : desc.getAliases()) {
                    if (alias != null && !alias.isBlank()) {
                        VesselCommandRootRegistry.registerBukkitRoot(alias.trim().toLowerCase(Locale.ROOT));
                    }
                }
                registeredCount++;
            }
        }
        return registeredCount;
    }
}
