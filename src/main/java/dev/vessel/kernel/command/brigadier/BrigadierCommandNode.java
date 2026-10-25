package dev.vessel.kernel.command.brigadier;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Clean-room representation of a hierarchical Brigadier command node within the
 * VESSEL sovereign command infrastructure.
 *
 * <p>Strictly adheres to Rule 17 with zero inner or nested classes.</p>
 *
 * @author VESSEL Engine Team
 * @version 1.0.0
 */
public final class BrigadierCommandNode {

    private final String name;
    private final String namespace;
    private final String description;
    private final String permission;
    private final Set<String> aliases;
    private final Map<String, BrigadierCommandNode> children;
    private final boolean executable;

    /**
     * Constructs an immutable Brigadier command node.
     *
     * @param name        the literal command name (non-null, non-blank)
     * @param namespace   the owning namespace identifier (e.g. "minecraft", "essentials", "vessel")
     * @param description human-readable command description (may be empty)
     * @param permission  required permission node (may be empty)
     * @param aliases     set of alternative literal aliases
     * @param children    map of child sub-command nodes keyed by literal name
     * @param executable  whether this node can be executed directly as an endpoint
     */
    public BrigadierCommandNode(
            String name,
            String namespace,
            String description,
            String permission,
            Set<String> aliases,
            Map<String, BrigadierCommandNode> children,
            boolean executable
    ) {
        this.name = Objects.requireNonNull(name, "Command name cannot be null").toLowerCase();
        this.namespace = (namespace != null && !namespace.isBlank()) ? namespace.toLowerCase() : "minecraft";
        this.description = description != null ? description : "";
        this.permission = permission != null ? permission : "";
        this.aliases = aliases != null
                ? Collections.unmodifiableSet(new LinkedHashSet<>(aliases))
                : Collections.emptySet();
        this.children = children != null
                ? Collections.unmodifiableMap(new LinkedHashMap<>(children))
                : Collections.emptyMap();
        this.executable = executable;
    }

    /**
     * Returns the literal command name.
     *
     * @return the command name in lowercase
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the origin namespace of this command node.
     *
     * @return the namespace in lowercase
     */
    public String getNamespace() {
        return namespace;
    }

    /**
     * Returns the qualified namespaced literal (e.g. "essentials:spawn").
     *
     * @return the fully-qualified namespaced command string
     */
    public String getQualifiedName() {
        return namespace + ":" + name;
    }

    /**
     * Returns the command description.
     *
     * @return the description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Returns the required permission node string.
     *
     * @return the permission node
     */
    public String getPermission() {
        return permission;
    }

    /**
     * Returns the unmodifiable set of command aliases.
     *
     * @return set of aliases
     */
    public Set<String> getAliases() {
        return aliases;
    }

    /**
     * Returns child command nodes keyed by literal sub-command name.
     *
     * @return unmodifiable map of child nodes
     */
    public Map<String, BrigadierCommandNode> getChildren() {
        return children;
    }

    /**
     * Checks if this command node is an executable endpoint.
     *
     * @return {@code true} if executable
     */
    public boolean isExecutable() {
        return executable;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        BrigadierCommandNode that = (BrigadierCommandNode) o;
        return executable == that.executable
                && Objects.equals(name, that.name)
                && Objects.equals(namespace, that.namespace);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, namespace, executable);
    }

    @Override
    public String toString() {
        return "BrigadierCommandNode[" + getQualifiedName() + "]";
    }
}
