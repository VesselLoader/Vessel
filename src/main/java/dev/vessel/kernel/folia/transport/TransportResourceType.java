package dev.vessel.kernel.folia.transport;

/**
 * Defines the category of physical resource transported across Folia regional boundaries.
 * <p>
 * Directly resolves ERR-14, ERR-18, and FOL-001..020.
 * </p>
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public enum TransportResourceType {

    /**
     * Electrical energy / Forge Energy / Redstone Flux / IndustrialCraft EU / Joule.
     */
    ENERGY,

    /**
     * Liquid or gas fluid payloads (water, lava, steam, liquid oxygen, oil).
     */
    FLUID,

    /**
     * Physical item stack payloads routed via conveyor, pipe, or pneumatic tube.
     */
    ITEM,

    /**
     * Pressurized chemical substances, slurries, or radionuclides.
     */
    CHEMICAL,

    /**
     * Custom modded cross-chunk signal or arbitrary data payload.
     */
    CUSTOM
}
