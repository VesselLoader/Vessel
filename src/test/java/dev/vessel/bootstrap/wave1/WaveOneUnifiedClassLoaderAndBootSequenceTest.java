package dev.vessel.bootstrap.wave1;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.vessel.bootstrap.classloading.BootPhaseLifecycleCoordinator;
import dev.vessel.bootstrap.classloading.UnifiedGameAndModClassLoaderBridge;
import dev.vessel.bootstrap.mojang.TierOneGameTransformingClassLoader;
import java.net.URL;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Integration tests for Wave 1 ({@code ADR-0001}: {@code VES-CLASS-001} &amp; {@code VES-BOOT-002})
 * verifying bidirectional class resolution between {@link TierOneGameTransformingClassLoader} and
 * the mod classloader via {@link UnifiedGameAndModClassLoaderBridge} (including thread-local
 * reentrancy protection) and deterministic boot phase transitions in
 * {@link BootPhaseLifecycleCoordinator}.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
final class WaveOneUnifiedClassLoaderAndBootSequenceTest {

    @Test
    void testBidirectionalClassLoaderBridgeResolutionAndReentrancyProtection() throws Exception {
        UnifiedGameAndModClassLoaderBridge bridge = new UnifiedGameAndModClassLoaderBridge();
        assertFalse(bridge.isFullyBridged());

        try (TierOneGameTransformingClassLoader tierOneLoader =
                new TierOneGameTransformingClassLoader(
                        "wave1-tier1-game",
                        new URL[0],
                        ClassLoader.getPlatformClassLoader())) {
            tierOneLoader.setUnifiedClassLoaderBridge(bridge);
            assertSame(bridge, tierOneLoader.getUnifiedClassLoaderBridge());

            ClassLoader appModLoader =
                    WaveOneUnifiedClassLoaderAndBootSequenceTest.class.getClassLoader();
            bridge.bindTierOneGameClassLoader(tierOneLoader);
            bridge.bindTierTwoModClassLoader(appModLoader);
            assertTrue(bridge.isFullyBridged());

            // TierOneGameTransformingClassLoader has only PlatformClassLoader as parent and 0 URLs,
            // so resolving SampleForgeWaveOneModFixture must succeed via UnifiedGameAndModClassLoaderBridge.
            Class<?> resolvedModFixture =
                    tierOneLoader.loadClass(SampleForgeWaveOneModFixture.class.getName());
            assertSame(SampleForgeWaveOneModFixture.class, resolvedModFixture);

            Class<?> resolvedViaGameBridge =
                    bridge.tryLoadFromGameClassLoader(SampleForgeWaveOneModFixture.class.getName());
            assertSame(SampleForgeWaveOneModFixture.class, resolvedViaGameBridge);

            // Verify reentrancy protection when two bridged TierOne loaders delegate to each other
            try (TierOneGameTransformingClassLoader secondTierLoader =
                    new TierOneGameTransformingClassLoader(
                            "wave1-tier2-cycle-probe",
                            new URL[0],
                            ClassLoader.getPlatformClassLoader())) {
                secondTierLoader.setUnifiedClassLoaderBridge(bridge);
                bridge.bindTierTwoModClassLoader(secondTierLoader);

                assertNull(bridge.tryLoadFromGameClassLoader("non.existent.CyclicMissingClass"));
                assertNull(bridge.tryLoadFromModClassLoader("non.existent.CyclicMissingClass"));
            }
        }
    }

    @Test
    void testBootPhaseLifecycleCoordinatorOrderedTransitionsAndReset() {
        BootPhaseLifecycleCoordinator coordinator = new BootPhaseLifecycleCoordinator();
        assertEquals(BootPhaseLifecycleCoordinator.PHASE_UNINITIALIZED, coordinator.getCurrentPhase());
        assertTrue(coordinator.getPhaseHistory().isEmpty());

        coordinator.transitionToPhase(BootPhaseLifecycleCoordinator.PHASE_PRE_MAIN_MOD_INIT);
        coordinator.transitionToPhase(BootPhaseLifecycleCoordinator.PHASE_VANILLA_BOOTSTRAP_ACTIVE);
        coordinator.transitionToPhase(BootPhaseLifecycleCoordinator.PHASE_SERVER_STARTING);
        coordinator.transitionToPhase(BootPhaseLifecycleCoordinator.PHASE_SERVER_STARTED_POST_WORLD);

        assertEquals(
                BootPhaseLifecycleCoordinator.PHASE_SERVER_STARTED_POST_WORLD,
                coordinator.getCurrentPhase());
        assertEquals(
                List.of(
                        BootPhaseLifecycleCoordinator.PHASE_PRE_MAIN_MOD_INIT,
                        BootPhaseLifecycleCoordinator.PHASE_VANILLA_BOOTSTRAP_ACTIVE,
                        BootPhaseLifecycleCoordinator.PHASE_SERVER_STARTING,
                        BootPhaseLifecycleCoordinator.PHASE_SERVER_STARTED_POST_WORLD),
                coordinator.getPhaseHistory());

        assertTrue(
                coordinator.hasCompletedPhase(
                        BootPhaseLifecycleCoordinator.PHASE_PRE_MAIN_MOD_INIT));
        assertTrue(
                coordinator.hasCompletedPhase(
                        BootPhaseLifecycleCoordinator.PHASE_VANILLA_BOOTSTRAP_ACTIVE));
        assertTrue(
                coordinator.hasCompletedPhase(
                        BootPhaseLifecycleCoordinator.PHASE_SERVER_STARTING));
        assertTrue(
                coordinator.hasCompletedPhase(
                        BootPhaseLifecycleCoordinator.PHASE_SERVER_STARTED_POST_WORLD));
        assertFalse(
                coordinator.hasCompletedPhase(
                        BootPhaseLifecycleCoordinator.PHASE_SERVER_STOPPING));

        coordinator.transitionToPhase(BootPhaseLifecycleCoordinator.PHASE_SERVER_STOPPING);
        assertTrue(
                coordinator.hasCompletedPhase(
                        BootPhaseLifecycleCoordinator.PHASE_SERVER_STOPPING));

        coordinator.reset();
        assertEquals(BootPhaseLifecycleCoordinator.PHASE_UNINITIALIZED, coordinator.getCurrentPhase());
        assertTrue(coordinator.getPhaseHistory().isEmpty());
    }
}
