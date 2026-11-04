package dev.vessel.kernel.compat.era;

/**
 * Universal multi-era compatibility bridge restoring RGB integer color lookups for
 * ChatFormatting that were stripped in modern Minecraft 1.21+ / 26.3.
 *
 * <p>Uses {@link Object} signature to prevent ClassLoader LinkageError between Tier-1 vanilla
 * and App classloaders.</p>
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.1
 */
public final class ChatFormattingColorBridge {

    private ChatFormattingColorBridge() {
    }

    /**
     * Returns the RGB color integer value for the given formatting, matching legacy Minecraft behavior.
     *
     * @param formatting target format object (typically an enum)
     * @return RGB integer or {@code null} if not a color format
     */
    public static Integer getColor(Object formatting) {
        if (formatting == null) {
            return null;
        }
        String name = formatting instanceof Enum<?> e ? e.name() : formatting.toString();
        return switch (name) {
            case "BLACK" -> 0x000000;
            case "DARK_BLUE" -> 0x0000AA;
            case "DARK_GREEN" -> 0x00AA00;
            case "DARK_AQUA" -> 0x00AAAA;
            case "DARK_RED" -> 0xAA0000;
            case "DARK_PURPLE" -> 0xAA00AA;
            case "GOLD" -> 0xFFAA00;
            case "GRAY" -> 0xAAAAAA;
            case "DARK_GRAY" -> 0x555555;
            case "BLUE" -> 0x5555FF;
            case "GREEN" -> 0x55FF55;
            case "AQUA" -> 0x55FFFF;
            case "RED" -> 0xFF5555;
            case "LIGHT_PURPLE" -> 0xFF55FF;
            case "YELLOW" -> 0xFFFF55;
            case "WHITE" -> 0xFFFFFF;
            default -> null;
        };
    }

    /**
     * SRG mapped method alias for {@link #getColor(Object)}.
     *
     * @param formatting target format
     * @return RGB integer or {@code null}
     */
    public static Integer m_126665_(Object formatting) {
        return getColor(formatting);
    }
}
