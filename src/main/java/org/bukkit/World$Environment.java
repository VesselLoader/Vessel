package org.bukkit;

/**
 * Standard Bukkit {@code World.Environment} enum alias, representing world dimension types.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public enum World$Environment {
    NORMAL(0),
    NETHER(-1),
    THE_END(1),
    CUSTOM(2);

    private final int id;

    World$Environment(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public static World$Environment getEnvironment(int id) {
        return switch (id) {
            case -1 -> NETHER;
            case 1 -> THE_END;
            case 2 -> CUSTOM;
            default -> NORMAL;
        };
    }
}
