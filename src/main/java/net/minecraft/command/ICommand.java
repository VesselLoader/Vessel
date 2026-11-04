package net.minecraft.command;

/**
 * Universal cross-era shim for Minecraft 1.12.2 ICommand interface.
 * Allows legacy Forge 1.12.2 mods registering server commands to resolve the interface.
 * Maintained under VESSEL Rule 17: Standalone single-purpose interface.
 *
 * @version 1.0.0
 */
public interface ICommand extends Comparable<ICommand> {

    /**
     * Returns command name.
     *
     * @return non-null command name
     */
    String getName();

    /**
     * Returns localized command usage message.
     *
     * @param sender command sender
     * @return usage string
     */
    String getUsage(Object sender);

    /**
     * Executes the command.
     *
     * @param server dedicated server instance
     * @param sender command sender
     * @param args   command arguments
     */
    void execute(Object server, Object sender, String[] args);

    @Override
    default int compareTo(ICommand other) {
        if (other == null) {
            return 1;
        }
        return this.getName().compareTo(other.getName());
    }
}
