package dev.vessel.kernel.network.protocol;

/**
 * Immutable summary containing metrics of a dirty-only metadata packing operation.
 *
 * @param totalEntriesCount       total metadata entries registered on the entity
 * @param dirtyEntriesPackedCount number of dirty entries that were actually packed into the packet
 * @param bytesWritten            total byte count written to the output buffer
 * @param bandwidthSavingsPercent estimated percentage of network bandwidth saved
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public record DirtyPackingSummary(
        int totalEntriesCount,
        int dirtyEntriesPackedCount,
        int bytesWritten,
        double bandwidthSavingsPercent
) {

    /**
     * Compact constructor validating range constraints.
     */
    public DirtyPackingSummary {
        if (totalEntriesCount < 0) {
            throw new IllegalArgumentException("totalEntriesCount cannot be negative: " + totalEntriesCount);
        }
        if (dirtyEntriesPackedCount < 0 || dirtyEntriesPackedCount > totalEntriesCount) {
            throw new IllegalArgumentException("dirtyEntriesPackedCount out of bounds: " + dirtyEntriesPackedCount);
        }
        if (bytesWritten < 0) {
            throw new IllegalArgumentException("bytesWritten cannot be negative: " + bytesWritten);
        }
    }
}
