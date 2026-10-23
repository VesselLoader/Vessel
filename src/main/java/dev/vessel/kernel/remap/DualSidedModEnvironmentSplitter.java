package dev.vessel.kernel.remap;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Splits dual-sided mod archives (such as Create, Immersive Engineering, and Botania) by replacing
 * client-side rendering invocations ({@code Flywheel}, {@code PoseStack}, {@code Minecraft} client)
 * with stack-balanced no-op bytecode stubs on the server and filtering out client entrypoints.
 *
 * <p>Strictly adheres to Rule 17 with zero inner or nested classes.</p>
 *
 * @version 1.0.0
 * @author Shiroz &amp; Antigravity
 * @since 1.0.0 (Pass K-27)
 */
public final class DualSidedModEnvironmentSplitter {

    /**
     * Replaces invocations targeting client-only rendering classes inside a dual-sided mod class
     * with stack-balanced no-op instructions so the class can execute safely on the server.
     *
     * @param classBytes raw JVM classfile bytes
     * @return server-safe JVM classfile bytes
     */
    public byte[] stubClientRenderCallsOnServer(byte[] classBytes) {
        Objects.requireNonNull(classBytes, "classBytes cannot be null");

        ClassReader reader = new ClassReader(classBytes);
        ClassNode classNode = new ClassNode();
        reader.accept(classNode, 0);

        if (classNode.methods != null) {
            for (MethodNode method : classNode.methods) {
                stubMethodRenderInstructions(method);
            }
        }

        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        classNode.accept(writer);
        return writer.toByteArray();
    }

    /**
     * Filters a list of declared mod entrypoint class or method names, retaining only entrypoints
     * safe to initialize on a dedicated server.
     *
     * @param declaredEntrypoints list of entrypoint identifiers
     * @return unmodifiable list of server-safe entrypoints
     */
    public List<String> filterServerSafeEntrypoints(List<String> declaredEntrypoints) {
        Objects.requireNonNull(declaredEntrypoints, "declaredEntrypoints cannot be null");

        List<String> serverSafe = new ArrayList<>(declaredEntrypoints.size());
        for (String entrypoint : declaredEntrypoints) {
            if (entrypoint == null || entrypoint.isBlank()) {
                continue;
            }
            if (!isClientSpecificEntrypoint(entrypoint)) {
                serverSafe.add(entrypoint);
            }
        }
        return List.copyOf(serverSafe);
    }

    private static void stubMethodRenderInstructions(MethodNode method) {
        if (method.instructions == null) {
            return;
        }
        AbstractInsnNode current = method.instructions.getFirst();
        while (current != null) {
            AbstractInsnNode next = current.getNext();
            if (current instanceof MethodInsnNode methodInsn && isClientRenderOwner(methodInsn.owner)) {
                InsnList replacement = buildStackBalancedNoOpStub(methodInsn);
                method.instructions.insertBefore(methodInsn, replacement);
                method.instructions.remove(methodInsn);
            }
            current = next;
        }
    }

    private static boolean isClientRenderOwner(String ownerInternalName) {
        if (ownerInternalName == null) {
            return false;
        }
        return ownerInternalName.startsWith("net/minecraft/client/")
                || ownerInternalName.startsWith("com/mojang/blaze3d/")
                || ownerInternalName.startsWith("com/jozufozu/flywheel/")
                || ownerInternalName.startsWith("dev/engine_room/flywheel/")
                || ownerInternalName.contains("/client/renderer/")
                || ownerInternalName.endsWith("/PoseStack");
    }

    private static InsnList buildStackBalancedNoOpStub(MethodInsnNode methodInsn) {
        InsnList list = new InsnList();
        Type[] argTypes = Type.getArgumentTypes(methodInsn.desc);
        for (int i = argTypes.length - 1; i >= 0; i--) {
            int size = argTypes[i].getSize();
            list.add(new InsnNode(size == 2 ? Opcodes.POP2 : Opcodes.POP));
        }

        if (methodInsn.getOpcode() != Opcodes.INVOKESTATIC) {
            list.add(new InsnNode(Opcodes.POP));
        }

        Type returnType = Type.getReturnType(methodInsn.desc);
        switch (returnType.getSort()) {
            case Type.VOID -> {
                // Nothing to push
            }
            case Type.BOOLEAN, Type.BYTE, Type.CHAR, Type.SHORT, Type.INT -> list.add(new InsnNode(Opcodes.ICONST_0));
            case Type.LONG -> list.add(new InsnNode(Opcodes.LCONST_0));
            case Type.FLOAT -> list.add(new InsnNode(Opcodes.FCONST_0));
            case Type.DOUBLE -> list.add(new InsnNode(Opcodes.DCONST_0));
            default -> list.add(new InsnNode(Opcodes.ACONST_NULL));
        }
        return list;
    }

    private static boolean isClientSpecificEntrypoint(String entrypoint) {
        String lower = entrypoint.toLowerCase(Locale.ROOT);
        return lower.contains(".client.")
                || lower.contains("/client/")
                || lower.endsWith("client")
                || lower.contains("clientsetup")
                || lower.contains("clientinit")
                || lower.contains("flywheel")
                || lower.contains("ponder")
                || lower.contains("renderer")
                || lower.contains("keybind");
    }
}
