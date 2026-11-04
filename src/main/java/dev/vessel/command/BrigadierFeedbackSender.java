package dev.vessel.command;

import com.mojang.brigadier.context.CommandContext;
import java.lang.reflect.Method;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * Sends command feedback directly to the command source encapsulated in a Brigadier {@link CommandContext}.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.0
 */
public final class BrigadierFeedbackSender {

    public BrigadierFeedbackSender() {
    }

    /**
     * Emits a message back to the initiator of the given Brigadier command context.
     *
     * @param context active command context
     * @param message text message to output
     */
    public void sendFeedback(CommandContext<?> context, String message) {
        if (message == null) {
            return;
        }
        if (context == null) {
            System.out.println(message);
            return;
        }

        Object source;
        try {
            source = context.getSource();
        } catch (Throwable t) {
            System.out.println(message);
            return;
        }

        if (source == null) {
            System.out.println(message);
            return;
        }

        try {
            ClassLoader loader = source.getClass().getClassLoader();
            if (loader == null) {
                loader = getClass().getClassLoader();
            }
            Class<?> componentClass = Class.forName("net.minecraft.network.chat.Component", true, loader);
            Method literalMethod = componentClass.getMethod("literal", String.class);
            Object component = literalMethod.invoke(null, message);

            for (Method m : source.getClass().getMethods()) {
                if ("sendSuccess".equals(m.getName()) && m.getParameterCount() == 2) {
                    Class<?> p1 = m.getParameterTypes()[0];
                    if (Supplier.class.isAssignableFrom(p1)) {
                        VesselComponentSupplier supplier = VesselComponentSupplier.of(component);
                        m.invoke(source, supplier, false);
                        return;
                    } else if (p1.isAssignableFrom(componentClass)) {
                        m.invoke(source, component, false);
                        return;
                    }
                }
            }
            for (Method m : source.getClass().getMethods()) {
                if ("sendSystemMessage".equals(m.getName()) && m.getParameterCount() == 1
                        && m.getParameterTypes()[0].isAssignableFrom(componentClass)) {
                    m.invoke(source, component);
                    return;
                }
            }
        } catch (Throwable ignored) {
        }

        System.out.println(message);
    }
}
