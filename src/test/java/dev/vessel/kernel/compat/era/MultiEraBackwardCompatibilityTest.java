package dev.vessel.kernel.compat.era;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.vessel.kernel.identity.SourceLoaderKind;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;

/**
 * Unit tests for Sub-Stage 7.2: Multi-Era Lower-Version Mod &amp; Plugin Backward-Compatibility
 * Engine (`dev.vessel.kernel.compat.era`).
 *
 * @version 1.0.0
 */
class MultiEraBackwardCompatibilityTest {

    @Test
    void testVersionRangeRelaxerPacketUpgraderAndDataPackSchemaUpgrader() {
        UniversalVersionRangeRelaxer relaxer = new UniversalVersionRangeRelaxer();
        String legacyFabricJson = "{\"depends\":{\"minecraft\":\"~1.20.1\"}}";
        String relaxed = relaxer.relaxMetadataVersionConstraints(legacyFabricJson, "1.21.4");
        assertTrue(relaxed.contains(">=1.21.4"));
        assertTrue(relaxer.isVersionCompatibleUnderUniversalMode("[1.16.5,1.17)", "1.21.4", true));

        LegacyModPacketAndCodecUpgrader packetUpgrader = new LegacyModPacketAndCodecUpgrader();
        byte[] upgradedPacket = packetUpgrader.upgradeLegacyPacketPayload(
                "Create",
                "kinetic_sync",
                new byte[]{1, 2, 3, 4});
        assertEquals("create:kinetic_sync", packetUpgrader.extractCanonicalPayloadId(upgradedPacket));

        LegacyRecipeAndLootSchemaUpgrader schemaUpgrader = new LegacyRecipeAndLootSchemaUpgrader();
        assertEquals(
                "data/create/loot_table/blocks/cogwheel.json",
                schemaUpgrader.upgradeDataPackResourcePath("data/create/loot_tables/blocks/cogwheel.json"));
        assertEquals(
                "data/create/recipe/mechanical_crafting/crushing_wheel.json",
                schemaUpgrader.upgradeDataPackResourcePath(
                        "data/create/recipes/mechanical_crafting/crushing_wheel.json"));
        assertEquals(
                "data/forge/tags/item/ingots/copper.json",
                schemaUpgrader.upgradeDataPackResourcePath("data/forge/tags/items/ingots/copper.json"));

        String legacyRecipeJson = """
                {
                  "type": "minecraft:crafting_shaped",
                  "result": {
                    "item": "create:cogwheel",
                    "count": 1
                  }
                }
                """;
        String upgradedRecipeJson = schemaUpgrader.upgradeLegacyRecipeOrLootJson(legacyRecipeJson);
        assertTrue(upgradedRecipeJson.contains("\"id\": \"create:cogwheel\""));
    }

    @Test
    void testCrossEraMinecraftBytecodeRewriterReplacesResourceLocationInitAndEntityLevelField() {
        ClassWriter cw = new ClassWriter(0);
        cw.visit(
                Opcodes.V21,
                Opcodes.ACC_PUBLIC | Opcodes.ACC_SUPER,
                "com/example/legacy120/LegacyModClass",
                null,
                "java/lang/Object",
                null);

        MethodVisitor mv = cw.visitMethod(
                Opcodes.ACC_PUBLIC,
                "createIdAndGetLevel",
                "(Lnet/minecraft/world/entity/Entity;)Lnet/minecraft/resources/ResourceLocation;",
                null,
                null);
        mv.visitCode();
        // Legacy 1.16-1.19.4 field access: entity.level
        mv.visitVarInsn(Opcodes.ALOAD, 1);
        mv.visitFieldInsn(
                Opcodes.GETFIELD,
                "net/minecraft/world/entity/Entity",
                "level",
                "Lnet/minecraft/world/level/Level;");
        mv.visitInsn(Opcodes.POP);

        // Legacy <= 1.20.6 constructor: new ResourceLocation("create", "cogwheel")
        mv.visitTypeInsn(Opcodes.NEW, "net/minecraft/resources/ResourceLocation");
        mv.visitInsn(Opcodes.DUP);
        mv.visitLdcInsn("create");
        mv.visitLdcInsn("cogwheel");
        mv.visitMethodInsn(
                Opcodes.INVOKESPECIAL,
                "net/minecraft/resources/ResourceLocation",
                "<init>",
                "(Ljava/lang/String;Ljava/lang/String;)V",
                false);
        mv.visitInsn(Opcodes.ARETURN);
        mv.visitMaxs(4, 2);
        mv.visitEnd();
        cw.visitEnd();

        MultiEraBytecodeTransformationPipeline pipeline =
                new MultiEraBytecodeTransformationPipeline();
        byte[] upgradedBytes = pipeline.transformLowerVersionClassForLatestServer(
                cw.toByteArray(),
                SourceLoaderKind.FORGE,
                "1.19.2",
                "1.21.4");

        ClassNode inspected = new ClassNode();
        new ClassReader(upgradedBytes).accept(inspected, 0);
        boolean foundFromNamespaceAndPath = false;
        boolean foundLevelMethodCall = false;
        for (AbstractInsnNode insn : inspected.methods.get(0).instructions) {
            if (insn instanceof MethodInsnNode mInsn) {
                if (mInsn.getOpcode() == Opcodes.INVOKESTATIC
                        && "net/minecraft/resources/ResourceLocation".equals(mInsn.owner)
                        && "fromNamespaceAndPath".equals(mInsn.name)) {
                    foundFromNamespaceAndPath = true;
                }
                if (mInsn.getOpcode() == Opcodes.INVOKEVIRTUAL
                        && "net/minecraft/world/entity/Entity".equals(mInsn.owner)
                        && "level".equals(mInsn.name)
                        && "()Lnet/minecraft/world/level/Level;".equals(mInsn.desc)) {
                    foundLevelMethodCall = true;
                }
            }
        }
        assertTrue(foundFromNamespaceAndPath);
        assertTrue(foundLevelMethodCall);
    }

    @Test
    void testLegacyBukkitBridgeUpgradesInventoryViewInvokeVirtualToInvokeInterface() {
        ClassWriter cw = new ClassWriter(0);
        cw.visit(
                Opcodes.V21,
                Opcodes.ACC_PUBLIC | Opcodes.ACC_SUPER,
                "com/example/plugin116/LegacyGuiPlugin",
                null,
                "java/lang/Object",
                null);
        MethodVisitor mv = cw.visitMethod(
                Opcodes.ACC_PUBLIC,
                "readTitle",
                "(Lorg/bukkit/inventory/InventoryView;)Ljava/lang/String;",
                null,
                null);
        mv.visitCode();
        mv.visitVarInsn(Opcodes.ALOAD, 1);
        mv.visitMethodInsn(
                Opcodes.INVOKEVIRTUAL,
                "org/bukkit/inventory/InventoryView",
                "getTitle",
                "()Ljava/lang/String;",
                false);
        mv.visitInsn(Opcodes.ARETURN);
        mv.visitMaxs(1, 2);
        mv.visitEnd();
        cw.visitEnd();

        MultiEraBytecodeTransformationPipeline pipeline =
                new MultiEraBytecodeTransformationPipeline();
        byte[] upgradedBytes = pipeline.transformLowerVersionClassForLatestServer(
                cw.toByteArray(),
                SourceLoaderKind.SPIGOT,
                "1.16.5",
                "1.21.4");

        ClassNode inspected = new ClassNode();
        new ClassReader(upgradedBytes).accept(inspected, 0);
        boolean foundInvokeInterface = false;
        for (AbstractInsnNode insn : inspected.methods.get(0).instructions) {
            if (insn instanceof MethodInsnNode mInsn
                    && mInsn.getOpcode() == Opcodes.INVOKEINTERFACE
                    && "org/bukkit/inventory/InventoryView".equals(mInsn.owner)
                    && "getTitle".equals(mInsn.name)
                    && mInsn.itf) {
                foundInvokeInterface = true;
            }
        }
        assertTrue(foundInvokeInterface);

        LegacyBukkitApiVersionBridge bukkitBridge = new LegacyBukkitApiVersionBridge();
        assertEquals("CRAFTING_TABLE", bukkitBridge.normalizeLegacyMaterialName("LEGACY_WORKBENCH"));
        assertEquals("SHORT_GRASS", bukkitBridge.normalizeLegacyMaterialName("GRASS"));
    }
}
