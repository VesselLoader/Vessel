package org.bukkit;

/**
 * Canonical Bukkit {@code Difficulty} enumeration representing standard world difficulty levels.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public enum Difficulty {
    PEACEFUL(0),
    EASY(1),
    NORMAL(2),
    HARD(3);

    private final int value;

    Difficulty(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public static Difficulty getByValue(int value) {
        for (Difficulty difficulty : values()) {
            if (difficulty.value == value) {
                return difficulty;
            }
        }
        return null;
    }
}
