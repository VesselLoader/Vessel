package dev.vessel.kernel.test;

import dev.vessel.kernel.tick.HighPrecisionNanoTicker;
import dev.vessel.kernel.tick.ServerTickBudgetTracker;
import dev.vessel.kernel.tick.TickPhaseStage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit verification suite for server tick pacing, phases, and MSPT/TPS budget tracking.
 *
 * @version 1.0.0
 * @author Shiroz &amp; Antigravity
 * @since 1.0.0 (Pass K-6)
 */
public class ServerTickTest {

    @Test
    @DisplayName("TickPhaseStage defines all canonical phases of a game tick")
    void testTickPhases() {
        assertThat(TickPhaseStage.values()).containsExactly(
                TickPhaseStage.PRE_TICK,
                TickPhaseStage.POLL_NETWORK,
                TickPhaseStage.TICK_GLOBAL_STRAND,
                TickPhaseStage.TICK_REGIONAL_STRANDS,
                TickPhaseStage.POST_ENTITY_TICK,
                TickPhaseStage.FLUSH_NETWORK,
                TickPhaseStage.POST_TICK
        );
    }

    @Test
    @DisplayName("HighPrecisionNanoTicker paces tick intervals and advances tick count")
    void testHighPrecisionNanoTicker() {
        HighPrecisionNanoTicker ticker = new HighPrecisionNanoTicker();
        assertThat(ticker.isRunning()).isFalse();

        ticker.start();
        assertThat(ticker.isRunning()).isTrue();
        assertThat(ticker.getTickCount()).isEqualTo(0L);

        // Advance two ticks
        ticker.awaitNextTick();
        assertThat(ticker.getTickCount()).isEqualTo(1L);

        ticker.stop();
        assertThat(ticker.isRunning()).isFalse();
    }

    @Test
    @DisplayName("ServerTickBudgetTracker computes rolling MSPT and dynamic TPS without allocations")
    void testTickBudgetTracker() {
        ServerTickBudgetTracker tracker = new ServerTickBudgetTracker();
        assertThat(tracker.getSampleCount()).isEqualTo(0);
        assertThat(tracker.getCalculatedTps()).isEqualTo(20.0);

        // Record 10 healthy ticks of 25ms (25,000,000 ns)
        for (int i = 0; i < 10; i++) {
            tracker.recordTick(25_000_000L);
        }

        assertThat(tracker.getSampleCount()).isEqualTo(10);
        assertThat(tracker.getAverageMspt()).isEqualTo(25.0);
        assertThat(tracker.getMaxMspt()).isEqualTo(25.0);
        assertThat(tracker.getMinMspt()).isEqualTo(25.0);
        assertThat(tracker.getCalculatedTps()).isEqualTo(20.0);

        // Record a lag spike tick of 100ms
        tracker.recordTick(100_000_000L);
        assertThat(tracker.getMaxMspt()).isEqualTo(100.0);

        // Reset
        tracker.reset();
        assertThat(tracker.getSampleCount()).isEqualTo(0);
    }
}
