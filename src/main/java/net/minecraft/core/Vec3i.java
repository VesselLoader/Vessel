package net.minecraft.core;

import java.util.Objects;

/**
 * Immutable 3D integer coordinate vector ({@code net.minecraft.core.Vec3i}).
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public class Vec3i implements Comparable<Vec3i> {

    /**
     * Origin vector {@code (0, 0, 0)}.
     */
    public static final Vec3i ZERO = new Vec3i(0, 0, 0);

    private final int x;
    private final int y;
    private final int z;

    /**
     * Constructs a new {@code Vec3i} with the given integer coordinates.
     *
     * @param x X coordinate
     * @param y Y coordinate
     * @param z Z coordinate
     */
    public Vec3i(int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    /**
     * Returns the X coordinate.
     *
     * @return X coordinate
     */
    public final int getX() {
        return x;
    }

    /**
     * Returns the Y coordinate.
     *
     * @return Y coordinate
     */
    public final int getY() {
        return y;
    }

    /**
     * Returns the Z coordinate.
     *
     * @return Z coordinate
     */
    public final int getZ() {
        return z;
    }

    /**
     * Computes the Manhattan distance between this vector and {@code other}.
     *
     * @param other target vector
     * @return Manhattan distance
     */
    public final int distManhattan(Vec3i other) {
        Objects.requireNonNull(other, "other must not be null");
        return Math.abs(this.x - other.x)
                + Math.abs(this.y - other.y)
                + Math.abs(this.z - other.z);
    }

    /**
     * Computes the squared Euclidean distance between this vector and {@code other}.
     *
     * @param other target vector
     * @return squared Euclidean distance
     */
    public final double distSqr(Vec3i other) {
        Objects.requireNonNull(other, "other must not be null");
        double dx = (double) this.x - other.x;
        double dy = (double) this.y - other.y;
        double dz = (double) this.z - other.z;
        return dx * dx + dy * dy + dz * dz;
    }

    @Override
    public int compareTo(Vec3i other) {
        if (this.y != other.y) {
            return Integer.compare(this.y, other.y);
        }
        if (this.z != other.z) {
            return Integer.compare(this.z, other.z);
        }
        return Integer.compare(this.x, other.x);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Vec3i other)) {
            return false;
        }
        return this.x == other.x && this.y == other.y && this.z == other.z;
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y, z);
    }

    @Override
    public String toString() {
        return "Vec3i{" + "x=" + x + ", y=" + y + ", z=" + z + '}';
    }
}
