package dev.vessel.kernel.world.dimensions;

/**
 * Event listener triggered whenever a dynamic dimension transitions across lifecycle states.
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public interface DimensionLifecycleEventListener {

    /**
     * Invoked when a dimension changes lifecycle state.
     *
     * @param dimensionKey canonical resource location of the dimension
     * @param oldState     previous lifecycle state
     * @param newState     new active lifecycle state
     */
    void onDimensionStateChanged(String dimensionKey, DimensionLifecycleState oldState, DimensionLifecycleState newState);
}
