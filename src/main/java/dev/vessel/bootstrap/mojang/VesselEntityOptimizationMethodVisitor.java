package dev.vessel.bootstrap.mojang;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * ASM {@link MethodVisitor} injecting dormancy position telemetry and player-discriminating LOD
 * at the beginning of {@code Entity.tick()}.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.1
 * @author VESSEL Engine Team
 */
public final class VesselEntityOptimizationMethodVisitor extends MethodVisitor {

    public VesselEntityOptimizationMethodVisitor(MethodVisitor parentVisitor) {
        super(Opcodes.ASM9, parentVisitor);
    }

    @Override
    public void visitCode() {
        super.visitCode();

        // Load 'this' and invoke this.getId() -> I
        super.visitVarInsn(Opcodes.ALOAD, 0);
        super.visitMethodInsn(
                Opcodes.INVOKEVIRTUAL,
                "net/minecraft/world/entity/Entity",
                "getId",
                "()I",
                false);

        // Load 'this' and invoke this.getX() -> D
        super.visitVarInsn(Opcodes.ALOAD, 0);
        super.visitMethodInsn(
                Opcodes.INVOKEVIRTUAL,
                "net/minecraft/world/entity/Entity",
                "getX",
                "()D",
                false);

        // Load 'this' and invoke this.getY() -> D
        super.visitVarInsn(Opcodes.ALOAD, 0);
        super.visitMethodInsn(
                Opcodes.INVOKEVIRTUAL,
                "net/minecraft/world/entity/Entity",
                "getY",
                "()D",
                false);

        // Load 'this' and invoke this.getZ() -> D
        super.visitVarInsn(Opcodes.ALOAD, 0);
        super.visitMethodInsn(
                Opcodes.INVOKEVIRTUAL,
                "net/minecraft/world/entity/Entity",
                "getZ",
                "()D",
                false);

        // Load 'this' and check instanceof Player -> Z
        super.visitVarInsn(Opcodes.ALOAD, 0);
        super.visitTypeInsn(Opcodes.INSTANCEOF, "net/minecraft/world/entity/player/Player");

        // Invoke VesselOptimizationRuntimeHooks.onEntityTick(int, double, double, double, boolean) -> Z
        super.visitMethodInsn(
                Opcodes.INVOKESTATIC,
                "dev/vessel/kernel/optimization/VesselOptimizationRuntimeHooks",
                "onEntityTick",
                "(IDDDZ)Z",
                false);

        // Discard the boolean result from the operand stack
        super.visitInsn(Opcodes.POP);
    }
}
