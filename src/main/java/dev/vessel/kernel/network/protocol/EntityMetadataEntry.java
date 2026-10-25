package dev.vessel.kernel.network.protocol;

import java.util.Arrays;
import java.util.Objects;

/**
 * Immutable record representing an entity data watcher metadata item.
 *
 * @param index      the zero-based metadata index (0..63)
 * @param typeId     the data watcher serializer type ID
 * @param valueBytes raw serialized binary value
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public record EntityMetadataEntry(
        int index,
        int typeId,
        byte[] valueBytes
) {

    /**
     * Compact constructor validating non-null requirements and index bounds.
     */
    public EntityMetadataEntry {
        if (index < 0 || index >= 64) {
            throw new IllegalArgumentException("Metadata index must be between 0 and 63 inclusive: " + index);
        }
        if (typeId < 0) {
            throw new IllegalArgumentException("TypeId cannot be negative: " + typeId);
        }
        Objects.requireNonNull(valueBytes, "valueBytes must not be null");
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof EntityMetadataEntry other)) {
            return false;
        }
        return index == other.index
                && typeId == other.typeId
                && Arrays.equals(valueBytes, other.valueBytes);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(index, typeId);
        result = 31 * result + Arrays.hashCode(valueBytes);
        return result;
    }

    @Override
    public String toString() {
        return "EntityMetadataEntry{"
                + "index=" + index
                + ", typeId=" + typeId
                + ", valueLength=" + valueBytes.length
                + '}';
    }
}
