package dev.vessel.adapter.bridge;

import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Maps custom modded entity types (Alex's Mobs, Cobblemon, Ice &amp; Fire, Create)
 * into synthetic Bukkit {@code EntityType} identifiers so Bukkit plugins can classify
 * living/monster entities without throwing {@code IllegalArgumentException}.
 *
 * @version 1.0.0
 */
public final class UniversalEntityTypeBridge {

    private final Map<String, String> mappedBukkitTypes = new ConcurrentHashMap<>();

    /**
     * Creates a new universal entity type bridge.
     */
    public UniversalEntityTypeBridge() {
    }

    /**
     * Maps a modded entity namespaced key into a canonical uppercase Bukkit entity type
     * identifier and records the mapping.
     *
     * @param moddedEntityKey namespaced modded entity key (e.g., {@code "alexsmobs:grizzly_bear"})
     * @param isLiving        whether the modded entity extends {@code LivingEntity}
     * @param isMonster       whether the modded entity is hostile
     * @return mapped Bukkit entity type string
     */
    public String mapModdedEntityToBukkitType(
            String moddedEntityKey, boolean isLiving, boolean isMonster) {
        Objects.requireNonNull(moddedEntityKey, "moddedEntityKey must not be null");
        String normalizedKey = moddedEntityKey.trim().toLowerCase(Locale.ROOT);
        if (normalizedKey.isEmpty()) {
            throw new IllegalArgumentException("moddedEntityKey must not be empty");
        }
        String categorySuffix = isMonster ? "MONSTER" : (isLiving ? "CREATURE" : "MISC");
        String sanitized = normalizedKey.replace(':', '_').replace('-', '_').replace('/', '_')
                .toUpperCase(Locale.ROOT);
        String bukkitType = "MOD_" + categorySuffix + "_" + sanitized;
        mappedBukkitTypes.put(normalizedKey, bukkitType);
        return bukkitType;
    }

    /**
     * Checks whether the given modded entity key has been registered with this bridge.
     *
     * @param moddedEntityKey namespaced modded entity key
     * @return {@code true} if the entity key is known
     */
    public boolean isKnownModdedEntity(String moddedEntityKey) {
        if (moddedEntityKey == null || moddedEntityKey.isBlank()) {
            return false;
        }
        return mappedBukkitTypes.containsKey(moddedEntityKey.trim().toLowerCase(Locale.ROOT));
    }
}
