package org.bukkit;

import java.util.Objects;

/**
 * Canonical Bukkit {@code WorldBorder} handle managing spatial boundary size, center coordinates,
 * damage parameters, and warning distances for a {@link World}.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public final class WorldBorder {

    private volatile double size = 60000000.0D;
    private volatile double centerX = 0.0D;
    private volatile double centerZ = 0.0D;
    private volatile double damageAmount = 0.2D;
    private volatile double damageBuffer = 5.0D;
    private volatile int warningDistance = 5;

    public double getSize() {
        return size;
    }

    public void setSize(double newSize) {
        this.size = Math.max(1.0D, newSize);
    }

    public double getCenterX() {
        return centerX;
    }

    public double getCenterZ() {
        return centerZ;
    }

    public void setCenter(double x, double z) {
        this.centerX = x;
        this.centerZ = z;
    }

    public void setCenter(Location location) {
        Objects.requireNonNull(location, "location must not be null");
        setCenter(location.getX(), location.getZ());
    }

    public double getDamageAmount() {
        return damageAmount;
    }

    public void setDamageAmount(double damage) {
        this.damageAmount = Math.max(0.0D, damage);
    }

    public double getDamageBuffer() {
        return damageBuffer;
    }

    public void setDamageBuffer(double blocks) {
        this.damageBuffer = Math.max(0.0D, blocks);
    }

    public int getWarningDistance() {
        return warningDistance;
    }

    public void setWarningDistance(int distance) {
        this.warningDistance = Math.max(0, distance);
    }

    public boolean isInside(Location location) {
        Objects.requireNonNull(location, "location must not be null");
        double halfSize = size / 2.0D;
        return Math.abs(location.getX() - centerX) <= halfSize
                && Math.abs(location.getZ() - centerZ) <= halfSize;
    }
}
