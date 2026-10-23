package dev.vessel.bootstrap.target;

import dev.vessel.kernel.tick.TickPhaseStage;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Injects deterministic telemetry hooks into the server game loop across all {@link TickPhaseStage}
 * boundaries, measuring exact per-tick nanosecond duration.
 *
 * <p>Strictly adheres to Rule 17 with zero inner or nested classes.</p>
 *
 * @version 1.0.0
 * @author Shiroz &amp; Antigravity
 * @since 1.0.0 (Pass K-29)
 */
public final class ServerTickLoopInjector {

    private final AtomicLong activeTickNumber = new AtomicLong(0L);
    private final AtomicLong tickStartNanoTime = new AtomicLong(0L);
    private final AtomicReference<TickPhaseStage> currentPhase = new AtomicReference<>(TickPhaseStage.PRE_TICK);

    /**
     * Invoked at the start of a server tick before network polling begins.
     *
     * @param tickNumber monotonic server tick index
     */
    public void onServerTickStart(long tickNumber) {
        activeTickNumber.set(tickNumber);
        tickStartNanoTime.set(System.nanoTime());
        currentPhase.set(TickPhaseStage.PRE_TICK);
    }

    /**
     * Transitions the current server tick into the next {@link TickPhaseStage}.
     *
     * @param phase new tick execution phase
     */
    public void onServerTickPhaseTransition(TickPhaseStage phase) {
        Objects.requireNonNull(phase, "phase cannot be null");
        currentPhase.set(phase);
    }

    /**
     * Finalizes the current server tick and returns the elapsed duration in nanoseconds.
     *
     * @param tickNumber monotonic server tick index
     * @return non-negative elapsed nanoseconds for the tick
     */
    public long onServerTickEnd(long tickNumber) {
        currentPhase.set(TickPhaseStage.POST_TICK);
        long start = tickStartNanoTime.get();
        if (start == 0L) {
            return 0L;
        }
        return Math.max(1L, System.nanoTime() - start);
    }

    public TickPhaseStage getCurrentPhase() {
        return currentPhase.get();
    }

    public long getActiveTickNumber() {
        return activeTickNumber.get();
    }
}
