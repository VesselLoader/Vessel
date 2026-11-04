package net.minecraft.world.item;

/**
 * Enumeration representing armor equipment slot types (HELMET, CHESTPLATE, LEGGINGS, BOOTS).
 * Adheres strictly to Rule 17 (Single Responsibility Principle - zero inner classes).
 *
 * @version 1.0.0
 */
public enum ArmorItemType {
    HELMET(11, "helmet"),
    CHESTPLATE(16, "chestplate"),
    LEGGINGS(15, "leggings"),
    BOOTS(13, "boots");

    private final int slotId;
    private final String name;

    ArmorItemType(int slotId, String name) {
        this.slotId = slotId;
        this.name = name;
    }

    public int getSlotId() {
        return slotId;
    }

    public String getName() {
        return name;
    }
}
