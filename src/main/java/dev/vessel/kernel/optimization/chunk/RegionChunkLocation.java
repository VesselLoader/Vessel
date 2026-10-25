package dev.vessel.kernel.optimization.chunk;

/**
 * Immutable location descriptor of a chunk inside an Anvil or Linear region file.
 * Offsets and sector allocations align to 4096-byte boundaries.
 *
 * @param sectorOffset sector offset index within the region file
 * @param sectorCount number of 4096-byte sectors allocated
 * @param timestamp epoch second timestamp of chunk's last modification
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public record RegionChunkLocation(
        int sectorOffset,
        int sectorCount,
        int timestamp
) {
    public static final RegionChunkLocation EMPTY = new RegionChunkLocation(0, 0, 0);
    public static final int SECTOR_BYTES = 4096;

    /**
     * Checks if this location indicates an unallocated/empty chunk slot.
     *
     * @return true if empty
     */
    public boolean isEmpty() {
        return sectorOffset == 0 && sectorCount == 0;
    }

    /**
     * Byte offset from the start of the region file.
     *
     * @return byte offset
     */
    public long byteOffset() {
        return (long) sectorOffset * SECTOR_BYTES;
    }

    /**
     * Total allocated byte capacity for this chunk slot.
     *
     * @return byte capacity
     */
    public int byteCapacity() {
        return sectorCount * SECTOR_BYTES;
    }
}
