package dev.vessel.bootstrap.mojang;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * Instruments {@code net.minecraft.network.chat.TextColor} with Forge 1.20.1 SRG method aliases
 * ({@code m_131266_} -> {@code fromRgb}, {@code m_131265_} -> {@code getValue},
 * {@code m_131270_} -> {@code fromLegacyFormat}, {@code m_131273_} -> {@code serialize}) so mods
 * like Mekanism can construct and inspect chat TextColor instances without {@link NoSuchMethodError}.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public final class VesselTextColorClassVisitor extends ClassVisitor {

    public VesselTextColorClassVisitor(ClassVisitor cv) {
        super(Opcodes.ASM9, cv);
    }

    @Override
    public void visitEnd() {
        addFromRgbSrgMethod();
        addGetValueSrgMethod();
        addFromLegacyFormatSrgMethod();
        addSerializeSrgMethod();
        super.visitEnd();
    }

    private void addFromRgbSrgMethod() {
        MethodVisitor mv = super.visitMethod(
                Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC,
                "m_131266_",
                "(I)Lnet/minecraft/network/chat/TextColor;",
                null,
                null);
        if (mv != null) {
            mv.visitCode();
            mv.visitVarInsn(Opcodes.ILOAD, 0);
            mv.visitMethodInsn(
                    Opcodes.INVOKESTATIC,
                    "net/minecraft/network/chat/TextColor",
                    "fromRgb",
                    "(I)Lnet/minecraft/network/chat/TextColor;",
                    false);
            mv.visitInsn(Opcodes.ARETURN);
            mv.visitMaxs(1, 1);
            mv.visitEnd();
        }
    }

    private void addGetValueSrgMethod() {
        MethodVisitor mv = super.visitMethod(
                Opcodes.ACC_PUBLIC,
                "m_131265_",
                "()I",
                null,
                null);
        if (mv != null) {
            mv.visitCode();
            mv.visitVarInsn(Opcodes.ALOAD, 0);
            mv.visitMethodInsn(
                    Opcodes.INVOKEVIRTUAL,
                    "net/minecraft/network/chat/TextColor",
                    "getValue",
                    "()I",
                    false);
            mv.visitInsn(Opcodes.IRETURN);
            mv.visitMaxs(1, 1);
            mv.visitEnd();
        }
    }

    private void addFromLegacyFormatSrgMethod() {
        MethodVisitor mv = super.visitMethod(
                Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC,
                "m_131270_",
                "(Lnet/minecraft/ChatFormatting;)Lnet/minecraft/network/chat/TextColor;",
                null,
                null);
        if (mv != null) {
            mv.visitCode();
            mv.visitVarInsn(Opcodes.ALOAD, 0);
            mv.visitMethodInsn(
                    Opcodes.INVOKESTATIC,
                    "net/minecraft/network/chat/TextColor",
                    "fromLegacyFormat",
                    "(Lnet/minecraft/ChatFormatting;)Lnet/minecraft/network/chat/TextColor;",
                    false);
            mv.visitInsn(Opcodes.ARETURN);
            mv.visitMaxs(1, 1);
            mv.visitEnd();
        }
    }

    private void addSerializeSrgMethod() {
        MethodVisitor mv = super.visitMethod(
                Opcodes.ACC_PUBLIC,
                "m_131273_",
                "()Ljava/lang/String;",
                null,
                null);
        if (mv != null) {
            mv.visitCode();
            mv.visitVarInsn(Opcodes.ALOAD, 0);
            mv.visitMethodInsn(
                    Opcodes.INVOKEVIRTUAL,
                    "net/minecraft/network/chat/TextColor",
                    "serialize",
                    "()Ljava/lang/String;",
                    false);
            mv.visitInsn(Opcodes.ARETURN);
            mv.visitMaxs(1, 1);
            mv.visitEnd();
        }
    }
}
