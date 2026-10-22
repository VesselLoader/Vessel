package dev.vessel.kernel.identity;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Immutable canonical 4-tuple scoped module identifier.
 *
 * <p>Constitutes the fundamental identity pillar of the VESSEL microkernel:
 * <pre>
 * ModuleIdentity = &lang;SourceLoader, Namespace, SemVer, Classifier&rang;
 * </pre>
 *
 * <p>Prevents cross-ecosystem name collisions (Decision 5, 7, 8). Two modules sharing the same
 * namespace but originating from different loaders remain completely isolated in distinct
 * namespace scopes without interference.</p>
 *
 * @param loader     the ecosystem origin loader kind
 * @param namespace  the normalized module identifier (e.g., "worldedit", "jei")
 * @param version    the declared semantic version string
 * @param classifier the structural classification role (default: {@link ModuleClassifier#MAIN})
 * @version 1.0.1
 * @author Shiroz &amp; Antigravity
 * @since 1.0.0 (Pass K-3)
 */
public record VesselModuleIdentifier(
        SourceLoaderKind loader,
        String namespace,
        String version,
        ModuleClassifier classifier
) implements Comparable<VesselModuleIdentifier>, java.io.Serializable {

    private static final long serialVersionUID = 1L;
    private static final Pattern NAMESPACE_PATTERN = Pattern.compile("^[a-z0-9_.-]+$");

    /**
     * Compact constructor enforcing strict normalization and invariant validation.
     */
    public VesselModuleIdentifier {
        Objects.requireNonNull(loader, "loader must not be null");
        Objects.requireNonNull(namespace, "namespace must not be null");
        Objects.requireNonNull(version, "version must not be null");
        Objects.requireNonNull(classifier, "classifier must not be null");

        namespace = namespace.trim().toLowerCase();
        if (namespace.isEmpty()) {
            throw new IllegalArgumentException("namespace cannot be empty");
        }
        if (!NAMESPACE_PATTERN.matcher(namespace).matches()) {
            throw new IllegalArgumentException("namespace contains invalid characters: " + namespace +
                    " (must match " + NAMESPACE_PATTERN.pattern() + ")");
        }

        version = version.trim();
        if (version.isEmpty()) {
            throw new IllegalArgumentException("version cannot be empty");
        }
    }

    /**
     * Convenience factory for creating a primary MAIN classified module identifier.
     *
     * @param loader    the loader ecosystem
     * @param namespace the module namespace
     * @param version   the version string
     * @return a new VesselModuleIdentifier instance
     */
    public static VesselModuleIdentifier of(SourceLoaderKind loader, String namespace, String version) {
        return new VesselModuleIdentifier(loader, namespace, version, ModuleClassifier.MAIN);
    }

    /**
     * Formats the identifier into a canonical serialized string representation.
     * Example: {@code fabric:sodium@0.5.8} or {@code forge:jei@15.2.0.27#api}.
     *
     * @return the formatted identity string
     */
    public String toCanonicalString() {
        if (classifier == ModuleClassifier.MAIN) {
            return loader.getId() + ":" + namespace + "@" + version;
        }
        return loader.getId() + ":" + namespace + "@" + version + "#" + classifier.getTag();
    }

    @Override
    public String toString() {
        return toCanonicalString();
    }

    @Override
    public int compareTo(VesselModuleIdentifier o) {
        int loaderCompare = this.loader.compareTo(o.loader);
        if (loaderCompare != 0) return loaderCompare;

        int nsCompare = this.namespace.compareTo(o.namespace);
        if (nsCompare != 0) return nsCompare;

        int verCompare = this.version.compareTo(o.version);
        if (verCompare != 0) return verCompare;

        return this.classifier.compareTo(o.classifier);
    }
}
