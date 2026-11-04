package net.minecraft.world.item;

/**
 * Universal cross-era stub for ArmorItem, bridging legacy 1.20.1 armor references.
 * Adheres strictly to Rule 17 (Single Responsibility Principle).
 *
 * @version 1.0.0
 */
public class ArmorItem {

    private final ArmorMaterial material;
    private final ArmorItemType type;

    public ArmorItem(ArmorMaterial material, ArmorItemType type) {
        this.material = material;
        this.type = type;
    }

    public ArmorMaterial getMaterial() {
        return material;
    }

    public ArmorItemType getType() {
        return type;
    }
}
