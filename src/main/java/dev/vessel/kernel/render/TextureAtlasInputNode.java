package dev.vessel.kernel.render;

import java.util.Objects;

/**
 * Immutable input request for packing an individual texture sprite into an atlas.
 *
 * @param textureId unique resource location or sprite identifier
 * @param width sprite pixel width
 * @param height sprite pixel height
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public record TextureAtlasInputNode(
        String textureId,
        int width,
        int height
) {
    public TextureAtlasInputNode {
        Objects.requireNonNull(textureId, "textureId cannot be null");
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("Texture dimensions must be strictly positive: " + width + "x" + height);
        }
    }
}
