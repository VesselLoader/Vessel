package dev.vessel.command;

import dev.vessel.bootstrap.target.VesselServerLauncher;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Bridges the entire VESSEL Sovereign command suite ({@code /vessel}, {@code /geyser},
 * {@code /floodgate}, {@code /spark}, and {@code /restart}) into Minecraft's native Brigadier
 * {@code CommandDispatcher<CommandSourceStack>} so that in-game players with operator permissions
 * have full tab-completion, syntax highlighting, and immediate execution parity with the console.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public final class VesselBrigadierCommandBridge {

    public static final String[] ROOT_COMMANDS = new String[]{
            "vessel", "geyser", "floodgate", "fwhitelist", "spark", "restart"
    };

    private volatile boolean commandsRegistered;

    /**
     * Polls and registers the VESSEL command suite into the live Minecraft server's Brigadier
     * dispatcher in the background as soon as {@code MinecraftServer} finishes startup.
     *
     * @param tierOneClassLoader active Tier-1 Minecraft classloader
     * @param launcher            active launcher instance
     */
    public void scheduleBackgroundBrigadierRegistration(
            ClassLoader tierOneClassLoader,
            VesselServerLauncher launcher) {
        Objects.requireNonNull(tierOneClassLoader, "tierOneClassLoader must not be null");
        Objects.requireNonNull(launcher, "launcher must not be null");

        Thread registrationThread = new Thread(
                new VesselBrigadierRegistrationTask(tierOneClassLoader, launcher, this),
                "vessel-brigadier-registration");
        registrationThread.setDaemon(true);
        registrationThread.start();
    }

    /**
     * Attempts to register all VESSEL commands into the given MinecraftServer or Commands instance.
     *
     * @param serverOrCommands   the running {@code MinecraftServer} or {@code Commands} instance
     * @param tierOneClassLoader the Tier-1 classloader containing Minecraft classes
     * @param launcher           the active launcher instance
     * @return {@code true} if commands were successfully registered into Brigadier
     */
    public boolean registerIntoBrigadier(
            Object serverOrCommands,
            ClassLoader tierOneClassLoader,
            VesselServerLauncher launcher) {
        if (serverOrCommands == null || tierOneClassLoader == null) {
            return false;
        }
        try {
            Object dispatcher = extractDispatcher(serverOrCommands);
            if (dispatcher == null) {
                return false;
            }

            Class<?> literalClass = Class.forName(
                    "com.mojang.brigadier.builder.LiteralArgumentBuilder",
                    true,
                    tierOneClassLoader);
            Method literalMethod = literalClass.getMethod("literal", String.class);
            Class<?> commandInterface = Class.forName(
                    "com.mojang.brigadier.Command",
                    true,
                    tierOneClassLoader);
            Class<?> argumentBuilderClass = Class.forName(
                    "com.mojang.brigadier.builder.ArgumentBuilder",
                    true,
                    tierOneClassLoader);
            Method executesMethod = argumentBuilderClass.getMethod("executes", commandInterface);
            Method registerMethod = dispatcher.getClass().getMethod(
                    "register",
                    literalClass);

            VesselBrigadierCommandInvocationHandler handler =
                    new VesselBrigadierCommandInvocationHandler(launcher, tierOneClassLoader);
            Object commandProxy = Proxy.newProxyInstance(
                    tierOneClassLoader,
                    new Class<?>[]{commandInterface},
                    handler);

            for (String rootName : ROOT_COMMANDS) {
                Object builder = literalMethod.invoke(null, rootName);
                builder = executesMethod.invoke(builder, commandProxy);

                // Add subcommands for tab-completion
                List<String> subcommands = getSubcommandsForRoot(rootName);
                for (String sub : subcommands) {
                    Object subBuilder = literalMethod.invoke(null, sub);
                    subBuilder = executesMethod.invoke(subBuilder, commandProxy);
                    Method thenMethod = argumentBuilderClass.getMethod("then", argumentBuilderClass);
                    builder = thenMethod.invoke(builder, subBuilder);
                }

                registerMethod.invoke(dispatcher, builder);
            }

            commandsRegistered = true;
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    /**
     * Returns whether the commands have been registered into the live server's Brigadier dispatcher.
     *
     * @return {@code true} if registered
     */
    public boolean isCommandsRegistered() {
        return commandsRegistered;
    }

    private static Object extractDispatcher(Object target) {
        try {
            // Case 1: Target is already CommandDispatcher
            if (target.getClass().getName().contains("CommandDispatcher")) {
                return target;
            }
            // Case 2: Target is Commands (net.minecraft.commands.Commands)
            for (Method m : target.getClass().getMethods()) {
                if (m.getParameterCount() == 0
                        && m.getReturnType().getName().contains("CommandDispatcher")) {
                    return m.invoke(target);
                }
            }
            // Case 3: Target is MinecraftServer
            for (Method m : target.getClass().getMethods()) {
                if (m.getParameterCount() == 0
                        && m.getReturnType().getName().endsWith("Commands")) {
                    Object commandsObj = m.invoke(target);
                    if (commandsObj != null) {
                        return extractDispatcher(commandsObj);
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private static List<String> getSubcommandsForRoot(String rootName) {
        return switch (rootName.toLowerCase(Locale.ROOT)) {
            case "vessel" -> List.of(
                    "version", "status", "tps", "mods", "plugins", "top", "profile",
                    "heap", "chunks", "entities", "pregen", "reload", "dump", "help");
            case "geyser" -> List.of("version", "list", "status", "reload", "dump", "help");
            case "floodgate" -> List.of("version", "whitelist", "help");
            case "spark" -> List.of("tps", "health", "profiler", "heapsummary", "help");
            default -> List.of();
        };
    }
}
