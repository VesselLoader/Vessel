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
 * @version 1.0.1
 */
public final class ServerEulaAndPropertiesBootstrap {

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
     * returns the parsed key-value property map.
     *
     * @param serverRoot  the server root directory
     * @param defaultPort the default TCP server port (e.g., {@code 25565})
     * @return an unmodifiable map of server properties
     */
    public Map<String, String> ensureServerProperties(Path serverRoot, int defaultPort) {
        Objects.requireNonNull(serverRoot, "serverRoot must not be null");
        Path propsPath = serverRoot.resolve("server.properties");
        int safePort = (defaultPort > 0 && defaultPort <= 65535) ? defaultPort : 25565;

        Map<String, String> defaults = new LinkedHashMap<>();
        defaults.put("server-port", String.valueOf(safePort));
        defaults.put("query.port", String.valueOf(safePort));
        defaults.put("online-mode", "false");
        defaults.put("enforce-secure-profile", "false");
        defaults.put("motd", "A VESSEL Sovereign Hybrid Server (Java & Bedrock Crossplay)");
        defaults.put("max-players", "100");
        defaults.put("view-distance", "10");
        defaults.put("simulation-distance", "10");
        defaults.put("level-name", "world");
        defaults.put("difficulty", "normal");
        defaults.put("gamemode", "survival");
        defaults.put("spawn-protection", "0");
        defaults.put("network-compression-threshold", "256");
        defaults.put("sync-chunk-writes", "false");

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
            return Map.copyOf(loaded);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to initialize server.properties at " + propsPath, e);
        }
    }
}
