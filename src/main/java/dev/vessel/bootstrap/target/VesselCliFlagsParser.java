package dev.vessel.bootstrap.target;

import dev.vessel.bootstrap.mojang.VesselCustomFlagsProfile;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Command-line argument parser for the VESSEL server launcher.
 *
 * <p>Parses {@code --vessel-version="26.2","26.1","1.21.1"} (Decision 2), {@code --nogui},
 * VESSEL's custom deep-debug and ultra-optimization flag suite ({@link VesselCustomFlagsProfile}),
 * and arbitrary {@code --key=value} server flags in strict compliance with Rule 17.</p>
 *
 * @version 1.0.2
 * @since 1.0.0 (Pass K-29)
 */
public final class VesselCliFlagsParser {

    private final List<String> scopedVesselVersions = new ArrayList<>();
    private final Map<String, String> parsedFlags = new ConcurrentHashMap<>();
    private final VesselCustomFlagsProfile customFlagsProfile = new VesselCustomFlagsProfile();
    private volatile boolean noGuiRequested;

    /**
     * Parses the raw JVM command-line arguments array.
     *
     * @param args command-line arguments
     */
    public void parseArguments(String[] args) {
        Objects.requireNonNull(args, "args cannot be null");
        scopedVesselVersions.clear();
        parsedFlags.clear();
        noGuiRequested = false;
        customFlagsProfile.applyCliArguments(args);

        for (String rawArg : args) {
            if (rawArg == null || rawArg.isBlank()) {
                continue;
            }
            String arg = rawArg.trim();
            if ("--nogui".equalsIgnoreCase(arg) || "nogui".equalsIgnoreCase(arg)) {
                noGuiRequested = true;
                parsedFlags.put("nogui", "true");
                continue;
            }

            if (arg.startsWith("--")) {
                String body = arg.substring(2);
                int eqIdx = body.indexOf('=');
                if (eqIdx > 0) {
                    String key = body.substring(0, eqIdx).trim();
                    String val = stripSurroundingQuotes(body.substring(eqIdx + 1).trim());
                    parsedFlags.put(key, val);
                    if ("vessel-version".equalsIgnoreCase(key)) {
                        parseVersionList(val);
                    }
                } else {
                    parsedFlags.put(body.trim(), "true");
                }
            }
        }
    }

    /**
     * Returns the ordered list of Minecraft versions specified via {@code --vessel-version}.
     *
     * @return unmodifiable list of scoped version strings
     */
    public List<String> getScopedVesselVersions() {
        return List.copyOf(scopedVesselVersions);
    }

    /**
     * Checks whether headless console mode ({@code --nogui} or {@code nogui}) was requested.
     *
     * @return {@code true} if GUI is disabled
     */
    public boolean isNoGuiRequested() {
        return noGuiRequested;
    }

    /**
     * Returns the parsed VESSEL custom flags profile (deep debug, ultra optimization, single-pass
     * boot, auto-latest version, and universal multi-era compatibility).
     *
     * @return the {@link VesselCustomFlagsProfile}
     */
    public VesselCustomFlagsProfile getCustomFlagsProfile() {
        return customFlagsProfile;
    }

    /**
     * Retrieves the parsed value of a CLI flag, or returns {@code defaultValue} if absent.
     *
     * @param flagKey      flag name without leading {@code --}
     * @param defaultValue fallback value
     * @return parsed flag value or fallback
     */
    public String getFlagValueOrDefault(String flagKey, String defaultValue) {
        Objects.requireNonNull(flagKey, "flagKey cannot be null");
        String cleanKey = flagKey.startsWith("--") ? flagKey.substring(2) : flagKey;
        return parsedFlags.getOrDefault(cleanKey, defaultValue);
    }

    private void parseVersionList(String rawValue) {
        String[] parts = rawValue.split(",");
        for (String part : parts) {
            String cleaned = stripQuotesAndWhitespace(part);
            if (!cleaned.isEmpty()) {
                scopedVesselVersions.add(cleaned);
            }
        }
    }

    private static String stripSurroundingQuotes(String input) {
        if (input.contains(",")) {
            return input.trim();
        }
        return stripQuotesAndWhitespace(input);
    }

    private static String stripQuotesAndWhitespace(String input) {
        int start = 0;
        int end = input.length();
        while (start < end && (Character.isWhitespace(input.charAt(start))
                || input.charAt(start) == '"'
                || input.charAt(start) == '\'')) {
            start++;
        }
        while (end > start && (Character.isWhitespace(input.charAt(end - 1))
                || input.charAt(end - 1) == '"'
                || input.charAt(end - 1) == '\'')) {
            end--;
        }
        return input.substring(start, end);
    }
}
