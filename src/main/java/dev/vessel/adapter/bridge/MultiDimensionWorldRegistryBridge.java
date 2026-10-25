package dev.vessel.adapter.bridge;

import dev.vessel.kernel.migration.WorldDimensionType;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Bridges custom modded dimensions (Twilight Forest, The Aether, Ad Astra, Deeper and Darker)
 * into {@code Bukkit.getWorlds()} with deterministic {@link WorldDimensionType} environments.
 *
 * @version 1.0.0
 */
public final class MultiDimensionWorldRegistryBridge {

    private final List<String> orderedWorlds = new CopyOnWriteArrayList<>();
    private final Map<String, WorldDimensionType> dimensionEnvironments = new ConcurrentHashMap<>();

    /**
     * Creates a new multi-dimension world registry bridge pre-populated with Vanilla dimensions.
     */
    public MultiDimensionWorldRegistryBridge() {
        registerCustomModDimension("minecraft:overworld", WorldDimensionType.OVERWORLD);
        registerCustomModDimension("minecraft:the_nether", WorldDimensionType.THE_NETHER);
        registerCustomModDimension("minecraft:the_end", WorldDimensionType.THE_END);
    }

    /**
     * Registers a custom modded dimension and maps it to a Bukkit-compatible world environment.
     *
     * @param dimensionKey    namespaced dimension identifier (e.g., {@code "twilightforest:twilight_forest"})
     * @param environmentType canonical dimension environment classification
     */
    public void registerCustomModDimension(
            String dimensionKey, WorldDimensionType environmentType) {
        Objects.requireNonNull(dimensionKey, "dimensionKey must not be null");
        Objects.requireNonNull(environmentType, "environmentType must not be null");
        String normalized = dimensionKey.trim().toLowerCase(Locale.ROOT);
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("dimensionKey must not be empty");
        }
        dimensionEnvironments.put(normalized, environmentType);
        if (!orderedWorlds.contains(normalized)) {
            orderedWorlds.add(normalized);
        }
    }

    /**
     * Returns the immutable list of all worlds exposed to {@code Bukkit.getWorlds()}.
     *
     * @return list of registered dimension keys
     */
    public List<String> getBukkitRegisteredWorlds() {
        return Collections.unmodifiableList(new ArrayList<>(orderedWorlds));
    }

    /**
     * Resolves the {@link WorldDimensionType} environment for a given dimension key.
     *
     * @param dimensionKey namespaced dimension key
     * @return the mapped {@link WorldDimensionType}, or {@link WorldDimensionType#CUSTOM} if unknown
     */
    public WorldDimensionType getDimensionEnvironment(String dimensionKey) {
        if (dimensionKey == null || dimensionKey.isBlank()) {
            return WorldDimensionType.CUSTOM;
        }
        return dimensionEnvironments.getOrDefault(
                dimensionKey.trim().toLowerCase(Locale.ROOT),
                WorldDimensionType.CUSTOM);
    }
}
