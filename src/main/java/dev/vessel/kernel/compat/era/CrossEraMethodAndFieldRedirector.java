package dev.vessel.kernel.compat.era;

import dev.vessel.kernel.compat.bytecode.AsmClassReaderBytecodeSanitizer;
import java.util.Objects;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InvokeDynamicInsnNode;
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
 * @version 1.0.13
 */
public final class CrossEraMethodAndFieldRedirector {

    private static final String RESOURCE_LOCATION_INTERNAL =
            "net/minecraft/resources/ResourceLocation";
    private static final String LEVEL_DESC =
            "Lnet/minecraft/world/level/Level;";

    private static final AsmClassReaderBytecodeSanitizer SANITIZER =
            new AsmClassReaderBytecodeSanitizer();

    /**
     * Rewrites legacy Minecraft method and field instructions in {@code classBytes} to match the
     * latest Minecraft server API.
     *
     * @param classBytes the raw class bytecode from a lower-version mod
     * @return the upgraded class bytecode
     */
    public byte[] rewriteLegacyMinecraftCalls(byte[] classBytes) {
        Objects.requireNonNull(classBytes, "classBytes must not be null");
        int origMajor = SANITIZER.extractMajorVersion(classBytes);
        ClassNode classNode = new ClassNode();
        ClassReader reader = SANITIZER.createSafeClassReader(classBytes);
        reader.accept(classNode, 0);

        for (MethodNode method : classNode.methods) {
            rewriteMethodInstructions(method.instructions);
        }

        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        classNode.accept(writer);
        byte[] producedBytes = writer.toByteArray();
        return SANITIZER.restoreOriginalMajorVersion(producedBytes, origMajor);
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
                } else if (methodInsn.getOpcode() == Opcodes.INVOKEVIRTUAL
                        && "net/minecraft/commands/CommandSourceStack".equals(methodInsn.owner)
                        && ("m_6761_".equals(methodInsn.name) || "hasPermission".equals(methodInsn.name))) {
                    MethodInsnNode replacement = new MethodInsnNode(
                            Opcodes.INVOKESTATIC,
                            "dev/vessel/kernel/compat/era/CommandPermissionBridge",
                            "hasPermission",
                            "(Ljava/lang/Object;I)Z",
                            false);
                    instructions.set(methodInsn, replacement);
                } else if (methodInsn.getOpcode() == Opcodes.INVOKEVIRTUAL
                        && "net/minecraft/ChatFormatting".equals(methodInsn.owner)
                        && ("m_126665_".equals(methodInsn.name) || "getColor".equals(methodInsn.name))) {
                    MethodInsnNode replacement = new MethodInsnNode(
                            Opcodes.INVOKESTATIC,
                            "dev/vessel/kernel/compat/era/ChatFormattingColorBridge",
                            "getColor",
                            "(Ljava/lang/Object;)Ljava/lang/Integer;",
                            false);
                    instructions.set(methodInsn, replacement);
                } else if (methodInsn.getOpcode() == Opcodes.INVOKESTATIC
                        && "java/util/ServiceLoader".equals(methodInsn.owner)
                        && "load".equals(methodInsn.name)
                        && "(Ljava/lang/Class;)Ljava/util/ServiceLoader;".equals(methodInsn.desc)) {
                    methodInsn.owner = "dev/vessel/kernel/compat/era/CrossEraServiceLoaderBridge";
                } else {
                    methodInsn.name = remapMethodName(methodInsn.owner, methodInsn.name, methodInsn.desc);
                }
            } else if (current instanceof InvokeDynamicInsnNode indyInsn) {
                if (indyInsn.bsmArgs != null) {
                    for (int i = 0; i < indyInsn.bsmArgs.length; i++) {
                        if (indyInsn.bsmArgs[i] instanceof Handle handle) {
                            String remapped = remapMethodName(handle.getOwner(), handle.getName(), handle.getDesc());
                            if (!remapped.equals(handle.getName())) {
                                indyInsn.bsmArgs[i] = new Handle(
                                        handle.getTag(),
                                        handle.getOwner(),
                                        remapped,
                                        handle.getDesc(),
                                        handle.isInterface());
                            }
                        }
                    }
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

    private static String remapMethodName(String owner, String name, String desc) {
        if (owner == null || name == null) {
            return name;
        }
        if ("net/minecraft/commands/Commands".equals(owner)
                || "net/minecraft/command/Commands".equals(owner)) {
            if ("m_82127_".equals(name)) {
                return "literal";
            } else if ("m_82129_".equals(name)) {
                return "argument";
            }
        } else if ("net/minecraft/commands/arguments/coordinates/BlockPosArgument".equals(owner)
                || "net/minecraft/command/arguments/coordinates/BlockPosArgument".equals(owner)) {
            if ("m_118239_".equals(name)) {
                return "blockPos";
            }
        } else if ("net/minecraft/commands/arguments/coordinates/ColumnPosArgument".equals(owner)
                || "net/minecraft/command/arguments/coordinates/ColumnPosArgument".equals(owner)) {
            if ("m_118989_".equals(name)) {
                return "columnPos";
            } else if ("m_118991_".equals(name)) {
                return "getColumnPos";
            }
        } else if ("net/minecraft/commands/arguments/coordinates/Vec3Argument".equals(owner)
                || "net/minecraft/command/arguments/coordinates/Vec3Argument".equals(owner)) {
            if ("m_120841_".equals(name) || "m_120842_".equals(name)) {
                return "vec3";
            } else if ("m_120849_".equals(name)) {
                return "getCoordinates";
            } else if ("m_120844_".equals(name) || "m_120845_".equals(name)) {
                return "getVec3";
            }
        } else if ("net/minecraft/commands/arguments/DimensionArgument".equals(owner)
                || "net/minecraft/command/arguments/DimensionArgument".equals(owner)) {
            if ("m_88805_".equals(name) || "m_88806_".equals(name)) {
                return "dimension";
            } else if ("m_88808_".equals(name)) {
                return "getDimension";
            }
        } else if ("net/minecraft/commands/arguments/UuidArgument".equals(owner)
                || "net/minecraft/command/arguments/UuidArgument".equals(owner)) {
            if ("m_113850_".equals(name)) {
                return "uuid";
            } else if ("m_113853_".equals(name)) {
                return "getUuid";
            }
        } else if ("net/minecraft/commands/arguments/EntityArgument".equals(owner)
                || "net/minecraft/command/arguments/EntityArgument".equals(owner)) {
            if ("m_91470_".equals(name)) {
                return "player";
            } else if ("m_91473_".equals(name) || "m_91474_".equals(name)) {
                return "players";
            } else if ("m_91449_".equals(name)) {
                return "entity";
            } else if ("m_91460_".equals(name) || "m_91461_".equals(name)) {
                if (desc != null && desc.startsWith("()")) {
                    return "entities";
                } else {
                    return "getEntities";
                }
            } else if ("m_91452_".equals(name) || "m_91453_".equals(name)) {
                return "getEntity";
            } else if ("m_91477_".equals(name) || "m_91478_".equals(name)) {
                return "getPlayer";
            } else if ("m_91481_".equals(name) || "m_91482_".equals(name)) {
                return "getPlayers";
            }
        } else if ("net/minecraft/commands/CommandSourceStack".equals(owner)) {
            if ("m_81373_".equals(name)) {
                return "getEntity";
            } else if ("m_81377_".equals(name)) {
                return "getPlayer";
            } else if ("m_81372_".equals(name)) {
                return "getServer";
            } else if ("m_81374_".equals(name)) {
                return "getLevel";
            } else if ("m_81371_".equals(name)) {
                return "getPosition";
            } else if ("m_288197_".equals(name)) {
                return "sendSuccess";
            } else if ("m_81352_".equals(name)) {
                return "sendFailure";
            } else if ("m_81375_".equals(name)) {
                return "getPlayerOrException";
            } else if ("m_230897_".equals(name)) {
                return "isPlayer";
            }
        } else if ("net/minecraft/core/BlockPos".equals(owner)
                || "net/minecraft/core/Vec3i".equals(owner)) {
            if ("m_123341_".equals(name)) {
                return "getX";
            } else if ("m_123342_".equals(name)) {
                return "getY";
            } else if ("m_123343_".equals(name)) {
                return "getZ";
            }
        } else if ("net/minecraft/network/chat/Component".equals(owner)
                || "net/minecraft/network/chat/MutableComponent".equals(owner)) {
            if ("m_237110_".equals(name) || "m_237113_".equals(name)) {
                return "translatable";
            } else if ("m_237115_".equals(name)) {
                return "literal";
            } else if ("m_237112_".equals(name)) {
                return "nullToEmpty";
            } else if ("m_130675_".equals(name) || "m_7383_".equals(name)) {
                return "getString";
            } else if ("m_130946_".equals(name) || "m_130948_".equals(name)) {
                return "withStyle";
            } else if ("m_7220_".equals(name)) {
                return "append";
            }
        }
        return name;
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
