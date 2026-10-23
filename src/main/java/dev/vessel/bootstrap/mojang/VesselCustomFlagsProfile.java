package dev.vessel.bootstrap.mojang;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Defines and evaluates VESSEL's custom CLI flag suite, including the dedicated deep-debugging
 * mode ({@code --vessel-deep-debug}), the maximum-performance optimization bundle
 * ({@code --vessel-ultra-optimize} / {@code --vessel-max-perf}), universal multi-era
 * mod/plugin compatibility ({@code --vessel-universal-compat}), automatic latest Minecraft release
 * resolution ({@code --vessel-auto-latest}), and single-pass first-run startup
 * ({@code --vessel-single-pass-boot}).
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public final class VesselCustomFlagsProfile {

    public static final String FLAG_DEEP_DEBUG = "vessel-deep-debug";
    public static final String FLAG_DEBUG_ALL = "vessel-debug-all";
    public static final String FLAG_TRACE_BYTECODE = "vessel-trace-bytecode";
    public static final String FLAG_TRACE_CLASSLOADER = "vessel-trace-classloader";
    public static final String FLAG_TRACE_EVENTS = "vessel-trace-events";
    public static final String FLAG_TRACE_REGISTRY = "vessel-trace-registry";
    public static final String FLAG_PROFILE_BOOT = "vessel-profile-boot";

    public static final String FLAG_ULTRA_OPTIMIZE = "vessel-ultra-optimize";
    public static final String FLAG_MAX_PERF = "vessel-max-perf";
    public static final String FLAG_SIMD_PHYSICS = "vessel-simd-physics";
    public static final String FLAG_ZERO_COPY_NETTY = "vessel-zero-copy-netty";
    public static final String FLAG_AGGRESSIVE_AI_LOD = "vessel-aggressive-ai-lod";
    public static final String FLAG_ZSTD_RAM_CACHE = "vessel-zstd-ram-cache";
    public static final String FLAG_LOCKLESS_STRANDS = "vessel-lockless-strands";
    public static final String FLAG_LAZY_DFU_NOISIUM = "vessel-lazy-dfu-noisium";

    public static final String FLAG_UNIVERSAL_COMPAT = "vessel-universal-compat";
    public static final String FLAG_AUTO_LATEST = "vessel-auto-latest";
    public static final String FLAG_SINGLE_PASS_BOOT = "vessel-single-pass-boot";
    public static final String FLAG_ACCEPT_EULA = "vessel-accept-eula";
    public static final String FLAG_LENIENT_LOAD = "vessel-lenient-load";
    public static final String FLAG_LEGACY_FIRST_RUN_EXIT = "vessel-legacy-first-run-exit";

    private final Set<String> activeBooleanFlags = ConcurrentHashMap.newKeySet();
    private final Map<String, String> customKeyValues = new ConcurrentHashMap<>();

    /**
     * Constructs a new custom flags profile with VESSEL's out-of-the-box defaults enabled
     * ({@code universal-compat}, {@code auto-latest}, {@code single-pass-boot}, and
     * {@code lenient-load}).
     */
    public VesselCustomFlagsProfile() {
        resetToDefaults();
    }

    /**
     * Resets all flags to VESSEL's canonical single-pass universal defaults.
     */
    public void resetToDefaults() {
        activeBooleanFlags.clear();
        customKeyValues.clear();
        activeBooleanFlags.add(FLAG_UNIVERSAL_COMPAT);
        activeBooleanFlags.add(FLAG_AUTO_LATEST);
        activeBooleanFlags.add(FLAG_SINGLE_PASS_BOOT);
        activeBooleanFlags.add(FLAG_LENIENT_LOAD);
    }

    /**
     * Parses raw JVM command-line arguments and activates matching VESSEL custom flags and
     * composite optimization/debug profiles.
     *
     * @param args the raw CLI arguments array
     */
    public void applyCliArguments(String[] args) {
        Objects.requireNonNull(args, "args must not be null");
        resetToDefaults();

        for (String rawArg : args) {
            if (rawArg == null || rawArg.isBlank()) {
                continue;
            }
            String trimmed = rawArg.trim();
            if (!trimmed.startsWith("--")) {
                continue;
            }
            String body = trimmed.substring(2).trim();
            int eqIdx = body.indexOf('=');
            String key;
            String val;
            if (eqIdx > 0) {
                key = body.substring(0, eqIdx).trim().toLowerCase(Locale.ROOT);
                val = stripQuotes(body.substring(eqIdx + 1).trim());
            } else {
                key = body.toLowerCase(Locale.ROOT);
                val = "true";
            }

            customKeyValues.put(key, val);
            if ("false".equalsIgnoreCase(val) || "0".equals(val) || "off".equalsIgnoreCase(val)) {
                activeBooleanFlags.remove(key);
                continue;
            }

            activeBooleanFlags.add(key);
            if (FLAG_DEEP_DEBUG.equals(key) || FLAG_DEBUG_ALL.equals(key)) {
                enableDeepDebugSuite();
            }
            if (FLAG_ULTRA_OPTIMIZE.equals(key) || FLAG_MAX_PERF.equals(key)) {
                enableUltraOptimizationSuite();
            }
            if (FLAG_LEGACY_FIRST_RUN_EXIT.equals(key)) {
                activeBooleanFlags.remove(FLAG_SINGLE_PASS_BOOT);
            }
        }
    }

    /**
     * Activates the complete deep-debugging diagnostic suite (`--vessel-deep-debug`).
     */
    public void enableDeepDebugSuite() {
        activeBooleanFlags.add(FLAG_DEEP_DEBUG);
        activeBooleanFlags.add(FLAG_DEBUG_ALL);
        activeBooleanFlags.add(FLAG_TRACE_BYTECODE);
        activeBooleanFlags.add(FLAG_TRACE_CLASSLOADER);
        activeBooleanFlags.add(FLAG_TRACE_EVENTS);
        activeBooleanFlags.add(FLAG_TRACE_REGISTRY);
        activeBooleanFlags.add(FLAG_PROFILE_BOOT);
    }

    /**
     * Activates the maximum-performance optimization suite (`--vessel-ultra-optimize` /
     * `--vessel-max-perf`), turning on all 40 super-mod and cross-chunk governors plus SIMD,
     * zero-copy Netty, Zstd RAM caching, lockless strands, and LazyDFU/Noisium direct palette I/O.
     */
    public void enableUltraOptimizationSuite() {
        activeBooleanFlags.add(FLAG_ULTRA_OPTIMIZE);
        activeBooleanFlags.add(FLAG_MAX_PERF);
        activeBooleanFlags.add(FLAG_SIMD_PHYSICS);
        activeBooleanFlags.add(FLAG_ZERO_COPY_NETTY);
        activeBooleanFlags.add(FLAG_AGGRESSIVE_AI_LOD);
        activeBooleanFlags.add(FLAG_ZSTD_RAM_CACHE);
        activeBooleanFlags.add(FLAG_LOCKLESS_STRANDS);
        activeBooleanFlags.add(FLAG_LAZY_DFU_NOISIUM);
    }

    /**
     * Returns whether deep diagnostic debugging is active.
     *
     * @return {@code true} if deep debug mode is enabled
     */
    public boolean isDeepDebugEnabled() {
        return activeBooleanFlags.contains(FLAG_DEEP_DEBUG)
                || activeBooleanFlags.contains(FLAG_DEBUG_ALL);
    }

    /**
     * Returns whether maximum-performance ultra optimization mode is active.
     *
     * @return {@code true} if ultra optimization mode is enabled
     */
    public boolean isUltraOptimizationEnabled() {
        return activeBooleanFlags.contains(FLAG_ULTRA_OPTIMIZE)
                || activeBooleanFlags.contains(FLAG_MAX_PERF);
    }

    /**
     * Returns whether single-pass boot (completing first-run setup and booting the server in one
     * invocation without restarting) is active.
     *
     * @return {@code true} if single-pass boot is enabled
     */
    public boolean isSinglePassBootEnabled() {
        return activeBooleanFlags.contains(FLAG_SINGLE_PASS_BOOT)
                && !activeBooleanFlags.contains(FLAG_LEGACY_FIRST_RUN_EXIT);
    }

    /**
     * Returns whether universal multi-era mod and plugin compatibility is enabled.
     *
     * @return {@code true} if universal backward compatibility is enabled
     */
    public boolean isUniversalCompatEnabled() {
        return activeBooleanFlags.contains(FLAG_UNIVERSAL_COMPAT);
    }

    /**
     * Returns whether automatic latest Minecraft release resolution is enabled.
     *
     * @return {@code true} if auto-latest version resolution is enabled
     */
    public boolean isAutoLatestVersionEnabled() {
        return activeBooleanFlags.contains(FLAG_AUTO_LATEST);
    }

    /**
     * Returns whether lenient mod/plugin loading (synthesizing missing stubs instead of aborting)
     * is enabled.
     *
     * @return {@code true} if lenient load is enabled
     */
    public boolean isLenientLoadEnabled() {
        return activeBooleanFlags.contains(FLAG_LENIENT_LOAD);
    }

    /**
     * Returns whether automatic EULA acceptance (`--vessel-accept-eula`) was passed.
     *
     * @return {@code true} if EULA auto-accept is enabled
     */
    public boolean isAcceptEulaEnabled() {
        return activeBooleanFlags.contains(FLAG_ACCEPT_EULA);
    }

    /**
     * Checks whether a specific boolean flag key (without leading {@code --}) is active.
     *
     * @param flagKey the normalized flag key
     * @return {@code true} if active
     */
    public boolean isFlagEnabled(String flagKey) {
        Objects.requireNonNull(flagKey, "flagKey must not be null");
        String clean = flagKey.startsWith("--")
                ? flagKey.substring(2).toLowerCase(Locale.ROOT)
                : flagKey.toLowerCase(Locale.ROOT);
        return activeBooleanFlags.contains(clean);
    }

    /**
     * Exports a diagnostic snapshot of all active VESSEL flags and their states.
     *
     * @return an unmodifiable map of active flags
     */
    public Map<String, Boolean> exportActiveFlagSnapshot() {
        List<String> sorted = activeBooleanFlags.stream().sorted().toList();
        Map<String, Boolean> snapshot = new LinkedHashMap<>();
        for (String flag : sorted) {
            snapshot.put(flag, Boolean.TRUE);
        }
        return Collections.unmodifiableMap(snapshot);
    }

    private static String stripQuotes(String raw) {
        if (raw.length() >= 2
                && ((raw.startsWith("\"") && raw.endsWith("\""))
                        || (raw.startsWith("'") && raw.endsWith("'")))) {
            return raw.substring(1, raw.length() - 1).trim();
        }
        return raw;
    }
}
