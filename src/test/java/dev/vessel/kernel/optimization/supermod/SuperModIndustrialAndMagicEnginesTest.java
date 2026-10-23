package dev.vessel.kernel.optimization.supermod;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for Sub-Stage 6.11: all 10 super-mod industrial, magic, backpack, and Botania
 * optimization engines in {@code dev.vessel.kernel.optimization.supermod} (Part 1).
 *
 * @version 1.0.0
 */
class SuperModIndustrialAndMagicEnginesTest {

    @Test
    void testKineticGraphBeltCoalescerMultiblockSuppressorAndStorageDebounce() {
        KineticNetworkGraphCache kineticCache = new KineticNetworkGraphCache();
        kineticCache.cacheNetworkSpeedMultiplier(10L, 500L, 2.5f);
        assertEquals(80.0f, kineticCache.computePropagatedRpmO1(10L, 500L, 32.0f), 1e-4f);
        kineticCache.invalidateKineticNetwork(10L);
        assertEquals(0.0f, kineticCache.computePropagatedRpmO1(10L, 500L, 32.0f), 1e-4f);

        BeltItemCoalescingOptimizer beltOptimizer = new BeltItemCoalescingOptimizer();
        assertEquals(16, beltOptimizer.coalesceItemsOnBelt(700L, "create:crushed_raw_iron", 16));
        assertEquals(48, beltOptimizer.coalesceItemsOnBelt(700L, "create:crushed_raw_iron", 32));
        assertEquals(5, beltOptimizer.advanceBeltSegmentBatch(700L, 5));
        assertEquals(9, beltOptimizer.advanceBeltSegmentBatch(700L, 4));

        MultiblockSlaveTickSupressor multiblockSupressor = new MultiblockSlaveTickSupressor();
        multiblockSupressor.registerMultiblockStructure(1000L, List.of(1001L, 1002L, 1003L));
        assertTrue(multiblockSupressor.shouldTickMultiblockPart(1000L));
        assertFalse(multiblockSupressor.shouldTickMultiblockPart(1001L));
        assertFalse(multiblockSupressor.shouldTickMultiblockPart(1003L));
        assertTrue(multiblockSupressor.shouldTickMultiblockPart(9999L));

        StorageNetworkDebounceCoordinator debounceCoordinator =
                new StorageNetworkDebounceCoordinator();
        AtomicInteger rebuildCount = new AtomicInteger(0);
        debounceCoordinator.scheduleGridTopologyRebuild(42L, 1000L, rebuildCount::incrementAndGet);
        debounceCoordinator.scheduleGridTopologyRebuild(42L, 1020L, rebuildCount::incrementAndGet);
        assertEquals(0, debounceCoordinator.flushDebouncedGridRebuilds(1050L, 50L));
        assertEquals(1, debounceCoordinator.flushDebouncedGridRebuilds(1080L, 50L));
        assertEquals(1, rebuildCount.get());
    }

    @Test
    void testExplosionSlicerMagicProjectileSimdAiLodAndThermalThrottle() {
        ExplosionRaycastSlicer explosionSlicer = new ExplosionRaycastSlicer();
        explosionSlicer.enqueueMassiveExplosion(99L, List.of(1L, 2L, 3L, 4L, 5L), 2);
        assertEquals(List.of(1L, 2L), explosionSlicer.processNextExplosionSlice(99L));
        assertEquals(List.of(3L, 4L), explosionSlicer.processNextExplosionSlice(99L));
        assertEquals(List.of(5L), explosionSlicer.processNextExplosionSlice(99L));
        assertTrue(explosionSlicer.processNextExplosionSlice(99L).isEmpty());

        MagicProjectileRaycastOptimizer magicOptimizer = new MagicProjectileRaycastOptimizer();
        float[] positions = magicOptimizer.batchComputeProjectilePositions(
                new float[]{0.0f, 10.0f},
                new float[]{64.0f, 70.0f},
                new float[]{0.0f, -5.0f},
                new float[]{1.5f, -2.0f},
                new float[]{0.5f, 0.0f},
                new float[]{2.0f, 1.0f},
                2.0f);
        assertArrayEquals(
                new float[]{3.0f, 65.0f, 4.0f, 6.0f, 70.0f, -3.0f},
                positions,
                1e-4f);

        ModdedEntityAiLodManager lodManager = new ModdedEntityAiLodManager();
        assertTrue(lodManager.shouldTickModdedEntityAi(100.0, 1L, 16.0, 48.0, true));
        assertFalse(lodManager.shouldTickModdedEntityAi(900.0, 1L, 16.0, 48.0, true));
        assertTrue(lodManager.shouldTickModdedEntityAi(900.0, 4L, 16.0, 48.0, true));
        assertFalse(lodManager.shouldTickModdedEntityAi(5000.0, 20L, 16.0, 48.0, true));
        assertTrue(lodManager.shouldTickModdedEntityAi(5000.0, 20L, 16.0, 48.0, false));

        HeatPressureSimulationThrottle heatThrottle = new HeatPressureSimulationThrottle();
        float updatedTemp = heatThrottle.computeThrottledThermalTransfer(
                300L, 100.0f, 200.0f, 0.05f, 5L, 5);
        assertEquals(125.0f, updatedTemp, 1e-4f);
        float cachedTemp = heatThrottle.computeThrottledThermalTransfer(
                300L, 125.0f, 200.0f, 0.05f, 6L, 5);
        assertEquals(125.0f, cachedTemp, 1e-4f);
    }

    @Test
    void testNestedBackpackCuriosGovernorAndManaBurstCorporeaCoalescer() {
        NestedBackpackAndCuriosTickGovernor backpackGov =
                new NestedBackpackAndCuriosTickGovernor();
        UUID playerUuid = UUID.randomUUID();
        assertTrue(backpackGov.coalescePlayerMagnetUpgradeScan(playerUuid, 8.0, 100L));
        assertFalse(backpackGov.coalescePlayerMagnetUpgradeScan(playerUuid, 8.0, 103L));
        assertTrue(backpackGov.coalescePlayerMagnetUpgradeScan(playerUuid, 8.0, 105L));

        assertFalse(backpackGov.suppressNestedContainerRecursiveTick(
                1, "sophisticatedbackpacks:netherite_backpack"));
        assertTrue(backpackGov.suppressNestedContainerRecursiveTick(
                2, "sophisticatedbackpacks:netherite_backpack"));

        assertTrue(backpackGov.shouldEvaluateInventoryUpgrade(playerUuid, 0, 10L, 9L));
        assertFalse(backpackGov.shouldEvaluateInventoryUpgrade(playerUuid, 0, 10L, 10L));

        ManaBurstAndCorporeaNetworkCoalescer botaniaCoalescer =
                new ManaBurstAndCorporeaNetworkCoalescer();
        assertTrue(botaniaCoalescer.canConvertManaBurstToVirtualTransfer(10L, 20L, false, true));
        assertFalse(botaniaCoalescer.canConvertManaBurstToVirtualTransfer(10L, 20L, true, true));
        botaniaCoalescer.scheduleVirtualManaTransfer(10L, 20L, 160, 40, 100L);

        botaniaCoalescer.indexCorporeaNetworkInventory(7L, 801L, "botania:manasteel_ingot", 64);
        botaniaCoalescer.indexCorporeaNetworkInventory(7L, 802L, "botania:manasteel_ingot", 128);
        assertEquals(192, botaniaCoalescer.queryCorporeaIndexedCount(7L, "botania:manasteel_ingot"));
        botaniaCoalescer.indexCorporeaNetworkInventory(7L, 801L, "botania:manasteel_ingot", 32);
        assertEquals(160, botaniaCoalescer.queryCorporeaIndexedCount(7L, "botania:manasteel_ingot"));
    }
}
