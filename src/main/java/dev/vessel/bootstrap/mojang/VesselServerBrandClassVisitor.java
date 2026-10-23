package dev.vessel.bootstrap.mojang;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * ASM {@link ClassVisitor} targeting {@code net.minecraft.server.MinecraftServer} to intercept
 * and rewrite {@code getServerModName()Ljava/lang/String;} so F3, network packets, and crash reports
 * report {@code "VESSEL"}.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public final class VesselServerBrandClassVisitor extends ClassVisitor {

    private boolean brandMethodFound;

    public VesselServerBrandClassVisitor(ClassVisitor classVisitor) {
        super(Opcodes.ASM9, classVisitor);
    }

    @Override
    public MethodVisitor visitMethod(
            int access,
            String name,
            String descriptor,
            String signature,
            String[] exceptions) {
        if ("getServerModName".equals(name) && "()Ljava/lang/String;".equals(descriptor)) {
            brandMethodFound = true;
            MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);
            return new VesselServerBrandMethodVisitor(mv);
        }
        return super.visitMethod(access, name, descriptor, signature, exceptions);
    }

    @Override
    public void visitEnd() {
        if (!brandMethodFound) {
            MethodVisitor mv = super.visitMethod(
                    Opcodes.ACC_PUBLIC,
                    "getServerModName",
                    "()Ljava/lang/String;",
                    null,
                    null);
            mv.visitCode();
            mv.visitLdcInsn("VESSEL");
            mv.visitInsn(Opcodes.ARETURN);
            mv.visitMaxs(1, 1);
            mv.visitEnd();
        }
        super.visitEnd();
    }
}
