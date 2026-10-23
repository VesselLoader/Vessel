package dev.vessel.command;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe persistent home data store for Bukkit and EssentialsX home commands
 * ({@code /sethome}, {@code /home}, {@code /delhome}).
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public final class VesselHomeDataStore {

    private static final String STORE_FILE_NAME = "homes.properties";
    private final Map<String, String> inMemoryHomes = new ConcurrentHashMap<>();
    private volatile Path activeConfigDir;

    public VesselHomeDataStore() {
    }

    /**
     * Initializes the storage directory and loads any existing homes from disk.
     *
     * @param serverRoot root directory of the running server
     */
    public synchronized void initialize(Path serverRoot) {
        if (serverRoot == null) {
            return;
        }
        this.activeConfigDir = serverRoot.resolve("vessel-config");
        try {
            Files.createDirectories(activeConfigDir);
            Path homeFile = activeConfigDir.resolve(STORE_FILE_NAME);
            if (Files.isRegularFile(homeFile)) {
                List<String> lines = Files.readAllLines(homeFile, StandardCharsets.UTF_8);
                for (String line : lines) {
                    String trimmed = line.trim();
                    if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                        continue;
                    }
                    int eq = trimmed.indexOf('=');
                    if (eq > 0) {
                        String key = trimmed.substring(0, eq).trim();
                        String val = trimmed.substring(eq + 1).trim();
                        inMemoryHomes.put(key, val);
                    }
                }
            }
        } catch (IOException ignored) {
        }
    }

    /**
     * Sets or updates a named home for a player.
     *
     * @param playerKey player name or UUID string
     * @param homeName  name of the home (e.g., "home" or "base")
     * @param world     dimension or world name
     * @param x         X coordinate
     * @param y         Y coordinate
     * @param z         Z coordinate
     * @param yaw       player yaw rotation
     * @param pitch     player pitch rotation
     */
    public void setHome(
            String playerKey,
            String homeName,
            String world,
            double x,
            double y,
            double z,
            float yaw,
            float pitch) {
        Objects.requireNonNull(playerKey, "playerKey must not be null");
        String safeHomeName = (homeName == null || homeName.isBlank()) ? "home" : homeName.trim().toLowerCase(Locale.ROOT);
        String compoundKey = playerKey.trim().toLowerCase(Locale.ROOT) + "." + safeHomeName;
        String val = String.format(Locale.ROOT, "%s:%.2f:%.2f:%.2f:%.2f:%.2f",
                world != null ? world : "minecraft:overworld", x, y, z, yaw, pitch);
        inMemoryHomes.put(compoundKey, val);
        persistToDisk();
    }

    /**
     * Returns the serialized coordinate string for a player's named home, or {@code null} if not found.
     *
     * @param playerKey player name or UUID string
     * @param homeName  name of the home
     * @return serialized location string (format: {@code "world:x:y:z:yaw:pitch"}), or null
     */
    public String getHome(String playerKey, String homeName) {
        if (playerKey == null) {
            return null;
        }
        String safeHomeName = (homeName == null || homeName.isBlank()) ? "home" : homeName.trim().toLowerCase(Locale.ROOT);
        String compoundKey = playerKey.trim().toLowerCase(Locale.ROOT) + "." + safeHomeName;
        return inMemoryHomes.get(compoundKey);
    }

    /**
     * Deletes a named home for a player.
     *
     * @param playerKey player name or UUID string
     * @param homeName  name of the home
     * @return {@code true} if a home was removed
     */
    public boolean deleteHome(String playerKey, String homeName) {
        if (playerKey == null) {
            return false;
        }
        String safeHomeName = (homeName == null || homeName.isBlank()) ? "home" : homeName.trim().toLowerCase(Locale.ROOT);
        String compoundKey = playerKey.trim().toLowerCase(Locale.ROOT) + "." + safeHomeName;
        boolean removed = inMemoryHomes.remove(compoundKey) != null;
        if (removed) {
            persistToDisk();
        }
        return removed;
    }

    /**
     * Returns a list of all home names saved for the given player.
     *
     * @param playerKey player name or UUID string
     * @return unmodifiable list of home names
     */
    public List<String> listHomes(String playerKey) {
        if (playerKey == null) {
            return List.of();
        }
        String prefix = playerKey.trim().toLowerCase(Locale.ROOT) + ".";
        List<String> result = new ArrayList<>();
        for (String k : inMemoryHomes.keySet()) {
            if (k.startsWith(prefix)) {
                result.add(k.substring(prefix.length()));
            }
        }
        Collections.sort(result);
        return Collections.unmodifiableList(result);
    }

    private synchronized void persistToDisk() {
        if (activeConfigDir == null) {
            return;
        }
        try {
            Path homeFile = activeConfigDir.resolve(STORE_FILE_NAME);
            StringBuilder sb = new StringBuilder(1024);
            sb.append("# VESSEL Home Storage\n");
            for (Map.Entry<String, String> entry : inMemoryHomes.entrySet()) {
                sb.append(entry.getKey()).append('=').append(entry.getValue()).append('\n');
            }
            Files.writeString(homeFile, sb.toString(), StandardCharsets.UTF_8);
        } catch (IOException ignored) {
        }
    }
}
