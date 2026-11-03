package net.kyori.adventure.text.format;

import java.util.Objects;

/**
 * Canonical Kyori Adventure standard 16 named Minecraft text colors
 * ({@code net.kyori.adventure.text.format.NamedTextColor}).
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public final class NamedTextColor extends TextColor {

    public static final NamedTextColor BLACK = new NamedTextColor("black", 0x000000);
    public static final NamedTextColor DARK_BLUE = new NamedTextColor("dark_blue", 0x0000AA);
    public static final NamedTextColor DARK_GREEN = new NamedTextColor("dark_green", 0x00AA00);
    public static final NamedTextColor DARK_AQUA = new NamedTextColor("dark_aqua", 0x00AAAA);
    public static final NamedTextColor DARK_RED = new NamedTextColor("dark_red", 0xAA0000);
    public static final NamedTextColor DARK_PURPLE = new NamedTextColor("dark_purple", 0xAA00AA);
    public static final NamedTextColor GOLD = new NamedTextColor("gold", 0xFFAA00);
    public static final NamedTextColor GRAY = new NamedTextColor("gray", 0xAAAAAA);
    public static final NamedTextColor DARK_GRAY = new NamedTextColor("dark_gray", 0x555555);
    public static final NamedTextColor BLUE = new NamedTextColor("blue", 0x5555FF);
    public static final NamedTextColor GREEN = new NamedTextColor("green", 0x55FF55);
    public static final NamedTextColor AQUA = new NamedTextColor("aqua", 0x55FFFF);
    public static final NamedTextColor RED = new NamedTextColor("red", 0xFF5555);
    public static final NamedTextColor LIGHT_PURPLE = new NamedTextColor("light_purple", 0xFF55FF);
    public static final NamedTextColor YELLOW = new NamedTextColor("yellow", 0xFFFF55);
    public static final NamedTextColor WHITE = new NamedTextColor("white", 0xFFFFFF);

    private final String name;

    private NamedTextColor(String name, int value) {
        super(value);
        this.name = Objects.requireNonNull(name, "name must not be null");
    }

    /**
     * Returns the canonical lowercase name of this named text color.
     *
     * @return color name (e.g., {@code "green"}, {@code "gold"})
     */
    public String name() {
        return name;
    }

    @Override
    public String toString() {
        return name;
    }
}
