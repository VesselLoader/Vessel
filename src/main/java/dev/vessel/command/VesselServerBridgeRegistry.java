package dev.vessel.command;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Thread-safe global registry retaining singleton references to the running Minecraft server
 * instance, its Commands dispatcher, connected player list, and server-ready lifecycle callbacks.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.5
 */
public final class VesselServerBridgeRegistry {

    private static volatile Object activeMinecraftServer;
    private static volatile Object activeCommands;
    private static volatile Object activeLegacyCommandManager;
    private static volatile ClassLoader activeModClassLoader;
    private static final List<VesselServerReadyCallback> READY_CALLBACKS = new CopyOnWriteArrayList<>();

    private VesselServerBridgeRegistry() {
    }

    /**
     * Registers a callback to be invoked immediately when the server or command dispatcher is ready.
     * If already ready, the callback is executed immediately.
     *
     * @param callback non-null ready callback
     */
    public static void registerServerReadyCallback(VesselServerReadyCallback callback) {
        Objects.requireNonNull(callback, "callback must not be null");
        READY_CALLBACKS.add(callback);
        Object target = activeMinecraftServer != null ? activeMinecraftServer : activeCommands;
        if (target != null) {
            try {
                callback.onServerOrCommandsReady(target);
            } catch (Throwable t) {
                System.err.println("[VESSEL/ERROR] Error in server ready callback: " + t.getMessage());
            }
        }
    }

    /**
     * Removes a previously registered server ready callback.
     *
     * @param callback callback to remove
     */
    public static void unregisterServerReadyCallback(VesselServerReadyCallback callback) {
        if (callback != null) {
            READY_CALLBACKS.remove(callback);
        }
    }

    /**
     * Stores the active transforming classloader hosting loaded mod and plugin classes.
     *
     * @param loader active mod classloader
     */
    public static void setActiveModClassLoader(ClassLoader loader) {
        activeModClassLoader = loader;
    }

    /**
     * Returns the active transforming classloader hosting loaded mod and plugin classes, or {@code null}.
     *
     * @return active mod classloader
     */
    public static ClassLoader getActiveModClassLoader() {
        return activeModClassLoader;
    }

    /**
     * Stores the active running {@code net.minecraft.server.MinecraftServer} instance.
     *
     * @param server the running server instance
     */
    public static void setActiveMinecraftServer(Object server) {
        activeMinecraftServer = server;
        if (server != null) {
            System.out.println("[VESSEL] Registered active MinecraftServer instance: " + server.getClass().getName());
            new dev.vessel.kernel.lifecycle.CrossEraServerLifecycleSyncHook().onServerStarting(server);
            dispatchReadyCallbacks(server);
        }
    }

    /**
     * Returns the active running {@code net.minecraft.server.MinecraftServer} instance, or {@code null}.
     *
     * @return the running server instance
     */
    public static Object getActiveMinecraftServer() {
        return activeMinecraftServer;
    }

    /**
     * Stores the active running {@code net.minecraft.commands.Commands} or {@code ServerCommandManager} instance.
     *
     * @param commands the running Commands instance
     */
    public static void setActiveCommands(Object commands) {
        activeCommands = commands;
        if (commands != null) {
            System.out.println("[VESSEL] Registered active Commands instance: " + commands.getClass().getName());
            String cName = commands.getClass().getName();
            if (cName.contains("CommandManager") || cName.contains("CommandHandler")) {
                activeLegacyCommandManager = commands;
            } else {
                ClassLoader cl = commands.getClass().getClassLoader();
                if (cl != null) {
                    try {
                        new DynamicBukkitCommandBrigadierAdapter().registerAllBukkitCommandsIntoBrigadier(commands, cl);
                        new ForgeDeferredCommandRegistrationBridge().dispatchRegisterCommandsEvent(commands, cl);
                    } catch (Throwable ignored) {
                    }
                }
            }
            dispatchReadyCallbacks(commands);
        }
    }

    /**
     * Returns the active running {@code net.minecraft.commands.Commands} instance, or {@code null}.
     *
     * @return the running Commands instance
     */
    public static Object getActiveCommands() {
        return activeCommands;
    }

    /**
     * Stores the active running legacy pre-1.13 command manager instance.
     *
     * @param manager legacy command manager
     */
    public static void setActiveLegacyCommandManager(Object manager) {
        activeLegacyCommandManager = manager;
    }

    /**
     * Returns the active running legacy pre-1.13 command manager instance, or {@code null}.
     *
     * @return legacy command manager
     */
    public static Object getActiveLegacyCommandManager() {
        return activeLegacyCommandManager;
    }

    /**
     * Synchronously broadcasts the updated Brigadier command syntax tree to all connected players
     * by invoking {@code Commands#sendCommands(ServerPlayer)}.
     *
     * @return count of connected players notified
     */
    public static int sendCommandsToAllPlayers() {
        Object server = activeMinecraftServer;
        Object commands = activeCommands;
        if (server == null || commands == null) {
            return 0;
        }

        try {
            Method getPlayerListMethod = null;
            for (Method m : server.getClass().getMethods()) {
                if (m.getParameterCount() == 0 && m.getReturnType().getName().contains("PlayerList")) {
                    getPlayerListMethod = m;
                    break;
                }
            }
            if (getPlayerListMethod == null) {
                return 0;
            }

            Object playerList = getPlayerListMethod.invoke(server);
            if (playerList == null) {
                return 0;
            }

            Method getPlayersMethod = null;
            for (Method m : playerList.getClass().getMethods()) {
                if (m.getParameterCount() == 0 && List.class.isAssignableFrom(m.getReturnType())) {
                    getPlayersMethod = m;
                    break;
                }
            }
            if (getPlayersMethod == null) {
                return 0;
            }

            Object playersObj = getPlayersMethod.invoke(playerList);
            if (!(playersObj instanceof List<?> players) || players.isEmpty()) {
                return 0;
            }

            Method sendCommandsMethod = null;
            for (Method m : commands.getClass().getMethods()) {
                if ("sendCommands".equals(m.getName()) && m.getParameterCount() == 1) {
                    sendCommandsMethod = m;
                    break;
                }
            }
            if (sendCommandsMethod == null) {
                return 0;
            }

            int count = 0;
            for (Object player : players) {
                if (player != null) {
                    try {
                        sendCommandsMethod.invoke(commands, player);
                        count++;
                    } catch (Exception ignored) {
                    }
                }
            }
            return count;
        } catch (Exception ignored) {
            return 0;
        }
    }

    private static void dispatchReadyCallbacks(Object serverOrCommands) {
        if (serverOrCommands == null) {
            return;
        }
        for (VesselServerReadyCallback cb : READY_CALLBACKS) {
            try {
                cb.onServerOrCommandsReady(serverOrCommands);
            } catch (Throwable t) {
                System.err.println("[VESSEL/ERROR] Error dispatching server ready callback: " + t.getMessage());
            }
        }
    }
}
