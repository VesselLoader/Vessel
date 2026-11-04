package dev.vessel.command;

import java.lang.reflect.Method;
import java.util.Objects;

/**
 * Executes command strings directly on the active MinecraftServer Brigadier engine
 * using the server console's {@code CommandSourceStack}.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.1
 */
public final class BrigadierCommandExecutionBridge {

    public BrigadierCommandExecutionBridge() {
    }

    /**
     * Dispatches a raw console command string into the running MinecraftServer Commands dispatcher.
     *
     * @param commandLine raw command string (with or without leading slash)
     * @return {@code true} if successfully dispatched to MinecraftServer; {@code false} otherwise
     */
    public boolean executeServerCommand(String commandLine) {
        if (commandLine == null || commandLine.isBlank()) {
            return false;
        }
        Object server = VesselServerBridgeRegistry.getActiveMinecraftServer();
        if (server == null) {
            return false;
        }
        String clean = commandLine.trim();
        String commandWithSlash = clean.startsWith("/") ? clean : "/" + clean;
        String commandWithoutSlash = clean.startsWith("/") ? clean.substring(1) : clean;

        if ("stop".equalsIgnoreCase(commandWithoutSlash)) {
            try {
                for (Method m : server.getClass().getMethods()) {
                    if ("halt".equals(m.getName()) && m.getParameterCount() == 1) {
                        m.invoke(server, false);
                        System.out.println("[VESSEL] Server halt signaled via console command.");
                        return true;
                    }
                }
            } catch (Throwable t) {
                System.err.println("[VESSEL] Warning: Failed to invoke halt(): " + t.getMessage());
            }
        }

        try {
            Method createStack = null;
            for (Method m : server.getClass().getMethods()) {
                if ("createCommandSourceStack".equals(m.getName()) && m.getParameterCount() == 0) {
                    createStack = m;
                    break;
                }
            }
            if (createStack == null) {
                System.err.println("[VESSEL] Warning: createCommandSourceStack() not found on server " + server.getClass().getName());
                return false;
            }
            Object consoleSource = createStack.invoke(server);

            Object commands = VesselServerBridgeRegistry.getActiveCommands();
            if (commands == null) {
                Method getCommands = null;
                for (Method m : server.getClass().getMethods()) {
                    if ("getCommands".equals(m.getName()) && m.getParameterCount() == 0) {
                        getCommands = m;
                        break;
                    }
                }
                if (getCommands != null) {
                    commands = getCommands.invoke(server);
                }
            }
            if (commands == null) {
                System.err.println("[VESSEL] Warning: Commands instance not found on server");
                return false;
            }

            for (Method m : commands.getClass().getMethods()) {
                if ("performPrefixedCommand".equals(m.getName()) && m.getParameterCount() == 2) {
                    System.out.println("[VESSEL] Executing via performPrefixedCommand: " + commandWithoutSlash);
                    m.invoke(commands, consoleSource, commandWithoutSlash);
                    return true;
                }
            }
            for (Method m : commands.getClass().getMethods()) {
                if ("performCommand".equals(m.getName()) && m.getParameterCount() == 2
                        && m.getParameterTypes()[1] == String.class) {
                    System.out.println("[VESSEL] Executing via performCommand: " + commandWithSlash);
                    m.invoke(commands, consoleSource, commandWithSlash);
                    return true;
                }
            }
            System.err.println("[VESSEL] Warning: No matching perform method found on Commands");
            return false;
        } catch (Throwable t) {
            System.err.println("[VESSEL] Notice: Failed to dispatch command '" + clean + "': " + t.getMessage());
            t.printStackTrace();
            return false;
        }
    }
}
