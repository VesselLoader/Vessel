package org.bukkit;

/**
 * Standard Bukkit {@code WorldBorder} contract managing spatial boundary size, center coordinates,
 * damage parameters, and warning distances for a {@link World}.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.2
 */
public interface WorldBorder {

    double getSize();

    void setSize(double newSize);

    double getCenterX();

    double getCenterZ();

    Location getCenter();

    void setCenter(double x, double z);

    void setCenter(Location location);

    double getDamageAmount();

    void setDamageAmount(double damage);

    double getDamageBuffer();

    void setDamageBuffer(double blocks);

    int getWarningDistance();

    void setWarningDistance(int distance);

    boolean isInside(Location location);
}
