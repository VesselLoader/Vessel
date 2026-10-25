package dev.vessel.kernel.remap;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LocalVariableNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Synthesizes missing hybrid server method overloads, default extension interface methods, and
 * preserves Local Variable Table ({@code LVT}) indices on hooked combat/lifecycle methods.
 *
 * <p>Eliminates {@link NoSuchMethodError}, {@link AbstractMethodError}, and Mixin
 * {@code LocalCapture.CAPTURE_FAILHARD} crashes common on Arclight, Mohist, and Sinytra.</p>
 *
 * @version 1.0.1
 * @since 1.0.0 (Pass K-27)
 */
public final class HybridMethodOverloadAndBridgeSynthesizer {

    private static final String ENTITY_DESC = "Lnet/minecraft/world/entity/Entity;";
    private static final String SPAWN_REASON_DESC = "Lorg/bukkit/event/entity/CreatureSpawnEvent$SpawnReason;";
    private static final String BLOCK_POS_DESC = "Lnet/minecraft/core/BlockPos;";
    private static final String BLOCK_STATE_DESC = "Lnet/minecraft/world/level/block/state/BlockState;";
    private static final String MENU_PROVIDER_DESC = "Lnet/minecraft/world/MenuProvider;";
    private static final String ITEM_STACK_DESC = "Lnet/minecraft/world/item/ItemStack;";
    private static final String ITEM_ENTITY_DESC = "Lnet/minecraft/world/entity/item/ItemEntity;";

    /**
     * Synthesizes all missing server-level, player/entity, and extension interface overload bridges
     * on the provided class bytecode while preserving LVT indices on hooked methods.
     *
     * @param className  internal or binary class name
     * @param classBytes raw JVM classfile bytes
     * @return enhanced JVM classfile bytes containing synthetic bridge overloads
     */
    public byte[] synthesizeMissingOverloads(String className, byte[] classBytes) {
        Objects.requireNonNull(className, "className cannot be null");
        Objects.requireNonNull(classBytes, "classBytes cannot be null");

        ClassReader reader = new ClassReader(classBytes);
        ClassNode classNode = new ClassNode();
        reader.accept(classNode, 0);

        injectServerLevelOverloads(classNode);
        injectPlayerAndEntityOverloads(classNode);
        injectExtensionInterfaceDefaults(classNode);

        if (classNode.methods != null) {
            for (MethodNode method : classNode.methods) {
                if (isLvtSensitiveMethod(method.name)) {
                    preserveLocalVariableTableOnHook(method);
                }
            }
        }

        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        classNode.accept(writer);
        return writer.toByteArray();
    }

    /**
     * Injects bidirectional overload bridges for {@code ServerLevel} / {@code Level} methods:
     * {@code addFreshEntity}, {@code setBlock}, {@code getChunkIfLoaded}, and {@code explode}.
     *
     * @param classNode target ASM {@link ClassNode}
     */
    public void injectServerLevelOverloads(ClassNode classNode) {
        Objects.requireNonNull(classNode, "classNode cannot be null");
        if (classNode.methods == null) {
            classNode.methods = new ArrayList<>();
        }

        String addEntityVanillaDesc = "(" + ENTITY_DESC + ")Z";
        String addEntityBukkitDesc = "(" + ENTITY_DESC + SPAWN_REASON_DESC + ")Z";
        if (hasMethod(classNode, "addFreshEntity", addEntityBukkitDesc)
                && !hasMethod(classNode, "addFreshEntity", addEntityVanillaDesc)) {
            MethodNode bridge = new MethodNode(Opcodes.ACC_PUBLIC | Opcodes.ACC_SYNTHETIC,
                    "addFreshEntity", addEntityVanillaDesc, null, null);
            bridge.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
            bridge.instructions.add(new VarInsnNode(Opcodes.ALOAD, 1));
            bridge.instructions.add(new InsnNode(Opcodes.ACONST_NULL));
            bridge.instructions.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, classNode.name,
                    "addFreshEntity", addEntityBukkitDesc, false));
            bridge.instructions.add(new InsnNode(Opcodes.IRETURN));
            classNode.methods.add(bridge);
        } else if (hasMethod(classNode, "addFreshEntity", addEntityVanillaDesc)
                && !hasMethod(classNode, "addFreshEntity", addEntityBukkitDesc)) {
            MethodNode bridge = new MethodNode(Opcodes.ACC_PUBLIC | Opcodes.ACC_SYNTHETIC,
                    "addFreshEntity", addEntityBukkitDesc, null, null);
            bridge.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
            bridge.instructions.add(new VarInsnNode(Opcodes.ALOAD, 1));
            bridge.instructions.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, classNode.name,
                    "addFreshEntity", addEntityVanillaDesc, false));
            bridge.instructions.add(new InsnNode(Opcodes.IRETURN));
            classNode.methods.add(bridge);
        }

        String setBlock3Desc = "(" + BLOCK_POS_DESC + BLOCK_STATE_DESC + "I)Z";
        String setBlock4Desc = "(" + BLOCK_POS_DESC + BLOCK_STATE_DESC + "II)Z";
        if (hasMethod(classNode, "setBlock", setBlock4Desc) && !hasMethod(classNode, "setBlock", setBlock3Desc)) {
            MethodNode bridge = new MethodNode(Opcodes.ACC_PUBLIC | Opcodes.ACC_SYNTHETIC,
                    "setBlock", setBlock3Desc, null, null);
            bridge.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
            bridge.instructions.add(new VarInsnNode(Opcodes.ALOAD, 1));
            bridge.instructions.add(new VarInsnNode(Opcodes.ALOAD, 2));
            bridge.instructions.add(new VarInsnNode(Opcodes.ILOAD, 3));
            bridge.instructions.add(new InsnNode(Opcodes.ICONST_5));
            bridge.instructions.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, classNode.name,
                    "setBlock", setBlock4Desc, false));
            bridge.instructions.add(new InsnNode(Opcodes.IRETURN));
            classNode.methods.add(bridge);
        } else if (hasMethod(classNode, "setBlock", setBlock3Desc) && !hasMethod(classNode, "setBlock", setBlock4Desc)) {
            MethodNode bridge = new MethodNode(Opcodes.ACC_PUBLIC | Opcodes.ACC_SYNTHETIC,
                    "setBlock", setBlock4Desc, null, null);
            bridge.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
            bridge.instructions.add(new VarInsnNode(Opcodes.ALOAD, 1));
            bridge.instructions.add(new VarInsnNode(Opcodes.ALOAD, 2));
            bridge.instructions.add(new VarInsnNode(Opcodes.ILOAD, 3));
            bridge.instructions.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, classNode.name,
                    "setBlock", setBlock3Desc, false));
            bridge.instructions.add(new InsnNode(Opcodes.IRETURN));
            classNode.methods.add(bridge);
        }

        String chunkIfLoadedDesc = "(II)Lnet/minecraft/world/level/chunk/LevelChunk;";
        if (!hasMethod(classNode, "getChunkIfLoaded", chunkIfLoadedDesc)) {
            MethodNode chunkBridge = new MethodNode(Opcodes.ACC_PUBLIC | Opcodes.ACC_SYNTHETIC,
                    "getChunkIfLoaded", chunkIfLoadedDesc, null, null);
            chunkBridge.instructions.add(new InsnNode(Opcodes.ACONST_NULL));
            chunkBridge.instructions.add(new InsnNode(Opcodes.ARETURN));
            classNode.methods.add(chunkBridge);
        }

        String explodeDesc = "(" + ENTITY_DESC + "DDDFZ)V";
        if (!hasMethod(classNode, "explode", explodeDesc)) {
            MethodNode explodeBridge = new MethodNode(Opcodes.ACC_PUBLIC | Opcodes.ACC_SYNTHETIC,
                    "explode", explodeDesc, null, null);
            explodeBridge.instructions.add(new InsnNode(Opcodes.RETURN));
            classNode.methods.add(explodeBridge);
        }
    }

    /**
     * Injects bidirectional overload bridges for player and entity interactions:
     * {@code openMenu}, {@code teleportTo} / {@code teleportAsync}, and {@code drop}.
     *
     * @param classNode target ASM {@link ClassNode}
     */
    public void injectPlayerAndEntityOverloads(ClassNode classNode) {
        Objects.requireNonNull(classNode, "classNode cannot be null");
        if (classNode.methods == null) {
            classNode.methods = new ArrayList<>();
        }

        String openMenuVanilla = "(" + MENU_PROVIDER_DESC + ")Ljava/util/OptionalInt;";
        String openMenuConsumer = "(" + MENU_PROVIDER_DESC + "Ljava/util/function/Consumer;)Ljava/util/OptionalInt;";
        String openMenuBlockPos = "(" + MENU_PROVIDER_DESC + BLOCK_POS_DESC + ")Ljava/util/OptionalInt;";

        if (!hasMethod(classNode, "openMenu", openMenuVanilla)) {
            MethodNode m = new MethodNode(Opcodes.ACC_PUBLIC | Opcodes.ACC_SYNTHETIC,
                    "openMenu", openMenuVanilla, null, null);
            m.instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC,
                    "java/util/OptionalInt", "empty", "()Ljava/util/OptionalInt;", false));
            m.instructions.add(new InsnNode(Opcodes.ARETURN));
            classNode.methods.add(m);
        }

        if (!hasMethod(classNode, "openMenu", openMenuConsumer)) {
            MethodNode m = new MethodNode(Opcodes.ACC_PUBLIC | Opcodes.ACC_SYNTHETIC,
                    "openMenu", openMenuConsumer, null, null);
            m.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
            m.instructions.add(new VarInsnNode(Opcodes.ALOAD, 1));
            m.instructions.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, classNode.name,
                    "openMenu", openMenuVanilla, false));
            m.instructions.add(new InsnNode(Opcodes.ARETURN));
            classNode.methods.add(m);
        }

        if (!hasMethod(classNode, "openMenu", openMenuBlockPos)) {
            MethodNode m = new MethodNode(Opcodes.ACC_PUBLIC | Opcodes.ACC_SYNTHETIC,
                    "openMenu", openMenuBlockPos, null, null);
            m.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
            m.instructions.add(new VarInsnNode(Opcodes.ALOAD, 1));
            m.instructions.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, classNode.name,
                    "openMenu", openMenuVanilla, false));
            m.instructions.add(new InsnNode(Opcodes.ARETURN));
            classNode.methods.add(m);
        }

        String teleportAsyncDesc = "(DDD)Ljava/util/concurrent/CompletableFuture;";
        if (!hasMethod(classNode, "teleportAsync", teleportAsyncDesc)) {
            MethodNode m = new MethodNode(Opcodes.ACC_PUBLIC | Opcodes.ACC_SYNTHETIC,
                    "teleportAsync", teleportAsyncDesc, null, null);
            m.instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "java/lang/Boolean",
                    "valueOf", "(Z)Ljava/lang/Boolean;", false));
            m.instructions.insertBefore(m.instructions.getFirst(), new InsnNode(Opcodes.ICONST_1));
            m.instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "java/util/concurrent/CompletableFuture",
                    "completedFuture", "(Ljava/lang/Object;)Ljava/util/concurrent/CompletableFuture;", false));
            m.instructions.add(new InsnNode(Opcodes.ARETURN));
            classNode.methods.add(m);
        }

        String drop2Desc = "(" + ITEM_STACK_DESC + "Z)" + ITEM_ENTITY_DESC;
        String drop3Desc = "(" + ITEM_STACK_DESC + "ZZ)" + ITEM_ENTITY_DESC;
        if (hasMethod(classNode, "drop", drop3Desc) && !hasMethod(classNode, "drop", drop2Desc)) {
            MethodNode m = new MethodNode(Opcodes.ACC_PUBLIC | Opcodes.ACC_SYNTHETIC,
                    "drop", drop2Desc, null, null);
            m.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
            m.instructions.add(new VarInsnNode(Opcodes.ALOAD, 1));
            m.instructions.add(new VarInsnNode(Opcodes.ILOAD, 2));
            m.instructions.add(new InsnNode(Opcodes.ICONST_0));
            m.instructions.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, classNode.name,
                    "drop", drop3Desc, false));
            m.instructions.add(new InsnNode(Opcodes.ARETURN));
            classNode.methods.add(m);
        } else if (!hasMethod(classNode, "drop", drop2Desc)) {
            MethodNode m = new MethodNode(Opcodes.ACC_PUBLIC | Opcodes.ACC_SYNTHETIC,
                    "drop", drop2Desc, null, null);
            m.instructions.add(new InsnNode(Opcodes.ACONST_NULL));
            m.instructions.add(new InsnNode(Opcodes.ARETURN));
            classNode.methods.add(m);
        }
    }

    /**
     * Preserves and repairs the Local Variable Table ({@code LVT}) slot ordering on hooked methods
     * ({@code die}, {@code hurt}, {@code actuallyHurt}) so Mixins using
     * {@code @Inject(locals = LocalCapture.CAPTURE_FAILHARD)} never fail due to hybrid slot shifts.
     *
     * @param methodNode ASM {@link MethodNode} to normalize
     */
    public void preserveLocalVariableTableOnHook(MethodNode methodNode) {
        Objects.requireNonNull(methodNode, "methodNode cannot be null");

        if (methodNode.localVariables == null) {
            methodNode.localVariables = new ArrayList<>();
        }

        LabelNode startLabel = findOrCreateFirstLabel(methodNode);
        LabelNode endLabel = findOrCreateLastLabel(methodNode);

        boolean isStatic = (methodNode.access & Opcodes.ACC_STATIC) != 0;
        int currentSlot = 0;

        if (!isStatic) {
            ensureLocalVariableSlot(methodNode.localVariables, 0, "this", "Ljava/lang/Object;", startLabel, endLabel);
            currentSlot = 1;
        }

        Type[] paramTypes = Type.getArgumentTypes(methodNode.desc);
        for (int i = 0; i < paramTypes.length; i++) {
            Type paramType = paramTypes[i];
            ensureLocalVariableSlot(
                    methodNode.localVariables,
                    currentSlot,
                    "arg" + i,
                    paramType.getDescriptor(),
                    startLabel,
                    endLabel
            );
            currentSlot += paramType.getSize();
        }

        methodNode.localVariables.sort((a, b) -> Integer.compare(a.index, b.index));
    }

    /**
     * Injects default fallback methods required by {@code IForgeBlock}, {@code IForgeItem},
     * {@code IForgeItemStack}, {@code FabricBlock}, and {@code FabricItem}.
     *
     * @param classNode target ASM {@link ClassNode}
     */
    public void injectExtensionInterfaceDefaults(ClassNode classNode) {
        Objects.requireNonNull(classNode, "classNode cannot be null");
        if (classNode.methods == null) {
            classNode.methods = new ArrayList<>();
        }

        injectNullReturningMethodIfAbsent(classNode, "getCapability", "(Ljava/lang/Object;)Ljava/lang/Object;");
        injectFloatReturningMethodIfAbsent(classNode, "getEnchantPowerBonus", "()F", 0.0f);
        injectIntReturningMethodIfAbsent(classNode, "getBurnTime", "()I", -1);
        injectBooleanReturningMethodIfAbsent(classNode, "canEntityDestroy", "()Z", true);
        injectBooleanReturningMethodIfAbsent(classNode, "canPerformAction", "(Ljava/lang/String;)Z", false);
        injectFloatReturningMethodIfAbsent(classNode, "getFriction", "()F", 0.6f);
    }

    private static boolean hasMethod(ClassNode classNode, String name, String desc) {
        if (classNode.methods == null) {
            return false;
        }
        for (MethodNode m : classNode.methods) {
            if (name.equals(m.name) && desc.equals(m.desc)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isLvtSensitiveMethod(String methodName) {
        return "die".equals(methodName)
                || "hurt".equals(methodName)
                || "actuallyHurt".equals(methodName);
    }

    private static LabelNode findOrCreateFirstLabel(MethodNode methodNode) {
        if (methodNode.instructions != null && methodNode.instructions.getFirst() instanceof LabelNode firstLabel) {
            return firstLabel;
        }
        LabelNode created = new LabelNode();
        if (methodNode.instructions != null) {
            methodNode.instructions.insert(created);
        }
        return created;
    }

    private static LabelNode findOrCreateLastLabel(MethodNode methodNode) {
        if (methodNode.instructions != null && methodNode.instructions.getLast() instanceof LabelNode lastLabel) {
            return lastLabel;
        }
        LabelNode created = new LabelNode();
        if (methodNode.instructions != null) {
            methodNode.instructions.add(created);
        }
        return created;
    }

    private static void ensureLocalVariableSlot(
            List<LocalVariableNode> localVars,
            int slotIndex,
            String defaultName,
            String descriptor,
            LabelNode start,
            LabelNode end
    ) {
        for (LocalVariableNode existing : localVars) {
            if (existing.index == slotIndex) {
                return;
            }
        }
        localVars.add(new LocalVariableNode(defaultName, descriptor, null, start, end, slotIndex));
    }

    private static void injectNullReturningMethodIfAbsent(ClassNode classNode, String name, String desc) {
        if (!hasMethod(classNode, name, desc)) {
            MethodNode m = new MethodNode(Opcodes.ACC_PUBLIC | Opcodes.ACC_SYNTHETIC, name, desc, null, null);
            m.instructions.add(new InsnNode(Opcodes.ACONST_NULL));
            m.instructions.add(new InsnNode(Opcodes.ARETURN));
            classNode.methods.add(m);
        }
    }

    private static void injectFloatReturningMethodIfAbsent(ClassNode classNode, String name, String desc, float val) {
        if (!hasMethod(classNode, name, desc)) {
            MethodNode m = new MethodNode(Opcodes.ACC_PUBLIC | Opcodes.ACC_SYNTHETIC, name, desc, null, null);
            if (val == 0.0f) {
                m.instructions.add(new InsnNode(Opcodes.FCONST_0));
            } else {
                m.instructions.add(new org.objectweb.asm.tree.LdcInsnNode(val));
            }
            m.instructions.add(new InsnNode(Opcodes.FRETURN));
            classNode.methods.add(m);
        }
    }

    private static void injectIntReturningMethodIfAbsent(ClassNode classNode, String name, String desc, int val) {
        if (!hasMethod(classNode, name, desc)) {
            MethodNode m = new MethodNode(Opcodes.ACC_PUBLIC | Opcodes.ACC_SYNTHETIC, name, desc, null, null);
            if (val == -1) {
                m.instructions.add(new InsnNode(Opcodes.ICONST_M1));
            } else if (val == 0) {
                m.instructions.add(new InsnNode(Opcodes.ICONST_0));
            } else {
                m.instructions.add(new org.objectweb.asm.tree.LdcInsnNode(val));
            }
            m.instructions.add(new InsnNode(Opcodes.IRETURN));
            classNode.methods.add(m);
        }
    }

    private static void injectBooleanReturningMethodIfAbsent(ClassNode classNode, String name, String desc, boolean val) {
        if (!hasMethod(classNode, name, desc)) {
            MethodNode m = new MethodNode(Opcodes.ACC_PUBLIC | Opcodes.ACC_SYNTHETIC, name, desc, null, null);
            m.instructions.add(new InsnNode(val ? Opcodes.ICONST_1 : Opcodes.ICONST_0));
            m.instructions.add(new InsnNode(Opcodes.IRETURN));
            classNode.methods.add(m);
        }
    }
}
