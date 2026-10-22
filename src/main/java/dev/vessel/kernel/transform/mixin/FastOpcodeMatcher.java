package dev.vessel.kernel.transform.mixin;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * High-speed opcode traversal and pattern-matching worker for ASM bytecode instructions.
 * Locates injection sites (HEAD, RETURN, INVOKE, FIELD) with zero object allocation churn.
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public final class FastOpcodeMatcher {

    /**
     * Finds the first executable instruction at the method head, skipping leading labels and line numbers.
     *
     * @param method target method node
     * @return the first executable instruction, or the first instruction if none found
     */
    public AbstractInsnNode findHead(MethodNode method) {
        Objects.requireNonNull(method, "method cannot be null");
        AbstractInsnNode insn = method.instructions.getFirst();
        while (insn != null) {
            if (!(insn instanceof LabelNode) && !(insn instanceof LineNumberNode)) {
                return insn;
            }
            insn = insn.getNext();
        }
        return method.instructions.getFirst();
    }

    /**
     * Finds all return instructions (IRETURN, LRETURN, FRETURN, DRETURN, ARETURN, RETURN) in the method.
     *
     * @param method target method node
     * @return list of all return instruction nodes
     */
    public List<AbstractInsnNode> findReturns(MethodNode method) {
        Objects.requireNonNull(method, "method cannot be null");
        List<AbstractInsnNode> returns = new ArrayList<>();
        AbstractInsnNode insn = method.instructions.getFirst();

        while (insn != null) {
            int opcode = insn.getOpcode();
            if (opcode >= Opcodes.IRETURN && opcode <= Opcodes.RETURN) {
                returns.add(insn);
            }
            insn = insn.getNext();
        }
        return returns;
    }

    /**
     * Finds all method invocation instructions matching the target owner and method name.
     *
     * @param method target method node
     * @param targetOwner internal owner name
     * @param targetName target method name
     * @return list of matching MethodInsnNodes
     */
    public List<MethodInsnNode> findMethodInvocations(MethodNode method, String targetOwner, String targetName) {
        Objects.requireNonNull(method, "method cannot be null");
        List<MethodInsnNode> matches = new ArrayList<>();
        AbstractInsnNode insn = method.instructions.getFirst();

        while (insn != null) {
            if (insn instanceof MethodInsnNode minsn) {
                boolean ownerMatch = (targetOwner == null || targetOwner.isEmpty() || targetOwner.equals(minsn.owner));
                boolean nameMatch = (targetName == null || targetName.isEmpty() || targetName.equals(minsn.name));
                if (ownerMatch && nameMatch) {
                    matches.add(minsn);
                }
            }
            insn = insn.getNext();
        }
        return matches;
    }

    /**
     * Finds all field access instructions matching the target owner and field name.
     *
     * @param method target method node
     * @param targetOwner internal owner name
     * @param targetField target field name
     * @return list of matching FieldInsnNodes
     */
    public List<FieldInsnNode> findFieldAccesses(MethodNode method, String targetOwner, String targetField) {
        Objects.requireNonNull(method, "method cannot be null");
        List<FieldInsnNode> matches = new ArrayList<>();
        AbstractInsnNode insn = method.instructions.getFirst();

        while (insn != null) {
            if (insn instanceof FieldInsnNode finsn) {
                boolean ownerMatch = (targetOwner == null || targetOwner.isEmpty() || targetOwner.equals(finsn.owner));
                boolean nameMatch = (targetField == null || targetField.isEmpty() || targetField.equals(finsn.name));
                if (ownerMatch && nameMatch) {
                    matches.add(finsn);
                }
            }
            insn = insn.getNext();
        }
        return matches;
    }
}
