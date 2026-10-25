package dev.vessel.kernel.autoupdate;

import java.util.Locale;
import java.util.Objects;

/**
 * Lifecycle states for transactional server migration and healthcheck observation.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @author VESSEL Engine Team
 * @version 1.0.0
 */
public enum SentinelMigrationState {

    /**
     * Server is operating under confirmed stable execution without pending migrations.
     */
    STABLE,

    /**
     * An update has been deployed and is undergoing post-restart startup healthcheck verification.
     */
    UPGRADING_PENDING_HEALTHCHECK,

    /**
     * Startup healthcheck threw an unrecoverable exception; Sentinel has restored the prior snapshot.
     */
    BOOTSTRAP_FAILED_ROLLED_BACK;

    /**
     * Returns whether the server is currently under active post-upgrade health monitoring.
     *
     * @return {@code true} if healthcheck is pending
     */
    public boolean isPendingHealthcheck() {
        return this == UPGRADING_PENDING_HEALTHCHECK;
    }

    /**
     * Returns whether the server is operating normally in a confirmed stable state.
     *
     * @return {@code true} if confirmed stable
     */
    public boolean isStable() {
        return this == STABLE;
    }

    /**
     * Parses a string representation into a {@link SentinelMigrationState}.
     *
     * @param rawName state name string
     * @return resolved {@link SentinelMigrationState}
     */
    public static SentinelMigrationState fromString(String rawName) {
        if (rawName == null || rawName.isBlank()) {
            return STABLE;
        }
        try {
            return SentinelMigrationState.valueOf(rawName.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return STABLE;
        }
    }
}
