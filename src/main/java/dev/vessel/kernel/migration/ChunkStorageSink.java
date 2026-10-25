package dev.vessel.kernel.migration;

/**
 * Pluggable backend storage sink responsible for persisting an immutable chunk snapshot to disk.
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
@FunctionalInterface
public interface ChunkStorageSink {

    /**
     * Persists the given chunk snapshot to permanent storage.
     *
     * @param snapshot the immutable chunk snapshot to write
     * @throws Exception if an I/O error occurs during write
     */
    void writeChunk(ImmutableChunkSnapshot snapshot) throws Exception;
}
