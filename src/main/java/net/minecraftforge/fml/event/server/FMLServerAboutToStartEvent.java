package net.minecraftforge.fml.event.server;

/**
 * Universal cross-era shim for Forge FMLServerAboutToStartEvent.
 * Dispatched on Forge event bus immediately before Minecraft dedicated server initializes worlds.
 * Maintained under VESSEL Rule 17: Standalone single-purpose class.
 *
 * @version 1.0.0
 */
public class FMLServerAboutToStartEvent {

    private final Object server;

    public FMLServerAboutToStartEvent() {
        this(null);
    }

    public FMLServerAboutToStartEvent(Object server) {
        this.server = server;
    }

    /**
     * Returns running dedicated server instance.
     *
     * @return dedicated server
     */
    public Object getServer() {
        return this.server;
    }
}
