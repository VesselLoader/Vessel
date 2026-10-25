package dev.vessel.kernel.datapack;

import java.util.Objects;

/**
 * Immutable zero-copy slice descriptor representing a sub-region within an NBT buffer.
 *
 * @param offset byte offset from beginning of buffer
 * @param length payload length in bytes
 * @param tagType NBT tag type
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public record ZeroCopyNbtSlice(
        int offset,
        int length,
        NbtTagType tagType
) {
    public ZeroCopyNbtSlice {
        if (offset < 0) {
            throw new IllegalArgumentException("offset cannot be negative: " + offset);
        }
        if (length < 0) {
            throw new IllegalArgumentException("length cannot be negative: " + length);
        }
        Objects.requireNonNull(tagType, "tagType cannot be null");
    }
}
