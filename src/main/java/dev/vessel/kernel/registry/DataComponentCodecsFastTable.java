package dev.vessel.kernel.registry;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Direct O(1) indexed lookup table for Minecraft 1.20.5+ Data Components.
 * <p>
 * Eliminates ERR-15 and INV-0001..0400 (excessive NBT serialization overhead,
 * GC pressure during inventory streaming and item transfer).
 * </p>
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public final class DataComponentCodecsFastTable {

    public static final int TABLE_CAPACITY = 256;
    public static final int UNKNOWN_COMPONENT_ID = -1;

    // Standard Minecraft 1.20.5+ Data Component IDs
    public static final int ID_CUSTOM_DATA = 0;
    public static final int ID_MAX_STACK_SIZE = 1;
    public static final int ID_MAX_DAMAGE = 2;
    public static final int ID_DAMAGE = 3;
    public static final int ID_UNBREAKABLE = 4;
    public static final int ID_CUSTOM_NAME = 5;
    public static final int ID_ITEM_NAME = 6;
    public static final int ID_LORE = 7;
    public static final int ID_RARITY = 8;
    public static final int ID_ENCHANTMENTS = 9;
    public static final int ID_CAN_PLACE_ON = 10;
    public static final int ID_CAN_BREAK = 11;
    public static final int ID_ATTRIBUTE_MODIFIERS = 12;
    public static final int ID_CUSTOM_MODEL_DATA = 13;
    public static final int ID_HIDE_ADDITIONAL_TOOLTIP = 14;
    public static final int ID_REPAIR_COST = 15;
    public static final int ID_CREATIVE_SLOT_LOCK = 16;
    public static final int ID_ENCHANTMENT_GLINT_OVERRIDE = 17;
    public static final int ID_INTANGIBLE_PROJECTILE = 18;
    public static final int ID_FOOD = 19;
    public static final int ID_CONSUMABLE = 20;
    public static final int ID_STORED_ENCHANTMENTS = 21;
    public static final int ID_DYED_COLOR = 22;
    public static final int ID_MAP_COLOR = 23;

    // Component ResourceLocation Strings
    public static final String KEY_CUSTOM_DATA = "minecraft:custom_data";
    public static final String KEY_MAX_STACK_SIZE = "minecraft:max_stack_size";
    public static final String KEY_MAX_DAMAGE = "minecraft:max_damage";
    public static final String KEY_DAMAGE = "minecraft:damage";
    public static final String KEY_UNBREAKABLE = "minecraft:unbreakable";
    public static final String KEY_CUSTOM_NAME = "minecraft:custom_name";
    public static final String KEY_ITEM_NAME = "minecraft:item_name";
    public static final String KEY_LORE = "minecraft:lore";
    public static final String KEY_RARITY = "minecraft:rarity";
    public static final String KEY_ENCHANTMENTS = "minecraft:enchantments";
    public static final String KEY_CAN_PLACE_ON = "minecraft:can_place_on";
    public static final String KEY_CAN_BREAK = "minecraft:can_break";
    public static final String KEY_ATTRIBUTE_MODIFIERS = "minecraft:attribute_modifiers";
    public static final String KEY_CUSTOM_MODEL_DATA = "minecraft:custom_model_data";
    public static final String KEY_HIDE_ADDITIONAL_TOOLTIP = "minecraft:hide_additional_tooltip";
    public static final String KEY_REPAIR_COST = "minecraft:repair_cost";
    public static final String KEY_CREATIVE_SLOT_LOCK = "minecraft:creative_slot_lock";
    public static final String KEY_ENCHANTMENT_GLINT_OVERRIDE = "minecraft:enchantment_glint_override";
    public static final String KEY_INTANGIBLE_PROJECTILE = "minecraft:intangible_projectile";
    public static final String KEY_FOOD = "minecraft:food";
    public static final String KEY_CONSUMABLE = "minecraft:consumable";
    public static final String KEY_STORED_ENCHANTMENTS = "minecraft:stored_enchantments";
    public static final String KEY_DYED_COLOR = "minecraft:dyed_color";
    public static final String KEY_MAP_COLOR = "minecraft:map_color";

    private final DataComponentCodecDescriptor[] directTable;
    private final Map<String, DataComponentCodecDescriptor> keyIndex;
    private final Map<String, Integer> idIndex;

    /**
     * Constructs a fast table pre-populated with all standard Minecraft 1.20.5+ components.
     */
    public DataComponentCodecsFastTable() {
        this.directTable = new DataComponentCodecDescriptor[TABLE_CAPACITY];
        this.keyIndex = new ConcurrentHashMap<>();
        this.idIndex = new ConcurrentHashMap<>();

        registerDefaultComponents();
    }

    /**
     * Registers a custom or standard Data Component descriptor into the fast table.
     *
     * @param descriptor descriptor entry
     */
    public void register(DataComponentCodecDescriptor descriptor) {
        Objects.requireNonNull(descriptor, "descriptor cannot be null");
        int id = descriptor.componentId();
        if (id >= TABLE_CAPACITY) {
            throw new IllegalArgumentException("Component ID exceeds fast table capacity: " + id);
        }

        directTable[id] = descriptor;
        keyIndex.put(descriptor.namespacedKey(), descriptor);
        idIndex.put(descriptor.namespacedKey(), id);
    }

    /**
     * Retrieves descriptor by direct primitive integer ID in O(1) single CPU instruction.
     *
     * @param componentId integer component identifier
     * @return descriptor or null if not registered
     */
    public DataComponentCodecDescriptor getById(int componentId) {
        if (componentId < 0 || componentId >= TABLE_CAPACITY) {
            return null;
        }
        return directTable[componentId];
    }

    /**
     * Retrieves descriptor by namespaced key in O(1) hash lookup.
     *
     * @param namespacedKey component key
     * @return descriptor or null if not registered
     */
    public DataComponentCodecDescriptor getByKey(String namespacedKey) {
        if (namespacedKey == null) {
            return null;
        }
        return keyIndex.get(namespacedKey);
    }

    /**
     * Resolves component integer ID from namespaced key in O(1).
     *
     * @param namespacedKey component key
     * @return integer ID, or UNKNOWN_COMPONENT_ID (-1)
     */
    public int getId(String namespacedKey) {
        if (namespacedKey == null) {
            return UNKNOWN_COMPONENT_ID;
        }
        Integer id = idIndex.get(namespacedKey);
        return (id != null) ? id : UNKNOWN_COMPONENT_ID;
    }

    /**
     * Checks if a component ID is currently active in the table.
     *
     * @param componentId component ID
     * @return true if registered
     */
    public boolean containsId(int componentId) {
        return componentId >= 0 && componentId < TABLE_CAPACITY && directTable[componentId] != null;
    }

    /**
     * Checks if a namespaced key is currently registered.
     *
     * @param namespacedKey component key
     * @return true if registered
     */
    public boolean containsKey(String namespacedKey) {
        return namespacedKey != null && keyIndex.containsKey(namespacedKey);
    }

    /**
     * Serializes a primitive or simple component value directly to a ByteBuffer without allocating NBT nodes.
     *
     * @param componentId component ID
     * @param value Java value
     * @param buffer destination ByteBuffer
     */
    public void encodeDirect(int componentId, Object value, ByteBuffer buffer) {
        DataComponentCodecDescriptor desc = getById(componentId);
        if (desc == null) {
            throw new IllegalArgumentException("Unrecognized component ID: " + componentId);
        }
        Objects.requireNonNull(buffer, "buffer cannot be null");

        buffer.putShort((short) componentId);

        switch (desc.valueType()) {
            case BOOLEAN -> {
                boolean b = (value instanceof Boolean bool) ? bool : false;
                buffer.put((byte) (b ? 1 : 0));
            }
            case BYTE -> {
                byte b = (value instanceof Number n) ? n.byteValue() : 0;
                buffer.put(b);
            }
            case SHORT -> {
                short s = (value instanceof Number n) ? n.shortValue() : 0;
                buffer.putShort(s);
            }
            case INT -> {
                int i = (value instanceof Number n) ? n.intValue() : 0;
                buffer.putInt(i);
            }
            case LONG -> {
                long l = (value instanceof Number n) ? n.longValue() : 0L;
                buffer.putLong(l);
            }
            case FLOAT -> {
                float f = (value instanceof Number n) ? n.floatValue() : 0.0f;
                buffer.putFloat(f);
            }
            case DOUBLE -> {
                double d = (value instanceof Number n) ? n.doubleValue() : 0.0;
                buffer.putDouble(d);
            }
            case STRING -> {
                String str = (value != null) ? value.toString() : "";
                byte[] bytes = str.getBytes(StandardCharsets.UTF_8);
                buffer.putShort((short) bytes.length);
                buffer.put(bytes);
            }
            case COMPOUND_BLOB, LIST -> {
                if (value instanceof byte[] blob) {
                    buffer.putInt(blob.length);
                    buffer.put(blob);
                } else {
                    buffer.putInt(0);
                }
            }
        }
    }

    /**
     * Decodes a component value directly from a ByteBuffer without intermediate object allocation.
     *
     * @param componentId component ID
     * @param buffer source ByteBuffer
     * @return decoded value
     */
    public Object decodeDirect(int componentId, ByteBuffer buffer) {
        DataComponentCodecDescriptor desc = getById(componentId);
        if (desc == null) {
            throw new IllegalArgumentException("Unrecognized component ID: " + componentId);
        }
        Objects.requireNonNull(buffer, "buffer cannot be null");

        return switch (desc.valueType()) {
            case BOOLEAN -> buffer.get() != 0;
            case BYTE -> buffer.get();
            case SHORT -> buffer.getShort();
            case INT -> buffer.getInt();
            case LONG -> buffer.getLong();
            case FLOAT -> buffer.getFloat();
            case DOUBLE -> buffer.getDouble();
            case STRING -> {
                int len = buffer.getShort() & 0xFFFF;
                byte[] strBytes = new byte[len];
                buffer.get(strBytes);
                yield new String(strBytes, StandardCharsets.UTF_8);
            }
            case COMPOUND_BLOB, LIST -> {
                int len = buffer.getInt();
                byte[] data = new byte[len];
                buffer.get(data);
                yield data;
            }
        };
    }

    private void registerDefaultComponents() {
        register(new DataComponentCodecDescriptor(ID_CUSTOM_DATA, KEY_CUSTOM_DATA, DataComponentValueType.COMPOUND_BLOB, null, false));
        register(new DataComponentCodecDescriptor(ID_MAX_STACK_SIZE, KEY_MAX_STACK_SIZE, DataComponentValueType.INT, 64, false));
        register(new DataComponentCodecDescriptor(ID_MAX_DAMAGE, KEY_MAX_DAMAGE, DataComponentValueType.INT, 0, false));
        register(new DataComponentCodecDescriptor(ID_DAMAGE, KEY_DAMAGE, DataComponentValueType.INT, 0, false));
        register(new DataComponentCodecDescriptor(ID_UNBREAKABLE, KEY_UNBREAKABLE, DataComponentValueType.BOOLEAN, false, false));
        register(new DataComponentCodecDescriptor(ID_CUSTOM_NAME, KEY_CUSTOM_NAME, DataComponentValueType.STRING, "", false));
        register(new DataComponentCodecDescriptor(ID_ITEM_NAME, KEY_ITEM_NAME, DataComponentValueType.STRING, "", false));
        register(new DataComponentCodecDescriptor(ID_LORE, KEY_LORE, DataComponentValueType.LIST, null, false));
        register(new DataComponentCodecDescriptor(ID_RARITY, KEY_RARITY, DataComponentValueType.STRING, "common", false));
        register(new DataComponentCodecDescriptor(ID_ENCHANTMENTS, KEY_ENCHANTMENTS, DataComponentValueType.COMPOUND_BLOB, null, false));
        register(new DataComponentCodecDescriptor(ID_CAN_PLACE_ON, KEY_CAN_PLACE_ON, DataComponentValueType.LIST, null, false));
        register(new DataComponentCodecDescriptor(ID_CAN_BREAK, KEY_CAN_BREAK, DataComponentValueType.LIST, null, false));
        register(new DataComponentCodecDescriptor(ID_ATTRIBUTE_MODIFIERS, KEY_ATTRIBUTE_MODIFIERS, DataComponentValueType.LIST, null, false));
        register(new DataComponentCodecDescriptor(ID_CUSTOM_MODEL_DATA, KEY_CUSTOM_MODEL_DATA, DataComponentValueType.INT, 0, false));
        register(new DataComponentCodecDescriptor(ID_HIDE_ADDITIONAL_TOOLTIP, KEY_HIDE_ADDITIONAL_TOOLTIP, DataComponentValueType.BOOLEAN, false, false));
        register(new DataComponentCodecDescriptor(ID_REPAIR_COST, KEY_REPAIR_COST, DataComponentValueType.INT, 0, false));
        register(new DataComponentCodecDescriptor(ID_CREATIVE_SLOT_LOCK, KEY_CREATIVE_SLOT_LOCK, DataComponentValueType.BOOLEAN, false, false));
        register(new DataComponentCodecDescriptor(ID_ENCHANTMENT_GLINT_OVERRIDE, KEY_ENCHANTMENT_GLINT_OVERRIDE, DataComponentValueType.BOOLEAN, false, false));
        register(new DataComponentCodecDescriptor(ID_INTANGIBLE_PROJECTILE, KEY_INTANGIBLE_PROJECTILE, DataComponentValueType.BOOLEAN, false, false));
        register(new DataComponentCodecDescriptor(ID_FOOD, KEY_FOOD, DataComponentValueType.COMPOUND_BLOB, null, false));
        register(new DataComponentCodecDescriptor(ID_CONSUMABLE, KEY_CONSUMABLE, DataComponentValueType.COMPOUND_BLOB, null, false));
        register(new DataComponentCodecDescriptor(ID_STORED_ENCHANTMENTS, KEY_STORED_ENCHANTMENTS, DataComponentValueType.COMPOUND_BLOB, null, false));
        register(new DataComponentCodecDescriptor(ID_DYED_COLOR, KEY_DYED_COLOR, DataComponentValueType.INT, 0, false));
        register(new DataComponentCodecDescriptor(ID_MAP_COLOR, KEY_MAP_COLOR, DataComponentValueType.INT, 0, false));
    }
}
