package net.minecraftforge.fml.event.lifecycle;

/**
 * Canonical Minecraft Forge load completion lifecycle event
 * ({@code net.minecraftforge.fml.event.lifecycle.FMLLoadCompleteEvent}).
 *
 * <p>Fired on the mod event bus once all mod setup phases have completed.</p>
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public class FMLLoadCompleteEvent extends ModLifecycleEvent {

    /**
     * Constructs a load completion event for the default namespace.
     */
    public FMLLoadCompleteEvent() {
        super();
    }

    /**
     * Constructs a load completion event for the specified mod identifier.
     *
     * @param modId target mod identifier
     */
    public FMLLoadCompleteEvent(String modId) {
        super(modId);
    }
}
