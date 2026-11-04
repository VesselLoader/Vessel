package net.minecraft.client.gui;

/**
 * Headless server-safe stub for Font to prevent NoClassDefFoundError when loading Catnip/Create text builders.
 * Adheres strictly to Rule 17 (Single Responsibility Principle).
 *
 * @version 1.0.0
 */
public class Font {

    public Font() {
    }

    public int width(String text) {
        return text != null ? text.length() * 6 : 0;
    }
}
