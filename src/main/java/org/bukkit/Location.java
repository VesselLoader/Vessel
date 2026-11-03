package org.bukkit;

import org.bukkit.block.Block;

/**
 * Represents a 3D spatial position and orientation within a {@link World}
 * ({@code org.bukkit.Location}).
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public final class Location {

    private World world;
    private double x;
    private double y;
    private double z;
    private float yaw;
    private float pitch;

    /**
     * Constructs a new {@code Location} with zero rotation (`yaw = 0.0F`, `pitch = 0.0F`).
     *
     * @param world target world
     * @param x     X coordinate
     * @param y     Y coordinate
     * @param z     Z coordinate
     */
    public Location(World world, double x, double y, double z) {
        this(world, x, y, z, 0.0F, 0.0F);
    }

    /**
     * Constructs a new {@code Location} with explicit coordinates and rotation angles.
     *
     * @param world target world
     * @param x     X coordinate
     * @param y     Y coordinate
     * @param z     Z coordinate
     * @param yaw   horizontal rotation angle in degrees
     * @param pitch vertical rotation angle in degrees
     */
    public Location(World world, double x, double y, double z, float yaw, float pitch) {
        this.world = world;
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = yaw;
        this.pitch = pitch;
    }

    public World getWorld() {
        return world;
    }

    public void setWorld(World world) {
        this.world = world;
    }

    public double getX() {
        return x;
    }

    public void setX(double x) {
        this.x = x;
    }

    public double getY() {
        return y;
    }

    public void setY(double y) {
        this.y = y;
    }

    public double getZ() {
        return z;
    }

    public void setZ(double z) {
        this.z = z;
    }

    public int getBlockX() {
        return (int) Math.floor(x);
    }

    public int getBlockY() {
        return (int) Math.floor(y);
    }

    public int getBlockZ() {
        return (int) Math.floor(z);
    }

    public float getYaw() {
        return yaw;
    }

    public float getPitch() {
        return pitch;
    }

    /**
     * Translates this location by the specified coordinate deltas in-place.
     *
     * @param dx delta X
     * @param dy delta Y
     * @param dz delta Z
     * @return this mutated {@code Location}
     */
    public Location add(double dx, double dy, double dz) {
        this.x += dx;
        this.y += dy;
        this.z += dz;
        return this;
    }

    /**
     * Returns the {@link Block} located at this position's floored coordinates, or {@code null}
     * if no world is bound.
     *
     * @return block at this location, or {@code null}
     */
    public Block getBlock() {
        return world != null ? world.getBlockAt(getBlockX(), getBlockY(), getBlockZ()) : null;
    }
}
