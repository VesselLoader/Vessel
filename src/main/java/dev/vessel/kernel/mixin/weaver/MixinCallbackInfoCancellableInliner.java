package dev.vessel.kernel.mixin.weaver;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;

/**
 * Inlines {@code CallbackInfo}-receiving {@code @Inject} hooks (including {@code cancellable = true}
 * early-return branches) at the {@code HEAD} of target methods in a {@link ClassNode}.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public final class MixinCallbackInfoCancellableInliner {

    private static final String INJECT_DESC = "Lorg/spongepowered/asm/mixin/injection/Inject;";
    private static final String CALLBACK_INFO_INTERNAL =
            "org/spongepowered/asm/mixin/injection/callback/CallbackInfo";
    private static final String CALLBACK_INFO_INIT_DESC = "(Ljava/lang/String;Z)V";
    private static final String CALLBACK_INFO_METHOD_DESC =
            "(Lorg/spongepowered/asm/mixin/injection/callback/CallbackInfo;)V";

    /**
     * Inlines {@code (CallbackInfo)V} {@code @Inject} hooks from {@code mixinNode} into matching
     * {@code ()V} target methods in {@code targetNode}, synthesizing conditional early-return
     * bytecode when {@code cancellable = true}.
     *
     * @param targetNode target class ASM node (must not be null)
     * @param mixinNode  mixin class ASM node (must not be null)
     * @return total number of inlined {@code CallbackInfo} hook sites
     */
    public int inlineCallbackInfoInjectHooks(ClassNode targetNode, ClassNode mixinNode) {
        Objects.requireNonNull(targetNode, "targetNode must not be null");
        Objects.requireNonNull(mixinNode, "mixinNode must not be null");
        if (targetNode.methods == null || mixinNode.methods == null) {
            return 0;
        }

        int inlinedSites = 0;
        for (MethodNode mixinMethod : mixinNode.methods) {
            if (!CALLBACK_INFO_METHOD_DESC.equals(mixinMethod.desc)) {
                continue;
            }
            AnnotationNode injectAnn = findInjectAnnotation(mixinMethod);
            if (injectAnn == null) {
                continue;
            }
            List<String> targetSelectors = extractTargetMethodSelectors(injectAnn);
            List<String> atPoints = extractAtPoints(injectAnn);
            boolean cancellable = extractCancellableFlag(injectAnn);
            if (targetSelectors.isEmpty()) {
                continue;
            }

            for (MethodNode targetMethod : targetNode.methods) {
                if (!"()V".equals(targetMethod.desc)) {
                    continue;
                }
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
                        stripPreliminaryInlineIfPresent(
                                targetMethod,
                                targetNode.name,
                                mixinMethod.name);
                        if (inlineCancellableAtHead(
                                targetNode.name,
                                targetMethod,
                                mixinMethod,
                                cancellable)) {
                            inlinedSites++;
                        }
                    }
                }
            }
        }
        return inlinedSites;
    }

    private static boolean inlineCancellableAtHead(
            String targetOwner,
            MethodNode targetMethod,
            MethodNode mixinMethod,
            boolean cancellable) {
        if (targetMethod.instructions == null || targetMethod.instructions.size() == 0) {
            return false;
        }
        int minLocals = Type.getArgumentsAndReturnSizes(targetMethod.desc) >> 2;
        if ((targetMethod.access & Opcodes.ACC_STATIC) != 0) {
            minLocals -= 1;
        }
        if (targetMethod.maxLocals < minLocals) {
            targetMethod.maxLocals = minLocals;
        }
        int ciSlot = targetMethod.maxLocals;
        targetMethod.maxLocals += 1;

        boolean isStaticMixin = (mixinMethod.access & Opcodes.ACC_STATIC) != 0;
        InsnList list = new InsnList();
        list.add(new TypeInsnNode(Opcodes.NEW, CALLBACK_INFO_INTERNAL));
        list.add(new InsnNode(Opcodes.DUP));
        list.add(new LdcInsnNode(targetMethod.name));
        list.add(new InsnNode(cancellable ? Opcodes.ICONST_1 : Opcodes.ICONST_0));
        list.add(new MethodInsnNode(
                Opcodes.INVOKESPECIAL,
                CALLBACK_INFO_INTERNAL,
                "<init>",
                CALLBACK_INFO_INIT_DESC,
                false));
        list.add(new VarInsnNode(Opcodes.ASTORE, ciSlot));

        if (!isStaticMixin) {
            list.add(new VarInsnNode(Opcodes.ALOAD, 0));
        }
        list.add(new VarInsnNode(Opcodes.ALOAD, ciSlot));
        list.add(new MethodInsnNode(
                isStaticMixin ? Opcodes.INVOKESTATIC : Opcodes.INVOKEVIRTUAL,
                targetOwner,
                mixinMethod.name,
                CALLBACK_INFO_METHOD_DESC,
                false));

        if (cancellable) {
            LabelNode continueLabel = new LabelNode();
            list.add(new VarInsnNode(Opcodes.ALOAD, ciSlot));
            list.add(new MethodInsnNode(
                    Opcodes.INVOKEVIRTUAL,
                    CALLBACK_INFO_INTERNAL,
                    "isCancelled",
                    "()Z",
                    false));
            list.add(new JumpInsnNode(Opcodes.IFEQ, continueLabel));
            list.add(new InsnNode(Opcodes.RETURN));
            list.add(continueLabel);
        }

        targetMethod.instructions.insertBefore(targetMethod.instructions.getFirst(), list);
        targetMethod.maxStack = Math.max(targetMethod.maxStack, 4);
        return true;
    }

    private static void stripPreliminaryInlineIfPresent(
            MethodNode targetMethod,
            String targetOwner,
            String mixinMethodName) {
        if (targetMethod.instructions == null) {
            return;
        }
        for (AbstractInsnNode insn = targetMethod.instructions.getFirst();
                insn != null;
                insn = insn.getNext()) {
            if (insn instanceof MethodInsnNode callNode
                    && targetOwner.equals(callNode.owner)
                    && mixinMethodName.equals(callNode.name)
                    && CALLBACK_INFO_METHOD_DESC.equals(callNode.desc)) {
                AbstractInsnNode prev = callNode.getPrevious();
                if (prev instanceof MethodInsnNode initNode
                        && CALLBACK_INFO_INTERNAL.equals(initNode.owner)
                        && "<init>".equals(initNode.name)) {
                    List<AbstractInsnNode> toRemove = new ArrayList<>();
                    toRemove.add(callNode);
                    AbstractInsnNode cursor = prev;
                    for (int step = 0; step < 5 && cursor != null; step++) {
                        toRemove.add(cursor);
                        cursor = cursor.getPrevious();
                    }
                    if (cursor instanceof VarInsnNode varNode
                            && varNode.getOpcode() == Opcodes.ALOAD
                            && varNode.var == 0) {
                        toRemove.add(varNode);
                    }
                    for (AbstractInsnNode rem : toRemove) {
                        targetMethod.instructions.remove(rem);
                    }
                    return;
                }
            }
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

    private static boolean extractCancellableFlag(AnnotationNode injectAnn) {
        if (injectAnn.values == null) {
            return false;
        }
        for (int i = 0; i + 1 < injectAnn.values.size(); i += 2) {
            String key = String.valueOf(injectAnn.values.get(i));
            Object val = injectAnn.values.get(i + 1);
            if ("cancellable".equals(key) && val instanceof Boolean b) {
                return b;
            }
        }
        return false;
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
