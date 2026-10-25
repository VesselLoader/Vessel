package dev.vessel.kernel.command.brigadier;

/**
 * Strategy applied when two or more subsystems register commands sharing the
 * identical root literal (e.g. {@code /spawn}, {@code /home}, {@code /tpa}).
 *
 * <p>Strictly adheres to Rule 17 with zero inner or nested classes.</p>
 *
 * @author VESSEL Engine Team
 * @version 1.0.0
 */
public enum CommandConflictResolutionStrategy {

    /**
     * Preserves the previously registered command as the un-namespaced root handler.
     * The conflicting newcomer is mapped strictly under its qualified namespace.
     */
    PRESERVE_EXISTING,

    /**
     * Overwrites the un-namespaced root handler with the newcomer.
     * The previous command remains accessible via its qualified namespace.
     */
    OVERWRITE,

    /**
     * Resolves the conflict deterministically using the priority weight of the registering source.
     */
    PRIORITY_BASED,

    /**
     * Strips un-namespaced binding if conflicting to avoid ambiguity, requiring explicit namespace.
     */
    REQUIRE_NAMESPACE_ON_COLLISION
}
