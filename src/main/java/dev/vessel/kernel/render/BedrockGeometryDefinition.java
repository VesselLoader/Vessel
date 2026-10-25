package dev.vessel.kernel.render;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Immutable definition of a Bedrock 1.21 Geometry model ("minecraft:geometry").
 *
 * @param geometryIdentifier model identifier (e.g. "geometry.vessel.crusher_machine")
 * @param textureWidth atlas texture width (e.g. 64, 128)
 * @param textureHeight atlas texture height (e.g. 64, 128)
 * @param bones hierarchical bone collection
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public record BedrockGeometryDefinition(
        String geometryIdentifier,
        int textureWidth,
        int textureHeight,
        List<BedrockGeometryBone> bones
) {
    public BedrockGeometryDefinition {
        Objects.requireNonNull(geometryIdentifier, "geometryIdentifier cannot be null");
        if (textureWidth <= 0 || textureHeight <= 0) {
            throw new IllegalArgumentException("Texture dimensions must be positive");
        }
        bones = bones != null ? Collections.unmodifiableList(bones) : Collections.emptyList();
    }
}
