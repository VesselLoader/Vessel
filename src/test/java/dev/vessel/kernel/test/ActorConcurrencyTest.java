package dev.vessel.kernel.test;

import dev.vessel.kernel.actor.ActorMailboxQueue;
import dev.vessel.kernel.actor.GlobalRegionActorStrand;
import dev.vessel.kernel.actor.GlobalRegionSyncBarrier;
import dev.vessel.kernel.actor.RegionCoordinateHash;
import dev.vessel.kernel.actor.RegionalActorStrand;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit verification suite for Actor Mailbox, Regional/Global Strands, and Universal Harmonizer Barrier.
 *
 * @version 1.0.3
 * @since 1.0.0 (Pass K-4)
 */
public class ActorConcurrencyTest {

    @Test
    @DisplayName("ActorMailboxQueue handles lockless concurrent MPSC task submission without loss")
    void testLocklessMpscMailbox() throws InterruptedException {
        ActorMailboxQueue<Integer> mailbox = new ActorMailboxQueue<>();
        int producerCount = 8;
        int messagesPerProducer = 250;
        int totalExpected = producerCount * messagesPerProducer;

        ExecutorService producers = Executors.newFixedThreadPool(producerCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch finishLatch = new CountDownLatch(producerCount);

        for (int p = 0; p < producerCount; p++) {
            final int producerId = p;
            producers.submit(() -> {
                try {
                    startLatch.await();
                    for (int m = 0; m < messagesPerProducer; m++) {
                        mailbox.send((producerId * 10000) + m);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    finishLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        assertThat(finishLatch.await(5, TimeUnit.SECONDS)).isTrue();
        producers.shutdown();

        List<Integer> drained = new ArrayList<>();
        int count = mailbox.drainTo(drained::add);

        assertThat(count).isEqualTo(totalExpected);
        assertThat(drained).hasSize(totalExpected);
        assertThat(mailbox.isEmpty()).isTrue();
    }

    @Test
    @DisplayName("RegionalActorStrand executes tasks and verifies strand thread identity")
    void testRegionalStrandExecution() throws Exception {
        RegionCoordinateHash hash = RegionCoordinateHash.fromBlock(100, 200);
        try (RegionalActorStrand strand = new RegionalActorStrand(hash)) {
            assertThat(strand.getRegionHash()).isEqualTo(hash);
            assertThat(strand.isCurrentStrand()).isFalse();

            String result = strand.ask(() -> {
                assertThat(strand.isCurrentStrand()).isTrue();
                return "BlockAt(" + hash.regionX() + "," + hash.regionZ() + ")";
            }).get(2, TimeUnit.SECONDS);

            assertThat(result).isEqualTo("BlockAt(" + hash.regionX() + "," + hash.regionZ() + ")");
        }
    }

    @Test
    @DisplayName("GlobalRegionActorStrand runs tasks on dedicated pinned global thread")
    void testGlobalRegionStrandExecution() throws Exception {
        try (GlobalRegionActorStrand globalStrand = new GlobalRegionActorStrand()) {
            assertThat(globalStrand.isCurrentStrand()).isFalse();

            String threadName = globalStrand.ask(() -> {
                assertThat(globalStrand.isCurrentStrand()).isTrue();
                return Thread.currentThread().getName();
            }).get(2, TimeUnit.SECONDS);

            assertThat(threadName).isEqualTo("vessel-global-region-strand");
        }
    }

    @Test
    @DisplayName("Decision 40 Choice 2: GlobalRegionSyncBarrier harmonizes cross-strand queries with timeout barrier")
    void testUniversalHarmonizerBarrier() throws Exception {
        RegionCoordinateHash targetHash = RegionCoordinateHash.fromBlock(512, 1024);
        GlobalRegionSyncBarrier barrier = new GlobalRegionSyncBarrier();

        try (RegionalActorStrand regionStrand = new RegionalActorStrand(targetHash);
             GlobalRegionActorStrand globalStrand = new GlobalRegionActorStrand()) {

            // Warm up worker threads to eliminate cold OS thread initialization jitter
            regionStrand.ask(() -> "warmup").get(2, TimeUnit.SECONDS);
            globalStrand.ask(() -> "warmup").get(2, TimeUnit.SECONDS);

            // Case A: Query target regional strand from global strand through barrier
            String queryResult = globalStrand.ask(() -> {
                try {
                    return barrier.executeWithBarrier(regionStrand, () -> "ChunkData@(" + targetHash.regionX() + "," + targetHash.regionZ() + ")");
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }).get(2, TimeUnit.SECONDS);

            assertThat(queryResult).isEqualTo("ChunkData@(" + targetHash.regionX() + "," + targetHash.regionZ() + ")");
            assertThat(barrier.getHarmonizedCallCount()).isEqualTo(1L);

            // Case B: Fast path zero overhead when called directly on target strand
            String fastPathResult = regionStrand.ask(() -> {
                try {
                    return barrier.executeWithBarrier(regionStrand, () -> "FastPath");
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }).get(2, TimeUnit.SECONDS);

            assertThat(fastPathResult).isEqualTo("FastPath");
            assertThat(barrier.getHarmonizedCallCount()).isEqualTo(1L); // Not incremented on fast path

            // Case C: Barrier times out if target strand is blocked
            assertThatThrownBy(() -> barrier.executeWithBarrier(regionStrand, () -> {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException ignored) {}
                return "Delayed";
            }, 10, TimeUnit.MILLISECONDS))
                    .isInstanceOf(TimeoutException.class);
        }
    }
}
