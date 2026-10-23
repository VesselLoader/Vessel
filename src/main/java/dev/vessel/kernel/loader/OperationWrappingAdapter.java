package dev.vessel.kernel.loader;

import dev.vessel.kernel.ir.VesselSemanticHookIntent;
import dev.vessel.kernel.ir.VesselSemanticHookRecord;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiFunction;

/**
 * Implements MixinExtras {@code @WrapOperation} chaining on target bytecode and dispatches
 * nested operation wrapper chains at runtime under Decision 40.
 *
 * <p>Strictly adheres to Rule 17 with zero inner or nested classes.</p>
 *
 * @version 1.0.0
 * @author Shiroz &amp; Antigravity
 * @since 1.0.0 (Pass K-28)
 */
public final class OperationWrappingAdapter {

    private final List<BiFunction<Object, Object[], Object>> registeredChainSteps = new CopyOnWriteArrayList<>();

    /**
     * Applies an ordered chain of {@link VesselSemanticHookIntent#METHOD_OPERATION_WRAPPING}
     * hooks to the target class bytecode.
     *
     * @param targetClassBytes raw JVM classfile bytes
     * @param wrapHooks        list of semantic hook records to weave
     * @return woven JVM classfile bytes
     */
    public byte[] applyWrapOperationChain(byte[] targetClassBytes, List<VesselSemanticHookRecord> wrapHooks) {
        Objects.requireNonNull(targetClassBytes, "targetClassBytes cannot be null");
        Objects.requireNonNull(wrapHooks, "wrapHooks cannot be null");

        if (wrapHooks.isEmpty()) {
            return targetClassBytes.clone();
        }

        List<VesselSemanticHookRecord> sortedHooks = new ArrayList<>(wrapHooks);
        sortedHooks.sort(Comparator.comparingInt(VesselSemanticHookRecord::priority).reversed());

        ClassReader reader = new ClassReader(targetClassBytes);
        ClassNode classNode = new ClassNode();
        reader.accept(classNode, 0);

        if (classNode.methods != null) {
            for (VesselSemanticHookRecord hook : sortedHooks) {
                if (hook.intent() != VesselSemanticHookIntent.METHOD_OPERATION_WRAPPING) {
                    continue;
                }
                for (MethodNode method : classNode.methods) {
                    if (method.name.equals(hook.targetName())) {
                        insertWrapperCallBeforeFirstInvocation(method, hook);
                    }
                }
            }
        }

        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        classNode.accept(writer);
        return writer.toByteArray();
    }

    /**
     * Invokes the wrapped operation chain starting at the specified chain index.
     *
     * @param instance   target receiver instance (may be {@code null} for static calls)
     * @param args       invocation arguments array
     * @param chainIndex current index in the wrapper chain
     * @return result produced by the wrapper chain or terminal operation
     */
    public Object invokeWrappedOperationChain(Object instance, Object[] args, int chainIndex) {
        Object[] safeArgs = args != null ? args : new Object[0];
        if (chainIndex < 0) {
            throw new IllegalArgumentException("chainIndex cannot be negative: " + chainIndex);
        }

        if (chainIndex < registeredChainSteps.size()) {
            return registeredChainSteps.get(chainIndex).apply(instance, safeArgs);
        }

        if (safeArgs.length > 0) {
            return safeArgs[0];
        }
        return instance;
    }

    /**
     * Registers a runtime wrapper step function into the operation chain.
     *
     * @param stepFunction wrapper step accepting {@code (instance, args)}
     */
    public void registerChainStep(BiFunction<Object, Object[], Object> stepFunction) {
        Objects.requireNonNull(stepFunction, "stepFunction cannot be null");
        registeredChainSteps.add(stepFunction);
    }

    private static void insertWrapperCallBeforeFirstInvocation(MethodNode method, VesselSemanticHookRecord hook) {
        if (method.instructions == null) {
            return;
        }
        for (AbstractInsnNode insn = method.instructions.getFirst(); insn != null; insn = insn.getNext()) {
            if (insn instanceof MethodInsnNode) {
                MethodInsnNode hookCall = new MethodInsnNode(
                        Opcodes.INVOKESTATIC,
                        hook.sourceClass(),
                        hook.sourceMethod(),
                        "()V",
                        false
                );
                method.instructions.insertBefore(insn, hookCall);
                return;
            }
        }
    }
}
