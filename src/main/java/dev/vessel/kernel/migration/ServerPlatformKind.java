package dev.vessel.kernel.migration;

/**
 * Upstream server platform types recognized by the VESSEL migration engine.
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public enum ServerPlatformKind {

    /** Standard vanilla Minecraft dedicated server. */
    VANILLA,

    /** Bukkit / CraftBukkit / Spigot server. */
    BUKKIT_SPIGOT,

    /** Paper / Purpur server. */
    PAPER,

    /** Folia regionized multithreaded server. */
    FOLIA,

    /** Fabric Loader dedicated server. */
    FABRIC,

    /** Quilt Loader dedicated server. */
    QUILT,

    /** Minecraft Forge dedicated server. */
    FORGE,

    /** NeoForge dedicated server. */
    NEOFORGE
}
