package dev.vessel.kernel.test;

import dev.vessel.kernel.identity.BukkitPluginYamlParser;
import dev.vessel.kernel.identity.FabricModJsonParser;
import dev.vessel.kernel.identity.ForgeModsTomlParser;
import dev.vessel.kernel.identity.IdentityCollisionDetector;
import dev.vessel.kernel.identity.InvalidMetadataException;
import dev.vessel.kernel.identity.ModuleCollisionException;
import dev.vessel.kernel.identity.ModuleIdentityParser;
import dev.vessel.kernel.identity.SourceLoaderKind;
import dev.vessel.kernel.identity.VesselModuleIdentifier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit verification suite for Module Identity, Descriptors Parsing, and Collision Detection.
 *
 * @version 1.0.0
 * @author Shiroz &amp; Antigravity
 * @since 1.0.0 (Pass K-4)
 */
public class IdentityCollisionTest {

    @Test
    @DisplayName("FabricModJsonParser extracts id and version from fabric.mod.json")
    void testFabricParser() {
        String json = """
                {
                  "schemaVersion": 1,
                  "id": "sodium",
                  "version": "0.5.8",
                  "name": "Sodium"
                }
                """;
        FabricModJsonParser parser = new FabricModJsonParser();
        VesselModuleIdentifier id = parser.parseContent(json, false);

        assertThat(id.loader()).isEqualTo(SourceLoaderKind.FABRIC);
        assertThat(id.namespace()).isEqualTo("sodium");
        assertThat(id.version()).isEqualTo("0.5.8");
    }

    @Test
    @DisplayName("FabricModJsonParser extracts id and version from quilt.mod.json")
    void testQuiltParser() {
        String json = """
                {
                  "schema_version": 1,
                  "quilt_loader": {
                    "id": "quilt_biome_api",
                    "version": "1.2.0"
                  }
                }
                """;
        FabricModJsonParser parser = new FabricModJsonParser();
        VesselModuleIdentifier id = parser.parseContent(json, true);

        assertThat(id.loader()).isEqualTo(SourceLoaderKind.QUILT);
        assertThat(id.namespace()).isEqualTo("quilt_biome_api");
        assertThat(id.version()).isEqualTo("1.2.0");
    }

    @Test
    @DisplayName("BukkitPluginYamlParser distinguishes Bukkit, Paper, and Folia plugins")
    void testBukkitParser() {
        BukkitPluginYamlParser parser = new BukkitPluginYamlParser();

        String spigotYaml = """
                name: WorldEdit
                version: 7.2.15
                main: com.sk89q.worldedit.bukkit.WorldEditPlugin
                """;
        VesselModuleIdentifier spigotId = parser.parseContent(spigotYaml, false);
        assertThat(spigotId.loader()).isEqualTo(SourceLoaderKind.BUKKIT);
        assertThat(spigotId.namespace()).isEqualTo("worldedit");
        assertThat(spigotId.version()).isEqualTo("7.2.15");

        String foliaYaml = """
                name: Chunky
                version: 1.4.10
                folia-supported: true
                """;
        VesselModuleIdentifier foliaId = parser.parseContent(foliaYaml, false);
        assertThat(foliaId.loader()).isEqualTo(SourceLoaderKind.FOLIA);
        assertThat(foliaId.namespace()).isEqualTo("chunky");

        String paperYaml = """
                name: PaperDiagnostics
                version: 2.0.0
                """;
        VesselModuleIdentifier paperId = parser.parseContent(paperYaml, true);
        assertThat(paperId.loader()).isEqualTo(SourceLoaderKind.PAPER);
    }

    @Test
    @DisplayName("ForgeModsTomlParser extracts modId and version from mods.toml and neoforge.mods.toml")
    void testForgeParser() {
        ForgeModsTomlParser parser = new ForgeModsTomlParser();

        String forgeToml = """
                modLoader="javafml"
                loaderVersion="[47,)"
                [[mods]]
                modId="jei"
                version="15.2.0.27"
                """;
        VesselModuleIdentifier forgeId = parser.parseContent(forgeToml, false);
        assertThat(forgeId.loader()).isEqualTo(SourceLoaderKind.FORGE);
        assertThat(forgeId.namespace()).isEqualTo("jei");
        assertThat(forgeId.version()).isEqualTo("15.2.0.27");

        String neoforgeToml = """
                modLoader="javafml"
                [[mods]]
                modId="create"
                version="0.5.1.f"
                """;
        VesselModuleIdentifier neoId = parser.parseContent(neoforgeToml, true);
        assertThat(neoId.loader()).isEqualTo(SourceLoaderKind.NEOFORGE);
        assertThat(neoId.namespace()).isEqualTo("create");
        assertThat(neoId.version()).isEqualTo("0.5.1.f");
    }

    @Test
    @DisplayName("ModuleIdentityParser dispatches correctly across various descriptor filenames")
    void testModuleIdentityDispatcher() {
        ModuleIdentityParser dispatcher = new ModuleIdentityParser();

        String rawFabric = "{\"schemaVersion\": 1, \"id\": \"iris\", \"version\": \"1.7.0\"}";
        ByteArrayInputStream in = new ByteArrayInputStream(rawFabric.getBytes(StandardCharsets.UTF_8));

        VesselModuleIdentifier id = dispatcher.parse("META-INF/fabric.mod.json", in);
        assertThat(id.loader()).isEqualTo(SourceLoaderKind.FABRIC);
        assertThat(id.namespace()).isEqualTo("iris");
        assertThat(id.version()).isEqualTo("1.7.0");
    }

    @Test
    @DisplayName("Decision 7: Same namespace across DIFFERENT loaders registered concurrently without collision")
    void testCrossLoaderIsolation() {
        IdentityCollisionDetector detector = new IdentityCollisionDetector();

        VesselModuleIdentifier bukkitWe = VesselModuleIdentifier.of(SourceLoaderKind.BUKKIT, "worldedit", "7.2.15");
        VesselModuleIdentifier fabricWe = VesselModuleIdentifier.of(SourceLoaderKind.FABRIC, "worldedit", "7.2.15");

        detector.register(bukkitWe, "/plugins/WorldEdit-Bukkit.jar");
        detector.register(fabricWe, "/mods/WorldEdit-Fabric.jar");

        assertThat(detector.size()).isEqualTo(2);
        assertThat(detector.get(SourceLoaderKind.BUKKIT, "worldedit")).contains(bukkitWe);
        assertThat(detector.get(SourceLoaderKind.FABRIC, "worldedit")).contains(fabricWe);
    }

    @Test
    @DisplayName("Decision 8: Duplicate namespace within SAME loader triggers Hard Stop ModuleCollisionException")
    void testSameLoaderCollisionHardStop() {
        IdentityCollisionDetector detector = new IdentityCollisionDetector();

        VesselModuleIdentifier we1 = VesselModuleIdentifier.of(SourceLoaderKind.FABRIC, "worldedit", "7.2.15");
        VesselModuleIdentifier we2 = VesselModuleIdentifier.of(SourceLoaderKind.FABRIC, "worldedit", "7.2.16");

        detector.register(we1, "/mods/worldedit-fabric-7.2.15.jar");

        assertThatThrownBy(() -> detector.register(we2, "/mods/worldedit-fabric-7.2.16.jar"))
                .isInstanceOf(ModuleCollisionException.class)
                .hasMessageContaining("Hard Stop")
                .hasMessageContaining("worldedit")
                .hasMessageContaining("worldedit-fabric-7.2.15.jar")
                .hasMessageContaining("worldedit-fabric-7.2.16.jar");
    }

    @Test
    @DisplayName("Missing required fields trigger InvalidMetadataException")
    void testMalformedMetadata() {
        FabricModJsonParser parser = new FabricModJsonParser();
        assertThatThrownBy(() -> parser.parseContent("{\"schemaVersion\": 1}", false))
                .isInstanceOf(InvalidMetadataException.class)
                .hasMessageContaining("Missing or empty 'id'");
    }
}
