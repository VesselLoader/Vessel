package net.minecraft.util.text;

import java.util.Objects;

/**
 * Universal cross-era shim for Minecraft 1.12.2 TextComponentString.
 * Maintained under VESSEL Rule 17: Standalone single-purpose class.
 *
 * @version 1.0.0
 */
public class TextComponentString implements ITextComponent {

    private final String text;

    public TextComponentString(String text) {
        this.text = Objects.requireNonNull(text, "text must not be null");
    }

    @Override
    public String getUnformattedText() {
        return text;
    }

    @Override
    public String toString() {
        return text;
    }
}
