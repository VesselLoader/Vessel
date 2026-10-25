package dev.vessel.kernel.adapter.forge;

import java.util.Objects;

/**
 * Fatal exception enforcing hard stop termination when duplicate module namespace collisions occur.
 * <p>
 * Enforces Architectural Decision 8 and Decision 17 (Zero Tolerance for Ambiguous Module Namespaces).
 * </p>
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public class DuplicateModuleHardStopException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final String namespaceId;
    private final String loaderType;

    /**
     * Constructs the hard stop exception with namespace ID, loader type, and collision summary.
     *
     * @param namespaceId the colliding mod namespace identifier
     * @param loaderType  the target loader environment (Forge, NeoForge, Fabric, etc.)
     * @param message     detailed diagnostic message
     */
    public DuplicateModuleHardStopException(String namespaceId, String loaderType, String message) {
        super(String.format("[DECISION-8-HARD-STOP] Namespace '%s' collided on loader '%s': %s",
                namespaceId, loaderType, message));
        this.namespaceId = Objects.requireNonNull(namespaceId, "namespaceId must not be null");
        this.loaderType = Objects.requireNonNull(loaderType, "loaderType must not be null");
    }

    /**
     * Returns the conflicting namespace ID.
     *
     * @return namespace ID
     */
    public String getNamespaceId() {
        return namespaceId;
    }

    /**
     * Returns the loader environment where collision occurred.
     *
     * @return loader type
     */
    public String getLoaderType() {
        return loaderType;
    }
}
