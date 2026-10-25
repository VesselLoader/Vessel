package dev.vessel.kernel.network.handshake;

/**
 * Enumeration of synthetic handshake acknowledgment phases for modded loader handshakes.
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public enum SyntheticHandshakePhase {

    /**
     * FML Handshake mod list receipt acknowledgment.
     */
    FML_HANDSHAKE_RECEIVED_MODS,

    /**
     * FML Handshake registry data synchronization acknowledgment.
     */
    FML_HANDSHAKE_SYNCED_REGISTRIES,

    /**
     * FML Handshake final connection acknowledgment.
     */
    FML_HANDSHAKE_ACK_CONNECTION,

    /**
     * NeoForge Handshake registry and channel acknowledgment.
     */
    NEOFORGE_HANDSHAKE_ACK,

    /**
     * Fabric registry synchronization complete confirmation.
     */
    FABRIC_REGISTRY_SYNC_COMPLETE,

    /**
     * Fabric channel registration acknowledgment.
     */
    FABRIC_CHANNEL_REGISTER_ACK
}
