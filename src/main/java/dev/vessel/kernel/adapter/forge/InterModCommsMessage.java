package dev.vessel.kernel.adapter.forge;

import java.util.Objects;

/**
 * Immutable carrier representing an Inter-Mod Communications (IMC) message exchanged between Forge mods.
 *
 * @param senderModId    the sender mod identifier
 * @param targetModId    the destination mod identifier
 * @param method         communication method/protocol identifier (e.g., "register_compat")
 * @param messagePayload payload object passed to recipient
 * @param timestampNanos monotonic enqueue timestamp in nanoseconds
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public record InterModCommsMessage(
        String senderModId,
        String targetModId,
        String method,
        Object messagePayload,
        long timestampNanos
) {

    /**
     * Compact constructor validating non-null identity fields.
     */
    public InterModCommsMessage {
        Objects.requireNonNull(senderModId, "senderModId must not be null");
        Objects.requireNonNull(targetModId, "targetModId must not be null");
        Objects.requireNonNull(method, "method must not be null");
    }
}
