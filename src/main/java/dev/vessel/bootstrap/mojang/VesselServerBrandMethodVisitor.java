package dev.vessel.bootstrap.mojang;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * ASM {@link MethodVisitor} that rewrites {@code MinecraftServer#getServerModName()} method body
 * to return {@code "VESSEL"}.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public final class VesselServerBrandMethodVisitor extends MethodVisitor {

    public VesselServerBrandMethodVisitor(MethodVisitor parentVisitor) {
        super(Opcodes.ASM9, parentVisitor);
    }

    @Override
    public void visitCode() {
        super.visitCode();
        super.visitLdcInsn("VESSEL");
        super.visitInsn(Opcodes.ARETURN);
    }

    @Override
    public void visitInsn(int opcode) {
        // Suppress original bytecode instructions
    }

    @Override
    public void visitIntInsn(int opcode, int operand) {
        // Suppress original bytecode instructions
    }

    @Override
    public void visitVarInsn(int opcode, int varIndex) {
        // Suppress original bytecode instructions
    }

    @Override
    public void visitTypeInsn(int opcode, String type) {
        // Suppress original bytecode instructions
    }

    @Override
    public void visitFieldInsn(int opcode, String owner, String name, String descriptor) {
        // Suppress original bytecode instructions
    }

    @Override
    public void visitMethodInsn(
            int opcode,
            String owner,
            String name,
            String descriptor,
            boolean isInterface) {
        // Suppress original bytecode instructions
    }

    @Override
    public void visitLdcInsn(Object value) {
        // Suppress original bytecode instructions
    }

    @Override
    public void visitMaxs(int maxStack, int maxLocals) {
        super.visitMaxs(1, 1);
    }
}
