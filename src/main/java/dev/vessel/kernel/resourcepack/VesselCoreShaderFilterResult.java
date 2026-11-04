package dev.vessel.kernel.resourcepack;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Result descriptor produced by {@link VesselCoreShaderSafetyFilter}.
 *
 * @param safeEntries map of approved, safe resource pack entries
 * @param removedShaderPaths list of unsafe core shader file paths filtered out
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public record VesselCoreShaderFilterResult(
        Map<String, byte[]> safeEntries,
        List<String> removedShaderPaths
) {

    public VesselCoreShaderFilterResult {
        Objects.requireNonNull(safeEntries, "safeEntries cannot be null");
        Objects.requireNonNull(removedShaderPaths, "removedShaderPaths cannot be null");
        safeEntries = Map.copyOf(safeEntries);
        removedShaderPaths = List.copyOf(removedShaderPaths);
    }

    /**
     * Checks if any unsafe core shaders were detected and removed.
     *
     * @return true if one or more shaders were filtered
     */
    public boolean hasFilteredShaders() {
        return !removedShaderPaths.isEmpty();
    }

    /**
     * Number of unsafe shaders removed.
     *
     * @return removed count
     */
    public int filteredCount() {
        return removedShaderPaths.size();
    }
}
