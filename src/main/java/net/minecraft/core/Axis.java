package net.minecraft.core;

/**
 * Canonical 3D coordinate axis ({@code net.minecraft.core.Axis}) used by Minecraft block positions
 * and directional vectors.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public enum Axis {

    X("x"),
    Y("y"),
    Z("z");

    private final String name;

    Axis(String name) {
        this.name = name;
    }

    /**
     * Returns the lowercase identifier of this axis ({@code "x"}, {@code "y"}, or {@code "z"}).
     *
     * @return axis name
     */
    public String getName() {
        return name;
    }

    /**
     * Returns whether this axis lies on the horizontal plane ({@link #X} or {@link #Z}).
     *
     * @return {@code true} if horizontal
     */
    public boolean isHorizontal() {
        return this == X || this == Z;
    }

    /**
     * Returns whether this axis is vertical ({@link #Y}).
     *
     * @return {@code true} if vertical
     */
    public boolean isVertical() {
        return this == Y;
    }
}
