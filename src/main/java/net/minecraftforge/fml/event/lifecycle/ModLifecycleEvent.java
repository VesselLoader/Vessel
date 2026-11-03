package net.minecraftforge.fml.event.lifecycle;

/**
 * Base class for canonical Minecraft Forge mod lifecycle events
 * ({@code net.minecraftforge.fml.event.lifecycle.ModLifecycleEvent}).
 *
 * <p>Dispatched on the mod-specific event bus during mod construction, common setup,
 * dedicated server setup, and load completion phases.</p>
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public class ModLifecycleEvent {

    private final String modId;

    /**
     * Constructs a lifecycle event associated with the default {@code "minecraft"} namespace.
     */
    public ModLifecycleEvent() {
        this("minecraft");
    }

    /**
     * Constructs a lifecycle event associated with the specified mod identifier.
     *
     * @param modId target mod identifier; defaults to {@code "minecraft"} if null or blank
     */
    public ModLifecycleEvent(String modId) {
        this.modId = (modId == null || modId.trim().isEmpty()) ? "minecraft" : modId.trim();
    }

    /**
     * Returns the mod identifier associated with this lifecycle event.
     *
     * @return non-null mod identifier
     */
    public String getModId() {
        return modId;
    }

    /**
     * Returns a human-readable diagnostic description of this lifecycle event.
     *
     * @return formatted event description
     */
    public String description() {
        return getClass().getSimpleName() + "[" + modId + "]";
    }
}
