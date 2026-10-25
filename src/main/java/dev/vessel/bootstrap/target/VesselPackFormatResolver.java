package dev.vessel.bootstrap.target;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Resolves the canonical Minecraft client resource pack {@code pack_format} version for any given
 * Minecraft release version across Minecraft history from modern 26.3 / 1.21.4 down to legacy 1.6.1.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes, and zero lambdas.</p>
 *
 * @version 1.0.0
 */
public final class VesselPackFormatResolver {

    private static final Pattern VERSION_PATTERN = Pattern.compile("(\\d+)\\.(\\d+)(?:\\.(\\d+))?");

    public VesselPackFormatResolver() {
    }

    /**
     * Computes the exact resource pack {@code pack_format} integer for a given Minecraft version.
     *
     * @param mcVersion Minecraft version string (e.g. {@code "1.21.4"}, {@code "1.12.2"}, {@code "26.3"})
     * @return the official {@code pack_format} integer
     */
    public int resolvePackFormat(String mcVersion) {
        if (mcVersion == null || mcVersion.isBlank()) {
            return 46;
        }

        String trimmed = mcVersion.trim().toLowerCase(Locale.ROOT);
        if (trimmed.startsWith("26.") || trimmed.equals("26")) {
            return 46;
        }

        Matcher matcher = VERSION_PATTERN.matcher(trimmed);
        if (!matcher.find()) {
            return 46;
        }

        int major = Integer.parseInt(matcher.group(1));
        int minor = Integer.parseInt(matcher.group(2));
        int patch = matcher.group(3) != null ? Integer.parseInt(matcher.group(3)) : 0;

        if (major >= 26) {
            return 46;
        }
        if (major != 1) {
            return 46;
        }

        if (minor >= 22) {
            return 46;
        }
        if (minor == 21) {
            if (patch >= 4) {
                return 46;
            }
            if (patch >= 2) {
                return 42;
            }
            return 34;
        }
        if (minor == 20) {
            if (patch >= 5) {
                return 32;
            }
            if (patch >= 3) {
                return 22;
            }
            return 15;
        }
        if (minor == 19) {
            if (patch >= 4) {
                return 12;
            }
            return 9;
        }
        if (minor == 18) {
            return 8;
        }
        if (minor == 17) {
            return 7;
        }
        if (minor == 16) {
            if (patch >= 2) {
                return 6;
            }
            return 5;
        }
        if (minor == 15) {
            return 5;
        }
        if (minor == 14 || minor == 13) {
            return 4;
        }
        if (minor == 12 || minor == 11) {
            return 3;
        }
        if (minor == 10 || minor == 9) {
            return 2;
        }
        if (minor >= 6) {
            return 1;
        }
        return 1;
    }
}
