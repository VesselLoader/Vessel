package dev.vessel.kernel.command.brigadier;

import java.util.Collections;
import java.util.Objects;
import java.util.Set;

/**
 * Result metrics and diagnostic record produced after a Brigadier command tree merge operation.
 *
 * <p>Strictly adheres to Rule 17 with zero inner or nested classes.</p>
 *
 * @param mergedCommandCount       total number of active top-level command literals
 * @param conflictsDetected        count of name collisions detected across subsystems
 * @param namespacedAliasesCreated count of automatic namespace aliases synthesized
 * @param registeredLiterals       unmodifiable set of registered command literals
 * @param success                  whether the tree merge completed without fatal state violations
 * @author VESSEL Engine Team
 * @version 1.0.0
 */
public record BrigadierTreeMergeResult(
        int mergedCommandCount,
        int conflictsDetected,
        int namespacedAliasesCreated,
        Set<String> registeredLiterals,
        boolean success
) {

    /**
     * Compact constructor guaranteeing immutability of registered command literals.
     */
    public BrigadierTreeMergeResult {
        Objects.requireNonNull(registeredLiterals, "registeredLiterals cannot be null");
        registeredLiterals = Collections.unmodifiableSet(registeredLiterals);
    }
}
