package com.example.mod.pure;

import dev.vessel.api.command.CommandPermissionFilter;
import dev.vessel.api.command.VesselCommandHub;

import java.util.List;
import java.util.Objects;

/**
 * Demonstrates cross-loader command registration and tab-completion trie resolution
 * using VESSEL Super-API.
 *
 * <p>A single command registered through {@link VesselCommandHub} is automatically transpiled
 * into native Brigadier command nodes for Forge/Fabric/NeoForge/Vanilla and Bukkit/Spigot/Paper command maps.</p>
 *
 * <p>Strictly adheres to Rule 17: Single Responsibility, zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.1
 * @since 1.0.0
 */
public final class SovereignModCommand {

    private final VesselCommandHub commandHub;
    private final CommandPermissionFilter permissionFilter;

    /**
     * Constructs a new sovereign mod command handler.
     */
    public SovereignModCommand() {
        this.commandHub = VesselCommandHub.getInstance();
        this.permissionFilter = commandHub.getPermissionFilter();
    }

    /**
     * Registers a command with its execution handler.
     *
     * @param commandName command name
     * @param executor    command executor handle
     */
    public void registerCommand(String commandName, Object executor) {
        Objects.requireNonNull(commandName, "commandName cannot be null");
        Objects.requireNonNull(executor, "executor cannot be null");

        commandHub.registerCommand(commandName, executor);
    }

    /**
     * Executes a command line on behalf of a sender.
     *
     * @param sender      command sender
     * @param commandLine command line text
     * @return {@code true} if successfully dispatched
     */
    public boolean execute(Object sender, String commandLine) {
        return commandHub.executeCommand(sender, commandLine);
    }

    /**
     * Resolves tab completions for a partial command input string from the trie cache.
     *
     * @param prefix partial text typed by the player
     * @return matching candidate completions from the high-speed trie
     */
    public List<String> resolveTabCompletions(String prefix) {
        return commandHub.getTabCompletionCache().complete(prefix);
    }

    /**
     * Grants a player or sender permission to use the command.
     *
     * @param sender command sender handle
     * @param node   permission node
     */
    public void grantPermission(Object sender, String node) {
        permissionFilter.assignPermission(sender, node);
    }
}
