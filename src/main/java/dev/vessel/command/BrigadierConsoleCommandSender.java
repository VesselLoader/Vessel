package dev.vessel.command;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Supplier;
import org.bukkit.command.CommandSender;

/**
 * Live {@link CommandSender} adapter wrapping a native Minecraft {@code CommandSourceStack}
 * to bridge Bukkit command feedback ({@link #sendMessage(String)}) directly back to the active
 * console, rcon, or player command session.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.1
 */
public final class BrigadierConsoleCommandSender implements CommandSender {

    private final Object commandSourceStack;
    private final ClassLoader tierOneClassLoader;
    private final List<String> sentMessages = new CopyOnWriteArrayList<>();

    public BrigadierConsoleCommandSender(Object commandSourceStack, ClassLoader tierOneClassLoader) {
        this.commandSourceStack = commandSourceStack;
        this.tierOneClassLoader = tierOneClassLoader;
    }

    @Override
    public String getName() {
        return "CONSOLE";
    }

    @Override
    public void sendMessage(String message) {
        if (message == null) {
            return;
        }
        sentMessages.add(message);
        System.out.println(message);
        System.out.flush();
        if (commandSourceStack != null && tierOneClassLoader != null) {
            try {
                Class<?> componentClass = Class.forName(
                        "net.minecraft.network.chat.Component",
                        true,
                        tierOneClassLoader);
                Method literalMethod = componentClass.getMethod("literal", String.class);
                Object component = literalMethod.invoke(null, message);

                for (Method m : commandSourceStack.getClass().getMethods()) {
                    if ("sendSuccess".equals(m.getName()) && m.getParameterCount() == 2) {
                        Class<?> firstParam = m.getParameterTypes()[0];
                        if (Supplier.class.isAssignableFrom(firstParam)) {
                            VesselComponentSupplier supplier = new VesselComponentSupplier(component);
                            m.invoke(commandSourceStack, supplier, false);
                            return;
                        } else if (firstParam.isAssignableFrom(componentClass)) {
                            m.invoke(commandSourceStack, component, false);
                            return;
                        }
                    }
                }
            } catch (Exception ignored) {
            }
        }
    }

    @Override
    public void sendMessage(String[] messages) {
        if (messages != null) {
            for (String m : messages) {
                sendMessage(m);
            }
        }
    }

    @Override
    public boolean hasPermission(String name) {
        return true;
    }

    @Override
    public boolean isOp() {
        return true;
    }

    public List<String> getSentMessages() {
        return Collections.unmodifiableList(sentMessages);
    }
}
