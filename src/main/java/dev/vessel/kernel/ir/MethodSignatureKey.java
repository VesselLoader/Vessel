package dev.vessel.kernel.ir;

import java.util.Objects;

/**
 * Immutable lookup key identifying a JVM method by name and descriptor.
 *
 * @param name method name
 * @param descriptor method raw bytecode descriptor
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public record MethodSignatureKey(
        String name,
        String descriptor
) {
    public MethodSignatureKey {
        Objects.requireNonNull(name, "name cannot be null");
        Objects.requireNonNull(descriptor, "descriptor cannot be null");
    }
}
