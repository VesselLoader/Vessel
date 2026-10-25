package dev.vessel.kernel.tick;

import java.util.Objects;

/**
 * Immutable search result returned by the {@link FlatArrayMinHeapPathfinder}.
 * <p>
 * Contains the execution status, path length, primitive array of path nodes, and total cost.
 * </p>
 * <p>
 * Directly resolves ERR-40 and OPT-040..050.
 * </p>
 *
 * @param status path search outcome status
 * @param pathLength number of coordinate steps in the found path
 * @param packedCoordinates flat array of packed (x, y, z) coordinates along the path
 * @param totalCost cumulative traversal cost
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public record PathResult(
        PathSearchStatus status,
        int pathLength,
        int[] packedCoordinates,
        float totalCost
) {

    /**
     * Compact constructor validating invariants.
     */
    public PathResult {
        Objects.requireNonNull(status, "status cannot be null");
        Objects.requireNonNull(packedCoordinates, "packedCoordinates cannot be null");
        if (pathLength < 0) {
            throw new IllegalArgumentException("pathLength cannot be negative: " + pathLength);
        }
    }

    /**
     * Returns whether the search successfully located a path.
     *
     * @return {@code true} if status is FOUND
     */
    public boolean isSuccess() {
        return status == PathSearchStatus.FOUND;
    }
}
