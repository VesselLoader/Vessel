package dev.vessel.kernel.mixin.weaver;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

/**
 * Merges non-{@code @Shadow} fields (such as {@code @Unique} state fields) and concrete methods
 * (such as {@code @Inject} callback handlers and {@code @Unique} helper methods) from a Mixin
 * class directly into its target class bytecode via the OW2 ASM Tree API, and inlines {@code HEAD}
 * and {@code TAIL}/{@code RETURN} {@code @Inject} hook invocations, cancellable
 * {@code CallbackInfo} branches, {@code @ModifyReturnValue} return interceptors, and
 * {@code @Redirect} call-site rewrites into matching target methods.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.5
 */
public final class MixinInjectAndUniqueMemberMerger {

    private static final String SHADOW_DESC = "Lorg/spongepowered/asm/mixin/Shadow;";

    private final MixinHeadAndTailInstructionInliner instructionInliner =
            new MixinHeadAndTailInstructionInliner();
    private final MixinCallbackInfoCancellableInliner callbackInfoInliner =
            new MixinCallbackInfoCancellableInliner();
    private final MixinCallbackInfoReturnableInliner callbackInfoReturnableInliner =
            new MixinCallbackInfoReturnableInliner();
    private final MixinModifyReturnValueInliner modifyReturnValueInliner =
            new MixinModifyReturnValueInliner();
    private final MixinRedirectCallSiteInliner redirectCallSiteInliner =
            new MixinRedirectCallSiteInliner();
    private final MixinWrapOperationCallSiteInliner wrapOperationCallSiteInliner =
            new MixinWrapOperationCallSiteInliner();

    /**
     * Merges unique fields and concrete methods from {@code mixinClassBytes} into
     * {@code targetClassBytes}.
     *
     * @param targetClassBytes raw JVM classfile bytes of the target class (must not be null)
     * @param mixinClassBytes  raw JVM classfile bytes of the mixin class (must not be null)
     * @return merged target classfile bytes
     */
    public byte[] mergeMixinMembersIntoTarget(byte[] targetClassBytes, byte[] mixinClassBytes) {
        Objects.requireNonNull(targetClassBytes, "targetClassBytes must not be null");
        Objects.requireNonNull(mixinClassBytes, "mixinClassBytes must not be null");
        if (!hasValidClassMagic(targetClassBytes) || !hasValidClassMagic(mixinClassBytes)) {
            return targetClassBytes;
        }
        try {
            ClassReader targetReader = new ClassReader(targetClassBytes);
            ClassNode targetNode = new ClassNode();
            targetReader.accept(targetNode, 0);

            ClassReader mixinReader = new ClassReader(mixinClassBytes);
            ClassNode mixinNode = new ClassNode();
            mixinReader.accept(mixinNode, 0);

            Set<String> existingFields = new HashSet<>();
            if (targetNode.fields != null) {
                for (FieldNode fn : targetNode.fields) {
                    existingFields.add(fn.name);
                }
            }

            Set<String> existingMethods = new HashSet<>();
            if (targetNode.methods != null) {
                for (MethodNode mn : targetNode.methods) {
                    existingMethods.add(mn.name + mn.desc);
                }
            }

            if (mixinNode.fields != null) {
                for (FieldNode mixinField : mixinNode.fields) {
                    if (!existingFields.contains(mixinField.name) && !isShadowField(mixinField)) {
                        int mergedAccess = Opcodes.ACC_PUBLIC | (mixinField.access & Opcodes.ACC_STATIC);
                        FieldNode copiedField = new FieldNode(
                                mergedAccess,
                                mixinField.name,
                                mixinField.desc,
                                mixinField.signature,
                                mixinField.value);
                        targetNode.fields.add(copiedField);
                        existingFields.add(mixinField.name);
                    }
                }
            }

            if (mixinNode.methods != null) {
                for (MethodNode mixinMethod : mixinNode.methods) {
                    if ("<init>".equals(mixinMethod.name)
                            || "<clinit>".equals(mixinMethod.name)
                            || (mixinMethod.access & Opcodes.ACC_ABSTRACT) != 0) {
                        continue;
                    }
                    String methodKey = mixinMethod.name + mixinMethod.desc;
                    if (!existingMethods.contains(methodKey)) {
                        mixinMethod.access =
                                (mixinMethod.access & ~(Opcodes.ACC_PRIVATE | Opcodes.ACC_PROTECTED))
                                        | Opcodes.ACC_PUBLIC;
                        remapOwnerReferences(mixinMethod, mixinNode.name, targetNode.name);
                        targetNode.methods.add(mixinMethod);
                        existingMethods.add(methodKey);
                    }
                }
            }

            instructionInliner.inlineNoArgInjectHooks(targetNode, mixinNode);
            callbackInfoInliner.inlineCallbackInfoInjectHooks(targetNode, mixinNode);
            callbackInfoReturnableInliner.inlineReturnableInjectHooks(targetNode, mixinNode);
            modifyReturnValueInliner.inlineModifyReturnValueHooks(targetNode, mixinNode);
            redirectCallSiteInliner.inlineRedirectHooks(targetNode, mixinNode);
            wrapOperationCallSiteInliner.inlineWrapOperationHooks(targetNode, mixinNode);

            try {
                ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
                targetNode.accept(cw);
                return cw.toByteArray();
            } catch (Exception frameFallback) {
                ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
                targetNode.accept(cw);
                return cw.toByteArray();
            }
        } catch (Exception e) {
            return targetClassBytes;
        }
    }

    private static boolean hasValidClassMagic(byte[] bytes) {
        return bytes.length >= 8
                && (bytes[0] & 0xFF) == 0xCA
                && (bytes[1] & 0xFF) == 0xFE
                && (bytes[2] & 0xFF) == 0xBA
                && (bytes[3] & 0xFF) == 0xBE;
    }

    private static boolean isShadowField(FieldNode fieldNode) {
        return hasDescriptor(fieldNode.visibleAnnotations, SHADOW_DESC)
                || hasDescriptor(fieldNode.invisibleAnnotations, SHADOW_DESC);
    }

    private static boolean hasDescriptor(List<AnnotationNode> annotations, String descriptor) {
        if (annotations == null) {
            return false;
        }
        for (AnnotationNode node : annotations) {
            if (descriptor.equals(node.desc)) {
                return true;
            }
        }
        return false;
    }

    private static void remapOwnerReferences(
            MethodNode methodNode,
            String mixinInternalName,
            String targetInternalName) {
        if (methodNode.instructions == null
                || mixinInternalName == null
                || targetInternalName == null) {
            return;
        }
        for (AbstractInsnNode insn = methodNode.instructions.getFirst();
                insn != null;
                insn = insn.getNext()) {
            if (insn instanceof FieldInsnNode fieldInsn
                    && mixinInternalName.equals(fieldInsn.owner)) {
                fieldInsn.owner = targetInternalName;
            } else if (insn instanceof MethodInsnNode methodInsn
                    && mixinInternalName.equals(methodInsn.owner)) {
                methodInsn.owner = targetInternalName;
            }
        }
    }
}
