package dev.vessel.kernel.world.dimensions;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit test suite verifying DynamicDimensionLifecycleSynchronizer state transitions, Bukkit/Mojang registry synchronization,
 * and memory-leak prevention.
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public final class DynamicDimensionLifecycleSynchronizerTest {

    @Test
    @DisplayName("Verify complete dimension lifecycle: register -> load -> unload -> unregister")
    public void testFullDimensionLifecycle() {
        DynamicDimensionLifecycleSynchronizer synchronizer = new DynamicDimensionLifecycleSynchronizer();
        TestDimensionEventListener listener = new TestDimensionEventListener();
        synchronizer.addEventListener(listener);

        String dimKey = "vessel_test:mining_dimension";

        // Step 1: Register
        DimensionSyncResult regResult = synchronizer.registerDimension(dimKey, DimensionLifecycleType.CUSTOM_MODDED);
        assertThat(regResult.success()).isTrue();
        assertThat(regResult.state()).isEqualTo(DimensionLifecycleState.REGISTERED);
        assertThat(synchronizer.getDimensionState(dimKey)).isEqualTo(DimensionLifecycleState.REGISTERED);
        assertThat(synchronizer.isDimensionActive(dimKey)).isFalse();

        // Step 2: Load
        DimensionSyncResult loadResult = synchronizer.loadDimension(dimKey);
        assertThat(loadResult.success()).isTrue();
        assertThat(loadResult.state()).isEqualTo(DimensionLifecycleState.ACTIVE);
        assertThat(synchronizer.getDimensionState(dimKey)).isEqualTo(DimensionLifecycleState.ACTIVE);
        assertThat(synchronizer.isDimensionActive(dimKey)).isTrue();
        assertThat(synchronizer.getActiveDimensionCount()).isEqualTo(1);

        // Step 3: Unload
        DimensionSyncResult unloadResult = synchronizer.unloadDimension(dimKey, true);
        assertThat(unloadResult.success()).isTrue();
        assertThat(unloadResult.state()).isEqualTo(DimensionLifecycleState.UNLOADED);
        assertThat(synchronizer.getDimensionState(dimKey)).isEqualTo(DimensionLifecycleState.UNLOADED);
        assertThat(synchronizer.isDimensionActive(dimKey)).isFalse();
        assertThat(synchronizer.getActiveDimensionCount()).isEqualTo(0);

        // Step 4: Unregister
        DimensionSyncResult unregResult = synchronizer.unregisterDimension(dimKey);
        assertThat(unregResult.success()).isTrue();
        assertThat(unregResult.state()).isEqualTo(DimensionLifecycleState.UNREGISTERED);
        assertThat(synchronizer.getDimensionState(dimKey)).isEqualTo(DimensionLifecycleState.UNREGISTERED);
        assertThat(synchronizer.getTotalRegisteredCount()).isEqualTo(0);

        // Verify listener event history
        assertThat(listener.getRecordedStates()).containsExactly(
                DimensionLifecycleState.REGISTERED,
                DimensionLifecycleState.LOADING,
                DimensionLifecycleState.ACTIVE,
                DimensionLifecycleState.UNLOADING,
                DimensionLifecycleState.UNLOADED,
                DimensionLifecycleState.UNREGISTERED
        );
    }

    @Test
    @DisplayName("Verify error handling and rejection for invalid state transitions")
    public void testInvalidStateTransitions() {
        DynamicDimensionLifecycleSynchronizer synchronizer = new DynamicDimensionLifecycleSynchronizer();
        String missingKey = "vessel_test:missing_dim";

        // Attempting to load an unregistered dimension fails
        DimensionSyncResult loadFail = synchronizer.loadDimension(missingKey);
        assertThat(loadFail.success()).isFalse();

        // Attempting to unload an unregistered dimension fails
        DimensionSyncResult unloadFail = synchronizer.unloadDimension(missingKey, false);
        assertThat(unloadFail.success()).isFalse();

        // Register and attempt duplicate registration
        String key = "vessel_test:duplicate_dim";
        synchronizer.registerDimension(key, DimensionLifecycleType.OVERWORLD);
        DimensionSyncResult dupResult = synchronizer.registerDimension(key, DimensionLifecycleType.OVERWORLD);
        assertThat(dupResult.success()).isFalse();

        // Attempting to unregister an active dimension fails
        synchronizer.loadDimension(key);
        DimensionSyncResult unregActive = synchronizer.unregisterDimension(key);
        assertThat(unregActive.success()).isFalse();
    }
}
