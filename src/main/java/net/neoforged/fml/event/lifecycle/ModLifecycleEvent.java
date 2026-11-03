package net.neoforged.fml.event.lifecycle;

/**
 * Base NeoForge FML mod lifecycle event ({@code net.neoforged.fml.event.lifecycle.ModLifecycleEvent})
 * dispatched on a mod's {@code IEventBus} during container initialization and setup phases.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public class ModLifecycleEvent {

    private final String modId;

    /**
     * Constructs a new {@code ModLifecycleEvent} associated with the default {@code "minecraft"} namespace.
     */
    public ModLifecycleEvent() {
        this("minecraft");
    }

    /**
     * Constructs a new {@code ModLifecycleEvent} for the specified mod identifier.
     *
     * @param modId target mod identifier; defaults to {@code "minecraft"} if {@code null} or blank
     */
    public ModLifecycleEvent(String modId) {
        this.modId = (modId == null || modId.isBlank()) ? "minecraft" : modId.trim();
    }

    /**
     * Returns the mod identifier receiving this lifecycle event.
     *
     * @return non-null mod identifier
     */
    public String getModId() {
        return modId;
    }

    /**
     * Returns a human-readable diagnostic description of this lifecycle event.
     *
     * @return formatted event description {@code ClassName[modId]}
     */
    public String description() {
        return getClass().getSimpleName() + "[" + modId + "]";
    }
}
