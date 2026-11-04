package dev.vessel.bootstrap.mojang;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * ASM MethodVisitor that intercepts {@code <clinit>} of {@code Style} to initialize
 * Forge 1.20.1 SRG static field ({@code f_131099_ = EMPTY}).
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public final class VesselStyleClinitMethodVisitor extends MethodVisitor {

    public VesselStyleClinitMethodVisitor(MethodVisitor mv) {
        super(Opcodes.ASM9, mv);
    }

    @Override
    public void visitInsn(int opcode) {
        if (opcode == Opcodes.RETURN) {
            super.visitFieldInsn(
                    Opcodes.GETSTATIC,
                    "net/minecraft/network/chat/Style",
                    "EMPTY",
                    "Lnet/minecraft/network/chat/Style;");
            super.visitFieldInsn(
                    Opcodes.PUTSTATIC,
                    "net/minecraft/network/chat/Style",
                    "f_131099_",
                    "Lnet/minecraft/network/chat/Style;");
        }
        super.visitInsn(opcode);
    }
}
