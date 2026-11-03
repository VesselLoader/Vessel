package net.minecraftforge.fml.event.lifecycle;

/**
 * Canonical Minecraft Forge dedicated server setup lifecycle event
 * ({@code net.minecraftforge.fml.event.lifecycle.FMLDedicatedServerSetupEvent}).
 *
 * <p>Fired on the mod event bus during dedicated server initialization after common setup.</p>
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public class FMLDedicatedServerSetupEvent extends ModLifecycleEvent {

    /**
     * Constructs a dedicated server setup event for the default namespace.
     */
    public FMLDedicatedServerSetupEvent() {
        super();
    }

    /**
     * Constructs a dedicated server setup event for the specified mod identifier.
     *
     * @param modId target mod identifier
     */
    public FMLDedicatedServerSetupEvent(String modId) {
        super(modId);
    }
}
