package dev.vessel.bootstrap.mojang;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * ASM {@link ClassVisitor} targeting {@code net.minecraft.server.MinecraftServer} to instrument
 * constructors with {@code VesselServerBridgeRegistry.setActiveMinecraftServer(this)}.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public final class VesselServerLifecycleClassVisitor extends ClassVisitor {

    public VesselServerLifecycleClassVisitor(ClassVisitor classVisitor) {
        super(Opcodes.ASM9, classVisitor);
    }

    @Override
    public MethodVisitor visitMethod(
            int access,
            String name,
            String descriptor,
            String signature,
            String[] exceptions) {
        MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);
        if ("<init>".equals(name)) {
            return new VesselServerLifecycleMethodVisitor(mv);
        }
        return mv;
    }
}
