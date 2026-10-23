package dev.vessel.command;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Objects;

/**
 * Reflectively sends chat and feedback messages to Minecraft {@code ICommandSender} instances
 * across legacy pre-1.13 Minecraft versions (1.7.10 – 1.12.2), supporting both {@code TextComponentString}
 * (1.9 – 1.12.2) and {@code ChatComponentText} (1.7.10 – 1.8.9).
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes, and zero lambdas.</p>
 *
 * @version 1.0.0
 */
public final class VesselLegacyChatSender {

    private static final String[] TEXT_COMPONENT_CLASS_NAMES = new String[]{
            "net.minecraft.util.text.TextComponentString",
            "net.minecraft.util.ChatComponentText"
    };

    public VesselLegacyChatSender() {
    }

    /**
     * Sends a text message to the given command sender (player or console).
     *
     * @param sender             legacy {@code ICommandSender} instance
     * @param message            plain text message to send
     * @param tierOneClassLoader active game classloader
     */
    public void sendMessage(Object sender, String message, ClassLoader tierOneClassLoader) {
        if (sender == null || message == null || tierOneClassLoader == null) {
            return;
        }

        Object chatComponent = createChatComponent(message, tierOneClassLoader);
        if (chatComponent != null) {
            for (Method m : sender.getClass().getMethods()) {
                if (("sendMessage".equals(m.getName()) || "addChatMessage".equals(m.getName()))
                        && m.getParameterCount() == 1) {
                    try {
                        m.invoke(sender, chatComponent);
                        return;
                    } catch (Exception ignored) {
                    }
                }
            }
        }
    }

    private static Object createChatComponent(String text, ClassLoader classLoader) {
        for (String cName : TEXT_COMPONENT_CLASS_NAMES) {
            try {
                Class<?> compClass = Class.forName(cName, true, classLoader);
                Constructor<?> ctor = compClass.getConstructor(String.class);
                return ctor.newInstance(text);
            } catch (Exception ignored) {
            }
        }
        return null;
    }
}
