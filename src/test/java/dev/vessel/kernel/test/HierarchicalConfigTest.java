package dev.vessel.kernel.test;

import dev.vessel.kernel.config.HierarchicalConfigNode;
import dev.vessel.kernel.config.LightweightConfigParser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit verification suite for HierarchicalConfigNode and LightweightConfigParser.
 *
 * @version 1.0.0
 */
public class HierarchicalConfigTest {

    @Test
    @DisplayName("LightweightConfigParser parses indented YAML text with comments into HierarchicalConfigNode")
    void testYamlParsing() {
        String yaml = """
                # Server core settings
                server:
                  # Network bind port
                  port: 25565
                  ip: "127.0.0.1"
                  online_mode: true
                # Database storage config
                database:
                  enabled: false
                  pool_size: 10
                """;

        HierarchicalConfigNode root = LightweightConfigParser.parse(yaml);

        assertThat(root.contains("server")).isTrue();
        assertThat(root.getInt("server.port", 0)).isEqualTo(25565);
        assertThat(root.getString("server.ip", "")).isEqualTo("127.0.0.1");
        assertThat(root.getBoolean("server.online_mode", false)).isTrue();

        assertThat(root.getBoolean("database.enabled", true)).isFalse();
        assertThat(root.getInt("database.pool_size", 0)).isEqualTo(10);

        // Check comment preservation
        HierarchicalConfigNode serverNode = root.getNode("server");
        assertThat(serverNode.getComment()).contains("Server core settings");

        HierarchicalConfigNode portNode = root.getNode("server.port");
        assertThat(portNode.getComment()).contains("Network bind port");
    }

    @Test
    @DisplayName("HierarchicalConfigNode supports dynamic mutations and serialization to YAML")
    void testMutationAndSerialization() {
        HierarchicalConfigNode root = new HierarchicalConfigNode("");

        root.set("network.compression_threshold", 256);
        root.setComment("network.compression_threshold", "Packet compression limit in bytes");

        root.set("network.raknet.enabled", true);
        root.set("network.raknet.port", 19132);

        assertThat(root.getInt("network.compression_threshold", 0)).isEqualTo(256);
        assertThat(root.getBoolean("network.raknet.enabled", false)).isTrue();
        assertThat(root.getInt("network.raknet.port", 0)).isEqualTo(19132);

        String serialized = LightweightConfigParser.toYamlString(root);
        assertThat(serialized).contains("# Packet compression limit in bytes");
        assertThat(serialized).contains("compression_threshold: 256");
        assertThat(serialized).contains("raknet:");
        assertThat(serialized).contains("port: 19132");

        // Re-parse serialized output
        HierarchicalConfigNode roundtrip = LightweightConfigParser.parse(serialized);
        assertThat(roundtrip.getInt("network.compression_threshold", 0)).isEqualTo(256);
        assertThat(roundtrip.getInt("network.raknet.port", 0)).isEqualTo(19132);
    }
}
