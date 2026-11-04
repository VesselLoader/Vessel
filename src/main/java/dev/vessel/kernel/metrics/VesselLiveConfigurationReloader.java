package dev.vessel.kernel.metrics;

import dev.vessel.bootstrap.target.VesselServerConfig;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

/**
 * Performs actual live reloads of the server {@code vessel.yml} configuration file from disk
 * and refreshes {@link VesselServerConfig} in memory.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public final class VesselLiveConfigurationReloader {

    public VesselLiveConfigurationReloader() {
    }

    /**
     * Reloads configuration from the given server root directory into the target configuration store.
     *
     * @param serverRoot root directory of the server
     * @param targetConfig configuration store to refresh
     * @return reload result details
     */
    public VesselConfigurationReloadResult reloadConfiguration(Path serverRoot, VesselServerConfig targetConfig) {
        VesselServerConfig config = (targetConfig != null) ? targetConfig : VesselServerConfig.getActiveConfig();
        Path root = (serverRoot != null) ? serverRoot : Path.of(".");
        Path configFile = root.resolve("vessel.yml");

        try {
            config.loadOrGenerateDefaults(root);
            return new VesselConfigurationReloadResult(
                    true,
                    config.getCanonicalSectionsCount(),
                    config.getLoadedPropertiesCount(),
                    configFile.toString(),
                    null);
        } catch (Exception ex) {
            return new VesselConfigurationReloadResult(
                    false,
                    0,
                    0,
                    configFile.toString(),
                    ex.getMessage());
        }
    }

    /**
     * Formats a single-line summary string for {@code /vessel reload}.
     *
     * @param result configuration reload outcome
     * @return single-line summary
     */
    public String formatSingleLineReload(VesselConfigurationReloadResult result) {
        Objects.requireNonNull(result, "result must not be null");
        if (result.success()) {
            return "[VESSEL] Reloaded " + result.sectionCount() + " canonical sections ("
                    + result.propertyCount() + " properties) from " + result.configurationSource();
        }
        return "[VESSEL] ERROR: Failed to reload configuration: " + result.errorMessage();
    }

    /**
     * Formats console lines for {@code vessel reload}.
     *
     * @param result configuration reload outcome
     * @return list of console output lines
     */
    public List<String> formatConsoleReloadReport(VesselConfigurationReloadResult result) {
        Objects.requireNonNull(result, "result must not be null");
        if (result.success()) {
            return List.of(
                    "[VESSEL/RELOAD] Hot-reloaded " + result.sectionCount()
                            + " enterprise configuration sections (" + result.propertyCount()
                            + " properties) from " + result.configurationSource(),
                    "[VESSEL/RELOAD] Configuration status: LIVE (Hot-applied to runtime settings)");
        }
        return List.of(
                "[VESSEL/RELOAD] FAILED to reload configuration from " + result.configurationSource(),
                "[VESSEL/RELOAD] Cause: " + result.errorMessage());
    }
}
