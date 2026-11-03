package net.fabricmc.fabric.api.command.v2;

/**
 * Canonical Fabric Command API v2 command registration handler contract
 * ({@code net.fabricmc.fabric.api.command.v2.CommandRegistrationHandler}).
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
@FunctionalInterface
public interface CommandRegistrationHandler {

    /**
     * Invoked when server commands are being registered.
     *
     * @param dispatcher      active command dispatcher instance
     * @param registryAccess  active command registry access handle
     * @param dedicatedServer {@code true} if registering on a dedicated server
     */
    void register(Object dispatcher, Object registryAccess, boolean dedicatedServer);
}
