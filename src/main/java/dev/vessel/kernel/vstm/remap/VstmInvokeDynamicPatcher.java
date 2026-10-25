package dev.vessel.kernel.vstm.remap;

import java.util.Objects;
import org.objectweb.asm.Handle;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.Remapper;

/**
 * ASM MethodVisitor specializing in the inspection and patching of {@code invokedynamic} instructions.
 *
 * <p>Translates callsite descriptors, bootstrap handles, and constant arguments for
 * dynamic invocations (notably {@code LambdaMetafactory} and {@code StringConcatFactory})
 * ensuring bytecode remains fully verifiable and executable across remapped ecosystems.</p>
 *
 * @version 1.0.0
 * @since 1.0.0 (Wave 3)
 */
public final class VstmInvokeDynamicPatcher extends MethodVisitor {

    private final Remapper remapper;
    private final VstmBootstrapMethodPatcher patcher;

    /**
     * Constructs an invokedynamic patcher using default ASM9 version and a new bootstrap patcher.
     *
     * @param methodVisitor the downstream method visitor
     * @param remapper      the active remapper
     */
    public VstmInvokeDynamicPatcher(MethodVisitor methodVisitor, Remapper remapper) {
        this(Opcodes.ASM9, methodVisitor, remapper, new VstmBootstrapMethodPatcher());
    }

    /**
     * Constructs an invokedynamic patcher with a custom bootstrap patcher.
     *
     * @param methodVisitor the downstream method visitor
     * @param remapper      the active remapper
     * @param patcher       the bootstrap method patcher helper
     */
    public VstmInvokeDynamicPatcher(
            MethodVisitor methodVisitor,
            Remapper remapper,
            VstmBootstrapMethodPatcher patcher
    ) {
        this(Opcodes.ASM9, methodVisitor, remapper, patcher);
    }

    /**
     * Constructs an invokedynamic patcher with full configuration parameters.
     *
     * @param api           the ASM API version
     * @param methodVisitor the downstream method visitor
     * @param remapper      the active remapper
     * @param patcher       the bootstrap method patcher helper
     */
    public VstmInvokeDynamicPatcher(
            int api,
            MethodVisitor methodVisitor,
            Remapper remapper,
            VstmBootstrapMethodPatcher patcher
    ) {
        super(api, methodVisitor);
        this.remapper = Objects.requireNonNull(remapper, "remapper must not be null");
        this.patcher = Objects.requireNonNull(patcher, "patcher must not be null");
    }

    @Override
    public void visitInvokeDynamicInsn(
            String name,
            String descriptor,
            Handle bootstrapMethodHandle,
            Object... bootstrapMethodArguments
    ) {
        String mappedName = remapper.mapInvokeDynamicMethodName(name, descriptor);
        if (mappedName == null || mappedName.isEmpty()) {
            mappedName = name;
        }

        String mappedDesc = remapper.mapMethodDesc(descriptor);
        Handle mappedBsm = patcher.patchHandle(bootstrapMethodHandle, remapper);
        Object[] mappedArgs = patcher.patchArguments(bootstrapMethodHandle, bootstrapMethodArguments, remapper);

        super.visitInvokeDynamicInsn(mappedName, mappedDesc, mappedBsm, mappedArgs);
    }

    /**
     * Retrieves the active bytecode remapper.
     *
     * @return the remapper
     */
    public Remapper getRemapper() {
        return remapper;
    }

    /**
     * Retrieves the bootstrap method patcher.
     *
     * @return the bootstrap patcher
     */
    public VstmBootstrapMethodPatcher getPatcher() {
        return patcher;
    }
}
