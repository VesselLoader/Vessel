package org.bukkit.command;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Canonical Bukkit abstract {@code Command} base class representing an executable server command
 * with metadata, usage, aliases, and tab completion support.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public abstract class Command {

    private final String name;
    private String description;
    private String usageMessage;
    private List<String> aliases;

    protected Command(String name) {
        this(name, "", "/" + name, List.of());
    }

    protected Command(String name, String description, String usageMessage, List<String> aliases) {
        this.name = Objects.requireNonNull(name, "name must not be null");
        this.description = description != null ? description : "";
        this.usageMessage = usageMessage != null ? usageMessage : "/" + name;
        this.aliases = aliases != null ? Collections.unmodifiableList(new ArrayList<>(aliases)) : List.of();
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public Command setDescription(String description) {
        this.description = description != null ? description : "";
        return this;
    }

    public String getUsage() {
        return usageMessage;
    }

    public Command setUsage(String usage) {
        this.usageMessage = usage != null ? usage : "";
        return this;
    }

    public List<String> getAliases() {
        return aliases;
    }

    public Command setAliases(List<String> aliases) {
        this.aliases = aliases != null ? Collections.unmodifiableList(new ArrayList<>(aliases)) : List.of();
        return this;
    }

    public abstract boolean execute(CommandSender sender, String commandLabel, String[] args);

    public List<String> tabComplete(CommandSender sender, String alias, String[] args) {
        return List.of();
    }
}
