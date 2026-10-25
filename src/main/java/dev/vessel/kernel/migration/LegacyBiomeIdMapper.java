package dev.vessel.kernel.migration;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Deterministic mapper translating legacy integer biome IDs (Minecraft Anvil format)
 * to modern namespaced resource locations without memory allocation overhead.
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public final class LegacyBiomeIdMapper {

    /** Default fallback modern biome ID when an unknown legacy ID is encountered. */
    public static final String DEFAULT_FALLBACK_BIOME = "minecraft:plains";

    /** Maximum standard legacy biome index (0 to 255). */
    public static final int MAX_LEGACY_BIOME_ID = 255;

    /** Minimum standard legacy biome index. */
    public static final int MIN_LEGACY_BIOME_ID = 0;

    private static final Map<Integer, String> LEGACY_TO_NAMESPACED_MAP;

    static {
        Map<Integer, String> map = new HashMap<>();
        map.put(0, "minecraft:ocean");
        map.put(1, "minecraft:plains");
        map.put(2, "minecraft:desert");
        map.put(3, "minecraft:mountains");
        map.put(4, "minecraft:forest");
        map.put(5, "minecraft:taiga");
        map.put(6, "minecraft:swamp");
        map.put(7, "minecraft:river");
        map.put(8, "minecraft:nether_wastes");
        map.put(9, "minecraft:the_end");
        map.put(10, "minecraft:frozen_ocean");
        map.put(11, "minecraft:frozen_river");
        map.put(12, "minecraft:snowy_tundra");
        map.put(13, "minecraft:snowy_mountains");
        map.put(14, "minecraft:mushroom_fields");
        map.put(15, "minecraft:mushroom_field_shore");
        map.put(16, "minecraft:beach");
        map.put(17, "minecraft:desert_hills");
        map.put(18, "minecraft:wooded_hills");
        map.put(19, "minecraft:taiga_hills");
        map.put(20, "minecraft:mountain_edge");
        map.put(21, "minecraft:jungle");
        map.put(22, "minecraft:jungle_hills");
        map.put(23, "minecraft:jungle_edge");
        map.put(24, "minecraft:deep_ocean");
        map.put(25, "minecraft:stone_shore");
        map.put(26, "minecraft:snowy_beach");
        map.put(27, "minecraft:birch_forest");
        map.put(28, "minecraft:birch_forest_hills");
        map.put(29, "minecraft:dark_forest");
        map.put(30, "minecraft:snowy_taiga");
        map.put(31, "minecraft:snowy_taiga_hills");
        map.put(32, "minecraft:giant_tree_taiga");
        map.put(33, "minecraft:giant_tree_taiga_hills");
        map.put(34, "minecraft:wooded_mountains");
        map.put(35, "minecraft:savanna");
        map.put(36, "minecraft:savanna_plateau");
        map.put(37, "minecraft:badlands");
        map.put(38, "minecraft:wooded_badlands_plateau");
        map.put(39, "minecraft:badlands_plateau");
        map.put(127, "minecraft:the_void");
        map.put(129, "minecraft:sunflower_plains");
        map.put(130, "minecraft:desert_lakes");
        map.put(131, "minecraft:gravelly_mountains");
        map.put(132, "minecraft:flower_forest");
        map.put(133, "minecraft:taiga_mountains");
        map.put(134, "minecraft:swamp_hills");
        map.put(140, "minecraft:ice_spikes");
        map.put(149, "minecraft:modified_jungle");
        map.put(151, "minecraft:modified_jungle_edge");
        map.put(155, "minecraft:tall_birch_forest");
        map.put(156, "minecraft:tall_birch_hills");
        map.put(157, "minecraft:dark_forest_hills");
        map.put(158, "minecraft:snowy_taiga_mountains");
        map.put(160, "minecraft:giant_spruce_taiga");
        map.put(161, "minecraft:giant_spruce_taiga_hills");
        map.put(162, "minecraft:modified_gravelly_mountains");
        map.put(163, "minecraft:shattered_savanna");
        map.put(164, "minecraft:shattered_savanna_plateau");
        map.put(165, "minecraft:eroded_badlands");
        map.put(166, "minecraft:modified_wooded_badlands_plateau");
        map.put(167, "minecraft:modified_badlands_plateau");
        map.put(168, "minecraft:bamboo_jungle");
        map.put(169, "minecraft:bamboo_jungle_hills");
        map.put(170, "minecraft:soul_sand_valley");
        map.put(171, "minecraft:crimson_forest");
        map.put(172, "minecraft:warped_forest");
        map.put(173, "minecraft:basalt_deltas");
        LEGACY_TO_NAMESPACED_MAP = Collections.unmodifiableMap(map);
    }

    /**
     * Resolves a legacy raw byte or integer ID to its modern namespaced equivalent.
     *
     * @param legacyId the raw biome ID (0-255)
     * @return the canonical modern namespaced ID
     */
    public String resolveModernBiome(int legacyId) {
        int unsignedId = legacyId & 0xFF;
        String mapped = LEGACY_TO_NAMESPACED_MAP.get(unsignedId);
        if (mapped != null) {
            return mapped;
        }
        return DEFAULT_FALLBACK_BIOME;
    }

    /**
     * Checks if a legacy biome ID is recognized in the standard registry.
     *
     * @param legacyId the raw biome ID
     * @return true if the ID is known
     */
    public boolean isKnownLegacyId(int legacyId) {
        int unsignedId = legacyId & 0xFF;
        return LEGACY_TO_NAMESPACED_MAP.containsKey(unsignedId);
    }
}
