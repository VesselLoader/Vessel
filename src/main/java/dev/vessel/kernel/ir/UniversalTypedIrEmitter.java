package dev.vessel.kernel.ir;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.RecordComponentNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Emits loader-agnostic Universal Typed Semantic IR representations from ASM ClassNodes or bytecode.
 * <p>
 * Extracts full type signatures, hierarchy, access flags, record components, sealed permitted subclasses,
 * and method/field descriptors into normalized immutable data structures.
 * </p>
 * <p>
 * Directly resolves ERR-21 and MAP-011..MAP-020.
 * </p>
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public final class UniversalTypedIrEmitter {

    /**
     * Emits Universal Typed IR from raw bytecode bytes.
     *
     * @param bytecode raw class bytecode
     * @return normalized UniversalTypedClassIR
     */
    public UniversalTypedClassIR emitFromBytecode(byte[] bytecode) {
        Objects.requireNonNull(bytecode, "bytecode cannot be null");
        ClassReader reader = new ClassReader(bytecode);
        ClassNode classNode = new ClassNode();
        reader.accept(classNode, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
        return emit(classNode);
    }

    /**
     * Emits Universal Typed IR from a populated ASM ClassNode.
     *
     * @param node source ASM ClassNode
     * @return normalized UniversalTypedClassIR
     */
    public UniversalTypedClassIR emit(ClassNode node) {
        Objects.requireNonNull(node, "node cannot be null");

        List<UniversalTypedFieldIR> fields = new ArrayList<>();
        if (node.fields != null) {
            for (FieldNode fn : node.fields) {
                fields.add(new UniversalTypedFieldIR(
                        fn.name,
                        fn.desc,
                        fn.signature,
                        fn.access,
                        fn.value
                ));
            }
        }

        List<UniversalTypedMethodIR> methods = new ArrayList<>();
        if (node.methods != null) {
            for (MethodNode mn : node.methods) {
                methods.add(new UniversalTypedMethodIR(
                        mn.name,
                        mn.desc,
                        mn.signature,
                        mn.access,
                        mn.exceptions
                ));
            }
        }

        List<UniversalTypedRecordComponentIR> recordComponents = new ArrayList<>();
        if (node.recordComponents != null) {
            for (RecordComponentNode rcn : node.recordComponents) {
                recordComponents.add(new UniversalTypedRecordComponentIR(
                        rcn.name,
                        rcn.descriptor,
                        rcn.signature
                ));
            }
        }

        List<String> permittedSubclasses = node.permittedSubclasses != null
                ? new ArrayList<>(node.permittedSubclasses)
                : List.of();

        List<String> interfaces = node.interfaces != null
                ? new ArrayList<>(node.interfaces)
                : List.of();

        return new UniversalTypedClassIR(
                node.name,
                node.superName,
                interfaces,
                node.signature,
                node.access,
                fields,
                methods,
                recordComponents,
                permittedSubclasses
        );
    }
}
