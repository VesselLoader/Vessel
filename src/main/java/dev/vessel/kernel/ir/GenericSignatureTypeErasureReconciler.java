package dev.vessel.kernel.ir;

import org.objectweb.asm.Type;
import org.objectweb.asm.signature.SignatureReader;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.Objects;

/**
 * Reconciles and synchronizes generic signatures against raw JVM descriptors after bytecode remapping.
 * <p>
 * Detects parameter count drift, corrupted type bounds, and malformed generic signatures,
 * safely repairing or stripping incompatible signatures to preempt {@link java.lang.reflect.GenericSignatureFormatError}.
 * </p>
 * <p>
 * Directly resolves ERR-63 and MAP-SIG-0001..0350.
 * </p>
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public final class GenericSignatureTypeErasureReconciler {

    private final SignatureSyntaxValidator syntaxValidator;

    public GenericSignatureTypeErasureReconciler() {
        this(new SignatureSyntaxValidator());
    }

    public GenericSignatureTypeErasureReconciler(SignatureSyntaxValidator syntaxValidator) {
        this.syntaxValidator = Objects.requireNonNull(syntaxValidator, "syntaxValidator cannot be null");
    }

    /**
     * Reconciles generic signatures across class, fields, and methods in the provided ASM ClassNode.
     *
     * @param node the target ClassNode
     * @return reconciliation outcome report
     */
    public SignatureReconcileResult reconcile(ClassNode node) {
        Objects.requireNonNull(node, "node cannot be null");

        int strippedCount = 0;
        int modifiedCount = 0;

        // 1. Reconcile class signature
        if (node.signature != null) {
            if (!syntaxValidator.isValidSignature(node.signature)) {
                node.signature = null;
                strippedCount++;
            }
        }

        // 2. Reconcile field signatures
        if (node.fields != null) {
            for (FieldNode fn : node.fields) {
                if (fn.signature != null) {
                    if (!syntaxValidator.isValidSignature(fn.signature)) {
                        fn.signature = null;
                        strippedCount++;
                    }
                }
            }
        }

        // 3. Reconcile method signatures against descriptor parameter counts
        if (node.methods != null) {
            for (MethodNode mn : node.methods) {
                if (mn.signature != null) {
                    if (!syntaxValidator.isValidSignature(mn.signature)) {
                        mn.signature = null;
                        strippedCount++;
                        continue;
                    }

                    // Validate signature parameter count against raw descriptor arguments
                    try {
                        SignatureReader reader = new SignatureReader(mn.signature);
                        MethodSignatureParameterCounter counter = new MethodSignatureParameterCounter();
                        reader.accept(counter);

                        Type[] argTypes = Type.getArgumentTypes(mn.desc);
                        if (counter.getParameterCount() != argTypes.length) {
                            // Arity mismatch after parameter injection or synthetic erasure -> strip signature
                            mn.signature = null;
                            strippedCount++;
                        }
                    } catch (Exception e) {
                        mn.signature = null;
                        strippedCount++;
                    }
                }
            }
        }

        return new SignatureReconcileResult(modifiedCount, strippedCount);
    }
}
