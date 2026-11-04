package net.minecraft.util.text;

/**
 * Universal cross-era shim for Minecraft 1.12.2 ITextComponent interface.
 * Redirects legacy 1.12 text components into modern Minecraft 26.3 chat components.
 * Maintained under VESSEL Rule 17: Standalone single-purpose interface.
 *
 * @version 1.0.0
 */
public interface ITextComponent {

    /**
     * Returns raw unformatted plain text.
     *
     * @return unformatted text string
     */
    String getUnformattedText();

    /**
     * Returns formatted text string with styling codes.
     *
     * @return formatted text string
     */
    default String getFormattedText() {
        return getUnformattedText();
    }
}
