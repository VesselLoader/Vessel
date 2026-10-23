package dev.vessel.kernel.compat.era;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Deterministic semantic version parser and comparator for Minecraft release versions
 * (e.g., {@code "1.8.9"}, {@code "1.16.5"}, {@code "1.20.4"}, {@code "1.21.4"}, {@code "26.1.2"}).
 *
 * <p>Used by {@link ForwardVersionModRejectionGuard} to detect when a server is running a lower
 * Minecraft version than a mod or plugin requires.</p>
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public final class SemanticMinecraftVersionComparator {

    private static final Pattern VERSION_NUMBER_PATTERN =
            Pattern.compile("(\\d+)\\.(\\d+)(?:\\.(\\d+))?");

    /**
     * Extracts the first canonical {@code major.minor(.patch)} Minecraft version token from a
     * metadata dependency constraint string (such as {@code ">=1.21.4"}, {@code "[1.20.4,1.22)"},
     * {@code "~1.21.1"}, or {@code "1.21"}).
     *
     * @param versionConstraint raw dependency constraint string
     * @return normalized version string (e.g., {@code "1.21.4"}), or {@code ""} if wildcard/absent
     */
    public String extractMinimumRequiredMinecraftVersion(String versionConstraint) {
        if (versionConstraint == null || versionConstraint.isBlank()) {
            return "";
        }
        String trimmed = versionConstraint.trim();
        if ("*".equals(trimmed)) {
            return "";
        }
        Matcher matcher = VERSION_NUMBER_PATTERN.matcher(trimmed);
        if (matcher.find()) {
            String major = matcher.group(1);
            String minor = matcher.group(2);
            String patch = matcher.group(3);
            if (patch != null && !patch.isEmpty()) {
                return major + "." + minor + "." + patch;
            }
            return major + "." + minor + ".0";
        }
        return "";
    }

    /**
     * Compares two Minecraft release version strings numerically component by component.
     *
     * @param versionA first Minecraft version (e.g., {@code "1.21.4"})
     * @param versionB second Minecraft version (e.g., {@code "1.20.1"})
     * @return negative if {@code versionA < versionB}, zero if equal, positive if {@code versionA > versionB}
     */
    public int compareMinecraftVersions(String versionA, String versionB) {
        Objects.requireNonNull(versionA, "versionA must not be null");
        Objects.requireNonNull(versionB, "versionB must not be null");

        int[] partsA = parseNumericComponents(versionA);
        int[] partsB = parseNumericComponents(versionB);
        for (int i = 0; i < 3; i++) {
            int cmp = Integer.compare(partsA[i], partsB[i]);
            if (cmp != 0) {
                return cmp;
            }
        }
        return 0;
    }

    /**
     * Returns {@code true} if {@code modRequiredMcVersion} is strictly higher than
     * {@code activeServerMcVersion} (meaning the server is running a lower version than the mod).
     *
     * @param modRequiredMcVersion  the Minecraft version required by the mod or plugin
     * @param activeServerMcVersion the Minecraft version currently running on the server
     * @return {@code true} if the server version is lower than the mod's required version
     */
    public boolean isServerVersionLowerThanMod(
            String modRequiredMcVersion,
            String activeServerMcVersion) {
        String normalizedMod = extractMinimumRequiredMinecraftVersion(modRequiredMcVersion);
        String normalizedServer = extractMinimumRequiredMinecraftVersion(activeServerMcVersion);
        if (normalizedMod.isEmpty() || normalizedServer.isEmpty()) {
            return false;
        }
        return compareMinecraftVersions(normalizedMod, normalizedServer) > 0;
    }

    private static int[] parseNumericComponents(String version) {
        Matcher matcher = VERSION_NUMBER_PATTERN.matcher(version);
        if (!matcher.find()) {
            return new int[]{0, 0, 0};
        }
        int major = Integer.parseInt(matcher.group(1));
        int minor = Integer.parseInt(matcher.group(2));
        int patch = matcher.group(3) != null ? Integer.parseInt(matcher.group(3)) : 0;
        return new int[]{major, minor, patch};
    }
}
