package net.fabricmc.fabric.api.command.v2;

import java.util.List;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;

/**
 * Canonical Fabric Command API v2 command registration event callback holder
 * ({@code net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback}).
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public final class CommandRegistrationCallback {

    /**
     * Event fired when the server command tree is being constructed or reloaded.
     */
    public static final Event<CommandRegistrationHandler> EVENT =
            EventFactory.createArrayBacked(
                    CommandRegistrationHandler.class,
                    CommandRegistrationCallback::createCompositeHandler);

    private CommandRegistrationCallback() {
    }

    private static CommandRegistrationHandler createCompositeHandler(
            List<CommandRegistrationHandler> handlers) {
        return (dispatcher, registryAccess, dedicatedServer) -> {
            for (CommandRegistrationHandler h : handlers) {
                h.register(dispatcher, registryAccess, dedicatedServer);
            }
        };
    }
}
