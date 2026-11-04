package dev.vessel.command;

/**
 * Lifecycle callback invoked immediately when the live Minecraft server or its Brigadier
 * {@code Commands} dispatcher completes initialization.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public interface VesselServerReadyCallback {

    /**
     * Called synchronously or asynchronously as soon as {@code MinecraftServer} or {@code Commands} is active.
     *
     * @param serverOrCommands the non-null active {@code MinecraftServer} or {@code Commands} instance
     */
    void onServerOrCommandsReady(Object serverOrCommands);
}
