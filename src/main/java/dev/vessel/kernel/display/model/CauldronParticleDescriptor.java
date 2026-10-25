package dev.vessel.kernel.display.model;

import java.util.Objects;

/**
 * Immutable particle emission event descriptor for synthetic client broadcast packets.
 *
 * @param particleType Minecraft particle identifier (e.g. "minecraft:crimson_spore", "minecraft:bubble_pop")
 * @param posX world X coordinate
 * @param posY world Y coordinate
 * @param posZ world Z coordinate
 * @param velocityX motion vector X
 * @param velocityY motion vector Y
 * @param velocityZ motion vector Z
 * @param count particle count
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public record CauldronParticleDescriptor(
        String particleType,
        double posX,
        double posY,
        double posZ,
        double velocityX,
        double velocityY,
        double velocityZ,
        int count
) {
    public CauldronParticleDescriptor {
        Objects.requireNonNull(particleType, "particleType cannot be null");
        if (count < 1) {
            throw new IllegalArgumentException("count must be at least 1: " + count);
        }
    }
}
