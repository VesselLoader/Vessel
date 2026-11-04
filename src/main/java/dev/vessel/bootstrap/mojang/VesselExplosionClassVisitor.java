package dev.vessel.bootstrap.mojang;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * ASM {@link ClassVisitor} targeting {@code net.minecraft.world.level.Explosion} to instrument
 * explosion execution with {@link VesselExplosionMethodVisitor}.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public final class VesselExplosionClassVisitor extends ClassVisitor {

    public VesselExplosionClassVisitor(ClassVisitor parentVisitor) {
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
        if (isExplodeMethod(name, descriptor)) {
            return new VesselExplosionMethodVisitor(mv);
        }
        return mv;
    }

    private boolean isExplodeMethod(String name, String descriptor) {
        // Mojang: explode()V, Intermediary: method_46062()V / method_46061()V, SRG: m_46062_()V
        return ("explode".equals(name) || name.startsWith("method_460") || name.startsWith("m_46062"))
                && "()V".equals(descriptor);
    }
}
