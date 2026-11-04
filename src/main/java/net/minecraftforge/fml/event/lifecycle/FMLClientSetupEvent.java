package net.minecraftforge.fml.event.lifecycle;

/**
 * Universal headless server-safe shim for Forge FMLClientSetupEvent.
 * Dispatched on mod event bus during client setup wave; no-op on dedicated server.
 * Maintained under VESSEL Rule 17: Standalone single-purpose class.
 *
 * @version 1.0.0
 */
public class FMLClientSetupEvent extends ParallelDispatchEvent {

    public FMLClientSetupEvent() {
        super();
    }
}
