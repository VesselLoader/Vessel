package dev.vessel.kernel.network;

/**
 * Protocol family distinguishing native Java Edition packets from Bedrock Edition RakNet frames.
 * <p>
 * Governs network multiplexing and serialization rules in the VESSEL dual-protocol core.
 * </p>
 *
 * @version 1.0.1
 * @since 1.0.0 (Pass K-6)
 */
public enum PacketProtocolFamily {

    /** Native Java Edition TCP protocol (Netty-based). */
    JAVA_NATIVE,

    /** Bedrock Edition UDP RakNet protocol (Geyser/Bedrock multiplexed). */
    BEDROCK_RAKNET
}
