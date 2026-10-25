package dev.vessel.kernel.folia.blockentity;

import java.util.Objects;

/**
 * Immutable record representing an active BlockEntity tracked by the sentinel.
 * <p>
 * Directly resolves ERR-36 and FOL-061..080.
 * </p>
 *
 * @param chunkKey packed 64-bit coordinate of the chunk
 * @param blockX world X coordinate
 * @param blockY world Y coordinate
 * @param blockZ world Z coordinate
 * @param blockEntityId namespaced identifier of the BlockEntity
 * @param state current lifecycle and ticket state
 * @param callback optional lifecycle notification callback
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public record BlockEntityRegistration(
        long chunkKey,
        int blockX,
        int blockY,
        int blockZ,
        String blockEntityId,
        BlockEntityTicketState state,
        BlockEntityLifecycleCallback callback
) {

    /**
     * Compact constructor validating invariants.
     */
    public BlockEntityRegistration {
        Objects.requireNonNull(blockEntityId, "blockEntityId cannot be null");
        Objects.requireNonNull(state, "state cannot be null");
    }

    /**
     * Produces a updated registration with a new ticket state.
     *
     * @param newState the new lifecycle state
     * @return updated registration record
     */
    public BlockEntityRegistration withState(BlockEntityTicketState newState) {
        return new BlockEntityRegistration(
                chunkKey, blockX, blockY, blockZ, blockEntityId, newState, callback
        );
    }
}
