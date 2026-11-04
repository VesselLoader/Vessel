package dev.vessel.bootstrap.mojang;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * Instruments {@code net.minecraft.resources.ResourceKey} bytecode with legacy Forge 1.20.1
 * and SRG method shims ({@code m_135788_}, {@code m_135785_}, {@code createRegistryKey}, {@code create})
 * so older mods resolving keys with {@code ResourceLocation} run seamlessly on Minecraft 26.3.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public final class VesselResourceKeyClassVisitor extends ClassVisitor {

    public VesselResourceKeyClassVisitor(ClassVisitor cv) {
        super(Opcodes.ASM9, cv);
    }

    @Override
    public void visitEnd() {
        addCreateRegistryKeySrgMethod();
        addCreateRegistryKeyLegacyMethod();
        addCreateSrgMethod();
        addCreateLegacyMethod();
        super.visitEnd();
    }

    private void addCreateRegistryKeySrgMethod() {
        MethodVisitor mv = super.visitMethod(
                Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC,
                "m_135788_",
                "(Lnet/minecraft/resources/ResourceLocation;)Lnet/minecraft/resources/ResourceKey;",
                null,
                null);
        if (mv != null) {
            mv.visitCode();
            mv.visitVarInsn(Opcodes.ALOAD, 0);
            mv.visitMethodInsn(
                    Opcodes.INVOKEVIRTUAL,
                    "java/lang/Object",
                    "toString",
                    "()Ljava/lang/String;",
                    false);
            mv.visitMethodInsn(
                    Opcodes.INVOKESTATIC,
                    "net/minecraft/resources/Identifier",
                    "parse",
                    "(Ljava/lang/String;)Lnet/minecraft/resources/Identifier;",
                    false);
            mv.visitMethodInsn(
                    Opcodes.INVOKESTATIC,
                    "net/minecraft/resources/ResourceKey",
                    "createRegistryKey",
                    "(Lnet/minecraft/resources/Identifier;)Lnet/minecraft/resources/ResourceKey;",
                    false);
            mv.visitInsn(Opcodes.ARETURN);
            mv.visitMaxs(1, 1);
            mv.visitEnd();
        }
    }

    private void addCreateRegistryKeyLegacyMethod() {
        MethodVisitor mv = super.visitMethod(
                Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC,
                "createRegistryKey",
                "(Lnet/minecraft/resources/ResourceLocation;)Lnet/minecraft/resources/ResourceKey;",
                null,
                null);
        if (mv != null) {
            mv.visitCode();
            mv.visitVarInsn(Opcodes.ALOAD, 0);
            mv.visitMethodInsn(
                    Opcodes.INVOKESTATIC,
                    "net/minecraft/resources/ResourceKey",
                    "m_135788_",
                    "(Lnet/minecraft/resources/ResourceLocation;)Lnet/minecraft/resources/ResourceKey;",
                    false);
            mv.visitInsn(Opcodes.ARETURN);
            mv.visitMaxs(1, 1);
            mv.visitEnd();
        }
    }

    private void addCreateSrgMethod() {
        MethodVisitor mv = super.visitMethod(
                Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC,
                "m_135785_",
                "(Lnet/minecraft/resources/ResourceKey;Lnet/minecraft/resources/ResourceLocation;)Lnet/minecraft/resources/ResourceKey;",
                null,
                null);
        if (mv != null) {
            mv.visitCode();
            mv.visitVarInsn(Opcodes.ALOAD, 0);
            mv.visitVarInsn(Opcodes.ALOAD, 1);
            mv.visitMethodInsn(
                    Opcodes.INVOKEVIRTUAL,
                    "java/lang/Object",
                    "toString",
                    "()Ljava/lang/String;",
                    false);
            mv.visitMethodInsn(
                    Opcodes.INVOKESTATIC,
                    "net/minecraft/resources/Identifier",
                    "parse",
                    "(Ljava/lang/String;)Lnet/minecraft/resources/Identifier;",
                    false);
            mv.visitMethodInsn(
                    Opcodes.INVOKESTATIC,
                    "net/minecraft/resources/ResourceKey",
                    "create",
                    "(Lnet/minecraft/resources/ResourceKey;Lnet/minecraft/resources/Identifier;)Lnet/minecraft/resources/ResourceKey;",
                    false);
            mv.visitInsn(Opcodes.ARETURN);
            mv.visitMaxs(2, 2);
            mv.visitEnd();
        }
    }

    private void addCreateLegacyMethod() {
        MethodVisitor mv = super.visitMethod(
                Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC,
                "create",
                "(Lnet/minecraft/resources/ResourceKey;Lnet/minecraft/resources/ResourceLocation;)Lnet/minecraft/resources/ResourceKey;",
                null,
                null);
        if (mv != null) {
            mv.visitCode();
            mv.visitVarInsn(Opcodes.ALOAD, 0);
            mv.visitVarInsn(Opcodes.ALOAD, 1);
            mv.visitMethodInsn(
                    Opcodes.INVOKESTATIC,
                    "net/minecraft/resources/ResourceKey",
                    "m_135785_",
                    "(Lnet/minecraft/resources/ResourceKey;Lnet/minecraft/resources/ResourceLocation;)Lnet/minecraft/resources/ResourceKey;",
                    false);
            mv.visitInsn(Opcodes.ARETURN);
            mv.visitMaxs(2, 2);
            mv.visitEnd();
        }
    }
}
