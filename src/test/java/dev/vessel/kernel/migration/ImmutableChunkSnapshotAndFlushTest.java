package dev.vessel.kernel.migration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit test suite verifying immutable chunk snapshot barriers and asynchronous flush persistence.
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public final class ImmutableChunkSnapshotAndFlushTest {

    @Test
    @DisplayName("ImmutableChunkSnapshotBarrier captures snapshot isolating state from subsequent mutations")
    public void testSnapshotIsolationFromMutations() {
        ImmutableChunkSnapshotBarrier barrier = new ImmutableChunkSnapshotBarrier();

        List<ChunkSectionSnapshot> sections = new ArrayList<>();
        short[] blockStates = new short[ChunkSectionSnapshot.BLOCKS_PER_SECTION];
        blockStates[0] = 1;
        PalettedBiomeContainer biomes = new PalettedBiomeContainer(0, List.of("minecraft:plains"), new long[0], 0);
        sections.add(new ChunkSectionSnapshot(0, biomes, blockStates, new byte[0], new byte[0], false));

        Map<String, byte[]> tileEntities = new HashMap<>();
        byte[] tileData = new byte[]{1, 2, 3};
        tileEntities.put("0,64,0", tileData);

        ImmutableChunkSnapshot snapshot = barrier.captureSnapshot(
                10,
                20,
                "minecraft:overworld",
                1000L,
                sections,
                tileEntities,
                List.of(),
                new long[37],
                ChunkSnapshotCaptureMode.FULL_AUTOSAVE
        );

        // Mutate original arrays and maps
        tileData[0] = 99;
        tileEntities.put("new_key", new byte[]{5});
        blockStates[0] = 999;

        // Snapshot must remain completely pristine and unaffected
        assertThat(snapshot.chunkKey()).isEqualTo("minecraft:overworld:10:20");
        assertThat(snapshot.blockEntitiesNbt().get("0,64,0")[0]).isEqualTo((byte) 1);
        assertThat(snapshot.blockEntitiesNbt()).doesNotContainKey("new_key");
        assertThat(snapshot.sections().getFirst().blockStateIds()[0]).isEqualTo((short) 1);
    }

    @Test
    @DisplayName("AsyncChunkFlushWriter asynchronously writes snapshots to storage sink and drains cleanly")
    public void testAsyncChunkFlushWriterLifecycle() throws Exception {
        InMemoryTestChunkStorageSink storageSink = new InMemoryTestChunkStorageSink();
        ChunkFlushQueueThrottler throttler = new ChunkFlushQueueThrottler(50, 100);

        try (AsyncChunkFlushWriter writer = new AsyncChunkFlushWriter(storageSink, 2, throttler)) {
            ImmutableChunkSnapshotBarrier barrier = new ImmutableChunkSnapshotBarrier();

            for (int i = 0; i < 10; i++) {
                ImmutableChunkSnapshot snap = barrier.captureSnapshot(
                        i,
                        i * 2,
                        "minecraft:overworld",
                        100L + i,
                        List.of(),
                        Map.of(),
                        List.of(),
                        new long[0],
                        ChunkSnapshotCaptureMode.FULL_AUTOSAVE
                );
                boolean submitted = writer.submit(snap);
                assertThat(submitted).isTrue();
            }

            boolean drained = writer.flushAllSync(3000L);
            assertThat(drained).isTrue();

            ChunkFlushMetrics metrics = writer.getMetrics();
            assertThat(metrics.writtenCount()).isEqualTo(10L);
            assertThat(storageSink.getWrittenSnapshots()).hasSize(10);
        }
    }

    @Test
    @DisplayName("AsyncChunkFlushWriter tolerates sink failures without crashing worker threads")
    public void testAsyncChunkFlushWriterResilience() throws Exception {
        InMemoryTestChunkStorageSink failingSink = new InMemoryTestChunkStorageSink();
        failingSink.setSimulateFailure(true);

        try (AsyncChunkFlushWriter writer = new AsyncChunkFlushWriter(failingSink, 1, new ChunkFlushQueueThrottler())) {
            ImmutableChunkSnapshotBarrier barrier = new ImmutableChunkSnapshotBarrier();
            ImmutableChunkSnapshot snap = barrier.captureSnapshot(
                    5, 5, "minecraft:the_nether", 50L,
                    List.of(), Map.of(), List.of(), new long[0], ChunkSnapshotCaptureMode.ON_DEMAND
            );

            writer.submit(snap);
            writer.flushAllSync(2000L);

            ChunkFlushMetrics metrics = writer.getMetrics();
            assertThat(metrics.failureCount()).isGreaterThanOrEqualTo(1L);
        }
    }
}
