package dev.vessel.kernel.asset.stream;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Immutable compilation product containing generated file maps for legacy, modern,
 * and universal dual-generation resource pack architectures.
 * Extinguishes AST-MOD.
 *
 * @param legacyFiles file mappings for Minecraft 1.20.1 - 1.20.4
 * @param modernFiles file mappings for Minecraft 1.20.5+
 * @param combinedFiles unified file map containing both targets and shared assets
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public record DualGenerationPackArtifact(
        Map<String, String> legacyFiles,
        Map<String, String> modernFiles,
        Map<String, String> combinedFiles
) {

    public DualGenerationPackArtifact {
        legacyFiles = Collections.unmodifiableMap(new HashMap<>(Objects.requireNonNull(legacyFiles, "legacyFiles cannot be null")));
        modernFiles = Collections.unmodifiableMap(new HashMap<>(Objects.requireNonNull(modernFiles, "modernFiles cannot be null")));
        combinedFiles = Collections.unmodifiableMap(new HashMap<>(Objects.requireNonNull(combinedFiles, "combinedFiles cannot be null")));
    }

    /**
     * Gets the total count of distinct files in the combined pack.
     *
     * @return file count
     */
    public int totalCombinedFiles() {
        return combinedFiles.size();
    }
}
