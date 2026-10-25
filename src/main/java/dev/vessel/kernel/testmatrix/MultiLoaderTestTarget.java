package dev.vessel.kernel.testmatrix;

import dev.vessel.kernel.identity.SourceLoaderKind;
import java.util.Objects;
import java.util.Set;

/**
 * Immutable descriptor representing an isolated mod or plugin module targeted for concurrent multi-loader execution.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @param modId unique mod or plugin identifier
 * @param loaderKind originating upstream loader ecosystem
 * @param version declared semantic version string
 * @param exportedPackages set of exported Java package namespace roots
 * @param entrypointClasses set of bootstrap and lifecycle entrypoint classes
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public record MultiLoaderTestTarget(
        String modId,
        SourceLoaderKind loaderKind,
        String version,
        Set<String> exportedPackages,
        Set<String> entrypointClasses
) {
    public MultiLoaderTestTarget {
        Objects.requireNonNull(modId, "modId must not be null");
        Objects.requireNonNull(loaderKind, "loaderKind must not be null");
        Objects.requireNonNull(version, "version must not be null");
        exportedPackages = Set.copyOf(Objects.requireNonNull(exportedPackages, "exportedPackages must not be null"));
        entrypointClasses = Set.copyOf(Objects.requireNonNull(entrypointClasses, "entrypointClasses must not be null"));
    }
}
