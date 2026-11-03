package dev.vessel.kernel.gui.security;

import java.util.Objects;

/**
 * Immutable audit log record capturing an attempted Virtual GUI exploit or theft attack.
 *
 * @param timestamp epoch millisecond timestamp of the incident
 * @param playerId unique identifier of the offending player
 * @param vector the specific theft vector detected
 * @param slotIndex slot index involved in the attempt
 * @param details contextual protocol details or packet metadata
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public record SecurityAuditEntry(
        long timestamp,
        String playerId,
        TheftVector vector,
        int slotIndex,
        String details
) {
    public SecurityAuditEntry {
        Objects.requireNonNull(playerId, "playerId cannot be null");
        Objects.requireNonNull(vector, "vector cannot be null");
        Objects.requireNonNull(details, "details cannot be null");
    }
}
