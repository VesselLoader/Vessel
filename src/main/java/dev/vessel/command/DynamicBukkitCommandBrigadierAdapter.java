package dev.vessel.command;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.SimpleCommandMap;

/**
 * Scans the live Bukkit {@link SimpleCommandMap} and dynamically binds each registered plugin
 * command and alias into Minecraft's native Brigadier {@code CommandDispatcher} using greedy
 * argument parsing and bidirectional sender proxying.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.1
 */
public final class DynamicBukkitCommandBrigadierAdapter {

    private final BrigadierDispatcherExtractor dispatcherExtractor = new BrigadierDispatcherExtractor();

    /**
     * Registers all known Bukkit plugin commands into the given Brigadier {@code dispatcher}.
     *
     * @param dispatcher          live Brigadier CommandDispatcher or MinecraftServer/Commands instance
     * @param tierOneClassLoader active Tier-1 classloader containing Minecraft/Mojang classes
     * @return count of root command literals successfully registered
     */
    public int registerAllBukkitCommandsIntoBrigadier(
            Object dispatcher,
            ClassLoader tierOneClassLoader) {
        Objects.requireNonNull(dispatcher, "dispatcher must not be null");
        Objects.requireNonNull(tierOneClassLoader, "tierOneClassLoader must not be null");

        Object targetDispatcher = dispatcherExtractor.extractDispatcher(dispatcher);
        if (targetDispatcher == null) {
            return 0;
        }

        SimpleCommandMap commandMap = Bukkit.getCommandMap();
        if (commandMap == null) {
            return 0;
        }

        Map<String, Command> known = commandMap.getKnownCommands();
        if (known.isEmpty()) {
            return 0;
        }

        int registeredCount = 0;
        try {
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
            Class<?> requiredBuilderClass = Class.forName(
                    "com.mojang.brigadier.builder.RequiredArgumentBuilder",
                    true,
                    tierOneClassLoader);
            Class<?> argumentTypeClass = Class.forName(
                    "com.mojang.brigadier.arguments.ArgumentType",
                    true,
                    tierOneClassLoader);
            Class<?> stringArgTypeClass = Class.forName(
                    "com.mojang.brigadier.arguments.StringArgumentType",
                    true,
                    tierOneClassLoader);
            Method greedyStringMethod = stringArgTypeClass.getMethod("greedyString");
            Object greedyStringInstance = greedyStringMethod.invoke(null);

            Method argumentMethod = requiredBuilderClass.getMethod("argument", String.class, argumentTypeClass);
            Method executesMethod = argumentBuilderClass.getMethod("executes", commandInterface);
            Method thenMethod = argumentBuilderClass.getMethod("then", argumentBuilderClass);
            Method registerMethod = targetDispatcher.getClass().getMethod("register", literalClass);

            Set<String> processedLabels = new HashSet<>();
            for (Map.Entry<String, Command> entry : known.entrySet()) {
                String label = entry.getKey();
                Command cmd = entry.getValue();
                if (label == null || label.isBlank() || cmd == null) {
                    continue;
                }
                // Skip namespace duplicates like "chunky:chunky" if already registered or register both
                if (!processedLabels.add(label)) {
                    continue;
                }

                BukkitToBrigadierCommandProxy proxyHandler =
                        new BukkitToBrigadierCommandProxy(cmd, tierOneClassLoader);
                Object commandProxy = Proxy.newProxyInstance(
                        tierOneClassLoader,
                        new Class<?>[]{commandInterface},
                        proxyHandler);

                Object builder = literalMethod.invoke(null, label);
                builder = executesMethod.invoke(builder, commandProxy);

                // Add greedy string argument to accept arbitrary subarguments without Brigadier syntax rejection
                Object greedyArgBuilder = argumentMethod.invoke(null, "args", greedyStringInstance);
                greedyArgBuilder = executesMethod.invoke(greedyArgBuilder, commandProxy);
                builder = thenMethod.invoke(builder, greedyArgBuilder);

                registerMethod.invoke(targetDispatcher, builder);
                registeredCount++;
            }
        } catch (Exception e) {
            System.err.println("[VESSEL] Failed to register dynamic Bukkit commands into Brigadier: " + e.getMessage());
        }
        return registeredCount;
    }
}
