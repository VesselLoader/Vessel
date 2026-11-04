package org.bukkit;

import java.util.Objects;

/**
 * Sovereign implementation of {@link WorldBorder} managing spatial boundary size, center coordinates,
 * damage parameters, and warning distances for a {@link World}.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public class VesselWorldBorder implements WorldBorder {

    private volatile double size = 60000000.0D;
    private volatile double centerX = 0.0D;
    private volatile double centerZ = 0.0D;
    private volatile double damageAmount = 0.2D;
    private volatile double damageBuffer = 5.0D;
    private volatile int warningDistance = 5;

    public VesselWorldBorder() {
    }

    @Override
    public double getSize() {
        return size;
    }

    @Override
    public void setSize(double newSize) {
        this.size = Math.max(1.0D, newSize);
    }

    @Override
    public double getCenterX() {
        return centerX;
    }

    @Override
    public double getCenterZ() {
        return centerZ;
    }

    @Override
    public Location getCenter() {
        return new Location(null, centerX, 0.0D, centerZ);
    }

    @Override
    public void setCenter(double x, double z) {
        this.centerX = x;
        this.centerZ = z;
    }

    @Override
    public void setCenter(Location location) {
        Objects.requireNonNull(location, "location must not be null");
        setCenter(location.getX(), location.getZ());
    }

    @Override
    public double getDamageAmount() {
        return damageAmount;
    }

    @Override
    public void setDamageAmount(double damage) {
        this.damageAmount = Math.max(0.0D, damage);
    }

    @Override
    public double getDamageBuffer() {
        return damageBuffer;
    }

    @Override
    public void setDamageBuffer(double blocks) {
        this.damageBuffer = Math.max(0.0D, blocks);
    }

    @Override
    public int getWarningDistance() {
        return warningDistance;
    }

    @Override
    public void setWarningDistance(int distance) {
        this.warningDistance = Math.max(0, distance);
    }

    @Override
    public boolean isInside(Location location) {
        Objects.requireNonNull(location, "location must not be null");
        double halfSize = size / 2.0D;
        return Math.abs(location.getX() - centerX) <= halfSize
                && Math.abs(location.getZ() - centerZ) <= halfSize;
    }
}
