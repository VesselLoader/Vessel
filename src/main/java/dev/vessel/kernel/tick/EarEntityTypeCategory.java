package dev.vessel.kernel.tick;

/**
 * Functional entity categories used for 3D Spherical Entity Activation Range (EAR) evaluation.
 * <p>
 * Directly resolves ERR-41 and ERR-42.
 * </p>
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public enum EarEntityTypeCategory {

    /**
     * Hostile mobs (Zombies, Skeletons, Creepers, Endermen).
     */
    MONSTER,

    /**
     * Passive land animals (Cows, Sheep, Pigs, Chickens, Horses).
     */
    ANIMAL,

    /**
     * Water mobs (Fish, Squids, Dolphins, Guardians).
     */
    WATER_CREATURE,

    /**
     * Flying entities (Bats, Phantoms, Ghasts, Allays).
     */
    FLYING,

    /**
     * Ambient environmental entities.
     */
    AMBIENT,

    /**
     * Projectiles, items, falling blocks, experience orbs, minecarts.
     */
    MISC,

    /**
     * Villagers and Wandering Traders.
     */
    VILLAGER,

    /**
     * Pillagers, Vindicators, Evokers, Ravagers, Witches during raids.
     */
    RAIDER
}
