package dev.vessel.kernel.folia.blockentity;

import java.lang.ref.WeakReference;
import java.util.Objects;

/**
 * GC-safe weak reference wrapper for cross-region block entity capabilities.
 * <p>
 * Ensures that external energy cables, pipes, and wireless networks
 * do not hold strong references to block entities in neighboring chunks.
 * </p>
 * <p>
 * Directly resolves ERR-37 and FOL-081..100.
 * </p>
 *
 * @param chunkKey packed 64-bit coordinate of the owning chunk
 * @param blockX world X coordinate of the provider block
 * @param blockY world Y coordinate of the provider block
 * @param blockZ world Z coordinate of the provider block
 * @param capabilityId namespaced identifier for the capability (e.g. "forge:energy", "fabric:fluid_transfer")
 * @param reference weak reference to the capability instance
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public record WeakCapabilityEntry(
        long chunkKey,
        int blockX,
        int blockY,
        int blockZ,
        String capabilityId,
        WeakReference<Object> reference
) {

    /**
     * Compact constructor validating invariants.
     */
    public WeakCapabilityEntry {
        Objects.requireNonNull(capabilityId, "capabilityId cannot be null");
        Objects.requireNonNull(reference, "reference cannot be null");
    }

    /**
     * Checks if the underlying capability instance is still reachable in memory.
     *
     * @return {@code true} if referent has not been garbage collected
     */
    public boolean isReachable() {
        return reference.get() != null;
    }

    /**
     * Retrieves and dynamically casts the underlying capability if still reachable and type-compatible.
     *
     * @param <T> desired capability type
     * @param expectedType expected class type
     * @return typed capability, or {@code null} if collected or incompatible
     */
    public <T> T get(Class<T> expectedType) {
        Objects.requireNonNull(expectedType, "expectedType cannot be null");
        Object obj = reference.get();
        if (obj == null) {
            return null;
        }
        if (expectedType.isInstance(obj)) {
            return expectedType.cast(obj);
        }
        return null;
    }
}
