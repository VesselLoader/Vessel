package dev.vessel.bootstrap.mojang;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * Instruments {@code net.minecraft.network.chat.MutableComponent} with Forge 1.20.1 SRG method aliases
 * ({@code m_6881_} -> {@code copy}, {@code m_7383_} -> {@code getStyle},
 * {@code m_6270_} -> {@code setStyle}, {@code m_7220_} -> {@code append},
 * {@code m_130940_} -> {@code withStyle}, {@code m_130942_} -> {@code withStyle}) so mods can
 * mutate components without {@link NoSuchMethodError}.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public final class VesselMutableComponentClassVisitor extends ClassVisitor {

    public VesselMutableComponentClassVisitor(ClassVisitor cv) {
        super(Opcodes.ASM9, cv);
    }

    @Override
    public void visitEnd() {
        addCopySrgMethod();
        addGetStyleSrgMethod();
        addSetStyleSrgMethod();
        addAppendComponentSrgMethod();
        addAppendStringSrgMethod();
        addWithStyleFormattingSrgMethod();
        addWithStyleStyleSrgMethod();
        super.visitEnd();
    }

    private void addCopySrgMethod() {
        MethodVisitor mv = super.visitMethod(
                Opcodes.ACC_PUBLIC,
                "m_6881_",
                "()Lnet/minecraft/network/chat/MutableComponent;",
                null,
                null);
        if (mv != null) {
            mv.visitCode();
            mv.visitVarInsn(Opcodes.ALOAD, 0);
            mv.visitMethodInsn(
                    Opcodes.INVOKEVIRTUAL,
                    "net/minecraft/network/chat/MutableComponent",
                    "copy",
                    "()Lnet/minecraft/network/chat/MutableComponent;",
                    false);
            mv.visitInsn(Opcodes.ARETURN);
            mv.visitMaxs(1, 1);
            mv.visitEnd();
        }
    }

    private void addGetStyleSrgMethod() {
        MethodVisitor mv = super.visitMethod(
                Opcodes.ACC_PUBLIC,
                "m_7383_",
                "()Lnet/minecraft/network/chat/Style;",
                null,
                null);
        if (mv != null) {
            mv.visitCode();
            mv.visitVarInsn(Opcodes.ALOAD, 0);
            mv.visitMethodInsn(
                    Opcodes.INVOKEVIRTUAL,
                    "net/minecraft/network/chat/MutableComponent",
                    "getStyle",
                    "()Lnet/minecraft/network/chat/Style;",
                    false);
            mv.visitInsn(Opcodes.ARETURN);
            mv.visitMaxs(1, 1);
            mv.visitEnd();
        }
    }

    private void addSetStyleSrgMethod() {
        MethodVisitor mv = super.visitMethod(
                Opcodes.ACC_PUBLIC,
                "m_6270_",
                "(Lnet/minecraft/network/chat/Style;)Lnet/minecraft/network/chat/MutableComponent;",
                null,
                null);
        if (mv != null) {
            mv.visitCode();
            mv.visitVarInsn(Opcodes.ALOAD, 0);
            mv.visitVarInsn(Opcodes.ALOAD, 1);
            mv.visitMethodInsn(
                    Opcodes.INVOKEVIRTUAL,
                    "net/minecraft/network/chat/MutableComponent",
                    "setStyle",
                    "(Lnet/minecraft/network/chat/Style;)Lnet/minecraft/network/chat/MutableComponent;",
                    false);
            mv.visitInsn(Opcodes.ARETURN);
            mv.visitMaxs(2, 2);
            mv.visitEnd();
        }
    }

    private void addAppendComponentSrgMethod() {
        MethodVisitor mv = super.visitMethod(
                Opcodes.ACC_PUBLIC,
                "m_7220_",
                "(Lnet/minecraft/network/chat/Component;)Lnet/minecraft/network/chat/MutableComponent;",
                null,
                null);
        if (mv != null) {
            mv.visitCode();
            mv.visitVarInsn(Opcodes.ALOAD, 0);
            mv.visitVarInsn(Opcodes.ALOAD, 1);
            mv.visitMethodInsn(
                    Opcodes.INVOKEVIRTUAL,
                    "net/minecraft/network/chat/MutableComponent",
                    "append",
                    "(Lnet/minecraft/network/chat/Component;)Lnet/minecraft/network/chat/MutableComponent;",
                    false);
            mv.visitInsn(Opcodes.ARETURN);
            mv.visitMaxs(2, 2);
            mv.visitEnd();
        }
    }

    private void addAppendStringSrgMethod() {
        MethodVisitor mv = super.visitMethod(
                Opcodes.ACC_PUBLIC,
                "m_130948_",
                "(Ljava/lang/String;)Lnet/minecraft/network/chat/MutableComponent;",
                null,
                null);
        if (mv != null) {
            mv.visitCode();
            mv.visitVarInsn(Opcodes.ALOAD, 0);
            mv.visitVarInsn(Opcodes.ALOAD, 1);
            mv.visitMethodInsn(
                    Opcodes.INVOKEVIRTUAL,
                    "net/minecraft/network/chat/MutableComponent",
                    "append",
                    "(Ljava/lang/String;)Lnet/minecraft/network/chat/MutableComponent;",
                    false);
            mv.visitInsn(Opcodes.ARETURN);
            mv.visitMaxs(2, 2);
            mv.visitEnd();
        }
    }

    private void addWithStyleFormattingSrgMethod() {
        MethodVisitor mv = super.visitMethod(
                Opcodes.ACC_PUBLIC,
                "m_130940_",
                "(Lnet/minecraft/ChatFormatting;)Lnet/minecraft/network/chat/MutableComponent;",
                null,
                null);
        if (mv != null) {
            mv.visitCode();
            mv.visitVarInsn(Opcodes.ALOAD, 0);
            mv.visitVarInsn(Opcodes.ALOAD, 1);
            mv.visitMethodInsn(
                    Opcodes.INVOKEVIRTUAL,
                    "net/minecraft/network/chat/MutableComponent",
                    "withStyle",
                    "(Lnet/minecraft/ChatFormatting;)Lnet/minecraft/network/chat/MutableComponent;",
                    false);
            mv.visitInsn(Opcodes.ARETURN);
            mv.visitMaxs(2, 2);
            mv.visitEnd();
        }
    }

    private void addWithStyleStyleSrgMethod() {
        MethodVisitor mv = super.visitMethod(
                Opcodes.ACC_PUBLIC,
                "m_130942_",
                "(Lnet/minecraft/network/chat/Style;)Lnet/minecraft/network/chat/MutableComponent;",
                null,
                null);
        if (mv != null) {
            mv.visitCode();
            mv.visitVarInsn(Opcodes.ALOAD, 0);
            mv.visitVarInsn(Opcodes.ALOAD, 1);
            mv.visitMethodInsn(
                    Opcodes.INVOKEVIRTUAL,
                    "net/minecraft/network/chat/MutableComponent",
                    "withStyle",
                    "(Lnet/minecraft/network/chat/Style;)Lnet/minecraft/network/chat/MutableComponent;",
                    false);
            mv.visitInsn(Opcodes.ARETURN);
            mv.visitMaxs(2, 2);
            mv.visitEnd();
        }
    }
}
