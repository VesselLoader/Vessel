package net.minecraftforge.fml.event.lifecycle;

/**
 * Canonical Minecraft Forge mod construction lifecycle event
 * ({@code net.minecraftforge.fml.event.lifecycle.FMLConstructModEvent}).
 *
 * <p>Fired on the mod event bus immediately after the {@code @Mod} entrypoint class
 * is instantiated.</p>
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public class FMLConstructModEvent extends ModLifecycleEvent {

    /**
     * Constructs a mod construction lifecycle event for the default namespace.
     */
    public FMLConstructModEvent() {
        super();
    }

    /**
     * Constructs a mod construction lifecycle event for the specified mod identifier.
     *
     * @param modId target mod identifier
     */
    public FMLConstructModEvent(String modId) {
        super(modId);
    }
}
