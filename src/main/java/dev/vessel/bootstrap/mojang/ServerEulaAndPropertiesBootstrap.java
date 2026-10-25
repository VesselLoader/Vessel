package dev.vessel.bootstrap.mojang;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Scaffolds and validates {@code eula.txt} and {@code server.properties} in the server root
 * directory so VESSEL can boot a live Minecraft server in a single pass on the very first run.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.5
 */
public final class ServerEulaAndPropertiesBootstrap {

    private final dev.vessel.bootstrap.target.VesselServerPropertiesPackConfigurator packConfigurator =
            new dev.vessel.bootstrap.target.VesselServerPropertiesPackConfigurator();

    public ServerEulaAndPropertiesBootstrap() {
    }

    /**
     * Ensures {@code eula.txt} exists in {@code serverRoot} and checks or writes EULA acceptance.
     * When {@code autoAcceptFlag} is {@code true} (single-pass boot or {@code --vessel-accept-eula}
     * or {@code -Dcom.mojang.eula.agree=true}), writes {@code eula=true} automatically so the first
     * launch proceeds without requiring a manual restart.
     *
     * @param serverRoot     the server root directory
     * @param autoAcceptFlag whether EULA acceptance should be written automatically
     * @return {@code true} if {@code eula=true} is active
     */
    public boolean ensureEulaAccepted(Path serverRoot, boolean autoAcceptFlag) {
        Objects.requireNonNull(serverRoot, "serverRoot must not be null");
        Path eulaPath = serverRoot.resolve("eula.txt");
        boolean jvmPropertyAgree = Boolean.getBoolean("com.mojang.eula.agree");
        boolean effectiveAutoAccept = autoAcceptFlag || jvmPropertyAgree;

        try {
            Files.createDirectories(serverRoot);
            if (Files.exists(eulaPath)) {
                String existing = Files.readString(eulaPath, StandardCharsets.UTF_8);
                if (existing.contains("eula=true") || existing.contains("eula = true")) {
                    return true;
                }
                if (effectiveAutoAccept) {
                    Files.writeString(
                            eulaPath,
                            "#By changing the setting below to TRUE you are indicating your agreement to Mojang's EULA (https://aka.ms/MinecraftEULA).\n"
                                    + "eula=true\n",
                            StandardCharsets.UTF_8);
                    return true;
                }
                return false;
            }

            String eulaValue = effectiveAutoAccept ? "true" : "false";
            Files.writeString(
                    eulaPath,
                    "#By changing the setting below to TRUE you are indicating your agreement to Mojang's EULA (https://aka.ms/MinecraftEULA).\n"
                            + "eula=" + eulaValue + "\n",
                    StandardCharsets.UTF_8);
            return effectiveAutoAccept;
        } catch (IOException e) {
            throw new IllegalStateException("Failed to initialize eula.txt at " + eulaPath, e);
        }
    }

    /**
     * Ensures {@code server.properties} exists in {@code serverRoot} with canonical defaults and
     * returns the parsed key-value property map for Minecraft 1.21.4 / 26.3.
     *
     * @param serverRoot  the server root directory
     * @param defaultPort the default TCP server port (e.g., {@code 25565})
     * @return an unmodifiable map of server properties
     */
    public Map<String, String> ensureServerProperties(Path serverRoot, int defaultPort) {
        return ensureServerProperties(serverRoot, defaultPort, "1.21.4");
    }

    /**
     * Ensures {@code server.properties} exists in {@code serverRoot} with era-aware defaults and
     * returns the parsed key-value property map.
     *
     * @param serverRoot  the server root directory
     * @param defaultPort the default TCP server port (e.g., {@code 25565})
     * @param mcVersion   the running Minecraft version string
     * @return an unmodifiable map of server properties
     */
    public Map<String, String> ensureServerProperties(Path serverRoot, int defaultPort, String mcVersion) {
        Objects.requireNonNull(serverRoot, "serverRoot must not be null");
        Path propsPath = serverRoot.resolve("server.properties");
        int safePort = (defaultPort > 0 && defaultPort <= 65535) ? defaultPort : 25565;

        int[] v = parseVersion(mcVersion);
        boolean isModern = v[0] >= 26;
        boolean supportsCompressionAndWatchdog = isModern || (v[0] == 1 && v[1] >= 8);
        boolean supportsSyncChunkWrites = isModern || (v[0] == 1 && (v[1] > 14 || (v[1] == 14 && v[2] >= 4)));
        boolean supportsSimulationDistance = isModern || (v[0] == 1 && v[1] >= 18);
        boolean supportsSecureProfile = isModern || (v[0] == 1 && (v[1] > 19 || (v[1] == 19 && v[2] >= 1)));
        boolean supportsPauseWhenEmpty = isModern || (v[0] == 1 && (v[1] > 20 || (v[1] == 20 && v[2] >= 5)));

        Map<String, String> defaults = new LinkedHashMap<>();
        defaults.put("server-port", String.valueOf(safePort));
        defaults.put("query.port", String.valueOf(safePort));
        defaults.put("online-mode", "false");
        if (supportsSecureProfile) {
            defaults.put("enforce-secure-profile", "false");
        }
        defaults.put("motd", "A VESSEL Sovereign Hybrid Server (Java & Bedrock Crossplay)");
        defaults.put("max-players", "100");
        defaults.put("view-distance", "7");
        if (supportsSimulationDistance) {
            defaults.put("simulation-distance", "5");
        }
        if (supportsPauseWhenEmpty) {
            defaults.put("pause-when-empty-seconds", "0");
        }
        defaults.put("level-name", "world");
        defaults.put("difficulty", "normal");
        defaults.put("gamemode", "survival");
        defaults.put("spawn-protection", "0");
        if (supportsCompressionAndWatchdog) {
            defaults.put("network-compression-threshold", "256");
            defaults.put("max-tick-time", "-1");
        }
        if (supportsSyncChunkWrites) {
            defaults.put("sync-chunk-writes", "false");
        }
        if (packConfigurator.isSupportedPrompt(mcVersion)) {
            defaults.put("resource-pack-prompt", "{\"text\":\"VESSEL Sovereign Universal Mod Presentation Pack\"}");
        }
        if (packConfigurator.isSupportedPackId(mcVersion)) {
            defaults.put("resource-pack-id", "fcce4567-99de-3e01-949b-9799d6489542");
        }

        try {
            Files.createDirectories(serverRoot);
            if (!Files.exists(propsPath)) {
                StringBuilder sb = new StringBuilder(512);
                sb.append("#Minecraft server properties (Managed by VESSEL Sovereign Microkernel)\n");
                for (Map.Entry<String, String> entry : defaults.entrySet()) {
                    sb.append(entry.getKey()).append('=').append(entry.getValue()).append('\n');
                }
                Files.writeString(propsPath, sb.toString(), StandardCharsets.UTF_8);
                return Map.copyOf(defaults);
            }

            Map<String, String> loaded = new LinkedHashMap<>(defaults);
            String content = Files.readString(propsPath, StandardCharsets.UTF_8);
            boolean modified = false;
            for (String rawLine : content.split("\\R")) {
                String line = rawLine.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                int eq = line.indexOf('=');
                if (eq > 0) {
                    loaded.put(line.substring(0, eq).trim(), line.substring(eq + 1).trim());
                }
            }
            if (supportsCompressionAndWatchdog && ("60000".equals(loaded.get("max-tick-time")) || !loaded.containsKey("max-tick-time"))) {
                loaded.put("max-tick-time", "-1");
                modified = true;
            }
            if (!supportsCompressionAndWatchdog) {
                if (loaded.remove("network-compression-threshold") != null) {
                    modified = true;
                }
                if (loaded.remove("max-tick-time") != null) {
                    modified = true;
                }
            }
            if (!supportsSimulationDistance && loaded.remove("simulation-distance") != null) {
                modified = true;
            }
            if (!supportsSecureProfile && loaded.remove("enforce-secure-profile") != null) {
                modified = true;
            }
            if (!supportsPauseWhenEmpty && loaded.remove("pause-when-empty-seconds") != null) {
                modified = true;
            }
            if (!supportsSyncChunkWrites && loaded.remove("sync-chunk-writes") != null) {
                modified = true;
            }
            if (modified) {
                StringBuilder sb = new StringBuilder(512);
                sb.append("#Minecraft server properties (Managed by VESSEL Sovereign Microkernel)\n");
                for (Map.Entry<String, String> entry : loaded.entrySet()) {
                    sb.append(entry.getKey()).append('=').append(entry.getValue()).append('\n');
                }
                Files.writeString(propsPath, sb.toString(), StandardCharsets.UTF_8);
            }
            return Map.copyOf(loaded);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to initialize server.properties at " + propsPath, e);
        }
    }

    private static int[] parseVersion(String mcVersion) {
        if (mcVersion == null || mcVersion.isBlank()) {
            return new int[]{1, 21, 4};
        }
        String trimmed = mcVersion.trim();
        if (trimmed.startsWith("26.") || trimmed.equals("26")) {
            return new int[]{26, 3, 0};
        }
        java.util.regex.Matcher matcher =
                java.util.regex.Pattern.compile("(\\d+)\\.(\\d+)(?:\\.(\\d+))?").matcher(trimmed);
        if (!matcher.find()) {
            return new int[]{1, 21, 4};
        }
        int major = Integer.parseInt(matcher.group(1));
        int minor = Integer.parseInt(matcher.group(2));
        int patch = matcher.group(3) != null ? Integer.parseInt(matcher.group(3)) : 0;
        return new int[]{major, minor, patch};
    }
}
