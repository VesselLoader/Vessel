package dev.vessel.kernel.world.worldgen;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link CascadingWorldGenThrottler} and {@link PhantomChunkReadOnlyView}.
 * <p>
 * Validates cascading recursion depth enforcement, deadlock cycle rejection,
 * and immutable synthetic terrain view behavior.
 * </p>
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public class CascadingWorldGenThrottlerTest {

    @Test
    public void testThrottlerDepthLimitEnforcement() {
        CascadingWorldGenThrottler throttler = new CascadingWorldGenThrottler(2, 10);
        CascadingThreadDepthTracker.current().clear();

        long chunk1 = 100L;
        long chunk2 = 101L;
        long chunk3 = 102L;

        GenerationPermit permit1 = throttler.acquirePermit(chunk1);
        assertThat(permit1.status()).isEqualTo(CascadingPermitStatus.ACQUIRED);
        assertThat(permit1.cascadingDepth()).isEqualTo(0);

        GenerationPermit permit2 = throttler.acquirePermit(chunk2);
        assertThat(permit2.status()).isEqualTo(CascadingPermitStatus.ACQUIRED);
        assertThat(permit2.cascadingDepth()).isEqualTo(1);

        // Depth 2 exceeds maxCascadingDepth of 2 -> rejected
        GenerationPermit permit3 = throttler.acquirePermit(chunk3);
        assertThat(permit3.status()).isEqualTo(CascadingPermitStatus.REJECTED_DEPTH_EXCEEDED);
        assertThat(throttler.totalCascadesThrottled()).isEqualTo(1L);

        // Clean release
        throttler.releasePermit(permit2);
        throttler.releasePermit(permit1);
        assertThat(throttler.activeGenerationsCount()).isEqualTo(0);
    }

    @Test
    public void testCircularDependencyCycleRejection() {
        CascadingWorldGenThrottler throttler = new CascadingWorldGenThrottler(4, 10);
        CascadingThreadDepthTracker.current().clear();

        long chunkA = 500L;
        GenerationPermit permitA = throttler.acquirePermit(chunkA);
        assertThat(permitA.status()).isEqualTo(CascadingPermitStatus.ACQUIRED);

        // Attempting to acquire the same chunk on the same thread should be rejected as cycle
        GenerationPermit cyclicPermit = throttler.acquirePermit(chunkA);
        assertThat(cyclicPermit.status()).isEqualTo(CascadingPermitStatus.REJECTED_CYCLE_DETECTED);
        assertThat(throttler.cyclesAvoided()).isEqualTo(1L);

        throttler.releasePermit(permitA);
    }

    @Test
    public void testPhantomChunkSyntheticTerrainAndAir() {
        PhantomChunkReadOnlyView terrainView = new PhantomChunkReadOnlyView(5, 5);

        assertThat(terrainView.isReadOnly()).isTrue();
        assertThat(terrainView.chunkX()).isEqualTo(5);
        assertThat(terrainView.chunkZ()).isEqualTo(5);

        // Below or at sea level (default 63) should return stone
        PhantomBlockSnapshot stoneBlock = terrainView.getBlock(0, 60, 0);
        assertThat(stoneBlock.blockId()).isEqualTo("minecraft:stone");
        assertThat(stoneBlock.blockStateId()).isEqualTo(1);

        // Above sea level should return air
        PhantomBlockSnapshot airBlock = terrainView.getBlock(0, 70, 0);
        assertThat(airBlock.blockId()).isEqualTo("minecraft:air");
        assertThat(airBlock.blockStateId()).isEqualTo(0);

        // Height at sea level
        assertThat(terrainView.getHeight(0, 0)).isEqualTo(PhantomChunkReadOnlyView.DEFAULT_SEA_LEVEL);
    }

    @Test
    public void testPhantomChunkImmutableMutationRejection() {
        PhantomChunkReadOnlyView view = new PhantomChunkReadOnlyView(0, 0);

        assertThatThrownBy(() -> view.setBlock(0, 64, 0, "minecraft:diamond_block"))
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessageContaining("immutable and read-only");
    }
}
