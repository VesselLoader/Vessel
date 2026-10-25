package dev.vessel.kernel.gui;

import java.util.Objects;

/**
 * Immutable rendered representation of a composed font glyph UI slice.
 * Contains JSON-formatted Minecraft component text and its visual dimensions.
 *
 * @param jsonText Minecraft JSON raw component string
 * @param totalAdvancePx cumulative pixel horizontal advance after rendering
 * @param layerPriority sorting priority of this component
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public record RenderedGlyphComponent(
        String jsonText,
        int totalAdvancePx,
        int layerPriority
) {
    public RenderedGlyphComponent {
        Objects.requireNonNull(jsonText, "jsonText cannot be null");
    }
}
