package dev.vessel.command;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.Locale;
import java.util.Objects;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

/**
 * Dynamic proxy invocation handler adapting Brigadier's {@code Command#run(CommandContext)} calls
 * to a target Bukkit {@link Command#execute(CommandSender, String, String[])}.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes.</p>
 *
 * @version 1.0.1
 */
public final class BukkitToBrigadierCommandProxy implements InvocationHandler {

    private final Command bukkitCommand;
    private final BukkitBrigadierSenderAdapter senderAdapter;

    public BukkitToBrigadierCommandProxy(
            Command bukkitCommand,
            ClassLoader tierOneClassLoader) {
        this.bukkitCommand = Objects.requireNonNull(bukkitCommand, "bukkitCommand must not be null");
        this.senderAdapter = new BukkitBrigadierSenderAdapter(tierOneClassLoader);
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        if ("run".equals(method.getName()) && args != null && args.length == 1) {
            Object context = args[0];
            String input = extractInput(context);
            Object source = extractSource(context);
            CommandSender sender = senderAdapter.adaptSender(source);

            String trimmed = input.trim();
            String withoutSlash = trimmed.startsWith("/") ? trimmed.substring(1).trim() : trimmed;
            String[] tokens = withoutSlash.split("\\s+");
            String label = tokens.length > 0 ? tokens[0].toLowerCase(Locale.ROOT) : bukkitCommand.getName();
            String[] subArgs = tokens.length > 1 ? new String[tokens.length - 1] : new String[0];
            if (tokens.length > 1) {
                System.arraycopy(tokens, 1, subArgs, 0, subArgs.length);
            }

            try {
                boolean ok = bukkitCommand.execute(sender, label, subArgs);
                return ok ? 1 : 0;
            } catch (Throwable t) {
                System.err.println("[VESSEL] Error dispatching Bukkit command '" + label + "': " + t.getMessage());
                t.printStackTrace();
                return 0;
            }
        }
        if ("toString".equals(method.getName())) {
            return "BukkitToBrigadierCommandProxy[" + bukkitCommand.getName() + "]";
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
        return "";
    }

    private static Object extractSource(Object context) {
        try {
            Method getSourceMethod = context.getClass().getMethod("getSource");
            return getSourceMethod.invoke(context);
        } catch (Exception ignored) {
        }
        return null;
    }
}
