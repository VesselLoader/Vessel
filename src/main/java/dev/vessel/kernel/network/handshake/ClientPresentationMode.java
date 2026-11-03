package dev.vessel.kernel.network.handshake;

import java.util.Objects;

/**
 * Enumeration of client presentation and connection modes for the VESSEL microkernel engine.
 *
 * <p>In {@link #SOVEREIGN_VANILLA} (default), the server synthesizes UI/models and allows
 * standard Vanilla and Bedrock clients to join with zero client-side mods. In
 * {@link #STRICT_CLIENT_MODDED}, the server bypasses server-side presentation pipelines and
 * requires connecting clients to possess the matching client-side modpack.</p>
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public enum ClientPresentationMode {

    /**
     * Default mode: Zero Client Modifications. Server synthesizes font glyphs, core shaders,
     * virtual display surfaces, and streams resource packs.
     */
    SOVEREIGN_VANILLA("SOVEREIGN_VANILLA"),

    /**
     * Strict Client-Modded Peer Mode: Bypasses server presentation overhead and enforces
     * universal client mod audits. Clients must possess the required modpack.
     */
    STRICT_CLIENT_MODDED("STRICT_CLIENT_MODDED");

    /**
     * Canonical CLI flag to enforce strict client modded peer mode.
     */
    public static final String CLI_FLAG_ENFORCE_CLIENT_MODS = "--vessel-enforce-client-mods";

    /**
     * CLI alias for strict client modded peer mode.
     */
    public static final String CLI_FLAG_STRICT_MODDED = "--vessel-strict-modded";

    /**
     * CLI alias for native client modded peer mode.
     */
    public static final String CLI_FLAG_NATIVE_MODDED = "--vessel-native-modded";

    /**
     * Configuration key in {@code vessel.yml} for client presentation mode.
     */
    public static final String CONFIG_KEY = "network.client_presentation_mode";

    private final String configurationName;

    ClientPresentationMode(String configurationName) {
        this.configurationName = configurationName;
    }

    /**
     * Returns the canonical configuration string for this presentation mode.
     *
     * @return configuration name
     */
    public String getConfigurationName() {
        return configurationName;
    }

    /**
     * Returns whether this mode strictly enforces connecting clients to possess matching mods.
     *
     * @return {@code true} if strict client mods are enforced
     */
    public boolean enforcesClientMods() {
        return this == STRICT_CLIENT_MODDED;
    }

    /**
     * Returns whether this mode completely bypasses server-side presentation generation
     * (e.g. font glyph atlas baking and virtual display surface tracking).
     *
     * @return {@code true} if server presentation is bypassed
     */
    public boolean bypassesServerPresentation() {
        return this == STRICT_CLIENT_MODDED;
    }

    /**
     * Returns whether standard unmodded vanilla clients are permitted to join.
     *
     * @return {@code true} if vanilla clients can connect
     */
    public boolean allowsVanillaClients() {
        return this == SOVEREIGN_VANILLA;
    }

    /**
     * Parses the client presentation mode from command line arguments.
     *
     * @param args the command-line arguments array
     * @return {@link #STRICT_CLIENT_MODDED} if any enforcement flag is present, otherwise {@link #SOVEREIGN_VANILLA}
     */
    public static ClientPresentationMode fromCliArgs(String[] args) {
        if (args == null || args.length == 0) {
            return SOVEREIGN_VANILLA;
        }
        for (String arg : args) {
            if (arg != null) {
                String normalized = arg.trim();
                if (CLI_FLAG_ENFORCE_CLIENT_MODS.equalsIgnoreCase(normalized)
                        || CLI_FLAG_STRICT_MODDED.equalsIgnoreCase(normalized)
                        || CLI_FLAG_NATIVE_MODDED.equalsIgnoreCase(normalized)) {
                    return STRICT_CLIENT_MODDED;
                }
            }
        }
        return SOVEREIGN_VANILLA;
    }

    /**
     * Parses the client presentation mode from a configuration string.
     *
     * @param configValue the configuration string value
     * @return corresponding {@link ClientPresentationMode}, defaulting to {@link #SOVEREIGN_VANILLA}
     */
    public static ClientPresentationMode fromConfigValue(String configValue) {
        if (configValue == null || configValue.isBlank()) {
            return SOVEREIGN_VANILLA;
        }
        String normalized = configValue.trim().toUpperCase();
        if (STRICT_CLIENT_MODDED.name().equals(normalized)) {
            return STRICT_CLIENT_MODDED;
        }
        return SOVEREIGN_VANILLA;
    }
}
