package dev.vessel.kernel.optimization.entity;

/**
 * Immutable outcome of an asynchronous pathfinding search.
 *
 * @param entityId           unique entity identifier
 * @param waypointsX         calculated path waypoint X coordinates
 * @param waypointsY         calculated path waypoint Y coordinates
 * @param waypointsZ         calculated path waypoint Z coordinates
 * @param waypointCount      valid number of waypoints
 * @param success            true if a path was successfully found
 * @param cancelled          true if this request was superseded and cancelled
 * @param computeDurationNs  nanoseconds spent computing the path
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public record PathfindingResult(
        int entityId,
        double[] waypointsX,
        double[] waypointsY,
        double[] waypointsZ,
        int waypointCount,
        boolean success,
        boolean cancelled,
        long computeDurationNs
) {
    public static PathfindingResult cancelled(int entityId) {
        return new PathfindingResult(entityId, new double[0], new double[0], new double[0], 0, false, true, 0L);
    }

    public static PathfindingResult failure(int entityId, long durationNs) {
        return new PathfindingResult(entityId, new double[0], new double[0], new double[0], 0, false, false, durationNs);
    }
}
