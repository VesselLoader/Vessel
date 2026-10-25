package dev.vessel.kernel.world.dimensions;

import java.util.ArrayList;
import java.util.List;

/**
 * Concrete event listener recording state transitions for testing DynamicDimensionLifecycleSynchronizer.
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public final class TestDimensionEventListener implements DimensionLifecycleEventListener {

    private final List<DimensionLifecycleState> recordedStates = new ArrayList<>();

    @Override
    public void onDimensionStateChanged(String dimensionKey, DimensionLifecycleState oldState, DimensionLifecycleState newState) {
        recordedStates.add(newState);
    }

    public List<DimensionLifecycleState> getRecordedStates() {
        return recordedStates;
    }

    public void clear() {
        recordedStates.clear();
    }
}
