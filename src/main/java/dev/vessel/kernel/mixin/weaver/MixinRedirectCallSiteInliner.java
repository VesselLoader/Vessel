package dev.vessel.kernel.mixin.weaver;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

/**
 * Rewrites matching {@code INVOKEVIRTUAL}, {@code INVOKESTATIC}, and {@code INVOKEINTERFACE}
 * call sites inside target methods in-place when a Mixin declares a {@code @Redirect} hook
 * targeting {@code @At(value = "INVOKE", target = "...")}.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public final class MixinRedirectCallSiteInliner {

    private static final String REDIRECT_DESC = "Lorg/spongepowered/asm/mixin/injection/Redirect;";

    /**
     * Scans {@code mixinNode} for {@code @Redirect}-annotated methods and rewrites matching
     * {@code INVOKE} call sites in {@code targetNode.methods} in-place.
     *
     * @param targetNode target class ASM node (must not be null)
     * @param mixinNode  mixin class ASM node (must not be null)
     */
    public void inlineRedirectHooks(ClassNode targetNode, ClassNode mixinNode) {
        Objects.requireNonNull(targetNode, "targetNode must not be null");
        Objects.requireNonNull(mixinNode, "mixinNode must not be null");
        if (targetNode.methods == null || mixinNode.methods == null) {
            return;
        }

        for (MethodNode redirectMixinMethod : mixinNode.methods) {
            AnnotationNode redirectAnn = findRedirectAnnotation(redirectMixinMethod);
            if (redirectAnn == null) {
                continue;
            }
            if ((redirectMixinMethod.access & Opcodes.ACC_STATIC) == 0) {
                continue;
            }
            List<String> targetMethodSelectors = extractTargetMethodSelectors(redirectAnn);
            AnnotationNode atNode = extractAtAnnotationNode(redirectAnn);
            if (targetMethodSelectors.isEmpty() || atNode == null) {
                continue;
            }
            String atValue = extractAtAttribute(atNode, "value", "INVOKE").toUpperCase(Locale.ROOT);
            String atTarget = extractAtAttribute(atNode, "target", "");
            if (!"INVOKE".equals(atValue) || atTarget.isBlank()) {
                continue;
            }

            ensureTargetMethodStaticPublic(targetNode, redirectMixinMethod);

            for (MethodNode targetMethod : targetNode.methods) {
                if (targetMethod.name.equals(redirectMixinMethod.name)
                        && targetMethod.desc.equals(redirectMixinMethod.desc)) {
                    continue;
                }
                if (!matchesAnyTargetSelector(targetMethod, targetMethodSelectors)) {
                    continue;
                }
                if (targetMethod.instructions == null) {
                    continue;
                }
                for (AbstractInsnNode insn = targetMethod.instructions.getFirst();
                        insn != null;
                        insn = insn.getNext()) {
                    if (insn instanceof MethodInsnNode methodInsn
                            && matchesInvokeTarget(methodInsn, atTarget)) {
                        methodInsn.setOpcode(Opcodes.INVOKESTATIC);
                        methodInsn.owner = targetNode.name;
                        methodInsn.name = redirectMixinMethod.name;
                        methodInsn.desc = redirectMixinMethod.desc;
                        methodInsn.itf = false;
                    }
                }
            }
        }
    }

    private static void ensureTargetMethodStaticPublic(
            ClassNode targetNode,
            MethodNode redirectMixinMethod) {
        for (MethodNode mn : targetNode.methods) {
            if (mn.name.equals(redirectMixinMethod.name)
                    && mn.desc.equals(redirectMixinMethod.desc)) {
                mn.access = (mn.access & ~(Opcodes.ACC_PRIVATE | Opcodes.ACC_PROTECTED))
                        | Opcodes.ACC_PUBLIC
                        | Opcodes.ACC_STATIC;
            }
        }
    }

    private static boolean matchesInvokeTarget(MethodInsnNode methodInsn, String atTarget) {
        String trimmed = atTarget.trim();
        String fullDescriptor = "L" + methodInsn.owner + ";" + methodInsn.name + methodInsn.desc;
        if (trimmed.equals(fullDescriptor)) {
            return true;
        }
        String slashDotDescriptor = methodInsn.owner + "." + methodInsn.name + methodInsn.desc;
        if (trimmed.equals(slashDotDescriptor)) {
            return true;
        }
        String binaryDotDescriptor =
                methodInsn.owner.replace('/', '.') + "." + methodInsn.name + methodInsn.desc;
        if (trimmed.equals(binaryDotDescriptor)) {
            return true;
        }
        String nameAndDesc = methodInsn.name + methodInsn.desc;
        return trimmed.equals(nameAndDesc) || trimmed.equals(methodInsn.name);
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

    private static AnnotationNode findRedirectAnnotation(MethodNode methodNode) {
        if (methodNode.visibleAnnotations != null) {
            for (AnnotationNode node : methodNode.visibleAnnotations) {
                if (REDIRECT_DESC.equals(node.desc)) {
                    return node;
                }
            }
        }
        if (methodNode.invisibleAnnotations != null) {
            for (AnnotationNode node : methodNode.invisibleAnnotations) {
                if (REDIRECT_DESC.equals(node.desc)) {
                    return node;
                }
            }
        }
        return null;
    }

    private static List<String> extractTargetMethodSelectors(AnnotationNode redirectAnn) {
        List<String> methods = new ArrayList<>();
        if (redirectAnn.values == null) {
            return methods;
        }
        for (int i = 0; i + 1 < redirectAnn.values.size(); i += 2) {
            String key = String.valueOf(redirectAnn.values.get(i));
            Object val = redirectAnn.values.get(i + 1);
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

    private static AnnotationNode extractAtAnnotationNode(AnnotationNode redirectAnn) {
        if (redirectAnn.values == null) {
            return null;
        }
        for (int i = 0; i + 1 < redirectAnn.values.size(); i += 2) {
            String key = String.valueOf(redirectAnn.values.get(i));
            Object val = redirectAnn.values.get(i + 1);
            if ("at".equals(key)) {
                if (val instanceof AnnotationNode atNode) {
                    return atNode;
                }
                if (val instanceof List<?> list && !list.isEmpty() && list.get(0) instanceof AnnotationNode atNode) {
                    return atNode;
                }
            }
        }
        return null;
    }

    private static String extractAtAttribute(
            AnnotationNode atNode,
            String attributeKey,
            String defaultValue) {
        if (atNode.values != null) {
            for (int i = 0; i + 1 < atNode.values.size(); i += 2) {
                String key = String.valueOf(atNode.values.get(i));
                Object val = atNode.values.get(i + 1);
                if (attributeKey.equals(key) && val instanceof String s && !s.isBlank()) {
                    return s;
                }
            }
        }
        return defaultValue;
    }
}
