package dev.vessel.bootstrap.mojang;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * Instruments {@code net.minecraft.world.level.material.MapColor} with Forge 1.20.1 SRG static fields
 * ({@code f_283820_} for {@code NETHER}, {@code f_283869_} for {@code COLOR_LIGHT_BLUE}) so mods
 * like Mekanism can initialize {@code EnumColor} constants without {@link NoSuchFieldError}.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public final class VesselMapColorClassVisitor extends ClassVisitor {

    public VesselMapColorClassVisitor(ClassVisitor cv) {
        super(Opcodes.ASM9, cv);
    }

    @Override
    public MethodVisitor visitMethod(
            int access,
            String name,
            String descriptor,
            String signature,
            String[] exceptions) {
        MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);
        if ("<clinit>".equals(name) && mv != null) {
            return new VesselMapColorClinitMethodVisitor(mv);
        }
        return mv;
    }

    @Override
    public void visitEnd() {
        FieldVisitor fv1 = super.visitField(
                Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC,
                "f_283820_",
                "Lnet/minecraft/world/level/material/MapColor;",
                null,
                null);
        if (fv1 != null) {
            fv1.visitEnd();
        }

        FieldVisitor fv2 = super.visitField(
                Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC,
                "f_283869_",
                "Lnet/minecraft/world/level/material/MapColor;",
                null,
                null);
        if (fv2 != null) {
            fv2.visitEnd();
        }

        super.visitEnd();
    }
}
