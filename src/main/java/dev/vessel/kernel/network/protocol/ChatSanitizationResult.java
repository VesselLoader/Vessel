package dev.vessel.kernel.network.protocol;

import java.util.Arrays;
import java.util.Objects;
import java.util.UUID;

/**
 * Immutable record representing the sanitized result of an inbound or outbound chat packet.
 *
 * @param senderUuid         the UUID of the chat sender
 * @param sanitizedContent   the message content string
 * @param sanitizedSignature the sanitized or stripped signature byte array
 * @param envelopeType       the resolved envelope security classification
 * @param signatureStripped  whether an invalid or unsupported signature was stripped
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public record ChatSanitizationResult(
        UUID senderUuid,
        String sanitizedContent,
        byte[] sanitizedSignature,
        ChatEnvelopeType envelopeType,
        boolean signatureStripped
) {

    /**
     * Compact constructor validating non-null requirements.
     */
    public ChatSanitizationResult {
        Objects.requireNonNull(senderUuid, "senderUuid must not be null");
        Objects.requireNonNull(sanitizedContent, "sanitizedContent must not be null");
        Objects.requireNonNull(sanitizedSignature, "sanitizedSignature must not be null");
        Objects.requireNonNull(envelopeType, "envelopeType must not be null");
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof ChatSanitizationResult other)) {
            return false;
        }
        return signatureStripped == other.signatureStripped
                && Objects.equals(senderUuid, other.senderUuid)
                && Objects.equals(sanitizedContent, other.sanitizedContent)
                && Objects.equals(envelopeType, other.envelopeType)
                && Arrays.equals(sanitizedSignature, other.sanitizedSignature);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(senderUuid, sanitizedContent, envelopeType, signatureStripped);
        result = 31 * result + Arrays.hashCode(sanitizedSignature);
        return result;
    }

    @Override
    public String toString() {
        return "ChatSanitizationResult{"
                + "senderUuid=" + senderUuid
                + ", sanitizedContent='" + sanitizedContent + '\''
                + ", signatureLength=" + sanitizedSignature.length
                + ", envelopeType=" + envelopeType
                + ", signatureStripped=" + signatureStripped
                + '}';
    }
}
