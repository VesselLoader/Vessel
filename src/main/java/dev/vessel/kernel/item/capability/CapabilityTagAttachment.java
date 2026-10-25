package dev.vessel.kernel.item.capability;

import java.util.Arrays;
import java.util.Objects;

/**
 * Immutable attachment representing a serialized Forge or NeoForge capability state
 * associated with an item stack.
 *
 * <p>Strictly adheres to Rule 17 with zero inner or nested classes.</p>
 *
 * @param capabilityKey canonical capability identifier (e.g. "forge:energy", "forge:fluid_handler")
 * @param rawPayload    serialized byte representation of capability NBT or data component
 * @param schemaVersion integer version for data format migrations
 * @author VESSEL Engine Team
 * @version 1.0.0
 */
public record CapabilityTagAttachment(
        String capabilityKey,
        byte[] rawPayload,
        int schemaVersion
) {

    /**
     * Compact constructor creating defensive copies of byte payload.
     */
    public CapabilityTagAttachment {
        Objects.requireNonNull(capabilityKey, "capabilityKey cannot be null");
        Objects.requireNonNull(rawPayload, "rawPayload cannot be null");
        rawPayload = Arrays.copyOf(rawPayload, rawPayload.length);
    }

    @Override
    public byte[] rawPayload() {
        return Arrays.copyOf(rawPayload, rawPayload.length);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        CapabilityTagAttachment that = (CapabilityTagAttachment) o;
        return schemaVersion == that.schemaVersion
                && Objects.equals(capabilityKey, that.capabilityKey)
                && Arrays.equals(rawPayload, that.rawPayload);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(capabilityKey, schemaVersion);
        result = 31 * result + Arrays.hashCode(rawPayload);
        return result;
    }
}
