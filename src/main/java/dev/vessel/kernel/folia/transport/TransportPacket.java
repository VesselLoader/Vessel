package dev.vessel.kernel.folia.transport;

import java.util.Objects;

/**
 * Immutable packet representing resource transfer across Folia chunk boundaries.
 * <p>
 * Eliminates cross-region deadlocks by carrying all transaction parameters
 * in a self-contained, thread-safe value record.
 * </p>
 * <p>
 * Directly resolves ERR-14, ERR-18, and FOL-001..020.
 * </p>
 *
 * @param sourceChunkKey packed coordinate key of the source chunk
 * @param targetChunkKey packed coordinate key of the destination chunk
 * @param blockX destination block X coordinate
 * @param blockY destination block Y coordinate
 * @param blockZ destination block Z coordinate
 * @param resourceType the category of resource being moved
 * @param amount quantity of energy, millibuckets of fluid, or item count
 * @param payload optional typed payload object (e.g. ItemStack, FluidStack, or custom data)
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public record TransportPacket(
        long sourceChunkKey,
        long targetChunkKey,
        int blockX,
        int blockY,
        int blockZ,
        TransportResourceType resourceType,
        long amount,
        Object payload
) {

    /**
     * Compact constructor validating invariants.
     */
    public TransportPacket {
        Objects.requireNonNull(resourceType, "resourceType cannot be null");
        if (amount < 0) {
            throw new IllegalArgumentException("amount cannot be negative: " + amount);
        }
    }
}
