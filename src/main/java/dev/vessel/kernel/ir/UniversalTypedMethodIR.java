package dev.vessel.kernel.ir;

import java.util.List;
import java.util.Objects;

/**
 * Immutable carrier representing a normalized method declaration in Universal Typed IR.
 *
 * @param name method name
 * @param descriptor raw JVM method descriptor
 * @param signature generic signature, or null if unparameterized
 * @param accessFlags JVM method access bitmask
 * @param exceptions declared thrown exception internal names
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public record UniversalTypedMethodIR(
        String name,
        String descriptor,
        String signature,
        int accessFlags,
        List<String> exceptions
) {
    public UniversalTypedMethodIR {
        Objects.requireNonNull(name, "name cannot be null");
        Objects.requireNonNull(descriptor, "descriptor cannot be null");
        exceptions = exceptions == null ? List.of() : List.copyOf(exceptions);
    }

    public boolean isStatic() {
        return (accessFlags & org.objectweb.asm.Opcodes.ACC_STATIC) != 0;
    }

    public boolean isBridge() {
        return (accessFlags & org.objectweb.asm.Opcodes.ACC_BRIDGE) != 0;
    }

    public boolean isSynthetic() {
        return (accessFlags & org.objectweb.asm.Opcodes.ACC_SYNTHETIC) != 0;
    }

    public boolean isAbstract() {
        return (accessFlags & org.objectweb.asm.Opcodes.ACC_ABSTRACT) != 0;
    }
}
