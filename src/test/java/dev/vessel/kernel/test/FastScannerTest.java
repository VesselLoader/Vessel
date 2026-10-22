package dev.vessel.kernel.test;

import dev.vessel.kernel.identity.FastStringScanner;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verification test suite for high-performance FastStringScanner.
 *
 * @version 1.0.0
 * @author Shiroz &amp; Antigravity
 * @since 1.0.0 (Pass K-5)
 */
public class FastScannerTest {

    @Test
    @DisplayName("FastStringScanner extracts JSON string fields without regex overhead")
    void testJsonScanning() {
        FastStringScanner scanner = new FastStringScanner();
        String json = """
                {
                  "schemaVersion": 1,
                  "id": "iris",
                  "version": "1.7.0",
                  "description": "Modern shaders mod"
                }
                """;

        assertThat(scanner.extractJsonStringField(json, "id")).isEqualTo("iris");
        assertThat(scanner.extractJsonStringField(json, "version")).isEqualTo("1.7.0");
        assertThat(scanner.extractJsonStringField(json, "non_existent")).isNull();
    }

    @Test
    @DisplayName("FastStringScanner extracts YAML properties and skips comments")
    void testYamlScanning() {
        FastStringScanner scanner = new FastStringScanner();
        String yaml = """
                # WorldGuard configuration
                name: WorldGuard # core name
                # author comments
                version: "7.0.9"
                main: com.sk89q.worldguard.bukkit.WorldGuardPlugin
                """;

        assertThat(scanner.extractYamlSimpleProperty(yaml, "name")).isEqualTo("WorldGuard");
        assertThat(scanner.extractYamlSimpleProperty(yaml, "version")).isEqualTo("7.0.9");
        assertThat(scanner.extractYamlSimpleProperty(yaml, "missing")).isNull();
    }

    @Test
    @DisplayName("FastStringScanner extracts TOML string properties")
    void testTomlScanning() {
        FastStringScanner scanner = new FastStringScanner();
        String toml = """
                modLoader="javafml"
                loaderVersion="[1,)"
                [[mods]]
                modId = "create"
                version = "0.5.1"
                displayName = "Create"
                """;

        assertThat(scanner.extractTomlStringProperty(toml, "modId")).isEqualTo("create");
        assertThat(scanner.extractTomlStringProperty(toml, "version")).isEqualTo("0.5.1");
        assertThat(scanner.extractTomlStringProperty(toml, "absent")).isNull();
    }
}
