package dev.vessel.kernel.adapter.forge;

/**
 * Enumeration of distinct FML lifecycle stages conforming to Minecraft Forge specification.
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public enum FmlLifecyclePhase {

    /**
     * Mod construction phase where mod classes and initial states are instantiated.
     */
    CONSTRUCT_MOD,

    /**
     * Common setup phase where universal registry structures and network channels are initialized.
     */
    COMMON_SETUP,

    /**
     * Sided setup phase executed on the dedicated physical side (Server/Client).
     */
    SIDED_SETUP,

    /**
     * Inter-mod communications enqueue phase where mods queue messages for other mods.
     */
    INTER_MOD_ENQUEUE,

    /**
     * Inter-mod communications processing phase where queued messages are consumed.
     */
    INTER_MOD_PROCESS,

    /**
     * Load complete phase indicating all mods have concluded initialization routines.
     */
    LOAD_COMPLETE
}
