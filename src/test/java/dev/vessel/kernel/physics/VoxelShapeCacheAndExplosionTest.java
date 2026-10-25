package dev.vessel.kernel.physics;

import dev.vessel.kernel.optimization.physics.ExplosionBatchMergeResult;
import dev.vessel.kernel.optimization.physics.PooledVoxelAabb;
import dev.vessel.kernel.optimization.physics.RaytraceDdaResult;
import dev.vessel.kernel.optimization.physics.RaytracingDdaEarlyExitOptimizer;
import dev.vessel.kernel.optimization.physics.UnbatchedExplosionBlockUpdateMerger;
import dev.vessel.kernel.optimization.physics.VoxelAabbPoolStats;
import dev.vessel.kernel.optimization.physics.VoxelShapeAabbCachePool;
import dev.vessel.kernel.scene.CreateContraptionBatchPacket;
import dev.vessel.kernel.scene.CreateContraptionDeltaRingBuffer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit test suite verifying AABB pooling, explosion batch merging, early-exit DDA ray-tracing,
 * and Create contraption 128-tick delta coalescing.
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public final class VoxelShapeCacheAndExplosionTest {

    @Test
    @DisplayName("Verify VoxelShapeAabbCachePool recycling and zero-allocation semantics")
    public void testAabbCachePool() {
        VoxelShapeAabbCachePool pool = new VoxelShapeAabbCachePool(64);
        pool.prime(10);
        assertThat(pool.getAvailableCount()).isEqualTo(10);

        try (PooledVoxelAabb box1 = pool.acquire(0.0, 0.0, 0.0, 1.0, 1.0, 1.0)) {
            assertThat(box1.isInUse()).isTrue();
            assertThat(box1.contains(0.5, 0.5, 0.5)).isTrue();
            assertThat(box1.contains(1.5, 0.5, 0.5)).isFalse();
            assertThat(box1.intersects(0.5, 0.5, 0.5, 1.5, 1.5, 1.5)).isTrue();
            assertThat(box1.intersects(2.0, 2.0, 2.0, 3.0, 3.0, 3.0)).isFalse();
            assertThat(pool.getAvailableCount()).isEqualTo(9);
        }

        // After close, box1 should be returned to pool
        assertThat(pool.getAvailableCount()).isEqualTo(10);
        VoxelAabbPoolStats stats = pool.getStats();
        assertThat(stats.totalAcquired()).isEqualTo(1L);
        assertThat(stats.totalReleased()).isEqualTo(1L);
    }

    @Test
    @DisplayName("Verify UnbatchedExplosionBlockUpdateMerger deduplication and unified section relighting")
    public void testExplosionBlockUpdateMerger() {
        UnbatchedExplosionBlockUpdateMerger merger = new UnbatchedExplosionBlockUpdateMerger();
        TestExplosionBlockCollector blockCollector = new TestExplosionBlockCollector();
        TestExplosionLightCollector lightCollector = new TestExplosionLightCollector();

        // Record 10 block hits in Section (0, 4, 0) - coords [0..15, 64..79, 0..15]
        for (int i = 0; i < 10; i++) {
            merger.recordExplodedBlock(i, 64, 0, 0); // Air
        }

        // Duplicate hits at (0, 64, 0) and (1, 64, 0)
        merger.recordExplodedBlock(0, 64, 0, 0);
        merger.recordExplodedBlock(1, 64, 0, 0);

        // Record 5 block hits in adjacent Section (1, 4, 0) - coords [16..31, 64..79, 0..15]
        for (int i = 16; i < 21; i++) {
            merger.recordExplodedBlock(i, 64, 0, 0);
        }

        assertThat(merger.getPendingCount()).isEqualTo(15); // 10 + 5 (duplicates ignored)
        assertThat(merger.getAffectedSectionCount()).isEqualTo(2);

        ExplosionBatchMergeResult result = merger.commitBatch(blockCollector, lightCollector);

        assertThat(result.totalBlocksProcessed()).isEqualTo(15);
        assertThat(result.affectedSectionsCount()).isEqualTo(2);
        assertThat(result.lightUpdatesDispatched()).isEqualTo(2);
        assertThat(blockCollector.getCount()).isEqualTo(15);
        assertThat(lightCollector.getCount()).isEqualTo(2);
        assertThat(merger.getPendingCount()).isEqualTo(0);
    }

    @Test
    @DisplayName("Verify RaytracingDdaEarlyExitOptimizer line-of-sight and early-exit detection")
    public void testDdaEarlyExit() {
        RaytracingDdaEarlyExitOptimizer optimizer = new RaytracingDdaEarlyExitOptimizer();
        TestVoxelSolidMap world = new TestVoxelSolidMap();

        // Place a solid wall at X=5, Y=64, Z=0
        world.setSolid(5, 64, 0);

        // Test 1: Obstructed line of sight from (0.5, 64.5, 0.5) to (10.5, 64.5, 0.5)
        boolean hasLos = optimizer.hasLineOfSight(0.5, 64.5, 0.5, 10.5, 64.5, 0.5, world);
        assertThat(hasLos).isFalse();

        RaytraceDdaResult hit = optimizer.traceWithEarlyExit(0.5, 64.5, 0.5, 1.0, 0.0, 0.0, 10.0, world);
        assertThat(hit.hasHit()).isTrue();
        assertThat(hit.hitX()).isEqualTo(5);
        assertThat(hit.hitY()).isEqualTo(64);
        assertThat(hit.hitZ()).isEqualTo(0);

        // Test 2: Clear line of sight along unobstructed parallel path Y=65
        boolean clearLos = optimizer.hasLineOfSight(0.5, 65.5, 0.5, 10.5, 65.5, 0.5, world);
        assertThat(clearLos).isTrue();

        assertThat(optimizer.getEarlyExitCount()).isGreaterThan(0L);
    }

    @Test
    @DisplayName("Verify CreateContraptionDeltaRingBuffer 128-tick coalescing and batch packet generation")
    public void testCreateContraptionDeltaRingBuffer() {
        long contraptionId = 42L;
        CreateContraptionDeltaRingBuffer buffer = new CreateContraptionDeltaRingBuffer(contraptionId, 128);

        // Record 128 movement ticks moving along X axis: tick 0 -> posX 0.0 to tick 127 -> posX 127.0
        for (long t = 0; t < 128; t++) {
            buffer.recordFrame(t, t * 1.0, 100.0, 200.0, (float) t, 0.0f, 0.0f, 1L, 1);
        }

        CreateContraptionBatchPacket batch = buffer.buildConsolidatedBatch(0L, 127L);

        assertThat(batch.contraptionId()).isEqualTo(contraptionId);
        assertThat(batch.startTick()).isEqualTo(0L);
        assertThat(batch.endTick()).isEqualTo(127L);
        assertThat(batch.frameCount()).isEqualTo(128);
        assertThat(batch.deltaX()).isEqualTo(127.0);
        assertThat(batch.deltaY()).isEqualTo(0.0);
        assertThat(batch.deltaZ()).isEqualTo(0.0);
        assertThat(batch.deltaYaw()).isEqualTo(127.0f);
        assertThat(batch.combinedDirtyMask()).isEqualTo(1);
        assertThat(batch.serializedPayload()).hasSize(CreateContraptionDeltaRingBuffer.PACKET_HEADER_SIZE);
    }
}
