package dev.vessel.kernel.ir;

import java.util.Objects;

/**
 * Immutable carrier representing a Java 16+ record component in Universal Typed IR.
 *
 * @param name component name
 * @param descriptor field descriptor
 * @param signature generic signature, or null if absent
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public record UniversalTypedRecordComponentIR(
        String name,
        String descriptor,
        String signature
) {
    public UniversalTypedRecordComponentIR {
        Objects.requireNonNull(name, "name cannot be null");
        Objects.requireNonNull(descriptor, "descriptor cannot be null");
    }
}
