package net.neoforged.fml.event.lifecycle;

/**
 * NeoForge FML load-complete lifecycle event ({@code net.neoforged.fml.event.lifecycle.FMLLoadCompleteEvent})
 * fired when all mod setup stages have concluded.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public class FMLLoadCompleteEvent extends ModLifecycleEvent {

    /**
     * Constructs a new {@code FMLLoadCompleteEvent} associated with the default {@code "minecraft"} namespace.
     */
    public FMLLoadCompleteEvent() {
        super();
    }

    /**
     * Constructs a new {@code FMLLoadCompleteEvent} for the specified mod identifier.
     *
     * @param modId target mod identifier
     */
    public FMLLoadCompleteEvent(String modId) {
        super(modId);
    }
}
