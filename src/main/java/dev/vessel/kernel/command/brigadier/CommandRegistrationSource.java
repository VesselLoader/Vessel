package dev.vessel.kernel.command.brigadier;

import java.util.Objects;

/**
 * Metadata record describing the originating subsystem registering a command hierarchy.
 *
 * <p>Strictly adheres to Rule 17 with zero inner or nested classes.</p>
 *
 * @param subsystemName  identifying label of the subsystem (e.g. "ESSENTIALS_BUKKIT", "FORGE_MOD")
 * @param namespace      canonical namespace prefix (e.g. "essentials", "mekanism")
 * @param priorityWeight integer precedence weight where higher values win conflicts
 * @author VESSEL Engine Team
 * @version 1.0.0
 */
public record CommandRegistrationSource(
        String subsystemName,
        String namespace,
        int priorityWeight
) {

    public static final int PRIORITY_VESSEL_CORE = 1000;
    public static final int PRIORITY_BUKKIT_PLUGIN = 800;
    public static final int PRIORITY_FORGE_MOD = 600;
    public static final int PRIORITY_FABRIC_MOD = 500;
    public static final int PRIORITY_VANILLA = 100;

    /**
     * Compact constructor validating registration source invariants.
     */
    public CommandRegistrationSource {
        Objects.requireNonNull(subsystemName, "Subsystem name cannot be null");
        Objects.requireNonNull(namespace, "Namespace cannot be null");
        namespace = namespace.trim().toLowerCase();
    }
}
