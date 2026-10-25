package dev.vessel.kernel.render;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Immutable outcome of a bin-packing operation on a collection of textures.
 *
 * @param atlasWidth total width of the resolved atlas canvas (Power of Two)
 * @param atlasHeight total height of the resolved atlas canvas (Power of Two)
 * @param occupancyRatio area utilization fraction [0.0, 1.0]
 * @param packedRegions list of successfully packed texture regions
 * @param unplacedNodes list of textures that could not fit into the maximum atlas size
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public record TextureAtlasPackResult(
        int atlasWidth,
        int atlasHeight,
        float occupancyRatio,
        List<TextureAtlasPackedRegion> packedRegions,
        List<TextureAtlasInputNode> unplacedNodes
) {
    public TextureAtlasPackResult {
        if (atlasWidth <= 0 || atlasHeight <= 0) {
            throw new IllegalArgumentException("Atlas dimensions must be positive");
        }
        packedRegions = packedRegions != null ? Collections.unmodifiableList(packedRegions) : Collections.emptyList();
        unplacedNodes = unplacedNodes != null ? Collections.unmodifiableList(unplacedNodes) : Collections.emptyList();
    }

    /**
     * Checks if all requested textures were packed with zero unplaced nodes.
     */
    public boolean isFullyPacked() {
        return unplacedNodes.isEmpty();
    }
}
