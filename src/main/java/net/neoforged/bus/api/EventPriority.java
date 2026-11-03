package net.neoforged.bus.api;

/**
 * NeoForge event listener execution priority ({@code net.neoforged.bus.api.EventPriority}).
 *
 * <p>Listeners with {@link #HIGHEST} priority execute first, proceeding in declaration order
 * through {@link #LOWEST}.</p>
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public enum EventPriority {
    /**
     * Executed first before any lower-priority listeners.
     */
    HIGHEST,

    /**
     * Executed after {@link #HIGHEST} and before {@link #NORMAL}.
     */
    HIGH,

    /**
     * Default execution priority for NeoForge event listeners.
     */
    NORMAL,

    /**
     * Executed after {@link #NORMAL} and before {@link #LOWEST}.
     */
    LOW,

    /**
     * Executed last among NeoForge event listeners.
     */
    LOWEST
}
