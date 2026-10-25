package dev.vessel.kernel.identity;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.util.Objects;

/**
 * ASM MethodVisitor that scans method bytecode instructions for inter-loader API references.
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public final class SourceClassifierMethodVisitor extends MethodVisitor {

    private final SourceClassifierContext context;

    /**
     * Constructs a SourceClassifierMethodVisitor with the target accumulation context.
     *
     * @param context classifier context
     */
    public SourceClassifierMethodVisitor(SourceClassifierContext context) {
        super(Opcodes.ASM9);
        this.context = Objects.requireNonNull(context, "context cannot be null");
    }

    @Override
    public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
        context.recordTypeReference(owner);
        super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
    }

    @Override
    public void visitFieldInsn(int opcode, String owner, String name, String descriptor) {
        context.recordTypeReference(owner);
        super.visitFieldInsn(opcode, owner, name, descriptor);
    }

    @Override
    public void visitTypeInsn(int opcode, String type) {
        context.recordTypeReference(type);
        super.visitTypeInsn(opcode, type);
    }
}
