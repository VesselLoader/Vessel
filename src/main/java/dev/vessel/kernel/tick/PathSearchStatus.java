package dev.vessel.kernel.tick;

/**
 * Termination status outcomes for the FlatArrayMinHeapPathfinder A* search.
 * <p>
 * Directly resolves ERR-40 and OPT-040..050.
 * </p>
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public enum PathSearchStatus {

    /**
     * An optimal path was successfully discovered from start to goal.
     */
    FOUND,

    /**
     * Search space was exhausted without finding a reachable path to the goal.
     */
    NOT_FOUND,

    /**
     * Search terminated early because the iteration limit was exceeded.
     */
    EXCEEDED_MAX_ITERATIONS,

    /**
     * Starting point or destination is inside an impassable obstacle.
     */
    UNREACHABLE
}
