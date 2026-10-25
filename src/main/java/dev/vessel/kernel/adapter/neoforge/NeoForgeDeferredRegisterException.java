package dev.vessel.kernel.adapter.neoforge;

import java.util.Objects;

/**
 * Exception thrown when NeoForge DeferredRegister operations encounter premature access or registry freeze violations.
 * <p>
 * Eliminates ERR-92 and suppresses ecosystem registration conflicts ECO-021 through ECO-030.
 * </p>
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public class NeoForgeDeferredRegisterException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final String errorCode;
    private final String registryName;
    private final String entryKey;

    /**
     * Constructs a DeferredRegister exception with errorCode, registryName, entryKey and message.
     *
     * @param errorCode    error identifier
     * @param registryName target registry identifier
     * @param entryKey     entry key involved in violation
     * @param message      explanatory message
     */
    public NeoForgeDeferredRegisterException(
            String errorCode,
            String registryName,
            String entryKey,
            String message
    ) {
        super(String.format("[%s] Registry '%s' entry '%s': %s", errorCode, registryName, entryKey, message));
        this.errorCode = Objects.requireNonNull(errorCode, "errorCode must not be null");
        this.registryName = Objects.requireNonNull(registryName, "registryName must not be null");
        this.entryKey = Objects.requireNonNull(entryKey, "entryKey must not be null");
    }

    /**
     * Returns the error code.
     *
     * @return error code
     */
    public String getErrorCode() {
        return errorCode;
    }

    /**
     * Returns the registry name.
     *
     * @return registry name
     */
    public String getRegistryName() {
        return registryName;
    }

    /**
     * Returns the entry key.
     *
     * @return entry key
     */
    public String getEntryKey() {
        return entryKey;
    }
}
