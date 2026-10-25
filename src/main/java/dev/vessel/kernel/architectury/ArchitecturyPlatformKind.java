package dev.vessel.kernel.architectury;

/**
 * Enumeration of supported runtime platforms bridged by Architectury synthetic hooks.
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public enum ArchitecturyPlatformKind {

    /**
     * Fabric Loader runtime environment.
     */
    FABRIC,

    /**
     * NeoForge modern runtime environment.
     */
    NEOFORGE,

    /**
     * Legacy Forge runtime environment.
     */
    FORGE,

    /**
     * VESSEL sovereign native microkernel runtime.
     */
    VESSEL_NATIVE
}
