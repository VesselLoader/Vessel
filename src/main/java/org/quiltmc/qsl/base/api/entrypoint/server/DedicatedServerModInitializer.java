package org.quiltmc.qsl.base.api.entrypoint.server;

import net.fabricmc.loader.api.ModContainer;

/**
 * Canonical Quilt Standard Libraries (QSL) dedicated server mod initializer entrypoint
 * ({@code org.quiltmc.qsl.base.api.entrypoint.server.DedicatedServerModInitializer}).
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public interface DedicatedServerModInitializer {

    /**
     * Invoked by VESSEL when a Quilt mod is initialized on a dedicated server.
     *
     * @param mod the owning {@link ModContainer} for the initializing Quilt server mod
     */
    void onInitializeServer(ModContainer mod);
}
