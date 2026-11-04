package dev.vessel.bootstrap.mojang;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * Instruments {@code net.minecraft.network.chat.Component} interface with Forge 1.20.1 SRG default and
 * static methods ({@code m_6881_} -> {@code copy}, {@code m_237113_} -> {@code literal},
 * {@code m_237110_} -> {@code translatable}, {@code m_237115_} -> {@code empty}) so mods can
 * manipulate chat components without {@link NoSuchMethodError}.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public final class VesselComponentClassVisitor extends ClassVisitor {

    public VesselComponentClassVisitor(ClassVisitor cv) {
        super(Opcodes.ASM9, cv);
    }

    @Override
    public void visitEnd() {
        addCopyDefaultMethod();
        addLiteralStaticMethod();
        addTranslatableStaticMethod();
        addEmptyStaticMethod();
        super.visitEnd();
    }

    private void addCopyDefaultMethod() {
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
                    Opcodes.INVOKEINTERFACE,
                    "net/minecraft/network/chat/Component",
                    "copy",
                    "()Lnet/minecraft/network/chat/MutableComponent;",
                    true);
            mv.visitInsn(Opcodes.ARETURN);
            mv.visitMaxs(1, 1);
            mv.visitEnd();
        }
    }

    private void addLiteralStaticMethod() {
        MethodVisitor mv = super.visitMethod(
                Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC,
                "m_237113_",
                "(Ljava/lang/String;)Lnet/minecraft/network/chat/MutableComponent;",
                null,
                null);
        if (mv != null) {
            mv.visitCode();
            mv.visitVarInsn(Opcodes.ALOAD, 0);
            mv.visitMethodInsn(
                    Opcodes.INVOKESTATIC,
                    "net/minecraft/network/chat/Component",
                    "literal",
                    "(Ljava/lang/String;)Lnet/minecraft/network/chat/MutableComponent;",
                    true);
            mv.visitInsn(Opcodes.ARETURN);
            mv.visitMaxs(1, 1);
            mv.visitEnd();
        }
    }

    private void addTranslatableStaticMethod() {
        MethodVisitor mv = super.visitMethod(
                Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC,
                "m_237110_",
                "(Ljava/lang/String;[Ljava/lang/Object;)Lnet/minecraft/network/chat/MutableComponent;",
                null,
                null);
        if (mv != null) {
            mv.visitCode();
            mv.visitVarInsn(Opcodes.ALOAD, 0);
            mv.visitVarInsn(Opcodes.ALOAD, 1);
            mv.visitMethodInsn(
                    Opcodes.INVOKESTATIC,
                    "net/minecraft/network/chat/Component",
                    "translatable",
                    "(Ljava/lang/String;[Ljava/lang/Object;)Lnet/minecraft/network/chat/MutableComponent;",
                    true);
            mv.visitInsn(Opcodes.ARETURN);
            mv.visitMaxs(2, 2);
            mv.visitEnd();
        }
    }

    private void addEmptyStaticMethod() {
        MethodVisitor mv = super.visitMethod(
                Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC,
                "m_237115_",
                "()Lnet/minecraft/network/chat/MutableComponent;",
                null,
                null);
        if (mv != null) {
            mv.visitCode();
            mv.visitMethodInsn(
                    Opcodes.INVOKESTATIC,
                    "net/minecraft/network/chat/Component",
                    "empty",
                    "()Lnet/minecraft/network/chat/MutableComponent;",
                    true);
            mv.visitInsn(Opcodes.ARETURN);
            mv.visitMaxs(1, 0);
            mv.visitEnd();
        }
    }
}
