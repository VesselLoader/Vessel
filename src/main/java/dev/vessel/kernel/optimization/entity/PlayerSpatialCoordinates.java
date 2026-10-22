package dev.vessel.kernel.optimization.entity;

/**
 * Immutable spatial coordinate triple for active players.
 *
 * @param x coordinate X
 * @param y coordinate Y
 * @param z coordinate Z
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public record PlayerSpatialCoordinates(double x, double y, double z) {
}
