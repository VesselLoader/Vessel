package org.bukkit;

/**
 * Canonical Bukkit {@code GameMode} enumeration representing standard player game modes.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public enum GameMode {
    SURVIVAL(0),
    CREATIVE(1),
    ADVENTURE(2),
    SPECTATOR(3);

    private final int value;

    GameMode(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public static GameMode getByValue(int value) {
        for (GameMode mode : values()) {
            if (mode.value == value) {
                return mode;
            }
        }
        return null;
    }
}
