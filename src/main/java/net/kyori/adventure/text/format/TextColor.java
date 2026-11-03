package net.kyori.adventure.text.format;

import java.util.Locale;

/**
 * Canonical Kyori Adventure 24-bit RGB text color
 * ({@code net.kyori.adventure.text.format.TextColor}).
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public class TextColor {

    private final int value;

    /**
     * Constructs a 24-bit RGB {@code TextColor}.
     *
     * @param value 24-bit RGB integer ({@code 0xRRGGBB})
     */
    public TextColor(int value) {
        this.value = value & 0xFFFFFF;
    }

    /**
     * Creates a {@code TextColor} from a 24-bit RGB integer.
     *
     * @param value RGB value
     * @return {@code TextColor} instance
     */
    public static TextColor color(int value) {
        return new TextColor(value);
    }

    /**
     * Creates a {@code TextColor} from individual 8-bit red, green, and blue components.
     *
     * @param r red component (0-255)
     * @param g green component (0-255)
     * @param b blue component (0-255)
     * @return {@code TextColor} instance
     */
    public static TextColor color(int r, int g, int b) {
        return new TextColor(((r & 0xFF) << 16) | ((g & 0xFF) << 8) | (b & 0xFF));
    }

    /**
     * Parses a hexadecimal RGB string ({@code "#RRGGBB"} or {@code "RRGGBB"}) into a {@code TextColor}.
     *
     * @param hex hexadecimal color string
     * @return parsed {@code TextColor}, or {@code null} if invalid
     */
    public static TextColor fromHexString(String hex) {
        if (hex == null) {
            return null;
        }
        String normalized = hex.trim();
        if (normalized.startsWith("#")) {
            normalized = normalized.substring(1);
        }
        if (normalized.length() != 6) {
            return null;
        }
        try {
            int parsed = Integer.parseInt(normalized, 16);
            return new TextColor(parsed);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Returns the 24-bit RGB integer value ({@code 0xRRGGBB}).
     *
     * @return RGB integer
     */
    public final int value() {
        return value;
    }

    /**
     * Returns the 8-bit red channel (0-255).
     *
     * @return red component
     */
    public final int red() {
        return (value >> 16) & 0xFF;
    }

    /**
     * Returns the 8-bit green channel (0-255).
     *
     * @return green component
     */
    public final int green() {
        return (value >> 8) & 0xFF;
    }

    /**
     * Returns the 8-bit blue channel (0-255).
     *
     * @return blue component
     */
    public final int blue() {
        return value & 0xFF;
    }

    /**
     * Formats this color as a lowercase hexadecimal string ({@code "#rrggbb"}).
     *
     * @return formatted hex string
     */
    public final String asHexString() {
        return String.format(Locale.ROOT, "#%06x", value);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof TextColor other)) {
            return false;
        }
        return value == other.value;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(value);
    }

    @Override
    public String toString() {
        return asHexString();
    }
}
