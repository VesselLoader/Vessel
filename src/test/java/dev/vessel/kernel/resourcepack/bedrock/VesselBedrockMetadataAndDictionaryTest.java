package dev.vessel.kernel.resourcepack.bedrock;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Comprehensive verification test suite for Bedrock Metadata, Translation & Model Dictionary Pipeline.
 * <p>
 * Tests {@link VesselBedrockTextureDictionaryCompiler}, {@link VesselBedrockLocaleMapper},
 * {@link VesselBedrockLanguageFileGenerator}, and {@link VesselBedrockManifestDependencyCleaner}.
 * </p>
 * <p>
 * Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.
 * </p>
 *
 * @version 1.0.2
 * @author VESSEL Engine Team
 */
public class VesselBedrockMetadataAndDictionaryTest {

    @Test
    @DisplayName("VesselBedrockTextureDictionaryCompiler: Namespace isolation prevents silent overwrites between mods")
    void testTextureDictionaryNamespaceIsolation() {
        VesselBedrockTextureDictionaryCompiler compiler = new VesselBedrockTextureDictionaryCompiler();

        // Mod A registers an item called 'wrench'
        compiler.registerItem("techmod", "wrench", "textures/techmod/items/wrench.png");

        // Mod B also registers an item called 'wrench'
        compiler.registerItem("magicmod", "wrench", "textures/magicmod/items/wrench.png");

        // Vanilla registers 'apple'
        compiler.registerItem("minecraft", "apple", "textures/items/apple.png");

        // Verify neither mod overwrote the other
        assertThat(compiler.hasItem("techmod:wrench")).isTrue();
        assertThat(compiler.hasItem("techmod_wrench")).isTrue();
        assertThat(compiler.hasItem("techmod.wrench")).isTrue();

        assertThat(compiler.hasItem("magicmod:wrench")).isTrue();
        assertThat(compiler.hasItem("magicmod_wrench")).isTrue();
        assertThat(compiler.hasItem("magicmod.wrench")).isTrue();

        assertThat(compiler.getItemEntry("techmod:wrench").bedrockPath())
                .isEqualTo("textures/techmod/items/wrench");
        assertThat(compiler.getItemEntry("magicmod:wrench").bedrockPath())
                .isEqualTo("textures/magicmod/items/wrench");

        // Vanilla should have unnamespaced key and namespaced alias
        assertThat(compiler.hasItem("apple")).isTrue();
        assertThat(compiler.hasItem("minecraft:apple")).isTrue();
        assertThat(compiler.getItemEntry("apple").bedrockPath())
                .isEqualTo("textures/items/apple");

        // Compile JSON and verify structure
        String itemJson = compiler.compileItemTextureJson("test_pack");
        JsonObject root = JsonParser.parseString(itemJson).getAsJsonObject();

        assertThat(root.get("resource_pack_name").getAsString()).isEqualTo("test_pack");
        assertThat(root.get("texture_name").getAsString()).isEqualTo("atlas.items");

        JsonObject data = root.getAsJsonObject("texture_data");
        assertThat(data.has("techmod:wrench")).isTrue();
        assertThat(data.getAsJsonObject("techmod:wrench").get("textures").getAsString())
                .isEqualTo("textures/techmod/items/wrench");
        assertThat(data.has("magicmod:wrench")).isTrue();
        assertThat(data.getAsJsonObject("magicmod:wrench").get("textures").getAsString())
                .isEqualTo("textures/magicmod/items/wrench");
        assertThat(data.has("apple")).isTrue();
    }

    @Test
    @DisplayName("VesselBedrockTextureDictionaryCompiler: Terrain dictionary compiles with mip levels and padding")
    void testTerrainDictionaryCompilation() {
        VesselBedrockTextureDictionaryCompiler compiler = new VesselBedrockTextureDictionaryCompiler();

        compiler.registerTerrain("techmod", "crusher", "\\textures\\techmod\\blocks\\crusher.png");
        compiler.registerTerrain("minecraft", "stone", "textures/blocks/stone");

        String terrainJson = compiler.compileTerrainTextureJson("vessel_core", 4, 8);
        JsonObject root = JsonParser.parseString(terrainJson).getAsJsonObject();

        assertThat(root.get("resource_pack_name").getAsString()).isEqualTo("vessel_core");
        assertThat(root.get("texture_name").getAsString()).isEqualTo("atlas.terrain");
        assertThat(root.get("num_mip_levels").getAsInt()).isEqualTo(4);
        assertThat(root.get("padding").getAsInt()).isEqualTo(8);

        JsonObject data = root.getAsJsonObject("texture_data");
        assertThat(data.has("techmod:crusher")).isTrue();
        assertThat(data.getAsJsonObject("techmod:crusher").get("textures").getAsString())
                .isEqualTo("textures/techmod/blocks/crusher");
        assertThat(data.has("stone")).isTrue();
        assertThat(data.getAsJsonObject("stone").get("textures").getAsString())
                .isEqualTo("textures/blocks/stone");

        // compileAll produces both dictionaries
        Map<String, String> all = compiler.compileAll();
        assertThat(all).containsKey("textures/item_texture.json");
        assertThat(all).containsKey("textures/terrain_texture.json");
    }

    @Test
    @DisplayName("VesselBedrockTextureDictionaryCompiler: Path normalization strips .png and normalizes slashes")
    void testTexturePathNormalization() {
        VesselBedrockTextureEntry entry = new VesselBedrockTextureEntry(
                "mod:gem",
                "mod",
                "gem",
                "//textures\\items\\nested\\gem.png",
                VesselBedrockTextureType.ITEM
        );
        assertThat(entry.bedrockPath()).isEqualTo("textures/items/nested/gem");
    }

    @Test
    @DisplayName("VesselBedrockTextureDictionaryCompiler: Thread-safe concurrent texture registration")
    void testConcurrentTextureRegistration() throws InterruptedException {
        VesselBedrockTextureDictionaryCompiler compiler = new VesselBedrockTextureDictionaryCompiler();
        int threads = 8;
        int entriesPerThread = 50;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch latch = new CountDownLatch(threads);
        AtomicInteger counter = new AtomicInteger();

        for (int t = 0; t < threads; t++) {
            final int threadId = t;
            executor.submit(() -> {
                try {
                    for (int i = 0; i < entriesPerThread; i++) {
                        String mod = "mod_" + threadId;
                        String item = "item_" + i;
                        compiler.registerItem(mod, item, "textures/" + mod + "/items/" + item);
                        counter.incrementAndGet();
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        boolean finished = latch.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        assertThat(finished).isTrue();
        assertThat(counter.get()).isEqualTo(threads * entriesPerThread);
        // Each custom namespace item generates 3 alias keys (: _ .)
        assertThat(compiler.itemCount()).isEqualTo(threads * entriesPerThread * 3);
    }

    @Test
    @DisplayName("VesselBedrockLocaleMapper: Maps Java locale codes to standard Bedrock locale format")
    void testLocaleMapper() {
        assertThat(VesselBedrockLocaleMapper.toBedrockLocale("en_us")).isEqualTo("en_US");
        assertThat(VesselBedrockLocaleMapper.toBedrockLocale("zh_cn")).isEqualTo("zh_CN");
        assertThat(VesselBedrockLocaleMapper.toBedrockLocale("es_es.json")).isEqualTo("es_ES");
        assertThat(VesselBedrockLocaleMapper.toBedrockLocale("vi_vn")).isEqualTo("vi_VN");
        assertThat(VesselBedrockLocaleMapper.toBedrockLocale("ja_jp")).isEqualTo("ja_JP");
        assertThat(VesselBedrockLocaleMapper.toBedrockLocale("pt_br")).isEqualTo("pt_BR");

        // Path resolution
        assertThat(VesselBedrockLocaleMapper.toBedrockFilePath("en_us"))
                .isEqualTo("texts/en_US.lang");
        assertThat(VesselBedrockLocaleMapper.toBedrockFilePath("zh_cn.json"))
                .isEqualTo("texts/zh_CN.lang");
    }

    @Test
    @DisplayName("VesselBedrockLanguageFileGenerator: Rewrites block and item keys to eliminate %tile...% bugs")
    void testLanguageFileGeneratorKeyPrefixing() {
        VesselBedrockLanguageFileGenerator generator = new VesselBedrockLanguageFileGenerator();

        String javaJson = """
                {
                  "block.techmod.quantum_generator": "Quantum Generator",
                  "item.techmod.wrench": "Tech Wrench",
                  "entity.techmod.drone": "Mining Drone",
                  "block.minecraft.diamond_block": "Block of Diamond",
                  "item.minecraft.golden_apple": "Golden Apple",
                  "message.techmod.welcome": "Welcome to %s!"
                }
                """;

        String langContent = generator.generateLangFromContent("techmod", javaJson);

        // Blocks must have tile.<namespace>.<id>.name
        assertThat(langContent).contains("tile.techmod.quantum_generator.name=Quantum Generator");

        // Items must have item.<namespace>.<id>.name
        assertThat(langContent).contains("item.techmod.wrench.name=Tech Wrench");

        // Entities must have entity.<namespace>.<id>.name
        assertThat(langContent).contains("entity.techmod.drone.name=Mining Drone");

        // Vanilla block should have both tile.minecraft.<id>.name and tile.<id>.name
        assertThat(langContent).contains("tile.minecraft.diamond_block.name=Block of Diamond");
        assertThat(langContent).contains("tile.diamond_block.name=Block of Diamond");

        // Vanilla item should have both item.minecraft.<id>.name and item.<id>.name
        assertThat(langContent).contains("item.minecraft.golden_apple.name=Golden Apple");
        assertThat(langContent).contains("item.golden_apple.name=Golden Apple");

        // General messages retained
        assertThat(langContent).contains("message.techmod.welcome=Welcome to %s!");

        // Must start with auto-generated header
        assertThat(langContent).startsWith("## Auto-generated by VESSEL Bedrock Language Pipeline");
    }

    @Test
    @DisplayName("VesselBedrockLanguageFileGenerator: Handles multiline strings with literal \\n escaping")
    void testLanguageFileGeneratorMultilineEscaping() {
        VesselBedrockLanguageFileGenerator generator = new VesselBedrockLanguageFileGenerator();

        Map<String, String> translations = Map.of(
                "item.techmod.guide_book", "Line 1\nLine 2\r\nLine 3"
        );

        String langContent = generator.generateLangFromMap("techmod", translations);
        assertThat(langContent).contains("item.techmod.guide_book.name=Line 1\\nLine 2\\nLine 3");
    }

    @Test
    @DisplayName("VesselBedrockManifestDependencyCleaner: Strips dependencies from JSON string and byte array")
    void testManifestDependencyCleaner() {
        VesselBedrockManifestDependencyCleaner cleaner = new VesselBedrockManifestDependencyCleaner();

        String dirtyManifest = """
                {
                  "format_version": 2,
                  "header": {
                    "name": "VESSEL Mod Resources",
                    "description": "Auto-compiled resources",
                    "uuid": "e9b867c4-5412-4f81-a957-6e9a65f4bb2a",
                    "version": [1, 0, 0],
                    "min_engine_version": [1, 20, 0]
                  },
                  "modules": [
                    {
                      "type": "resources",
                      "uuid": "4c8f5f24-2c93-4e4b-9755-d41c888d3637",
                      "version": [1, 0, 0]
                    }
                  ],
                  "dependencies": [
                    {
                      "uuid": "c044815b-6101-447b-8ff3-e3c3b0dfb720",
                      "version": [1, 0, 0]
                    },
                    {
                      "uuid": "8f2a11b5-3129-45aa-b883-29a8d9df4821",
                      "version": [2, 0, 0]
                    }
                  ]
                }
                """;

        // Pre-cleaning assertions
        assertThat(cleaner.hasDependencies(dirtyManifest)).isTrue();
        assertThat(cleaner.countDependencies(dirtyManifest)).isEqualTo(2);

        // Perform cleaning with audit
        VesselBedrockManifestCleaningResult result = cleaner.cleanWithAudit(dirtyManifest);
        assertThat(result.modified()).isTrue();
        assertThat(result.removedDependencyCount()).isEqualTo(2);
        assertThat(result.removedIdentifiers())
                .containsExactly("c044815b-6101-447b-8ff3-e3c3b0dfb720", "8f2a11b5-3129-45aa-b883-29a8d9df4821");

        // Verify cleaned JSON has NO dependencies block
        JsonObject cleanedRoot = JsonParser.parseString(result.cleanedManifestJson()).getAsJsonObject();
        assertThat(cleanedRoot.has("dependencies")).isFalse();
        assertThat(cleanedRoot.has("header")).isTrue();
        assertThat(cleanedRoot.has("modules")).isTrue();
        assertThat(cleanedRoot.get("format_version").getAsInt()).isEqualTo(2);

        // Verify byte array cleaner
        byte[] cleanedBytes = cleaner.clean(dirtyManifest.getBytes(StandardCharsets.UTF_8));
        String fromBytes = new String(cleanedBytes, StandardCharsets.UTF_8);
        assertThat(cleaner.hasDependencies(fromBytes)).isFalse();
    }

    @Test
    @DisplayName("VesselBedrockManifestDependencyCleaner: Manifests without dependencies remain unmodified")
    void testManifestWithoutDependenciesUnmodified() {
        VesselBedrockManifestDependencyCleaner cleaner = new VesselBedrockManifestDependencyCleaner();

        String cleanManifest = """
                {
                  "format_version": 2,
                  "header": {
                    "name": "Clean Pack",
                    "uuid": "e9b867c4-5412-4f81-a957-6e9a65f4bb2a",
                    "version": [1, 0, 0]
                  },
                  "modules": [
                    {
                      "type": "resources",
                      "uuid": "4c8f5f24-2c93-4e4b-9755-d41c888d3637",
                      "version": [1, 0, 0]
                    }
                  ]
                }
                """;

        assertThat(cleaner.hasDependencies(cleanManifest)).isFalse();
        assertThat(cleaner.countDependencies(cleanManifest)).isEqualTo(0);

        VesselBedrockManifestCleaningResult result = cleaner.cleanWithAudit(cleanManifest);
        assertThat(result.modified()).isFalse();
        assertThat(result.removedDependencyCount()).isEqualTo(0);
        assertThat(result.removedIdentifiers()).isEmpty();
    }

    @Test
    @DisplayName("VesselBedrockManifestDependencyCleaner: Targeted virtual BP dependency stripping")
    void testTargetedVirtualDependencyStripping() {
        VesselBedrockManifestDependencyCleaner cleaner = new VesselBedrockManifestDependencyCleaner();

        String manifestWithMixedDeps = """
                {
                  "format_version": 2,
                  "header": { "name": "Mixed Pack" },
                  "dependencies": [
                    { "uuid": "virtual-bp-0001", "version": [1, 0, 0] },
                    { "uuid": "valid-subpack-0002", "version": [1, 0, 0] }
                  ]
                }
                """;

        VesselBedrockManifestCleaningResult result = cleaner.cleanTargetedDependencies(
                manifestWithMixedDeps,
                Set.of("virtual-bp-0001")
        );

        assertThat(result.modified()).isTrue();
        assertThat(result.removedDependencyCount()).isEqualTo(1);
        assertThat(result.removedIdentifiers()).containsExactly("virtual-bp-0001");

        JsonObject cleanedObj = JsonParser.parseString(result.cleanedManifestJson()).getAsJsonObject();
        assertThat(cleanedObj.has("dependencies")).isTrue();
        JsonArray remaining = cleanedObj.getAsJsonArray("dependencies");
        assertThat(remaining.size()).isEqualTo(1);
        assertThat(remaining.get(0).getAsJsonObject().get("uuid").getAsString())
                .isEqualTo("valid-subpack-0002");
    }

    @Test
    @DisplayName("VesselBedrockManifestDependencyCleaner: Throws on invalid input")
    void testManifestCleanerValidation() {
        VesselBedrockManifestDependencyCleaner cleaner = new VesselBedrockManifestDependencyCleaner();

        assertThatThrownBy(() -> cleaner.cleanWithAudit(null))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> cleaner.cleanWithAudit("   "))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> cleaner.cleanWithAudit("{ not valid json }"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("VesselBedrockLanguageFileGenerator: Prevents double .name.name suffix on blocks/items ending in .name")
    void testLanguageFileGeneratorDoubleNamePrevention() {
        VesselBedrockLanguageFileGenerator generator = new VesselBedrockLanguageFileGenerator();

        String javaJson = """
                {
                  "block.techmod.quantum_generator.name": "Quantum Generator",
                  "item.techmod.wrench.name": "Tech Wrench",
                  "entity.techmod.drone.name": "Mining Drone"
                }
                """;

        String langContent = generator.generateLangFromContent("techmod", javaJson);

        // Must NOT contain .name.name
        assertThat(langContent).doesNotContain(".name.name");
        assertThat(langContent).contains("tile.techmod.quantum_generator.name=Quantum Generator");
        assertThat(langContent).contains("item.techmod.wrench.name=Tech Wrench");
        assertThat(langContent).contains("entity.techmod.drone.name=Mining Drone");
    }

    @Test
    @DisplayName("VesselBedrockLanguageFileGenerator: Generates colon aliases and item fallbacks for custom blocks")
    void testLanguageFileGeneratorColonAliasesAndFallbacks() {
        VesselBedrockLanguageFileGenerator generator = new VesselBedrockLanguageFileGenerator();

        Map<String, String> translations = Map.of(
                "block.techmod.crusher", "Ore Crusher",
                "item.techmod.drill", "Mining Drill"
        );

        NavigableMap<String, VesselBedrockLanguageEntry> entries = generator.parseTranslations("techmod", translations);

        // Block should have dot and colon format, plus item form when held in inventory
        assertThat(entries).containsKey("tile.techmod.crusher.name");
        assertThat(entries).containsKey("tile.techmod:crusher.name");
        assertThat(entries).containsKey("item.techmod.crusher.name");
        assertThat(entries).containsKey("item.techmod:crusher.name");

        // Item should have dot and colon format
        assertThat(entries).containsKey("item.techmod.drill.name");
        assertThat(entries).containsKey("item.techmod:drill.name");

        // Custom mod MUST NOT pollute unnamespaced tile.crusher.name
        assertThat(entries).doesNotContainKey("tile.crusher.name");
    }

    @Test
    @DisplayName("VesselBedrockTextureDictionaryCompiler: Strips redundant namespace prefixes and handles unnamespaced registration")
    void testTextureDictionaryRedundantPrefixHandling() {
        VesselBedrockTextureDictionaryCompiler compiler = new VesselBedrockTextureDictionaryCompiler();

        // Mod passes id that already contains 'techmod:' or 'techmod_'
        compiler.registerItem("techmod", "techmod:wrench", "textures/techmod/items/wrench");
        compiler.registerTerrain("techmod", "techmod_crusher", "textures/techmod/blocks/crusher");

        // Should NOT create 'techmod:techmod:wrench'
        assertThat(compiler.hasItem("techmod:wrench")).isTrue();
        assertThat(compiler.hasItem("techmod:techmod:wrench")).isFalse();
        assertThat(compiler.hasTerrain("techmod:crusher")).isTrue();
        assertThat(compiler.hasTerrain("techmod:techmod_crusher")).isFalse();

        // Unnamespaced registerItem("stick", ...) delegates to minecraft:stick
        compiler.registerItem("stick", "textures/items/stick");
        assertThat(compiler.hasItem("stick")).isTrue();
        assertThat(compiler.hasItem("minecraft:stick")).isTrue();
        assertThat(compiler.hasItem("minecraft_stick")).isTrue();
    }

    @Test
    @DisplayName("VesselBedrockTextureDictionaryCompiler: Normalizes Java assets path and strips various image extensions")
    void testTextureEntryJavaAssetsAndExtensions() {
        VesselBedrockTextureEntry tgaEntry = new VesselBedrockTextureEntry(
                "custom:icon", "custom", "icon", "items/icon.tga", VesselBedrockTextureType.ITEM
        );
        assertThat(tgaEntry.bedrockPath()).isEqualTo("textures/items/icon");

        VesselBedrockTextureEntry assetsEntry = new VesselBedrockTextureEntry(
                "mod:block", "mod", "block", "assets/mod/textures/block/furnace.png", VesselBedrockTextureType.TERRAIN
        );
        assertThat(assetsEntry.bedrockPath()).isEqualTo("textures/mod/blocks/furnace");
    }

    @Test
    @DisplayName("VesselBedrockManifestDependencyCleaner: Case-insensitive targeted UUID matching and BOM handling")
    void testManifestCleanerCaseInsensitivityAndBom() {
        VesselBedrockManifestDependencyCleaner cleaner = new VesselBedrockManifestDependencyCleaner();

        // Lowercase in JSON, uppercase in target set
        String manifest = """
                {
                  "format_version": 2,
                  "header": { "name": "BOM & UUID Test" },
                  "dependencies": [
                    { "uuid": "c044815b-6101-447b-8ff3-e3c3b0dfb720", "version": [1, 0, 0] },
                    { "uuid": "valid-keep-1234", "version": [1, 0, 0] }
                  ]
                }
                """;

        // Prepend UTF-8 BOM bytes
        byte[] manifestBytes = manifest.getBytes(StandardCharsets.UTF_8);
        byte[] withBom = new byte[manifestBytes.length + 3];
        withBom[0] = (byte) 0xEF;
        withBom[1] = (byte) 0xBB;
        withBom[2] = (byte) 0xBF;
        System.arraycopy(manifestBytes, 0, withBom, 3, manifestBytes.length);

        // Targeted clean passing uppercase UUID
        byte[] cleanedBytes = cleaner.cleanTargetedDependencies(withBom, Set.of("C044815B-6101-447B-8FF3-E3C3B0DFB720"));
        String cleanedString = new String(cleanedBytes, StandardCharsets.UTF_8);

        JsonObject root = JsonParser.parseString(cleanedString).getAsJsonObject();
        JsonArray remaining = root.getAsJsonArray("dependencies");
        assertThat(remaining.size()).isEqualTo(1);
        assertThat(remaining.get(0).getAsJsonObject().get("uuid").getAsString()).isEqualTo("valid-keep-1234");
    }

    @Test
    @DisplayName("VesselBedrockLocaleMapper: Maps single-language codes and regional variants")
    void testLocaleMapperExpandedMappings() {
        assertThat(VesselBedrockLocaleMapper.toBedrockLocale("en")).isEqualTo("en_US");
        assertThat(VesselBedrockLocaleMapper.toBedrockLocale("vi")).isEqualTo("vi_VN");
        assertThat(VesselBedrockLocaleMapper.toBedrockLocale("es-ar")).isEqualTo("es_MX");
        assertThat(VesselBedrockLocaleMapper.toBedrockLocale("no_no")).isEqualTo("nb_NO");
        assertThat(VesselBedrockLocaleMapper.toBedrockLocale("fil_ph")).isEqualTo("fil_PH");
        assertThat(VesselBedrockLocaleMapper.toBedrockFilePath("en")).isEqualTo("texts/en_US.lang");
    }

    @Test
    @DisplayName("VesselBedrockTextureDictionaryCompiler: Supports compact high-speed JSON for network packets")
    void testCompactJsonOutput() {
        VesselBedrockTextureDictionaryCompiler compiler = new VesselBedrockTextureDictionaryCompiler();
        compiler.registerItem("techmod", "wrench", "textures/techmod/items/wrench");

        String prettyJson = compiler.compileItemTextureJson("pack", true);
        String compactJson = compiler.compileItemTextureJson("pack", false);

        assertThat(prettyJson).contains("\n");
        assertThat(compactJson).doesNotContain("\n");

        JsonObject rootPretty = JsonParser.parseString(prettyJson).getAsJsonObject();
        JsonObject rootCompact = JsonParser.parseString(compactJson).getAsJsonObject();
        assertThat(rootPretty).isEqualTo(rootCompact);
    }
}
