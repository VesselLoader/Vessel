package dev.vessel.kernel.mixin.weaver;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;

/**
 * Inlines direct invocation instructions at {@code HEAD} and {@code TAIL}/{@code RETURN} injection
 * points in a target {@link ClassNode} for {@code @Inject}-annotated methods declared on a
 * {@link ClassNode} mixin.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public final class MixinHeadAndTailInstructionInliner {

    private static final String INJECT_DESC = "Lorg/spongepowered/asm/mixin/injection/Inject;";
    private static final String CALLBACK_INFO_INTERNAL =
            "org/spongepowered/asm/mixin/injection/callback/CallbackInfo";
    private static final String CALLBACK_INFO_INIT_DESC = "(Ljava/lang/String;Z)V";
    private static final String CALLBACK_INFO_METHOD_DESC =
            "(Lorg/spongepowered/asm/mixin/injection/callback/CallbackInfo;)V";

    /**
     * Inlines calls to {@code @Inject} hook methods from {@code mixinNode} into matching target
     * methods in {@code targetNode} at {@code HEAD} or {@code TAIL}/{@code RETURN} points.
     *
     * @param targetNode target class ASM node (must not be null)
     * @param mixinNode  mixin class ASM node (must not be null)
     * @return total number of inlined hook invocation sites
     */
    public int inlineNoArgInjectHooks(ClassNode targetNode, ClassNode mixinNode) {
        Objects.requireNonNull(targetNode, "targetNode must not be null");
        Objects.requireNonNull(mixinNode, "mixinNode must not be null");
        if (targetNode.methods == null || mixinNode.methods == null) {
            return 0;
        }

        int inlinedSites = 0;
        for (MethodNode mixinMethod : mixinNode.methods) {
            AnnotationNode injectAnn = findInjectAnnotation(mixinMethod);
            if (injectAnn == null) {
                continue;
            }
            boolean isNoArgVoid = "()V".equals(mixinMethod.desc);
            boolean isCallbackInfoVoid = CALLBACK_INFO_METHOD_DESC.equals(mixinMethod.desc);
            if (!isNoArgVoid && !isCallbackInfoVoid) {
                continue;
            }

            List<String> targetSelectors = extractTargetMethodSelectors(injectAnn);
            List<String> atPoints = extractAtPoints(injectAnn);
            if (targetSelectors.isEmpty()) {
                continue;
            }

            for (MethodNode targetMethod : targetNode.methods) {
                if (targetMethod.name.equals(mixinMethod.name)
                        && targetMethod.desc.equals(mixinMethod.desc)) {
                    continue;
                }
                if (!matchesAnyTargetSelector(targetMethod, targetSelectors)) {
                    continue;
                }
                for (String atPoint : atPoints) {
                    String normalizedPoint = atPoint.toUpperCase(Locale.ROOT);
                    if ("HEAD".equals(normalizedPoint)) {
                        if (inlineAtHead(targetNode.name, targetMethod, mixinMethod, isCallbackInfoVoid)) {
                            inlinedSites++;
                        }
                    } else if ("TAIL".equals(normalizedPoint) || "RETURN".equals(normalizedPoint)) {
                        inlinedSites += inlineAtReturns(
                                targetNode.name,
                                targetMethod,
                                mixinMethod,
                                isCallbackInfoVoid);
                    }
                }
            }
        }
        return inlinedSites;
    }

    private static boolean inlineAtHead(
            String targetOwner,
            MethodNode targetMethod,
            MethodNode mixinMethod,
            boolean passCallbackInfo) {
        if (targetMethod.instructions == null || targetMethod.instructions.size() == 0) {
            return false;
        }
        InsnList hookCall = buildHookInvocationList(
                targetOwner,
                targetMethod.name,
                mixinMethod,
                passCallbackInfo);
        targetMethod.instructions.insertBefore(targetMethod.instructions.getFirst(), hookCall);
        ensureAdequateMaxStackAndLocals(targetMethod, passCallbackInfo);
        return true;
    }

    private static int inlineAtReturns(
            String targetOwner,
            MethodNode targetMethod,
            MethodNode mixinMethod,
            boolean passCallbackInfo) {
        if (targetMethod.instructions == null || targetMethod.instructions.size() == 0) {
            return 0;
        }
        List<AbstractInsnNode> returnNodes = new ArrayList<>();
        for (AbstractInsnNode insn = targetMethod.instructions.getFirst();
                insn != null;
                insn = insn.getNext()) {
            int opcode = insn.getOpcode();
            if (opcode >= Opcodes.IRETURN && opcode <= Opcodes.RETURN) {
                returnNodes.add(insn);
            }
        }
        for (AbstractInsnNode retInsn : returnNodes) {
            InsnList hookCall = buildHookInvocationList(
                    targetOwner,
                    targetMethod.name,
                    mixinMethod,
                    passCallbackInfo);
            targetMethod.instructions.insertBefore(retInsn, hookCall);
        }
        if (!returnNodes.isEmpty()) {
            ensureAdequateMaxStackAndLocals(targetMethod, passCallbackInfo);
        }
        return returnNodes.size();
    }

    private static InsnList buildHookInvocationList(
            String targetOwner,
            String targetMethodName,
            MethodNode mixinMethod,
            boolean passCallbackInfo) {
        InsnList list = new InsnList();
        boolean isStatic = (mixinMethod.access & Opcodes.ACC_STATIC) != 0;
        if (!isStatic) {
            list.add(new VarInsnNode(Opcodes.ALOAD, 0));
        }
        if (passCallbackInfo) {
            list.add(new TypeInsnNode(Opcodes.NEW, CALLBACK_INFO_INTERNAL));
            list.add(new InsnNode(Opcodes.DUP));
            list.add(new LdcInsnNode(targetMethodName));
            list.add(new InsnNode(Opcodes.ICONST_0));
            list.add(new MethodInsnNode(
                    Opcodes.INVOKESPECIAL,
                    CALLBACK_INFO_INTERNAL,
                    "<init>",
                    CALLBACK_INFO_INIT_DESC,
                    false));
        }
        list.add(new MethodInsnNode(
                isStatic ? Opcodes.INVOKESTATIC : Opcodes.INVOKEVIRTUAL,
                targetOwner,
                mixinMethod.name,
                mixinMethod.desc,
                false));
        return list;
    }

    private static void ensureAdequateMaxStackAndLocals(
            MethodNode targetMethod,
            boolean passCallbackInfo) {
        int requiredStack = passCallbackInfo ? 6 : 2;
        if (targetMethod.maxStack < requiredStack) {
            targetMethod.maxStack = requiredStack;
        }
        if ((targetMethod.access & Opcodes.ACC_STATIC) == 0 && targetMethod.maxLocals < 1) {
            targetMethod.maxLocals = 1;
        }
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

    private static AnnotationNode findInjectAnnotation(MethodNode methodNode) {
        if (methodNode.visibleAnnotations != null) {
            for (AnnotationNode node : methodNode.visibleAnnotations) {
                if (INJECT_DESC.equals(node.desc)) {
                    return node;
                }
            }
        }
        if (methodNode.invisibleAnnotations != null) {
            for (AnnotationNode node : methodNode.invisibleAnnotations) {
                if (INJECT_DESC.equals(node.desc)) {
                    return node;
                }
            }
        }
        return null;
    }

    private static List<String> extractTargetMethodSelectors(AnnotationNode injectAnn) {
        List<String> methods = new ArrayList<>();
        if (injectAnn.values == null) {
            return methods;
        }
        for (int i = 0; i + 1 < injectAnn.values.size(); i += 2) {
            String key = String.valueOf(injectAnn.values.get(i));
            Object val = injectAnn.values.get(i + 1);
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

    private static List<String> extractAtPoints(AnnotationNode injectAnn) {
        List<String> points = new ArrayList<>();
        if (injectAnn.values != null) {
            for (int i = 0; i + 1 < injectAnn.values.size(); i += 2) {
                String key = String.valueOf(injectAnn.values.get(i));
                Object val = injectAnn.values.get(i + 1);
                if ("at".equals(key)) {
                    if (val instanceof List<?> list) {
                        for (Object item : list) {
                            if (item instanceof AnnotationNode atNode) {
                                points.add(extractAtValue(atNode));
                            }
                        }
                    } else if (val instanceof AnnotationNode atNode) {
                        points.add(extractAtValue(atNode));
                    }
                }
            }
        }
        if (points.isEmpty()) {
            points.add("HEAD");
        }
        return points;
    }

    private static String extractAtValue(AnnotationNode atNode) {
        if (atNode.values != null) {
            for (int i = 0; i + 1 < atNode.values.size(); i += 2) {
                String key = String.valueOf(atNode.values.get(i));
                Object val = atNode.values.get(i + 1);
                if ("value".equals(key) && val instanceof String s && !s.isBlank()) {
                    return s;
                }
            }
        }
        return "HEAD";
    }
}
