package dev.vessel.bootstrap.target;

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * Verifies Velocity Modern Forwarding HMAC-SHA256 signatures and extracts forwarded
 * player IP addresses from Velocity or BungeeCord login handshake payloads.
 *
 * @version 1.0.0
 */
public final class VelocityModernForwardingHandler {

    private static final String HMAC_SHA256_ALGORITHM = "HmacSHA256";

    /**
     * Creates a new Velocity Modern Forwarding handler.
     */
    public VelocityModernForwardingHandler() {
    }

    /**
     * Verifies the Velocity Modern Forwarding HMAC-SHA256 signature in constant time.
     *
     * @param payloadBytes      the forwarded player info payload bytes
     * @param receivedSignature the 32-byte HMAC-SHA256 signature sent by the proxy
     * @param forwardingSecret  the shared Velocity forwarding secret string
     * @return {@code true} if the signature is valid and matches the secret
     */
    public boolean verifyVelocityHmacSignature(
            byte[] payloadBytes, byte[] receivedSignature, String forwardingSecret) {
        if (payloadBytes == null || receivedSignature == null || forwardingSecret == null) {
            return false;
        }
        if (forwardingSecret.isEmpty() || receivedSignature.length == 0) {
            return false;
        }
        try {
            Mac mac = Mac.getInstance(HMAC_SHA256_ALGORITHM);
            byte[] keyBytes = forwardingSecret.getBytes(StandardCharsets.UTF_8);
            SecretKeySpec keySpec = new SecretKeySpec(keyBytes, HMAC_SHA256_ALGORITHM);
            mac.init(keySpec);
            byte[] computedSignature = mac.doFinal(payloadBytes);
            return MessageDigest.isEqual(computedSignature, receivedSignature);
        } catch (NoSuchAlgorithmException | InvalidKeyException ex) {
            throw new IllegalStateException("Failed to compute HMAC-SHA256 for Velocity forwarding", ex);
        }
    }

    /**
     * Extracts the forwarded client IP address from either a BungeeCord null-delimited
     * handshake string ({@code host\0ip\0uuid}) or a key-value / colon-delimited Velocity
     * forwarding payload.
     *
     * @param bungeeOrVelocityPayload the handshake or forwarding payload string
     * @return the extracted player IP address, or {@code "127.0.0.1"} if absent
     */
    public String extractForwardedPlayerIp(String bungeeOrVelocityPayload) {
        Objects.requireNonNull(bungeeOrVelocityPayload, "bungeeOrVelocityPayload must not be null");
        String trimmed = bungeeOrVelocityPayload.trim();
        if (trimmed.isEmpty()) {
            return "127.0.0.1";
        }
        if (trimmed.indexOf('\0') >= 0 || trimmed.indexOf('\u0001') >= 0) {
            String[] segments = trimmed.split("[\0\u0001]");
            if (segments.length >= 2 && !segments[1].isBlank()) {
                return segments[1].trim();
            }
        }
        int ipPrefixIdx = trimmed.indexOf("ip=");
        if (ipPrefixIdx >= 0) {
            String remainder = trimmed.substring(ipPrefixIdx + 3);
            int delimiterIdx = findFirstDelimiter(remainder);
            String candidate = delimiterIdx >= 0 ? remainder.substring(0, delimiterIdx) : remainder;
            if (!candidate.isBlank()) {
                return candidate.trim();
            }
        }
        if (trimmed.contains("|")) {
            String[] parts = trimmed.split("\\|");
            if (parts.length >= 2 && !parts[1].isBlank()) {
                return parts[1].trim();
            }
        }
        return trimmed;
    }

    private static int findFirstDelimiter(String input) {
        int minIdx = -1;
        char[] delimiters = new char[]{';', ',', '|', ' ', '\n', '\r'};
        for (char delimiter : delimiters) {
            int idx = input.indexOf(delimiter);
            if (idx >= 0 && (minIdx < 0 || idx < minIdx)) {
                minIdx = idx;
            }
        }
        return minIdx;
    }
}
