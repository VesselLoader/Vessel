package dev.vessel.kernel.migration;

/**
 * World storage directory organizational scheme classifier.
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public enum WorldStorageDirectoryLayout {
    /**
     * Vanilla Minecraft, Fabric, Forge, and NeoForge unified single root world folder.
     * Nether is stored in {@code world/DIM-1/}, End in {@code world/DIM1/}.
     */
    VANILLA_UNIFIED,

    /**
     * CraftBukkit, Spigot, Paper, Purpur, and Folia split world directory model.
     * Dimensions are stored in independent top-level root folders:
     * {@code world/}, {@code world_nether/}, and {@code world_the_end/}.
     */
    SPIGOT_SPLIT,

    /**
     * Unrecognized or uninitialized directory scheme.
     */
    UNKNOWN
}
