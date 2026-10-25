package dev.vessel.kernel.world.worldgen;

import java.util.Objects;

/**
 * Immutable token representing a granted or rejected chunk generation permit.
 * <p>
 * Directly resolves ERR-33 and WGN-0001..0250.
 * </p>
 *
 * @param chunkKey packed 64-bit coordinate of the chunk
 * @param threadId Java thread ID requesting the generation
 * @param cascadingDepth current depth in the recursive generation chain
 * @param status status outcome of the permit evaluation
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public record GenerationPermit(
        long chunkKey,
        long threadId,
        int cascadingDepth,
        CascadingPermitStatus status
) {

    /**
     * Compact constructor validating invariants.
     */
    public GenerationPermit {
        Objects.requireNonNull(status, "status cannot be null");
        if (cascadingDepth < 0) {
            throw new IllegalArgumentException("cascadingDepth cannot be negative: " + cascadingDepth);
        }
    }

    /**
     * Checks if this permit successfully granted execution rights.
     *
     * @return {@code true} if acquired
     */
    public boolean isAcquired() {
        return status == CascadingPermitStatus.ACQUIRED;
    }
}
