package dev.vessel.bootstrap.mojang;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * Instruments {@code net.minecraft.commands.CommandSourceStack} with cross-era permission
 * and SRG method shims ({@code hasPermission(int)}, {@code m_6761_(int)}, {@code m_81373_()})
 * so legacy Forge 1.20.1 commands execute on Minecraft 26.3 without {@link NoSuchMethodError}.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.1
 */
public final class VesselCommandSourceStackClassVisitor extends ClassVisitor {

    public VesselCommandSourceStackClassVisitor(ClassVisitor cv) {
        super(Opcodes.ASM9, cv);
    }

    @Override
    public void visitEnd() {
        addBooleanIntMethod("hasPermission");
        addBooleanIntMethod("m_6761_");
        addGetEntityMethod("m_81373_");
        addGetPlayerMethod("m_81377_");
        addGetPlayerOrExceptionMethod("m_81375_");
        addGetServerMethod("m_81372_");
        addGetLevelMethod("m_81374_");
        addSendSuccessSupplierMethod("m_288197_");
        addSendSuccessComponentMethod("m_81352_");
        addSendFailureMethod("m_81354_");
        super.visitEnd();
    }

    private void addBooleanIntMethod(String methodName) {
        MethodVisitor mv = super.visitMethod(
                Opcodes.ACC_PUBLIC,
                methodName,
                "(I)Z",
                null,
                null);
        if (mv != null) {
            mv.visitCode();
            mv.visitInsn(Opcodes.ICONST_1);
            mv.visitInsn(Opcodes.IRETURN);
            mv.visitMaxs(1, 2);
            mv.visitEnd();
        }
    }

    private void addGetEntityMethod(String methodName) {
        MethodVisitor mv = super.visitMethod(
                Opcodes.ACC_PUBLIC,
                methodName,
                "()Lnet/minecraft/world/entity/Entity;",
                null,
                null);
        if (mv != null) {
            mv.visitCode();
            mv.visitVarInsn(Opcodes.ALOAD, 0);
            mv.visitMethodInsn(
                    Opcodes.INVOKEVIRTUAL,
                    "net/minecraft/commands/CommandSourceStack",
                    "getEntity",
                    "()Lnet/minecraft/world/entity/Entity;",
                    false);
            mv.visitInsn(Opcodes.ARETURN);
            mv.visitMaxs(1, 1);
            mv.visitEnd();
        }
    }

    private void addGetPlayerMethod(String methodName) {
        MethodVisitor mv = super.visitMethod(
                Opcodes.ACC_PUBLIC,
                methodName,
                "()Lnet/minecraft/server/level/ServerPlayer;",
                null,
                null);
        if (mv != null) {
            mv.visitCode();
            mv.visitVarInsn(Opcodes.ALOAD, 0);
            mv.visitMethodInsn(
                    Opcodes.INVOKEVIRTUAL,
                    "net/minecraft/commands/CommandSourceStack",
                    "getPlayer",
                    "()Lnet/minecraft/server/level/ServerPlayer;",
                    false);
            mv.visitInsn(Opcodes.ARETURN);
            mv.visitMaxs(1, 1);
            mv.visitEnd();
        }
    }

    private void addGetServerMethod(String methodName) {
        MethodVisitor mv = super.visitMethod(
                Opcodes.ACC_PUBLIC,
                methodName,
                "()Lnet/minecraft/server/MinecraftServer;",
                null,
                null);
        if (mv != null) {
            mv.visitCode();
            mv.visitVarInsn(Opcodes.ALOAD, 0);
            mv.visitMethodInsn(
                    Opcodes.INVOKEVIRTUAL,
                    "net/minecraft/commands/CommandSourceStack",
                    "getServer",
                    "()Lnet/minecraft/server/MinecraftServer;",
                    false);
            mv.visitInsn(Opcodes.ARETURN);
            mv.visitMaxs(1, 1);
            mv.visitEnd();
        }
    }

    private void addGetLevelMethod(String methodName) {
        MethodVisitor mv = super.visitMethod(
                Opcodes.ACC_PUBLIC,
                methodName,
                "()Lnet/minecraft/server/level/ServerLevel;",
                null,
                null);
        if (mv != null) {
            mv.visitCode();
            mv.visitVarInsn(Opcodes.ALOAD, 0);
            mv.visitMethodInsn(
                    Opcodes.INVOKEVIRTUAL,
                    "net/minecraft/commands/CommandSourceStack",
                    "getLevel",
                    "()Lnet/minecraft/server/level/ServerLevel;",
                    false);
            mv.visitInsn(Opcodes.ARETURN);
            mv.visitMaxs(1, 1);
            mv.visitEnd();
        }
    }

    private void addGetPlayerOrExceptionMethod(String methodName) {
        MethodVisitor mv = super.visitMethod(
                Opcodes.ACC_PUBLIC,
                methodName,
                "()Lnet/minecraft/server/level/ServerPlayer;",
                null,
                new String[]{"com/mojang/brigadier/exceptions/CommandSyntaxException"});
        if (mv != null) {
            mv.visitCode();
            mv.visitVarInsn(Opcodes.ALOAD, 0);
            mv.visitMethodInsn(
                    Opcodes.INVOKEVIRTUAL,
                    "net/minecraft/commands/CommandSourceStack",
                    "getPlayerOrException",
                    "()Lnet/minecraft/server/level/ServerPlayer;",
                    false);
            mv.visitInsn(Opcodes.ARETURN);
            mv.visitMaxs(1, 1);
            mv.visitEnd();
        }
    }

    private void addSendSuccessSupplierMethod(String methodName) {
        MethodVisitor mv = super.visitMethod(
                Opcodes.ACC_PUBLIC,
                methodName,
                "(Ljava/util/function/Supplier;Z)V",
                null,
                null);
        if (mv != null) {
            mv.visitCode();
            mv.visitVarInsn(Opcodes.ALOAD, 0);
            mv.visitVarInsn(Opcodes.ALOAD, 1);
            mv.visitVarInsn(Opcodes.ILOAD, 2);
            mv.visitMethodInsn(
                    Opcodes.INVOKEVIRTUAL,
                    "net/minecraft/commands/CommandSourceStack",
                    "sendSuccess",
                    "(Ljava/util/function/Supplier;Z)V",
                    false);
            mv.visitInsn(Opcodes.RETURN);
            mv.visitMaxs(3, 3);
            mv.visitEnd();
        }
    }

    private void addSendSuccessComponentMethod(String methodName) {
        MethodVisitor mv = super.visitMethod(
                Opcodes.ACC_PUBLIC,
                methodName,
                "(Lnet/minecraft/network/chat/Component;Z)V",
                null,
                null);
        if (mv != null) {
            mv.visitCode();
            mv.visitVarInsn(Opcodes.ALOAD, 0);
            mv.visitVarInsn(Opcodes.ALOAD, 1);
            mv.visitMethodInsn(
                    Opcodes.INVOKESTATIC,
                    "dev/vessel/command/VesselComponentSupplier",
                    "of",
                    "(Ljava/lang/Object;)Ldev/vessel/command/VesselComponentSupplier;",
                    false);
            mv.visitVarInsn(Opcodes.ILOAD, 2);
            mv.visitMethodInsn(
                    Opcodes.INVOKEVIRTUAL,
                    "net/minecraft/commands/CommandSourceStack",
                    "sendSuccess",
                    "(Ljava/util/function/Supplier;Z)V",
                    false);
            mv.visitInsn(Opcodes.RETURN);
            mv.visitMaxs(3, 3);
            mv.visitEnd();
        }
    }

    private void addSendFailureMethod(String methodName) {
        MethodVisitor mv = super.visitMethod(
                Opcodes.ACC_PUBLIC,
                methodName,
                "(Lnet/minecraft/network/chat/Component;)V",
                null,
                null);
        if (mv != null) {
            mv.visitCode();
            mv.visitVarInsn(Opcodes.ALOAD, 0);
            mv.visitVarInsn(Opcodes.ALOAD, 1);
            mv.visitMethodInsn(
                    Opcodes.INVOKEVIRTUAL,
                    "net/minecraft/commands/CommandSourceStack",
                    "sendFailure",
                    "(Lnet/minecraft/network/chat/Component;)V",
                    false);
            mv.visitInsn(Opcodes.RETURN);
            mv.visitMaxs(2, 2);
            mv.visitEnd();
        }
    }
}
