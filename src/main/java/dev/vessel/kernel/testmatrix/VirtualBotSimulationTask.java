package dev.vessel.kernel.testmatrix;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Worker task executing continuous packet generation and movement simulations for a subset of virtual bots.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public final class VirtualBotSimulationTask implements Runnable {

    private final List<VirtualBotSession> bots;
    private final VirtualBotConfig config;
    private final CountDownLatch startGate;
    private final CountDownLatch endGate;
    private final AtomicLong totalProcessedCounter;
    private final AtomicLong errorCounter;

    public VirtualBotSimulationTask(
            List<VirtualBotSession> bots,
            VirtualBotConfig config,
            CountDownLatch startGate,
            CountDownLatch endGate,
            AtomicLong totalProcessedCounter,
            AtomicLong errorCounter
    ) {
        this.bots = Objects.requireNonNull(bots, "bots must not be null");
        this.config = Objects.requireNonNull(config, "config must not be null");
        this.startGate = Objects.requireNonNull(startGate, "startGate must not be null");
        this.endGate = Objects.requireNonNull(endGate, "endGate must not be null");
        this.totalProcessedCounter = Objects.requireNonNull(totalProcessedCounter, "totalProcessedCounter must not be null");
        this.errorCounter = Objects.requireNonNull(errorCounter, "errorCounter must not be null");
    }

    @Override
    public void run() {
        try {
            startGate.await();

            int ticks = config.ticksToRun();
            int packetsPerTick = config.packetsPerTick();
            double speed = config.movementSpeed();

            for (int tick = 0; tick < ticks; tick++) {
                for (int b = 0; b < bots.size(); b++) {
                    VirtualBotSession session = bots.get(b);
                    for (int p = 0; p < packetsPerTick; p++) {
                        if (p == 0) {
                            session.stepMovement(speed, 0.0, speed, (float) (tick * 5.0), 0.0f);
                        } else {
                            session.recordActionPacket();
                        }
                        totalProcessedCounter.incrementAndGet();
                    }
                }
            }
        } catch (InterruptedException interruptedException) {
            Thread.currentThread().interrupt();
            errorCounter.incrementAndGet();
        } catch (Throwable throwable) {
            errorCounter.incrementAndGet();
        } finally {
            endGate.countDown();
        }
    }
}
