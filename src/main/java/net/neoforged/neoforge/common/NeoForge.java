package net.neoforged.neoforge.common;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.VesselNeoEventBus;

/**
 * Global NeoForge runtime bus holder ({@code net.neoforged.neoforge.common.NeoForge})
 * exposing {@link #EVENT_BUS} for game-wide gameplay and server lifecycle events.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.1
 */
public final class NeoForge {

    /**
     * Global NeoForge gameplay and server event bus.
     */
    public static final IEventBus EVENT_BUS = new VesselNeoEventBus();

    private NeoForge() {
    }
}
