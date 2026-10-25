package dev.vessel.kernel.jvm.reflection;

import java.util.Objects;

/**
 * Immutable carrier representing an exact memory offset of a field calculated via Unsafe.
 *
 * @param className declaring class binary name
 * @param fieldName declared field name
 * @param offset byte offset in heap memory
 * @param isStatic true if the field is static, false if instance
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public record FieldOffsetEntry(
        String className,
        String fieldName,
        long offset,
        boolean isStatic
) {
    public FieldOffsetEntry {
        Objects.requireNonNull(className, "className cannot be null");
        Objects.requireNonNull(fieldName, "fieldName cannot be null");
    }
}
