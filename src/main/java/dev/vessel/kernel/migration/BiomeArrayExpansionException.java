package dev.vessel.kernel.migration;

/**
 * Domain exception thrown when legacy Anvil biome array expansion encounters invalid or corrupt data.
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public final class BiomeArrayExpansionException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * Constructs a new exception with the specified detail message.
     *
     * @param message human-readable detail message
     */
    public BiomeArrayExpansionException(String message) {
        super(message);
    }

    /**
     * Constructs a new exception with the specified detail message and root cause.
     *
     * @param message human-readable detail message
     * @param cause   the root cause of the error
     */
    public BiomeArrayExpansionException(String message, Throwable cause) {
        super(message, cause);
    }
}
