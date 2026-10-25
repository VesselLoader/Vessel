package dev.vessel.kernel.gui;

import java.util.Objects;

/**
 * Immutable atomic element representation in a negative-space glyph layout.
 *
 * @param glyphChar character sequence representing the texture or icon glyph
 * @param xOffset horizontal pixel coordinate relative to the root origin
 * @param yOffset vertical pixel coordinate relative to the root origin
 * @param widthPx width of this glyph in pixels
 * @param layer layer category defining depth sorting
 * @param rgbColor RGB color hex code or -1 for default/white tint
 * @param bold whether bold text styling should be applied
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public record FontGlyphElement(
        String glyphChar,
        int xOffset,
        int yOffset,
        int widthPx,
        GlyphLayerType layer,
        int rgbColor,
        boolean bold
) {
    public FontGlyphElement {
        Objects.requireNonNull(glyphChar, "glyphChar cannot be null");
        Objects.requireNonNull(layer, "layer cannot be null");
        if (widthPx < 0) {
            throw new IllegalArgumentException("widthPx must be non-negative: " + widthPx);
        }
    }
}
