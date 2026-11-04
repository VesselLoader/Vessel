package dev.vessel.command;

import java.lang.reflect.Method;
import org.bukkit.command.CommandSender;

/**
 * Adapter converting a native Minecraft {@code CommandSourceStack} instance into a canonical
 * Bukkit {@link CommandSender}.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public final class BukkitBrigadierSenderAdapter {

    private final ClassLoader tierOneClassLoader;

    public BukkitBrigadierSenderAdapter(ClassLoader tierOneClassLoader) {
        this.tierOneClassLoader = tierOneClassLoader;
    }

    /**
     * Converts a raw command source object into a Bukkit {@link CommandSender}.
     *
     * @param source native {@code CommandSourceStack} or similar
     * @return adapted Bukkit command sender
     */
    public CommandSender adaptSender(Object source) {
        return new BrigadierConsoleCommandSender(source, tierOneClassLoader);
    }
}
