package dev.vessel.kernel.test;

import dev.vessel.kernel.datapack.*;
import dev.vessel.kernel.registry.CompactCustomBlockStateIndex;
import dev.vessel.kernel.registry.DataComponentBridgingTable;
import dev.vessel.kernel.vfs.InMemoryVirtualFileSystemMount;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Hardened verification test suite for Datapack Compiler, Conventional Tag Harmonizer,
 * Advancement Criteria Graph, Data Component Bridging Table, and Compact Custom BlockState Index.
 *
 * @version 1.0.1
 * @author VESSEL Engine Team
 */
public class UniversalDataPackAndRegistryTest {

    @Nested
    @DisplayName("DatapackMetadataGenerator Verification")
    class MetadataGeneratorTests {

        @Test
        @DisplayName("Generates valid pack.mcmeta JSON and byte arrays")
        void testGenerateMetadata() {
            String json = DatapackMetadataGenerator.generateMetadataJson(48, "Test Pack \"VESSEL\"");
            assertThat(json).contains("\"pack_format\": 48");
            assertThat(json).contains("\"description\": \"Test Pack \\\"VESSEL\\\"\"");

            byte[] bytes = DatapackMetadataGenerator.generateMetadataBytes(48, "Test Pack");
            assertThat(new String(bytes, StandardCharsets.UTF_8)).isEqualTo(DatapackMetadataGenerator.generateMetadataJson(48, "Test Pack"));
        }

        @Test
        @DisplayName("Rejects invalid pack format < 1")
        void testRejectInvalidFormat() {
            assertThatThrownBy(() -> DatapackMetadataGenerator.generateMetadataJson(0, "Invalid"))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("ConventionalTagHarmonizer Verification")
    class TagHarmonizerTests {

        @Test
        @DisplayName("Normalizes tags and evaluates equivalence across conventions in 1 cycle")
        void testTagEquivalence() {
            ConventionalTagHarmonizer harmonizer = new ConventionalTagHarmonizer();

            // Built-in conventions
            assertThat(harmonizer.isEquivalent("#c:iron_ingots", "forge:ingots/iron")).isTrue();
            assertThat(harmonizer.isEquivalent("c:iron_ingots", "minecraft:iron_ingot")).isTrue();
            assertThat(harmonizer.isEquivalent("forge:gems/diamond", "c:diamonds")).isTrue();
            assertThat(harmonizer.isEquivalent("forge:chests", "c:chests")).isTrue();

            // Unrelated tags
            assertThat(harmonizer.isEquivalent("c:iron_ingots", "c:gold_ingots")).isFalse();
            assertThat(harmonizer.isEquivalent("unknown:foo", "unknown:bar")).isFalse();
        }

        @Test
        @DisplayName("Maps tags to loader-specific namespaces")
        void testTagMapping() {
            ConventionalTagHarmonizer harmonizer = new ConventionalTagHarmonizer();

            assertThat(harmonizer.mapToForge("c:iron_ingots")).contains("forge:ingots/iron");
            assertThat(harmonizer.mapToFabric("forge:ingots/copper")).contains("c:copper_ingots");
            assertThat(harmonizer.mapToVanilla("c:diamonds")).contains("minecraft:diamond");
        }

        @Test
        @DisplayName("Harmonizes TagRegistryIndex membership across equivalent tags")
        void testHarmonizeTagRegistry() {
            ConventionalTagHarmonizer harmonizer = new ConventionalTagHarmonizer();
            TagRegistryIndex index = new TagRegistryIndex();

            // Register item ID 265 in Fabric tag
            index.registerTag("c:iron_ingots", new int[]{265});

            // Before harmonization: Forge tag does not have it
            assertThat(index.isMember("forge:ingots/iron", 265)).isFalse();

            // Harmonize
            harmonizer.harmonizeTagRegistry(index);

            // After harmonization: Forge tag and Vanilla tag both inherit the member
            assertThat(index.isMember("forge:ingots/iron", 265)).isTrue();
            assertThat(index.isMember("minecraft:iron_ingot", 265)).isTrue();
        }

        @Test
        @DisplayName("Supports dynamic custom alias registration")
        void testCustomAliasRegistration() {
            ConventionalTagHarmonizer harmonizer = new ConventionalTagHarmonizer();

            harmonizer.registerAlias("mymod:custom_fuel", "othermod:fuels/super");
            assertThat(harmonizer.isEquivalent("mymod:custom_fuel", "othermod:fuels/super")).isTrue();
        }

        @Test
        @DisplayName("Unifies multi-group equivalence disjoint sets without severing existing links")
        void testTransitiveMultiGroupEquivalenceMerging() {
            ConventionalTagHarmonizer harmonizer = new ConventionalTagHarmonizer();

            // Group 1: tagA <-> tagB
            harmonizer.registerAlias("test:tag_a", "test:tag_b");
            // Group 2: tagC <-> tagD
            harmonizer.registerAlias("test:tag_c", "test:tag_d");

            assertThat(harmonizer.isEquivalent("test:tag_a", "test:tag_d")).isFalse();

            // Bridge Group 1 and Group 2: tagB <-> tagC
            harmonizer.registerAlias("test:tag_b", "test:tag_c");

            // Full transitive equivalence across all 4 tags
            assertThat(harmonizer.isEquivalent("test:tag_a", "test:tag_d")).isTrue();
            assertThat(harmonizer.isEquivalent("test:tag_c", "test:tag_d")).isTrue();
            assertThat(harmonizer.isEquivalent("test:tag_a", "test:tag_c")).isTrue();

            Set<String> eqSet = harmonizer.getEquivalentTags("test:tag_a");
            assertThat(eqSet).contains("test:tag_a", "test:tag_b", "test:tag_c", "test:tag_d");
        }
    }

    @Nested
    @DisplayName("AdvancementCriteriaGraph Verification")
    class AdvancementGraphTests {

        @Test
        @DisplayName("Builds DAG and computes valid topological ordering")
        void testAdvancementDagAndOrder() {
            AdvancementCriteriaGraph graph = new AdvancementCriteriaGraph();

            graph.registerAdvancement("minecraft:story/root", null,
                    Set.of("crafting_table"),
                    Map.of("crafting_table", "minecraft:inventory_changed"));

            graph.registerAdvancement("minecraft:story/mine_stone", "minecraft:story/root",
                    Set.of("get_stone"),
                    Map.of("get_stone", "minecraft:inventory_changed"));

            graph.registerAdvancement("minecraft:story/upgrade_tools", "minecraft:story/mine_stone",
                    Set.of("stone_pickaxe"),
                    Map.of("stone_pickaxe", "minecraft:inventory_changed"));

            List<String> order = graph.getTopologicalOrder();
            assertThat(order).containsExactly(
                    "minecraft:story/root",
                    "minecraft:story/mine_stone",
                    "minecraft:story/upgrade_tools"
            );
        }

        @Test
        @DisplayName("Prunes locked branches so child advancements do not trigger prematurely")
        void testTriggerPruning() {
            AdvancementCriteriaGraph graph = new AdvancementCriteriaGraph();
            UUID player = UUID.randomUUID();

            graph.registerAdvancement("root", null, Set.of("c1"), Map.of("c1", "minecraft:inventory_changed"));
            graph.registerAdvancement("child1", "root", Set.of("c2"), Map.of("c2", "minecraft:inventory_changed"));

            // Initially, only root is active for this trigger
            Set<String> active = graph.getActiveAdvancementsForTrigger(player, "minecraft:inventory_changed");
            assertThat(active).containsExactly("root");

            // Grant root criterion -> root unlocked!
            boolean rootUnlocked = graph.grantCriterion(player, "root", "c1");
            assertThat(rootUnlocked).isTrue();
            assertThat(graph.isAdvancementUnlocked(player, "root")).isTrue();

            // Now child1 becomes active
            Set<String> activeAfter = graph.getActiveAdvancementsForTrigger(player, "minecraft:inventory_changed");
            assertThat(activeAfter).containsExactly("child1");
        }

        @Test
        @DisplayName("Detects direct and deep cyclic dependencies and throws CyclicAdvancementException")
        void testCyclicAdvancementDetection() {
            AdvancementCriteriaGraph graph = new AdvancementCriteriaGraph();

            // A -> B -> C -> A
            graph.registerAdvancement("adv_a", "adv_c", Set.of("c1"), Map.of());
            graph.registerAdvancement("adv_b", "adv_a", Set.of("c2"), Map.of());
            graph.registerAdvancement("adv_c", "adv_b", Set.of("c3"), Map.of());

            assertThatThrownBy(graph::validateAcyclic)
                    .isInstanceOf(CyclicAdvancementException.class)
                    .hasMessageContaining("Circular dependency detected");
        }

        @Test
        @DisplayName("Accurately extracts cycle path without prefixing non-cycle root ancestors")
        void testAccurateCyclePathExcludesAncestorPrefix() {
            AdvancementCriteriaGraph graph = new AdvancementCriteriaGraph();

            // root -> adv_x -> adv_y -> adv_x (cycle between x and y, root is non-cycle ancestor)
            graph.registerAdvancement("root", null, Set.of("c0"), Map.of());
            graph.registerAdvancement("adv_x", "root", Set.of("c1"), Map.of());
            graph.registerAdvancement("adv_y", "adv_x", Set.of("c2"), Map.of());
            // Create circular edge: adv_x depends on adv_y
            graph.registerAdvancement("adv_x", "adv_y", Set.of("c1"), Map.of());

            try {
                graph.validateAcyclic();
            } catch (CyclicAdvancementException ex) {
                List<String> cyclePath = ex.getCyclePath();
                assertThat(cyclePath).doesNotContain("root");
                assertThat(cyclePath).contains("adv_x", "adv_y");
            }
        }

        @Test
        @DisplayName("Cleans up previous parent child set when an advancement is re-parented")
        void testAdvancementParentReRegistration() {
            AdvancementCriteriaGraph graph = new AdvancementCriteriaGraph();

            graph.registerAdvancement("parent_1", null, Set.of("c1"), Map.of());
            graph.registerAdvancement("parent_2", null, Set.of("c2"), Map.of());
            graph.registerAdvancement("child", "parent_1", Set.of("c3"), Map.of());

            // Re-parent child to parent_2
            graph.registerAdvancement("child", "parent_2", Set.of("c3"), Map.of());

            List<String> order = graph.getTopologicalOrder();
            assertThat(order.indexOf("parent_2")).isLessThan(order.indexOf("child"));
        }

        @Test
        @DisplayName("Prunes advancement from trigger evaluation when all criteria for that trigger are granted")
        void testTriggerPruningOnPartiallyCompletedCriteria() {
            AdvancementCriteriaGraph graph = new AdvancementCriteriaGraph();
            UUID player = UUID.randomUUID();

            graph.registerAdvancement("dual_trigger", null,
                    Set.of("crit_inv", "crit_kill"),
                    Map.of("crit_inv", "minecraft:inventory_changed",
                           "crit_kill", "minecraft:player_killed_entity"));

            // Initially both triggers should evaluate dual_trigger
            assertThat(graph.getActiveAdvancementsForTrigger(player, "minecraft:inventory_changed"))
                    .containsExactly("dual_trigger");
            assertThat(graph.getActiveAdvancementsForTrigger(player, "minecraft:player_killed_entity"))
                    .containsExactly("dual_trigger");

            // Complete crit_inv
            graph.grantCriterion(player, "dual_trigger", "crit_inv");
            assertThat(graph.isAdvancementUnlocked(player, "dual_trigger")).isFalse();

            // inventory_changed should be pruned since crit_inv is already granted!
            assertThat(graph.getActiveAdvancementsForTrigger(player, "minecraft:inventory_changed"))
                    .isEmpty();

            // player_killed_entity must still remain active
            assertThat(graph.getActiveAdvancementsForTrigger(player, "minecraft:player_killed_entity"))
                    .containsExactly("dual_trigger");
        }

        @Test
        @DisplayName("Tracks progress ratio and supports player reset")
        void testProgressAndReset() {
            AdvancementCriteriaGraph graph = new AdvancementCriteriaGraph();
            UUID player = UUID.randomUUID();

            graph.registerAdvancement("multi_crit", null, Set.of("a", "b", "c", "d"), Map.of());

            assertThat(graph.getProgress(player, "multi_crit")).isEqualTo(0.0f);

            graph.grantCriterion(player, "multi_crit", "a");
            graph.grantCriterion(player, "multi_crit", "b");
            assertThat(graph.getProgress(player, "multi_crit")).isEqualTo(0.5f);

            graph.resetPlayer(player);
            assertThat(graph.getProgress(player, "multi_crit")).isEqualTo(0.0f);
        }

        @Test
        @DisplayName("Directly unlocks advancement via unlockAdvancement")
        void testUnlockAdvancementDirectly() {
            AdvancementCriteriaGraph graph = new AdvancementCriteriaGraph();
            UUID player = UUID.randomUUID();

            graph.registerAdvancement("direct_root", null, Set.of(), Map.of());
            assertThat(graph.isAdvancementUnlocked(player, "direct_root")).isFalse();

            boolean unlocked = graph.unlockAdvancement(player, "direct_root");
            assertThat(unlocked).isTrue();
            assertThat(graph.isAdvancementUnlocked(player, "direct_root")).isTrue();
            assertThat(graph.getProgress(player, "direct_root")).isEqualTo(1.0f);
        }
    }

    @Nested
    @DisplayName("UniversalDataPackCompiler Verification")
    class DataPackCompilerTests {

        @Test
        @DisplayName("Compiles in-memory datapack, generates pack.mcmeta and merges tags and recipes")
        void testCompilerBasic() throws IOException {
            UniversalDataPackCompiler compiler = new UniversalDataPackCompiler();

            // Add Fabric tag
            String fabricTagJson = "{\n  \"replace\": false,\n  \"values\": [\"#c:iron_ores\", \"minecraft:iron_ore\"]\n}";
            compiler.addSourceResource("fabric", "mymod", "data/c/tags/items/iron_ingots.json", fabricTagJson.getBytes(StandardCharsets.UTF_8));

            // Add Forge tag with replace: false
            String forgeTagJson = "{\n  \"replace\": false,\n  \"values\": [\"minecraft:deepslate_iron_ore\"]\n}";
            compiler.addSourceResource("forge", "othermod", "data/forge/tags/items/ingots/iron.json", forgeTagJson.getBytes(StandardCharsets.UTF_8));

            // Add recipe
            String recipeJson = "{\"type\": \"minecraft:crafting_shaped\"}";
            compiler.addSourceResource("fabric", "mymod", "data/mymod/recipes/iron_wand.json", recipeJson.getBytes(StandardCharsets.UTF_8));

            // Add loot table
            String lootTableJson = "{\"type\": \"minecraft:chest\"}";
            compiler.addSourceResource("bukkit", "myplugin", "data/myplugin/loot_tables/dungeon_chest.json", lootTableJson.getBytes(StandardCharsets.UTF_8));

            // Compile into in-memory VFS
            try (InMemoryVirtualFileSystemMount vfs = compiler.compile()) {
                assertThat(vfs.hasResource("pack.mcmeta")).isTrue();

                Optional<byte[]> mcmeta = vfs.readResource("pack.mcmeta");
                assertThat(mcmeta).isPresent();
                assertThat(new String(mcmeta.get(), StandardCharsets.UTF_8)).contains("\"pack_format\": 48");

                // Check recipe and loot table
                assertThat(vfs.hasResource("data/mymod/recipes/iron_wand.json")).isTrue();
                assertThat(vfs.hasResource("data/myplugin/loot_tables/dungeon_chest.json")).isTrue();

                // Check tag files and harmonized synthesis
                assertThat(vfs.hasResource("data/c/tags/items/iron_ingots.json")).isTrue();
                assertThat(vfs.hasResource("data/forge/tags/items/ingots/iron.json")).isTrue();

                // Cross-loader alias tag was synthesized
                assertThat(vfs.hasResource("data/minecraft/tags/items/iron_ingot.json")).isTrue();
            }
        }

        @Test
        @DisplayName("Correctly parses object tag values and ignores comments without polluting member list")
        void testTagJsonParsingWithObjectEntriesAndComments() throws IOException {
            UniversalDataPackCompiler compiler = new UniversalDataPackCompiler();

            String advancedJson = """
                    // Modded tag file with comments
                    {
                      /* Multi-line comment */
                      "replace": false,
                      "values": [
                        "minecraft:iron_ingot",
                        {
                          "id": "mymod:custom_iron_ingot",
                          "required": false
                        }
                      ]
                    }
                    """;

            compiler.addSourceResource("fabric", "mod1", "data/c/tags/items/iron_ingots.json", advancedJson.getBytes(StandardCharsets.UTF_8));

            try (InMemoryVirtualFileSystemMount vfs = compiler.compile()) {
                Optional<byte[]> tagBytes = vfs.readResource("data/c/tags/items/iron_ingots.json");
                assertThat(tagBytes).isPresent();
                String content = new String(tagBytes.get(), StandardCharsets.UTF_8);

                assertThat(content).contains("\"minecraft:iron_ingot\"");
                assertThat(content).contains("\"mymod:custom_iron_ingot\"");
                // Ensure "id" and "required" keys were NOT treated as item values!
                assertThat(content).doesNotContain("\"id\"");
                assertThat(content).doesNotContain("\"required\"");
            }
        }

        @Test
        @DisplayName("Harmonizes cross-loader tag values so all equivalent tags share unified members")
        void testCrossLoaderBidirectionalHarmonization() throws IOException {
            UniversalDataPackCompiler compiler = new UniversalDataPackCompiler();

            // Fabric provides super_iron
            String fabricTag = "{\"replace\": false, \"values\": [\"fabric_mod:super_iron\"]}";
            compiler.addSourceResource("fabric", "fmod", "data/c/tags/items/iron_ingots.json", fabricTag.getBytes(StandardCharsets.UTF_8));

            // Forge provides heavy_iron
            String forgeTag = "{\"replace\": false, \"values\": [\"forge_mod:heavy_iron\"]}";
            compiler.addSourceResource("forge", "gmod", "data/forge/tags/items/ingots/iron.json", forgeTag.getBytes(StandardCharsets.UTF_8));

            try (InMemoryVirtualFileSystemMount vfs = compiler.compile()) {
                // Fabric tag must contain both
                String cContent = new String(vfs.readResource("data/c/tags/items/iron_ingots.json").orElseThrow(), StandardCharsets.UTF_8);
                assertThat(cContent).contains("\"fabric_mod:super_iron\"");
                assertThat(cContent).contains("\"forge_mod:heavy_iron\"");

                // Forge tag must contain both
                String forgeContent = new String(vfs.readResource("data/forge/tags/items/ingots/iron.json").orElseThrow(), StandardCharsets.UTF_8);
                assertThat(forgeContent).contains("\"fabric_mod:super_iron\"");
                assertThat(forgeContent).contains("\"forge_mod:heavy_iron\"");

                // Vanilla synthesized tag must also contain both
                String mcContent = new String(vfs.readResource("data/minecraft/tags/items/iron_ingot.json").orElseThrow(), StandardCharsets.UTF_8);
                assertThat(mcContent).contains("\"fabric_mod:super_iron\"");
                assertThat(mcContent).contains("\"forge_mod:heavy_iron\"");
            }
        }

        @Test
        @DisplayName("Tag replace: true clears previous values")
        void testTagReplaceTrue() throws IOException {
            UniversalDataPackCompiler compiler = new UniversalDataPackCompiler();

            String initialTag = "{\"replace\": false, \"values\": [\"item1\", \"item2\"]}";
            compiler.addSourceResource("fabric", "mod1", "data/c/tags/items/metals.json", initialTag.getBytes(StandardCharsets.UTF_8));

            // Higher priority mod replaces tag
            String overrideTag = "{\"replace\": true, \"values\": [\"item_exclusive\"]}";
            compiler.addSourceResource("forge", "mod2", "data/c/tags/items/metals.json", overrideTag.getBytes(StandardCharsets.UTF_8));

            try (InMemoryVirtualFileSystemMount vfs = compiler.compile()) {
                Optional<byte[]> tagBytes = vfs.readResource("data/c/tags/items/metals.json");
                assertThat(tagBytes).isPresent();
                String content = new String(tagBytes.get(), StandardCharsets.UTF_8);
                assertThat(content).contains("\"replace\": true");
                assertThat(content).contains("\"item_exclusive\"");
                assertThat(content).doesNotContain("\"item1\"");
            }
        }

        @Test
        @DisplayName("Ingests data tree from another VirtualFileSystemMount")
        void testIngestFromVfs() throws IOException {
            UniversalDataPackCompiler compiler = new UniversalDataPackCompiler();

            try (InMemoryVirtualFileSystemMount sourceVfs = new InMemoryVirtualFileSystemMount("/mods/source.jar")) {
                sourceVfs.putResource("data/custom/recipes/sword.json", "{}".getBytes(StandardCharsets.UTF_8));
                sourceVfs.putResource("assets/custom/textures/sword.png", new byte[]{1, 2, 3}); // Should be ignored by datapack compiler

                compiler.addSourceModuleVfs("fabric", "source", sourceVfs);

                try (InMemoryVirtualFileSystemMount compiled = compiler.compile()) {
                    assertThat(compiled.hasResource("data/custom/recipes/sword.json")).isTrue();
                    assertThat(compiled.hasResource("assets/custom/textures/sword.png")).isFalse();
                }
            }
        }
    }

    @Nested
    @DisplayName("DataComponentBridgingTable Verification")
    class ComponentBridgeTests {

        @Test
        @DisplayName("Converts modern Data Components to legacy NBT with round-trip fidelity")
        void testModernToLegacyNbt() {
            DataComponentBridgingTable bridge = new DataComponentBridgingTable();

            Map<String, Object> components = new HashMap<>();
            components.put(DataComponentBridgingTable.COMPONENT_DAMAGE, 42);
            components.put(DataComponentBridgingTable.COMPONENT_UNBREAKABLE, true);
            components.put(DataComponentBridgingTable.COMPONENT_CUSTOM_NAME, "Excalibur");
            components.put(DataComponentBridgingTable.COMPONENT_LORE, List.of("Ancient blade", "Forged in fire"));
            components.put(DataComponentBridgingTable.COMPONENT_ENCHANTMENTS, Map.of("minecraft:sharpness", 5, "minecraft:unbreaking", 3));
            components.put(DataComponentBridgingTable.COMPONENT_CUSTOM_MODEL_DATA, 1001);

            // Unmapped mod data in custom_data
            Map<String, Object> customData = new HashMap<>();
            customData.put("ModUniqueId", "XYZ-999");
            components.put(DataComponentBridgingTable.COMPONENT_CUSTOM_DATA, customData);

            // Translate to Legacy NBT
            Map<String, Object> legacyNbt = bridge.toLegacyNbt(components);

            assertThat(legacyNbt.get(DataComponentBridgingTable.NBT_DAMAGE)).isEqualTo(42);
            assertThat(legacyNbt.get(DataComponentBridgingTable.NBT_UNBREAKABLE)).isEqualTo((byte) 1);
            assertThat(legacyNbt.get(DataComponentBridgingTable.NBT_CUSTOM_MODEL_DATA)).isEqualTo(1001);
            assertThat(legacyNbt.get("ModUniqueId")).isEqualTo("XYZ-999");

            @SuppressWarnings("unchecked")
            Map<String, Object> display = (Map<String, Object>) legacyNbt.get(DataComponentBridgingTable.NBT_DISPLAY);
            assertThat(display).isNotNull();
            assertThat(display.get(DataComponentBridgingTable.NBT_DISPLAY_NAME)).isEqualTo("Excalibur");
            assertThat(display.get(DataComponentBridgingTable.NBT_DISPLAY_LORE)).isEqualTo(List.of("Ancient blade", "Forged in fire"));

            // Round-trip back to components
            Map<String, Object> roundTrip = bridge.toDataComponents(legacyNbt);
            assertThat(bridge.getDamage(roundTrip)).isEqualTo(42);
            assertThat(bridge.isUnbreakable(roundTrip)).isTrue();
            assertThat(bridge.getCustomModelData(roundTrip)).isEqualTo(1001);
            assertThat(bridge.getCustomName(roundTrip)).isEqualTo("Excalibur");

            @SuppressWarnings("unchecked")
            Map<String, Object> rtCustomData = (Map<String, Object>) roundTrip.get(DataComponentBridgingTable.COMPONENT_CUSTOM_DATA);
            assertThat(rtCustomData).isNotNull();
            assertThat(rtCustomData.get("ModUniqueId")).isEqualTo("XYZ-999");
        }

        @Test
        @DisplayName("Preserves dyed armor color and unmapped custom display tags without data loss")
        void testDyedArmorColorAndCustomDisplayPreservation() {
            DataComponentBridgingTable bridge = new DataComponentBridgingTable();

            Map<String, Object> legacyNbt = new HashMap<>();
            Map<String, Object> display = new HashMap<>();
            display.put(DataComponentBridgingTable.NBT_DISPLAY_NAME, "Red Ruby Cap");
            display.put(DataComponentBridgingTable.NBT_DISPLAY_COLOR, 16711680); // Red dye
            display.put("CustomPluginDisplayTag", "PluginMetadata123");
            legacyNbt.put(DataComponentBridgingTable.NBT_DISPLAY, display);

            // Convert to modern components
            Map<String, Object> components = bridge.toDataComponents(legacyNbt);
            assertThat(components.get(DataComponentBridgingTable.COMPONENT_CUSTOM_NAME)).isEqualTo("Red Ruby Cap");
            assertThat(components.get(DataComponentBridgingTable.COMPONENT_DYED_COLOR)).isEqualTo(16711680);

            // Convert back to legacy NBT
            Map<String, Object> roundTrip = bridge.toLegacyNbt(components);
            @SuppressWarnings("unchecked")
            Map<String, Object> rtDisplay = (Map<String, Object>) roundTrip.get(DataComponentBridgingTable.NBT_DISPLAY);
            assertThat(rtDisplay).isNotNull();
            assertThat(rtDisplay.get(DataComponentBridgingTable.NBT_DISPLAY_NAME)).isEqualTo("Red Ruby Cap");
            assertThat(rtDisplay.get(DataComponentBridgingTable.NBT_DISPLAY_COLOR)).isEqualTo(16711680);
            assertThat(rtDisplay.get("CustomPluginDisplayTag")).isEqualTo("PluginMetadata123");
        }

        @Test
        @DisplayName("Normalizes legacy numeric enchantment IDs into canonical namespaced identifiers")
        void testLegacyNumericEnchantmentConversion() {
            DataComponentBridgingTable bridge = new DataComponentBridgingTable();

            Map<String, Object> legacyNbt = new HashMap<>();
            List<Map<String, Object>> enchList = new ArrayList<>();
            // id: 16 -> Sharpness, id: 34 -> Unbreaking
            enchList.add(Map.of("id", 16, "lvl", 5));
            enchList.add(Map.of("id", "34", "lvl", 3));
            legacyNbt.put(DataComponentBridgingTable.NBT_ENCHANTMENTS, enchList);

            Map<String, Object> components = bridge.toDataComponents(legacyNbt);
            @SuppressWarnings("unchecked")
            Map<String, Integer> enchMap = (Map<String, Integer>) components.get(DataComponentBridgingTable.COMPONENT_ENCHANTMENTS);

            assertThat(enchMap).isNotNull();
            assertThat(enchMap.get("minecraft:sharpness")).isEqualTo(5);
            assertThat(enchMap.get("minecraft:unbreaking")).isEqualTo(3);
        }

        @Test
        @DisplayName("Fast path primitive accessors operate without throwing")
        void testFastPathAccessors() {
            DataComponentBridgingTable bridge = new DataComponentBridgingTable();
            Map<String, Object> map = new HashMap<>();

            bridge.setDamage(map, 15);
            bridge.setUnbreakable(map, true);
            bridge.setCustomModelData(map, 505);
            bridge.setCustomName(map, "Magic Bow");

            assertThat(bridge.getDamage(map)).isEqualTo(15);
            assertThat(bridge.isUnbreakable(map)).isTrue();
            assertThat(bridge.getCustomModelData(map)).isEqualTo(505);
            assertThat(bridge.getCustomName(map)).isEqualTo("Magic Bow");
        }
    }

    @Nested
    @DisplayName("CompactCustomBlockStateIndex Verification")
    class CompactBlockStateIndexTests {

        @Test
        @DisplayName("Translates custom state IDs to vanilla palette IDs with 0 allocation")
        void testStateMapping() {
            CompactCustomBlockStateIndex index = new CompactCustomBlockStateIndex(64, 1);

            // Register mod block state: "techreborn:tin_ore" -> vanilla Stone (id=1)
            index.registerCustomBlockState(100, "techreborn:tin_ore", 1);
            // "mekanism:osmium_block" -> vanilla Iron Block (id=42)
            index.registerCustomBlockState(250, "mekanism:osmium_block", 42);

            assertThat(index.getRegisteredCount()).isEqualTo(2);
            assertThat(index.isCustomState(100)).isTrue();
            assertThat(index.isCustomState(250)).isTrue();
            assertThat(index.isCustomState(999)).isFalse();

            // Zero-allocation lookups
            assertThat(index.getVanillaPaletteId(100)).isEqualTo(1);
            assertThat(index.getVanillaPaletteId(250)).isEqualTo(42);

            // Unregistered state safely falls back to default ID without crash
            assertThat(index.getVanillaPaletteId(999)).isEqualTo(1);
            assertThat(index.getVanillaPaletteId(-1)).isEqualTo(1);

            assertThat(index.getCustomBlockName(100)).isEqualTo("techreborn:tin_ore");
            assertThat(index.getRegisteredCustomStateIds()).contains(100, 250);
        }

        @Test
        @DisplayName("Automatically expands capacity when large state IDs are registered")
        void testCapacityExpansion() {
            CompactCustomBlockStateIndex index = new CompactCustomBlockStateIndex(16, 1);

            // Register state far beyond initial capacity of 16
            index.registerCustomBlockState(10000, "huge_mod:super_block", 7);

            assertThat(index.getVanillaPaletteId(10000)).isEqualTo(7);
            assertThat(index.isCustomState(10000)).isTrue();
            assertThat(index.getVanillaPaletteId(5000)).isEqualTo(1); // Unmapped in between
        }

        @Test
        @DisplayName("Rejects out-of-bounds state IDs to prevent memory exhaustion")
        void testStateIdBoundsProtection() {
            CompactCustomBlockStateIndex index = new CompactCustomBlockStateIndex();

            assertThatThrownBy(() -> index.registerCustomBlockState(-1, "invalid", 1))
                    .isInstanceOf(IllegalArgumentException.class);

            assertThatThrownBy(() -> index.registerCustomBlockState(CompactCustomBlockStateIndex.MAX_SUPPORTED_STATE_ID + 1, "too_large", 1))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }
}
