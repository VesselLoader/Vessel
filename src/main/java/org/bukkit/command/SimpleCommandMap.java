package org.bukkit.command;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Server;

/**
 * Standard thread-safe clean-room implementation of {@link CommandMap} maintaining the registry of
 * all live Bukkit plugin commands and aliases.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public final class SimpleCommandMap implements CommandMap {

    private final Map<String, Command> knownCommands = new ConcurrentHashMap<>();
    private final Server server;

    /**
     * Constructs a {@code SimpleCommandMap} bound to the given Bukkit {@link Server}.
     *
     * @param server active Bukkit server facade
     */
    public SimpleCommandMap(Server server) {
        this.server = server;
    }

    @Override
    public void registerAll(String fallbackPrefix, List<Command> commands) {
        if (commands == null) {
            return;
        }
        for (Command c : commands) {
            register(fallbackPrefix, c);
        }
    }

    @Override
    public boolean register(String fallbackPrefix, Command command) {
        if (command == null) {
            return false;
        }
        return register(command.getName(), fallbackPrefix, command);
    }

    @Override
    public boolean register(String label, String fallbackPrefix, Command command) {
        if (label == null || command == null) {
            return false;
        }
        String cleanLabel = label.trim().toLowerCase(Locale.ROOT);
        String prefix = (fallbackPrefix != null && !fallbackPrefix.isBlank())
                ? fallbackPrefix.trim().toLowerCase(Locale.ROOT)
                : "bukkit";

        // Register under explicit label
        knownCommands.put(cleanLabel, command);

        // Register under prefixed label (e.g., "chunky:chunky")
        String namespacedKey = prefix + ":" + cleanLabel;
        knownCommands.put(namespacedKey, command);

        // Register aliases
        for (String alias : command.getAliases()) {
            if (alias != null && !alias.isBlank()) {
                String cleanAlias = alias.trim().toLowerCase(Locale.ROOT);
                knownCommands.putIfAbsent(cleanAlias, command);
                knownCommands.put(prefix + ":" + cleanAlias, command);
            }
        }
        return true;
    }

    @Override
    public boolean dispatch(CommandSender sender, String cmdLine) {
        if (sender == null || cmdLine == null || cmdLine.isBlank()) {
            return false;
        }
        String trimmed = cmdLine.trim();
        String commandText = trimmed.startsWith("/") ? trimmed.substring(1).trim() : trimmed;
        if (commandText.isEmpty()) {
            return false;
        }
        String[] tokens = commandText.split("\\s+");
        String label = tokens[0].toLowerCase(Locale.ROOT);
        Command target = knownCommands.get(label);
        if (target == null) {
            return false;
        }
        String[] args;
        if (tokens.length <= 1) {
            args = new String[0];
        } else {
            args = new String[tokens.length - 1];
            System.arraycopy(tokens, 1, args, 0, args.length);
        }
        return target.execute(sender, label, args);
    }

    @Override
    public void clearCommands() {
        knownCommands.clear();
    }

    @Override
    public Command getCommand(String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        return knownCommands.get(name.trim().toLowerCase(Locale.ROOT));
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String cmdLine) {
        if (sender == null || cmdLine == null) {
            return List.of();
        }
        String trimmed = cmdLine.startsWith("/") ? cmdLine.substring(1) : cmdLine;
        String[] tokens = trimmed.split("\\s+", -1);
        if (tokens.length <= 1) {
            String prefix = tokens[0].toLowerCase(Locale.ROOT);
            List<String> matched = new ArrayList<>();
            for (String key : knownCommands.keySet()) {
                if (key.startsWith(prefix) && !key.contains(":")) {
                    matched.add("/" + key);
                }
            }
            return Collections.unmodifiableList(matched);
        }
        String label = tokens[0].toLowerCase(Locale.ROOT);
        Command target = knownCommands.get(label);
        if (target == null) {
            return List.of();
        }
        String[] args = new String[tokens.length - 1];
        System.arraycopy(tokens, 1, args, 0, args.length);
        return target.tabComplete(sender, label, args);
    }

    /**
     * Returns an unmodifiable snapshot of all known registered commands mapped by label.
     *
     * @return unmodifiable map of registered commands
     */
    public Map<String, Command> getKnownCommands() {
        return Collections.unmodifiableMap(knownCommands);
    }

    /**
     * Returns the Bukkit Server instance hosting this command map.
     *
     * @return active Bukkit server
     */
    public Server getServer() {
        return server;
    }
}
