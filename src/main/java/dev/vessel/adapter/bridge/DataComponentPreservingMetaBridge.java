package dev.vessel.adapter.bridge;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Preserves 1.20.5+ modded Data Components (e.g., Mekanism energy, Create schematic data,
 * Apotheosis sockets) when a Bukkit plugin mutates an item's {@code ItemMeta} display name
 * or lore.
 *
 * @version 1.0.0
 */
public final class DataComponentPreservingMetaBridge {

    private static final String CUSTOM_NAME_COMPONENT = "minecraft:custom_name";
    private static final String LORE_COMPONENT = "minecraft:lore";

    /**
     * Creates a new Data Component preserving ItemMeta bridge.
     */
    public DataComponentPreservingMetaBridge() {
    }

    /**
     * Applies updated Bukkit display name and lore onto a copy of the original item Data
     * Component map while preserving 100% of custom modded components.
     *
     * @param originalComponents the item's existing Data Components map
     * @param newDisplayName     updated display name from Bukkit {@code ItemMeta} (may be null)
     * @param newLore            updated lore lines from Bukkit {@code ItemMeta} (may be null)
     * @return unmodifiable merged Data Components map
     */
    public Map<String, Object> applyBukkitMetaPreservingModComponents(
            Map<String, Object> originalComponents,
            String newDisplayName,
            List<String> newLore) {
        Objects.requireNonNull(originalComponents, "originalComponents must not be null");
        Map<String, Object> merged = new LinkedHashMap<>(originalComponents);
        if (newDisplayName != null && !newDisplayName.isEmpty()) {
            merged.put(CUSTOM_NAME_COMPONENT, newDisplayName);
        }
        if (newLore != null) {
            merged.put(LORE_COMPONENT, Collections.unmodifiableList(new ArrayList<>(newLore)));
        }
        return Collections.unmodifiableMap(merged);
    }
}
