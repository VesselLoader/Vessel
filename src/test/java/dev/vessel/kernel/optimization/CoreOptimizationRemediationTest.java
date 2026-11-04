package dev.vessel.kernel.optimization;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.vessel.kernel.optimization.block.BreadthFirstFluidSimulator;
import dev.vessel.kernel.optimization.block.FluidFlowAction;
import dev.vessel.kernel.optimization.block.FluidSimulationType;
import dev.vessel.kernel.optimization.chunk.LinearRegionFileFastPath;
import dev.vessel.kernel.optimization.chunk.RegionCompressionFormat;
import dev.vessel.kernel.optimization.entity.AsyncPathfindingDispatcher;
import dev.vessel.kernel.optimization.entity.PathfindingGoal;
import dev.vessel.kernel.optimization.lighting.ChunkSectionLightData;
import dev.vessel.kernel.optimization.lighting.LightPropagationType;
import dev.vessel.kernel.optimization.lighting.StarlightZeroCopyLightEngine;
import dev.vessel.kernel.optimization.physics.EntityDormancyTracker;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

/**
 * Unit verification tests covering concurrency, boundary coordinates, and two-pass lighting
 * across the remediated optimization engines.
 *
 * @version 1.0.0
 */
class CoreOptimizationRemediationTest {

    @Test
    void testFluidSimulatorReachesDeepslateNegativeY() {
        BreadthFirstFluidSimulator simulator = new BreadthFirstFluidSimulator(FluidSimulationType.WATER, -64);
        List<FluidFlowAction> actions = simulator.simulateSpread(
                0, 5, 0, true, (x, y, z) -> y >= -64);
        assertFalse(actions.isEmpty());
        // Lowest action should reach down to -64
        int lowestY = actions.stream().mapToInt(FluidFlowAction::y).min().orElse(0);
        assertEquals(-64, lowestY);
    }

    @Test
    void testStarlightTwoPassLightRemovalRetainsAdjacentLight() {
        StarlightZeroCopyLightEngine engine = new StarlightZeroCopyLightEngine(LightPropagationType.BLOCK);
        ChunkSectionLightData data = new ChunkSectionLightData();

        // Place Torch 1 at (2, 8, 8) with level 14
        engine.propagate(data, 2, 8, 8, 14);
        // Place Torch 2 at (4, 8, 8) with level 14
        engine.propagate(data, 4, 8, 8, 14);

        assertEquals(14, data.get(2, 8, 8));
        assertEquals(14, data.get(4, 8, 8));
        assertEquals(13, data.get(3, 8, 8));

        // Now remove Torch 1 at (2, 8, 8)
        engine.removeLight(data, 2, 8, 8);

        // Block (2, 8, 8) is illuminated by surviving Torch 2 at distance 2 (14 - 2 = 12)!
        assertEquals(12, data.get(2, 8, 8));
        // Block (3, 8, 8) and (4, 8, 8) must retain illumination from Torch 2!
        assertEquals(14, data.get(4, 8, 8));
        assertEquals(13, data.get(3, 8, 8));

        // Solitary torch test: Place at (12, 12, 12) with no neighbors
        engine.propagate(data, 12, 12, 12, 14);
        assertEquals(14, data.get(12, 12, 12));
        engine.removeLight(data, 12, 12, 12);
        assertEquals(0, data.get(12, 12, 12));
    }

    @Test
    void testAsyncPathfindingDispatcherDoesNotMisCancelSuccessorRequest() throws Exception {
        AsyncPathfindingDispatcher dispatcher = new AsyncPathfindingDispatcher(2, 1024);
        AtomicBoolean req1Success = new AtomicBoolean(false);
        AtomicBoolean req1Cancelled = new AtomicBoolean(false);
        AtomicBoolean req2Success = new AtomicBoolean(false);
        AtomicBoolean req2Cancelled = new AtomicBoolean(false);
        CountDownLatch latch = new CountDownLatch(1);

        int entityId = 999;
        PathfindingGoal goal1 = new PathfindingGoal(entityId, 0, 64, 0, 10, 64, 10, 32.0, 1.0f);
        PathfindingGoal goal2 = new PathfindingGoal(entityId, 0, 64, 0, 20, 64, 20, 32.0, 1.0f);

        // Submit Request 1 immediately followed by Request 2
        dispatcher.submitPathRequest(goal1, res -> {
            if (res.isSuccessful()) {
                req1Success.set(true);
            } else if (res.isCancelled()) {
                req1Cancelled.set(true);
            }
        });

        dispatcher.submitPathRequest(goal2, res -> {
            if (res.isSuccessful()) {
                req2Success.set(true);
            } else if (res.isCancelled()) {
                req2Cancelled.set(true);
            }
            latch.countDown();
        });

        assertTrue(latch.await(3, TimeUnit.SECONDS));
        assertTrue(req2Success.get(), "Request 2 should succeed and not be falsely cancelled");
        assertFalse(req2Cancelled.get(), "Request 2 must not be cancelled");
    }

    @Test
    void testLinearRegionFileConcurrentReads() throws Exception {
        LinearRegionFileFastPath fastPath = new LinearRegionFileFastPath(0, 0, 65536);
        byte[] payload1 = "ChunkPayload_0_0_TestData".getBytes();
        byte[] payload2 = "ChunkPayload_1_1_TestData".getBytes();

        fastPath.writeChunk(0, 0, payload1, RegionCompressionFormat.DEFLATE);
        fastPath.writeChunk(1, 1, payload2, RegionCompressionFormat.DEFLATE);

        int readerThreads = 8;
        ExecutorService executor = Executors.newFixedThreadPool(readerThreads);
        CountDownLatch latch = new CountDownLatch(readerThreads);
        AtomicInteger failures = new AtomicInteger(0);

        for (int i = 0; i < readerThreads; i++) {
            final int threadIdx = i;
            executor.submit(() -> {
                try {
                    for (int iter = 0; iter < 50; iter++) {
                        int cx = threadIdx % 2;
                        byte[] read = fastPath.readChunk(cx, cx);
                        if (read == null || read.length == 0) {
                            failures.incrementAndGet();
                        }
                    }
                } catch (IOException e) {
                    failures.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        assertTrue(latch.await(5, TimeUnit.SECONDS));
        executor.shutdown();
        assertEquals(0, failures.get(), "No concurrent read failures or position collisions should occur");
        fastPath.close();
    }

    @Test
    void testEntityDormancyTrackerConcurrentWake() throws Exception {
        EntityDormancyTracker tracker = new EntityDormancyTracker(1, 0.001);
        tracker.registerEntity(10, 0.0, 64.0, 0.0);
        tracker.recordTickPosition(10, 0.0, 64.0, 0.0);
        assertTrue(tracker.recordTickPosition(10, 0.0, 64.0, 0.0));
        assertEquals(1, tracker.getDormantEntityCount());

        int threads = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch latch = new CountDownLatch(threads);
        AtomicInteger awakeReports = new AtomicInteger(0);

        for (int i = 0; i < threads; i++) {
            executor.submit(() -> {
                try {
                    if (tracker.wake(10)) {
                        awakeReports.incrementAndGet();
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        assertTrue(latch.await(3, TimeUnit.SECONDS));
        executor.shutdown();
        assertEquals(1, awakeReports.get(), "Exactly one thread should successfully transition from dormant to awake");
        assertEquals(0, tracker.getDormantEntityCount(), "Dormant count must be zero and never negative");
    }
}
