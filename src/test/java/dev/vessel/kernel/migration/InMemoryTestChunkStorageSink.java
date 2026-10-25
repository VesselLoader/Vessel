package dev.vessel.kernel.migration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * In-memory test implementation of {@link ChunkStorageSink} for unit test verifications.
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public final class InMemoryTestChunkStorageSink implements ChunkStorageSink {

    private final List<ImmutableChunkSnapshot> writtenSnapshots;
    private volatile boolean throwOnWrite;

    /**
     * Constructs an in-memory sink.
     */
    public InMemoryTestChunkStorageSink() {
        this.writtenSnapshots = new CopyOnWriteArrayList<>();
        this.throwOnWrite = false;
    }

    @Override
    public void writeChunk(ImmutableChunkSnapshot snapshot) throws Exception {
        Objects.requireNonNull(snapshot, "snapshot cannot be null");
        if (throwOnWrite) {
            throw new java.io.IOException("Simulated I/O disk failure");
        }
        writtenSnapshots.add(snapshot);
    }

    /**
     * Enables or disables simulated write failures.
     *
     * @param fail true to throw exception on write
     */
    public void setSimulateFailure(boolean fail) {
        this.throwOnWrite = fail;
    }

    /**
     * Returns an unmodifiable snapshot list of all written chunks.
     *
     * @return list of written snapshots
     */
    public List<ImmutableChunkSnapshot> getWrittenSnapshots() {
        return Collections.unmodifiableList(new ArrayList<>(writtenSnapshots));
    }

    /**
     * Clears recorded snapshots.
     */
    public void clear() {
        writtenSnapshots.clear();
    }
}
