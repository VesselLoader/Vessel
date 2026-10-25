package dev.vessel.kernel.migration;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Shield filter protecting modded inventory capability NBT tags and third-party data trees
 * from being stripped during cross-platform migration or server format translation.
 * <p>
 * Mitigates: <b>ERR-92</b>, <b>OPS-0001..0200</b>.
 * </p>
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public final class ModdedInventoryNbtShield {

    private static final Set<String> DEFAULT_PROTECTED_TAG_PREFIXES;
    private static final Set<String> DEFAULT_PROTECTED_TAG_NAMES;

    static {
        Set<String> prefixes = new HashSet<>();
        prefixes.add("forge:");
        prefixes.add("curios:");
        prefixes.add("cardinal_components");
        prefixes.add("neoforge:");
        prefixes.add("fabric:");
        prefixes.add("vessel:");
        prefixes.add("ae2:");
        prefixes.add("mekanism:");
        DEFAULT_PROTECTED_TAG_PREFIXES = Collections.unmodifiableSet(prefixes);

        Set<String> names = new HashSet<>();
        names.add("ForgeCaps");
        names.add("cardinal_components");
        names.add("Curios");
        names.add("CuriosInventory");
        names.add("Accessories");
        names.add("EnderStorage");
        names.add("Baubles");
        names.add("TechReborn");
        names.add("IronChests");
        names.add("vessel_custom_data");
        DEFAULT_PROTECTED_TAG_NAMES = Collections.unmodifiableSet(names);
    }

    /**
     * Determines whether a given NBT tag key represents a protected modded inventory or capability container.
     *
     * @param tagKey the NBT compound key name
     * @return true if the tag must be preserved intact
     */
    public boolean isProtectedTag(String tagKey) {
        if (tagKey == null || tagKey.trim().isEmpty()) {
            return false;
        }
        String cleanKey = tagKey.trim();

        if (DEFAULT_PROTECTED_TAG_NAMES.contains(cleanKey)) {
            return true;
        }

        String lowerKey = cleanKey.toLowerCase();
        for (String prefix : DEFAULT_PROTECTED_TAG_PREFIXES) {
            if (lowerKey.startsWith(prefix)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Extracts all protected modded tags from a raw NBT tag map.
     *
     * @param sourceTags complete map of NBT tags
     * @return map containing only protected modded capability entries
     */
    public Map<String, byte[]> extractProtectedTags(Map<String, byte[]> sourceTags) {
        if (sourceTags == null || sourceTags.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<String, byte[]> result = new java.util.HashMap<>();
        for (Map.Entry<String, byte[]> entry : sourceTags.entrySet()) {
            if (isProtectedTag(entry.getKey())) {
                result.put(entry.getKey(), entry.getValue() != null ? entry.getValue().clone() : new byte[0]);
            }
        }
        return Collections.unmodifiableMap(result);
    }
}
