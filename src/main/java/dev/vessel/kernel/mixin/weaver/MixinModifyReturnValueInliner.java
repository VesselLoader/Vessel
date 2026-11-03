package dev.vessel.kernel.mixin.weaver;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

/**
 * Inlines MixinExtras {@code @ModifyReturnValue} interceptor calls immediately prior to every
 * value-returning instruction ({@code IRETURN}, {@code LRETURN}, {@code FRETURN}, {@code DRETURN},
 * {@code ARETURN}) in matching target methods of a {@link ClassNode}.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public final class MixinModifyReturnValueInliner {

    private static final String MODIFY_RETURN_VALUE_DESC =
            "Lcom/llamalad7/mixinextras/injector/ModifyReturnValue;";

    /**
     * Inlines {@code @ModifyReturnValue} hooks from {@code mixinNode} into matching target methods
     * in {@code targetNode}.
     *
     * @param targetNode target class ASM node (must not be null)
     * @param mixinNode  mixin class ASM node (must not be null)
     * @return total number of inlined {@code @ModifyReturnValue} return sites
     */
    public int inlineModifyReturnValueHooks(ClassNode targetNode, ClassNode mixinNode) {
        Objects.requireNonNull(targetNode, "targetNode must not be null");
        Objects.requireNonNull(mixinNode, "mixinNode must not be null");
        if (targetNode.methods == null || mixinNode.methods == null) {
            return 0;
        }

        int inlinedSites = 0;
        for (MethodNode mixinMethod : mixinNode.methods) {
            AnnotationNode modifyAnn = findModifyReturnValueAnnotation(mixinMethod);
            if (modifyAnn == null) {
                continue;
            }
            Type[] argTypes = Type.getArgumentTypes(mixinMethod.desc);
            Type retType = Type.getReturnType(mixinMethod.desc);
            if (argTypes.length != 1
                    || retType.getSort() == Type.VOID
                    || !argTypes[0].equals(retType)) {
                continue;
            }

            List<String> targetSelectors = extractTargetMethodSelectors(modifyAnn);
            if (targetSelectors.isEmpty()) {
                continue;
            }

            for (MethodNode targetMethod : targetNode.methods) {
                if (targetMethod.name.equals(mixinMethod.name)
                        && targetMethod.desc.equals(mixinMethod.desc)) {
                    continue;
                }
                if (!Type.getReturnType(targetMethod.desc).equals(retType)) {
                    continue;
                }
                if (!matchesAnyTargetSelector(targetMethod, targetSelectors)) {
                    continue;
                }
                inlinedSites += inlineBeforeValueReturns(
                        targetNode.name,
                        targetMethod,
                        mixinMethod);
            }
        }
        return inlinedSites;
    }

    private static int inlineBeforeValueReturns(
            String targetOwner,
            MethodNode targetMethod,
            MethodNode mixinMethod) {
        if (targetMethod.instructions == null || targetMethod.instructions.size() == 0) {
            return 0;
        }
        List<AbstractInsnNode> returnNodes = new ArrayList<>();
        for (AbstractInsnNode insn = targetMethod.instructions.getFirst();
                insn != null;
                insn = insn.getNext()) {
            int opcode = insn.getOpcode();
            if (opcode >= Opcodes.IRETURN && opcode <= Opcodes.ARETURN) {
                returnNodes.add(insn);
            }
        }
        if (returnNodes.isEmpty()) {
            return 0;
        }

        boolean isStaticMixin = (mixinMethod.access & Opcodes.ACC_STATIC) != 0;
        if (isStaticMixin) {
            for (AbstractInsnNode retInsn : returnNodes) {
                MethodInsnNode staticCall = new MethodInsnNode(
                        Opcodes.INVOKESTATIC,
                        targetOwner,
                        mixinMethod.name,
                        mixinMethod.desc,
                        false);
                targetMethod.instructions.insertBefore(retInsn, staticCall);
            }
            targetMethod.maxStack = Math.max(targetMethod.maxStack, 2);
            return returnNodes.size();
        }

        int minLocals = Type.getArgumentsAndReturnSizes(targetMethod.desc) >> 2;
        if ((targetMethod.access & Opcodes.ACC_STATIC) != 0) {
            minLocals -= 1;
        }
        if (targetMethod.maxLocals < minLocals) {
            targetMethod.maxLocals = minLocals;
        }
        int retSlot = targetMethod.maxLocals;
        targetMethod.maxLocals += 2;

        for (AbstractInsnNode retInsn : returnNodes) {
            int retOpcode = retInsn.getOpcode();
            int storeOpcode = toStoreOpcode(retOpcode);
            int loadOpcode = toLoadOpcode(retOpcode);

            InsnList list = new InsnList();
            list.add(new VarInsnNode(storeOpcode, retSlot));
            list.add(new VarInsnNode(Opcodes.ALOAD, 0));
            list.add(new VarInsnNode(loadOpcode, retSlot));
            list.add(new MethodInsnNode(
                    Opcodes.INVOKEVIRTUAL,
                    targetOwner,
                    mixinMethod.name,
                    mixinMethod.desc,
                    false));
            targetMethod.instructions.insertBefore(retInsn, list);
        }
        targetMethod.maxStack = Math.max(targetMethod.maxStack, 4);
        return returnNodes.size();
    }

    private static int toStoreOpcode(int returnOpcode) {
        return switch (returnOpcode) {
            case Opcodes.LRETURN -> Opcodes.LSTORE;
            case Opcodes.FRETURN -> Opcodes.FSTORE;
            case Opcodes.DRETURN -> Opcodes.DSTORE;
            case Opcodes.ARETURN -> Opcodes.ASTORE;
            default -> Opcodes.ISTORE;
        };
    }

    private static int toLoadOpcode(int returnOpcode) {
        return switch (returnOpcode) {
            case Opcodes.LRETURN -> Opcodes.LLOAD;
            case Opcodes.FRETURN -> Opcodes.FLOAD;
            case Opcodes.DRETURN -> Opcodes.DLOAD;
            case Opcodes.ARETURN -> Opcodes.ALOAD;
            default -> Opcodes.ILOAD;
        };
    }

    private static boolean matchesAnyTargetSelector(
            MethodNode targetMethod,
            List<String> selectors) {
        String fullKey = targetMethod.name + targetMethod.desc;
        for (String selector : selectors) {
            if (selector == null || selector.isBlank()) {
                continue;
            }
            String trimmed = selector.trim();
            if (trimmed.equals(targetMethod.name) || trimmed.equals(fullKey)) {
                return true;
            }
            int parenIdx = trimmed.indexOf('(');
            if (parenIdx > 0 && trimmed.substring(0, parenIdx).equals(targetMethod.name)) {
                return true;
            }
        }
        return false;
    }

    private static AnnotationNode findModifyReturnValueAnnotation(MethodNode methodNode) {
        if (methodNode.visibleAnnotations != null) {
            for (AnnotationNode node : methodNode.visibleAnnotations) {
                if (MODIFY_RETURN_VALUE_DESC.equals(node.desc)) {
                    return node;
                }
            }
        }
        if (methodNode.invisibleAnnotations != null) {
            for (AnnotationNode node : methodNode.invisibleAnnotations) {
                if (MODIFY_RETURN_VALUE_DESC.equals(node.desc)) {
                    return node;
                }
            }
        }
        return null;
    }

    private static List<String> extractTargetMethodSelectors(AnnotationNode modifyAnn) {
        List<String> methods = new ArrayList<>();
        if (modifyAnn.values == null) {
            return methods;
        }
        for (int i = 0; i + 1 < modifyAnn.values.size(); i += 2) {
            String key = String.valueOf(modifyAnn.values.get(i));
            Object val = modifyAnn.values.get(i + 1);
            if ("method".equals(key)) {
                if (val instanceof List<?> list) {
                    for (Object item : list) {
                        if (item instanceof String s && !s.isBlank()) {
                            methods.add(s);
                        }
                    }
                } else if (val instanceof String s && !s.isBlank()) {
                    methods.add(s);
                }
            }
        }
        return methods;
    }
}
