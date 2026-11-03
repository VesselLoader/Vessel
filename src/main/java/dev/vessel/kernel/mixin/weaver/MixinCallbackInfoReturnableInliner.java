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
 * Inlines {@code CallbackInfoReturnable}-receiving {@code @Inject} hooks at the {@code HEAD} of
 * non-void target methods, synthesizing conditional early-return bytecode returning the value
 * stored in {@code CallbackInfoReturnable#setReturnValue}.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public final class MixinCallbackInfoReturnableInliner {

    private static final String INJECT_DESC = "Lorg/spongepowered/asm/mixin/injection/Inject;";
    private static final String CIR_INTERNAL =
            "org/spongepowered/asm/mixin/injection/callback/CallbackInfoReturnable";
    private static final String CIR_INIT_DESC = "(Ljava/lang/String;Z)V";
    private static final String CIR_METHOD_DESC =
            "(Lorg/spongepowered/asm/mixin/injection/callback/CallbackInfoReturnable;)V";

    public int inlineReturnableInjectHooks(ClassNode targetNode, ClassNode mixinNode) {
        Objects.requireNonNull(targetNode, "targetNode must not be null");
        Objects.requireNonNull(mixinNode, "mixinNode must not be null");
        if (targetNode.methods == null || mixinNode.methods == null) {
            return 0;
        }

        int inlinedSites = 0;
        for (MethodNode mixinMethod : mixinNode.methods) {
            if (!CIR_METHOD_DESC.equals(mixinMethod.desc)) {
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
                if ("()V".equals(targetMethod.desc) || "V".equals(Type.getReturnType(targetMethod.desc).getDescriptor())) {
                    continue;
                }
                if (targetMethod.name.equals(mixinMethod.name) && targetMethod.desc.equals(mixinMethod.desc)) {
                    continue;
                }
                if (!matchesAnyTargetSelector(targetMethod, targetSelectors)) {
                    continue;
                }
                for (String atPoint : atPoints) {
                    if ("HEAD".equalsIgnoreCase(atPoint)) {
                        if (inlineReturnableAtHead(targetNode.name, targetMethod, mixinMethod, cancellable)) {
                            inlinedSites++;
                        }
                    }
                }
            }
        }
        return inlinedSites;
    }

    private static boolean inlineReturnableAtHead(
            String targetOwner,
            MethodNode targetMethod,
            MethodNode mixinMethod,
            boolean cancellable) {
        if (targetMethod.instructions == null) {
            targetMethod.instructions = new InsnList();
        }

        int cirVar = targetMethod.maxLocals;
        targetMethod.maxLocals = cirVar + 1;

        InsnList insns = new InsnList();
        insns.add(new TypeInsnNode(Opcodes.NEW, CIR_INTERNAL));
        insns.add(new InsnNode(Opcodes.DUP));
        insns.add(new LdcInsnNode(targetMethod.name));
        insns.add(new InsnNode(cancellable ? Opcodes.ICONST_1 : Opcodes.ICONST_0));
        insns.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, CIR_INTERNAL, "<init>", CIR_INIT_DESC, false));
        insns.add(new VarInsnNode(Opcodes.ASTORE, cirVar));

        boolean isStatic = (mixinMethod.access & Opcodes.ACC_STATIC) != 0;
        if (!isStatic) {
            insns.add(new VarInsnNode(Opcodes.ALOAD, 0));
        }
        insns.add(new VarInsnNode(Opcodes.ALOAD, cirVar));
        insns.add(new MethodInsnNode(
                isStatic ? Opcodes.INVOKESTATIC : Opcodes.INVOKEVIRTUAL,
                targetOwner,
                mixinMethod.name,
                mixinMethod.desc,
                false));

        if (cancellable) {
            LabelNode proceedLabel = new LabelNode();
            insns.add(new VarInsnNode(Opcodes.ALOAD, cirVar));
            insns.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, CIR_INTERNAL, "isCancelled", "()Z", false));
            insns.add(new JumpInsnNode(Opcodes.IFEQ, proceedLabel));

            Type returnType = Type.getReturnType(targetMethod.desc);
            insns.add(new VarInsnNode(Opcodes.ALOAD, cirVar));
            appendUnboxedReturn(insns, returnType);
            insns.add(proceedLabel);
        }

        targetMethod.instructions.insert(insns);
        return true;
    }

    private static void appendUnboxedReturn(InsnList insns, Type returnType) {
        int sort = returnType.getSort();
        switch (sort) {
            case Type.BOOLEAN -> {
                insns.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, CIR_INTERNAL, "getReturnValueZ", "()Z", false));
                insns.add(new InsnNode(Opcodes.IRETURN));
            }
            case Type.INT, Type.BYTE, Type.CHAR, Type.SHORT -> {
                insns.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, CIR_INTERNAL, "getReturnValueI", "()I", false));
                insns.add(new InsnNode(Opcodes.IRETURN));
            }
            case Type.LONG -> {
                insns.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, CIR_INTERNAL, "getReturnValueJ", "()J", false));
                insns.add(new InsnNode(Opcodes.LRETURN));
            }
            case Type.FLOAT -> {
                insns.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, CIR_INTERNAL, "getReturnValueF", "()F", false));
                insns.add(new InsnNode(Opcodes.FRETURN));
            }
            case Type.DOUBLE -> {
                insns.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, CIR_INTERNAL, "getReturnValueD", "()D", false));
                insns.add(new InsnNode(Opcodes.DRETURN));
            }
            default -> {
                insns.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, CIR_INTERNAL, "getReturnValue", "()Ljava/lang/Object;", false));
                insns.add(new TypeInsnNode(Opcodes.CHECKCAST, returnType.getInternalName()));
                insns.add(new InsnNode(Opcodes.ARETURN));
            }
        }
    }

    private static AnnotationNode findInjectAnnotation(MethodNode methodNode) {
        if (methodNode.visibleAnnotations != null) {
            for (AnnotationNode a : methodNode.visibleAnnotations) {
                if (INJECT_DESC.equals(a.desc)) return a;
            }
        }
        if (methodNode.invisibleAnnotations != null) {
            for (AnnotationNode a : methodNode.invisibleAnnotations) {
                if (INJECT_DESC.equals(a.desc)) return a;
            }
        }
        return null;
    }

    private static List<String> extractTargetMethodSelectors(AnnotationNode injectAnn) {
        List<String> list = new ArrayList<>();
        if (injectAnn == null || injectAnn.values == null) return list;
        for (int i = 0; i < injectAnn.values.size(); i += 2) {
            if ("method".equals(injectAnn.values.get(i))) {
                Object val = injectAnn.values.get(i + 1);
                if (val instanceof List<?> l) {
                    for (Object elem : l) {
                        if (elem != null) list.add(elem.toString());
                    }
                } else if (val instanceof String s) {
                    list.add(s);
                }
            }
        }
        return list;
    }

    private static List<String> extractAtPoints(AnnotationNode injectAnn) {
        List<String> list = new ArrayList<>();
        if (injectAnn == null || injectAnn.values == null) return list;
        for (int i = 0; i < injectAnn.values.size(); i += 2) {
            if ("at".equals(injectAnn.values.get(i))) {
                Object val = injectAnn.values.get(i + 1);
                if (val instanceof AnnotationNode an) {
                    String pt = extractAtValue(an);
                    if (pt != null) list.add(pt);
                } else if (val instanceof List<?> l) {
                    for (Object elem : l) {
                        if (elem instanceof AnnotationNode an) {
                            String pt = extractAtValue(an);
                            if (pt != null) list.add(pt);
                        }
                    }
                }
            }
        }
        return list;
    }

    private static String extractAtValue(AnnotationNode atAnn) {
        if (atAnn == null || atAnn.values == null) return null;
        for (int i = 0; i < atAnn.values.size(); i += 2) {
            if ("value".equals(atAnn.values.get(i))) {
                Object val = atAnn.values.get(i + 1);
                return val != null ? val.toString() : null;
            }
        }
        return null;
    }

    private static boolean extractCancellableFlag(AnnotationNode injectAnn) {
        if (injectAnn == null || injectAnn.values == null) return false;
        for (int i = 0; i < injectAnn.values.size(); i += 2) {
            if ("cancellable".equals(injectAnn.values.get(i))) {
                Object val = injectAnn.values.get(i + 1);
                if (val instanceof Boolean b) return b;
            }
        }
        return false;
    }

    private static boolean matchesAnyTargetSelector(MethodNode targetMethod, List<String> selectors) {
        String name = targetMethod.name;
        String fullKey = name + targetMethod.desc;
        for (String sel : selectors) {
            if (sel.equals(name) || sel.equals(fullKey)) return true;
        }
        return false;
    }
}
