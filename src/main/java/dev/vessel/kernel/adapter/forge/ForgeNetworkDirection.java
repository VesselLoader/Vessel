package dev.vessel.kernel.adapter.forge;

/**
 * Directional routing flag for Forge network channel packet flow.
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public enum ForgeNetworkDirection {

    /**
     * Play packet flowing from client to server.
     */
    PLAY_TO_SERVER,

    /**
     * Play packet flowing from server to client.
     */
    PLAY_TO_CLIENT,

    /**
     * Handshake/Login packet flowing from client to server.
     */
    LOGIN_TO_SERVER,

    /**
     * Handshake/Login packet flowing from server to client.
     */
    LOGIN_TO_CLIENT,

    /**
     * Configuration phase packet flowing from client to server.
     */
    CONFIGURATION_TO_SERVER,

    /**
     * Configuration phase packet flowing from server to client.
     */
    CONFIGURATION_TO_CLIENT
}
