package dev.vessel.command;

import dev.vessel.bootstrap.target.VesselServerLauncher;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Bridges all VESSEL kernel and Bukkit player commands into legacy pre-1.13 Minecraft servers
 * (1.7.10 – 1.12.2) using the native {@code net.minecraft.command.ICommandManager} and
 * {@code net.minecraft.command.ICommand} interface.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes, and zero lambdas.</p>
 *
 * @version 1.0.0
 */
public final class VesselLegacyCommandBridge {

    private final VesselBukkitCommandBridge bukkitBridge = new VesselBukkitCommandBridge();
    private volatile boolean commandsRegistered;

    public VesselLegacyCommandBridge() {
    }

    /**
     * Determines whether the running server is a legacy pre-1.13 server (lacking Brigadier
     * but having {@code net.minecraft.command.ICommand}).
     *
     * @param tierOneClassLoader active game classloader
     * @return {@code true} if legacy command management is required
     */
    public boolean isLegacyEra(ClassLoader tierOneClassLoader) {
        if (tierOneClassLoader == null) {
            return false;
        }
        try {
            Class.forName("com.mojang.brigadier.CommandDispatcher", false, tierOneClassLoader);
            return false;
        } catch (ClassNotFoundException ignored) {
        }

        try {
            Class.forName("net.minecraft.command.ICommand", false, tierOneClassLoader);
            return true;
        } catch (ClassNotFoundException ignored) {
            return false;
        }
    }

    /**
     * Registers all VESSEL commands into the pre-1.13 {@code ICommandManager} or {@code ServerCommandManager}.
     *
     * @param serverOrManager    the {@code MinecraftServer} or {@code ICommandManager} instance
     * @param tierOneClassLoader active game classloader
     * @param launcher           active server launcher
     * @return {@code true} if commands were successfully registered
     */
    public boolean registerIntoLegacyCommandManager(
            Object serverOrManager,
            ClassLoader tierOneClassLoader,
            VesselServerLauncher launcher) {
        Objects.requireNonNull(tierOneClassLoader, "tierOneClassLoader must not be null");
        Objects.requireNonNull(launcher, "launcher must not be null");

        if (serverOrManager == null) {
            return false;
        }

        try {
            Class<?> iCommandClass = Class.forName("net.minecraft.command.ICommand", true, tierOneClassLoader);
            Object manager = extractCommandManager(serverOrManager);
            if (manager == null) {
                return false;
            }

            Method registerMethod = null;
            for (Method m : manager.getClass().getMethods()) {
                if ("registerCommand".equals(m.getName())
                        && m.getParameterCount() == 1
                        && m.getParameterTypes()[0].isAssignableFrom(iCommandClass)) {
                    registerMethod = m;
                    break;
                }
            }
            if (registerMethod == null) {
                return false;
            }

            bukkitBridge.initialize(launcher.getServerRoot());

            List<String> allCommands = new ArrayList<>();
            allCommands.addAll(List.of(VesselBrigadierCommandBridge.KERNEL_COMMANDS));
            allCommands.addAll(List.of(VesselBrigadierCommandBridge.BUKKIT_COMMANDS));

            for (String rootName : allCommands) {
                List<String> subcommands = getSubcommandsForRoot(rootName);
                VesselLegacyCommandInvocationHandler handler = new VesselLegacyCommandInvocationHandler(
                        rootName,
                        subcommands,
                        tierOneClassLoader,
                        launcher,
                        bukkitBridge);
                Object proxy = Proxy.newProxyInstance(
                        tierOneClassLoader,
                        new Class<?>[]{iCommandClass},
                        handler);
                registerMethod.invoke(manager, proxy);
            }

            commandsRegistered = true;
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    /**
     * Returns whether legacy commands were registered.
     *
     * @return {@code true} if registered
     */
    public boolean isCommandsRegistered() {
        return commandsRegistered;
    }

    private static Object extractCommandManager(Object target) {
        if (target == null) {
            return null;
        }

        for (Method m : target.getClass().getMethods()) {
            if ("registerCommand".equals(m.getName()) && m.getParameterCount() == 1) {
                return target;
            }
        }

        for (Method m : target.getClass().getMethods()) {
            if (m.getParameterCount() == 0
                    && ("getCommandManager".equals(m.getName()) || "getCommandHandler".equals(m.getName()))) {
                try {
                    Object res = m.invoke(target);
                    if (res != null) {
                        return res;
                    }
                } catch (Exception ignored) {
                }
            }
        }

        return null;
    }

    private static List<String> getSubcommandsForRoot(String rootName) {
        return switch (rootName.toLowerCase(java.util.Locale.ROOT)) {
            case "vessel" -> List.of(
                    "version", "status", "tps", "mods", "plugins", "top", "profile",
                    "heap", "chunks", "entities", "pregen", "reload", "dump", "help");
            case "geyser" -> List.of("version", "list", "status", "reload", "dump", "help");
            case "floodgate" -> List.of("version", "whitelist", "help");
            case "spark" -> List.of("tps", "health", "profiler", "heapsummary", "help");
            case "sethome", "home", "delhome" -> List.of("home", "base", "spawn");
            default -> List.of();
        };
    }
}
