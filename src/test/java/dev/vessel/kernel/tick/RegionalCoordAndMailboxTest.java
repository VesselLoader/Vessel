package dev.vessel.kernel.tick;

import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link RegionalChunkCoordinateHasher} and {@link CrossRegionActorMailbox}.
 * <p>
 * Validates O(1) coordinate hashing, actor ask/tell message delivery,
 * deadline timeout handling, and cross-region isolation.
 * </p>
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public class RegionalCoordAndMailboxTest {

    @Test
    public void testRegionalChunkCoordinateHashing() {
        int chunkX = 35; // 35 >> 4 = 2, relative = 3
        int chunkZ = -19; // -19 >> 4 = -2, relative = 13

        long regionKey = RegionalChunkCoordinateHasher.toRegionKey(chunkX, chunkZ);
        int regX = RegionalChunkCoordinateHasher.unpackRegionX(regionKey);
        int regZ = RegionalChunkCoordinateHasher.unpackRegionZ(regionKey);

        assertThat(regX).isEqualTo(chunkX >> 4);
        assertThat(regZ).isEqualTo(chunkZ >> 4);
        assertThat(RegionalChunkCoordinateHasher.toChunkRelativeX(chunkX)).isEqualTo(3);
        assertThat(RegionalChunkCoordinateHasher.toChunkRelativeZ(chunkZ)).isEqualTo((-19) & 15);

        // Block to region
        int blockX = 512; // 512 >> 8 = 2
        int blockZ = 256; // 256 >> 8 = 1
        long blockRegKey = RegionalChunkCoordinateHasher.blockToRegionKey(blockX, blockZ);
        assertThat(RegionalChunkCoordinateHasher.unpackRegionX(blockRegKey)).isEqualTo(2);
        assertThat(RegionalChunkCoordinateHasher.unpackRegionZ(blockRegKey)).isEqualTo(1);

        // Same region check
        assertThat(RegionalChunkCoordinateHasher.isSameRegion(0, 0, 15, 15)).isTrue();
        assertThat(RegionalChunkCoordinateHasher.isSameRegion(0, 0, 16, 0)).isFalse();

        // Distance metrics
        long regA = RegionalChunkCoordinateHasher.toRegionKey(0, 0);
        long regB = RegionalChunkCoordinateHasher.toRegionKey(32, 48); // (2, 3)
        assertThat(RegionalChunkCoordinateHasher.chebyshevDistance(regA, regB)).isEqualTo(3);
        assertThat(RegionalChunkCoordinateHasher.manhattanDistance(regA, regB)).isEqualTo(5);

        // Avalanche hash non-zero
        int hash = RegionalChunkCoordinateHasher.hashRegion(regX, regZ);
        assertThat(hash).isNotZero();
    }

    @Test
    public void testActorMailboxTellPattern() {
        long regionKey = RegionalChunkCoordinateHasher.toRegionKey(0, 0);
        CrossRegionActorMailbox mailbox = new CrossRegionActorMailbox(regionKey, 64);

        AtomicInteger counter = new AtomicInteger(0);
        mailbox.tell(1L, counter::incrementAndGet);
        mailbox.tell(1L, counter::incrementAndGet);

        assertThat(mailbox.size()).isEqualTo(2);

        int processed = mailbox.drainBatch(10);
        assertThat(processed).isEqualTo(2);
        assertThat(counter.get()).isEqualTo(2);
        assertThat(mailbox.isEmpty()).isTrue();
        assertThat(mailbox.processedCount()).isEqualTo(2);
    }

    @Test
    public void testActorMailboxAskPatternSuccess() throws ExecutionException, InterruptedException, TimeoutException {
        long regionKey = RegionalChunkCoordinateHasher.toRegionKey(16, 0);
        CrossRegionActorMailbox mailbox = new CrossRegionActorMailbox(regionKey, 64);

        CompletableFuture<String> future = mailbox.ask(1L, () -> "COMPUTED_RESULT", 1000L);
        assertThat(future.isDone()).isFalse();

        mailbox.processNext();

        assertThat(future.isDone()).isTrue();
        assertThat(future.get(100, TimeUnit.MILLISECONDS)).isEqualTo("COMPUTED_RESULT");
        assertThat(mailbox.processedCount()).isEqualTo(1);
    }

    @Test
    public void testActorMailboxAskTimeoutPrevention() {
        long regionKey = RegionalChunkCoordinateHasher.toRegionKey(16, 16);
        CrossRegionActorMailbox mailbox = new CrossRegionActorMailbox(regionKey, 64);

        AtomicBoolean executed = new AtomicBoolean(false);
        mailbox.ask(1L, () -> {
            executed.set(true);
            return 42;
        }, 10L); // 10ms timeout

        // Sleep to let envelope expire before strand drains it
        try {
            Thread.sleep(50L);
        } catch (InterruptedException ignored) {
        }

        mailbox.processNext();

        // Expired task should not execute body
        assertThat(executed.get()).isFalse();
        assertThat(mailbox.timeoutCount()).isEqualTo(1L);
    }

    @Test
    public void testActorMailboxSaturationRejection() {
        long regionKey = RegionalChunkCoordinateHasher.toRegionKey(0, 0);
        CrossRegionActorMailbox mailbox = new CrossRegionActorMailbox(regionKey, 2);

        boolean tell1 = mailbox.tell(1L, () -> {});
        boolean tell2 = mailbox.tell(1L, () -> {});
        boolean tell3 = mailbox.tell(1L, () -> {});

        assertThat(tell1).isTrue();
        assertThat(tell2).isTrue();
        assertThat(tell3).isFalse();
        assertThat(mailbox.rejectedCount()).isEqualTo(1L);
    }
}
