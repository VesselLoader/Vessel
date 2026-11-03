package net.minecraft.core;

/**
 * Canonical 6-way cardinal block face direction ({@code net.minecraft.core.Direction}).
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public enum Direction {

    DOWN(0, 1, "down", Axis.Y, 0, -1, 0),
    UP(1, 0, "up", Axis.Y, 0, 1, 0),
    NORTH(2, 3, "north", Axis.Z, 0, 0, -1),
    SOUTH(3, 2, "south", Axis.Z, 0, 0, 1),
    WEST(4, 5, "west", Axis.X, -1, 0, 0),
    EAST(5, 4, "east", Axis.X, 1, 0, 0);

    private final int data3d;
    private final int oppositeIndex;
    private final String name;
    private final Axis axis;
    private final int stepX;
    private final int stepY;
    private final int stepZ;

    Direction(
            int data3d,
            int oppositeIndex,
            String name,
            Axis axis,
            int stepX,
            int stepY,
            int stepZ) {
        this.data3d = data3d;
        this.oppositeIndex = oppositeIndex;
        this.name = name;
        this.axis = axis;
        this.stepX = stepX;
        this.stepY = stepY;
        this.stepZ = stepZ;
    }

    /**
     * Returns the 3D index of this direction ({@code 0..5}).
     *
     * @return 3D data value
     */
    public int get3DDataValue() {
        return data3d;
    }

    /**
     * Returns the lowercase name of this direction.
     *
     * @return direction name
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the coordinate {@link Axis} along which this direction points.
     *
     * @return coordinate axis
     */
    public Axis getAxis() {
        return axis;
    }

    /**
     * Returns the unit step offset along the X axis ({@code -1}, {@code 0}, or {@code 1}).
     *
     * @return X step
     */
    public int getStepX() {
        return stepX;
    }

    /**
     * Returns the unit step offset along the Y axis ({@code -1}, {@code 0}, or {@code 1}).
     *
     * @return Y step
     */
    public int getStepY() {
        return stepY;
    }

    /**
     * Returns the unit step offset along the Z axis ({@code -1}, {@code 0}, or {@code 1}).
     *
     * @return Z step
     */
    public int getStepZ() {
        return stepZ;
    }

    /**
     * Returns the opposite cardinal direction.
     *
     * @return opposite direction
     */
    public Direction getOpposite() {
        return values()[oppositeIndex];
    }
}
