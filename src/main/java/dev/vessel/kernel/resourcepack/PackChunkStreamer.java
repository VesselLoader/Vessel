package dev.vessel.kernel.resourcepack;

import java.util.Objects;

/**
 * High-throughput chunked streamer for serving in-memory resource packs to joining players.
 * <p>
 * Slices virtual resource pack archives into network chunk frames (e.g. 256KB chunks) on-the-fly
 * for HTTP / TCP pack downloads with zero disk I/O bottlenecks.
 * </p>
 *
 * @version 1.0.1
 * @since 1.0.0 (Pass K-6)
 */
public class PackChunkStreamer {

    /** Default streaming chunk size: 256 KB. */
    public static final int DEFAULT_CHUNK_SIZE = 256 * 1024;

    /**
     * Calculates the total number of chunks needed to stream the pack.
     *
     * @param pack the virtual resource pack
     * @param chunkSize size of each chunk in bytes
     * @return total chunk count
     */
    public int calculateChunkCount(VirtualResourcePack pack, int chunkSize) {
        Objects.requireNonNull(pack, "pack cannot be null");
        if (chunkSize <= 0) {
            throw new IllegalArgumentException("chunkSize must be positive: " + chunkSize);
        }
        int totalBytes = pack.sizeBytes();
        if (totalBytes == 0) {
            return 0;
        }
        return (totalBytes + chunkSize - 1) / chunkSize;
    }

    /**
     * Extracts a specific slice chunk of the resource pack for network transmission.
     *
     * @param pack the virtual resource pack
     * @param chunkIndex zero-based chunk index
     * @param chunkSize size of each chunk in bytes
     * @return byte array containing the requested chunk slice
     */
    public byte[] getChunkSlice(VirtualResourcePack pack, int chunkIndex, int chunkSize) {
        Objects.requireNonNull(pack, "pack cannot be null");
        int totalChunks = calculateChunkCount(pack, chunkSize);
        if (chunkIndex < 0 || chunkIndex >= totalChunks) {
            throw new IndexOutOfBoundsException(
                    String.format("chunkIndex %d out of bounds (totalChunks=%d)", chunkIndex, totalChunks)
            );
        }

        byte[] raw = pack.archiveData();
        int offset = chunkIndex * chunkSize;
        int remaining = raw.length - offset;
        int actualLen = Math.min(chunkSize, remaining);

        byte[] slice = new byte[actualLen];
        System.arraycopy(raw, offset, slice, 0, actualLen);
        return slice;
    }

    /**
     * Verifies that the pack's computed SHA-1 hash matches the expected checksum string.
     *
     * @param pack virtual resource pack
     * @param expectedSha1Hex expected SHA-1 string (case-insensitive)
     * @return true if checksum matches
     */
    public boolean verifyIntegrity(VirtualResourcePack pack, String expectedSha1Hex) {
        Objects.requireNonNull(pack, "pack cannot be null");
        Objects.requireNonNull(expectedSha1Hex, "expectedSha1Hex cannot be null");
        return pack.sha1Hex().equalsIgnoreCase(expectedSha1Hex.trim());
    }
}
