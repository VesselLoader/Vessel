package dev.vessel.bootstrap.mojang;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * ASM {@link MethodVisitor} injecting GPU/SIMD accelerated raycast dispatch at the start of
 * {@code Explosion.explode()}.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public final class VesselExplosionMethodVisitor extends MethodVisitor {

    public VesselExplosionMethodVisitor(MethodVisitor parentVisitor) {
        super(Opcodes.ASM9, parentVisitor);
    }

    @Override
    public void visitCode() {
        super.visitCode();
        // Load 'this' and pass to VesselOptimizationRuntimeHooks.onExplosionStart(Object)
        super.visitVarInsn(Opcodes.ALOAD, 0);
        super.visitMethodInsn(
                Opcodes.INVOKESTATIC,
                "dev/vessel/kernel/optimization/VesselOptimizationRuntimeHooks",
                "onExplosionStart",
                "(Ljava/lang/Object;)V",
                false);
    }
}
