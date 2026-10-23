package dev.vessel.kernel.compat.era;

import java.util.Objects;

/**
 * Relaxes hard-coded Minecraft version upper bounds in legacy mod and plugin metadata descriptors
 * ({@code fabric.mod.json}, {@code quilt.mod.json}, {@code mods.toml}, {@code neoforge.mods.toml},
 * {@code plugin.yml}, {@code paper-plugin.yml}) when {@code --vessel-universal-compat} is active
 * so modules compiled for older Minecraft versions (1.8.9 through 1.21.x) are accepted on the
 * latest server runtime.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public final class UniversalVersionRangeRelaxer {

    /**
     * Rewrites restrictive Minecraft version constraints in a raw metadata descriptor string so it
     * permits execution on {@code activeLatestMcVersion}.
     *
     * @param rawMetadataContent    the raw JSON/TOML/YAML descriptor text
     * @param activeLatestMcVersion the currently running latest Minecraft version (e.g., {@code "1.21.4"})
     * @return the relaxed metadata descriptor content
     */
    public String relaxMetadataVersionConstraints(
            String rawMetadataContent,
            String activeLatestMcVersion) {
        Objects.requireNonNull(rawMetadataContent, "rawMetadataContent must not be null");
        Objects.requireNonNull(activeLatestMcVersion, "activeLatestMcVersion must not be null");
        String target = activeLatestMcVersion.trim();

        String relaxed = rawMetadataContent
                .replaceAll(
                        "(\"minecraft\"\\s*:\\s*\")([^\"]+)(\")",
                        "$1* (>=" + target + ")$3")
                .replaceAll(
                        "(api-version\\s*:\\s*['\"]?)([0-9]+\\.[0-9]+(?:\\.[0-9]+)?)(['\"]?)",
                        "$1" + extractMajorMinor(target) + "$3")
                .replaceAll(
                        "(versionRange\\s*=\\s*\"\\[)([0-9.]+),([0-9.]+)(\\)\")",
                        "$1$2,)\\\"");
        return relaxed;
    }

    /**
     * Evaluates whether a module's declared Minecraft version constraint is allowed to load on
     * {@code activeLatestMcVersion}.
     *
     * @param declaredConstraint     the version range string from the mod/plugin metadata
     * @param activeLatestMcVersion  the active server Minecraft version
     * @param universalCompatEnabled whether {@code --vessel-universal-compat} is enabled
     * @return {@code true} if the module is permitted to load
     */
    public boolean isVersionCompatibleUnderUniversalMode(
            String declaredConstraint,
            String activeLatestMcVersion,
            boolean universalCompatEnabled) {
        Objects.requireNonNull(declaredConstraint, "declaredConstraint must not be null");
        Objects.requireNonNull(activeLatestMcVersion, "activeLatestMcVersion must not be null");
        if (universalCompatEnabled) {
            return true;
        }
        String trimmed = declaredConstraint.trim();
        return "*".equals(trimmed) || trimmed.contains(activeLatestMcVersion.trim());
    }

    private static String extractMajorMinor(String fullVersion) {
        String[] parts = fullVersion.split("\\.");
        if (parts.length >= 2) {
            return parts[0] + "." + parts[1];
        }
        return fullVersion;
    }
}
