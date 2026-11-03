package net.minecraftforge.eventbus.api;

/**
 * Canonical Minecraft Forge event listener priority tiers
 * ({@code net.minecraftforge.eventbus.api.EventPriority}).
 *
 * <p>In Forge's event bus model, {@link #HIGHEST} executes first and {@link #LOWEST}
 * executes last.</p>
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public enum EventPriority {
    HIGHEST,
    HIGH,
    NORMAL,
    LOW,
    LOWEST
}
