package net.neoforged.fml;

import java.util.Objects;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.VesselNeoEventBus;

/**
 * NeoForge FML mod container ({@code net.neoforged.fml.ModContainer}) injectable into
 * {@code @Mod} constructors alongside or instead of {@link IEventBus}.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.1
 */
public final class ModContainer {

    private final String modId;
    private final IEventBus eventBus;

    /**
     * Constructs a new {@code ModContainer} for the specified mod identifier backed by a fresh
     * {@link IEventBus}.
     *
     * @param modId mod identifier
     */
    public ModContainer(String modId) {
        this(modId, new VesselNeoEventBus());
    }

    /**
     * Constructs a new {@code ModContainer} for the specified mod identifier and mod {@link IEventBus}.
     *
     * @param modId    mod identifier; defaults to {@code "minecraft"} if {@code null} or blank
     * @param eventBus non-null mod event bus
     */
    public ModContainer(String modId, IEventBus eventBus) {
        this.modId = (modId == null || modId.isBlank()) ? "minecraft" : modId.trim();
        this.eventBus = Objects.requireNonNull(eventBus, "eventBus must not be null");
    }

    /**
     * Returns the mod identifier of this container.
     *
     * @return non-null mod identifier
     */
    public String getModId() {
        return modId;
    }

    /**
     * Returns the mod-specific {@link IEventBus} owned by this container.
     *
     * @return non-null mod event bus
     */
    public IEventBus getEventBus() {
        return eventBus;
    }
}
