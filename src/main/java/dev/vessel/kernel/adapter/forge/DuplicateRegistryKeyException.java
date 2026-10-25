package dev.vessel.kernel.adapter.forge;

import java.util.Objects;

/**
 * Exception thrown when two or more mods claim the identical registry entry key within the same registry.
 * <p>
 * Eliminates ERR-92 (Registry key collisions leading to silent entry overwrites or freeze failures).
 * </p>
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public class DuplicateRegistryKeyException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public static final String ERROR_CODE = "ERR-92";

    private final String registryIdentifier;
    private final String namespacedKey;
    private final String firstModId;
    private final String secondModId;

    /**
     * Constructs a registry collision exception.
     *
     * @param registryIdentifier namespaced registry
     * @param namespacedKey      colliding key
     * @param firstModId         first claiming mod
     * @param secondModId        second claiming mod
     * @param message            diagnostic message
     */
    public DuplicateRegistryKeyException(
            String registryIdentifier,
            String namespacedKey,
            String firstModId,
            String secondModId,
            String message
    ) {
        super(String.format("[%s] Collision in registry '%s' on key '%s' between '%s' and '%s': %s",
                ERROR_CODE, registryIdentifier, namespacedKey, firstModId, secondModId, message));
        this.registryIdentifier = Objects.requireNonNull(registryIdentifier, "registryIdentifier must not be null");
        this.namespacedKey = Objects.requireNonNull(namespacedKey, "namespacedKey must not be null");
        this.firstModId = Objects.requireNonNull(firstModId, "firstModId must not be null");
        this.secondModId = Objects.requireNonNull(secondModId, "secondModId must not be null");
    }

    /**
     * Returns the target registry.
     *
     * @return registry identifier
     */
    public String getRegistryIdentifier() {
        return registryIdentifier;
    }

    /**
     * Returns the colliding key.
     *
     * @return entry key
     */
    public String getNamespacedKey() {
        return namespacedKey;
    }

    /**
     * Returns the first mod ID.
     *
     * @return first mod ID
     */
    public String getFirstModId() {
        return firstModId;
    }

    /**
     * Returns the second mod ID.
     *
     * @return second mod ID
     */
    public String getSecondModId() {
        return secondModId;
    }
}
