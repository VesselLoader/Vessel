package dev.vessel.kernel.optimization.chunk;

import java.util.Objects;

/**
 * Immutable descriptor for an active or scheduled chunk loading ticket.
 *
 * @param chunkKey packed 64-bit chunk coordinate ((long) chunkX &lt;&lt; 32 | (chunkZ &amp; 0xFFFFFFFFL))
 * @param priority ticket priority tier
 * @param creationTick tick timestamp when ticket was requested
 * @param expiryTick tick timestamp after which ticket is eligible for eviction
 * @param ownerId identifier of requester (player UUID, entity ID, or system component)
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public record ChunkTicketEntry(
        long chunkKey,
        ChunkTicketPriority priority,
        long creationTick,
        long expiryTick,
        String ownerId
) {
    public ChunkTicketEntry {
        Objects.requireNonNull(priority, "priority cannot be null");
        Objects.requireNonNull(ownerId, "ownerId cannot be null");
    }

    /**
     * Checks if this ticket has expired at the specified game tick.
     *
     * @param currentTick current server tick count
     * @return true if expired
     */
    public boolean isExpired(long currentTick) {
        return currentTick >= expiryTick;
    }

    /**
     * Extracts chunk X from packed chunkKey.
     *
     * @return chunk X coordinate
     */
    public int chunkX() {
        return (int) (chunkKey >> 32);
    }

    /**
     * Extracts chunk Z from packed chunkKey.
     *
     * @return chunk Z coordinate
     */
    public int chunkZ() {
        return (int) chunkKey;
    }

    /**
     * Packs chunk coordinates into a 64-bit primitive long key.
     */
    public static long packKey(int chunkX, int chunkZ) {
        return ((long) chunkX << 32) | (chunkZ & 0xFFFFFFFFL);
    }
}
