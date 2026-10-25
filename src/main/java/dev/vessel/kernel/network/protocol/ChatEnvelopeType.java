package dev.vessel.kernel.network.protocol;

/**
 * Enumeration of chat message envelope security types for chat packet sanitization.
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public enum ChatEnvelopeType {

    /**
     * Unaltered, cryptographically secure chat message passing Mojang public key verification.
     */
    PASSTHROUGH_SECURE,

    /**
     * System-style unverified message envelope with stripped or null signature.
     */
    SYSTEM_UNVERIFIED,

    /**
     * Synthetic signed chat envelope signed with the server's sovereign private key.
     */
    SYNTHETIC_SIGNED,

    /**
     * Bedrock forwarded chat envelope adapted for Geyser / Floodgate sessions.
     */
    BEDROCK_FORWARDED
}
