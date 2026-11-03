package org.quiltmc.qsl.base.api.entrypoint;

import net.fabricmc.loader.api.ModContainer;

/**
 * Canonical Quilt Standard Libraries (QSL) common mod initializer entrypoint
 * ({@code org.quiltmc.qsl.base.api.entrypoint.ModInitializer}).
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public interface ModInitializer {

    /**
     * Invoked by VESSEL when a Quilt mod is initialized.
     *
     * @param mod the owning {@link ModContainer} for the initializing Quilt mod
     */
    void onInitialize(ModContainer mod);
}
