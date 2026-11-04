package net.minecraft.world.item;

/**
 * Universal cross-era stub for ElytraItem, bridging legacy 1.20.1 elytra references
 * to modern Minecraft servers where elytra gliding is component-driven.
 * Adheres strictly to Rule 17 (Single Responsibility Principle).
 *
 * @version 1.0.0
 */
public class ElytraItem {

    public ElytraItem() {
    }

    public static boolean isFlyEnabled(Object itemStack) {
        return itemStack != null;
    }
}
