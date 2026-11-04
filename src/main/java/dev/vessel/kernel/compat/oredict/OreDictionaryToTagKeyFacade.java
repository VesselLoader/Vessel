package dev.vessel.kernel.compat.oredict;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Facade bridging legacy Forge 1.12 OreDictionary registrations and queries
 * to modern Minecraft TagKey and BuiltInRegistries.
 * Adheres strictly to Rule 17 (Single Responsibility Principle).
 *
 * @version 1.0.0
 */
public final class OreDictionaryToTagKeyFacade {

    private final ConcurrentMap<String, List<Object>> oreEntries = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Integer> oreIds = new ConcurrentHashMap<>();
    private int nextOreId = 1;

    public void registerOre(String oreName, Object itemStackOrItem) {
        Objects.requireNonNull(oreName, "oreName cannot be null");
        Objects.requireNonNull(itemStackOrItem, "itemStackOrItem cannot be null");
        oreEntries.computeIfAbsent(oreName, k -> new CopyOnWriteArrayList<>()).add(itemStackOrItem);
        oreIds.computeIfAbsent(oreName, k -> nextOreId++);
    }

    public List<Object> getOres(String oreName) {
        if (oreName == null) {
            return Collections.emptyList();
        }
        List<Object> list = oreEntries.get(oreName);
        return list != null ? Collections.unmodifiableList(list) : Collections.emptyList();
    }

    public int getOreId(String oreName) {
        if (oreName == null) {
            return -1;
        }
        return oreIds.getOrDefault(oreName, -1);
    }

    public String getOreName(int oreId) {
        for (var entry : oreIds.entrySet()) {
            if (entry.getValue() == oreId) {
                return entry.getKey();
            }
        }
        return "Unknown";
    }

    public boolean doesOreNameExist(String oreName) {
        return oreName != null && oreEntries.containsKey(oreName);
    }

    public String mapOreNameToModernTag(String legacyOreName) {
        if (legacyOreName == null || legacyOreName.isBlank()) {
            return "c:items";
        }
        // Transform e.g. "oreIron" -> "c:ores/iron", "ingotCopper" -> "c:ingots/copper"
        if (legacyOreName.startsWith("ore")) {
            return "c:ores/" + legacyOreName.substring(3).toLowerCase();
        } else if (legacyOreName.startsWith("ingot")) {
            return "c:ingots/" + legacyOreName.substring(5).toLowerCase();
        } else if (legacyOreName.startsWith("dust")) {
            return "c:dusts/" + legacyOreName.substring(4).toLowerCase();
        } else if (legacyOreName.startsWith("gem")) {
            return "c:gems/" + legacyOreName.substring(3).toLowerCase();
        }
        return "c:" + legacyOreName.toLowerCase();
    }
}
