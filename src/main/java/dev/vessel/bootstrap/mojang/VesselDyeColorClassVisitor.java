package dev.vessel.bootstrap.mojang;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * Instruments {@code net.minecraft.world.item.DyeColor} with Forge 1.20.1 SRG method aliases
 * ({@code m_284406_} -> {@code getMapColor}, {@code m_41071_} -> {@code getTextColor}) so mods
 * like Mekanism can construct chat/text formatting enums without {@link NoSuchMethodError}.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public final class VesselDyeColorClassVisitor extends ClassVisitor {

    public VesselDyeColorClassVisitor(ClassVisitor cv) {
        super(Opcodes.ASM9, cv);
    }

    @Override
    public void visitEnd() {
        addGetMapColorSrgMethod();
        addGetTextColorSrgMethod();
        addGetIdSrgMethod();
        addGetNameSrgMethod();
        super.visitEnd();
    }

    private void addGetMapColorSrgMethod() {
        MethodVisitor mv = super.visitMethod(
                Opcodes.ACC_PUBLIC,
                "m_284406_",
                "()Lnet/minecraft/world/level/material/MapColor;",
                null,
                null);
        if (mv != null) {
            mv.visitCode();
            mv.visitVarInsn(Opcodes.ALOAD, 0);
            mv.visitMethodInsn(
                    Opcodes.INVOKEVIRTUAL,
                    "net/minecraft/world/item/DyeColor",
                    "getMapColor",
                    "()Lnet/minecraft/world/level/material/MapColor;",
                    false);
            mv.visitInsn(Opcodes.ARETURN);
            mv.visitMaxs(1, 1);
            mv.visitEnd();
        }
    }

    private void addGetTextColorSrgMethod() {
        MethodVisitor mv = super.visitMethod(
                Opcodes.ACC_PUBLIC,
                "m_41071_",
                "()I",
                null,
                null);
        if (mv != null) {
            mv.visitCode();
            mv.visitVarInsn(Opcodes.ALOAD, 0);
            mv.visitMethodInsn(
                    Opcodes.INVOKEVIRTUAL,
                    "net/minecraft/world/item/DyeColor",
                    "getTextColor",
                    "()I",
                    false);
            mv.visitInsn(Opcodes.IRETURN);
            mv.visitMaxs(1, 1);
            mv.visitEnd();
        }
    }

    private void addGetIdSrgMethod() {
        MethodVisitor mv = super.visitMethod(
                Opcodes.ACC_PUBLIC,
                "m_41060_",
                "()I",
                null,
                null);
        if (mv != null) {
            mv.visitCode();
            mv.visitVarInsn(Opcodes.ALOAD, 0);
            mv.visitMethodInsn(
                    Opcodes.INVOKEVIRTUAL,
                    "net/minecraft/world/item/DyeColor",
                    "getId",
                    "()I",
                    false);
            mv.visitInsn(Opcodes.IRETURN);
            mv.visitMaxs(1, 1);
            mv.visitEnd();
        }
    }

    private void addGetNameSrgMethod() {
        MethodVisitor mv = super.visitMethod(
                Opcodes.ACC_PUBLIC,
                "m_41065_",
                "()Ljava/lang/String;",
                null,
                null);
        if (mv != null) {
            mv.visitCode();
            mv.visitVarInsn(Opcodes.ALOAD, 0);
            mv.visitMethodInsn(
                    Opcodes.INVOKEVIRTUAL,
                    "net/minecraft/world/item/DyeColor",
                    "getName",
                    "()Ljava/lang/String;",
                    false);
            mv.visitInsn(Opcodes.ARETURN);
            mv.visitMaxs(1, 1);
            mv.visitEnd();
        }
    }
}
