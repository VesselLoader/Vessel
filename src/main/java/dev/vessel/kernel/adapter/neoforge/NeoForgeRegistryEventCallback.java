package dev.vessel.kernel.adapter.neoforge;

/**
 * Functional callback interface notifying subscribers when a NeoForge deferred entry is registered.
 *
 * @version 1.0.0
 * @author VESSEL Engine Team
 */
@FunctionalInterface
public interface NeoForgeRegistryEventCallback {

    /**
     * Invoked when a deferred entry is registered or retroactively replayed.
     *
     * @param holder registered entry holder
     */
    void onEntryRegistered(NeoForgeRegistryHolder holder);
}
