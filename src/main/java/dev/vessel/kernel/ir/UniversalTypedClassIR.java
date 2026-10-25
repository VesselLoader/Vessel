package dev.vessel.kernel.ir;

import java.util.List;
import java.util.Objects;

/**
 * Immutable carrier representing a normalized, loader-agnostic class declaration in Universal Typed IR.
 * Directly satisfies ERR-21 and MAP-011..MAP-020.
 *
 * @param internalName class internal name (slash separated)
 * @param superName superclass internal name, or null if Object/interface
 * @param interfaces implemented interface internal names
 * @param signature generic signature of the class, or null
 * @param accessFlags JVM class access bitmask
 * @param fields declared field metadata list
 * @param methods declared method metadata list
 * @param recordComponents record components if Java record, else empty
 * @param permittedSubclasses permitted subclasses if sealed class, else empty
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public record UniversalTypedClassIR(
        String internalName,
        String superName,
        List<String> interfaces,
        String signature,
        int accessFlags,
        List<UniversalTypedFieldIR> fields,
        List<UniversalTypedMethodIR> methods,
        List<UniversalTypedRecordComponentIR> recordComponents,
        List<String> permittedSubclasses
) {
    public UniversalTypedClassIR {
        Objects.requireNonNull(internalName, "internalName cannot be null");
        interfaces = interfaces == null ? List.of() : List.copyOf(interfaces);
        fields = fields == null ? List.of() : List.copyOf(fields);
        methods = methods == null ? List.of() : List.copyOf(methods);
        recordComponents = recordComponents == null ? List.of() : List.copyOf(recordComponents);
        permittedSubclasses = permittedSubclasses == null ? List.of() : List.copyOf(permittedSubclasses);
    }

    public boolean isInterface() {
        return (accessFlags & org.objectweb.asm.Opcodes.ACC_INTERFACE) != 0;
    }

    public boolean isRecord() {
        return (accessFlags & org.objectweb.asm.Opcodes.ACC_RECORD) != 0;
    }

    public boolean isEnum() {
        return (accessFlags & org.objectweb.asm.Opcodes.ACC_ENUM) != 0;
    }

    public boolean isSealed() {
        return !permittedSubclasses.isEmpty();
    }
}
