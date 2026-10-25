package dev.vessel.kernel.testmatrix;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Extreme concurrency stress load runner simulating hundreds of virtual bot connections,
 * continuous movement packets, and high-frequency protocol interactions.
 *
 * <p>Remediates ERR-19 and ERR-20 by stress-testing pipeline stability and preventing backpressure deadlocks.</p>
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public final class ConcurrentModStressLoadRunner {

    public static final String BOT_ID_PREFIX = "vessel-bot-";
    public static final int THREAD_POOL_SIZE = 8;
    public static final long AWAIT_TIMEOUT_SECONDS = 30L;

    public ConcurrentModStressLoadRunner() {
    }

    /**
     * Executes an extreme concurrency stress test run with the specified virtual bot configuration.
     *
     * @param config test load configuration
     * @return stress load evaluation report
     */
    public ModStressLoadReport runStressTest(VirtualBotConfig config) {
        Objects.requireNonNull(config, "config must not be null");

        int botCount = config.botCount();
        List<VirtualBotSession> allBots = new ArrayList<>(botCount);
        for (int i = 0; i < botCount; i++) {
            allBots.add(new VirtualBotSession(BOT_ID_PREFIX + i, i, 0.0, 64.0, 0.0));
        }

        int threads = Math.min(botCount, THREAD_POOL_SIZE);
        ExecutorService executor = Executors.newFixedThreadPool(threads);

        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch endGate = new CountDownLatch(threads);

        AtomicLong totalProcessed = new AtomicLong(0);
        AtomicLong errorCount = new AtomicLong(0);

        int chunkSize = (botCount + threads - 1) / threads;
        for (int t = 0; t < threads; t++) {
            int from = t * chunkSize;
            int to = Math.min(from + chunkSize, botCount);
            if (from < to) {
                List<VirtualBotSession> subList = allBots.subList(from, to);
                executor.execute(new VirtualBotSimulationTask(subList, config, startGate, endGate, totalProcessed, errorCount));
            } else {
                endGate.countDown();
            }
        }

        long startNanos = System.nanoTime();
        startGate.countDown();

        try {
            boolean finished = endGate.await(AWAIT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            if (!finished) {
                errorCount.incrementAndGet();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            errorCount.incrementAndGet();
        } finally {
            executor.shutdownNow();
        }

        long elapsedNanos = System.nanoTime() - startNanos;
        long durationMs = TimeUnit.NANOSECONDS.toMillis(elapsedNanos);
        if (durationMs <= 0) {
            durationMs = 1;
        }

        long expectedPackets = (long) botCount * config.ticksToRun() * config.packetsPerTick();
        long processed = totalProcessed.get();
        long dropped = Math.max(0, expectedPackets - processed);

        double seconds = durationMs / 1000.0;
        double achievedTps = config.ticksToRun() / (seconds > 0.0 ? seconds : 1.0);
        double avgLatencyMs = (double) durationMs / (config.ticksToRun() > 0 ? config.ticksToRun() : 1);

        return new ModStressLoadReport(
                botCount,
                expectedPackets,
                processed,
                dropped,
                avgLatencyMs,
                (int) errorCount.get(),
                config.targetTps(),
                achievedTps,
                durationMs
        );
    }
}
