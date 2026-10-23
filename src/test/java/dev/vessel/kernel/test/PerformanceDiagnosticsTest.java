package dev.vessel.kernel.test;

import dev.vessel.kernel.actor.ActorMailboxQueue;
import dev.vessel.kernel.actor.CrossRegionHarmonizerMetrics;
import dev.vessel.kernel.actor.GlobalRegionSyncBarrier;
import dev.vessel.kernel.actor.RegionCoordinateHash;
import dev.vessel.kernel.actor.RegionalActorStrand;
import dev.vessel.kernel.perf.LagDiagnosticRateLimiter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Verification test suite for performance diagnostics, metrics, rate limiting, and batch draining.
 *
 * @version 1.0.2
 * @since 1.0.0 (Pass K-5)
 */
public class PerformanceDiagnosticsTest {

    @Test
    @DisplayName("LagDiagnosticRateLimiter suppresses burst warnings within time window")
    void testRateLimiterThrottling() {
        LagDiagnosticRateLimiter limiter = new LagDiagnosticRateLimiter(500); // 500ms

        // First attempt succeeds
        assertThat(limiter.tryAcquire("WARN_CROSS_REGION_LAG")).isTrue();

        // Immediate subsequent attempts within 500ms fail
        assertThat(limiter.tryAcquire("WARN_CROSS_REGION_LAG")).isFalse();
        assertThat(limiter.tryAcquire("WARN_CROSS_REGION_LAG")).isFalse();

        // Different category key succeeds
        assertThat(limiter.tryAcquire("WARN_METADATA_SYNTAX")).isTrue();

        // Reset allows key again
        limiter.reset();
        assertThat(limiter.tryAcquire("WARN_CROSS_REGION_LAG")).isTrue();
    }

    @Test
    @DisplayName("CrossRegionHarmonizerMetrics records latency, max wait, and timeouts")
    void testHarmonizerMetrics() throws Exception {
        GlobalRegionSyncBarrier barrier = new GlobalRegionSyncBarrier();
        RegionCoordinateHash hash = RegionCoordinateHash.fromBlock(0, 0);

        try (RegionalActorStrand strand = new RegionalActorStrand(hash)) {
            // Warm up strand execution thread to avoid OS thread initialization jitter
            strand.ask(() -> "warmup").get(2, TimeUnit.SECONDS);

            // Case 1: successful query
            String res = barrier.executeWithBarrier(strand, () -> "Hello");
            assertThat(res).isEqualTo("Hello");

            CrossRegionHarmonizerMetrics metrics = barrier.getMetrics();
            assertThat(metrics.getTotalQueries()).isEqualTo(1L);
            assertThat(metrics.getTotalBlockedNanos()).isGreaterThan(0L);
            assertThat(metrics.getMaxWaitNanos()).isGreaterThan(0L);
            assertThat(metrics.getTimeouts()).isEqualTo(0L);
            assertThat(metrics.getAverageWaitMillis()).isGreaterThan(0.0);

            // Case 2: timeout query
            assertThatThrownBy(() -> barrier.executeWithBarrier(strand, () -> {
                try {
                    Thread.sleep(50);
                } catch (InterruptedException ignored) {}
                return "Slow";
            }, 5, TimeUnit.MILLISECONDS))
                    .isInstanceOf(TimeoutException.class);

            assertThat(metrics.getTotalQueries()).isEqualTo(2L);
            assertThat(metrics.getTimeouts()).isEqualTo(1L);
        }
    }

    @Test
    @DisplayName("ActorMailboxQueue drainBatch enforces bounded time-slice processing")
    void testBoundedBatchDrain() {
        ActorMailboxQueue<String> queue = new ActorMailboxQueue<>();
        for (int i = 0; i < 10; i++) {
            queue.send("Task-" + i);
        }

        assertThat(queue.size()).isEqualTo(10);

        List<String> batch1 = new ArrayList<>();
        int drained1 = queue.drainBatch(batch1::add, 4);
        assertThat(drained1).isEqualTo(4);
        assertThat(batch1).containsExactly("Task-0", "Task-1", "Task-2", "Task-3");
        assertThat(queue.size()).isEqualTo(6);

        List<String> batch2 = new ArrayList<>();
        int drained2 = queue.drainBatch(batch2::add, 4);
        assertThat(drained2).isEqualTo(4);
        assertThat(batch2).containsExactly("Task-4", "Task-5", "Task-6", "Task-7");
        assertThat(queue.size()).isEqualTo(2);

        List<String> batch3 = new ArrayList<>();
        int drained3 = queue.drainBatch(batch3::add, 4);
        assertThat(drained3).isEqualTo(2);
        assertThat(batch3).containsExactly("Task-8", "Task-9");
        assertThat(queue.isEmpty()).isTrue();
    }
}
