package dev.vessel.kernel.optimization.entity;

/**
 * Immutable spatial goal descriptor for an asynchronous pathfinding search.
 *
 * @param entityId      unique entity identifier
 * @param startX        starting X coordinate
 * @param startY        starting Y coordinate
 * @param startZ        starting Z coordinate
 * @param targetX       target destination X coordinate
 * @param targetY       target destination Y coordinate
 * @param targetZ       target destination Z coordinate
 * @param searchRadius  maximum search expansion radius
 * @param entitySpeed   movement speed attribute
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public record PathfindingGoal(
        int entityId,
        double startX,
        double startY,
        double startZ,
        double targetX,
        double targetY,
        double targetZ,
        double searchRadius,
        float entitySpeed
) {
    public double distanceSquared() {
        double dx = targetX - startX;
        double dy = targetY - startY;
        double dz = targetZ - startZ;
        return dx * dx + dy * dy + dz * dz;
    }
}
