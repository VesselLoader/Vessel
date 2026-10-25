package dev.vessel.kernel.adapter.forge;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Immutable report capturing the outcome of pre-boot namespace collision scans.
 *
 * @param hasDuplicates         whether any duplicate namespace conflicts were detected
 * @param duplicateCount        total number of duplicate module instances
 * @param duplicatesByNamespace mapping of namespace identifier to colliding descriptors
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public record DuplicateScanReport(
        boolean hasDuplicates,
        int duplicateCount,
        Map<String, List<ModuleNamespaceDescriptor>> duplicatesByNamespace
) {

    /**
     * Compact constructor validating non-null collection map.
     */
    public DuplicateScanReport {
        Objects.requireNonNull(duplicatesByNamespace, "duplicatesByNamespace must not be null");
        duplicatesByNamespace = Collections.unmodifiableMap(duplicatesByNamespace);
    }
}
