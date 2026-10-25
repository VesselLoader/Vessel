package dev.vessel.kernel.test;

import dev.vessel.kernel.display.model.FrustumCullingVisibilityFilter;
import dev.vessel.kernel.network.CoalescedPacketPayload;
import dev.vessel.kernel.network.PacketCoalescingRingBuffer;
import dev.vessel.kernel.optimization.physics.BvhCollisionConsumer;
import dev.vessel.kernel.optimization.physics.EntityDormancyTracker;
import dev.vessel.kernel.optimization.physics.SpatialBoundingVolumeHierarchy;
import dev.vessel.kernel.scene.ContraptionDeltaRingBuffer;
import dev.vessel.kernel.scene.DynamicContraptionState;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verification test suite for Pass K-19:
 * Physics & Collision Broad-Phase (BVH, Dormancy), Dynamic Scene Delta Ring Buffer,
 * Frustum Culling Filter, and Multi-Channel Packet Coalescing.
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public class DynamicSceneAndPhysicsOptimizationTest {

    // =========================================================================
    // 1. SpatialBoundingVolumeHierarchy Tests
    // =========================================================================

    @Test
    @DisplayName("BVH: Insertion, query, update, and removal with zero-allocation callback")
    void testBvhLifecycleAndQueries() {
        SpatialBoundingVolumeHierarchy bvh = new SpatialBoundingVolumeHierarchy(64);

        // Insert 3 entities
        bvh.insert(101, 0.0f, 0.0f, 0.0f, 1.0f, 2.0f, 1.0f);     // At origin
        bvh.insert(102, 0.5f, 0.0f, 0.5f, 1.5f, 2.0f, 1.5f);     // Overlaps 101
        bvh.insert(103, 50.0f, 0.0f, 50.0f, 51.0f, 2.0f, 51.0f); // Far away

        assertThat(bvh.getLeafCount()).isEqualTo(3);

        // Query overlapping area near origin
        List<Integer> hitEntities = new ArrayList<>();
        int hits = bvh.queryIntersections(
                0.2f, 0.5f, 0.2f, 0.8f, 1.5f, 0.8f,
                (id, minX, minY, minZ, maxX, maxY, maxZ) -> hitEntities.add(id)
        );

        assertThat(hits).isEqualTo(2);
        assertThat(hitEntities).containsExactlyInAnyOrder(101, 102);

        // Query using primitive output array
        int[] outIds = new int[8];
        int arrayHits = bvh.queryIntersections(0.2f, 0.5f, 0.2f, 0.8f, 1.5f, 0.8f, outIds, outIds.length);
        assertThat(arrayHits).isEqualTo(2);
        Set<Integer> arraySet = Set.of(outIds[0], outIds[1]);
        assertThat(arraySet).containsExactlyInAnyOrder(101, 102);

        // Update entity 103 position close to origin
        boolean updated = bvh.update(103, 0.1f, 0.0f, 0.1f, 0.9f, 2.0f, 0.9f);
        assertThat(updated).isTrue();

        hitEntities.clear();
        bvh.queryIntersections(0.2f, 0.5f, 0.2f, 0.8f, 1.5f, 0.8f,
                (id, minX, minY, minZ, maxX, maxY, maxZ) -> hitEntities.add(id));
        assertThat(hitEntities).containsExactlyInAnyOrder(101, 102, 103);

        // Remove entity 102
        boolean removed = bvh.remove(102);
        assertThat(removed).isTrue();
        assertThat(bvh.getLeafCount()).isEqualTo(2);

        hitEntities.clear();
        bvh.queryIntersections(0.2f, 0.5f, 0.2f, 0.8f, 1.5f, 0.8f,
                (id, minX, minY, minZ, maxX, maxY, maxZ) -> hitEntities.add(id));
        assertThat(hitEntities).containsExactlyInAnyOrder(101, 103);

        // Clear BVH
        bvh.clear();
        assertThat(bvh.getLeafCount()).isEqualTo(0);
        assertThat(bvh.getRootNode()).isEqualTo(SpatialBoundingVolumeHierarchy.NULL_NODE);
    }

    @Test
    @DisplayName("BVH: High-density broad-phase stress test matches naive search")
    void testBvhStressAgainstNaive() {
        SpatialBoundingVolumeHierarchy bvh = new SpatialBoundingVolumeHierarchy(512);

        int count = 200;
        float[][] testBoxes = new float[count][6];

        for (int i = 0; i < count; i++) {
            float x = (float) ((i % 20) * 5.0);
            float y = (float) (((i / 20) % 5) * 5.0);
            float z = (float) ((i / 100) * 5.0);
            testBoxes[i][0] = x;
            testBoxes[i][1] = y;
            testBoxes[i][2] = z;
            testBoxes[i][3] = x + 2.0f;
            testBoxes[i][4] = y + 2.0f;
            testBoxes[i][5] = z + 2.0f;

            bvh.insert(i, testBoxes[i][0], testBoxes[i][1], testBoxes[i][2],
                    testBoxes[i][3], testBoxes[i][4], testBoxes[i][5]);
        }

        // Query a window [10, 0, 0] to [22, 10, 10]
        float qMinX = 10.0f, qMinY = 0.0f, qMinZ = 0.0f;
        float qMaxX = 22.0f, qMaxY = 10.0f, qMaxZ = 10.0f;

        // Ground-truth naive search
        Set<Integer> expectedHits = new HashSet<>();
        for (int i = 0; i < count; i++) {
            boolean overlap = testBoxes[i][0] <= qMaxX && testBoxes[i][3] >= qMinX &&
                              testBoxes[i][1] <= qMaxY && testBoxes[i][4] >= qMinY &&
                              testBoxes[i][2] <= qMaxZ && testBoxes[i][5] >= qMinZ;
            if (overlap) {
                expectedHits.add(i);
            }
        }

        Set<Integer> actualHits = new HashSet<>();
        bvh.queryIntersections(qMinX, qMinY, qMinZ, qMaxX, qMaxY, qMaxZ,
                (id, minX, minY, minZ, maxX, maxY, maxZ) -> actualHits.add(id));

        assertThat(actualHits).isEqualTo(expectedHits);
    }

    // =========================================================================
    // 2. EntityDormancyTracker Tests
    // =========================================================================

    @Test
    @DisplayName("EntityDormancyTracker: Stationary entity transitions to dormant and wakes on interaction or radius disturbance")
    void testEntityDormancyTransitions() {
        EntityDormancyTracker tracker = new EntityDormancyTracker(5, 0.01); // 5 ticks idle threshold

        tracker.registerEntity(1, 10.0, 64.0, 10.0);
        tracker.registerEntity(2, 50.0, 64.0, 50.0);

        assertThat(tracker.getTrackedCount()).isEqualTo(2);
        assertThat(tracker.getActiveCount()).isEqualTo(2);
        assertThat(tracker.getDormantCount()).isEqualTo(0);

        // Record 4 ticks stationary for entity 1 -> still active
        for (int i = 1; i <= 4; i++) {
            boolean dormant = tracker.recordTickPosition(1, 10.0, 64.0, 10.0);
            assertThat(dormant).isFalse();
        }
        assertThat(tracker.isDormant(1)).isFalse();

        // 5th tick stationary -> transitions to DORMANT
        boolean dormantOn5 = tracker.recordTickPosition(1, 10.0, 64.0, 10.0);
        assertThat(dormantOn5).isTrue();
        assertThat(tracker.isDormant(1)).isTrue();
        assertThat(tracker.getDormantCount()).isEqualTo(1);
        assertThat(tracker.getActiveCount()).isEqualTo(1);

        // Entity 2 moves continuously -> stays ACTIVE
        for (int i = 1; i <= 10; i++) {
            boolean d2 = tracker.recordTickPosition(2, 50.0 + i, 64.0, 50.0);
            assertThat(d2).isFalse();
        }
        assertThat(tracker.isDormant(2)).isFalse();

        // Direct wake on entity 1
        boolean awoken = tracker.wake(1);
        assertThat(awoken).isTrue();
        assertThat(tracker.isDormant(1)).isFalse();
        assertThat(tracker.getDormantCount()).isEqualTo(0);

        // Make entity 1 dormant again
        for (int i = 1; i <= 5; i++) {
            tracker.recordTickPosition(1, 10.0, 64.0, 10.0);
        }
        assertThat(tracker.isDormant(1)).isTrue();

        // Spatial disturbance: explosion at (12, 64, 10) radius 5.0 -> wakes entity 1
        int awokenRadius = tracker.wakeRadius(12.0, 64.0, 10.0, 5.0);
        assertThat(awokenRadius).isEqualTo(1);
        assertThat(tracker.isDormant(1)).isFalse();

        // Far disturbance: explosion at (0, 0, 0) radius 2.0 -> wakes 0
        for (int i = 1; i <= 5; i++) {
            tracker.recordTickPosition(1, 10.0, 64.0, 10.0);
        }
        assertThat(tracker.isDormant(1)).isTrue();
        int awokenFar = tracker.wakeRadius(0.0, 0.0, 0.0, 2.0);
        assertThat(awokenFar).isEqualTo(0);
        assertThat(tracker.isDormant(1)).isTrue();

        // Unregister entity
        tracker.unregisterEntity(1);
        assertThat(tracker.getTrackedCount()).isEqualTo(1);
        assertThat(tracker.getDormantCount()).isEqualTo(0);
    }

    // =========================================================================
    // 3. ContraptionDeltaRingBuffer Tests
    // =========================================================================

    @Test
    @DisplayName("ContraptionDeltaRingBuffer: Multi-mutation coalescence in same tick and historical interpolation")
    void testContraptionDeltaCoalescenceAndHistory() {
        ContraptionDeltaRingBuffer buffer = new ContraptionDeltaRingBuffer(5001L, 128);
        assertThat(buffer.getCapacity()).isEqualTo(128);

        // Record tick 10 frame
        buffer.recordFrame(10L, 100.0, 64.0, 200.0, 0.0f, 0.0f, 0.0f, 1L, DynamicContraptionState.DIRTY_POS);
        assertThat(buffer.hasFrame(10L)).isTrue();
        assertThat(buffer.getLatestTick()).isEqualTo(10L);

        // Coalesce multiple sub-tick operations within tick 10
        // Operation 2: rotation changed
        buffer.recordFrame(10L, 100.0, 64.0, 200.0, 45.0f, 0.0f, 0.0f, 1L, DynamicContraptionState.DIRTY_ROT);
        // Operation 3: structure changed and position moved slightly
        buffer.recordFrame(10L, 102.0, 64.0, 200.0, 45.0f, 0.0f, 0.0f, 2L, DynamicContraptionState.DIRTY_STRUCTURE);

        DynamicContraptionState frame10 = buffer.getFrameAt(10L);
        assertThat(frame10).isNotNull();
        assertThat(frame10.posX()).isEqualTo(102.0);
        assertThat(frame10.yaw()).isEqualTo(45.0f);
        assertThat(frame10.structureVersion()).isEqualTo(2L);
        // Dirty masks coalesced: POS (1) | ROT (2) | STRUCTURE (4) = 7
        assertThat(frame10.dirtyMask()).isEqualTo(DynamicContraptionState.DIRTY_POS |
                                                 DynamicContraptionState.DIRTY_ROT |
                                                 DynamicContraptionState.DIRTY_STRUCTURE);

        // Record tick 20
        buffer.recordFrame(20L, 112.0, 64.0, 200.0, 65.0f, 0.0f, 0.0f, 2L, DynamicContraptionState.DIRTY_POS);
        assertThat(buffer.getLatestTick()).isEqualTo(20L);

        // Interpolate between tick 10 and 20 at alpha = 0.5 (midpoint)
        DynamicContraptionState interpolated = buffer.interpolate(10L, 20L, 0.5f);
        assertThat(interpolated.posX()).isEqualTo(107.0); // 102 + (112 - 102) * 0.5
        assertThat(interpolated.yaw()).isEqualTo(55.0f); // 45 + (65 - 45) * 0.5

        // Packed delta check
        long delta64 = buffer.getPackedDelta(20L);
        assertThat(delta64).isNotZero();

        // Test circular buffer sliding
        for (long t = 21L; t <= 160L; t++) {
            buffer.recordFrame(t, t, 64.0, 0.0, 0.0f, 0.0f, 0.0f, 1L, 0);
        }
        assertThat(buffer.getLatestTick()).isEqualTo(160L);
        // Tick 10 is older than 160 - 128 = 32, so it has fallen out of the ring window
        assertThat(buffer.hasFrame(10L)).isFalse();
        assertThat(buffer.hasFrame(160L)).isTrue();
        assertThat(buffer.hasFrame(35L)).isTrue();
    }

    // =========================================================================
    // 4. FrustumCullingVisibilityFilter Tests
    // =========================================================================

    @Test
    @DisplayName("FrustumCullingVisibilityFilter: Accurately culls out-of-FOV & behind objects while preserving near-field")
    void testFrustumCullingMath() {
        FrustumCullingVisibilityFilter filter = new FrustumCullingVisibilityFilter(2.0); // 2 blocks near proximity

        // Player looking towards +Z (yaw = 0, pitch = 0)
        double px = 0.0, py = 64.0, pz = 0.0;
        float yaw = 0.0f, pitch = 0.0f;
        float fov = 70.0f;
        double maxDist = 64.0;

        // 1. Directly in front (+Z) at distance 20 -> VISIBLE
        boolean frontVisible = filter.isVisible(px, py, pz, yaw, pitch, 0.0, 64.0, 20.0, 1.0, maxDist, fov);
        assertThat(frontVisible).isTrue();

        // 2. Directly behind (-Z) at distance 20 -> CULLED
        boolean behindVisible = filter.isVisible(px, py, pz, yaw, pitch, 0.0, 64.0, -20.0, 1.0, maxDist, fov);
        assertThat(behindVisible).isFalse();

        // 3. Immediately behind player within near proximity (pz = -1.0) -> VISIBLE (proximity override)
        boolean nearBehindVisible = filter.isVisible(px, py, pz, yaw, pitch, 0.0, 64.0, -1.0, 0.5, maxDist, fov);
        assertThat(nearBehindVisible).isTrue();

        // 4. Far beyond render distance (pz = 150.0) -> CULLED
        boolean farVisible = filter.isVisible(px, py, pz, yaw, pitch, 0.0, 64.0, 150.0, 1.0, maxDist, fov);
        assertThat(farVisible).isFalse();

        // 5. Batch filter verification
        double[] coords = new double[]{
                0.0, 64.0, 20.0,   // index 0: visible front
                0.0, 64.0, -20.0,  // index 1: behind (culled)
                0.0, 64.0, 150.0,  // index 2: far (culled)
                0.0, 64.0, -1.0    // index 3: near behind (visible)
        };
        double[] radii = new double[]{1.0, 1.0, 1.0, 0.5};
        int[] visibleOut = new int[4];

        int visibleCount = filter.batchFilter(px, py, pz, yaw, pitch, maxDist, fov, coords, radii, 4, visibleOut);
        assertThat(visibleCount).isEqualTo(2);
        assertThat(visibleOut[0]).isEqualTo(0);
        assertThat(visibleOut[1]).isEqualTo(3);
    }

    // =========================================================================
    // 5. PacketCoalescingRingBuffer Tests
    // =========================================================================

    @Test
    @DisplayName("PacketCoalescingRingBuffer: Multi-channel packet batching and flush aggregation")
    void testPacketCoalescingAndFlush() {
        PacketCoalescingRingBuffer ringBuffer = new PacketCoalescingRingBuffer(4, 1024);

        assertThat(ringBuffer.hasPendingPackets()).isFalse();

        // Enqueue movement packets (Channel 0)
        byte[] move1 = new byte[]{0x01, 0x02, 0x03};
        byte[] move2 = new byte[]{0x04, 0x05};
        ringBuffer.enqueuePacket(PacketCoalescingRingBuffer.CHANNEL_ENTITY_MOVEMENT, move1);
        ringBuffer.enqueuePacket(PacketCoalescingRingBuffer.CHANNEL_ENTITY_MOVEMENT, move2);

        // Enqueue particle packets (Channel 1)
        byte[] particle1 = new byte[]{(byte) 0xAA, (byte) 0xBB};
        ringBuffer.enqueuePacket(PacketCoalescingRingBuffer.CHANNEL_PARTICLES, particle1);

        assertThat(ringBuffer.getPendingPacketCount(PacketCoalescingRingBuffer.CHANNEL_ENTITY_MOVEMENT)).isEqualTo(2);
        assertThat(ringBuffer.getPendingBytes(PacketCoalescingRingBuffer.CHANNEL_ENTITY_MOVEMENT)).isEqualTo(5);
        assertThat(ringBuffer.getPendingPacketCount(PacketCoalescingRingBuffer.CHANNEL_PARTICLES)).isEqualTo(1);
        assertThat(ringBuffer.getPendingBytes(PacketCoalescingRingBuffer.CHANNEL_PARTICLES)).isEqualTo(2);
        assertThat(ringBuffer.getTotalPendingPackets()).isEqualTo(3);
        assertThat(ringBuffer.getTotalPendingBytes()).isEqualTo(7);

        // Flush Channel 0
        CoalescedPacketPayload payload0 = ringBuffer.flushChannel(PacketCoalescingRingBuffer.CHANNEL_ENTITY_MOVEMENT);
        assertThat(payload0.channel()).isEqualTo(PacketCoalescingRingBuffer.CHANNEL_ENTITY_MOVEMENT);
        assertThat(payload0.packetCount()).isEqualTo(2);
        assertThat(payload0.byteLength()).isEqualTo(5);
        assertThat(payload0.data()).containsExactly(0x01, 0x02, 0x03, 0x04, 0x05);

        // Channel 0 is now empty, Channel 1 still has pending
        assertThat(ringBuffer.hasPendingPackets(PacketCoalescingRingBuffer.CHANNEL_ENTITY_MOVEMENT)).isFalse();
        assertThat(ringBuffer.hasPendingPackets(PacketCoalescingRingBuffer.CHANNEL_PARTICLES)).isTrue();

        // Flush all remaining
        CoalescedPacketPayload[] allPayloads = ringBuffer.flushAll();
        assertThat(allPayloads).hasSize(1);
        assertThat(allPayloads[0].channel()).isEqualTo(PacketCoalescingRingBuffer.CHANNEL_PARTICLES);
        assertThat(allPayloads[0].packetCount()).isEqualTo(1);
        assertThat(allPayloads[0].data()).containsExactly((byte) 0xAA, (byte) 0xBB);

        assertThat(ringBuffer.hasPendingPackets()).isFalse();
    }

    @Test
    @DisplayName("PacketCoalescingRingBuffer: Thread-safe high-throughput concurrent enqueue")
    void testConcurrentEnqueueThroughput() throws InterruptedException {
        PacketCoalescingRingBuffer ringBuffer = new PacketCoalescingRingBuffer(4, 2048);
        int threadCount = 4;
        int packetsPerThread = 500;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        byte[] samplePacket = new byte[]{1, 2, 3, 4, 5, 6, 7, 8};

        for (int t = 0; t < threadCount; t++) {
            final int channel = t % 4;
            executor.submit(() -> {
                try {
                    for (int i = 0; i < packetsPerThread; i++) {
                        ringBuffer.enqueuePacket(channel, samplePacket);
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        boolean finished = latch.await(5, TimeUnit.SECONDS);
        executor.shutdown();

        assertThat(finished).isTrue();
        assertThat(ringBuffer.getTotalPendingPackets()).isEqualTo(threadCount * packetsPerThread);
        assertThat(ringBuffer.getTotalPendingBytes()).isEqualTo(threadCount * packetsPerThread * samplePacket.length);

        CoalescedPacketPayload[] flushed = ringBuffer.flushAll();
        assertThat(flushed).hasSize(4);
        assertThat(ringBuffer.hasPendingPackets()).isFalse();
    }
}
