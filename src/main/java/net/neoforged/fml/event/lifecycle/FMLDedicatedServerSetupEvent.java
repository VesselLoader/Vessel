package net.neoforged.fml.event.lifecycle;

/**
 * NeoForge FML dedicated server setup lifecycle event ({@code net.neoforged.fml.event.lifecycle.FMLDedicatedServerSetupEvent})
 * fired on dedicated server distributions after common setup finishes.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public class FMLDedicatedServerSetupEvent extends ModLifecycleEvent {

    /**
     * Constructs a new {@code FMLDedicatedServerSetupEvent} associated with the default {@code "minecraft"} namespace.
     */
    public FMLDedicatedServerSetupEvent() {
        super();
    }

    /**
     * Constructs a new {@code FMLDedicatedServerSetupEvent} for the specified mod identifier.
     *
     * @param modId target mod identifier
     */
    public FMLDedicatedServerSetupEvent(String modId) {
        super(modId);
    }
}
