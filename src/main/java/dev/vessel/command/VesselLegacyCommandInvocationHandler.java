package dev.vessel.command;

import dev.vessel.bootstrap.target.VesselServerLauncher;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Dynamic {@link InvocationHandler} implementing {@code net.minecraft.command.ICommand} for
 * legacy Minecraft versions (1.7.10 – 1.12.2) prior to the introduction of Brigadier in 1.13.
 *
 * <p>Strictly adheres to Rule 17 with zero inner, nested, or anonymous classes, and zero lambdas.</p>
 *
 * @version 1.0.0
 */
public final class VesselLegacyCommandInvocationHandler implements InvocationHandler {

    private final String rootName;
    private final List<String> subcommands;
    private final ClassLoader tierOneClassLoader;
    private final VesselServerLauncher launcher;
    private final VesselBukkitCommandBridge bukkitBridge;
    private final VesselLegacyChatSender chatSender = new VesselLegacyChatSender();

    public VesselLegacyCommandInvocationHandler(
            String rootName,
            List<String> subcommands,
            ClassLoader tierOneClassLoader,
            VesselServerLauncher launcher,
            VesselBukkitCommandBridge bukkitBridge) {
        this.rootName = Objects.requireNonNull(rootName, "rootName must not be null");
        this.subcommands = subcommands != null ? subcommands : Collections.emptyList();
        this.tierOneClassLoader = Objects.requireNonNull(tierOneClassLoader, "tierOneClassLoader must not be null");
        this.launcher = Objects.requireNonNull(launcher, "launcher must not be null");
        this.bukkitBridge = Objects.requireNonNull(bukkitBridge, "bukkitBridge must not be null");
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        String methodName = method.getName();

        if ("getName".equals(methodName) || "getCommandName".equals(methodName)) {
            return rootName;
        }

        if ("getUsage".equals(methodName) || "getCommandUsage".equals(methodName)) {
            return "/" + rootName + " [subcommand]";
        }

        if ("getAliases".equals(methodName) || "getCommandAliases".equals(methodName)) {
            return Collections.emptyList();
        }

        if ("checkPermission".equals(methodName) || "canCommandSenderUseCommand".equals(methodName)) {
            return true;
        }

        if ("isUsernameIndex".equals(methodName)) {
            return false;
        }

        if ("compareTo".equals(methodName)) {
            if (args != null && args.length > 0 && args[0] != null) {
                return rootName.compareTo(args[0].toString());
            }
            return 0;
        }

        if ("getTabCompletions".equals(methodName) || "addTabCompletionOptions".equals(methodName)) {
            return handleTabCompletions(args, methodName);
        }

        if ("execute".equals(methodName) || "processCommand".equals(methodName)) {
            executeCommand(args, methodName);
            return null;
        }

        if ("toString".equals(methodName)) {
            return "VesselLegacyCommand[" + rootName + "]";
        }

        if ("hashCode".equals(methodName)) {
            return rootName.hashCode();
        }

        if ("equals".equals(methodName)) {
            return args != null && args.length > 0 && proxy == args[0];
        }

        return null;
    }

    private List<String> handleTabCompletions(Object[] args, String methodName) {
        int argsIndex = "getTabCompletions".equals(methodName) ? 2 : 1;
        if (args != null && args.length > argsIndex && args[argsIndex] instanceof String[] rawArgs) {
            if (rawArgs.length <= 1) {
                String prefix = rawArgs.length == 1 ? rawArgs[0].toLowerCase(Locale.ROOT) : "";
                List<String> matching = new ArrayList<>();
                for (String sub : subcommands) {
                    if (sub.toLowerCase(Locale.ROOT).startsWith(prefix)) {
                        matching.add(sub);
                    }
                }
                return matching;
            }
        }
        return subcommands;
    }

    private void executeCommand(Object[] args, String methodName) {
        Object sender = null;
        String[] cmdArgs = new String[0];

        if ("execute".equals(methodName) && args != null && args.length >= 3) {
            sender = args[1];
            if (args[2] instanceof String[] parsedArgs) {
                cmdArgs = parsedArgs;
            }
        } else if ("processCommand".equals(methodName) && args != null && args.length >= 2) {
            sender = args[0];
            if (args[1] instanceof String[] parsedArgs) {
                cmdArgs = parsedArgs;
            }
        }

        if (bukkitBridge.isBukkitCommand(rootName)) {
            List<String> responses = bukkitBridge.executeBukkitCommand(rootName, cmdArgs, sender);
            for (String line : responses) {
                chatSender.sendMessage(sender, line, tierOneClassLoader);
            }
            return;
        }

        if ("vessel".equalsIgnoreCase(rootName)) {
            VesselRootCommand rootCommand = new VesselRootCommand();
            String output = rootCommand.executeCommand(cmdArgs);
            chatSender.sendMessage(sender, output, tierOneClassLoader);
            return;
        }

        if ("restart".equalsIgnoreCase(rootName)) {
            chatSender.sendMessage(sender, "[VESSEL] Scheduled server restart initiated.", tierOneClassLoader);
            VesselServerRestartService restartService = new VesselServerRestartService();
            restartService.triggerServerProcessRestart(launcher.getServerRoot());
            return;
        }

        chatSender.sendMessage(
                sender,
                "[VESSEL] Legacy command /" + rootName + " executed successfully.",
                tierOneClassLoader);
    }
}
