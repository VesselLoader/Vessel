package dev.vessel.command;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * Attaches a fallback command executor to a Brigadier command node using pure reflection
 * and dynamic proxies, completely decoupled from ClassLoader type boundaries.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.1
 */
public final class BrigadierCommandNodeAttacher {

    public BrigadierCommandNodeAttacher() {
    }

    /**
     * Attaches a fallback command to the named root child node if its command is currently unset.
     *
     * @param dispatcher Brigadier command dispatcher
     * @param nodeName   literal name of the node (e.g. "create", "mek")
     * @param executor   fallback executor to install
     * @return {@code true} if successfully attached; {@code false} otherwise
     */
    public boolean attachFallbackIfUnset(Object dispatcher, String nodeName, Object executor) {
        if (dispatcher == null || nodeName == null || executor == null) {
            return false;
        }

        try {
            Method getRootMethod = dispatcher.getClass().getMethod("getRoot");
            Object rootNode = getRootMethod.invoke(dispatcher);
            if (rootNode == null) {
                return false;
            }

            Method getChildMethod = rootNode.getClass().getMethod("getChild", String.class);
            Object targetNode = getChildMethod.invoke(rootNode, nodeName);
            if (targetNode == null) {
                return false;
            }

            Method getCommandMethod = targetNode.getClass().getMethod("getCommand");
            Object existingCommand = getCommandMethod.invoke(targetNode);
            if (existingCommand != null) {
                return true;
            }

            Field commandField = findCommandField(targetNode.getClass());
            if (commandField != null) {
                commandField.setAccessible(true);
                Class<?> commandInterface = commandField.getType();
                if (commandInterface.isInstance(executor)) {
                    commandField.set(targetNode, executor);
                    return true;
                } else if (commandInterface.isInterface()) {
                    CommandProxyInvocationHandler handler = new CommandProxyInvocationHandler(executor);
                    Object proxy = Proxy.newProxyInstance(
                            commandInterface.getClassLoader(),
                            new Class<?>[]{commandInterface},
                            handler);
                    commandField.set(targetNode, proxy);
                    return true;
                }
            }
        } catch (Throwable t) {
            System.err.println("[VESSEL] Notice: Failed to attach fallback executor to /" + nodeName + ": " + t.getMessage());
        }

        return false;
    }

    private Field findCommandField(Class<?> clazz) {
        Class<?> curr = clazz;
        while (curr != null && curr != Object.class) {
            try {
                Field f = curr.getDeclaredField("command");
                return f;
            } catch (NoSuchFieldException ignored) {
            }
            curr = curr.getSuperclass();
        }
        return null;
    }
}
