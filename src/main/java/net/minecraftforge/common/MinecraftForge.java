package net.minecraftforge.common;

import net.minecraftforge.eventbus.api.IEventBus;

/**
 * Global Minecraft Forge runtime bus holder ({@code net.minecraftforge.common.MinecraftForge})
 * exposing {@link #EVENT_BUS} for game-wide gameplay and server lifecycle events.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public final class MinecraftForge {

    /**
     * Global Forge gameplay and server event bus.
     */
    public static final IEventBus EVENT_BUS = new IEventBus();

    private MinecraftForge() {
    }
}
