package dev.vessel.kernel.ir;

import java.util.Objects;

/**
 * Immutable carrier representing a normalized field declaration in Universal Typed IR.
 *
 * @param name field name
 * @param descriptor raw JVM type descriptor
 * @param signature generic signature, or null if unparameterized
 * @param accessFlags JVM field access bitmask
 * @param constantValue constant value if static final, or null
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public record UniversalTypedFieldIR(
        String name,
        String descriptor,
        String signature,
        int accessFlags,
        Object constantValue
) {
    public UniversalTypedFieldIR {
        Objects.requireNonNull(name, "name cannot be null");
        Objects.requireNonNull(descriptor, "descriptor cannot be null");
    }

    public boolean isStatic() {
        return (accessFlags & org.objectweb.asm.Opcodes.ACC_STATIC) != 0;
    }

    public boolean isFinal() {
        return (accessFlags & org.objectweb.asm.Opcodes.ACC_FINAL) != 0;
    }

    public boolean isPublic() {
        return (accessFlags & org.objectweb.asm.Opcodes.ACC_PUBLIC) != 0;
    }
}
