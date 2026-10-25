package dev.vessel.command;

import java.lang.reflect.Method;
import java.util.List;

/**
 * Thread-safe global registry retaining singleton references to the running Minecraft server
 * instance, its Commands dispatcher, and connected player list across ClassLoader boundaries.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.1
 */
public final class VesselServerBridgeRegistry {

    private static volatile Object activeMinecraftServer;
    private static volatile Object activeCommands;
    private static volatile Object activeLegacyCommandManager;

    private VesselServerBridgeRegistry() {
    }

    /**
     * Stores the active running {@code net.minecraft.server.MinecraftServer} instance.
     *
     * @param server the running server instance
     */
    public static void setActiveMinecraftServer(Object server) {
        activeMinecraftServer = server;
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
            String cName = commands.getClass().getName();
            if (cName.contains("CommandManager") || cName.contains("CommandHandler")) {
                activeLegacyCommandManager = commands;
            }
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
}
