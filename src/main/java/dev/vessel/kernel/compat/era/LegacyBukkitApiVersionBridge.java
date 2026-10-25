package dev.vessel.kernel.compat.era;

import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

/**
 * Provides bytecode and Material compatibility for Bukkit, Spigot, and Paper plugins compiled
 * against older API versions (1.8.8 through 1.20.6) when running on Minecraft 1.21+ servers.
 *
 * <p>Specifically upgrades {@code INVOKEVIRTUAL org/bukkit/inventory/InventoryView.*} calls to
 * {@code INVOKEINTERFACE org/bukkit/inventory/InventoryView.*}, preventing the breaking
 * {@code IncompatibleClassChangeError} introduced when {@code InventoryView} changed from an
 * abstract class to an interface in 1.21.</p>
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public final class LegacyBukkitApiVersionBridge {

    private static final String INVENTORY_VIEW_INTERNAL = "org/bukkit/inventory/InventoryView";

    private static final Map<String, String> LEGACY_MATERIAL_ALIASES = Map.ofEntries(
            Map.entry("WORKBENCH", "CRAFTING_TABLE"),
            Map.entry("WATCH", "CLOCK"),
            Map.entry("GOLD_SPADE", "GOLDEN_SHOVEL"),
            Map.entry("WOOD_PICKAXE", "WOODEN_PICKAXE"),
            Map.entry("SIGN_POST", "OAK_SIGN"),
            Map.entry("WALL_SIGN", "OAK_WALL_SIGN"),
            Map.entry("GRASS_PATH", "DIRT_PATH"),
            Map.entry("ZOMBIE_PIGMAN_SPAWN_EGG", "ZOMBIFIED_PIGLIN_SPAWN_EGG"),
            Map.entry("GRASS", "SHORT_GRASS"),
            Map.entry("SKULL_ITEM", "PLAYER_HEAD"));

    /**
     * Rewrites legacy Bukkit plugin bytecode instructions so plugins compiled for 1.8–1.20.6
     * execute cleanly against the modern 1.21+ Bukkit/Paper API surface.
     *
     * @param classBytes the raw plugin class bytecode
     * @return the upgraded class bytecode
     */
    public byte[] rewriteLegacyBukkitPluginBytecode(byte[] classBytes) {
        Objects.requireNonNull(classBytes, "classBytes must not be null");
        ClassNode classNode = new ClassNode();
        ClassReader reader = new ClassReader(classBytes);
        reader.accept(classNode, 0);

        for (MethodNode method : classNode.methods) {
            for (AbstractInsnNode insn : method.instructions) {
                if (insn instanceof MethodInsnNode methodInsn
                        && methodInsn.getOpcode() == Opcodes.INVOKEVIRTUAL
                        && INVENTORY_VIEW_INTERNAL.equals(methodInsn.owner)) {
                    methodInsn.setOpcode(Opcodes.INVOKEINTERFACE);
                    methodInsn.itf = true;
                }
            }
        }

        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        classNode.accept(writer);
        return writer.toByteArray();
    }

    /**
     * Normalizes pre-1.13 (`LEGACY_*`, `WORKBENCH`, `WATCH`) and pre-1.20.5 (`GRASS` &rarr;
     * `SHORT_GRASS`) Material enum names to their canonical modern Minecraft 1.21+ names.
     *
     * @param rawMaterialName the legacy Material name requested by an older plugin
     * @return the canonical modern Material enum name
     */
    public String normalizeLegacyMaterialName(String rawMaterialName) {
        Objects.requireNonNull(rawMaterialName, "rawMaterialName must not be null");
        String upper = rawMaterialName.trim().toUpperCase(Locale.ROOT);
        if (upper.startsWith("LEGACY_")) {
            upper = upper.substring("LEGACY_".length());
        }
        return LEGACY_MATERIAL_ALIASES.getOrDefault(upper, upper);
    }
}
