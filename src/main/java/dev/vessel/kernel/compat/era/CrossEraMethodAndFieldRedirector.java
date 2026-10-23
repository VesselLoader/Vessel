package dev.vessel.kernel.compat.era;

import java.util.Objects;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;

/**
 * Rewrites legacy Minecraft bytecode instructions from older mod eras (1.12.2, 1.16.5, 1.18.2,
 * 1.19.4, 1.20.1, 1.20.4) into their modern Minecraft 1.21+ equivalents:
 * <ul>
 *   <li>{@code new ResourceLocation(ns, path)} &rarr; {@code ResourceLocation.fromNamespaceAndPath(ns, path)}</li>
 *   <li>{@code new ResourceLocation(combined)} &rarr; {@code ResourceLocation.parse(combined)}</li>
 *   <li>{@code entity.level} field access (1.16–1.19.4) &rarr; {@code entity.level()} method call (1.20+)</li>
 *   <li>{@code itemStack.getOrCreateTag()} (1.12–1.20.4) &rarr; {@code itemStack.getCustomDataTag()} bridge</li>
 * </ul>
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public final class CrossEraMethodAndFieldRedirector {

    private static final String RESOURCE_LOCATION_INTERNAL =
            "net/minecraft/resources/ResourceLocation";
    private static final String LEVEL_DESC =
            "Lnet/minecraft/world/level/Level;";

    /**
     * Rewrites legacy Minecraft method and field instructions in {@code classBytes} to match the
     * latest Minecraft server API.
     *
     * @param classBytes the raw class bytecode from a lower-version mod
     * @return the upgraded class bytecode
     */
    public byte[] rewriteLegacyMinecraftCalls(byte[] classBytes) {
        Objects.requireNonNull(classBytes, "classBytes must not be null");
        ClassNode classNode = new ClassNode();
        ClassReader reader = new ClassReader(classBytes);
        reader.accept(classNode, 0);

        for (MethodNode method : classNode.methods) {
            rewriteMethodInstructions(method.instructions);
        }

        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        classNode.accept(writer);
        return writer.toByteArray();
    }

    private static void rewriteMethodInstructions(InsnList instructions) {
        AbstractInsnNode current = instructions.getFirst();
        while (current != null) {
            AbstractInsnNode next = current.getNext();
            if (current instanceof MethodInsnNode methodInsn) {
                if (methodInsn.getOpcode() == Opcodes.INVOKESPECIAL
                        && RESOURCE_LOCATION_INTERNAL.equals(methodInsn.owner)
                        && "<init>".equals(methodInsn.name)) {
                    rewriteResourceLocationConstructor(instructions, methodInsn);
                } else if (methodInsn.getOpcode() == Opcodes.INVOKEVIRTUAL
                        && "net/minecraft/world/item/ItemStack".equals(methodInsn.owner)
                        && "getOrCreateTag".equals(methodInsn.name)) {
                    methodInsn.name = "getCustomDataTag";
                }
            } else if (current instanceof FieldInsnNode fieldInsn) {
                if (fieldInsn.getOpcode() == Opcodes.GETFIELD
                        && "level".equals(fieldInsn.name)
                        && LEVEL_DESC.equals(fieldInsn.desc)) {
                    MethodInsnNode replacement = new MethodInsnNode(
                            Opcodes.INVOKEVIRTUAL,
                            fieldInsn.owner,
                            "level",
                            "()" + LEVEL_DESC,
                            false);
                    instructions.set(fieldInsn, replacement);
                }
            }
            current = next;
        }
    }

    private static void rewriteResourceLocationConstructor(
            InsnList instructions,
            MethodInsnNode initCall) {
        String factoryMethodName;
        String factoryDescriptor;
        if ("(Ljava/lang/String;Ljava/lang/String;)V".equals(initCall.desc)) {
            factoryMethodName = "fromNamespaceAndPath";
            factoryDescriptor =
                    "(Ljava/lang/String;Ljava/lang/String;)L" + RESOURCE_LOCATION_INTERNAL + ";";
        } else if ("(Ljava/lang/String;)V".equals(initCall.desc)) {
            factoryMethodName = "parse";
            factoryDescriptor = "(Ljava/lang/String;)L" + RESOURCE_LOCATION_INTERNAL + ";";
        } else {
            return;
        }

        // Walk backward to locate the matching NEW ResourceLocation and DUP instructions
        AbstractInsnNode cursor = initCall.getPrevious();
        while (cursor != null) {
            if (cursor instanceof TypeInsnNode typeInsn
                    && typeInsn.getOpcode() == Opcodes.NEW
                    && RESOURCE_LOCATION_INTERNAL.equals(typeInsn.desc)) {
                AbstractInsnNode afterNew = typeInsn.getNext();
                if (afterNew != null && afterNew.getOpcode() == Opcodes.DUP) {
                    instructions.remove(afterNew);
                }
                instructions.remove(typeInsn);
                break;
            }
            cursor = cursor.getPrevious();
        }

        MethodInsnNode staticFactoryCall = new MethodInsnNode(
                Opcodes.INVOKESTATIC,
                RESOURCE_LOCATION_INTERNAL,
                factoryMethodName,
                factoryDescriptor,
                false);
        instructions.set(initCall, staticFactoryCall);
    }
}
