package dev.vessel.kernel.optimization.block;

/**
 * Primitive functional interface testing three integer coordinate values without object allocations.
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
@FunctionalInterface
public interface TriIntPredicate {

    /**
     * Evaluates the predicate on the given coordinates.
     *
     * @param x coordinate X
     * @param y coordinate Y
     * @param z coordinate Z
     * @return true if matches
     */
    boolean test(int x, int y, int z);
}
