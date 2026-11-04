package org.bukkit;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Clean-room enumeration of legacy Minecraft chat formatting codes and colors
 * ({@code org.bukkit.ChatColor}).
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public enum ChatColor {

    BLACK('0', 0x00),
    DARK_BLUE('1', 0x01),
    DARK_GREEN('2', 0x02),
    DARK_AQUA('3', 0x03),
    DARK_RED('4', 0x04),
    DARK_PURPLE('5', 0x05),
    GOLD('6', 0x06),
    GRAY('7', 0x07),
    DARK_GRAY('8', 0x08),
    BLUE('9', 0x09),
    GREEN('a', 0x0A),
    AQUA('b', 0x0B),
    RED('c', 0x0C),
    LIGHT_PURPLE('d', 0x0D),
    YELLOW('e', 0x0E),
    WHITE('f', 0x0F),
    MAGIC('k', 0x10, true),
    BOLD('l', 0x11, true),
    STRIKETHROUGH('m', 0x12, true),
    UNDERLINE('n', 0x13, true),
    ITALIC('o', 0x14, true),
    RESET('r', 0x15);

    public static final char COLOR_CHAR = '\u00A7';
    private static final Pattern STRIP_COLOR_PATTERN =
            Pattern.compile("(?i)" + COLOR_CHAR + "[0-9A-FK-OR]");

    private static final Map<Character, ChatColor> BY_CHAR = new HashMap<>();

    static {
        for (ChatColor color : values()) {
            BY_CHAR.put(color.code, color);
        }
    }

    private final char code;
    private final int intCode;
    private final boolean isFormat;
    private final String toString;

    ChatColor(char code, int intCode) {
        this(code, intCode, false);
    }

    ChatColor(char code, int intCode, boolean isFormat) {
        this.code = code;
        this.intCode = intCode;
        this.isFormat = isFormat;
        this.toString = new String(new char[]{COLOR_CHAR, code});
    }

    public char getChar() {
        return code;
    }

    public boolean isFormat() {
        return isFormat;
    }

    public boolean isColor() {
        return !isFormat && this != RESET;
    }

    @Override
    public String toString() {
        return toString;
    }

    public static ChatColor getByChar(char code) {
        return BY_CHAR.get(Character.toLowerCase(code));
    }

    public static ChatColor getByChar(String code) {
        if (code == null || code.isEmpty()) {
            return null;
        }
        return getByChar(code.charAt(0));
    }

    public static String stripColor(String input) {
        if (input == null) {
            return null;
        }
        return STRIP_COLOR_PATTERN.matcher(input).replaceAll("");
    }

    public static String translateAlternateColorCodes(char altColorChar, String textToTranslate) {
        if (textToTranslate == null) {
            return null;
        }
        char[] chars = textToTranslate.toCharArray();
        for (int i = 0; i < chars.length - 1; i++) {
            if (chars[i] == altColorChar
                    && "0123456789AaBbCcDdEeFfKkLlMmNnOoRrXx".indexOf(chars[i + 1]) > -1) {
                chars[i] = COLOR_CHAR;
                chars[i + 1] = Character.toLowerCase(chars[i + 1]);
            }
        }
        return new String(chars);
    }
}
