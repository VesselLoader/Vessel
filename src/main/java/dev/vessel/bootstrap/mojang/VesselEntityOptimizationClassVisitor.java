package dev.vessel.bootstrap.mojang;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * ASM {@link ClassVisitor} targeting {@code net.minecraft.world.entity.Entity} to hook
 * {@code tick()} with dormancy tracking optimizations.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public final class VesselEntityOptimizationClassVisitor extends ClassVisitor {

    public VesselEntityOptimizationClassVisitor(ClassVisitor parentVisitor) {
        super(Opcodes.ASM9, parentVisitor);
    }

    @Override
    public MethodVisitor visitMethod(
            int access,
            String name,
            String descriptor,
            String signature,
            String[] exceptions) {
        MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);
        if ("tick".equals(name) && "()V".equals(descriptor)) {
            return new VesselEntityOptimizationMethodVisitor(mv);
        }
        return mv;
    }
}
