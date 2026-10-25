package dev.vessel.kernel.vstm.remap;

import dev.vessel.kernel.vstm.builder.VstmTransmutationContext;
import java.util.Objects;
import org.objectweb.asm.commons.Remapper;

/**
 * High-performance ASM Remapper bridge delegating bytecode symbol translations
 * directly into the immutable {@link VstmTransmutationContext}.
 *
 * <p>Translates class names, method invocations, field references, and record components
 * between source and target dialect spaces at $O(1)$ lookup velocity (Catalog ref: FOL-0040, USML-0021).
 * Supports dual-dialect owner and descriptor resolution across obfuscated and clean namespaces.</p>
 *
 * @version 1.0.1
 * @since 1.0.0 (Wave 3)
 */
public final class VstmAsmRemapperBridge extends Remapper {

    private final VstmTransmutationContext context;

    /**
     * Constructs a new ASM remapper bridge tied to a transmutation context.
     *
     * @param context the active module transmutation context
     */
    public VstmAsmRemapperBridge(VstmTransmutationContext context) {
        this.context = Objects.requireNonNull(context, "context must not be null");
    }

    /**
     * Remaps an internal class name from source dialect to target dialect.
     *
     * @param internalName internal class name (e.g. "net/minecraft/class_1937")
     * @return remapped internal class name
     */
    @Override
    public String map(String internalName) {
        if (internalName == null || internalName.isEmpty()) {
            return internalName;
        }
        String remapped = context.remapClass(internalName);
        return remapped != null ? remapped : internalName;
    }

    /**
     * Remaps a method name using the active transmutation engine with dual-dialect owner resolution.
     *
     * @param owner      the owner class internal name
     * @param name       the method name
     * @param descriptor the method descriptor
     * @return remapped method name
     */
    @Override
    public String mapMethodName(String owner, String name, String descriptor) {
        if (name == null || name.isEmpty()) {
            return name;
        }

        // 1. Direct lookup with provided signature
        String remapped = context.remapMethod(owner, name, descriptor);
        if (remapped != null && !remapped.equals(name)) {
            return remapped;
        }

        // 2. Dual-dialect resolution: attempt lookup with mapped owner and mapped descriptor
        String mappedOwner = map(owner);
        boolean ownerChanged = mappedOwner != null && !mappedOwner.equals(owner);

        if (ownerChanged) {
            String candidate = context.remapMethod(mappedOwner, name, descriptor);
            if (candidate != null && !candidate.equals(name)) {
                return candidate;
            }
        }

        String mappedDesc = mapMethodDesc(descriptor);
        boolean descChanged = mappedDesc != null && !mappedDesc.equals(descriptor);

        if (ownerChanged && descChanged) {
            String candidate = context.remapMethod(mappedOwner, name, mappedDesc);
            if (candidate != null && !candidate.equals(name)) {
                return candidate;
            }
        } else if (descChanged) {
            String candidate = context.remapMethod(owner, name, mappedDesc);
            if (candidate != null && !candidate.equals(name)) {
                return candidate;
            }
        }

        return remapped != null ? remapped : name;
    }

    /**
     * Remaps a field name using the active transmutation engine with dual-dialect owner resolution.
     *
     * @param owner      the owner class internal name
     * @param name       the field name
     * @param descriptor the field descriptor
     * @return remapped field name
     */
    @Override
    public String mapFieldName(String owner, String name, String descriptor) {
        if (name == null || name.isEmpty()) {
            return name;
        }

        // 1. Direct lookup with provided signature
        String remapped = context.remapField(owner, name, descriptor);
        if (remapped != null && !remapped.equals(name)) {
            return remapped;
        }

        // 2. Dual-dialect resolution: attempt lookup with mapped owner and mapped descriptor
        String mappedOwner = map(owner);
        boolean ownerChanged = mappedOwner != null && !mappedOwner.equals(owner);

        if (ownerChanged) {
            String candidate = context.remapField(mappedOwner, name, descriptor);
            if (candidate != null && !candidate.equals(name)) {
                return candidate;
            }
        }

        String mappedDesc = mapDesc(descriptor);
        boolean descChanged = mappedDesc != null && !mappedDesc.equals(descriptor);

        if (ownerChanged && descChanged) {
            String candidate = context.remapField(mappedOwner, name, mappedDesc);
            if (candidate != null && !candidate.equals(name)) {
                return candidate;
            }
        } else if (descChanged) {
            String candidate = context.remapField(owner, name, mappedDesc);
            if (candidate != null && !candidate.equals(name)) {
                return candidate;
            }
        }

        return remapped != null ? remapped : name;
    }

    /**
     * Remaps a record component name, treating it consistently with field mappings.
     *
     * @param owner      the owner class internal name
     * @param name       the record component name
     * @param descriptor the record component descriptor
     * @return remapped component name
     */
    @Override
    public String mapRecordComponentName(String owner, String name, String descriptor) {
        return mapFieldName(owner, name, descriptor);
    }

    /**
     * Remaps an invokedynamic method callsite name.
     *
     * @param name       the invokedynamic method name
     * @param descriptor the invokedynamic descriptor
     * @return remapped method name
     */
    @Override
    public String mapInvokeDynamicMethodName(String name, String descriptor) {
        if (name == null || name.isEmpty()) {
            return name;
        }
        return name;
    }

    /**
     * Retrieves the underlying active transmutation context.
     *
     * @return the transmutation context
     */
    public VstmTransmutationContext getContext() {
        return context;
    }
}
