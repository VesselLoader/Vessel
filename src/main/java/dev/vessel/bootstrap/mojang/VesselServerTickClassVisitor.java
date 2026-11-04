package dev.vessel.bootstrap.mojang;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * ASM {@link ClassVisitor} targeting {@code net.minecraft.server.MinecraftServer} to instrument
 * tick execution methods with {@link VesselServerTickMethodVisitor}.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public final class VesselServerTickClassVisitor extends ClassVisitor {

    public VesselServerTickClassVisitor(ClassVisitor parentVisitor) {
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
        if (isTickServerMethod(name, descriptor)) {
            return new VesselServerTickMethodVisitor(mv);
        }
        return mv;
    }

    private boolean isTickServerMethod(String name, String descriptor) {
        if ("tickServer".equals(name) || "method_3748".equals(name) || "m_5703_".equals(name)) {
            return true;
        }
        if ("tickChildren".equals(name) && "(Ljava/util/function/BooleanSupplier;)V".equals(descriptor)) {
            return true;
        }
        return false;
    }
}
