package net.minecraftforge.network;

/**
 * Forge network packet transmission direction ({@code net.minecraftforge.network.NetworkDirection}).
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public enum NetworkDirection {
    /**
     * Play-phase packet sent from client to server.
     */
    PLAY_TO_SERVER,

    /**
     * Play-phase packet sent from server to client.
     */
    PLAY_TO_CLIENT,

    /**
     * Login-handshake packet sent from client to server.
     */
    LOGIN_TO_SERVER,

    /**
     * Login-handshake packet sent from server to client.
     */
    LOGIN_TO_CLIENT
}
