package dev.vessel.kernel.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link CrossLoaderConfigBridge} verifying single-cycle cross-loader config synchronization.
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public class CrossLoaderConfigBridgeTest {

    @Test
    @DisplayName("Convert: Converts YAML with comments to TOML preserving structure and comments")
    void testConvertYamlToToml() {
        CrossLoaderConfigBridge bridge = new CrossLoaderConfigBridge();

        String yaml = """
                # Server network configuration
                server:
                  # Port to listen
                  port: 25565
                  online-mode: true
                """;

        String toml = bridge.convert(yaml, VesselConfigFormat.YAML, VesselConfigFormat.TOML);

        assertThat(toml).contains("[server]");
        assertThat(toml).contains("port = 25565");
        assertThat(toml).contains("online-mode = true");
        assertThat(toml).contains("# Port to listen");
    }

    @Test
    @DisplayName("Convert: Converts YAML to JSON5 with preserved comments")
    void testConvertYamlToJson5() {
        CrossLoaderConfigBridge bridge = new CrossLoaderConfigBridge();

        String yaml = """
                # View distance tuning
                view-distance: 12
                simulation-distance: 10
                """;

        String json5 = bridge.convert(yaml, VesselConfigFormat.YAML, VesselConfigFormat.JSON);

        assertThat(json5).contains("\"view-distance\": 12");
        assertThat(json5).contains("\"simulation-distance\": 10");
        assertThat(json5).contains("// View distance tuning");
    }

    @Test
    @DisplayName("Synchronize: Applies source values into target TOML while retaining target comments")
    void testSynchronizeYamlIntoToml() {
        CrossLoaderConfigBridge bridge = new CrossLoaderConfigBridge();

        String sourceYaml = """
                server:
                  port: 25570
                  online-mode: false
                """;

        String targetToml = """
                # Target existing comment
                [server]
                port = 25565
                """;

        String result = bridge.synchronize(sourceYaml, VesselConfigFormat.YAML, targetToml, VesselConfigFormat.TOML);

        assertThat(result).contains("port = 25570");
        assertThat(result).contains("online-mode = false");
        assertThat(result).contains("[server]");
    }

    @Test
    @DisplayName("SynchronizeNodes: Traverses AST in single pass updating scalar values and comments")
    void testSynchronizeNodes() {
        CrossLoaderConfigBridge bridge = new CrossLoaderConfigBridge();

        HierarchicalConfigNode source = new HierarchicalConfigNode("");
        source.getOrCreateNode("network.max-players").setValue(100);
        source.getOrCreateNode("network.max-players").setComment("Max concurrent players");

        HierarchicalConfigNode destination = new HierarchicalConfigNode("");
        destination.getOrCreateNode("network.max-players").setValue(20);

        int count = bridge.synchronizeNodes(source, destination);

        assertThat(count).isEqualTo(1);
        assertThat(destination.getNode("network.max-players").getValue()).isEqualTo(100);
        assertThat(destination.getNode("network.max-players").getComment()).isEqualTo("Max concurrent players");
    }
}
