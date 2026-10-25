package dev.vessel.kernel.transform.mixin;

import dev.vessel.kernel.ir.VesselSemanticHookIntent;
import dev.vessel.kernel.ir.VesselSemanticHookRecord;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

import java.util.List;
import java.util.Objects;

/**
 * Bytecode transformation engine that weaves Semantic Hooks directly into ASM ClassNodes.
 * Executes in-memory ahead-of-time bytecode alterations, mapping Mixin and MixinExtras intents
 * without runtime proxy overhead.
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public final class SemanticBytecodeTransformer {

    private final FastOpcodeMatcher opcodeMatcher;

    public SemanticBytecodeTransformer(FastOpcodeMatcher opcodeMatcher) {
        this.opcodeMatcher = Objects.requireNonNull(opcodeMatcher, "opcodeMatcher cannot be null");
    }

    public SemanticBytecodeTransformer() {
        this(new FastOpcodeMatcher());
    }

    /**
     * Transforms the target ClassNode by applying all matching semantic hooks.
     *
     * @param classNode the ASM ClassNode to transform
     * @param hooks the ordered list of hooks applicable to this class
     * @return the number of hook modifications applied
     */
    public int transform(ClassNode classNode, List<VesselSemanticHookRecord> hooks) {
        Objects.requireNonNull(classNode, "classNode cannot be null");
        if (hooks == null || hooks.isEmpty()) {
            return 0;
        }

        int appliedCount = 0;
        for (VesselSemanticHookRecord hook : hooks) {
            MethodNode targetMethod = findTargetMethod(classNode, hook.targetName(), hook.targetDesc());
            if (targetMethod == null) {
                continue;
            }

            if (applyHookToMethod(targetMethod, hook)) {
                appliedCount++;
            }
        }

        return appliedCount;
    }

    private MethodNode findTargetMethod(ClassNode classNode, String name, String desc) {
        for (MethodNode method : classNode.methods) {
            if (method.name.equals(name)) {
                // If descriptor specified, verify match; otherwise accept first matching name
                if (desc == null || desc.isEmpty() || "()V".equals(desc) || method.desc.equals(desc)) {
                    return method;
                }
            }
        }
        return null;
    }

    private boolean applyHookToMethod(MethodNode method, VesselSemanticHookRecord hook) {
        VesselSemanticHookIntent intent = hook.intent();

        if (intent == VesselSemanticHookIntent.METHOD_PRE_EXECUTION) {
            AbstractInsnNode head = opcodeMatcher.findHead(method);
            if (head != null) {
                InsnList injectList = new InsnList();
                injectList.add(new MethodInsnNode(
                        Opcodes.INVOKESTATIC,
                        hook.sourceClass(),
                        hook.sourceMethod(),
                        "()V",
                        false
                ));
                method.instructions.insertBefore(head, injectList);
                return true;
            }
        } else if (intent == VesselSemanticHookIntent.METHOD_POST_EXECUTION) {
            List<AbstractInsnNode> returns = opcodeMatcher.findReturns(method);
            if (!returns.isEmpty()) {
                for (AbstractInsnNode ret : returns) {
                    InsnList injectList = new InsnList();
                    injectList.add(new MethodInsnNode(
                            Opcodes.INVOKESTATIC,
                            hook.sourceClass(),
                            hook.sourceMethod(),
                            "()V",
                            false
                    ));
                    method.instructions.insertBefore(ret, injectList);
                }
                return true;
            }
        } else if (intent == VesselSemanticHookIntent.METHOD_OPERATION_WRAPPING
                || intent == VesselSemanticHookIntent.METHOD_INVOCATION_MUTATION) {
            // Replace or wrap matching method call site
            List<MethodInsnNode> invocations = opcodeMatcher.findMethodInvocations(method, null, null);
            if (!invocations.isEmpty()) {
                MethodInsnNode targetInsn = invocations.get(0);
                method.instructions.insertBefore(targetInsn, new MethodInsnNode(
                        Opcodes.INVOKESTATIC,
                        hook.sourceClass(),
                        hook.sourceMethod(),
                        "()V",
                        false
                ));
                return true;
            }
        } else if (intent == VesselSemanticHookIntent.FIELD_ACCESS_INTERCEPTION) {
            List<FieldInsnNode> fields = opcodeMatcher.findFieldAccesses(method, null, null);
            if (!fields.isEmpty()) {
                FieldInsnNode targetInsn = fields.get(0);
                method.instructions.insertBefore(targetInsn, new MethodInsnNode(
                        Opcodes.INVOKESTATIC,
                        hook.sourceClass(),
                        hook.sourceMethod(),
                        "()V",
                        false
                ));
                return true;
            }
        }

        return false;
    }
}
