package dev.vessel.kernel.command;

import java.util.Objects;

/**
 * Immutable snapshot record representing a specific tracked score entry.
 *
 * <p>Strictly adheres to Rule 17 with zero inner or nested classes.</p>
 *
 * @param holderName    name of score owner (player or custom entry)
 * @param objectiveName name of objective being scored
 * @param score         integer score value
 * @param locked        whether the score value is locked from modification
 * @author VESSEL Engine Team
 * @version 1.0.0
 */
public record ScoreboardScoreSnapshot(
        String holderName,
        String objectiveName,
        int score,
        boolean locked
) {

    /**
     * Compact constructor validating score entry keys.
     */
    public ScoreboardScoreSnapshot {
        Objects.requireNonNull(holderName, "holderName cannot be null");
        Objects.requireNonNull(objectiveName, "objectiveName cannot be null");
    }
}
