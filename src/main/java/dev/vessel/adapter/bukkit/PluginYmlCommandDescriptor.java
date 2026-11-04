package dev.vessel.adapter.bukkit;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Immutable descriptor representing a command declared in a Bukkit {@code plugin.yml} or
 * {@code paper-plugin.yml} descriptor file.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public final class PluginYmlCommandDescriptor {

    private final String name;
    private final String description;
    private final String usage;
    private final String permission;
    private final List<String> aliases;

    /**
     * Constructs a command descriptor with all parsed attributes.
     *
     * @param name        the primary command name
     * @param description human-readable command description
     * @param usage       command syntax usage string
     * @param permission  required permission node
     * @param aliases     list of command alias strings
     */
    public PluginYmlCommandDescriptor(
            String name,
            String description,
            String usage,
            String permission,
            List<String> aliases) {
        this.name = Objects.requireNonNull(name, "name must not be null").trim();
        this.description = description != null ? description.trim() : "";
        this.usage = usage != null ? usage.trim() : ("/" + this.name);
        this.permission = permission != null ? permission.trim() : "";
        this.aliases = aliases != null ? List.copyOf(aliases) : List.of();
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getUsage() {
        return usage;
    }

    public String getPermission() {
        return permission;
    }

    public List<String> getAliases() {
        return aliases;
    }
}
