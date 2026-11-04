package dev.vessel.command;

import java.lang.reflect.Method;
import java.util.Objects;

/**
 * Robust reflection extractor that resolves the native Brigadier {@code CommandDispatcher}
 * from various Minecraft server runtime containers (such as {@code MinecraftServer},
 * {@code DedicatedServer}, {@code Commands}, or the raw dispatcher itself) across all Minecraft eras.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public final class BrigadierDispatcherExtractor {

    /**
     * Extracts the underlying Brigadier {@code CommandDispatcher} from the given candidate object.
     *
     * @param target candidate runtime object (server, commands holder, or dispatcher)
     * @return live Brigadier {@code CommandDispatcher} instance, or {@code null} if unresolved
     */
    public Object extractDispatcher(Object target) {
        if (target == null) {
            target = VesselServerBridgeRegistry.getActiveCommands();
            if (target == null) {
                target = VesselServerBridgeRegistry.getActiveMinecraftServer();
            }
        }
        if (target == null) {
            return null;
        }

        try {
            // Case 1: Target is already CommandDispatcher
            if (target.getClass().getName().contains("CommandDispatcher")) {
                return target;
            }

            // Case 2: Target has a zero-arg method returning a CommandDispatcher
            for (Method m : target.getClass().getMethods()) {
                if (m.getParameterCount() == 0
                        && m.getReturnType().getName().contains("CommandDispatcher")) {
                    m.setAccessible(true);
                    Object res = m.invoke(target);
                    if (res != null) {
                        return res;
                    }
                }
            }

            // Case 3: Target is MinecraftServer / DedicatedServer with a method returning Commands
            for (Method m : target.getClass().getMethods()) {
                if (m.getParameterCount() == 0
                        && m.getReturnType().getName().endsWith("Commands")) {
                    m.setAccessible(true);
                    Object commandsObj = m.invoke(target);
                    if (commandsObj != null) {
                        Object subDispatcher = extractDispatcher(commandsObj);
                        if (subDispatcher != null) {
                            return subDispatcher;
                        }
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }
}
