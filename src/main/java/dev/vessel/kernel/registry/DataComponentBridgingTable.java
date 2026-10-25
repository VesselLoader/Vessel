package dev.vessel.kernel.registry;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * High-performance bidirectional bridging table between Minecraft 1.20.5+ Data Components
 * and legacy ItemStack NBT compound structures.
 * <p>
 * Minecraft 1.20.5 deprecated arbitrary root NBT compounds on items in favor of strictly typed
 * Data Components ({@code minecraft:damage}, {@code minecraft:custom_name}, {@code minecraft:enchantments}, etc.).
 * This bridging table ensures backward compatibility with legacy plugins and mods (1.7.10 - 1.20.4)
 * by performing bidirectional translation, with unmapped custom tags isolated into {@code minecraft:custom_data}
 * and legacy numeric enchantment IDs normalized into canonical namespaced identifiers.
 *
 * @version 1.0.1
 * @author VESSEL Engine Team
 */
public final class DataComponentBridgingTable {

    // Standard Component Identifiers
    public static final String COMPONENT_DAMAGE = "minecraft:damage";
    public static final String COMPONENT_MAX_DAMAGE = "minecraft:max_damage";
    public static final String COMPONENT_CUSTOM_NAME = "minecraft:custom_name";
    public static final String COMPONENT_LORE = "minecraft:lore";
    public static final String COMPONENT_ENCHANTMENTS = "minecraft:enchantments";
    public static final String COMPONENT_STORED_ENCHANTMENTS = "minecraft:stored_enchantments";
    public static final String COMPONENT_UNBREAKABLE = "minecraft:unbreakable";
    public static final String COMPONENT_CUSTOM_MODEL_DATA = "minecraft:custom_model_data";
    public static final String COMPONENT_REPAIR_COST = "minecraft:repair_cost";
    public static final String COMPONENT_ATTRIBUTE_MODIFIERS = "minecraft:attribute_modifiers";
    public static final String COMPONENT_CAN_PLACE_ON = "minecraft:can_place_on";
    public static final String COMPONENT_CAN_BREAK = "minecraft:can_break";
    public static final String COMPONENT_HIDE_FLAGS = "minecraft:hide_flags";
    public static final String COMPONENT_DYED_COLOR = "minecraft:dyed_color";
    public static final String COMPONENT_CUSTOM_DATA = "minecraft:custom_data";

    // Legacy NBT Keys
    public static final String NBT_DAMAGE = "Damage";
    public static final String NBT_MAX_DAMAGE = "MaxDamage";
    public static final String NBT_DISPLAY = "display";
    public static final String NBT_DISPLAY_NAME = "Name";
    public static final String NBT_DISPLAY_LORE = "Lore";
    public static final String NBT_DISPLAY_COLOR = "color";
    public static final String NBT_ENCHANTMENTS = "Enchantments";
    public static final String NBT_STORED_ENCHANTMENTS = "StoredEnchantments";
    public static final String NBT_UNBREAKABLE = "Unbreakable";
    public static final String NBT_CUSTOM_MODEL_DATA = "CustomModelData";
    public static final String NBT_REPAIR_COST = "RepairCost";
    public static final String NBT_ATTRIBUTE_MODIFIERS = "AttributeModifiers";
    public static final String NBT_CAN_PLACE_ON = "CanPlaceOn";
    public static final String NBT_CAN_DESTROY = "CanDestroy";
    public static final String NBT_HIDE_FLAGS = "HideFlags";

    private static final Map<Integer, String> LEGACY_ENCHANTMENT_IDS = new HashMap<>();

    static {
        LEGACY_ENCHANTMENT_IDS.put(0, "minecraft:protection");
        LEGACY_ENCHANTMENT_IDS.put(1, "minecraft:fire_protection");
        LEGACY_ENCHANTMENT_IDS.put(2, "minecraft:feather_falling");
        LEGACY_ENCHANTMENT_IDS.put(3, "minecraft:blast_protection");
        LEGACY_ENCHANTMENT_IDS.put(4, "minecraft:projectile_protection");
        LEGACY_ENCHANTMENT_IDS.put(5, "minecraft:respiration");
        LEGACY_ENCHANTMENT_IDS.put(6, "minecraft:aqua_affinity");
        LEGACY_ENCHANTMENT_IDS.put(7, "minecraft:thorns");
        LEGACY_ENCHANTMENT_IDS.put(8, "minecraft:depth_strider");
        LEGACY_ENCHANTMENT_IDS.put(9, "minecraft:frost_walker");
        LEGACY_ENCHANTMENT_IDS.put(10, "minecraft:binding_curse");
        LEGACY_ENCHANTMENT_IDS.put(16, "minecraft:sharpness");
        LEGACY_ENCHANTMENT_IDS.put(17, "minecraft:smite");
        LEGACY_ENCHANTMENT_IDS.put(18, "minecraft:bane_of_arthropods");
        LEGACY_ENCHANTMENT_IDS.put(19, "minecraft:knockback");
        LEGACY_ENCHANTMENT_IDS.put(20, "minecraft:fire_aspect");
        LEGACY_ENCHANTMENT_IDS.put(21, "minecraft:looting");
        LEGACY_ENCHANTMENT_IDS.put(22, "minecraft:sweeping");
        LEGACY_ENCHANTMENT_IDS.put(32, "minecraft:efficiency");
        LEGACY_ENCHANTMENT_IDS.put(33, "minecraft:silk_touch");
        LEGACY_ENCHANTMENT_IDS.put(34, "minecraft:unbreaking");
        LEGACY_ENCHANTMENT_IDS.put(35, "minecraft:fortune");
        LEGACY_ENCHANTMENT_IDS.put(48, "minecraft:power");
        LEGACY_ENCHANTMENT_IDS.put(49, "minecraft:punch");
        LEGACY_ENCHANTMENT_IDS.put(50, "minecraft:flame");
        LEGACY_ENCHANTMENT_IDS.put(51, "minecraft:infinity");
        LEGACY_ENCHANTMENT_IDS.put(61, "minecraft:luck_of_the_sea");
        LEGACY_ENCHANTMENT_IDS.put(62, "minecraft:lure");
        LEGACY_ENCHANTMENT_IDS.put(70, "minecraft:mending");
        LEGACY_ENCHANTMENT_IDS.put(71, "minecraft:vanishing_curse");
    }

    private final Map<String, String> componentToLegacy;
    private final Map<String, String> legacyToComponent;

    /**
     * Constructs a new bridging table with standard 1.20.5+ mappings.
     */
    public DataComponentBridgingTable() {
        this.componentToLegacy = new ConcurrentHashMap<>();
        this.legacyToComponent = new ConcurrentHashMap<>();

        registerDefaultMappings();
    }

    private void registerDefaultMappings() {
        registerMapping(COMPONENT_DAMAGE, NBT_DAMAGE);
        registerMapping(COMPONENT_MAX_DAMAGE, NBT_MAX_DAMAGE);
        registerMapping(COMPONENT_CUSTOM_MODEL_DATA, NBT_CUSTOM_MODEL_DATA);
        registerMapping(COMPONENT_REPAIR_COST, NBT_REPAIR_COST);
        registerMapping(COMPONENT_UNBREAKABLE, NBT_UNBREAKABLE);
        registerMapping(COMPONENT_HIDE_FLAGS, NBT_HIDE_FLAGS);
        registerMapping(COMPONENT_CAN_PLACE_ON, NBT_CAN_PLACE_ON);
        registerMapping(COMPONENT_CAN_BREAK, NBT_CAN_DESTROY);
    }

    /**
     * Registers a custom bidirectional mapping between a component type and a legacy NBT key.
     *
     * @param componentType component identifier (e.g., "minecraft:custom_model_data")
     * @param legacyPath    legacy root NBT key (e.g., "CustomModelData")
     */
    public void registerMapping(String componentType, String legacyPath) {
        Objects.requireNonNull(componentType, "componentType cannot be null");
        Objects.requireNonNull(legacyPath, "legacyPath cannot be null");

        componentToLegacy.put(componentType, legacyPath);
        legacyToComponent.put(legacyPath, componentType);
    }

    /**
     * Translates a modern Data Component map into a legacy NBT compound map.
     *
     * @param components modern 1.20.5+ data components
     * @return legacy NBT compound representation
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> toLegacyNbt(Map<String, Object> components) {
        if (components == null || components.isEmpty()) {
            return new HashMap<>();
        }

        Map<String, Object> legacyNbt = new HashMap<>();

        // 1. Copy unmapped custom data if present
        Map<String, Object> preservedDisplay = null;
        Object customDataObj = components.get(COMPONENT_CUSTOM_DATA);
        if (customDataObj instanceof Map<?, ?> customMap) {
            for (Map.Entry<?, ?> entry : customMap.entrySet()) {
                if (entry.getKey() != null) {
                    String k = entry.getKey().toString();
                    if (NBT_DISPLAY.equals(k) && entry.getValue() instanceof Map<?, ?> disp) {
                        preservedDisplay = new HashMap<>((Map<String, Object>) disp);
                    } else {
                        legacyNbt.put(k, entry.getValue());
                    }
                }
            }
        }

        // 2. Direct simple mappings
        for (Map.Entry<String, String> entry : componentToLegacy.entrySet()) {
            String compKey = entry.getKey();
            String legacyKey = entry.getValue();

            if (components.containsKey(compKey)) {
                Object val = components.get(compKey);
                if (COMPONENT_UNBREAKABLE.equals(compKey) && val instanceof Boolean b) {
                    legacyNbt.put(legacyKey, (byte) (b ? 1 : 0));
                } else {
                    legacyNbt.put(legacyKey, val);
                }
            }
        }

        // 3. Display compound: Custom Name, Lore, and Dyed Color
        boolean hasName = components.containsKey(COMPONENT_CUSTOM_NAME);
        boolean hasLore = components.containsKey(COMPONENT_LORE);
        boolean hasColor = components.containsKey(COMPONENT_DYED_COLOR);

        if (hasName || hasLore || hasColor || preservedDisplay != null) {
            Map<String, Object> display = (preservedDisplay != null) ? preservedDisplay : new HashMap<>();
            if (hasName) {
                display.put(NBT_DISPLAY_NAME, components.get(COMPONENT_CUSTOM_NAME));
            }
            if (hasLore) {
                display.put(NBT_DISPLAY_LORE, components.get(COMPONENT_LORE));
            }
            if (hasColor) {
                display.put(NBT_DISPLAY_COLOR, components.get(COMPONENT_DYED_COLOR));
            }
            legacyNbt.put(NBT_DISPLAY, display);
        }

        // 4. Enchantments map -> legacy list of {id, lvl}
        if (components.containsKey(COMPONENT_ENCHANTMENTS)) {
            Object enchObj = components.get(COMPONENT_ENCHANTMENTS);
            legacyNbt.put(NBT_ENCHANTMENTS, convertEnchantmentsToLegacy(enchObj));
        }

        // 5. Stored Enchantments map -> legacy list of {id, lvl}
        if (components.containsKey(COMPONENT_STORED_ENCHANTMENTS)) {
            Object enchObj = components.get(COMPONENT_STORED_ENCHANTMENTS);
            legacyNbt.put(NBT_STORED_ENCHANTMENTS, convertEnchantmentsToLegacy(enchObj));
        }

        // 6. Attribute modifiers
        if (components.containsKey(COMPONENT_ATTRIBUTE_MODIFIERS)) {
            legacyNbt.put(NBT_ATTRIBUTE_MODIFIERS, components.get(COMPONENT_ATTRIBUTE_MODIFIERS));
        }

        return legacyNbt;
    }

    /**
     * Translates a legacy NBT compound map into a modern 1.20.5+ Data Component map.
     *
     * @param legacyNbt legacy NBT compound
     * @return modern Data Component map
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> toDataComponents(Map<String, Object> legacyNbt) {
        if (legacyNbt == null || legacyNbt.isEmpty()) {
            return new HashMap<>();
        }

        Map<String, Object> components = new HashMap<>();
        Map<String, Object> customData = new HashMap<>();

        for (Map.Entry<String, Object> entry : legacyNbt.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();

            // Known direct mapping
            String compKey = legacyToComponent.get(key);
            if (compKey != null) {
                if (COMPONENT_UNBREAKABLE.equals(compKey)) {
                    if (value instanceof Number num) {
                        components.put(compKey, num.intValue() != 0);
                    } else if (value instanceof Boolean b) {
                        components.put(compKey, b);
                    }
                } else {
                    components.put(compKey, value);
                }
                continue;
            }

            // Display compound
            if (NBT_DISPLAY.equals(key) && value instanceof Map<?, ?> displayMap) {
                Map<String, Object> remainingDisplay = new HashMap<>();
                for (Map.Entry<?, ?> dispEntry : displayMap.entrySet()) {
                    if (dispEntry.getKey() == null) continue;
                    String dispKey = dispEntry.getKey().toString();
                    Object dispVal = dispEntry.getValue();

                    if (NBT_DISPLAY_NAME.equals(dispKey)) {
                        components.put(COMPONENT_CUSTOM_NAME, dispVal);
                    } else if (NBT_DISPLAY_LORE.equals(dispKey)) {
                        components.put(COMPONENT_LORE, dispVal);
                    } else if (NBT_DISPLAY_COLOR.equals(dispKey)) {
                        components.put(COMPONENT_DYED_COLOR, dispVal);
                    } else {
                        remainingDisplay.put(dispKey, dispVal);
                    }
                }
                if (!remainingDisplay.isEmpty()) {
                    customData.put(NBT_DISPLAY, remainingDisplay);
                }
                continue;
            }

            // Enchantments list -> map
            if (NBT_ENCHANTMENTS.equals(key) && value instanceof List<?> enchList) {
                components.put(COMPONENT_ENCHANTMENTS, convertLegacyToEnchantments(enchList));
                continue;
            }

            // Stored Enchantments list -> map
            if (NBT_STORED_ENCHANTMENTS.equals(key) && value instanceof List<?> enchList) {
                components.put(COMPONENT_STORED_ENCHANTMENTS, convertLegacyToEnchantments(enchList));
                continue;
            }

            // Attribute Modifiers
            if (NBT_ATTRIBUTE_MODIFIERS.equals(key)) {
                components.put(COMPONENT_ATTRIBUTE_MODIFIERS, value);
                continue;
            }

            // All other non-standard legacy keys go into custom_data
            customData.put(key, value);
        }

        if (!customData.isEmpty()) {
            components.put(COMPONENT_CUSTOM_DATA, customData);
        }

        return components;
    }

    private List<Map<String, Object>> convertEnchantmentsToLegacy(Object enchObj) {
        List<Map<String, Object>> list = new ArrayList<>();
        if (enchObj instanceof Map<?, ?> map) {
            for (Map.Entry<?, ?> e : map.entrySet()) {
                Map<String, Object> entry = new HashMap<>();
                entry.put("id", e.getKey().toString());
                entry.put("lvl", (e.getValue() instanceof Number n) ? n.intValue() : 1);
                list.add(entry);
            }
        }
        return list;
    }

    private Map<String, Integer> convertLegacyToEnchantments(List<?> enchList) {
        Map<String, Integer> map = new LinkedHashMap<>();
        for (Object item : enchList) {
            if (item instanceof Map<?, ?> m) {
                Object idObj = m.get("id");
                Object lvl = m.get("lvl");
                if (idObj != null) {
                    String idStr = idObj.toString();
                    // Normalize numeric legacy enchantment IDs (e.g. 16 -> minecraft:sharpness)
                    if (idObj instanceof Number num) {
                        String mapped = LEGACY_ENCHANTMENT_IDS.get(num.intValue());
                        if (mapped != null) {
                            idStr = mapped;
                        }
                    } else if (idStr.matches("^\\d+$")) {
                        try {
                            int numericId = Integer.parseInt(idStr);
                            String mapped = LEGACY_ENCHANTMENT_IDS.get(numericId);
                            if (mapped != null) {
                                idStr = mapped;
                            }
                        } catch (NumberFormatException ignored) {
                        }
                    }

                    int level = (lvl instanceof Number n) ? n.intValue() : 1;
                    map.put(idStr, level);
                }
            }
        }
        return map;
    }

    // Fast-path primitive accessors
    public int getDamage(Map<String, Object> components) {
        if (components == null) return 0;
        Object val = components.get(COMPONENT_DAMAGE);
        return (val instanceof Number n) ? n.intValue() : 0;
    }

    public void setDamage(Map<String, Object> components, int damage) {
        Objects.requireNonNull(components, "components cannot be null");
        components.put(COMPONENT_DAMAGE, damage);
    }

    public boolean isUnbreakable(Map<String, Object> components) {
        if (components == null) return false;
        Object val = components.get(COMPONENT_UNBREAKABLE);
        return Boolean.TRUE.equals(val);
    }

    public void setUnbreakable(Map<String, Object> components, boolean unbreakable) {
        Objects.requireNonNull(components, "components cannot be null");
        components.put(COMPONENT_UNBREAKABLE, unbreakable);
    }

    public int getCustomModelData(Map<String, Object> components) {
        if (components == null) return 0;
        Object val = components.get(COMPONENT_CUSTOM_MODEL_DATA);
        return (val instanceof Number n) ? n.intValue() : 0;
    }

    public void setCustomModelData(Map<String, Object> components, int customModelData) {
        Objects.requireNonNull(components, "components cannot be null");
        components.put(COMPONENT_CUSTOM_MODEL_DATA, customModelData);
    }

    public String getCustomName(Map<String, Object> components) {
        if (components == null) return null;
        Object val = components.get(COMPONENT_CUSTOM_NAME);
        return (val != null) ? val.toString() : null;
    }

    public void setCustomName(Map<String, Object> components, String customName) {
        Objects.requireNonNull(components, "components cannot be null");
        if (customName == null) {
            components.remove(COMPONENT_CUSTOM_NAME);
        } else {
            components.put(COMPONENT_CUSTOM_NAME, customName);
        }
    }
}
