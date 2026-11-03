package net.neoforged.fml.event.lifecycle;

/**
 * NeoForge FML mod construction lifecycle event ({@code net.neoforged.fml.event.lifecycle.FMLConstructModEvent})
 * fired immediately after a {@code @Mod} entrypoint instance is constructed.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public class FMLConstructModEvent extends ModLifecycleEvent {

    /**
     * Constructs a new {@code FMLConstructModEvent} associated with the default {@code "minecraft"} namespace.
     */
    public FMLConstructModEvent() {
        super();
    }

    /**
     * Constructs a new {@code FMLConstructModEvent} for the specified mod identifier.
     *
     * @param modId target mod identifier
     */
    public FMLConstructModEvent(String modId) {
        super(modId);
    }
}
