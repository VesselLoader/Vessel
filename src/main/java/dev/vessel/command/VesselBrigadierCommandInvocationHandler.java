package dev.vessel.command;

import dev.vessel.bootstrap.target.VesselServerLauncher;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * Dynamic invocation handler implementing Brigadier's {@code Command#run(CommandContext)}
 * to execute VESSEL kernel commands and Bukkit/EssentialsX player commands from in-game chat or rcon
 * and stream responses back to the sender.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.1
 */
public final class VesselBrigadierCommandInvocationHandler implements InvocationHandler {

    private final VesselServerLauncher launcher;
    private final ClassLoader tierOneClassLoader;
    private final VesselBukkitCommandBridge bukkitBridge = new VesselBukkitCommandBridge();

    public VesselBrigadierCommandInvocationHandler(
            VesselServerLauncher launcher,
            ClassLoader tierOneClassLoader) {
        this.launcher = Objects.requireNonNull(launcher, "launcher must not be null");
        this.tierOneClassLoader = Objects.requireNonNull(tierOneClassLoader, "tierOneClassLoader must not be null");
        this.bukkitBridge.initialize(launcher.getServerRoot());
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        if ("run".equals(method.getName()) && args != null && args.length == 1) {
            Object context = args[0];
            String input = extractInput(context);
            Object source = extractSource(context);

            String trimmed = input.trim();
            String withoutSlash = trimmed.startsWith("/") ? trimmed.substring(1).trim() : trimmed;
            String[] tokens = withoutSlash.split("\\s+");
            String rootCmd = tokens.length > 0 ? tokens[0].toLowerCase(Locale.ROOT) : "vessel";
            String[] subArgs = tokens.length > 1 ? new String[tokens.length - 1] : new String[0];
            if (tokens.length > 1) {
                System.arraycopy(tokens, 1, subArgs, 0, subArgs.length);
            }

            if ("restart".equalsIgnoreCase(rootCmd)) {
                sendMessageToSource(source, "[VESSEL] Initiating server graceful restart sequence...");
                VesselServerRestartService restartService = new VesselServerRestartService();
                restartService.triggerServerProcessRestart(launcher.getServerRoot());
                return 1;
            }

            if (bukkitBridge.isBukkitCommand(rootCmd)) {
                List<String> bukkitOutput = bukkitBridge.executeBukkitCommand(rootCmd, subArgs, source);
                for (String line : bukkitOutput) {
                    sendMessageToSource(source, line);
                }
                return 1;
            }

            List<String> output = launcher.executeVesselConsoleCommand(input);
            for (String line : output) {
                sendMessageToSource(source, line);
            }
            return 1;
        }
        if ("toString".equals(method.getName())) {
            return "VesselBrigadierCommandInvocationHandler";
        }
        if ("hashCode".equals(method.getName())) {
            return System.identityHashCode(this);
        }
        if ("equals".equals(method.getName()) && args != null && args.length == 1) {
            return proxy == args[0];
        }
        return 0;
    }

    private static String extractInput(Object context) {
        try {
            Method getInputMethod = context.getClass().getMethod("getInput");
            Object res = getInputMethod.invoke(context);
            if (res != null) {
                return res.toString();
            }
        } catch (Exception ignored) {
        }
        return "vessel";
    }

    private static Object extractSource(Object context) {
        try {
            Method getSourceMethod = context.getClass().getMethod("getSource");
            return getSourceMethod.invoke(context);
        } catch (Exception ignored) {
        }
        return null;
    }

    private void sendMessageToSource(Object source, String text) {
        if (source == null || text == null) {
            return;
        }
        try {
            Class<?> componentClass = Class.forName(
                    "net.minecraft.network.chat.Component",
                    true,
                    tierOneClassLoader);
            Method literalMethod = componentClass.getMethod("literal", String.class);
            Object component = literalMethod.invoke(null, text);

            for (Method m : source.getClass().getMethods()) {
                if ("sendSuccess".equals(m.getName()) && m.getParameterCount() == 2) {
                    Class<?> firstParam = m.getParameterTypes()[0];
                    if (Supplier.class.isAssignableFrom(firstParam)) {
                        VesselComponentSupplier supplier = new VesselComponentSupplier(component);
                        m.invoke(source, supplier, false);
                        return;
                    } else if (firstParam.isAssignableFrom(componentClass)) {
                        m.invoke(source, component, false);
                        return;
                    }
                }
            }
        } catch (Exception ignored) {
        }
    }
}
