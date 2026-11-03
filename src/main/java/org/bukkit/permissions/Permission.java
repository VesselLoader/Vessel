package org.bukkit.permissions;

import java.util.Objects;

/**
 * Represents a named permission node and its default evaluation policy
 * ({@code org.bukkit.permissions.Permission}).
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public final class Permission {

    private final String name;
    private final String description;
    private volatile PermissionDefault defaultValue;

    /**
     * Constructs a new {@code Permission} defaulting to {@link PermissionDefault#OP}.
     *
     * @param name permission node name
     */
    public Permission(String name) {
        this(name, "", PermissionDefault.OP);
    }

    /**
     * Constructs a new {@code Permission} with an explicit default value.
     *
     * @param name         permission node name
     * @param defaultValue default evaluation policy (defaults to {@link PermissionDefault#OP} if null)
     */
    public Permission(String name, PermissionDefault defaultValue) {
        this(name, "", defaultValue);
    }

    /**
     * Constructs a new {@code Permission} with a name, description, and default value.
     *
     * @param name         permission node name
     * @param description  human-readable description (defaults to {@code ""} if null)
     * @param defaultValue default evaluation policy (defaults to {@link PermissionDefault#OP} if null)
     */
    public Permission(String name, String description, PermissionDefault defaultValue) {
        this.name = Objects.requireNonNull(name, "name must not be null");
        this.description = description != null ? description : "";
        this.defaultValue = defaultValue != null ? defaultValue : PermissionDefault.OP;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public PermissionDefault getDefault() {
        return defaultValue;
    }

    public void setDefault(PermissionDefault value) {
        this.defaultValue = Objects.requireNonNull(value, "value must not be null");
    }
}
