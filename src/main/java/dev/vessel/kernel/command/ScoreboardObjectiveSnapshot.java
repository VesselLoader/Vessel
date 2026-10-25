package dev.vessel.kernel.command;

import java.util.Objects;

/**
 * Immutable snapshot record representing a Scoreboard Objective.
 *
 * <p>Strictly adheres to Rule 17 with zero inner or nested classes.</p>
 *
 * @param name        unique objective identifier
 * @param displayName display title formatted with styling
 * @param criteria    tracking criteria (e.g. "dummy", "deathCount", "health")
 * @param renderType  display rendering style ("integer" or "hearts")
 * @author VESSEL Engine Team
 * @version 1.0.0
 */
public record ScoreboardObjectiveSnapshot(
        String name,
        String displayName,
        String criteria,
        String renderType
) {

    /**
     * Compact constructor validating objective properties.
     */
    public ScoreboardObjectiveSnapshot {
        Objects.requireNonNull(name, "name cannot be null");
        displayName = (displayName != null) ? displayName : name;
        criteria = (criteria != null) ? criteria : "dummy";
        renderType = (renderType != null) ? renderType : "integer";
    }
}
