package dev.vessel.kernel.resourcepack.bedrock;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Immutable audit result returned by {@link VesselBedrockManifestDependencyCleaner}.
 * <p>
 * Contains indicators of whether the manifest was altered, the count of removed dependencies,
 * the cleaned JSON text, and the list of specific removed dependency UUIDs or identifiers.
 * </p>
 * <p>
 * Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.
 * </p>
 *
 * @param modified true if one or more dependencies were detected and stripped
 * @param removedDependencyCount total number of removed dependency definitions
 * @param cleanedManifestJson the normalized/sanitized JSON manifest payload
 * @param removedIdentifiers unmodifiable list of UUIDs or descriptors stripped from the manifest
 * @version 1.0.1
 * @author VESSEL Engine Team
 */
public record VesselBedrockManifestCleaningResult(
        boolean modified,
        int removedDependencyCount,
        String cleanedManifestJson,
        List<String> removedIdentifiers
) {

    public VesselBedrockManifestCleaningResult {
        Objects.requireNonNull(cleanedManifestJson, "cleanedManifestJson cannot be null");
        removedIdentifiers = (removedIdentifiers != null)
                ? Collections.unmodifiableList(List.copyOf(removedIdentifiers))
                : Collections.emptyList();
    }

    /**
     * Factory constructor for unmodified manifests.
     *
     * @param manifestJson original manifest JSON
     * @return clean audit result with modified = false
     */
    public static VesselBedrockManifestCleaningResult unmodified(String manifestJson) {
        return new VesselBedrockManifestCleaningResult(false, 0, manifestJson, Collections.emptyList());
    }
}
