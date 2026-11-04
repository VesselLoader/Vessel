package dev.vessel.bootstrap.mojang;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * ASM MethodVisitor that intercepts {@code <clinit>} of {@code MapColor} to initialize
 * Forge 1.20.1 SRG static fields ({@code f_283820_ = NETHER}, {@code f_283869_ = COLOR_LIGHT_BLUE}).
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public final class VesselMapColorClinitMethodVisitor extends MethodVisitor {

    public VesselMapColorClinitMethodVisitor(MethodVisitor mv) {
        super(Opcodes.ASM9, mv);
    }

    @Override
    public void visitInsn(int opcode) {
        if (opcode == Opcodes.RETURN) {
            super.visitFieldInsn(
                    Opcodes.GETSTATIC,
                    "net/minecraft/world/level/material/MapColor",
                    "NETHER",
                    "Lnet/minecraft/world/level/material/MapColor;");
            super.visitFieldInsn(
                    Opcodes.PUTSTATIC,
                    "net/minecraft/world/level/material/MapColor",
                    "f_283820_",
                    "Lnet/minecraft/world/level/material/MapColor;");

            super.visitFieldInsn(
                    Opcodes.GETSTATIC,
                    "net/minecraft/world/level/material/MapColor",
                    "COLOR_LIGHT_BLUE",
                    "Lnet/minecraft/world/level/material/MapColor;");
            super.visitFieldInsn(
                    Opcodes.PUTSTATIC,
                    "net/minecraft/world/level/material/MapColor",
                    "f_283869_",
                    "Lnet/minecraft/world/level/material/MapColor;");
        }
        super.visitInsn(opcode);
    }
}
