package dev.vessel.kernel.network.protocol;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link SovereignChatSignatureSanitizer} and {@link DynamicModdedPacketIdIsolator}.
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
class ChatSignatureAndPacketIsolatorTest {

    @Test
    @DisplayName("SovereignChatSignatureSanitizer handles Bedrock, empty, modified, and secure signatures")
    void testChatSignatureSanitizerBehavior() {
        SovereignChatSignatureSanitizer sanitizer = new SovereignChatSignatureSanitizer(true);
        UUID playerUuid = UUID.randomUUID();
        String message = "Hello from Sovereign Server!";
        long timestamp = System.currentTimeMillis();
        long salt = 123456789L;

        // 1. Bedrock client -> stripped signature, BEDROCK_FORWARDED
        ChatSanitizationResult bedrockResult = sanitizer.sanitizeInboundChat(
                playerUuid, message, new byte[]{1, 2, 3}, timestamp, salt, true
        );
        assertTrue(bedrockResult.signatureStripped());
        assertEquals(ChatEnvelopeType.BEDROCK_FORWARDED, bedrockResult.envelopeType());
        assertEquals(0, bedrockResult.sanitizedSignature().length);
        assertEquals(message, bedrockResult.sanitizedContent());

        // 2. Unsigned vanilla message -> SYSTEM_UNVERIFIED
        ChatSanitizationResult unsignedResult = sanitizer.sanitizeInboundChat(
                playerUuid, message, null, timestamp, salt, false
        );
        assertFalse(unsignedResult.signatureStripped());
        assertEquals(ChatEnvelopeType.SYSTEM_UNVERIFIED, unsignedResult.envelopeType());
        assertEquals(0, unsignedResult.sanitizedSignature().length);

        // 3. Short / altered signature (Paper AsyncChat modified message) -> SYNTHETIC_SIGNED
        byte[] shortSignature = new byte[32];
        ChatSanitizationResult modifiedResult = sanitizer.sanitizeInboundChat(
                playerUuid, message, shortSignature, timestamp, salt, false
        );
        assertTrue(modifiedResult.signatureStripped());
        assertEquals(ChatEnvelopeType.SYNTHETIC_SIGNED, modifiedResult.envelopeType());
        assertEquals(32, modifiedResult.sanitizedSignature().length); // SHA-256 length is 32 bytes

        // 4. Full valid 64-byte signature -> PASSTHROUGH_SECURE
        byte[] validSignature = new byte[64];
        for (int i = 0; i < 64; i++) {
            validSignature[i] = (byte) i;
        }
        ChatSanitizationResult secureResult = sanitizer.sanitizeInboundChat(
                playerUuid, message, validSignature, timestamp, salt, false
        );
        assertFalse(secureResult.signatureStripped());
        assertEquals(ChatEnvelopeType.PASSTHROUGH_SECURE, secureResult.envelopeType());
        assertArrayEquals(validSignature, secureResult.sanitizedSignature());

        // Verification requirement logic
        assertFalse(sanitizer.isSignatureVerificationRequired(true, true));
        assertTrue(sanitizer.isSignatureVerificationRequired(false, true));
        assertFalse(sanitizer.isSignatureVerificationRequired(false, false));

        // Metrics verification
        assertEquals(4L, sanitizer.getSanitizedMessagesCount());
        assertEquals(2L, sanitizer.getStrippedSignaturesCount());
    }

    @Test
    @DisplayName("SovereignChatSignatureSanitizer synthetic signature generation and outbound sanitization")
    void testSyntheticSignatureAndOutbound() {
        SovereignChatSignatureSanitizer sanitizer = new SovereignChatSignatureSanitizer();
        UUID uuid = UUID.randomUUID();
        byte[] sig1 = sanitizer.generateSyntheticSignature(uuid, "Test", 1000L, 55L);
        byte[] sig2 = sanitizer.generateSyntheticSignature(uuid, "Test", 1000L, 55L);
        byte[] sigDifferent = sanitizer.generateSyntheticSignature(uuid, "Other", 1000L, 55L);

        assertNotNull(sig1);
        assertEquals(32, sig1.length);
        assertArrayEquals(sig1, sig2);
        assertFalse(java.util.Arrays.equals(sig1, sigDifferent));

        byte[] rawEnvelope = "raw_packet_bytes".getBytes(StandardCharsets.UTF_8);
        byte[] outboundJava = sanitizer.sanitizeOutboundChatEnvelope(rawEnvelope, false);
        byte[] outboundBedrock = sanitizer.sanitizeOutboundChatEnvelope(rawEnvelope, true);
        assertArrayEquals(rawEnvelope, outboundJava);
        assertArrayEquals(rawEnvelope, outboundBedrock);
    }

    @Test
    @DisplayName("DynamicModdedPacketIdIsolator wraps and unwraps packets in vsl:mod_packet")
    void testDynamicModdedPacketIdIsolatorWrapAndUnwrap() {
        DynamicModdedPacketIdIsolator isolator = new DynamicModdedPacketIdIsolator();
        int originalPacketId = 0x4A;
        byte[] originalPayload = "create_contraption_motion_state".getBytes(StandardCharsets.UTF_8);

        IsolatedPacketEnvelope wrapped = isolator.wrapModdedPacket(originalPacketId, originalPayload);
        assertNotNull(wrapped);
        assertEquals(DynamicModdedPacketIdIsolator.ISOLATED_MOD_CHANNEL, wrapped.targetChannel());
        assertEquals(originalPacketId, wrapped.originalPacketId());
        assertTrue(wrapped.payload().length > originalPayload.length);

        IsolatedPacketEnvelope unwrapped = isolator.unwrapModdedPacket(wrapped.payload());
        assertNotNull(unwrapped);
        assertEquals(originalPacketId, unwrapped.originalPacketId());
        assertArrayEquals(originalPayload, unwrapped.payload());
        assertEquals(1L, isolator.getIsolatedPacketsCount());
    }

    @Test
    @DisplayName("DynamicModdedPacketIdIsolator validates packet ID registration and error handling")
    void testIsolatorRegistrationAndValidation() {
        DynamicModdedPacketIdIsolator isolator = new DynamicModdedPacketIdIsolator();

        // Threshold check
        assertTrue(isolator.isModdedPacketId(0x8001));
        assertFalse(isolator.isModdedPacketId(0x05));

        // Registration check
        isolator.registerModPacketId(0x05);
        assertTrue(isolator.isModdedPacketId(0x05));
        assertTrue(isolator.getRegisteredModPacketIds().contains(0x05));

        assertTrue(isolator.unregisterModPacketId(0x05));
        assertFalse(isolator.isModdedPacketId(0x05));

        // Invalid frame magic on unwrap
        byte[] corruptFrame = new byte[16];
        corruptFrame[0] = 0x11; // wrong magic
        assertThrows(IllegalStateException.class, () -> isolator.unwrapModdedPacket(corruptFrame));

        // Too small frame
        byte[] shortFrame = new byte[8];
        assertThrows(IllegalArgumentException.class, () -> isolator.unwrapModdedPacket(shortFrame));

        // Oversized payload on wrap
        byte[] oversized = new byte[DynamicModdedPacketIdIsolator.MAX_ISOLATED_PAYLOAD_SIZE + 1];
        assertThrows(IllegalArgumentException.class, () -> isolator.wrapModdedPacket(1, oversized));
    }
}
