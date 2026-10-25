package dev.vessel.kernel.optimization.entity;

/**
 * Functional callback interface invoked when an asynchronous path computation completes.
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
@FunctionalInterface
public interface PathfindingCallback {

    /**
     * Called upon completion of path search.
     *
     * @param result path search outcome
     */
    void onPathResult(PathfindingResult result);
}
