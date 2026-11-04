package dev.vessel.kernel.test;

import dev.vessel.kernel.resourcepack.bedrock.BedrockManifestDependencyCleaner;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verification test suite for {@link BedrockManifestDependencyCleaner}.
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public class BedrockManifestDependencyCleanerTest {

    @Test
    @DisplayName("cleanDependencies strips dependencies array from manifest JSON")
    void testCleanDependencies() {
        BedrockManifestDependencyCleaner cleaner = new BedrockManifestDependencyCleaner();

        String rawManifest = """
        {
          "format_version": 2,
          "header": {
            "name": "CustomPack",
            "uuid": "11111111-2222-3333-4444-555555555555",
            "version": [1, 0, 0]
          },
          "modules": [
            {
              "type": "resources",
              "uuid": "66666666-7777-8888-9999-000000000000",
              "version": [1, 0, 0]
            }
          ],
          "dependencies": [
            {
              "uuid": "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee",
              "version": [1, 0, 0]
            }
          ]
        }
        """;

        String cleaned = cleaner.cleanDependencies(rawManifest);

        assertThat(cleaned).doesNotContain("\"dependencies\"");
        assertThat(cleaned).contains("\"format_version\": 2");
        assertThat(cleaned).contains("\"CustomPack\"");
        assertThat(cleaned).contains("\"type\": \"resources\"");
    }

    @Test
    @DisplayName("cleanDependencies handles manifests without dependencies gracefully")
    void testCleanDependenciesUnchanged() {
        BedrockManifestDependencyCleaner cleaner = new BedrockManifestDependencyCleaner();

        String cleanManifest = """
        {
          "format_version": 2,
          "header": {
            "name": "CleanPack"
          }
        }
        """;

        String result = cleaner.cleanDependencies(cleanManifest);
        assertThat(result).isEqualTo(cleanManifest);
    }
}
