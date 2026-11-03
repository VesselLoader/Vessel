package net.neoforged.neoforge.common;

import net.neoforged.bus.api.IEventBus;

/**
 * Global NeoForge runtime bus holder ({@code net.neoforged.neoforge.common.NeoForge})
 * exposing {@link #EVENT_BUS} for game-wide gameplay and server lifecycle events.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public final class NeoForge {

    /**
     * Global NeoForge gameplay and server event bus.
     */
    public static final IEventBus EVENT_BUS = new IEventBus();

    private NeoForge() {
    }
}
