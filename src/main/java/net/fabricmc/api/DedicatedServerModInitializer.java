package net.fabricmc.api;

/**
 * Canonical Fabric/Quilt dedicated server mod entrypoint interface
 * ({@code net.fabricmc.api.DedicatedServerModInitializer}) invoked during VESSEL's single-pass
 * server startup sequence for all {@code "server"} entrypoints declared in {@code fabric.mod.json}
 * or {@code quilt.mod.json}.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public interface DedicatedServerModInitializer {

    /**
     * Runs the dedicated server initialization logic for this Fabric/Quilt mod.
     */
    void onInitializeServer();
}
