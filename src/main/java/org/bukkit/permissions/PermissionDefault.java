package org.bukkit.permissions;

import java.util.Locale;

/**
 * Represents the default evaluation strategy for a {@link Permission} when not explicitly assigned.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public enum PermissionDefault {

    /**
     * Permission is granted to all permissible subjects by default.
     */
    TRUE,

    /**
     * Permission is denied to all permissible subjects by default.
     */
    FALSE,

    /**
     * Permission is granted by default only to server operators.
     */
    OP,

    /**
     * Permission is granted by default only to non-operator subjects.
     */
    NOT_OP;

    /**
     * Evaluates the default permission grant state for the given operator status.
     *
     * @param op {@code true} if the subject is a server operator
     * @return {@code true} if the permission is granted by default
     */
    public boolean getValue(boolean op) {
        return switch (this) {
            case TRUE -> true;
            case FALSE -> false;
            case OP -> op;
            case NOT_OP -> !op;
        };
    }

    /**
     * Looks up a {@code PermissionDefault} by its case-insensitive descriptor name.
     *
     * @param name descriptor string (e.g., {@code "true"}, {@code "false"}, {@code "op"}, {@code "notop"}, {@code "not_op"})
     * @return matched {@code PermissionDefault}, or {@code null} if unrecognized
     */
    public static PermissionDefault getByName(String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        return switch (name.trim().toLowerCase(Locale.ROOT)) {
            case "true" -> TRUE;
            case "false" -> FALSE;
            case "op", "isop" -> OP;
            case "notop", "not_op", "!op", "isnotop" -> NOT_OP;
            default -> null;
        };
    }
}
