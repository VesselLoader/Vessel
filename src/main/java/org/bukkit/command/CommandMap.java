package org.bukkit.command;

import java.util.List;

/**
 * Core Bukkit contract ({@code org.bukkit.command.CommandMap}) managing the registration,
 * lookup, dispatching, and tab completion of commands across Bukkit plugins.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public interface CommandMap {

    /**
     * Registers all given commands under the specified fallback prefix.
     *
     * @param fallbackPrefix prefix used to disambiguate commands with identical names
     * @param commands       list of commands to register
     */
    void registerAll(String fallbackPrefix, List<Command> commands);

    /**
     * Registers a command using its default label.
     *
     * @param fallbackPrefix prefix used to disambiguate
     * @param command        the command to register
     * @return {@code true} if registered without collision, {@code false} otherwise
     */
    boolean register(String fallbackPrefix, Command command);

    /**
     * Registers a command under an explicit label.
     *
     * @param label          the explicit command label
     * @param fallbackPrefix prefix used to disambiguate
     * @param command        the command to register
     * @return {@code true} if registered without collision, {@code false} otherwise
     */
    boolean register(String label, String fallbackPrefix, Command command);

    /**
     * Dispatches the given command line from the specified sender.
     *
     * @param sender  the sender issuing the command
     * @param cmdLine raw command line (with or without leading slash)
     * @return {@code true} if the command was recognized and executed, {@code false} otherwise
     */
    boolean dispatch(CommandSender sender, String cmdLine);

    /**
     * Clears all registered commands from this command map.
     */
    void clearCommands();

    /**
     * Retrieves the registered command by name or alias.
     *
     * @param name command name or alias
     * @return the registered command, or {@code null} if not found
     */
    Command getCommand(String name);

    /**
     * Requests tab completion suggestions for the given command line.
     *
     * @param sender  the sender requesting completion
     * @param cmdLine partial command line
     * @return list of suggestions
     */
    List<String> tabComplete(CommandSender sender, String cmdLine);
}
