package dev.vessel.kernel.adapter.forge;

import java.util.Objects;

/**
 * Immutable descriptor capturing module namespace provenance and loader affinity.
 *
 * @param namespaceId canonical mod identifier / namespace
 * @param loaderType  loader categorization (e.g. "FORGE", "NEOFORGE", "FABRIC")
 * @param jarOrigin   filesystem or archive path from which the module was discovered
 * @param version     semantic version declared in module metadata
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public record ModuleNamespaceDescriptor(
        String namespaceId,
        String loaderType,
        String jarOrigin,
        String version
) {

    /**
     * Compact constructor validating non-null identity fields.
     */
    public ModuleNamespaceDescriptor {
        Objects.requireNonNull(namespaceId, "namespaceId must not be null");
        Objects.requireNonNull(loaderType, "loaderType must not be null");
        Objects.requireNonNull(jarOrigin, "jarOrigin must not be null");
        Objects.requireNonNull(version, "version must not be null");
    }
}
