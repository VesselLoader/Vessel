package dev.vessel.kernel.metrics;

import java.util.Objects;

/**
 * Immutable outcome of a configuration reload operation.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @param success             whether reload succeeded
 * @param sectionCount        number of canonical sections recognized
 * @param propertyCount       number of flat property keys loaded
 * @param configurationSource path or identifier of the source reloaded
 * @param errorMessage        error message if reload failed, or null if successful
 *
 * @version 1.0.0
 */
public record VesselConfigurationReloadResult(
        boolean success,
        int sectionCount,
        int propertyCount,
        String configurationSource,
        String errorMessage) {

    public VesselConfigurationReloadResult {
        Objects.requireNonNull(configurationSource, "configurationSource must not be null");
    }
}
