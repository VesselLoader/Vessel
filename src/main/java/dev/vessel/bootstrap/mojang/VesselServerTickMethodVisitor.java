package dev.vessel.bootstrap.mojang;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * ASM {@link MethodVisitor} injecting nanosecond server tick start and end telemetry hooks
 * directly into {@code MinecraftServer.tickServer(BooleanSupplier)} boundaries.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public final class VesselServerTickMethodVisitor extends MethodVisitor {

    public VesselServerTickMethodVisitor(MethodVisitor parentVisitor) {
        super(Opcodes.ASM9, parentVisitor);
    }

    @Override
    public void visitCode() {
        super.visitCode();
        // Invoke VesselOptimizationRuntimeHooks.onServerTickStart()
        super.visitMethodInsn(
                Opcodes.INVOKESTATIC,
                "dev/vessel/kernel/optimization/VesselOptimizationRuntimeHooks",
                "onServerTickStart",
                "()V",
                false);
    }

    @Override
    public void visitInsn(int opcode) {
        if (opcode == Opcodes.RETURN) {
            // Invoke VesselOptimizationRuntimeHooks.onServerTickEnd() before return
            super.visitMethodInsn(
                    Opcodes.INVOKESTATIC,
                    "dev/vessel/kernel/optimization/VesselOptimizationRuntimeHooks",
                    "onServerTickEnd",
                    "()V",
                    false);
        }
        super.visitInsn(opcode);
    }
}
