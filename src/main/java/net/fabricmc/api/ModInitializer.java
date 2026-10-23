package net.fabricmc.api;

/**
 * Canonical Fabric/Quilt common mod entrypoint interface ({@code net.fabricmc.api.ModInitializer})
 * invoked during VESSEL's single-pass mod initialization phase for all {@code "main"} entrypoints
 * declared in {@code fabric.mod.json} or {@code quilt.mod.json}.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public interface ModInitializer {

    /**
     * Runs the common (universal client/server) initialization logic for this Fabric/Quilt mod.
     */
    void onInitialize();
}
