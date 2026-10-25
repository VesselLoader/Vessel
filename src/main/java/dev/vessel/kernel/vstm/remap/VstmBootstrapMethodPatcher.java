package dev.vessel.kernel.vstm.remap;

import java.util.Objects;
import org.objectweb.asm.ConstantDynamic;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.Remapper;

/**
 * Single-responsibility patcher for ASM Bootstrap Methods (BSM) and invokedynamic arguments.
 *
 * <p>Specializes in precisely retargeting descriptors and method handles used in
 * {@code java.lang.invoke.LambdaMetafactory} and {@code java.lang.invoke.StringConcatFactory}
 * callsites across bytecode dialect boundaries (Catalog ref: USML-0021, FOL-0040).</p>
 *
 * @version 1.0.0
 * @since 1.0.0 (Wave 3)
 */
public final class VstmBootstrapMethodPatcher {

    /**
     * Constructs a bootstrap method patcher instance.
     */
    public VstmBootstrapMethodPatcher() {
        // Stateless transformation helper
    }

    /**
     * Patches an ASM method or field handle by remapping its owner, member name, and descriptor.
     *
     * @param handle   the original handle
     * @param remapper the active bytecode remapper
     * @return the remapped handle, or null if input is null
     */
    public Handle patchHandle(Handle handle, Remapper remapper) {
        if (handle == null) {
            return null;
        }
        Objects.requireNonNull(remapper, "remapper must not be null");

        String owner = handle.getOwner();
        String name = handle.getName();
        String desc = handle.getDesc();
        int tag = handle.getTag();
        boolean isInterface = handle.isInterface();

        String mappedOwner = remapper.mapType(owner);
        String mappedName;
        String mappedDesc;

        if (tag <= Opcodes.H_PUTSTATIC) {
            mappedName = remapper.mapFieldName(owner, name, desc);
            mappedDesc = remapper.mapDesc(desc);
        } else {
            mappedName = remapper.mapMethodName(owner, name, desc);
            mappedDesc = remapper.mapMethodDesc(desc);
        }

        return new Handle(
                tag,
                mappedOwner != null ? mappedOwner : owner,
                mappedName != null ? mappedName : name,
                mappedDesc != null ? mappedDesc : desc,
                isInterface
        );
    }

    /**
     * Patches an ASM Type descriptor (object, array, or method type).
     *
     * @param type     the ASM type to remap
     * @param remapper the active bytecode remapper
     * @return the remapped ASM type
     */
    public Type patchType(Type type, Remapper remapper) {
        if (type == null) {
            return null;
        }
        Objects.requireNonNull(remapper, "remapper must not be null");

        int sort = type.getSort();
        if (sort == Type.METHOD) {
            return Type.getMethodType(remapper.mapMethodDesc(type.getDescriptor()));
        } else if (sort == Type.OBJECT) {
            return Type.getObjectType(remapper.mapType(type.getInternalName()));
        } else if (sort == Type.ARRAY) {
            return Type.getType(remapper.mapDesc(type.getDescriptor()));
        }
        return type;
    }

    /**
     * Recursively patches a {@link ConstantDynamic} constant pool entry.
     *
     * @param condy    the dynamic constant entry
     * @param remapper the active bytecode remapper
     * @return the remapped dynamic constant entry
     */
    public ConstantDynamic patchConstantDynamic(ConstantDynamic condy, Remapper remapper) {
        if (condy == null) {
            return null;
        }
        Objects.requireNonNull(remapper, "remapper must not be null");

        String name = condy.getName();
        String descriptor = condy.getDescriptor();
        Handle bsm = condy.getBootstrapMethod();
        int bsmArgCount = condy.getBootstrapMethodArgumentCount();

        Object[] bsmArgs = new Object[bsmArgCount];
        for (int i = 0; i < bsmArgCount; i++) {
            bsmArgs[i] = condy.getBootstrapMethodArgument(i);
        }

        String mappedDesc = remapper.mapDesc(descriptor);
        Handle mappedBsm = patchHandle(bsm, remapper);
        Object[] mappedArgs = patchArguments(bsm, bsmArgs, remapper);

        return new ConstantDynamic(name, mappedDesc, mappedBsm, mappedArgs);
    }

    /**
     * Patches all bootstrap arguments associated with an invokedynamic instruction.
     *
     * @param bsmHandle the bootstrap method handle
     * @param bsmArgs   the original bootstrap arguments
     * @param remapper  the active bytecode remapper
     * @return an array of remapped arguments
     */
    public Object[] patchArguments(Handle bsmHandle, Object[] bsmArgs, Remapper remapper) {
        if (bsmArgs == null || bsmArgs.length == 0) {
            return bsmArgs;
        }
        Objects.requireNonNull(remapper, "remapper must not be null");

        Object[] patched = new Object[bsmArgs.length];
        for (int i = 0; i < bsmArgs.length; i++) {
            Object arg = bsmArgs[i];
            if (arg instanceof Handle handleArg) {
                patched[i] = patchHandle(handleArg, remapper);
            } else if (arg instanceof Type typeArg) {
                patched[i] = patchType(typeArg, remapper);
            } else if (arg instanceof ConstantDynamic condyArg) {
                patched[i] = patchConstantDynamic(condyArg, remapper);
            } else {
                patched[i] = arg;
            }
        }
        return patched;
    }
}
