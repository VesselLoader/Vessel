package dev.vessel.bootstrap.mojang;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * Instruments {@code net.minecraft.network.chat.Style} with Forge 1.20.1 SRG static fields and
 * methods so mods like Mekanism can format text components without {@link NoSuchFieldError} or
 * {@link NoSuchMethodError}.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public final class VesselStyleClassVisitor extends ClassVisitor {

    public VesselStyleClassVisitor(ClassVisitor cv) {
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
            return new VesselStyleClinitMethodVisitor(mv);
        }
        return mv;
    }

    @Override
    public void visitEnd() {
        FieldVisitor fv = super.visitField(
                Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC,
                "f_131099_",
                "Lnet/minecraft/network/chat/Style;",
                null,
                null);
        if (fv != null) {
            fv.visitEnd();
        }

        addWithColorSrgMethod();
        addApplyFormatSrgMethod();
        addWithClickEventSrgMethod();
        addWithHoverEventSrgMethod();
        addIsEmptySrgMethod();
        addGetColorSrgMethod();
        addGetClickEventSrgMethod();
        addGetHoverEventSrgMethod();
        addIsObfuscatedSrgMethod();
        addIsBoldSrgMethod();
        addIsStrikethroughSrgMethod();
        addIsUnderlinedSrgMethod();
        addIsItalicSrgMethod();

        super.visitEnd();
    }

    private void addWithColorSrgMethod() {
        MethodVisitor mv = super.visitMethod(
                Opcodes.ACC_PUBLIC,
                "m_131148_",
                "(Lnet/minecraft/network/chat/TextColor;)Lnet/minecraft/network/chat/Style;",
                null,
                null);
        if (mv != null) {
            mv.visitCode();
            mv.visitVarInsn(Opcodes.ALOAD, 0);
            mv.visitVarInsn(Opcodes.ALOAD, 1);
            mv.visitMethodInsn(
                    Opcodes.INVOKEVIRTUAL,
                    "net/minecraft/network/chat/Style",
                    "withColor",
                    "(Lnet/minecraft/network/chat/TextColor;)Lnet/minecraft/network/chat/Style;",
                    false);
            mv.visitInsn(Opcodes.ARETURN);
            mv.visitMaxs(2, 2);
            mv.visitEnd();
        }
    }

    private void addApplyFormatSrgMethod() {
        MethodVisitor mv = super.visitMethod(
                Opcodes.ACC_PUBLIC,
                "m_131157_",
                "(Lnet/minecraft/ChatFormatting;)Lnet/minecraft/network/chat/Style;",
                null,
                null);
        if (mv != null) {
            mv.visitCode();
            mv.visitVarInsn(Opcodes.ALOAD, 0);
            mv.visitVarInsn(Opcodes.ALOAD, 1);
            mv.visitMethodInsn(
                    Opcodes.INVOKEVIRTUAL,
                    "net/minecraft/network/chat/Style",
                    "applyFormat",
                    "(Lnet/minecraft/ChatFormatting;)Lnet/minecraft/network/chat/Style;",
                    false);
            mv.visitInsn(Opcodes.ARETURN);
            mv.visitMaxs(2, 2);
            mv.visitEnd();
        }
    }

    private void addWithClickEventSrgMethod() {
        MethodVisitor mv = super.visitMethod(
                Opcodes.ACC_PUBLIC,
                "m_131142_",
                "(Lnet/minecraft/network/chat/ClickEvent;)Lnet/minecraft/network/chat/Style;",
                null,
                null);
        if (mv != null) {
            mv.visitCode();
            mv.visitVarInsn(Opcodes.ALOAD, 0);
            mv.visitVarInsn(Opcodes.ALOAD, 1);
            mv.visitMethodInsn(
                    Opcodes.INVOKEVIRTUAL,
                    "net/minecraft/network/chat/Style",
                    "withClickEvent",
                    "(Lnet/minecraft/network/chat/ClickEvent;)Lnet/minecraft/network/chat/Style;",
                    false);
            mv.visitInsn(Opcodes.ARETURN);
            mv.visitMaxs(2, 2);
            mv.visitEnd();
        }
    }

    private void addWithHoverEventSrgMethod() {
        MethodVisitor mv = super.visitMethod(
                Opcodes.ACC_PUBLIC,
                "m_131144_",
                "(Lnet/minecraft/network/chat/HoverEvent;)Lnet/minecraft/network/chat/Style;",
                null,
                null);
        if (mv != null) {
            mv.visitCode();
            mv.visitVarInsn(Opcodes.ALOAD, 0);
            mv.visitVarInsn(Opcodes.ALOAD, 1);
            mv.visitMethodInsn(
                    Opcodes.INVOKEVIRTUAL,
                    "net/minecraft/network/chat/Style",
                    "withHoverEvent",
                    "(Lnet/minecraft/network/chat/HoverEvent;)Lnet/minecraft/network/chat/Style;",
                    false);
            mv.visitInsn(Opcodes.ARETURN);
            mv.visitMaxs(2, 2);
            mv.visitEnd();
        }
    }

    private void addIsEmptySrgMethod() {
        MethodVisitor mv = super.visitMethod(
                Opcodes.ACC_PUBLIC,
                "m_131179_",
                "()Z",
                null,
                null);
        if (mv != null) {
            mv.visitCode();
            mv.visitVarInsn(Opcodes.ALOAD, 0);
            mv.visitMethodInsn(
                    Opcodes.INVOKEVIRTUAL,
                    "net/minecraft/network/chat/Style",
                    "isEmpty",
                    "()Z",
                    false);
            mv.visitInsn(Opcodes.IRETURN);
            mv.visitMaxs(1, 1);
            mv.visitEnd();
        }
    }

    private void addGetColorSrgMethod() {
        MethodVisitor mv = super.visitMethod(
                Opcodes.ACC_PUBLIC,
                "m_131135_",
                "()Lnet/minecraft/network/chat/TextColor;",
                null,
                null);
        if (mv != null) {
            mv.visitCode();
            mv.visitVarInsn(Opcodes.ALOAD, 0);
            mv.visitMethodInsn(
                    Opcodes.INVOKEVIRTUAL,
                    "net/minecraft/network/chat/Style",
                    "getColor",
                    "()Lnet/minecraft/network/chat/TextColor;",
                    false);
            mv.visitInsn(Opcodes.ARETURN);
            mv.visitMaxs(1, 1);
            mv.visitEnd();
        }
    }

    private void addGetClickEventSrgMethod() {
        MethodVisitor mv = super.visitMethod(
                Opcodes.ACC_PUBLIC,
                "m_131182_",
                "()Lnet/minecraft/network/chat/ClickEvent;",
                null,
                null);
        if (mv != null) {
            mv.visitCode();
            mv.visitVarInsn(Opcodes.ALOAD, 0);
            mv.visitMethodInsn(
                    Opcodes.INVOKEVIRTUAL,
                    "net/minecraft/network/chat/Style",
                    "getClickEvent",
                    "()Lnet/minecraft/network/chat/ClickEvent;",
                    false);
            mv.visitInsn(Opcodes.ARETURN);
            mv.visitMaxs(1, 1);
            mv.visitEnd();
        }
    }

    private void addGetHoverEventSrgMethod() {
        MethodVisitor mv = super.visitMethod(
                Opcodes.ACC_PUBLIC,
                "m_131186_",
                "()Lnet/minecraft/network/chat/HoverEvent;",
                null,
                null);
        if (mv != null) {
            mv.visitCode();
            mv.visitVarInsn(Opcodes.ALOAD, 0);
            mv.visitMethodInsn(
                    Opcodes.INVOKEVIRTUAL,
                    "net/minecraft/network/chat/Style",
                    "getHoverEvent",
                    "()Lnet/minecraft/network/chat/HoverEvent;",
                    false);
            mv.visitInsn(Opcodes.ARETURN);
            mv.visitMaxs(1, 1);
            mv.visitEnd();
        }
    }

    private void addIsObfuscatedSrgMethod() {
        MethodVisitor mv = super.visitMethod(
                Opcodes.ACC_PUBLIC,
                "m_131176_",
                "()Z",
                null,
                null);
        if (mv != null) {
            mv.visitCode();
            mv.visitVarInsn(Opcodes.ALOAD, 0);
            mv.visitMethodInsn(
                    Opcodes.INVOKEVIRTUAL,
                    "net/minecraft/network/chat/Style",
                    "isObfuscated",
                    "()Z",
                    false);
            mv.visitInsn(Opcodes.IRETURN);
            mv.visitMaxs(1, 1);
            mv.visitEnd();
        }
    }

    private void addIsBoldSrgMethod() {
        MethodVisitor mv = super.visitMethod(
                Opcodes.ACC_PUBLIC,
                "m_131154_",
                "()Z",
                null,
                null);
        if (mv != null) {
            mv.visitCode();
            mv.visitVarInsn(Opcodes.ALOAD, 0);
            mv.visitMethodInsn(
                    Opcodes.INVOKEVIRTUAL,
                    "net/minecraft/network/chat/Style",
                    "isBold",
                    "()Z",
                    false);
            mv.visitInsn(Opcodes.IRETURN);
            mv.visitMaxs(1, 1);
            mv.visitEnd();
        }
    }

    private void addIsStrikethroughSrgMethod() {
        MethodVisitor mv = super.visitMethod(
                Opcodes.ACC_PUBLIC,
                "m_131168_",
                "()Z",
                null,
                null);
        if (mv != null) {
            mv.visitCode();
            mv.visitVarInsn(Opcodes.ALOAD, 0);
            mv.visitMethodInsn(
                    Opcodes.INVOKEVIRTUAL,
                    "net/minecraft/network/chat/Style",
                    "isStrikethrough",
                    "()Z",
                    false);
            mv.visitInsn(Opcodes.IRETURN);
            mv.visitMaxs(1, 1);
            mv.visitEnd();
        }
    }

    private void addIsUnderlinedSrgMethod() {
        MethodVisitor mv = super.visitMethod(
                Opcodes.ACC_PUBLIC,
                "m_131171_",
                "()Z",
                null,
                null);
        if (mv != null) {
            mv.visitCode();
            mv.visitVarInsn(Opcodes.ALOAD, 0);
            mv.visitMethodInsn(
                    Opcodes.INVOKEVIRTUAL,
                    "net/minecraft/network/chat/Style",
                    "isUnderlined",
                    "()Z",
                    false);
            mv.visitInsn(Opcodes.IRETURN);
            mv.visitMaxs(1, 1);
            mv.visitEnd();
        }
    }

    private void addIsItalicSrgMethod() {
        MethodVisitor mv = super.visitMethod(
                Opcodes.ACC_PUBLIC,
                "m_131161_",
                "()Z",
                null,
                null);
        if (mv != null) {
            mv.visitCode();
            mv.visitVarInsn(Opcodes.ALOAD, 0);
            mv.visitMethodInsn(
                    Opcodes.INVOKEVIRTUAL,
                    "net/minecraft/network/chat/Style",
                    "isItalic",
                    "()Z",
                    false);
            mv.visitInsn(Opcodes.IRETURN);
            mv.visitMaxs(1, 1);
            mv.visitEnd();
        }
    }
}
