package dev.vessel.kernel.ir;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.Objects;

/**
 * Bytecode transformer applying access widening rules directly to ASM {@link ClassNode} instances.
 * <p>
 * Performs in-place bitwise flag manipulation (clearing {@code ACC_PRIVATE}, {@code ACC_PROTECTED},
 * and {@code ACC_FINAL}, and applying {@code ACC_PUBLIC}) with zero AST recompilation overhead.
 * </p>
 *
 * @version 1.0.0
 * @author Shiroz &amp; Antigravity
 * @since 1.0.0 (Pass K-7)
 */
public class VesselAccessWidenerApplier {

    /**
     * Applies access widening rules to the specified ClassNode.
     *
     * @param classNode the ASM ClassNode to modify
     * @param table access widening table containing rules
     * @return number of access modifications applied (class + methods + fields)
     */
    public int apply(ClassNode classNode, VesselAccessWideningTable table) {
        Objects.requireNonNull(classNode, "classNode cannot be null");
        Objects.requireNonNull(table, "table cannot be null");

        int modifications = 0;
        String internalName = classNode.name;

        // 1. Class-level widening
        VesselAccessWideningTable.ClassRule classRule = table.getClassRule(internalName);
        if (classRule != null) {
            int originalAccess = classNode.access;
            classNode.access = widenVisibility(classNode.access, classRule.accessLevel());
            if (classRule.extendable()) {
                classNode.access &= ~Opcodes.ACC_FINAL;
            }
            if (classNode.access != originalAccess) {
                modifications++;
            }
        }

        // 2. Method-level widening
        if (classNode.methods != null) {
            for (MethodNode method : classNode.methods) {
                VesselAccessWideningTable.MethodRule mRule = table.getMethodRule(internalName, method.name, method.desc);
                if (mRule != null) {
                    int originalAccess = method.access;
                    method.access = widenVisibility(method.access, mRule.accessLevel());
                    if (mRule.extendable()) {
                        method.access &= ~Opcodes.ACC_FINAL;
                    }
                    if (method.access != originalAccess) {
                        modifications++;
                    }
                }
            }
        }

        // 3. Field-level widening
        if (classNode.fields != null) {
            for (FieldNode field : classNode.fields) {
                VesselAccessWideningTable.FieldRule fRule = table.getFieldRule(internalName, field.name);
                if (fRule != null) {
                    int originalAccess = field.access;
                    field.access = widenVisibility(field.access, fRule.accessLevel());
                    if (fRule.mutable()) {
                        field.access &= ~Opcodes.ACC_FINAL;
                    }
                    if (field.access != originalAccess) {
                        modifications++;
                    }
                }
            }
        }

        return modifications;
    }

    private static int widenVisibility(int access, VesselAccessLevel level) {
        int stripped = access & ~(Opcodes.ACC_PRIVATE | Opcodes.ACC_PROTECTED | Opcodes.ACC_PUBLIC);
        return switch (level) {
            case PUBLIC -> stripped | Opcodes.ACC_PUBLIC;
            case PROTECTED -> stripped | Opcodes.ACC_PROTECTED;
            case PACKAGE_PRIVATE -> stripped;
            case PRIVATE -> stripped | Opcodes.ACC_PRIVATE;
        };
    }
}
