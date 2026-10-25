package dev.vessel.kernel.testmatrix;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests verifying micro-tick TPS profiling, scalar replacement assist, JIT warm-up prioritization,
 * branchless jump routing, and weak memory barrier synchronizations.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public final class TpsProfilerAndHardwareOptimizationTest {

    @Test
    @DisplayName("Verify ZeroLagTpsProfiler records micro-latencies and confirms TPS >= 19.95")
    public void testZeroLagTpsProfiler() {
        ZeroLagTpsProfiler profiler = new ZeroLagTpsProfiler(100);

        // Record 100 ticks simulating 5.0ms duration each (well within 50.0ms budget)
        for (int i = 0; i < 100; i++) {
            profiler.recordTickDurationNanos(5_000_000L); // 5ms
        }

        TpsProfileSnapshot snapshot = profiler.captureSnapshot();

        assertThat(snapshot.recordedTicks()).isEqualTo(100L);
        assertThat(snapshot.currentTps()).isGreaterThanOrEqualTo(19.95);
        assertThat(snapshot.meanTickDurationMs()).isEqualTo(5.0);
        assertThat(snapshot.p95LatencyMs()).isEqualTo(5.0);
        assertThat(snapshot.p99LatencyMs()).isEqualTo(5.0);
        assertThat(snapshot.maxTickLatencyMs()).isEqualTo(5.0);
        assertThat(snapshot.zeroLagCompliant()).isTrue();
    }

    @Test
    @DisplayName("Verify PackedCoordinateCodec bitwise fidelity and scalar stack math")
    public void testPackedCoordinateCodecAndScalarReplacement() {
        int x = 12345;
        int y = 64;
        int z = -67890;

        long packedBlock = PackedCoordinateCodec.packBlockPos(x, y, z);
        assertThat(PackedCoordinateCodec.unpackX(packedBlock)).isEqualTo(x);
        assertThat(PackedCoordinateCodec.unpackY(packedBlock)).isEqualTo(y);
        assertThat(PackedCoordinateCodec.unpackZ(packedBlock)).isEqualTo(z);

        int chunkX = -105;
        int chunkZ = 204;
        long packedChunk = PackedCoordinateCodec.packChunkPos(chunkX, chunkZ);
        assertThat(PackedCoordinateCodec.unpackChunkX(packedChunk)).isEqualTo(chunkX);
        assertThat(PackedCoordinateCodec.unpackChunkZ(packedChunk)).isEqualTo(chunkZ);

        ScalarReplacementStackAllocationHardener hardener = new ScalarReplacementStackAllocationHardener();
        ScalarReplacementVerificationResult result = hardener.verifyZeroAllocation(1000);

        assertThat(result.operationsExecuted()).isEqualTo(1000L);
        assertThat(result.zeroAllocationAchieved()).isTrue();
    }

    @Test
    @DisplayName("Verify TieredCompilationPrioritySorter prioritizes C2 hot-paths over cold paths")
    public void testTieredCompilationPrioritySorter() {
        TieredCompilationPrioritySorter sorter = new TieredCompilationPrioritySorter();

        SampleWarmupTask coldTask = new SampleWarmupTask();
        CompilationTarget coldTarget = new CompilationTarget("ColdBootstrapTarget", CompilationTier.TIERED_COLD_PATH, 10, coldTask);

        SampleWarmupTask hotTask = new SampleWarmupTask();
        CompilationTarget hotTarget = new CompilationTarget("HotTickLoopTarget", CompilationTier.C2_HOT_PATH, 50, hotTask);

        // Register in reverse order
        sorter.registerTarget(coldTarget);
        sorter.registerTarget(hotTarget);

        TieredCompilationReport report = sorter.executePriorityWarmup();

        assertThat(report.totalTargetsCompiled()).isEqualTo(2);
        assertThat(report.hotPathsPromoted()).isEqualTo(1);
        assertThat(report.totalInvocations()).isEqualTo(60L);
        assertThat(hotTask.getInvocations()).isEqualTo(50);
        assertThat(coldTask.getInvocations()).isEqualTo(10);
    }

    @Test
    @DisplayName("Verify BranchlessJumpTableRouter O(1) table switch direct dispatch")
    public void testBranchlessJumpTableRouter() {
        BranchlessJumpTableRouter router = new BranchlessJumpTableRouter(16);

        SampleJumpRouteAction action0 = new SampleJumpRouteAction();
        SampleJumpRouteAction action1 = new SampleJumpRouteAction();
        SampleJumpRouteAction defaultAction = new SampleJumpRouteAction();

        router.registerRoute(0, action0);
        router.registerRoute(1, action1);
        router.setDefaultRoute(defaultAction);

        assertThat(router.getRegisteredRoutesCount()).isEqualTo(2);

        // Dispatch registered routes
        router.dispatch(0, 100L);
        router.dispatch(1, 200L);
        router.dispatch(5, 500L); // Falls back to default route

        assertThat(action0.getExecutedCount()).isEqualTo(1L);
        assertThat(action1.getExecutedCount()).isEqualTo(1L);
        assertThat(defaultAction.getExecutedCount()).isEqualTo(1L);

        // Benchmark run
        BranchlessRoutingReport benchmark = router.benchmarkDispatch(10_000);
        assertThat(benchmark.routedInvocations()).isEqualTo(10_000L);
        assertThat(benchmark.averageDispatchNanos()).isGreaterThan(0.0);
    }

    @Test
    @DisplayName("Verify WeakMemoryBarrierSynchronizer acquire/release state ordering and fences")
    public void testWeakMemoryBarrierSynchronizer() {
        WeakMemoryBarrierSynchronizer synchronizer = new WeakMemoryBarrierSynchronizer();

        // Exercise memory fence instructions
        synchronizer.acquireFence();
        synchronizer.releaseFence();
        synchronizer.loadLoadFence();
        synchronizer.storeStoreFence();
        synchronizer.fullFence();

        synchronizer.setInternalStateRelease(42L);
        assertThat(synchronizer.getInternalStateAcquire()).isEqualTo(42L);

        MemoryBarrierStats stats = synchronizer.benchmarkBarrier(MemoryBarrierMode.ACQUIRE_RELEASE, 10_000);
        assertThat(stats.operationsCount()).isEqualTo(10_000L);
        assertThat(stats.opsPerSecond()).isGreaterThan(0.0);
        assertThat(stats.mode()).isEqualTo(MemoryBarrierMode.ACQUIRE_RELEASE);
    }
}
