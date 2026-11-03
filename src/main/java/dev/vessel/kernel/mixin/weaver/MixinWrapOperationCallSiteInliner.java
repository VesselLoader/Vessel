package dev.vessel.kernel.mixin.weaver;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

/**
 * Inlines call-site interception for methods annotated with MixinExtras {@code @WrapOperation}.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public final class MixinWrapOperationCallSiteInliner {

    private static final String WRAP_OP_DESC = "Lcom/llamalad7/mixinextras/injector/wrapoperation/WrapOperation;";

    public int inlineWrapOperationHooks(ClassNode targetNode, ClassNode mixinNode) {
        Objects.requireNonNull(targetNode, "targetNode must not be null");
        Objects.requireNonNull(mixinNode, "mixinNode must not be null");
        if (targetNode.methods == null || mixinNode.methods == null) {
            return 0;
        }

        int inlinedCount = 0;
        for (MethodNode mixinMethod : mixinNode.methods) {
            AnnotationNode wrapAnn = findAnnotation(mixinMethod, WRAP_OP_DESC);
            if (wrapAnn == null) {
                continue;
            }
            List<String> targetSelectors = extractMethodSelectors(wrapAnn);
            String atTarget = extractAtTarget(wrapAnn);
            if (targetSelectors.isEmpty() || atTarget == null) {
                continue;
            }

            for (MethodNode targetMethod : targetNode.methods) {
                if (!matchesTarget(targetMethod, targetSelectors)) {
                    continue;
                }
                for (AbstractInsnNode insn = targetMethod.instructions.getFirst(); insn != null; insn = insn.getNext()) {
                    if (insn instanceof MethodInsnNode methodInsn && matchesInvokeTarget(methodInsn, atTarget)) {
                        if ((mixinMethod.access & Opcodes.ACC_STATIC) != 0) {
                            methodInsn.setOpcode(Opcodes.INVOKESTATIC);
                            methodInsn.owner = targetNode.name;
                            methodInsn.name = mixinMethod.name;
                            methodInsn.itf = false;
                            inlinedCount++;
                        }
                    }
                }
            }
        }
        return inlinedCount;
    }

    private static AnnotationNode findAnnotation(MethodNode mn, String desc) {
        if (mn.visibleAnnotations != null) {
            for (AnnotationNode an : mn.visibleAnnotations) {
                if (desc.equals(an.desc)) return an;
            }
        }
        if (mn.invisibleAnnotations != null) {
            for (AnnotationNode an : mn.invisibleAnnotations) {
                if (desc.equals(an.desc)) return an;
            }
        }
        return null;
    }

    private static List<String> extractMethodSelectors(AnnotationNode ann) {
        List<String> list = new ArrayList<>();
        if (ann == null || ann.values == null) return list;
        for (int i = 0; i < ann.values.size(); i += 2) {
            if ("method".equals(ann.values.get(i))) {
                Object val = ann.values.get(i + 1);
                if (val instanceof List<?> l) {
                    for (Object elem : l) if (elem != null) list.add(elem.toString());
                } else if (val instanceof String s) {
                    list.add(s);
                }
            }
        }
        return list;
    }

    private static String extractAtTarget(AnnotationNode ann) {
        if (ann == null || ann.values == null) return null;
        for (int i = 0; i < ann.values.size(); i += 2) {
            if ("at".equals(ann.values.get(i))) {
                Object val = ann.values.get(i + 1);
                if (val instanceof AnnotationNode atAnn && atAnn.values != null) {
                    for (int j = 0; j < atAnn.values.size(); j += 2) {
                        if ("target".equals(atAnn.values.get(j))) {
                            Object tVal = atAnn.values.get(j + 1);
                            return tVal != null ? tVal.toString() : null;
                        }
                    }
                }
            }
        }
        return null;
    }

    private static boolean matchesTarget(MethodNode mn, List<String> selectors) {
        String name = mn.name;
        String full = mn.name + mn.desc;
        for (String sel : selectors) {
            if (sel.equals(name) || sel.equals(full)) return true;
        }
        return false;
    }

    private static boolean matchesInvokeTarget(MethodInsnNode insn, String target) {
        String fullDesc = "L" + insn.owner + ";" + insn.name + insn.desc;
        String dotted = insn.owner + "." + insn.name + insn.desc;
        String nameDesc = insn.name + insn.desc;
        return target.equals(fullDesc) || target.equals(dotted) || target.equals(nameDesc) || target.equals(insn.name);
    }
}
