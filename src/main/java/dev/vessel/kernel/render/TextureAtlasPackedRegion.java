package dev.vessel.kernel.render;

import java.util.Objects;

/**
 * Immutable placement metadata for a packed texture sprite inside an atlas canvas.
 * Includes absolute pixel coordinates and normalized UV coordinates [0.0, 1.0].
 *
 * @param textureId unique resource location of the packed texture
 * @param x top-left horizontal pixel coordinate in atlas
 * @param y top-left vertical pixel coordinate in atlas
 * @param width sprite pixel width
 * @param height sprite pixel height
 * @param uMin normalized horizontal minimum UV coordinate [0.0, 1.0]
 * @param vMin normalized vertical minimum UV coordinate [0.0, 1.0]
 * @param uMax normalized horizontal maximum UV coordinate [0.0, 1.0]
 * @param vMax normalized vertical maximum UV coordinate [0.0, 1.0]
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public record TextureAtlasPackedRegion(
        String textureId,
        int x,
        int y,
        int width,
        int height,
        float uMin,
        float vMin,
        float uMax,
        float vMax
) {
    public TextureAtlasPackedRegion {
        Objects.requireNonNull(textureId, "textureId cannot be null");
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("Width and height must be positive");
        }
    }
}
