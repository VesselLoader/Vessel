package dev.vessel.kernel.ir;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.Objects;

/**
 * Updates constant string lookups inside synthetic $deserializeLambda$ methods when classes or methods are renamed.
 * <p>
 * Java serialization generates a synthetic $deserializeLambda$(SerializedLambda) method with string-based
 * validation for capturing class, implementation class, method name, and method signature.
 * When bytecode is remapped across namespaces, these strings must be updated to prevent runtime
 * {@link IllegalArgumentException} during lambda deserialization.
 * </p>
 * <p>
 * Directly resolves ERR-66 and MAP-DYN-0351..0700.
 * </p>
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public final class SerializedLambdaCaptureRemapper {

    public static final String DESERIALIZE_LAMBDA_NAME = "$deserializeLambda$";
    public static final String DESERIALIZE_LAMBDA_DESC = "(Ljava/lang/invoke/SerializedLambda;)Ljava/lang/Object;";

    /**
     * Remaps string constants in $deserializeLambda$ for the given ClassNode.
     *
     * @param node target ASM ClassNode
     * @param remapProvider remapping provider supplying name and descriptor translations
     * @return remapping outcome report
     */
    public LambdaCaptureRemapResult remap(ClassNode node, BootstrapRemapProvider remapProvider) {
        Objects.requireNonNull(node, "node cannot be null");
        Objects.requireNonNull(remapProvider, "remapProvider cannot be null");

        if (node.methods == null) {
            return LambdaCaptureRemapResult.ABSENT;
        }

        MethodNode deserializeMethod = null;
        for (MethodNode mn : node.methods) {
            if (DESERIALIZE_LAMBDA_NAME.equals(mn.name) && DESERIALIZE_LAMBDA_DESC.equals(mn.desc)) {
                deserializeMethod = mn;
                break;
            }
        }

        if (deserializeMethod == null || deserializeMethod.instructions == null) {
            return LambdaCaptureRemapResult.ABSENT;
        }

        int updatedCount = 0;
        for (AbstractInsnNode insn : deserializeMethod.instructions) {
            if (insn instanceof LdcInsnNode ldc && ldc.cst instanceof String strValue) {
                // 1. Check if string is a method descriptor: starts with '('
                if (strValue.startsWith("(") && strValue.contains(")")) {
                    String mappedDesc = remapProvider.mapMethodDescriptor(strValue);
                    if (!strValue.equals(mappedDesc)) {
                        ldc.cst = mappedDesc;
                        updatedCount++;
                        continue;
                    }
                }

                // 2. Check if string is an internal class name
                String mappedClass = remapProvider.mapClassName(strValue);
                if (!strValue.equals(mappedClass)) {
                    ldc.cst = mappedClass;
                    updatedCount++;
                    continue;
                }

                // 3. Check if string is a method name for the current class
                String mappedMethod = remapProvider.mapMethodName(node.name, strValue, "");
                if (!strValue.equals(mappedMethod)) {
                    ldc.cst = mappedMethod;
                    updatedCount++;
                }
            }
        }

        return new LambdaCaptureRemapResult(true, updatedCount);
    }
}
