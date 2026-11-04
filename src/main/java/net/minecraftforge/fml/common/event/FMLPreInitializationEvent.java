package net.minecraftforge.fml.common.event;

import java.io.File;

/**
 * Universal cross-era shim for Forge 1.12.2 FMLPreInitializationEvent.
 * Dispatched on mod event bus during early pre-initialization wave.
 * Maintained under VESSEL Rule 17: Standalone single-purpose class.
 *
 * @version 1.0.0
 */
public class FMLPreInitializationEvent {

    public FMLPreInitializationEvent() {
    }

    /**
     * Returns suggested configuration file for this mod.
     *
     * @return non-null file object
     */
    public File getSuggestedConfigurationFile() {
        return new File("config/legacy-mod.cfg");
    }

    /**
     * Returns the global mod configuration directory.
     *
     * @return non-null directory file
     */
    public File getModConfigurationDirectory() {
        return new File("config");
    }
}
