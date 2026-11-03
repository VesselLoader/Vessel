package net.minecraftforge.fml.config;

/**
 * Forge mod configuration scope type ({@code net.minecraftforge.fml.config.ModConfigType}).
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public enum ModConfigType {
    /**
     * Common configuration shared across both client and server environments.
     */
    COMMON,

    /**
     * Client-only visual and user interface configuration.
     */
    CLIENT,

    /**
     * World/server-specific gameplay configuration synchronized to connected clients.
     */
    SERVER,

    /**
     * Early startup configuration loaded prior to registry freeze.
     */
    STARTUP
}
