package dev.vessel.bootstrap.updater;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses, formats, and deterministically compares canonical VESSEL build version codes of the form:
 * {@code VESSEL-<major>.<minor>.<patch>-rev<revision>+mc<mcVer>-geyser<geyserVer>-spark<sparkVer>}.
 *
 * <p>Allows VESSEL's hourly auto-updater to determine whether the currently running build is
 * strictly older ({@code < 0}), identical ({@code 0}), or newer ({@code > 0}) than the remote
 * release published on GitHub Pages.</p>
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public final class VesselBuildVersionCode {

    public static final String CURRENT_EMBEDDED_VERSION_CODE =
            "VESSEL-2.3.0-rev1046+mc1.21.4-geyser2.6.1-b756-spark1.10.119";

    private static final Pattern SEMVER_PATTERN =
            Pattern.compile("VESSEL-(\\d+)\\.(\\d+)\\.(\\d+)");
    private static final Pattern REVISION_PATTERN =
            Pattern.compile("-rev(\\d+)");
    private static final Pattern MC_PATTERN =
            Pattern.compile("\\+mc([0-9]+\\.[0-9]+(?:\\.[0-9]+)?)");
    private static final Pattern GEYSER_PATTERN =
            Pattern.compile("-geyser([A-Za-z0-9._-]+?)(?=-spark|$)");
    private static final Pattern SPARK_PATTERN =
            Pattern.compile("-spark([A-Za-z0-9._-]+)$");

    /**
     * Formats a canonical VESSEL build version code string.
     *
     * @param vesselSemVer   VESSEL semantic version (e.g., {@code "2.3.0"})
     * @param revisionNumber monotonic integer revision number (e.g., {@code 1046})
     * @param mcVersion      default latest Minecraft version (e.g., {@code "1.21.4"})
     * @param geyserVersion  bundled Geyser version (e.g., {@code "2.6.1-b756"})
     * @param sparkVersion   bundled Spark version (e.g., {@code "1.10.119"})
     * @return canonical version code string
     */
    public String formatVersionCode(
            String vesselSemVer,
            int revisionNumber,
            String mcVersion,
            String geyserVersion,
            String sparkVersion) {
        Objects.requireNonNull(vesselSemVer, "vesselSemVer must not be null");
        Objects.requireNonNull(mcVersion, "mcVersion must not be null");
        Objects.requireNonNull(geyserVersion, "geyserVersion must not be null");
        Objects.requireNonNull(sparkVersion, "sparkVersion must not be null");
        return "VESSEL-" + vesselSemVer.trim()
                + "-rev" + revisionNumber
                + "+mc" + mcVersion.trim()
                + "-geyser" + geyserVersion.trim()
                + "-spark" + sparkVersion.trim();
    }

    /**
     * Extracts the monotonic integer revision number ({@code rev<N>}) from a version code.
     *
     * @param versionCode canonical version code string
     * @return revision number, or {@code 0} if absent
     */
    public int extractRevisionNumber(String versionCode) {
        if (versionCode == null || versionCode.isBlank()) {
            return 0;
        }
        Matcher matcher = REVISION_PATTERN.matcher(versionCode);
        if (matcher.find()) {
            return Integer.parseInt(matcher.group(1));
        }
        return 0;
    }

    /**
     * Extracts the bundled default Minecraft release version from a version code.
     *
     * @param versionCode canonical version code string
     * @return Minecraft version string (e.g., {@code "1.21.4"})
     */
    public String extractBundledMinecraftVersion(String versionCode) {
        if (versionCode == null || versionCode.isBlank()) {
            return "1.21.4";
        }
        Matcher matcher = MC_PATTERN.matcher(versionCode);
        return matcher.find() ? matcher.group(1).trim() : "1.21.4";
    }

    /**
     * Extracts the bundled latest Geyser version from a version code.
     *
     * @param versionCode canonical version code string
     * @return Geyser version string (e.g., {@code "2.6.1-b756"})
     */
    public String extractBundledGeyserVersion(String versionCode) {
        if (versionCode == null || versionCode.isBlank()) {
            return "2.6.1-b756";
        }
        Matcher matcher = GEYSER_PATTERN.matcher(versionCode);
        return matcher.find() ? matcher.group(1).trim() : "2.6.1-b756";
    }

    /**
     * Extracts the bundled latest Spark version from a version code.
     *
     * @param versionCode canonical version code string
     * @return Spark version string (e.g., {@code "1.10.119"})
     */
    public String extractBundledSparkVersion(String versionCode) {
        if (versionCode == null || versionCode.isBlank()) {
            return "1.10.119";
        }
        Matcher matcher = SPARK_PATTERN.matcher(versionCode);
        return matcher.find() ? matcher.group(1).trim() : "1.10.119";
    }

    /**
     * Compares {@code localVersionCode} against {@code remoteVersionCode}.
     *
     * @param localVersionCode  the currently active local VESSEL build code
     * @param remoteVersionCode the remote VESSEL build code fetched from GitHub
     * @return {@code -1} if local is older than remote (update required), {@code 0} if local is
     *         equal to remote (already latest), or {@code +1} if local is newer than remote
     */
    public int compareVersionCodes(String localVersionCode, String remoteVersionCode) {
        Objects.requireNonNull(localVersionCode, "localVersionCode must not be null");
        Objects.requireNonNull(remoteVersionCode, "remoteVersionCode must not be null");

        int[] localSemVer = parseSemVer(localVersionCode);
        int[] remoteSemVer = parseSemVer(remoteVersionCode);
        for (int i = 0; i < 3; i++) {
            int cmp = Integer.compare(localSemVer[i], remoteSemVer[i]);
            if (cmp != 0) {
                return cmp < 0 ? -1 : 1;
            }
        }
        int localRev = extractRevisionNumber(localVersionCode);
        int remoteRev = extractRevisionNumber(remoteVersionCode);
        int revCmp = Integer.compare(localRev, remoteRev);
        if (revCmp < 0) {
            return -1;
        }
        if (revCmp > 0) {
            return 1;
        }
        return 0;
    }

    private static int[] parseSemVer(String versionCode) {
        Matcher m = SEMVER_PATTERN.matcher(versionCode);
        if (!m.find()) {
            return new int[]{0, 0, 0};
        }
        return new int[]{
            Integer.parseInt(m.group(1)),
            Integer.parseInt(m.group(2)),
            Integer.parseInt(m.group(3))
        };
    }
}
