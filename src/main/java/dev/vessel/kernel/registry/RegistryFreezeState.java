package dev.vessel.kernel.registry;

/**
 * State representing the freeze lifecycle of a dynamic Minecraft registry.
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
public enum RegistryFreezeState {
    UNLOCKED,
    BOOT_PENDING,
    SEALED,
    IMMUTABLE
}
