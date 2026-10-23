package dev.vessel.kernel.compat.era;

import java.util.Objects;

/**
 * Exception thrown when a mod or plugin requires a newer Minecraft version than the version
 * currently running on the VESSEL server.
 *
 * <p>Instructs the administrator to either install a lower version of the mod/plugin that supports
 * the current server version, or upgrade the server to a higher Minecraft version.</p>
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public final class ForwardVersionModRejectedException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final String moduleId;
    private final String requiredMinecraftVersion;
    private final String activeServerMinecraftVersion;

    /**
     * Constructs a new forward-version rejection exception with an actionable remediation message.
     *
     * @param moduleId                     canonical module ID or plugin name
     * @param requiredMinecraftVersion     minimum Minecraft version required by the module
     * @param activeServerMinecraftVersion Minecraft version currently running on the server
     */
    public ForwardVersionModRejectedException(
            String moduleId,
            String requiredMinecraftVersion,
            String activeServerMinecraftVersion) {
        super(buildRemediationMessage(
                Objects.requireNonNull(moduleId, "moduleId must not be null"),
                Objects.requireNonNull(
                        requiredMinecraftVersion, "requiredMinecraftVersion must not be null"),
                Objects.requireNonNull(
                        activeServerMinecraftVersion,
                        "activeServerMinecraftVersion must not be null")));
        this.moduleId = moduleId;
        this.requiredMinecraftVersion = requiredMinecraftVersion;
        this.activeServerMinecraftVersion = activeServerMinecraftVersion;
    }

    /**
     * Returns the rejected mod or plugin identifier.
     *
     * @return module ID string
     */
    public String getModuleId() {
        return moduleId;
    }

    /**
     * Returns the minimum Minecraft version required by the rejected mod or plugin.
     *
     * @return required Minecraft version string
     */
    public String getRequiredMinecraftVersion() {
        return requiredMinecraftVersion;
    }

    /**
     * Returns the Minecraft version currently running on the server.
     *
     * @return active server Minecraft version string
     */
    public String getActiveServerMinecraftVersion() {
        return activeServerMinecraftVersion;
    }

    private static String buildRemediationMessage(
            String moduleId,
            String requiredMc,
            String serverMc) {
        return "VESSEL Forward-Version Guard refused to load '" + moduleId
                + "': module requires Minecraft " + requiredMc
                + ", but the server is running a lower version (Minecraft " + serverMc
                + "). Please install a lower build of '" + moduleId
                + "' that supports Minecraft " + serverMc
                + ", or run the server on Minecraft " + requiredMc
                + "+ (e.g., --vessel-version=" + requiredMc + " or default latest.release).";
    }
}
