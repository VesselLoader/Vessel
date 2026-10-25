package dev.vessel.bootstrap.mojang;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * ASM {@link MethodVisitor} injecting {@code VesselServerBridgeRegistry.setActiveMinecraftServer(this)}
 * right before {@code RETURN} instructions in {@code MinecraftServer.<init>}.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public final class VesselServerLifecycleMethodVisitor extends MethodVisitor {

    public VesselServerLifecycleMethodVisitor(MethodVisitor parentVisitor) {
        super(Opcodes.ASM9, parentVisitor);
    }

    @Override
    public void visitInsn(int opcode) {
        if (opcode == Opcodes.RETURN) {
            super.visitVarInsn(Opcodes.ALOAD, 0);
            super.visitMethodInsn(
                    Opcodes.INVOKESTATIC,
                    "dev/vessel/command/VesselServerBridgeRegistry",
                    "setActiveMinecraftServer",
                    "(Ljava/lang/Object;)V",
                    false);
        }
        super.visitInsn(opcode);
    }
}
